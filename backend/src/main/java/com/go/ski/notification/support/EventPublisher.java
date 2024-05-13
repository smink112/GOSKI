package com.go.ski.notification.support;

import com.go.ski.common.exception.ApiExceptionFactory;
import com.go.ski.feedback.support.dto.FeedbackCreateRequestDTO;
import com.go.ski.notification.support.events.*;
import com.go.ski.notification.support.dto.FcmSendRequestDTO;
import com.go.ski.notification.support.dto.InviteAcceptRequestDTO;
import com.go.ski.notification.support.dto.InviteRequestDTO;
import com.go.ski.payment.core.model.Lesson;
import com.go.ski.payment.core.model.LessonInfo;
import com.go.ski.redis.dto.PaymentCacheDto;
import com.go.ski.team.core.model.Team;
import com.go.ski.team.core.repository.SkiResortRepository;
import com.go.ski.team.core.repository.TeamInstructorRepository;
import com.go.ski.team.core.repository.TeamRepository;
import com.go.ski.team.support.exception.TeamExceptionEnum;
import com.go.ski.user.core.model.Instructor;
import com.go.ski.user.core.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TeamInstructorRepository teamInstructorRepository;
    private final TeamRepository teamRepository;
    private final SkiResortRepository skiResortRepository;

    public void publish(FcmSendRequestDTO fcmSendRequestDTO, User user, String imageUrl, String deviceType) {
        log.info("{}에게 DM 보내기",user.getUserName());
        applicationEventPublisher.publishEvent(MessageEvent.of(fcmSendRequestDTO,user, imageUrl, deviceType));
    }

    public void publish(InviteAcceptRequestDTO inviteAcceptRequestDTO, Team team, Instructor instructor, String deviceType) {
        List<Integer> userIds = new ArrayList<>(teamInstructorRepository.findByTeam(team)
                .orElseThrow(() -> ApiExceptionFactory.fromExceptionEnum(TeamExceptionEnum.TEAM_INSTRUCTOR_NOT_FOUND))
                .stream()
                .map(ti -> ti.getInstructor().getInstructorId())
                .filter(id -> !Objects.equals(id, instructor.getInstructorId()))
                .toList());
        userIds.add(team.getUser().getUserId()); // 사장 Id 추가
        publishEvent(inviteAcceptRequestDTO,userIds, instructor, deviceType);
    }

    private void publishEvent(InviteAcceptRequestDTO inviteAcceptRequestDTO, List<Integer> userIds, Instructor instructor, String deviceType) {
        log.info("초대 승낙 알림");
        userIds.forEach(
            userId -> applicationEventPublisher.publishEvent(
                    InviteAcceptEvent.of(inviteAcceptRequestDTO, userId, instructor, deviceType)
            )
        );
    }


    public void publish(InviteRequestDTO inviteRequestDTO, Team team, String deviceType) {
        log.info("초대 요청 알림");
        applicationEventPublisher.publishEvent(InviteEvent.of(inviteRequestDTO, team, deviceType));
    }


    public void publish(FeedbackCreateRequestDTO feedbackCreateRequestDTO, Lesson lesson, String deviceType) {
        log.info("피드백 생성 알림");
        applicationEventPublisher.publishEvent(FeedbackEvent.of(feedbackCreateRequestDTO,lesson, deviceType));
    }

    public void publish(Lesson lesson, LessonInfo lessonInfo, PaymentCacheDto paymentCache, String deviceType){
        log.info("예약 완료 알림");
        Team team = teamRepository.findById(lesson.getTeam().getTeamId())
                .orElseThrow(() -> ApiExceptionFactory.fromExceptionEnum(TeamExceptionEnum.TEAM_NOT_FOUND));

        Integer resortId = team.getSkiResort().getResortId();

        String resortName = skiResortRepository.findById(resortId)
                .orElseThrow(() -> new RuntimeException("해당 스키장이 존재하지 않습니다.")).getResortName();

        List<Integer> teamMemberIds = new ArrayList<>();
        if (lesson.getInstructor() != null) {  // 지정 강사가 있는 경우 추가
            log.info("lesson.getInstructor - {}",lesson.getInstructor());
            teamMemberIds.add(lesson.getInstructor().getInstructorId());
        }
        teamMemberIds.add(team.getUser().getUserId()); // 사장 추가

        teamMemberIds.forEach(
                teamMemberId ->  applicationEventPublisher.publishEvent(
                        LessonCreateInstructorEvent.of(lessonInfo, resortName, teamMemberId,paymentCache, deviceType))
        );

        // 결제 대표자
        applicationEventPublisher.publishEvent(
                LessonCreateStudentEvent.of(lessonInfo, resortName, lesson.getUser().getUserId(), deviceType));
    }
    
    // 강습 30분 전 알림
    public void publish(LessonInfo lessonInfo, Lesson lesson) {
        Team team = teamRepository.findById(lesson.getTeam().getTeamId())
                .orElseThrow(() -> ApiExceptionFactory.fromExceptionEnum(TeamExceptionEnum.TEAM_NOT_FOUND));

        List<Integer> receiverIds = new ArrayList<>();
        receiverIds.add(lesson.getInstructor().getInstructorId()); // 강사
        receiverIds.add(team.getUser().getUserId()); // 사장
        receiverIds.forEach(
                receiverId ->  applicationEventPublisher.publishEvent(
                        LessonAlertEvent.of(lessonInfo, receiverId, "MOBILE"))
        );
    }

}

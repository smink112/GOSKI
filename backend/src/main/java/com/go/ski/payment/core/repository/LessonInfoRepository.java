package com.go.ski.payment.core.repository;

import com.go.ski.payment.core.model.Lesson;
import com.go.ski.payment.core.model.LessonInfo;
import com.go.ski.team.core.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface LessonInfoRepository extends JpaRepository<LessonInfo, Integer> {
    List<LessonInfo> findByLessonDateAndLessonTeam(LocalDate lessonDate, Team team);
}
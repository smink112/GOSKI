package com.go.ski.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CommonExceptionEnum implements ExceptionEnum {
    DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "데이터베이스 오류"),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 500, "내부 서버 오류"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, 400, "잘못된 파일 형식입니다"),
    IMAGE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,500,"이미지 업로드에 실패했습니다."),
    IMAGE_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,500, "이미지 삭제에 실패했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;
}
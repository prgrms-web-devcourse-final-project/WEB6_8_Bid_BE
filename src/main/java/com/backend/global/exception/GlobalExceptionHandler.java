package com.backend.global.exception;

import com.backend.global.response.RsData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // ServiceException의 resultCode에 따라 동적으로 HTTP 상태 코드를 반환
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(ServiceException e) {
        HttpStatus httpStatus;
        try {
            int resultCode = Integer.parseInt(e.getResultCode().split("-")[0]);
            httpStatus = HttpStatus.valueOf(resultCode);
        } catch (NumberFormatException ex) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }

        String rawMessage = e.getMessage();
        // 메시지에서 resultCode 접두사 제거
        if (rawMessage != null && rawMessage.startsWith(e.getResultCode() + ":")) {
            rawMessage = rawMessage.substring(e.getResultCode().length() + 1);
        }

        RsData<Void> rsData = new RsData<>(e.getResultCode(), rawMessage);

        return new ResponseEntity<>(rsData, httpStatus);
    }
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<RsData<Void>> handle(NoSuchElementException ex) {
        // HTTP 404 Not Found 상태와 함께 에러 응답을 반환합니다.
        return new ResponseEntity<>(
                new RsData<>(
                        "404-1",
                        ex.getMessage()
                ),
                NOT_FOUND
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RsData<Void>> handle(IllegalArgumentException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "400-1",
                        ex.getMessage()
                ),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<RsData<Void>> handle(NullPointerException ex) {
        return new ResponseEntity<>(
                new RsData<>(
                        "404-1",
                        "NullPointerException"
                ),
                NOT_FOUND
        );
    }
}

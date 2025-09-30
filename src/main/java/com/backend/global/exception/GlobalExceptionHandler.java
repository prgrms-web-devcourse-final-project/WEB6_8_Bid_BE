package com.backend.global.exception;

import com.backend.global.response.RsData;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 커스텀 ServiceException: resultCode를 HTTP 코드로 사용 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<RsData<Void>> handleServiceException(ServiceException e) {
        HttpStatus httpStatus;
        try {
            httpStatus = HttpStatus.valueOf(Integer.parseInt(e.getResultCode()));
        } catch (NumberFormatException ex) {
            httpStatus = HttpStatus.BAD_REQUEST;
        }

        String msg = e.getMessage();
        if (msg != null && msg.startsWith(e.getResultCode() + ":")) {
            msg = msg.substring(e.getResultCode().length() + 1);
        }
        return new ResponseEntity<>(new RsData<>(e.getResultCode(), msg), httpStatus);
    }

    /** 스프링의 명시적 상태 예외는 그대로 통과 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RsData<Void>> handleRse(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        return new ResponseEntity<>(new RsData<>(String.valueOf(status.value()), e.getReason()), status);
    }

    /** 검증 실패: 400 */
    @ExceptionHandler({
            IllegalArgumentException.class,            // 비즈니스/입력 검증
            HttpMessageNotReadableException.class,    // JSON 파싱 에러
            ConstraintViolationException.class        // @Validated on params
    })
    public ResponseEntity<RsData<Void>> handleBadRequest(Exception e) {
        return new ResponseEntity<>(new RsData<>("400", e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    /** @Valid body 바인딩 에러: 400 + 필드 메시지 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("잘못된 요청입니다.");
        return new ResponseEntity<>(new RsData<>("400", msg), HttpStatus.BAD_REQUEST);
    }

    /** 리소스 없음: 404 */
    @ExceptionHandler({ NoSuchElementException.class, NullPointerException.class })
    public ResponseEntity<RsData<Void>> handleNotFound(RuntimeException e) {
        return new ResponseEntity<>(new RsData<>("404", e.getMessage() != null ? e.getMessage() : "Not Found"),
                HttpStatus.NOT_FOUND);
    }

    /** 인증/인가: 401/403 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RsData<Void>> handleAuth(AuthenticationException e) {
        return new ResponseEntity<>(new RsData<>("401", "인증이 필요합니다."), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RsData<Void>> handleAccess(AccessDeniedException e) {
        return new ResponseEntity<>(new RsData<>("403", "접근 권한이 없습니다."), HttpStatus.FORBIDDEN);
    }

    /** 그 외: 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<Void>> handleEtc(Exception e) {
        return new ResponseEntity<>(new RsData<>("500", "서버 오류"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

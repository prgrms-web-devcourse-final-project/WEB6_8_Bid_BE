package com.backend.domain.review.exception;

import com.backend.global.exception.ServiceException;
import com.backend.global.response.RsStatus;

public class ReviewException extends ServiceException {
    public ReviewException(RsStatus rsStatus) {
        super(rsStatus.getResultCode(), rsStatus.getDefaultMessage());
    }

    public static ReviewException reviewNotFound() {
        return new ReviewException(RsStatus.REVIEW_NOT_FOUND);
    }

    public static ReviewException alreadyExists() {
        return new ReviewException(RsStatus.REVIEW_ALREADY_EXISTS);
    }

    public static ReviewException accessDenied() {
        return new ReviewException(RsStatus.REVIEW_ACCESS_DENIED);
    }
}

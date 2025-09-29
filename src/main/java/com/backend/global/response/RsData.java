package com.backend.global.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

public record RsData<T>(
        @NonNull String resultCode,
        @JsonIgnore int statusCode,
        @NonNull String msg,
        T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public RsData(String resultCode, String msg, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), msg, data);
    }

    public static <T> RsData<T> ok(String msg, T data) {
        return new RsData<>(RsStatus.OK.getResultCode(), RsStatus.OK.getStatusCode(), msg, data);
    }

    public static RsData<Void> ok(String msg) {
        return new RsData<>(RsStatus.OK.getResultCode(), RsStatus.OK.getStatusCode(), msg, null);
    }

    public static <T> RsData<T> ok(int detailCode, String msg, T data) {
        return new RsData<>(RsStatus.OK.getResultCode() + "-" + detailCode, RsStatus.OK.getStatusCode(), msg, data);
    }

    public static <T> RsData<T> created(String msg, T data) {
        return new RsData<>(RsStatus.CREATED.getResultCode(), RsStatus.CREATED.getStatusCode(), msg, data);
    }
}

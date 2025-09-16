package com.backend.global.exception;

import com.backend.global.rsData.RsData;
import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {
    private String resultCode;
    private String msg;

    public ServiceException(String resultCode, String msg) {
        super(resultCode+":"+msg);
        this.resultCode = resultCode;
        this.msg = msg;
    }
    public RsData<Void> getRsData() {
        return new RsData<>(resultCode, msg, null);
    }
}

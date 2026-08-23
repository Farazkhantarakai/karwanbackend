package com.aiecomm.camp.common.exception;

import lombok.Getter;

@Getter
public class EmailAlreadyExistsException extends RuntimeException {
    private final String code;
    private final String error;
    private final String provider;

    public EmailAlreadyExistsException(String message) {
        super(message);
        this.code = "EMAIL_ALREADY_EXISTS";
        this.error = "Email address is already in use.";
        this.provider = "LOCAL";
    }

    public EmailAlreadyExistsException(String code, String error, String message, String provider) {
        super(message);
        this.code = code;
        this.error = error;
        this.provider = provider;
    }
}

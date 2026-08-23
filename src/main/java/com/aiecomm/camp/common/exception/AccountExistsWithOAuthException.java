package com.aiecomm.camp.common.exception;

import lombok.Getter;

@Getter
public class AccountExistsWithOAuthException extends RuntimeException {
    private final String code;
    private final String error;
    private final String provider;

    public AccountExistsWithOAuthException(String message) {
        super(message);
        this.code = "ACCOUNT_EXISTS_WITH_GOOGLE";
        this.error = "Account exists with Google Sign-In";
        this.provider = "GOOGLE";
    }

    public AccountExistsWithOAuthException(String code, String error, String message, String provider) {
        super(message);
        this.code = code;
        this.error = error;
        this.provider = provider;
    }
}

package com.aiecomm.camp.modules.commerce.exception;

import lombok.Getter;

/**
 * Runtime exception for all commerce domain violations.
 * The code field maps directly to the CommerceErrorCode contract
 * (PRODUCT_NOT_FOUND, INSUFFICIENT_INVENTORY, CROSS_STORE_ACCESS, etc.)
 * defined in lib/api/commerceErrors.ts on the frontend.
 */
@Getter
public class CommerceException extends RuntimeException {

    private final String code;
    private final Object details;

    public CommerceException(String code) {
        super(code);
        this.code = code;
        this.details = null;
    }

    public CommerceException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public CommerceException(String code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}

package com.aiecomm.camp.common.exception;

public class StoreNotFoundException extends ResourceNotFoundException {

    public StoreNotFoundException() {
        super("Access Denied or Store Not Found");
    }

    public StoreNotFoundException(String message) {
        super(message);
    }

    public StoreNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

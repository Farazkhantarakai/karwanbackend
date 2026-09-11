package com.aiecomm.camp.modules.subscription.exceptions;

import lombok.Getter;

@Getter
public class PlanLimitExceededException extends RuntimeException {
    private final String code;
    private final boolean upgradeRequired;

    public PlanLimitExceededException(String code, String message, boolean upgradeRequired) {
        super(message);
        this.code = code;
        this.upgradeRequired = upgradeRequired;
    }
}

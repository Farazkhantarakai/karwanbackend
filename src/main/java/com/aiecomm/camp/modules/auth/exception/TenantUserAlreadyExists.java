package com.aiecomm.camp.modules.auth.exception;

import lombok.Getter;


public class TenantUserAlreadyExists extends RuntimeException {

    public TenantUserAlreadyExists(String message){
        super(message);
    }

}

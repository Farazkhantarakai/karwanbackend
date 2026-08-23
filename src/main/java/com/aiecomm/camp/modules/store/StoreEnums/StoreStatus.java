package com.aiecomm.camp.modules.store.StoreEnums;

import lombok.ToString;

public enum StoreStatus {
    IsActive("Is Active")
    ,NotActive("Not Active");

    private final String item;

    StoreStatus(String value){
        this.item=value;
    }



    @Override
    public String toString() {
        return this.item;
    }
}

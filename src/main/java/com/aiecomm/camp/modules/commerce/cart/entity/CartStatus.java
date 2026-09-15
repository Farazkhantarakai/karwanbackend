package com.aiecomm.camp.modules.commerce.cart.entity;

public enum CartStatus {
    ACTIVE,       // customer is actively shopping
    CHECKOUT,     // checkout process started
    CONVERTED,    // order placed, cart closed
    ABANDONED,    // no activity for configured period
    EXPIRED       // TTL exceeded
}

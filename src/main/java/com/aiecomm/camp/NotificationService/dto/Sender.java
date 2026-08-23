package com.aiecomm.camp.NotificationService.dto;


import lombok.Builder;

@Builder
public record Sender(String name,String email) {
}

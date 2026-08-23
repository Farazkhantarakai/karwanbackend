package com.aiecomm.camp.NotificationService.dto;

import com.aiecomm.camp.NotificationService.NotificationResponse;

public record BrevoResponse(String messageId) implements NotificationResponse {
}

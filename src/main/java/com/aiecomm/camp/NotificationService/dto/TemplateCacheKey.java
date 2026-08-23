package com.aiecomm.camp.NotificationService.dto;

import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import com.aiecomm.camp.NotificationService.enums.NotificationEvent;

public record TemplateCacheKey(
        NotificationEvent event,
        NotificationChannel channel
) {
}
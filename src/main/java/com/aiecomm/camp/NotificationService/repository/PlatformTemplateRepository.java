package com.aiecomm.camp.NotificationService.repository;


import com.aiecomm.camp.NotificationService.entity.PlatformTemplate;
import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import com.aiecomm.camp.NotificationService.enums.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformTemplateRepository
        extends JpaRepository<PlatformTemplate, Long> {

    Optional<PlatformTemplate> findByEventTypeAndChannel(
            NotificationEvent eventType,
            NotificationChannel channel
    );
}
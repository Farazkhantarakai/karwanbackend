package com.aiecomm.camp.NotificationService.repository;


import com.aiecomm.camp.NotificationService.entity.NotificationConfiguration;
import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationConfigurationRepository
        extends JpaRepository<NotificationConfiguration, Long> {

    Optional<NotificationConfiguration> findByChannel(
            NotificationChannel channel
    );
}
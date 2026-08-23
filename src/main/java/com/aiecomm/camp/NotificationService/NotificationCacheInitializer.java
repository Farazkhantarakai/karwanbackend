package com.aiecomm.camp.NotificationService;

import com.aiecomm.camp.NotificationService.repository.NotificationConfigurationRepository;
import com.aiecomm.camp.NotificationService.repository.PlatformTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class NotificationCacheInitializer {

    private final PlatformTemplateRepository templateRepository;
    private final NotificationConfigurationRepository configurationRepository;
    private final NotificationCache notificationCache;

    private Logger logger= LoggerFactory.getLogger(NotificationCacheInitializer.class);

    @Bean
    public CommandLineRunner loadNotificationData() {
        return args -> {

            configurationRepository.findAll()
                    .forEach(notificationCache::putConfiguration);
             logger.info(" configurations loaded ");
            templateRepository.findAll()
                    .forEach(notificationCache::putTemplate);
            logger.info("templates loaded");
        };
    }
}
package com.aiecomm.camp.NotificationService;

import com.aiecomm.camp.NotificationService.dto.TemplateCacheKey;
import com.aiecomm.camp.NotificationService.entity.NotificationConfiguration;
import com.aiecomm.camp.NotificationService.entity.PlatformTemplate;
import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import com.aiecomm.camp.NotificationService.enums.NotificationEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationCache {

    private final Map<NotificationChannel, NotificationConfiguration>
            configurations = new ConcurrentHashMap<>();

    private final Map<TemplateCacheKey, PlatformTemplate>
            templates = new ConcurrentHashMap<>();

    public void putConfiguration(NotificationConfiguration configuration) {

        configurations.put(
                configuration.getChannel(),
                configuration
        );
    }

    public NotificationConfiguration getConfiguration(
            NotificationChannel channel
    ) {
        return configurations.get(channel);
    }

    public void putTemplate(PlatformTemplate template) {

        TemplateCacheKey key = new TemplateCacheKey(
                template.getEventType(),
                template.getChannel()
        );

        templates.put(key, template);
    }

    public PlatformTemplate getTemplate(
            NotificationEvent event,
            NotificationChannel channel
    ) {
        return templates.get(
                new TemplateCacheKey(event, channel)
        );
    }
}
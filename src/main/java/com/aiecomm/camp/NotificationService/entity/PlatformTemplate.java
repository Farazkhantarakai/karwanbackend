package com.aiecomm.camp.NotificationService.entity;

import com.aiecomm.camp.NotificationService.enums.NotificationChannel;
import com.aiecomm.camp.NotificationService.enums.NotificationEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "platform_template",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_platform_template_event",
                        columnNames = "event_type"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEvent eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel",nullable = false,length = 50)
    private NotificationChannel channel;

    @Column(name = "template_subject", nullable = false, length = 255)
    private String templateSubject;

    @Column(
            name = "template_body",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String templateBody;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
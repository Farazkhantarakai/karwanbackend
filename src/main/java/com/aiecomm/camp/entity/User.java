package com.aiecomm.camp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

//    @Column(name = "image_url", columnDefinition = "TEXT")
//    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id", columnDefinition = "TEXT")
    private String providerId;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "business_purpose")
    private String businessPurpose;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "trail_start_date",nullable = false)
    private Instant trailStartedAt;

    @Column(name = "trail_end_date",nullable = false)
    private Instant trailEndAt;

    @Column(name = "subscription_started_at",nullable = true)
    private Instant subscriptionStartedAt;

    @Column(name = "subscription_ended_at", nullable = true)
    private Instant subscriptionEndedAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.trailStartedAt == null) {
            this.trailStartedAt = Instant.now();
        }
        if (this.trailEndAt == null) {
            this.trailEndAt = Instant.now().plus(3, java.time.temporal.ChronoUnit.DAYS);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}


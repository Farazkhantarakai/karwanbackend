package com.aiecomm.camp.modules.tenant.entity;


import com.aiecomm.camp.modules.tenant.TenatEnums.OnboardingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Builder
@Table(name = "Tenant")
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
   private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
   private OnboardingStatus onBoardingStatus=OnboardingStatus.NotOnboarded;

    @Column(name = "Created_On",nullable = false)
    private Instant createdOn;

    @Column(name = "Updated_On",nullable = false)
    private Instant updatedOn;

}



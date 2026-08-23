package com.aiecomm.camp.modules.tenant.entity;


import com.aiecomm.camp.modules.tenant.TenatEnums.OnboardingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.Set;
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

    @OneToMany(mappedBy = "tenant")
    private Set<TenantUser> tenantUsers;

    @Column(name = "Created_On",nullable = false)
    private Instant createdOn;

    @Column(name = "Updated_On",nullable = false)
    private Instant updatedOn;

    @Column(name = "Created_By",nullable = false)
    private String createdBy;

    @Column(name = "Updated_By",nullable = false)
    private String updatedBy;

}



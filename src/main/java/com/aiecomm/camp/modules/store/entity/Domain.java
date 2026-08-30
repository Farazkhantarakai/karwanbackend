package com.aiecomm.camp.modules.store.entity;

import com.aiecomm.camp.modules.store.StoreEnums.DomainStatus;
import com.aiecomm.camp.modules.store.StoreEnums.SSLSTATUS;
import jakarta.persistence.*;
import lombok.*;
import net.minidev.json.annotate.JsonIgnore;

import java.time.Instant;

@Entity
@Table(name = "domains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "store")
@EqualsAndHashCode(exclude = "store")
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long domainId;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "domainlink")
    private String domainlink;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_custom", nullable = false)
    @Builder.Default
    private Boolean isCustom = false;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_status",nullable = false)
    private DomainStatus domainStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ssl_status",nullable = false)
    private SSLSTATUS sslstatus;

    @Column(name = "target",length = 100)
    private String target;

    @Column(name = "dns_configured")
    private Boolean dnsConfigured;

    @Column(name = "updated_on")
    private Instant updatedOn;

    @PrePersist
    protected void onCreate() {
        this.createdOn = Instant.now();
        this.updatedOn = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedOn = Instant.now();
    }
}

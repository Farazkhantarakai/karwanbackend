package com.aiecomm.camp.modules.store.entity;

import jakarta.persistence.*;
import lombok.*;

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

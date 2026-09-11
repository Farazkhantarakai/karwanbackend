package com.aiecomm.camp.modules.media.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "media_references", uniqueConstraints = {
        @UniqueConstraint(name = "uk_media_references_asset_target", columnNames = {"media_asset_id", "referenceable_type", "referenceable_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"mediaAsset"})
@EqualsAndHashCode(exclude = {"mediaAsset"})
public class MediaReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @Column(name = "referenceable_type", length = 50, nullable = false)
    private String referenceableType;

    @Column(name = "referenceable_id", nullable = false)
    private Long referenceableId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}

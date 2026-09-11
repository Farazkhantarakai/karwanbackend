package com.aiecomm.camp.modules.media.entity;

import com.aiecomm.camp.core.entity.BaseTenantEntity;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "media_assets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"tenant", "store"})
@ToString(exclude = {"tenant", "store"})
public class MediaAsset extends BaseTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "object_key", length = 512, nullable = false, unique = true)
    private String objectKey;

    @Column(name = "public_url", length = 1024)
    private String publicUrl;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private AssetStatus status = AssetStatus.UPLOADED;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ready_at")
    private Instant readyAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = AssetStatus.UPLOADED;
        }
    }
}

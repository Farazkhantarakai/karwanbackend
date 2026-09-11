package com.aiecomm.camp.modules.media.repository;

import com.aiecomm.camp.modules.media.entity.AssetStatus;
import com.aiecomm.camp.modules.media.entity.MediaAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {

    Page<MediaAsset> findByStore_StoreIdAndTenant_TenantIdAndStatus(Long storeId, UUID tenantId, AssetStatus status, Pageable pageable);

    Page<MediaAsset> findByStore_StoreIdAndTenant_TenantId(Long storeId, UUID tenantId, Pageable pageable);

    Page<MediaAsset> findByStore_StoreIdAndTenant_TenantIdAndOriginalFilenameContainingIgnoreCase(Long storeId, UUID tenantId, String filename, Pageable pageable);

    Optional<MediaAsset> findByIdAndStore_StoreIdAndTenant_TenantId(Long id, Long storeId, UUID tenantId);

    Optional<MediaAsset> findByIdAndTenant_TenantId(Long id, UUID tenantId);

    Optional<MediaAsset> findByObjectKey(String objectKey);

    boolean existsByObjectKey(String objectKey);

    @Query("SELECT COALESCE(SUM(m.size), 0L) FROM MediaAsset m WHERE m.tenant.tenantId = :tenantId AND m.status != com.aiecomm.camp.modules.media.entity.AssetStatus.DELETED")
    Long sumSizeByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(m.size), 0L) FROM MediaAsset m WHERE m.store.storeId = :storeId AND m.tenant.tenantId = :tenantId AND m.status != com.aiecomm.camp.modules.media.entity.AssetStatus.DELETED")
    Long sumSizeByStoreIdAndTenantId(@Param("storeId") Long storeId, @Param("tenantId") UUID tenantId);

    List<MediaAsset> findByStatusInAndCreatedAtBefore(List<AssetStatus> statuses, Instant cutoff);
}

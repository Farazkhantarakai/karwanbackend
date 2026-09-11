package com.aiecomm.camp.modules.media.repository;

import com.aiecomm.camp.modules.media.entity.Media;
import com.aiecomm.camp.modules.media.entity.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    Optional<Media> findByIdAndTenant_TenantId(UUID id, UUID tenantId);

    List<Media> findByStore_StoreIdAndTenant_TenantId(Long storeId, UUID tenantId);

    @Query("SELECT m FROM Media m WHERE m.status IN :statuses AND m.createdAt < :cutoff AND m.id NOT IN (SELECT pm.media.id FROM ProductMedia pm)")
    List<Media> findAbandonedUnattachedMedia(
            @Param("statuses") List<MediaStatus> statuses,
            @Param("cutoff") Instant cutoff
    );
}

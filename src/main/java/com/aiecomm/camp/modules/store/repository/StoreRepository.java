package com.aiecomm.camp.modules.store.repository;

import com.aiecomm.camp.modules.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, Object> {
    List<Store> findByTenant_TenantId(UUID tenantId);

    /** Dev-only: resolve store by slug for X-Store-Slug header fallback. */
    @Query("SELECT s FROM Store s JOIN FETCH s.tenant LEFT JOIN FETCH s.settings WHERE s.slug = :slug")
    Optional<Store> findBySlug(@Param("slug") String slug);
}

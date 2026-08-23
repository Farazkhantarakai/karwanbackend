package com.aiecomm.camp.modules.store.repository;

import com.aiecomm.camp.modules.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store,Object> {
    List<Store> findByTenant_TenantId(UUID tenantId);
}

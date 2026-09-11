package com.aiecomm.camp.modules.subscription.repository;

import com.aiecomm.camp.modules.subscription.entity.StoreSubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreSubscriptionPlanRepository extends JpaRepository<StoreSubscriptionPlan, Long> {
    Optional<StoreSubscriptionPlan> findFirstByTenant_TenantId(UUID tenantId);
}

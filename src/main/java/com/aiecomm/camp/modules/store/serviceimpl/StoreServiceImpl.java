package com.aiecomm.camp.modules.store.serviceimpl;

import com.aiecomm.camp.modules.store.StoreEnums.StoreStatus;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.store.service.StoreService;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private TenantRepository tenantRepository;

    Logger logger= LoggerFactory.getLogger(StoreServiceImpl.class);


    @Override
    public List<Store> getStoreStatusForUser(UUID tenantId) {
        if (tenantId == null) {
            return java.util.Collections.emptyList();
        }
        List<Store> tenantStores = storeRepository.findByTenant_TenantId(tenantId);
        return tenantStores != null ? tenantStores : java.util.Collections.emptyList();
    }

    public Store createStoreAgainstTheTenant(UUID tenantId, String domainName, String storeName) {
try {
    Optional<Tenant> tenant = tenantRepository.findById(tenantId);
    if (tenant.isPresent()) {

        Store store = new Store();
        store.setTenant(tenant.get());
        store.setDomainname(domainName);
        store.setStatus(StoreStatus.IsActive);
        store.setStorename(storeName);
        store.setTrailStart(Instant.now());
        store.setTrailEnd(Instant.now().plus(Duration.ofDays(15)));
        store.setCreatedOn(Instant.now());
        store.setUpdatedOn(Instant.now());
        store.setCreatedBy("System");
        store.setUpdatedBy("System");
        storeRepository.save(store);

        return store;
    }

} catch (Exception e) {
    logger.info("store cannot be saved something went wrong" +e.getMessage());
}

return null;
    }
}

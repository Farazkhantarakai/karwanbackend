package com.aiecomm.camp.modules.store.service;

import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.entity.Store;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    List<Store> getStoreStatusForUser(UUID tenantId);

    Store createStoreAgainstTheTenant(UUID tenantId, String domainName, String storeName);

    List<DomainDto> getStoreDomain(Long storeId);

    List<DomainDto> findFreeDomains(String name);

    Store getStoreForUser(Long storeId);

}

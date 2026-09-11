package com.aiecomm.camp.modules.store.service;

import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.dto.StoreDto;

import java.util.List;
import java.util.UUID;

public interface StoreService {

    List<StoreDto> getStoreStatusForUser(UUID tenantId);

    StoreDto createStoreAgainstTheTenant(UUID tenantId, String domainName, String storeName);

    List<DomainDto> getStoreDomain(Long storeId);

    List<DomainDto> findFreeDomains(String name);

    List<StoreDto> getStoreForUser(Long storeId);

}

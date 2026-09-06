package com.aiecomm.camp.modules.store.serviceimpl;

import com.aiecomm.camp.common.exception.StoreNotFoundException;
import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.store.StoreEnums.DomainStatus;
import com.aiecomm.camp.modules.store.StoreEnums.SSLSTATUS;
import com.aiecomm.camp.modules.store.StoreEnums.StoreStatus;
import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.dto.StoreDto;
import com.aiecomm.camp.modules.store.entity.Domain;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.DomainRepository;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.store.service.StoreService;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final TenantRepository tenantRepository;
    private final DomainRepository domainRepository;

    @Value("${app-domain-suffix}")
    private String platformPrefix;

    @Override
    public List<StoreDto> getStoreStatusForUser(UUID tenantId) {
        if (tenantId == null) {
            return Collections.emptyList();
        }
        List<Store> tenantStores = storeRepository.findByTenant_TenantId(tenantId);
        if (tenantStores == null) {
            return Collections.emptyList();
        }
        return tenantStores.stream()
                .map(StoreDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StoreDto createStoreAgainstTheTenant(UUID tenantId, String domainName, String storeName) {
        try {
            if (tenantId == null) {
                log.error("tenantId is null while creating store");
                return null;
            }

            Optional<Tenant> tenant = tenantRepository.findById(tenantId);
            if (tenant.isPresent()) {
                String dummyDomain = (domainName != null && !domainName.trim().isEmpty())
                        ? domainName.trim()
                        : getDomainDummyName();
                String basePath = "https://";

                Domain domain = new Domain();
                domain.setName(dummyDomain);
                domain.setDomainlink(basePath + dummyDomain + platformPrefix);
                domain.setUpdatedOn(null);
                domain.setIsVerified(true);
                domain.setIsPrimary(true);
                domain.setIsCustom(false);
                domain.setSslstatus(SSLSTATUS.NOTACTIVE);
                domain.setDnsConfigured(false);
                domain.setDomainStatus(DomainStatus.ACTIVE);
                domain.setTarget("cname.karwan.pk");
                domain.setCreatedOn(Instant.now());

                Store store = new Store();
                store.setTenant(tenant.get());
                store.setDomainname(dummyDomain);
                store.setStatus(StoreStatus.IsActive);
                store.setStorename(storeName);
                store.setTrailStart(Instant.now());
                store.setSlug(storeName != null ? storeName.toLowerCase() : dummyDomain.toLowerCase());
                store.setTrailEnd(Instant.now().plus(Duration.ofDays(15)));
                store.setCreatedOn(Instant.now());
                store.setUpdatedOn(Instant.now());
                store.setCreatedBy("System");
                store.setUpdatedBy("System");
                store.addDomain(domain);

                storeRepository.save(store);

                return StoreDto.fromEntity(store);
            }
        } catch (Exception e) {
            log.error("Store cannot be saved, something went wrong: {}", e.getMessage(), e);
        }

        return null;
    }

    String getDomainDummyName() {
        UUID uuid = UUID.randomUUID();
        int sixDigits = Math.abs(uuid.hashCode()) % 1000000;
        String digits = String.format("%06d", sixDigits);
        return digits.substring(0, 3) + "-" + digits.substring(3);
    }

    @Override
    public List<DomainDto> getStoreDomain(Long storeId) {
        List<DomainDto> domainDto = new ArrayList<>();
        if (storeId == null) {
            return domainDto;
        }

        Optional<List<Domain>> domains = domainRepository.findByStore_StoreId(storeId);
        if (domains.isPresent() && domains.get() != null) {
            log.info("domains for the store {}: {}", storeId, domains.get());
            for (Domain domain : domains.get()) {
                DomainDto result = DomainDto.fromEntity(domain);
                domainDto.add(result);
            }
        }
        return domainDto;
    }

    @Override
    public List<DomainDto> findFreeDomains(String name) {
        List<DomainDto> domainDto = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) {
            return domainDto;
        }

        Optional<List<Domain>> domains = domainRepository.findByName(name.trim());

        if (domains.isEmpty() || domains.get().isEmpty()) {
            return domainDto;
        }

        return domains.get().stream()
                .map(DomainDto::fromEntity)
                .toList();
    }

    @Override
    public StoreDto getStoreForUser(Long storeId) {
        UUID currentTenantId = TenantContext.getTenantId();
        if (currentTenantId == null) {
            throw new StoreNotFoundException("Access Denied: Tenant context not found");
        }

        return storeRepository.findById(storeId)
                .filter(store -> store.getTenant() != null && currentTenantId.equals(store.getTenant().getTenantId()))
                .map(StoreDto::fromEntity)
                .orElseThrow(() -> new StoreNotFoundException("Access Denied or Store Not Found"));
    }
}

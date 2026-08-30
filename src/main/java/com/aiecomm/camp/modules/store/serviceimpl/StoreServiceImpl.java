package com.aiecomm.camp.modules.store.serviceimpl;

import com.aiecomm.camp.modules.store.StoreEnums.DomainStatus;
import com.aiecomm.camp.modules.store.StoreEnums.SSLSTATUS;
import com.aiecomm.camp.modules.store.StoreEnums.StoreStatus;
import com.aiecomm.camp.modules.store.dto.DomainDto;
import com.aiecomm.camp.modules.store.entity.Domain;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.DomainRepository;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.store.service.StoreService;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.tenant.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

    @Autowired
    private DomainRepository domainRepository;


    @Value("${app-domain-suffix}")
    private String platformPrefix;

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

   String dummyDomain= getDomainDummyName();
   String basePath="https://";

    if (tenant.isPresent()) {

        Domain domain=new Domain();

        domain.setName(dummyDomain);
        domain.setDomainlink(basePath+dummyDomain+platformPrefix);
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
        store.setDomainname(getDomainDummyName());
        store.setStatus(StoreStatus.IsActive);
        store.setStorename(storeName);
        store.setTrailStart(Instant.now());
        store.setSlug(storeName.toLowerCase());
        store.setTrailEnd(Instant.now().plus(Duration.ofDays(15)));
        store.setCreatedOn(Instant.now());
        store.setUpdatedOn(Instant.now());
        store.setCreatedBy("System");
        store.setUpdatedBy("System");
        store.addDomain(domain);

        storeRepository.save(store);

        return store;
    }

} catch (Exception e) {
    logger.info("store cannot be saved something went wrong" +e.getMessage());
}

return null;
    }


    String getDomainDummyName(){
        UUID uuid = UUID.randomUUID();
        int sixDigits = Math.abs(uuid.hashCode()) % 1000000;
        String digits = String.format("%06d", sixDigits);
       return  digits.substring(0, 3) + "-" + digits.substring(3);
    }


    public List<DomainDto> getStoreDomain(Long storeId) {

        List<DomainDto> domainDto=new ArrayList<>();


       Optional<List<Domain>> domains=domainRepository.findByStore_StoreId(storeId);

        logger.info("domains for the store "+storeId +" domains "+domains.get());

        for(Domain domain:domains.get()){

         DomainDto result=   DomainDto.fromEntity(domain);
            domainDto.add(result);
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


}

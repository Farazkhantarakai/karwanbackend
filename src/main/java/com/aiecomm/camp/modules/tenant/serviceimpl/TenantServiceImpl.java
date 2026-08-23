package com.aiecomm.camp.modules.tenant.serviceimpl;

import com.aiecomm.camp.modules.auth.exception.TenantUserAlreadyExists;
import com.aiecomm.camp.modules.tenant.TenatEnums.OnboardingStatus;
import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.tenant.entity.TenantUser;
import com.aiecomm.camp.modules.tenant.repository.TenantRepository;
import com.aiecomm.camp.modules.tenant.repository.TenantUserRepository;
import com.aiecomm.camp.modules.tenant.service.TenantService;
import com.aiecomm.camp.modules.user.entity.Role;
import com.aiecomm.camp.modules.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TenantServiceImpl implements TenantService {

    @Autowired
    private TenantUserRepository tenantUserRepository;
    @Autowired
    private TenantRepository tenantRepository;

  Logger logger=LoggerFactory.getLogger(TenantServiceImpl.class);

    @Override
    public  UUID  createTenantForUser(User user) {

        UUID tenantId=null;

     Boolean isExist=  tenantUserRepository.existsByUserId(user.getId());

     if(isExist){


         throw  new TenantUserAlreadyExists("Tenant Already Exists ");
     }

        if(user!=null){
            Tenant tenant=new Tenant();
            tenant.setOnBoardingStatus(OnboardingStatus.NotOnboarded);
            tenant.setCreatedOn(Instant.now());
            tenant.setUpdatedOn(Instant.now());
            tenant.setCreatedBy("System");
            tenant.setUpdatedBy("System");
            Tenant savedTenant= tenantRepository.save(tenant);
              tenantId=savedTenant.getTenantId();
            TenantUser tenantUser = new TenantUser();
            tenantUser.setUser(user);
            tenantUser.setTenant(tenant);
            tenantUser.setRole(Role.ROLE_ADMIN);
            tenantUser.setCreatedOn(Instant.now());
            tenantUser.setUpdatedOn(Instant.now());

            tenantUserRepository.save(tenantUser);
            logger.info("tenant user saved ");

return tenantId;
        }

        logger.error("user is null that why not created");
return null;
    }


    public UUID getUserTenantId(User user) {

      Optional<TenantUser> tenantUser= tenantUserRepository.findByUserId(user.getId());

        return tenantUser.map(value -> value.getTenant().getTenantId()).orElse(null);


    }
}

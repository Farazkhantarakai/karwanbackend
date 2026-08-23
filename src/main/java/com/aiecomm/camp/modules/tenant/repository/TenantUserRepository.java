package com.aiecomm.camp.modules.tenant.repository;

import com.aiecomm.camp.modules.tenant.entity.TenantUser;
import com.aiecomm.camp.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser,Long> {

   Boolean existsByUserId(Long id);

    Optional<TenantUser> findByUserId(Long userId);
}

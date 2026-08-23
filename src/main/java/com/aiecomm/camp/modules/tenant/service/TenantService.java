package com.aiecomm.camp.modules.tenant.service;

import com.aiecomm.camp.modules.tenant.entity.Tenant;
import com.aiecomm.camp.modules.user.entity.User;

import java.util.UUID;

public interface TenantService {

  public UUID createTenantForUser(User user);

}

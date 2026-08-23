package com.aiecomm.camp.modules.store.service;

import com.aiecomm.camp.modules.store.entity.Store;

import java.util.List;
import java.util.UUID;

public interface StoreService {

  public List<Store> getStoreStatusForUser(UUID email);

}

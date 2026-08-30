package com.aiecomm.camp.modules.store.repository;

import com.aiecomm.camp.modules.store.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain,Long> {

    @Query("SELECT d FROM Domain d WHERE d.store.storeId = :storeId")
   Optional<List<Domain>> findByStore_StoreId(@Param("storeId") Long storeId);

    Optional<List<Domain>> findByName(String name);

}

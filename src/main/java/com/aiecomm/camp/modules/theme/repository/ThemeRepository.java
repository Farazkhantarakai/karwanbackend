package com.aiecomm.camp.modules.theme.repository;

import com.aiecomm.camp.modules.theme.entity.Theme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByStore_StoreId(Long storeId);
}

package com.aiecomm.camp.modules.theme.repository;

import com.aiecomm.camp.modules.theme.entity.ThemeVersion;
import com.aiecomm.camp.modules.theme.entity.ThemeVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThemeVersionRepository extends JpaRepository<ThemeVersion, Long> {
    Optional<ThemeVersion> findByTheme_IdAndStatus(Long themeId, ThemeVersionStatus status);
}

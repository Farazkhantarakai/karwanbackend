package com.aiecomm.camp.modules.theme.serviceimpl;

import com.aiecomm.camp.core.TenantContext;
import com.aiecomm.camp.modules.store.entity.Store;
import com.aiecomm.camp.modules.store.repository.StoreRepository;
import com.aiecomm.camp.modules.theme.dto.ThemeVersionDto;
import com.aiecomm.camp.modules.theme.entity.Theme;
import com.aiecomm.camp.modules.theme.entity.ThemeVersion;
import com.aiecomm.camp.modules.theme.entity.ThemeVersionStatus;
import com.aiecomm.camp.modules.theme.repository.ThemeRepository;
import com.aiecomm.camp.modules.theme.repository.ThemeVersionRepository;
import com.aiecomm.camp.modules.theme.service.ThemeService;
import com.aiecomm.camp.modules.theme.validation.ThemeConfigurationValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThemeServiceImpl implements ThemeService {

    private static final String DEFAULT_CONFIGURATION_JSON =
            "{\"version\":1,\"globalSettings\":{},\"sectionGroups\":{\"header\":{\"sections\":{},\"order\":[]},\"footer\":{\"sections\":{},\"order\":[]}},\"templates\":{}}";

    private final ThemeRepository themeRepository;
    private final ThemeVersionRepository themeVersionRepository;
    private final StoreRepository storeRepository;
    private final ThemeConfigurationValidator validator;
    private final ObjectMapper objectMapper;

    private Store resolveOwnedStore(Long storeId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new AccessDeniedException("Authenticated tenant context is required");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        if (store.getTenant() == null || !tenantId.equals(store.getTenant().getTenantId())) {
            throw new AccessDeniedException("Store does not belong to the authenticated tenant");
        }
        return store;
    }

    private Theme resolveOrCreateTheme(Store store) {
        return themeRepository.findByStore_StoreId(store.getStoreId())
                .orElseGet(() -> themeRepository.save(Theme.builder()
                        .tenant(store.getTenant())
                        .store(store)
                        .name("Default Theme")
                        .build()));
    }

    private ThemeVersion createDefaultDraft(Theme theme) {
        try {
            JsonNode defaultConfig = objectMapper.readTree(DEFAULT_CONFIGURATION_JSON);
            return themeVersionRepository.save(ThemeVersion.builder()
                    .theme(theme)
                    .version(1)
                    .configuration(defaultConfig)
                    .status(ThemeVersionStatus.DRAFT)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to seed default theme configuration", e);
        }
    }

    @Override
    @Transactional
    public ThemeVersionDto getDraftTheme(Long storeId) {
        Store store = resolveOwnedStore(storeId);
        Theme theme = resolveOrCreateTheme(store);

        ThemeVersion draft = themeVersionRepository
                .findByTheme_IdAndStatus(theme.getId(), ThemeVersionStatus.DRAFT)
                .orElseGet(() -> createDefaultDraft(theme));

        return ThemeVersionDto.fromEntity(draft);
    }

    @Override
    @Transactional
    public ThemeVersionDto updateDraftTheme(Long storeId, JsonNode configuration) {
        validator.validate(configuration);

        Store store = resolveOwnedStore(storeId);
        Theme theme = resolveOrCreateTheme(store);

        ThemeVersion draft = themeVersionRepository
                .findByTheme_IdAndStatus(theme.getId(), ThemeVersionStatus.DRAFT)
                .orElseGet(() -> createDefaultDraft(theme));

        draft.setConfiguration(configuration);
        return ThemeVersionDto.fromEntity(themeVersionRepository.save(draft));
    }

    @Override
    @Transactional
    public ThemeVersionDto publishTheme(Long storeId) {
        Store store = resolveOwnedStore(storeId);
        Theme theme = resolveOrCreateTheme(store);

        ThemeVersion draft = themeVersionRepository
                .findByTheme_IdAndStatus(theme.getId(), ThemeVersionStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("No draft theme to publish"));

        validator.validate(draft.getConfiguration());

        ThemeVersion published = themeVersionRepository
                .findByTheme_IdAndStatus(theme.getId(), ThemeVersionStatus.PUBLISHED)
                .orElseGet(() -> ThemeVersion.builder().theme(theme).status(ThemeVersionStatus.PUBLISHED).build());

        published.setConfiguration(draft.getConfiguration());
        published.setVersion(draft.getVersion());
        ThemeVersion saved = themeVersionRepository.save(published);

        log.info("Published theme for store {} (theme {}) at version {}", storeId, theme.getId(), saved.getVersion());
        return ThemeVersionDto.fromEntity(saved);
    }

    @Override
    @Transactional
    public ThemeVersionDto getPublishedTheme(Long storeId) {
        Store store = resolveOwnedStore(storeId);
        Theme theme = resolveOrCreateTheme(store);

        ThemeVersion published = themeVersionRepository
                .findByTheme_IdAndStatus(theme.getId(), ThemeVersionStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Theme has not been published yet"));

        return ThemeVersionDto.fromEntity(published);
    }
}

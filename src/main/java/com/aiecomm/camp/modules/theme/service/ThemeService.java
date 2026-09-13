package com.aiecomm.camp.modules.theme.service;

import com.aiecomm.camp.modules.theme.dto.ThemeVersionDto;
import com.fasterxml.jackson.databind.JsonNode;

public interface ThemeService {
    ThemeVersionDto getDraftTheme(Long storeId);
    ThemeVersionDto updateDraftTheme(Long storeId, JsonNode configuration);
    ThemeVersionDto publishTheme(Long storeId);
    ThemeVersionDto getPublishedTheme(Long storeId);
}

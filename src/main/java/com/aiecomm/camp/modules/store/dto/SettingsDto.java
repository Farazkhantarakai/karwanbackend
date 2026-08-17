package com.aiecomm.camp.modules.store.dto;

import com.aiecomm.camp.modules.store.entity.Settings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingsDto {

    private Long settingId;
    private Long storeId;
    private String countryCode;
    private String countryCurrency;
    private String countryLanguage;

    public static SettingsDto fromEntity(Settings entity) {
        if (entity == null) return null;
        return SettingsDto.builder()
                .settingId(entity.getSettingId())
                .storeId(entity.getStore() != null ? entity.getStore().getStoreId() : null)
                .countryCode(entity.getCountryCode())
                .countryCurrency(entity.getCountryCurrency())
                .countryLanguage(entity.getCountryLanguage())
                .build();
    }
}

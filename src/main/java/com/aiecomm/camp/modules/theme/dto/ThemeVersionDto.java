package com.aiecomm.camp.modules.theme.dto;

import com.aiecomm.camp.modules.theme.entity.ThemeVersion;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeVersionDto {
    private Long id;
    private Long themeId;
    private Integer version;
    private JsonNode configuration;
    private String status;
    private Instant createdAt;

    public static ThemeVersionDto fromEntity(ThemeVersion entity) {
        if (entity == null) return null;
        return ThemeVersionDto.builder()
                .id(entity.getId())
                .themeId(entity.getTheme() != null ? entity.getTheme().getId() : null)
                .version(entity.getVersion())
                .configuration(entity.getConfiguration())
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

package com.aiecomm.camp.modules.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageUsageDto {
    private Long usedBytes;
    private Long limitBytes;
    private Double usedPercentage;
    private String formattedUsed;
    private String formattedLimit;
}

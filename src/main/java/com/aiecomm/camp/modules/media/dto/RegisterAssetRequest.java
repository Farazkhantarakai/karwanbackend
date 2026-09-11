package com.aiecomm.camp.modules.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterAssetRequest {
    private Long storeId;
    private String objectKey;
    private String originalFilename;
    private String contentType;
}

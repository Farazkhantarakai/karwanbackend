package com.aiecomm.camp.modules.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadIntentResponse {
    private UUID mediaId;
    private String uploadUrl;
    private String url;
    private String objectKey;
    private long expiresIn;
}

package com.aiecomm.camp.modules.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {
    private Long storeId;
    private String productId;
    private String fileId;
    private String fileName;
    private String contentType;
}

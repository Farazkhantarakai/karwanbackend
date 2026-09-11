package com.aiecomm.camp.modules.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadIntentRequest {
    private Long storeId;
    private String fileName;
    private String contentType;
    private Long size;
}

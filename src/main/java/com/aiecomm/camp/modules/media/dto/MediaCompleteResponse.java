package com.aiecomm.camp.modules.media.dto;

import com.aiecomm.camp.modules.media.entity.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCompleteResponse {
    private UUID mediaId;
    private MediaStatus status;
    private String url;
    private Long size;
    private Integer width;
    private Integer height;
    private String contentType;
    private String originalFilename;
}

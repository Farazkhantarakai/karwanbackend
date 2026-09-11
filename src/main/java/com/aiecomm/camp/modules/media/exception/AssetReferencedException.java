package com.aiecomm.camp.modules.media.exception;

import com.aiecomm.camp.modules.media.dto.ConsumerRefDto;
import lombok.Getter;

import java.util.List;

@Getter
public class AssetReferencedException extends RuntimeException {
    private final Long assetId;
    private final List<ConsumerRefDto> references;

    public AssetReferencedException(Long assetId, List<ConsumerRefDto> references) {
        super(String.format("Media asset %d cannot be deleted because it is still referenced by %d consumer(s)",
                assetId, references.size()));
        this.assetId = assetId;
        this.references = references;
    }
}

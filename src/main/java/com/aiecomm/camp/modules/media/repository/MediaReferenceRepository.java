package com.aiecomm.camp.modules.media.repository;

import com.aiecomm.camp.modules.media.entity.MediaReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaReferenceRepository extends JpaRepository<MediaReference, Long> {

    long countByMediaAsset_Id(Long mediaAssetId);

    List<MediaReference> findByMediaAsset_Id(Long mediaAssetId);

    void deleteByReferenceableTypeAndReferenceableId(String referenceableType, Long referenceableId);

    boolean existsByMediaAsset_IdAndReferenceableTypeAndReferenceableId(Long mediaAssetId, String referenceableType, Long referenceableId);
}

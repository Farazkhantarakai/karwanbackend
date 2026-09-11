package com.aiecomm.camp.modules.media.repository;

import com.aiecomm.camp.modules.media.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProduct_ProductIdOrderBySortOrderAsc(Long productId);

    void deleteByProduct_ProductId(Long productId);

    long countByMediaAsset_Id(Long mediaAssetId);
}

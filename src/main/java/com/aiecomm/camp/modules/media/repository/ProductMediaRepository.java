package com.aiecomm.camp.modules.media.repository;

import com.aiecomm.camp.modules.media.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProduct_ProductIdOrderBySortOrderAsc(Long productId);

    void deleteByProduct_ProductId(Long productId);

    boolean existsByMedia_Id(UUID mediaId);
}

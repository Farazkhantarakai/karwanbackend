package com.aiecomm.camp.modules.commerce.cart;

import com.aiecomm.camp.modules.product.entity.Product;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds the display-only product snapshot stored in cart_items.product_snapshot.
 *
 * Snapshot contains: productId, title, sku, imageUrl.
 * Price is intentionally EXCLUDED — unit_price is the authoritative commerce value.
 *
 * When the Variant domain is added, extend this to include variantTitle, variantSku, etc.
 */
public final class ProductSnapshotService {

    private ProductSnapshotService() {}

    public static Map<String, Object> buildSnapshot(Product product) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("productId", product.getProductId());
        snapshot.put("title",     Objects.toString(product.getProductName(), ""));
        snapshot.put("sku",       Objects.toString(product.getSku(), ""));
        snapshot.put("imageUrl",  Objects.toString(product.getImageUrl(), ""));
        // price intentionally excluded — stored as CartItem.unitPrice, not in snapshot
        return snapshot;
    }
}

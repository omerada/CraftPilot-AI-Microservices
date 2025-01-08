package com.craftpilot.productservice.service;

import com.craftpilot.productservice.model.product.Product;
import com.craftpilot.productservice.model.product.dto.request.ProductUpdateRequest;

/**
 * Service interface named {@link ProductUpdateService} for updating products.
 */
public interface ProductUpdateService {

    /**
     * Updates a product identified by its unique ID using the provided update request.
     *
     * @param productId           The ID of the product to update.
     * @param productUpdateRequest The request containing updated data for the product.
     * @return The updated Product object.
     */
    Product updateProductById(final String productId, final ProductUpdateRequest productUpdateRequest);

}

package com.craftpilot.productservice.service.impl;

import com.craftpilot.productservice.exception.ProductAlreadyExistException;
import com.craftpilot.productservice.exception.ProductNotFoundException;
import com.craftpilot.productservice.model.product.Product;
import com.craftpilot.productservice.model.product.dto.request.ProductUpdateRequest;
import com.craftpilot.productservice.model.product.entity.ProductEntity;
import com.craftpilot.productservice.model.product.mapper.ProductEntityToProductMapper;
import com.craftpilot.productservice.model.product.mapper.ProductUpdateRequestToProductEntityMapper;
import com.craftpilot.productservice.repository.ProductRepository;
import com.craftpilot.productservice.service.ProductUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation named {@link ProductUpdateServiceImpl} for updating products.
 */
@Service
@RequiredArgsConstructor
public class ProductUpdateServiceImpl implements ProductUpdateService {

    private final ProductRepository productRepository;

    private final ProductUpdateRequestToProductEntityMapper productUpdateRequestToProductEntityMapper =
            ProductUpdateRequestToProductEntityMapper.initialize();

    private final ProductEntityToProductMapper productEntityToProductMapper =
            ProductEntityToProductMapper.initialize();

    /**
     * Updates a product identified by its unique ID using the provided update request.
     *
     * @param productId           The ID of the product to update.
     * @param productUpdateRequest The request containing updated data for the product.
     * @return The updated Product object.
     * @throws ProductNotFoundException If no product with the given ID exists.
     * @throws ProductAlreadyExistException If another product with the updated name already exists.
     */
    @Override
    public Product updateProductById(String productId, ProductUpdateRequest productUpdateRequest) {

        checkProductNameUniqueness(productUpdateRequest.getName());

        final ProductEntity productEntityToBeUpdate = productRepository
                .findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("With given productID = " + productId));

        productUpdateRequestToProductEntityMapper.mapForUpdating(productEntityToBeUpdate, productUpdateRequest);

        ProductEntity updatedProductEntity = productRepository.save(productEntityToBeUpdate);

        return productEntityToProductMapper.map(updatedProductEntity);

    }

    /**
     * Checks if a product with the updated name already exists in the repository.
     *
     * @param productName The updated name of the product to check.
     * @throws ProductAlreadyExistException If another product with the updated name already exists.
     */
    private void checkProductNameUniqueness(final String productName) {
        if (productRepository.existsProductEntityByName(productName)) {
            throw new ProductAlreadyExistException("With given product name = " + productName);
        }
    }

}

package com.craftpilot.productservice.service.impl;


import com.craftpilot.productservice.exception.ProductAlreadyExistException;
import com.craftpilot.productservice.model.product.Product;
import com.craftpilot.productservice.model.product.dto.request.ProductCreateRequest;
import com.craftpilot.productservice.model.product.entity.ProductEntity;
import com.craftpilot.productservice.model.product.mapper.ProductCreateRequestToProductEntityMapper;
import com.craftpilot.productservice.model.product.mapper.ProductEntityToProductMapper;
import com.craftpilot.productservice.repository.ProductRepository;
import com.craftpilot.productservice.service.ProductCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation named {@link ProductCreateServiceImpl} for creating products.
 */
@Service
@RequiredArgsConstructor
public class ProductCreateServiceImpl implements ProductCreateService {

    private final ProductRepository productRepository;

    private final ProductCreateRequestToProductEntityMapper productCreateRequestToProductEntityMapper =
            ProductCreateRequestToProductEntityMapper.initialize();

    private final ProductEntityToProductMapper productEntityToProductMapper = ProductEntityToProductMapper.initialize();

    /**
     * Creates a new product based on the provided product creation request.
     *
     * @param productCreateRequest The request containing data to create the product.
     * @return The created Product object.
     * @throws ProductAlreadyExistException If a product with the same name already exists.
     */
    @Override
    public Product createProduct(ProductCreateRequest productCreateRequest) {

        checkUniquenessProductName(productCreateRequest.getName());

        final ProductEntity productEntityToBeSave = productCreateRequestToProductEntityMapper.mapForSaving(productCreateRequest);

        ProductEntity savedProductEntity = productRepository.save(productEntityToBeSave);

        return productEntityToProductMapper.map(savedProductEntity);

    }

    /**
     * Checks if a product with the given name already exists in the repository.
     *
     * @param productName The name of the product to check.
     * @throws ProductAlreadyExistException If a product with the same name already exists.
     */
    private void checkUniquenessProductName(final String productName) {
        if (productRepository.existsProductEntityByName(productName)) {
            throw new ProductAlreadyExistException("There is another product with given name: " + productName);
        }
    }

}

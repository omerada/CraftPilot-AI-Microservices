package com.craftpilot.productservice.model.product.dto.request;

import com.craftpilot.productservice.model.common.dto.request.CustomPagingRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a paging request object for retrieving products as {@link ProductPagingRequest}.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class ProductPagingRequest extends CustomPagingRequest {


}

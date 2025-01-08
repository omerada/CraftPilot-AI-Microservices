package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface named {@link PubscriptionCreateRequestToPubscriptionEntityMapper} for converting {@link PubscriptionCreateRequest} to {@link PubscriptionEntity}.
 */
@Mapper
public interface PubscriptionCreateRequestToPubscriptionEntityMapper extends BaseMapper<PubscriptionCreateRequest, PubscriptionEntity> {

    /**
     * Maps PubscriptionCreateRequest to PubscriptionEntity for saving purposes.
     *
     * @param subscriptionCreateRequest The PubscriptionCreateRequest object to map.
     * @return PubscriptionEntity object containing mapped data.
     */
    @Named("mapForSaving")
    default PubscriptionEntity mapForSaving(PubscriptionCreateRequest subscriptionCreateRequest) {
        return PubscriptionEntity.builder()
                .amount(subscriptionCreateRequest.getAmount())
                .name(subscriptionCreateRequest.getName())
                .unitPrice(subscriptionCreateRequest.getUnitPrice())
                .build();
    }

    /**
     * Initializes and returns an instance of PubscriptionCreateRequestToPubscriptionEntityMapper.
     *
     * @return Initialized PubscriptionCreateRequestToPubscriptionEntityMapper instance.
     */
    static PubscriptionCreateRequestToPubscriptionEntityMapper initialize() {
        return Mappers.getMapper(PubscriptionCreateRequestToPubscriptionEntityMapper.class);
    }

}

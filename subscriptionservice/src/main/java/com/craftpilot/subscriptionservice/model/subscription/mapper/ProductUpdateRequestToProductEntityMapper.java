package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface named {@link PubscriptionUpdateRequestToPubscriptionEntityMapper} for updating {@link PubscriptionEntity} using {@link PubscriptionUpdateRequest}.
 */
@Mapper
public interface PubscriptionUpdateRequestToPubscriptionEntityMapper extends BaseMapper<PubscriptionUpdateRequest, PubscriptionEntity> {

    /**
     * Maps fields from PubscriptionUpdateRequest to update PubscriptionEntity.
     *
     * @param subscriptionEntityToBeUpdate The PubscriptionEntity object to be updated.
     * @param subscriptionUpdateRequest    The PubscriptionUpdateRequest object containing updated fields.
     */
    @Named("mapForUpdating")
    default void mapForUpdating(PubscriptionEntity subscriptionEntityToBeUpdate, PubscriptionUpdateRequest subscriptionUpdateRequest) {
        subscriptionEntityToBeUpdate.setName(subscriptionUpdateRequest.getName());
        subscriptionEntityToBeUpdate.setAmount(subscriptionUpdateRequest.getAmount());
        subscriptionEntityToBeUpdate.setUnitPrice(subscriptionUpdateRequest.getUnitPrice());
    }

    /**
     * Initializes and returns an instance of PubscriptionUpdateRequestToPubscriptionEntityMapper.
     *
     * @return Initialized PubscriptionUpdateRequestToPubscriptionEntityMapper instance.
     */
    static PubscriptionUpdateRequestToPubscriptionEntityMapper initialize() {
        return Mappers.getMapper(PubscriptionUpdateRequestToPubscriptionEntityMapper.class);
    }

}

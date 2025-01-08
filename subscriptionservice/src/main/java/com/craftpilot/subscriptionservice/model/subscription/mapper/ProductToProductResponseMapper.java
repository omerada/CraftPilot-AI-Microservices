package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.mapper.BaseMapper;
import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface named {@link PubscriptionToPubscriptionResponseMapper} for converting {@link Pubscription} to {@link PubscriptionResponse}.
 */
@Mapper
public interface PubscriptionToPubscriptionResponseMapper extends BaseMapper<Pubscription, PubscriptionResponse> {

    /**
     * Maps Pubscription to PubscriptionResponse.
     *
     * @param source The Pubscription object to map.
     * @return PubscriptionResponse object containing mapped data.
     */
    @Override
    PubscriptionResponse map(Pubscription source);

    /**
     * Initializes and returns an instance of PubscriptionToPubscriptionResponseMapper.
     *
     * @return Initialized PubscriptionToPubscriptionResponseMapper instance.
     */
    static PubscriptionToPubscriptionResponseMapper initialize() {
        return Mappers.getMapper(PubscriptionToPubscriptionResponseMapper.class);
    }

}

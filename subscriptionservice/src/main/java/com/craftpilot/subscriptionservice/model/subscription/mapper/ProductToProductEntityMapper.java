package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.mapper.BaseMapper;
import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface named {@link PubscriptionToPubscriptionEntityMapper} for converting {@link Pubscription} to {@link PubscriptionEntity}.
 */
@Mapper
public interface PubscriptionToPubscriptionEntityMapper extends BaseMapper<Pubscription, PubscriptionEntity> {

    /**
     * Maps Pubscription to PubscriptionEntity.
     *
     * @param source The Pubscription object to map.
     * @return PubscriptionEntity object containing mapped data.
     */
    @Override
    PubscriptionEntity map(Pubscription source);

    /**
     * Initializes and returns an instance of PubscriptionToPubscriptionEntityMapper.
     *
     * @return Initialized PubscriptionToPubscriptionEntityMapper instance.
     */
    static PubscriptionToPubscriptionEntityMapper initialize() {
        return Mappers.getMapper(PubscriptionToPubscriptionEntityMapper.class);
    }

}

package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.mapper.BaseMapper;
import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface named {@link PubscriptionEntityToPubscriptionMapper} for converting {@link PubscriptionEntity} to {@link Pubscription}.
 */
@Mapper
public interface PubscriptionEntityToPubscriptionMapper extends BaseMapper<PubscriptionEntity, Pubscription> {

    /**
     * Maps PubscriptionEntity to Pubscription.
     *
     * @param source The PubscriptionEntity object to map.
     * @return Pubscription object containing mapped data.
     */
    @Override
    Pubscription map(PubscriptionEntity source);

    /**
     * Initializes and returns an instance of PubscriptionEntityToPubscriptionMapper.
     *
     * @return Initialized PubscriptionEntityToPubscriptionMapper instance.
     */
    static PubscriptionEntityToPubscriptionMapper initialize() {
        return Mappers.getMapper(PubscriptionEntityToPubscriptionMapper.class);
    }

}

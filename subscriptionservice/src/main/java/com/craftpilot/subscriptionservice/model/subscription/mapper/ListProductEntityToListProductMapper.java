package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface named {@link ListPubscriptionEntityToListPubscriptionMapper} for converting {@link List<PubscriptionEntity>} to {@link List<Pubscription>}.
 */
@Mapper
public interface ListPubscriptionEntityToListPubscriptionMapper {

    PubscriptionEntityToPubscriptionMapper subscriptionEntityToPubscriptionMapper = Mappers.getMapper(PubscriptionEntityToPubscriptionMapper.class);

    /**
     * Converts a list of PubscriptionEntity objects to a list of Pubscription objects.
     *
     * @param subscriptionEntities The list of PubscriptionEntity objects to convert.
     * @return List of Pubscription objects containing mapped data.
     */
    default List<Pubscription> toPubscriptionList(List<PubscriptionEntity> subscriptionEntities) {

        if (subscriptionEntities == null) {
            return null;
        }

        return subscriptionEntities.stream()
                .map(subscriptionEntityToPubscriptionMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of ListPubscriptionEntityToListPubscriptionMapper.
     *
     * @return Initialized ListPubscriptionEntityToListPubscriptionMapper instance.
     */
    static ListPubscriptionEntityToListPubscriptionMapper initialize() {
        return Mappers.getMapper(ListPubscriptionEntityToListPubscriptionMapper.class);
    }

}

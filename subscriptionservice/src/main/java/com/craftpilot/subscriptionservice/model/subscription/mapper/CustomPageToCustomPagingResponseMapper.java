package com.craftpilot.subscriptionservice.model.subscription.mapper;

import com.craftpilot.subscriptionservice.model.common.CustomPage;
import com.craftpilot.subscriptionservice.model.common.dto.response.CustomPagingResponse;
import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface named {@link CustomPageToCustomPagingResponseMapper} for converting {@link CustomPage<Pubscription>} to {@link CustomPagingResponse<PubscriptionResponse>}.
 */
@Mapper
public interface CustomPageToCustomPagingResponseMapper {

    PubscriptionToPubscriptionResponseMapper subscriptionToPubscriptionResponseMapper = Mappers.getMapper(PubscriptionToPubscriptionResponseMapper.class);

    /**
     * Converts a CustomPage<Pubscription> object to CustomPagingResponse<PubscriptionResponse>.
     *
     * @param subscriptionPage The CustomPage<Pubscription> object to convert.
     * @return CustomPagingResponse<PubscriptionResponse> object containing mapped data.
     */
    default CustomPagingResponse<PubscriptionResponse> toPagingResponse(CustomPage<Pubscription> subscriptionPage) {

        if (subscriptionPage == null) {
            return null;
        }

        return CustomPagingResponse.<PubscriptionResponse>builder()
                .content(toPubscriptionResponseList(subscriptionPage.getContent()))
                .totalElementCount(subscriptionPage.getTotalElementCount())
                .totalPageCount(subscriptionPage.getTotalPageCount())
                .pageNumber(subscriptionPage.getPageNumber())
                .pageSize(subscriptionPage.getPageSize())
                .build();

    }

    /**
     * Converts a list of Pubscription objects to a list of PubscriptionResponse objects.
     *
     * @param subscriptions The list of Pubscription objects to convert.
     * @return List of PubscriptionResponse objects containing mapped data.
     */
    default List<PubscriptionResponse> toPubscriptionResponseList(List<Pubscription> subscriptions) {

        if (subscriptions == null) {
            return null;
        }

        return subscriptions.stream()
                .map(subscriptionToPubscriptionResponseMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of CustomPageToCustomPagingResponseMapper.
     *
     * @return Initialized CustomPageToCustomPagingResponseMapper instance.
     */
    static CustomPageToCustomPagingResponseMapper initialize() {
        return Mappers.getMapper(CustomPageToCustomPagingResponseMapper.class);
    }

}

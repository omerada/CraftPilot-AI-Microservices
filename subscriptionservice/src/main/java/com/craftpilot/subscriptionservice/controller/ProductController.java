package com.craftpilot.subscriptionservice.controller;

import com.craftpilot.subscriptionservice.model.common.CustomPage;
import com.craftpilot.subscriptionservice.model.common.dto.response.CustomPagingResponse;
import com.craftpilot.subscriptionservice.model.common.dto.response.CustomResponse;
import com.craftpilot.subscriptionservice.model.subscription.Pubscription;
import com.craftpilot.subscriptionservice.model.subscription.mapper.CustomPageToCustomPagingResponseMapper;
import com.craftpilot.subscriptionservice.model.subscription.mapper.PubscriptionToPubscriptionResponseMapper;
import com.craftpilot.subscriptionservice.service.PubscriptionCreateService;
import com.craftpilot.subscriptionservice.service.PubscriptionDeleteService;
import com.craftpilot.subscriptionservice.service.PubscriptionReadService;
import com.craftpilot.subscriptionservice.service.PubscriptionUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller named {@link PubscriptionController} for managing subscriptions.
 * Provides endpoints to create, read, update, and delete subscriptions.
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Validated
public class PubscriptionController {

    private final PubscriptionCreateService subscriptionCreateService;
    private final PubscriptionReadService subscriptionReadService;
    private final PubscriptionUpdateService subscriptionUpdateService;
    private final PubscriptionDeleteService subscriptionDeleteService;

    private final PubscriptionToPubscriptionResponseMapper subscriptionToPubscriptionResponseMapper = PubscriptionToPubscriptionResponseMapper.initialize();

    private final CustomPageToCustomPagingResponseMapper customPageToCustomPagingResponseMapper =
            CustomPageToCustomPagingResponseMapper.initialize();

    /**
     * Creates a new subscription.
     *
     * @param subscriptionCreateRequest the request payload containing subscription details
     * @return a {@link CustomResponse} containing the ID of the created subscription
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public CustomResponse<String> createPubscription(@RequestBody @Valid final PubscriptionCreateRequest subscriptionCreateRequest) {

        final Pubscription createdPubscription = subscriptionCreateService
                .createPubscription(subscriptionCreateRequest);

        return CustomResponse.successOf(createdPubscription.getId());
    }

    /**
     * Retrieves a subscription by its ID.
     *
     * @param subscriptionId the ID of the subscription to retrieve
     * @return a {@link CustomResponse} containing the subscription details
     */
    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public CustomResponse<PubscriptionResponse> getPubscriptionById(@PathVariable @UUID final String subscriptionId) {

        final Pubscription subscription = subscriptionReadService.getPubscriptionById(subscriptionId);

        final PubscriptionResponse subscriptionResponse = subscriptionToPubscriptionResponseMapper.map(subscription);

        return CustomResponse.successOf(subscriptionResponse);

    }

    /**
     * Retrieves a paginated list of subscriptions based on the paging request.
     *
     * @param subscriptionPagingRequest the request payload containing paging information
     * @return a {@link CustomResponse} containing the paginated list of subscriptions
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'USER')")
    public CustomResponse<CustomPagingResponse<PubscriptionResponse>> getPubscriptions(
            @RequestBody @Valid final PubscriptionPagingRequest subscriptionPagingRequest) {

        final CustomPage<Pubscription> subscriptionPage = subscriptionReadService.getPubscriptions(subscriptionPagingRequest);

        final CustomPagingResponse<PubscriptionResponse> subscriptionPagingResponse =
                customPageToCustomPagingResponseMapper.toPagingResponse(subscriptionPage);

        return CustomResponse.successOf(subscriptionPagingResponse);

    }

    /**
     * Updates an existing subscription by its ID.
     *
     * @param subscriptionUpdateRequest the request payload containing updated subscription details
     * @param subscriptionId the ID of the subscription to update
     * @return a {@link CustomResponse} containing the updated subscription details
     */
    @PutMapping("/{subscriptionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public CustomResponse<PubscriptionResponse> updatedPubscriptionById(
            @RequestBody @Valid final PubscriptionUpdateRequest subscriptionUpdateRequest,
            @PathVariable @UUID final String subscriptionId) {

        final Pubscription updatedPubscription = subscriptionUpdateService.updatePubscriptionById(subscriptionId, subscriptionUpdateRequest);

        final PubscriptionResponse subscriptionResponse = subscriptionToPubscriptionResponseMapper.map(updatedPubscription);

        return CustomResponse.successOf(subscriptionResponse);
    }

    /**
     * Deletes a subscription by its ID.
     *
     * @param subscriptionId the ID of the subscription to delete
     * @return a {@link CustomResponse} indicating successful deletion
     */
    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public CustomResponse<Void> deletePubscriptionById(@PathVariable @UUID final String subscriptionId) {

        subscriptionDeleteService.deletePubscriptionById(subscriptionId);
        return CustomResponse.SUCCESS;
    }

}

package com.craftpilot.subscriptionservice.service.impl;
 
import com.craftpilot.subscriptionservice.model.payment.PaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoPaymentResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.IyzicoRefundResult;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreatePaymentRequest;
import com.craftpilot.subscriptionservice.model.payment.iyzico.request.CreateRefundRequest;
import com.craftpilot.subscriptionservice.service.IyzicoService;
import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IyzicoServiceImpl implements IyzicoService {
 
    private final Options options;

    @Override
    @CircuitBreaker(name = "iyzico-service")
    public Mono<String> createPaymentLink(PaymentRequest request) {
        return Mono.fromCallable(() -> {
            try {
                CreateCheckoutFormInitializeRequest initializeRequest = new CreateCheckoutFormInitializeRequest();
                initializeRequest.setLocale("tr");
                initializeRequest.setConversationId(request.getUserId());
                initializeRequest.setPrice(new BigDecimal(String.valueOf(request.getAmount())));
                initializeRequest.setPaidPrice(new BigDecimal(String.valueOf(request.getAmount())));
                initializeRequest.setCurrency(Currency.TRY.name());
                initializeRequest.setBasketId(request.getPlanId());
                initializeRequest.setPaymentGroup(PaymentGroup.PRODUCT.name());
                initializeRequest.setCallbackUrl(request.getCallbackUrl());
                initializeRequest.setEnabledInstallments(new ArrayList<>(List.of(1)));

                Buyer buyer = new Buyer();
                buyer.setId(request.getUserId());
                buyer.setName("John");
                buyer.setSurname("Doe");
                buyer.setEmail("email@email.com");
                buyer.setIdentityNumber("74300864791");
                buyer.setRegistrationAddress("Nidakule Göztepe, Merdivenköy Mah. Bora Sok. No:1");
                buyer.setCity("Istanbul");
                buyer.setCountry("Turkey");
                initializeRequest.setBuyer(buyer);

                Address shippingAddress = new Address();
                shippingAddress.setContactName("John Doe");
                shippingAddress.setCity("Istanbul");
                shippingAddress.setCountry("Turkey");
                shippingAddress.setAddress("Nidakule Göztepe, Merdivenköy Mah. Bora Sok. No:1");
                initializeRequest.setShippingAddress(shippingAddress);

                Address billingAddress = new Address();
                billingAddress.setContactName("John Doe");
                billingAddress.setCity("Istanbul");
                billingAddress.setCountry("Turkey");
                billingAddress.setAddress("Nidakule Göztepe, Merdivenköy Mah. Bora Sok. No:1");
                initializeRequest.setBillingAddress(billingAddress);

                List<BasketItem> basketItems = new ArrayList<>();
                BasketItem basketItem = new BasketItem();
                basketItem.setId(request.getPlanId());
                basketItem.setName(request.getDescription());
                basketItem.setCategory1("Subscription");
                basketItem.setItemType(BasketItemType.VIRTUAL.name());
                basketItem.setPrice(new BigDecimal(String.valueOf(request.getAmount())));
                basketItems.add(basketItem);
                initializeRequest.setBasketItems(basketItems);

                CheckoutFormInitialize checkoutFormInitialize = CheckoutFormInitialize.create(initializeRequest, options);

                if (checkoutFormInitialize.getStatus().equals("success")) {
                    return checkoutFormInitialize.getPaymentPageUrl();
                } else {
                    throw new RuntimeException("Ödeme başlatma işlemi başarısız: " + checkoutFormInitialize.getErrorMessage());
                }
            } catch (Exception e) {
                log.error("Ödeme başlatma işlemi sırasında hata oluştu", e);
                throw new RuntimeException("Ödeme başlatma işlemi sırasında hata oluştu", e);
            }
        });
    }

    @Override
    @CircuitBreaker(name = "iyzico-service")
    public Mono<Void> handlePaymentCallback(String token) {
        return Mono.fromCallable(() -> {
            try {
                RetrieveCheckoutFormRequest request = new RetrieveCheckoutFormRequest();
                request.setToken(token);
                request.setLocale("tr");
                
                CheckoutForm checkoutForm = CheckoutForm.retrieve(request, options);
                
                if (!"success".equals(checkoutForm.getStatus())) {
                    throw new RuntimeException("Ödeme doğrulama başarısız: " + checkoutForm.getErrorMessage());
                }
                
                // TODO: Ödeme başarılı olduğunda gerekli işlemleri yap
                // - Subscription kaydını güncelle
                // - Kullanıcıya bildirim gönder
                // - Diğer gerekli işlemler
                
                return null;
            } catch (Exception e) {
                log.error("Ödeme callback işlemi sırasında hata oluştu", e);
                throw new RuntimeException("Ödeme callback işlemi sırasında hata oluştu", e);
            }
        }).then();
    }

    @Override
    public Mono<IyzicoPaymentResult> createPayment(CreatePaymentRequest request) {
        log.info("Creating payment with Iyzico: {}", request);
        return Mono.fromCallable(() -> {
            // Implement Iyzico payment creation logic
            return IyzicoPaymentResult.builder()
                    .status("success")
                    .paymentTransactionId("test-transaction-id")
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<IyzicoRefundResult> createRefund(CreateRefundRequest request) {
        log.info("Creating refund with Iyzico: {}", request);
        return Mono.fromCallable(() -> {
            // Implement Iyzico refund logic
            return IyzicoRefundResult.builder()
                    .status("success")
                    .refundTransactionId("test-refund-id")
                    .build();
        }).subscribeOn(Schedulers.boundedElastic());
    }
} 
package com.craftpilot.subscriptionservice.service.impl;

import com.craftpilot.subscriptionservice.model.subscription.dto.SubscriptionPlan;
import com.craftpilot.subscriptionservice.model.subscription.entity.SubscriptionPlanEntity;
import com.craftpilot.subscriptionservice.repository.SubscriptionPlanRepository;
import com.craftpilot.subscriptionservice.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    public SubscriptionPlan createSubscriptionPlan(SubscriptionPlan subscriptionPlanDTO) {
        // DTO'dan Entity'ye dönüşüm
        SubscriptionPlanEntity planEntity = new SubscriptionPlanEntity();
        planEntity.setName(subscriptionPlanDTO.getName());
        planEntity.setPrice(subscriptionPlanDTO.getPrice());
        planEntity.setDurationInMonths(subscriptionPlanDTO.getDurationInMonths());
        planEntity.setStatus(subscriptionPlanDTO.getStatus());

        // Abonelik planını veritabanına kaydet
        SubscriptionPlanEntity savedPlan = subscriptionPlanRepository.save(planEntity);

        // Entity'den DTO'ya dönüşüm ve geri döndürme
        return new SubscriptionPlan(
                savedPlan.getId(),
                savedPlan.getName(),
                savedPlan.getPrice(),
                savedPlan.getDurationInMonths(),
                savedPlan.getStatus()
        );
    }

    @Override
    public SubscriptionPlan getSubscriptionPlanById(Long id) {
        // Abonelik planını ID ile veritabanından al
        SubscriptionPlanEntity planEntity = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Abonelik planı bulunamadı"));

        // Entity'den DTO'ya dönüşüm
        return new SubscriptionPlan(
                planEntity.getId(),
                planEntity.getName(),
                planEntity.getPrice(),
                planEntity.getDurationInMonths(),
                planEntity.getStatus()
        );
    }

    @Override
    public SubscriptionPlan updateSubscriptionPlan(Long id, SubscriptionPlan subscriptionPlanDTO) {
        // Abonelik planını güncelle
        SubscriptionPlanEntity planEntity = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Abonelik planı bulunamadı"));

        planEntity.setName(subscriptionPlanDTO.getName());
        planEntity.setPrice(subscriptionPlanDTO.getPrice());
        planEntity.setDurationInMonths(subscriptionPlanDTO.getDurationInMonths());
        planEntity.setStatus(subscriptionPlanDTO.getStatus());

        // Güncellenmiş abonelik planını veritabanına kaydet
        SubscriptionPlanEntity updatedPlan = subscriptionPlanRepository.save(planEntity);

        // Entity'den DTO'ya dönüşüm
        return new SubscriptionPlan(
                updatedPlan.getId(),
                updatedPlan.getName(),
                updatedPlan.getPrice(),
                updatedPlan.getDurationInMonths(),
                updatedPlan.getStatus()
        );
    }

    @Override
    public void deleteSubscriptionPlan(Long id) {
        // Abonelik planını sil
        SubscriptionPlanEntity planEntity = subscriptionPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Abonelik planı bulunamadı"));

        subscriptionPlanRepository.delete(planEntity);
    }

    @Override
    public List<SubscriptionPlan> getAllSubscriptionPlans() {
        // Tüm abonelik planlarını al
        List<SubscriptionPlanEntity> planEntities = subscriptionPlanRepository.findAll();

        // Entity'lerden DTO'lara dönüşüm
        return planEntities.stream()
                .map(planEntity -> new SubscriptionPlan(
                        planEntity.getId(),
                        planEntity.getName(),
                        planEntity.getPrice(),
                        planEntity.getDurationInMonths(),
                        planEntity.getStatus()
                ))
                .collect(Collectors.toList());
    }
}

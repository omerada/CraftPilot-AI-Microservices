package com.craftpilot.userservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@LoadBalancerClients(defaultConfiguration = {LoadBalancerConfig.class})
public class LoadBalancerConfig {
    // Default configuration will use Caffeine cache
}

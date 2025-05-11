package com.craftpilot.analyticsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;

@Configuration
public class RepositoryConfig {

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validatorFactoryBean) {
        return new ValidatingMongoEventListener(validatorFactoryBean);
    }

    @Primary
    @Bean(name = "mongoValidator")
    public LocalValidatorFactoryBean mongoValidator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
            // Özel dönüştürücüler burada tanımlanabilir
        ));
    }
}

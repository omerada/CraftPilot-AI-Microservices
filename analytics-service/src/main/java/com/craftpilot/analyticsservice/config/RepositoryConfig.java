package com.craftpilot.analyticsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

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
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        // Expression Language olmadan çalışabilen interpolator kullan
        validator.setMessageInterpolator(new ParameterMessageInterpolator());
        return validator;
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(Arrays.asList(
            // Özel dönüştürücüler burada tanımlanabilir
        ));
    }
}

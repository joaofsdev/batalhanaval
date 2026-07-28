package com.softexpert.batalhanaval_api.config.observability;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability configuration enabling @Timed annotation support
 * and any additional metric customizations.
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
public class ObservabilityConfig {

    /**
     * Enables the use of @Timed annotation on methods to automatically
     * record their execution time as Micrometer timer metrics.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}

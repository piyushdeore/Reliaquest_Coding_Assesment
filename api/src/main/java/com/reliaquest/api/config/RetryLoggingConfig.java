package com.reliaquest.api.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RetryLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryLoggingConfig.class);

    private final RetryRegistry retryRegistry;

    public RetryLoggingConfig(RetryRegistry retryRegistry) {
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    public void registerRetryListener() {
        Retry retry = retryRegistry.retry("employee-api");

        retry.getEventPublisher()
                .onRetry(event -> log.warn("Retry attempt #{} for '{}'. Last error: {}",
                        event.getNumberOfRetryAttempts(),
                        event.getName(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "None"));

        retry.getEventPublisher()
                .onError(event -> log.error("All retry attempts failed for '{}'. Final error: {}",
                        event.getName(),
                        event.getLastThrowable() != null ? event.getLastThrowable().getMessage() : "None"));
    }
}

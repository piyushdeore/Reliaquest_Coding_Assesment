package com.reliaquest.api.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.core.IntervalFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Configuration
@Slf4j
public class RetryConfig {

    @Value("${employee.api.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${employee.api.retry.initial-interval-seconds:2}")
    private long initialInterval;

    @Value("${employee.api.retry.jitter:0.5}")
    private double jitter;

    @Value("${employee.api.retry.max-interval-seconds:30}")
    private long maxInterval;

    /**
     * Centralized Resilience4j Retry bean for RestTemplate calls.
     * Supports exponential backoff with jitter.
     */
    @Bean
    public Retry employeeApiRetry() {

        // Interval function: exponential backoff
        IntervalFunction intervalFn = IntervalFunction.ofExponentialBackoff(
                Duration.ofSeconds(initialInterval).toMillis(),
                2.0 // multiplier
        );

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFn)
                .retryOnException(isRetryableException())
                .failAfterMaxAttempts(true)
                .build();

        return Retry.of("employeeApiRetry", (Supplier<io.github.resilience4j.retry.RetryConfig>) config);
    }

    /**
     * Predicate to determine if an exception should trigger a retry.
     */
    private Predicate<Throwable> isRetryableException() {
        return throwable -> {
            // Retry on 5xx errors
            if (throwable instanceof HttpServerErrorException) return true;

            // Retry on 429 Too Many Requests
            if (throwable instanceof org.springframework.web.client.HttpClientErrorException.TooManyRequests) return true;

            // Retry on network-level exceptions
            if (throwable instanceof IOException) return true;

            return false;
        };
    }
}

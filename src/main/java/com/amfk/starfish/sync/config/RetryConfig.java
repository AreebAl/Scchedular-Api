package com.amfk.starfish.sync.config;

import org.apache.hc.core5.http.TruncatedChunkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RetryConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryConfig.class);
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Create a composite retry policy
        CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
        
        // Simple retry policy for specific exceptions
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpServerErrorException.class, true);
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(RestClientException.class, true);
        retryableExceptions.put(TruncatedChunkException.class, true);
        
        SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(5, retryableExceptions);
        
        // Timeout retry policy
        TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
        timeoutRetryPolicy.setTimeout(600000); // 10 minutes total timeout for large datasets
        
        compositeRetryPolicy.setPolicies(new RetryPolicy[]{simpleRetryPolicy, timeoutRetryPolicy});
        retryTemplate.setRetryPolicy(compositeRetryPolicy);
        
        // Exponential backoff policy
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000); // 1 second
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000); // 10 seconds max
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Custom retry listener for logging
        retryTemplate.setListeners(new org.springframework.retry.RetryListener[]{
            new org.springframework.retry.RetryListener() {
                @Override
                public <T, E extends Throwable> void onError(org.springframework.retry.RetryContext context, 
                                                           org.springframework.retry.RetryCallback<T, E> callback, 
                                                           Throwable throwable) {
                    if (isTruncatedChunkException(throwable)) {
                        logger.warn("Truncated chunk error detected, retrying... (attempt {})", 
                                  context.getRetryCount() + 1);
                    } else {
                        logger.warn("Retryable error detected, retrying... (attempt {})", 
                                  context.getRetryCount() + 1);
                    }
                }
            }
        });
        
        return retryTemplate;
    }
    
    /**
     * Checks if the given throwable is caused by a TruncatedChunkException
     */
    private boolean isTruncatedChunkException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof TruncatedChunkException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

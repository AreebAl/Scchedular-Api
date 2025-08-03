package com.example.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiCallService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiCallService.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${api.url:https://jsonplaceholder.typicode.com/posts/1}")
    private String apiUrl;
    
    public ApiCallService() {
        this.restTemplate = new RestTemplate();
    }
    
    public void callExternalApi() {
        try {
            logger.info("Making scheduled API call to: {}", apiUrl);
            
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("API call successful. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
            } else {
                logger.warn("API call returned non-success status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            logger.error("Error making API call: {}", e.getMessage(), e);
        }
    }
} 
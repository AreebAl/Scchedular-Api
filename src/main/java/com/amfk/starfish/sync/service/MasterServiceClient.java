package com.amfk.starfish.sync.service;

import com.amfk.starfish.sync.dto.SiteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class MasterServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MasterServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${master.service.base.url}")
    private String baseUrl;
    
    @Value("${master.service.api.key:}")
    private String apiKey;
    
    @Value("${master.service.timeout:30000}")
    private int timeout;
    
    public MasterServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    @Retryable(
        value = {HttpServerErrorException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<SiteDto> getSites() {
        String url = baseUrl + "/api/sites";
        logger.info("Fetching sites from master service: {}", url);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("X-API-Key", apiKey);
            }
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<SiteDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<SiteDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} sites from master service", response.getBody().size());
                return response.getBody();
            } else {
                logger.warn("Master service returned non-success status: {}", response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching sites from master service: {}", e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            logger.error("Server error while fetching sites from master service: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Connection error while fetching sites from master service: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites from master service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites from master service", e);
        }
    }
    
    @Retryable(
        value = {HttpServerErrorException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<SiteDto> getSitesByCluster(String clusterId) {
        String url = baseUrl + "/api/sites/cluster/" + clusterId;
        logger.info("Fetching sites for cluster {} from master service: {}", clusterId, url);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("X-API-Key", apiKey);
            }
            headers.set("Content-Type", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<SiteDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<SiteDto>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} sites for cluster {} from master service", 
                    response.getBody().size(), clusterId);
                return response.getBody();
            } else {
                logger.warn("Master service returned non-success status for cluster {}: {}", 
                    clusterId, response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching sites for cluster {} from master service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            logger.error("Server error while fetching sites for cluster {} from master service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Connection error while fetching sites for cluster {} from master service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites for cluster {} from master service: {}", 
                clusterId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites for cluster " + clusterId + " from master service", e);
        }
    }
    
    public boolean isServiceHealthy() {
        try {
            String healthUrl = baseUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Master service health check failed: {}", e.getMessage());
            return false;
        }
    }
} 
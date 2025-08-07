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
import java.util.Map;

@Service
public class MasterServiceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(MasterServiceClient.class);
    
    private final RestTemplate restTemplate;
    
    @Value("${master.service.base.url}")
    private String baseUrl;
    
    @Value("${master.service.bearer.token}")
    private String bearerToken;
    
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
        String url = baseUrl + "/amsp/api/masterdata/v1/sites";
        logger.info("Fetching sites from Master Service: {}", url);
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} sites from Master Service", response.getBody().size());
                
                // Convert Map to SiteDto objects
                List<SiteDto> sites = response.getBody().stream()
                    .map(this::convertToSiteDto)
                    .toList();
                
                return sites;
            } else {
                logger.warn("Master Service returned non-success status: {}", response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching sites from Master Service: {}", e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            logger.error("Server error while fetching sites from Master Service: {}", e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Connection error while fetching sites from Master Service: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites from Master Service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites from Master Service", e);
        }
    }
    
    private SiteDto convertToSiteDto(Map<String, Object> siteMap) {
        SiteDto siteDto = new SiteDto();
        
        // Map the fields from the Master Service response to SiteDto
        if (siteMap.containsKey("id")) {
            siteDto.setSiteId(siteMap.get("id").toString());
        }
        if (siteMap.containsKey("name")) {
            siteDto.setSiteName(siteMap.get("name").toString());
        }
        if (siteMap.containsKey("locationCode")) {
            siteDto.setSiteCode(siteMap.get("locationCode").toString());
        }
        if (siteMap.containsKey("active")) {
            siteDto.setStatus(siteMap.get("active").toString());
        }
        if (siteMap.containsKey("city")) {
            siteDto.setCity(siteMap.get("city").toString());
        }
        if (siteMap.containsKey("street")) {
            siteDto.setStreet(siteMap.get("street").toString());
        }
        if (siteMap.containsKey("clusterName")) {
            siteDto.setClusterName(siteMap.get("clusterName").toString());
        }
        if (siteMap.containsKey("clusterId")) {
            siteDto.setClusterId(siteMap.get("clusterId").toString());
        }
        
        // Map location as combination of city and street
        String city = siteMap.containsKey("city") ? siteMap.get("city").toString() : "";
        String street = siteMap.containsKey("street") ? siteMap.get("street").toString() : "";
        if (!city.isEmpty() || !street.isEmpty()) {
            siteDto.setLocation((city + " " + street).trim());
        }
        
        // Map timestamp fields if needed
        if (siteMap.containsKey("logCreatedOn")) {
            try {
                String createdOn = siteMap.get("logCreatedOn").toString();
                // You can parse this to LocalDateTime if needed
                // For now, we'll just log it
                logger.debug("Site {} created on: {}", siteDto.getSiteId(), createdOn);
            } catch (Exception e) {
                logger.warn("Could not parse logCreatedOn for site {}: {}", siteDto.getSiteId(), e.getMessage());
            }
        }
        
        return siteDto;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "en-US,en;q=0.9");
        headers.set("Connection", "keep-alive");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36");
        headers.set("sec-ch-ua", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"");
        headers.set("sec-ch-ua-mobile", "?0");
        headers.set("sec-ch-ua-platform", "\"Windows\"");
        
        if (bearerToken != null && !bearerToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + bearerToken);
        }
        
        return headers;
    }
    
    @Retryable(
        value = {HttpServerErrorException.class, ResourceAccessException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<SiteDto> getSitesByCluster(String clusterId) {
        String url = baseUrl + "/amsp/api/masterdata/v1/sites";
        logger.info("Fetching sites for cluster {} from Master Service: {}", clusterId, url);
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched {} sites for cluster {} from Master Service", 
                    response.getBody().size(), clusterId);
                
                // Convert Map to SiteDto objects and filter by cluster if needed
                List<SiteDto> sites = response.getBody().stream()
                    .map(this::convertToSiteDto)
                    .toList();
                
                return sites;
            } else {
                logger.warn("Master Service returned non-success status for cluster {}: {}", 
                    clusterId, response.getStatusCode());
                return List.of();
            }
            
        } catch (HttpClientErrorException e) {
            logger.error("Client error while fetching sites for cluster {} from Master Service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (HttpServerErrorException e) {
            logger.error("Server error while fetching sites for cluster {} from Master Service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            logger.error("Connection error while fetching sites for cluster {} from Master Service: {}", 
                clusterId, e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites for cluster {} from Master Service: {}", 
                clusterId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites for cluster " + clusterId + " from Master Service", e);
        }
    }
    
    public boolean isServiceHealthy() {
        try {
            String healthUrl = baseUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.warn("Master Service health check failed: {}", e.getMessage());
            return false;
        }
    }
} 
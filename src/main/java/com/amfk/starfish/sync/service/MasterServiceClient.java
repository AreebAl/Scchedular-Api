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
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.apache.hc.core5.http.TruncatedChunkException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

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
    
    /**
     * Fetches all sites from the Master Service.
     * Expected dataset size: ~471 records
     * This method includes retry logic for handling TruncatedChunkException
     * and other network-related issues common with large responses.
     */
    @Retryable(
        value = {HttpServerErrorException.class, ResourceAccessException.class, RestClientException.class, 
                org.springframework.http.converter.HttpMessageNotReadableException.class,
                com.fasterxml.jackson.databind.JsonMappingException.class},
        maxAttempts = 8,
        backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 60000)
    )
    public List<SiteDto> getSites() {
        String url = baseUrl + "/amsp/api/masterdata/v1/sites";
        
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            logger.info("Attempting to fetch sites from: {}", url);
            
            // Use a more robust approach for large responses
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (!rawResponse.getStatusCode().is2xxSuccessful()) {
                logger.warn("Master Service returned non-success status: {}", rawResponse.getStatusCode());
                return List.of();
            }
            
            String responseBody = rawResponse.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.warn("Empty response body from Master Service");
                return List.of();
            }
            
            logger.info("Received response body length: {} characters", responseBody.length());
            
            // Parse the JSON response manually to handle truncation better
            List<Map<String, Object>> sites = parseSitesResponse(responseBody);
            
            if (sites != null && !sites.isEmpty()) {
                int siteCount = sites.size();
                logger.info("Successfully parsed {} sites from Master Service", siteCount);
                
                // Log performance metrics for large datasets
                if (siteCount > 100) {
                    logger.info("Processing large dataset: {} sites - this may take longer", siteCount);
                }
                
                System.out.println("=== MASTER SERVICE API RESPONSE ===");
                System.out.println("Total sites returned: " + siteCount);
                System.out.println("First 5 sites:");
                for (int i = 0; i < Math.min(5, sites.size()); i++) {
                    Map<String, Object> site = sites.get(i);
                    System.out.println("Site " + (i+1) + ": " + site.get("name") + " (Cluster: " + site.get("clusterName") + ")");
                }
                if (siteCount > 5) {
                    System.out.println("... and " + (siteCount - 5) + " more sites");
                }
                System.out.println("=================================");
                
                // Convert Map to SiteDto objects
                List<SiteDto> siteDtos = sites.stream()
                    .map(this::convertToSiteDto)
                    .toList();
                
                return siteDtos;
            } else {
                logger.warn("No sites found in parsed response");
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
        } catch (RestClientException e) {
            // Check if this is a TruncatedChunkException
            if (isTruncatedChunkException(e)) {
                logger.warn("Truncated chunk error while fetching sites from Master Service (will retry): {}", e.getMessage());
            } else {
                logger.error("Rest client error while fetching sites from Master Service: {}", e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while fetching sites from Master Service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch sites from Master Service", e);
        }
    }
    
    /**
     * Recovery method for retry failures
     */
    @Recover
    public List<SiteDto> recover(Exception ex) {
        logger.error("All retry attempts failed for getSites. Last error: {}", ex.getMessage(), ex);
        throw new RuntimeException("Failed to fetch sites from Master Service after all retry attempts", ex);
    }
    
    /**
     * Parse the JSON response string into a list of site maps
     * This method handles potential truncation issues more gracefully
     */
    private List<Map<String, Object>> parseSitesResponse(String responseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            
            // Check if response looks truncated
            if (responseBody.trim().endsWith(",") || !responseBody.trim().endsWith("]")) {
                logger.warn("Response appears to be truncated. Attempting to fix...");
                
                // Try to fix common truncation issues
                String fixedResponse = fixTruncatedJson(responseBody);
                if (fixedResponse != null) {
                    responseBody = fixedResponse;
                    logger.info("Attempted to fix truncated JSON response");
                }
            }
            
            // Parse the JSON response
            List<Map<String, Object>> sites = objectMapper.readValue(
                responseBody, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            logger.info("Successfully parsed {} sites from JSON response", sites.size());
            return sites;
            
        } catch (Exception e) {
            logger.error("Failed to parse JSON response: {}", e.getMessage(), e);
            
            // If parsing fails, try to extract partial data
            return extractPartialSites(responseBody);
        }
    }
    
    /**
     * Attempt to fix common JSON truncation issues
     */
    private String fixTruncatedJson(String json) {
        try {
            String trimmed = json.trim();
            
            // If it ends with a comma, try to close the array
            if (trimmed.endsWith(",")) {
                return trimmed.substring(0, trimmed.length() - 1) + "]";
            }
            
            // If it doesn't end with ], try to add it
            if (!trimmed.endsWith("]")) {
                return trimmed + "]";
            }
            
            return json;
        } catch (Exception e) {
            logger.warn("Failed to fix truncated JSON: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Extract partial sites from a potentially corrupted JSON response
     */
    private List<Map<String, Object>> extractPartialSites(String responseBody) {
        try {
            // Try to find complete site objects in the response
            List<Map<String, Object>> sites = new java.util.ArrayList<>();
            
            // Look for site objects that start with { and end with }
            String[] lines = responseBody.split("\n");
            StringBuilder currentSite = new StringBuilder();
            boolean inSite = false;
            
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("{") && !inSite) {
                    inSite = true;
                    currentSite = new StringBuilder(line);
                } else if (inSite) {
                    currentSite.append(line);
                    if (line.endsWith("}")) {
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            Map<String, Object> site = mapper.readValue(currentSite.toString(), Map.class);
                            sites.add(site);
                        } catch (Exception e) {
                            // Skip this site if it can't be parsed
                        }
                        inSite = false;
                        currentSite = new StringBuilder();
                    }
                }
            }
            
            logger.warn("Extracted {} partial sites from corrupted response", sites.size());
            return sites;
            
        } catch (Exception e) {
            logger.error("Failed to extract partial sites: {}", e.getMessage());
            return List.of();
        }
    }

    
    private SiteDto convertToSiteDto(Map<String, Object> siteMap) {
        SiteDto siteDto = new SiteDto();
        
        // Map the fields from the Master Service response to SiteDto
        if (siteMap.containsKey("id")) {
            siteDto.setId((Integer) siteMap.get("id"));
        }
        if (siteMap.containsKey("name")) {
            siteDto.setName(siteMap.get("name").toString());
        }
        if (siteMap.containsKey("nameEnglish")) {
            siteDto.setNameEnglish(siteMap.get("nameEnglish") != null ? siteMap.get("nameEnglish").toString() : null);
        }
        if (siteMap.containsKey("nameGerman")) {
            siteDto.setNameGerman(siteMap.get("nameGerman") != null ? siteMap.get("nameGerman").toString() : null);
        }
        if (siteMap.containsKey("locationCode")) {
            siteDto.setLocationCode(siteMap.get("locationCode") != null ? siteMap.get("locationCode").toString() : null);
        }
        if (siteMap.containsKey("city")) {
            siteDto.setCity(siteMap.get("city") != null ? siteMap.get("city").toString() : null);
        }
        if (siteMap.containsKey("street")) {
            siteDto.setStreet(siteMap.get("street") != null ? siteMap.get("street").toString() : null);
        }
        if (siteMap.containsKey("remark")) {
            siteDto.setRemark(siteMap.get("remark") != null ? siteMap.get("remark").toString() : null);
        }
        if (siteMap.containsKey("active")) {
            siteDto.setActive((Integer) siteMap.get("active"));
        }
        if (siteMap.containsKey("logCreatedBy")) {
            siteDto.setLogCreatedBy(siteMap.get("logCreatedBy") != null ? siteMap.get("logCreatedBy").toString() : null);
        }
        if (siteMap.containsKey("logCreatedOn")) {
            // Handle date parsing if needed
            Object createdOn = siteMap.get("logCreatedOn");
            if (createdOn != null) {
                // You might need to parse the date string here
                // For now, we'll leave it as null if it's not already a LocalDateTime
            }
        }
        if (siteMap.containsKey("logUpdatedBy")) {
            siteDto.setLogUpdatedBy(siteMap.get("logUpdatedBy") != null ? siteMap.get("logUpdatedBy").toString() : null);
        }
        if (siteMap.containsKey("logUpdatedOn")) {
            // Handle date parsing if needed
            Object updatedOn = siteMap.get("logUpdatedOn");
            if (updatedOn != null) {
                // You might need to parse the date string here
                // For now, we'll leave it as null if it's not already a LocalDateTime
            }
        }
        if (siteMap.containsKey("clusterName")) {
            siteDto.setClusterName(siteMap.get("clusterName") != null ? siteMap.get("clusterName").toString() : null);
        }
        if (siteMap.containsKey("clusterId")) {
            siteDto.setClusterId((Integer) siteMap.get("clusterId"));
        }
        if (siteMap.containsKey("sipDomain")) {
            siteDto.setSipDomain(siteMap.get("sipDomain") != null ? siteMap.get("sipDomain").toString() : null);
        }
        if (siteMap.containsKey("routingPolicy")) {
            siteDto.setRoutingPolicy(siteMap.get("routingPolicy") != null ? siteMap.get("routingPolicy").toString() : null);
        }
        if (siteMap.containsKey("cmName")) {
            siteDto.setCmName(siteMap.get("cmName") != null ? siteMap.get("cmName").toString() : null);
        }
        if (siteMap.containsKey("notes")) {
            siteDto.setNotes(siteMap.get("notes") != null ? siteMap.get("notes").toString() : null);
        }
        if (siteMap.containsKey("ars")) {
            siteDto.setArs(siteMap.get("ars") != null ? siteMap.get("ars").toString() : null);
        }
        if (siteMap.containsKey("userStamp")) {
            siteDto.setUserStamp(siteMap.get("userStamp") != null ? siteMap.get("userStamp").toString() : null);
        }
        if (siteMap.containsKey("timeStamp")) {
            // Handle date parsing if needed
            Object timeStamp = siteMap.get("timeStamp");
            if (timeStamp != null) {
                // You might need to parse the date string here
                // For now, we'll leave it as null if it's not already a LocalDateTime
            }
        }
        
        // Map location as combination of city and street
        String city = siteDto.getCity() != null ? siteDto.getCity() : "";
        String street = siteDto.getStreet() != null ? siteDto.getStreet() : "";
        if (!city.isEmpty() || !street.isEmpty()) {
            siteDto.setLocation((city + " " + street).trim());
        }
        
        return siteDto;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");
        headers.set("Accept-Encoding", "gzip, deflate");
        headers.set("Connection", "keep-alive");
        headers.set("Cache-Control", "no-cache");
        
        if (bearerToken != null && !bearerToken.isEmpty()) {
            headers.set("Authorization", "Bearer " + bearerToken);
        }
        
        return headers;
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
    
    /**
     * Checks if the given exception is caused by a TruncatedChunkException
     */
    private boolean isTruncatedChunkException(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof TruncatedChunkException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}

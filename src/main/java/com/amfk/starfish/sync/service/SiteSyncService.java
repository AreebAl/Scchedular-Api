package com.amfk.starfish.sync.service;

import com.amfk.starfish.sync.dto.SiteDto;
import com.amfk.starfish.sync.entity.JobExecution;
import com.amfk.starfish.sync.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SiteSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SiteSyncService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final MasterServiceClient masterServiceClient;
    private final MockApiService mockApiService;
    private final JobExecutionRepository jobExecutionRepository;
    
    @Autowired
    public SiteSyncService(MasterServiceClient masterServiceClient,
                          MockApiService mockApiService,
                          JobExecutionRepository jobExecutionRepository) {
        this.masterServiceClient = masterServiceClient;
        this.mockApiService = mockApiService;
        this.jobExecutionRepository = jobExecutionRepository;
    }
    
    public String syncSites() {
        String jobId = UUID.randomUUID().toString();
        String jobName = "SITE_SYNC_JOB";
        
        JobExecution jobExecution = new JobExecution(jobId, jobName);
        jobExecutionRepository.save(jobExecution);
        
        logger.info("Starting site sync job with ID: {}", jobId);
        
        try {
            // Step 1: Fetch sites from Master Service API
            logger.info("Step 1: Fetching sites from Master Service API");
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites == null || sites.isEmpty()) {
                logger.warn("No sites found in Master Service API");
                jobExecution.setRecordsProcessed(0);
                jobExecution.complete();
                jobExecutionRepository.save(jobExecution);
                return "Site sync completed - no sites found";
            }
            
            logger.info("Retrieved {} sites from Master Service API", sites.size());
            
            // Check what clusters are available in the database
            mockApiService.checkAvailableClusters();
            
            // Step 2: For each site, call Mock API using cluster name
            logger.info("Step 2: Calling Mock API for each site");
            int processedCount = 0;
            int successCount = 0;
            int failureCount = 0;
            
            for (SiteDto site : sites) {
                try {
                    String siteName = site.getSiteName() != null ? site.getSiteName() : "Unknown";
                    String siteId = site.getSiteId() != null ? site.getSiteId() : "Unknown";
                    String clusterName = site.getClusterName() != null ? site.getClusterName() : "Unknown";
                    
                    logger.info("Processing site: {} ({}) with cluster: {}", siteName, siteId, clusterName);
                    
                    // Call Mock API for site details using cluster name
                    List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(clusterName);
                    
                    if (mockResponse != null && !mockResponse.isEmpty()) {
                        logger.info("Successfully retrieved Mock API data for site: {} - Response: {}", 
                            siteName, mockResponse);
                        successCount++;
                        
                        // Log the mock response data
                        logger.debug("Mock API data for site {}: {}", siteName, mockResponse);
                        
                    } else {
                        logger.warn("No Mock API data found for site: {} ({})", siteName, siteId);
                        failureCount++;
                    }
                    
                    processedCount++;
                    
                    // Add small delay to avoid overwhelming the database
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    logger.error("Error processing site: {}", e.getMessage(), e);
                    failureCount++;
                    processedCount++;
                }
            }
            
            logger.info("Processed {} sites from Master Service API with Mock API calls", processedCount);
            
            jobExecution.setRecordsProcessed(processedCount);
            jobExecution.complete();
            jobExecutionRepository.save(jobExecution);
            
            String result = String.format("Site sync completed successfully. Processed: %d, Success: %d, Failed: %d (Mock API calls)", 
                processedCount, successCount, failureCount);
            
            logger.info(result);
            return result;
            
        } catch (Exception e) {
            logger.error("Site sync job failed with ID {}: {}", jobId, e.getMessage(), e);
            
            jobExecution.fail(e.getMessage());
            jobExecutionRepository.save(jobExecution);
            
            throw new RuntimeException("Site sync job failed", e);
        }
    }
    
}

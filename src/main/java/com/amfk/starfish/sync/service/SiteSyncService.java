package com.amfk.starfish.sync.service;

import com.amfk.starfish.sync.dto.SiteDto;
import com.amfk.starfish.sync.dto.StarfishSiteDto;
import com.amfk.starfish.sync.entity.JobExecution;
import com.amfk.starfish.sync.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.mail.SimpleMailMessage;
// import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class SiteSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SiteSyncService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final MasterServiceClient masterServiceClient;
    // private final StarfishApiClient starfishApiClient;
    private final MockApiService mockApiService;
    private final JobExecutionRepository jobExecutionRepository;
    // private final JavaMailSender mailSender;
    
    @Autowired
    public SiteSyncService(MasterServiceClient masterServiceClient, 
                          // StarfishApiClient starfishApiClient,
                          MockApiService mockApiService,
                          JobExecutionRepository jobExecutionRepository
                          // JavaMailSender mailSender
                          ) {
        this.masterServiceClient = masterServiceClient;
        // this.starfishApiClient = starfishApiClient;
        this.mockApiService = mockApiService;
        this.jobExecutionRepository = jobExecutionRepository;
        // this.mailSender = mailSender;
    }
    
    @Async
    public CompletableFuture<String> syncSitesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            String jobId = UUID.randomUUID().toString();
            String jobName = "SITE_SYNC_JOB";
            
            JobExecution jobExecution = new JobExecution(jobId, jobName);
            jobExecutionRepository.save(jobExecution);
            
            logger.info("Starting site sync job with ID: {}", jobId);
            
            try {
                logger.info("Step 1: Pulling site data from master service");
                List<SiteDto> sites = masterServiceClient.getSites();
                
                if (sites.isEmpty()) {
                    logger.warn("No sites found in master service");
                    jobExecution.setRecordsProcessed(0);
                    jobExecution.complete();
                    jobExecutionRepository.save(jobExecution);
                    return "Site sync completed - no sites found";
                }
                
                logger.info("Retrieved {} sites from master service", sites.size());
                
                // Store the Master Service API response in JobExecution
                String apiResponseJson = convertSitesToJson(sites);
                jobExecution.setApiResponse(apiResponseJson);
                jobExecutionRepository.save(jobExecution);
                
                // Step 2: For each site, get information from Mock API
                logger.info("Step 2: Fetching Mock API information for each site");
                int processedCount = 0;
                int successCount = 0;
                int failureCount = 0;
                
                for (SiteDto site : sites) {
                    try {
                        logger.info("Processing site: {} ({}) with cluster: {}", site.getSiteName(), site.getSiteId(), site.getClusterName());
                        
                        // Call Mock API for site details using cluster name
                        List<Map<String, Object>> mockResponse = mockApiService.getSiteDetails(site.getClusterName());
                        
                        if (mockResponse != null && !mockResponse.isEmpty()) {
                            logger.info("Successfully retrieved Mock API data for site: {} - Response: {}", 
                                site.getSiteName(), mockResponse);
                            successCount++;
                            
                            // Log the mock response data
                            logger.debug("Mock API data for site {}: {}", site.getSiteName(), mockResponse);
                            
                        } else {
                            logger.warn("No Mock API data found for site: {} ({})", site.getSiteName(), site.getSiteId());
                            failureCount++;
                        }
                        
                        processedCount++;
                        
                        // Add small delay to avoid overwhelming the Mock API
                        Thread.sleep(100);
                        
                    } catch (Exception e) {
                        logger.error("Error processing site {} ({}): {}", 
                            site.getSiteName(), site.getSiteId(), e.getMessage(), e);
                        failureCount++;
                        processedCount++;
                    }
                }
                
                logger.info("Processed {} sites from master service with Mock API calls", processedCount);
                
                jobExecution.setRecordsProcessed(processedCount);
                jobExecution.complete();
                jobExecutionRepository.save(jobExecution);
                
                String result = String.format("Site sync completed successfully. Processed: %d, Success: %d, Failed: %d (Mock API calls)", 
                    processedCount, successCount, failureCount);
                
                logger.info(result);
                
                // Send notification if there were failures
                // COMMENTED OUT FOR NOW - Email notifications disabled
                /*
                if (failureCount > 0) {
                    sendNotificationEmail(jobId, processedCount, successCount, failureCount);
                }
                */
                
                return result;
                
            } catch (Exception e) {
                logger.error("Site sync job failed with ID {}: {}", jobId, e.getMessage(), e);
                
                jobExecution.fail(e.getMessage());
                jobExecutionRepository.save(jobExecution);
                
                // Send failure notification
                // COMMENTED OUT FOR NOW - Email notifications disabled
                // sendFailureNotificationEmail(jobId, e.getMessage());
                
                throw new RuntimeException("Site sync job failed", e);
            }
        });
    }
    
    public String syncSites() {
        try {
            return syncSitesAsync().get();
        } catch (Exception e) {
            logger.error("Error in syncSites: {}", e.getMessage(), e);
            throw new RuntimeException("Site sync failed", e);
        }
    }
    
    // COMMENTED OUT FOR NOW - Email functionality disabled
    /*
    private void sendNotificationEmail(String jobId, int processed, int success, int failed) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@example.com"); // Configure from properties
            message.setSubject("Site Sync Job Completed with Warnings - Job ID: " + jobId);
            message.setText(String.format(
                "Site sync job completed with some failures.\n\n" +
                "Job ID: %s\n" +
                "Timestamp: %s\n" +
                "Total Processed: %d\n" +
                "Successful: %d\n" +
                "Failed: %d\n\n" +
                "Please check the logs for more details.",
                jobId, LocalDateTime.now().format(formatter), processed, success, failed
            ));
            
            mailSender.send(message);
            logger.info("Notification email sent for job: {}", jobId);
            
        } catch (Exception e) {
            logger.error("Failed to send notification email for job {}: {}", jobId, e.getMessage());
        }
    }
    
    private void sendFailureNotificationEmail(String jobId, String errorMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@example.com"); // Configure from properties
            message.setSubject("Site Sync Job Failed - Job ID: " + jobId);
            message.setText(String.format(
                "Site sync job failed.\n\n" +
                "Job ID: %s\n" +
                "Timestamp: %s\n" +
                "Error: %s\n\n" +
                "Please check the logs for more details.",
                jobId, LocalDateTime.now().format(formatter), errorMessage
            ));
            
            mailSender.send(message);
            logger.info("Failure notification email sent for job: {}", jobId);
            
        } catch (Exception e) {
            logger.error("Failed to send failure notification email for job {}: {}", jobId, e.getMessage());
        }
    }
    */
    
    private String convertSitesToJson(List<SiteDto> sites) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\"sites\":[");
            
            for (int i = 0; i < sites.size(); i++) {
                SiteDto site = sites.get(i);
                json.append("{");
                json.append("\"siteId\":\"").append(site.getSiteId() != null ? site.getSiteId() : "").append("\",");
                json.append("\"siteName\":\"").append(site.getSiteName() != null ? site.getSiteName() : "").append("\",");
                json.append("\"siteCode\":\"").append(site.getSiteCode() != null ? site.getSiteCode() : "").append("\",");
                json.append("\"status\":\"").append(site.getStatus() != null ? site.getStatus() : "").append("\",");
                json.append("\"city\":\"").append(site.getCity() != null ? site.getCity() : "").append("\",");
                json.append("\"street\":\"").append(site.getStreet() != null ? site.getStreet() : "").append("\",");
                json.append("\"location\":\"").append(site.getLocation() != null ? site.getLocation() : "").append("\",");
                json.append("\"clusterName\":\"").append(site.getClusterName() != null ? site.getClusterName() : "").append("\",");
                json.append("\"clusterId\":\"").append(site.getClusterId() != null ? site.getClusterId() : "").append("\"");
                json.append("}");
                
                if (i < sites.size() - 1) {
                    json.append(",");
                }
            }
            
            json.append("]}");
            return json.toString();
        } catch (Exception e) {
            logger.error("Error converting sites to JSON: {}", e.getMessage());
            return "{\"error\":\"Failed to convert sites to JSON\"}";
        }
    }

} 
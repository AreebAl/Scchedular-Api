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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Service
public class SiteSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SiteSyncService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final MasterServiceClient masterServiceClient;
    // private final StarfishApiClient starfishApiClient;
    private final JobExecutionRepository jobExecutionRepository;
    // private final JavaMailSender mailSender;
    
    @Autowired
    public SiteSyncService(MasterServiceClient masterServiceClient, 
                          // StarfishApiClient starfishApiClient,
                          JobExecutionRepository jobExecutionRepository
                          // JavaMailSender mailSender
                          ) {
        this.masterServiceClient = masterServiceClient;
        // this.starfishApiClient = starfishApiClient;
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
                
                // Step 2: For each site, get information from STARFISH
                // COMMENTED OUT FOR NOW - Starfish API calls disabled
                /*
                logger.info("Step 2: Fetching STARFISH information for each site");
                int processedCount = 0;
                int successCount = 0;
                int failureCount = 0;
                
                for (SiteDto site : sites) {
                    try {
                        logger.info("Processing site: {} ({})", site.getSiteName(), site.getSiteId());
                        
                        // Try to get STARFISH info by site ID first, then by code
                        StarfishSiteDto starfishSite = starfishApiClient.getSiteInfo(site.getSiteId());
                        
                        if (starfishSite == null && site.getSiteName() != null) {
                            // Try by site name/code as fallback
                            starfishSite = starfishApiClient.getSiteInfoByCode(site.getSiteName());
                        }
                        
                        if (starfishSite != null) {
                            logger.info("Successfully retrieved STARFISH data for site: {} - Name: {}, Status: {}", 
                                site.getSiteId(), starfishSite.getName(), starfishSite.getStatus());
                            successCount++;
                            
                            // TODO: Store STARFISH data temporarily (DB design to be discussed)
                            // For now, just log the data
                            logger.debug("STARFISH data for site {}: {}", site.getSiteId(), starfishSite);
                            
                        } else {
                            logger.warn("No STARFISH data found for site: {} ({})", site.getSiteName(), site.getSiteId());
                            failureCount++;
                        }
                        
                        processedCount++;
                        
                        // Add small delay to avoid overwhelming the STARFISH API
                        Thread.sleep(100);
                        
                    } catch (Exception e) {
                        logger.error("Error processing site {} ({}): {}", 
                            site.getSiteName(), site.getSiteId(), e.getMessage(), e);
                        failureCount++;
                        processedCount++;
                    }
                }
                */
                
                // For now, just process the sites without calling Starfish API
                int processedCount = sites.size();
                int successCount = sites.size();
                int failureCount = 0;
                
                logger.info("Processed {} sites from master service (Starfish API calls disabled)", processedCount);
                
                jobExecution.setRecordsProcessed(processedCount);
                jobExecution.complete();
                jobExecutionRepository.save(jobExecution);
                
                String result = String.format("Site sync completed successfully. Processed: %d, Success: %d, Failed: %d (Starfish API calls disabled)", 
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
    
    
    public JobExecution getLastJobExecution() {
        return jobExecutionRepository.findFirstByJobNameOrderByCreateTsDesc("SITE_SYNC_JOB").orElse(null);
    }
    
    public List<JobExecution> getRecentJobExecutions(int limit) {
        // This would need a custom query in the repository
        // For now, return the last execution
        JobExecution last = getLastJobExecution();
        return last != null ? List.of(last) : List.of();
    }
} 
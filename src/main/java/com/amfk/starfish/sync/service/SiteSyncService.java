package com.amfk.starfish.sync.service;

import com.amfk.starfish.sync.dto.SiteDto;
import com.amfk.starfish.sync.dto.StarfishSiteDto;
import com.amfk.starfish.sync.entity.JobExecution;
import com.amfk.starfish.sync.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for handling site synchronization operations
 * 
 * This service orchestrates the site synchronization process by:
 * 1. Pulling site data from the master service
 * 2. For each site, calling the STARFISH API to get detailed information
 * 3. Storing STARFISH data temporarily
 * 4. Tracking job execution in the database
 * 5. Sending email notifications for failures/warnings
 * 
 * The service supports both synchronous and asynchronous execution modes.
 */
@Service
public class SiteSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(SiteSyncService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final MasterServiceClient masterServiceClient;
    private final StarfishApiClient starfishApiClient;
    private final JobExecutionRepository jobExecutionRepository;
    private final JavaMailSender mailSender;
    
    /**
     * Constructor for SiteSyncService
     * 
     * @param masterServiceClient Client for communicating with the master service
     * @param starfishApiClient Client for communicating with the STARFISH API
     * @param jobExecutionRepository Repository for persisting job execution data
     * @param mailSender Service for sending email notifications
     */
    @Autowired
    public SiteSyncService(MasterServiceClient masterServiceClient, 
                          StarfishApiClient starfishApiClient,
                          JobExecutionRepository jobExecutionRepository,
                          JavaMailSender mailSender) {
        this.masterServiceClient = masterServiceClient;
        this.starfishApiClient = starfishApiClient;
        this.jobExecutionRepository = jobExecutionRepository;
        this.mailSender = mailSender;
    }
    
    /**
     * Asynchronously executes the site synchronization process
     * 
     * This method runs the site sync job in a separate thread to avoid blocking
     * the calling thread. It performs the following steps:
     * 1. Creates a new job execution record with a unique ID
     * 2. Fetches all sites from the master service
     * 3. For each site, retrieves detailed information from STARFISH API
     * 4. Tracks success/failure counts and updates the job execution record
     * 5. Sends email notifications for failures or warnings
     * 
     * @return CompletableFuture<String> containing the result message
     */
    @Async
    public CompletableFuture<String> syncSitesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            // Generate unique job ID and create execution record
            String jobId = UUID.randomUUID().toString();
            String jobName = "SITE_SYNC_JOB";
            
            JobExecution jobExecution = new JobExecution(jobId, jobName);
            jobExecutionRepository.save(jobExecution);
            
            logger.info("Starting site sync job with ID: {}", jobId);
            
            try {
                // Step 1: Pull data from master service for sites/clusters
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
                
                // Update job execution with results
                jobExecution.setRecordsProcessed(processedCount);
                jobExecution.complete();
                jobExecutionRepository.save(jobExecution);
                
                String result = String.format("Site sync completed successfully. Processed: %d, Success: %d, Failed: %d", 
                    processedCount, successCount, failureCount);
                
                logger.info(result);
                
                // Send notification if there were failures
                if (failureCount > 0) {
                    sendNotificationEmail(jobId, processedCount, successCount, failureCount);
                }
                
                return result;
                
            } catch (Exception e) {
                logger.error("Site sync job failed with ID {}: {}", jobId, e.getMessage(), e);
                
                jobExecution.fail(e.getMessage());
                jobExecutionRepository.save(jobExecution);
                
                // Send failure notification
                sendFailureNotificationEmail(jobId, e.getMessage());
                
                throw new RuntimeException("Site sync job failed", e);
            }
        });
    }
    
    /**
     * Synchronously executes the site synchronization process
     * 
     * This method provides a synchronous wrapper around the async method.
     * It blocks until the async operation completes and returns the result.
     * 
     * @return String containing the result message
     * @throws RuntimeException if the sync operation fails
     */
    public String syncSites() {
        try {
            return syncSitesAsync().get();
        } catch (Exception e) {
            logger.error("Error in syncSites: {}", e.getMessage(), e);
            throw new RuntimeException("Site sync failed", e);
        }
    }
    
    /**
     * Sends a notification email when the site sync job completes with warnings
     * 
     * This method is called when the job completes but some sites failed to process.
     * It sends an email to the administrator with details about the job execution.
     * 
     * @param jobId Unique identifier for the job
     * @param processed Total number of sites processed
     * @param success Number of sites successfully processed
     * @param failed Number of sites that failed to process
     */
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
    
    /**
     * Sends a notification email when the site sync job fails completely
     * 
     * This method is called when the entire job fails due to an exception.
     * It sends an email to the administrator with the error details.
     * 
     * @param jobId Unique identifier for the job
     * @param errorMessage The error message describing what went wrong
     */
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
    
    /**
     * Retrieves the most recent job execution for site synchronization
     * 
     * This method queries the database for the latest job execution record
     * with the job name "SITE_SYNC_JOB".
     * 
     * @return JobExecution object representing the last execution, or null if none found
     */
    public JobExecution getLastJobExecution() {
        return jobExecutionRepository.findFirstByJobNameOrderByCreateTsDesc("SITE_SYNC_JOB").orElse(null);
    }
    
    /**
     * Retrieves recent job executions for site synchronization
     * 
     * This method returns a list of recent job executions. Currently, it returns
     * only the last execution, but this could be enhanced to return multiple
     * executions based on the limit parameter.
     * 
     * @param limit Maximum number of job executions to return
     * @return List of JobExecution objects
     */
    public List<JobExecution> getRecentJobExecutions(int limit) {
        // This would need a custom query in the repository
        // For now, return the last execution
        JobExecution last = getLastJobExecution();
        return last != null ? List.of(last) : List.of();
    }
} 
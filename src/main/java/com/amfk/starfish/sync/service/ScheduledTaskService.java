package com.amfk.starfish.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing scheduled tasks in the AMFK Starfish Sync application
 * 
 * This service handles the scheduling and execution of background jobs including:
 * - Regular API calls to external services
 * - Site synchronization jobs that pull data from master service and STARFISH API
 * 
 * The service uses Spring's @Scheduled annotation to configure task execution
 * and provides methods to query scheduling information.
 */
@Service
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ApiCallService apiCallService;
    private final SiteSyncService siteSyncService;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private static final long SCHEDULE_INTERVAL_MS = 120000; // 2 minutes
    
    @Value("${site.sync.enabled:true}")
    private boolean siteSyncEnabled;
    
    @Value("${site.sync.cron:0 0 */2 * * ?}")
    private String siteSyncCron;
    
    /**
     * Constructor for ScheduledTaskService
     * 
     * @param apiCallService Service for making external API calls
     * @param siteSyncService Service for site synchronization operations
     */
    @Autowired
    public ScheduledTaskService(ApiCallService apiCallService, SiteSyncService siteSyncService) {
        this.apiCallService = apiCallService;
        this.siteSyncService = siteSyncService;
        // Initialize next execution time to 2 minutes from now
        this.nextExecutionTime = LocalDateTime.now().plusMinutes(2);
    }
    
    /**
     * Scheduled method that executes API calls at fixed intervals
     * 
     * This method is scheduled to run every 2 minutes (120,000 milliseconds).
     * It calls the external API service and updates the execution tracking
     * information for monitoring purposes.
     * 
     * The method logs the start and completion times, and calculates the
     * next scheduled execution time.
     */
    @Scheduled(fixedRate = SCHEDULE_INTERVAL_MS)
    public void scheduledApiCall() {
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled API call at: {}", currentTime);
        
        // Update execution tracking
        this.lastExecutionTime = LocalDateTime.now();
        this.nextExecutionTime = this.lastExecutionTime.plusMinutes(2);
        
        // Execute the API call
        apiCallService.callExternalApi();
        
        logger.info("Completed scheduled API call at: {}", currentTime);
        logger.info("Next scheduled execution at: {}", this.nextExecutionTime.format(formatter));
    }
    
    /**
     * Scheduled method that executes site synchronization jobs
     * 
     * This method is scheduled using a cron expression (default: every 2 hours).
     * It checks if site sync is enabled before executing, and if enabled,
     * calls the site synchronization service to pull data from master service
     * and STARFISH API.
     * 
     * The method includes error handling and logging for monitoring purposes.
     * The cron expression can be configured via application properties.
     */
    @Scheduled(cron = "${site.sync.cron:0 0 */2 * * ?}")
    public void scheduledSiteSync() {
        if (!siteSyncEnabled) {
            logger.info("Site sync is disabled, skipping scheduled execution");
            return;
        }
        
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled site sync job at: {}", currentTime);
        
        try {
            // Execute the site synchronization process
            String result = siteSyncService.syncSites();
            logger.info("Completed scheduled site sync job at: {} with result: {}", currentTime, result);
        } catch (Exception e) {
            logger.error("Scheduled site sync job failed at {}: {}", currentTime, e.getMessage(), e);
        }
    }
    
    /**
     * Gets the timestamp of the last API call execution
     * 
     * @return LocalDateTime representing when the last scheduled API call was executed,
     *         or null if no execution has occurred yet
     */
    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    /**
     * Gets the timestamp of the next scheduled API call execution
     * 
     * @return LocalDateTime representing when the next scheduled API call will be executed
     */
    public LocalDateTime getNextExecutionTime() {
        return nextExecutionTime;
    }
    
    /**
     * Gets the schedule interval for API calls in milliseconds
     * 
     * @return long value representing the interval between API call executions in milliseconds
     */
    public long getScheduleIntervalMs() {
        return SCHEDULE_INTERVAL_MS;
    }
    
    /**
     * Checks if site synchronization is enabled
     * 
     * This value is configured via the 'site.sync.enabled' property
     * and determines whether scheduled site sync jobs will execute.
     * 
     * @return boolean true if site sync is enabled, false otherwise
     */
    public boolean isSiteSyncEnabled() {
        return siteSyncEnabled;
    }
    
    /**
     * Gets the cron expression used for site synchronization scheduling
     * 
     * This value is configured via the 'site.sync.cron' property
     * and defines when site sync jobs should be executed.
     * 
     * @return String containing the cron expression for scheduling
     */
    public String getSiteSyncCron() {
        return siteSyncCron;
    }
} 
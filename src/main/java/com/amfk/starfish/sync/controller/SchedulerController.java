package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.entity.JobExecution;
import com.amfk.starfish.sync.service.ApiCallService;
import com.amfk.starfish.sync.service.ScheduledTaskService;
import com.amfk.starfish.sync.service.SiteSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for AMFK Starfish Sync Service
 * 
 * This controller provides endpoints for:
 * - Manual triggering of API calls and site synchronization
 * - Monitoring job execution status and history
 * - Health checks and service status
 * - SAMS integration endpoints
 */
@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final ApiCallService apiCallService;
    private final ScheduledTaskService scheduledTaskService;
    private final SiteSyncService siteSyncService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Constructor for SchedulerController
     * 
     * @param apiCallService Service for making external API calls
     * @param scheduledTaskService Service for managing scheduled tasks
     * @param siteSyncService Service for site synchronization operations
     */
    @Autowired
    public SchedulerController(ApiCallService apiCallService, 
                              ScheduledTaskService scheduledTaskService,
                              SiteSyncService siteSyncService) {
        this.apiCallService = apiCallService;
        this.scheduledTaskService = scheduledTaskService;
        this.siteSyncService = siteSyncService;
    }
    
    /**
     * Manually triggers an external API call
     * 
     * This endpoint allows manual execution of the external API call that is normally
     * scheduled to run periodically. Useful for testing or immediate execution.
     * 
     * @return ResponseEntity containing:
     *         - status: "success" or "error"
     *         - message: Description of the operation result
     *         - timestamp: When the operation was performed
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerApiCall() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            // Execute the external API call
            apiCallService.callExternalApi();
            
            response.put("status", "success");
            response.put("message", "API call triggered successfully");
            response.put("timestamp", timestamp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to trigger API call: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Manually triggers site synchronization process
     * 
     * This endpoint initiates the site synchronization job that:
     * 1. Fetches site data from the master service
     * 2. For each site, calls the STARFISH API to get detailed information
     * 3. Stores the STARFISH data temporarily
     * 4. Tracks the job execution in the database
     * 5. Sends email notifications for failures/warnings
     * 
     * @return ResponseEntity containing:
     *         - status: "success" or "error"
     *         - message: Description of the operation result
     *         - result: Details about the sync operation
     *         - timestamp: When the operation was performed
     */
    @PostMapping("/site-sync/trigger")
    public ResponseEntity<Map<String, Object>> triggerSiteSync() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            // Execute the site synchronization process
            String result = siteSyncService.syncSites();
            
            response.put("status", "success");
            response.put("message", "Site sync triggered successfully");
            response.put("result", result);
            response.put("timestamp", timestamp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to trigger site sync: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Retrieves the current status of site synchronization
     * 
     * This endpoint provides information about:
     * - Whether site sync is enabled
     * - The cron expression used for scheduling
     * - Details of the last execution (if any)
     * - Current service status
     * 
     * @return ResponseEntity containing:
     *         - status: Service status
     *         - message: Status description
     *         - enabled: Whether site sync is enabled
     *         - cronExpression: The scheduling pattern
     *         - lastExecution: Details of the last job execution
     *         - timestamp: When the status was checked
     */
    @GetMapping("/site-sync/status")
    public ResponseEntity<Map<String, Object>> getSiteSyncStatus() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        // Get the most recent job execution for site sync
        JobExecution lastExecution = siteSyncService.getLastJobExecution();
        
        response.put("status", "running");
        response.put("message", "Site sync service is running");
        response.put("timestamp", timestamp);
        response.put("enabled", scheduledTaskService.isSiteSyncEnabled());
        response.put("cronExpression", scheduledTaskService.getSiteSyncCron());
        
        if (lastExecution != null) {
            response.put("lastExecution", lastExecution.getStartTime().format(formatter));
            response.put("lastExecutionStatus", lastExecution.getStatus().toString());
            response.put("lastExecutionDuration", lastExecution.getDurationMs());
            response.put("lastExecutionRecordsProcessed", lastExecution.getRecordsProcessed());
            
            if (lastExecution.getErrorMessage() != null) {
                response.put("lastExecutionError", lastExecution.getErrorMessage());
            }
        } else {
            response.put("lastExecution", "Not executed yet");
            response.put("lastExecutionStatus", "N/A");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves recent job execution history
     * 
     * This endpoint returns a list of recent job executions with details such as:
     * - Job ID and name
     * - Start and end times
     * - Duration and status
     * - Number of records processed
     * - Error messages (if any)
     * 
     * @param limit Maximum number of job executions to return (default: 10)
     * @return ResponseEntity containing:
     *         - status: "success" or "error"
     *         - message: Description of the operation result
     *         - count: Number of executions returned
     *         - executions: List of JobExecution objects
     *         - timestamp: When the data was retrieved
     */
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getJobExecutions(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            // Retrieve recent job executions from the database
            List<JobExecution> executions = siteSyncService.getRecentJobExecutions(limit);
            
            response.put("status", "success");
            response.put("message", "Job executions retrieved successfully");
            response.put("timestamp", timestamp);
            response.put("count", executions.size());
            response.put("executions", executions);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to retrieve job executions: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Retrieves the overall scheduler status
     * 
     * This endpoint provides information about the main scheduler including:
     * - Current status (running/stopped)
     * - Schedule interval in minutes
     * - Last and next execution times
     * - API endpoint being called
     * 
     * @return ResponseEntity containing:
     *         - status: Scheduler status
     *         - message: Status description with interval
     *         - scheduleIntervalMinutes: How often the scheduler runs
     *         - apiUrl: The external API being called
     *         - lastExecution: When the last execution occurred
     *         - nextExecution: When the next execution is scheduled
     *         - timestamp: When the status was checked
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        // Get scheduling information
        LocalDateTime lastExecution = scheduledTaskService.getLastExecutionTime();
        LocalDateTime nextExecution = scheduledTaskService.getNextExecutionTime();
        long intervalMs = scheduledTaskService.getScheduleIntervalMs();
        
        response.put("status", "running");
        response.put("message", "Scheduler is running and will call API every " + (intervalMs / 60000) + " minutes");
        response.put("timestamp", timestamp);
        response.put("scheduleIntervalMinutes", intervalMs / 60000);
        response.put("apiUrl", "https://jsonplaceholder.typicode.com/posts/1");
        
        if (lastExecution != null) {
            response.put("lastExecution", lastExecution.format(formatter));
        } else {
            response.put("lastExecution", "Not executed yet");
        }
        
        if (nextExecution != null) {
            response.put("nextExecution", nextExecution.format(formatter));
        } else {
            response.put("nextExecution", "Not scheduled yet");
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint for the service
     * 
     * This endpoint provides a simple health check that can be used by:
     * - Load balancers to determine if the service is healthy
     * - Monitoring systems to check service availability
     * - Kubernetes health probes
     * 
     * @return ResponseEntity containing:
     *         - status: "UP" if the service is healthy
     *         - service: Service name
     *         - timestamp: When the health check was performed
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AMFK Starfish Sync API");
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return ResponseEntity.ok(response);
    }
} 
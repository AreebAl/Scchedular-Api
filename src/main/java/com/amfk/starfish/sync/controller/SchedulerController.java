package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.entity.JobExecution;
import com.amfk.starfish.sync.service.ApiCallService;
import com.amfk.starfish.sync.service.ScheduledTaskService;
import com.amfk.starfish.sync.service.SiteSyncService;
import com.amfk.starfish.sync.service.StarfishApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final ApiCallService apiCallService;
    private final ScheduledTaskService scheduledTaskService;
    private final SiteSyncService siteSyncService;
    private final StarfishApiClient starfishApiClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public SchedulerController(ApiCallService apiCallService, 
                              ScheduledTaskService scheduledTaskService,
                              SiteSyncService siteSyncService,
                              StarfishApiClient starfishApiClient) {
        this.apiCallService = apiCallService;
        this.scheduledTaskService = scheduledTaskService;
        this.siteSyncService = siteSyncService;
        this.starfishApiClient = starfishApiClient;
    }
    
    @GetMapping("/starfish/sites")
    public ResponseEntity<Map<String, Object>> getStarfishSites() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            List<Map<String, Object>> sites = starfishApiClient.getSites();
            
            if (sites != null) {
                response.put("status", "success");
                response.put("message", "Successfully retrieved sites from Starfish API");
                response.put("sites", sites);
                response.put("count", sites.size());
                response.put("timestamp", timestamp);
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to retrieve sites from Starfish API - null response");
                response.put("timestamp", timestamp);
                
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to retrieve sites from Starfish API: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerApiCall() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
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
    
    @PostMapping("/site-synch/trigger")
    public ResponseEntity<Map<String, Object>> triggerSiteSync() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
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
    
    @GetMapping("/site-synch/status")
    public ResponseEntity<Map<String, Object>> getSiteSyncStatus() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
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
    
    @GetMapping("/jobs")
    public ResponseEntity<Map<String, Object>> getJobExecutions(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
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
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
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
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AMFK Starfish Sync API");
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return ResponseEntity.ok(response);
    }
} 
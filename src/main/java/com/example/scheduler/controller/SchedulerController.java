package com.example.scheduler.controller;

import com.example.scheduler.service.ApiCallService;
import com.example.scheduler.service.ScheduledTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {
    
    private final ApiCallService apiCallService;
    private final ScheduledTaskService scheduledTaskService;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public SchedulerController(ApiCallService apiCallService, ScheduledTaskService scheduledTaskService) {
        this.apiCallService = apiCallService;
        this.scheduledTaskService = scheduledTaskService;
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
        response.put("service", "Scheduler API");
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return ResponseEntity.ok(response);
    }
} 
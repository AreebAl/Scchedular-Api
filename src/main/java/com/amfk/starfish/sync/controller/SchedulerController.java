package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.entity.JobExecution;

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
    
    private final ScheduledTaskService scheduledTaskService;
    private final SiteSyncService siteSyncService;
    private final StarfishApiClient starfishApiClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public SchedulerController(ScheduledTaskService scheduledTaskService,
                              SiteSyncService siteSyncService,
                              StarfishApiClient starfishApiClient) {
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
    

    


} 
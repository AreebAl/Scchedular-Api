package com.amfk.starfish.sync.controller;

import com.amfk.starfish.sync.service.SiteSyncService;
import com.amfk.starfish.sync.service.MasterServiceClient;
import com.amfk.starfish.sync.dto.SiteDto;
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
    
    private final SiteSyncService siteSyncService;
    private final MasterServiceClient masterServiceClient;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Autowired
    public SchedulerController(SiteSyncService siteSyncService, MasterServiceClient masterServiceClient) {
        this.siteSyncService = siteSyncService;
        this.masterServiceClient = masterServiceClient;
    }
    
    @GetMapping("/sites")
    public ResponseEntity<Map<String, Object>> getSites() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            List<SiteDto> sites = masterServiceClient.getSites();
            
            if (sites != null) {
                response.put("status", "success");
                response.put("message", "Successfully retrieved sites from Master Service API");
                response.put("sites", sites);
                response.put("count", sites.size());
                response.put("timestamp", timestamp);
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Failed to retrieve sites from Master Service API - null response");
                response.put("timestamp", timestamp);
                
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to retrieve sites from Master Service API: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/sync-sites")
    public ResponseEntity<Map<String, Object>> syncSites() {
        Map<String, Object> response = new HashMap<>();
        String timestamp = LocalDateTime.now().format(formatter);
        
        try {
            String result = siteSyncService.syncSites();
            
            response.put("status", "success");
            response.put("message", "Site sync job started successfully");
            response.put("result", result);
            response.put("timestamp", timestamp);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to start site sync job: " + e.getMessage());
            response.put("timestamp", timestamp);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
} 
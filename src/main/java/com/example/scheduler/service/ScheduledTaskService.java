package com.example.scheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ScheduledTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final ApiCallService apiCallService;
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private static final long SCHEDULE_INTERVAL_MS = 120000; // 2 minutes
    
    @Autowired
    public ScheduledTaskService(ApiCallService apiCallService) {
        this.apiCallService = apiCallService;
        // Initialize next execution time to 2 minutes from now
        this.nextExecutionTime = LocalDateTime.now().plusMinutes(2);
    }
    
    @Scheduled(fixedRate = SCHEDULE_INTERVAL_MS)
    public void scheduledApiCall() {
        String currentTime = LocalDateTime.now().format(formatter);
        logger.info("Starting scheduled API call at: {}", currentTime);
        
        this.lastExecutionTime = LocalDateTime.now();
        this.nextExecutionTime = this.lastExecutionTime.plusMinutes(2);
        
        apiCallService.callExternalApi();
        
        logger.info("Completed scheduled API call at: {}", currentTime);
        logger.info("Next scheduled execution at: {}", this.nextExecutionTime.format(formatter));
    }
    
    public LocalDateTime getLastExecutionTime() {
        return lastExecutionTime;
    }
    
    public LocalDateTime getNextExecutionTime() {
        return nextExecutionTime;
    }
    
    public long getScheduleIntervalMs() {
        return SCHEDULE_INTERVAL_MS;
    }
    
    // Alternative method using cron expression (every 2 hours)
    // @Scheduled(cron = "0 0 */2 * * ?")
    // public void scheduledApiCallWithCron() {
    //     String currentTime = LocalDateTime.now().format(formatter);
    //     logger.info("Starting scheduled API call (cron) at: {}", currentTime);
    //     
    //     apiCallService.callExternalApi();
    //     
    //     logger.info("Completed scheduled API call (cron) at: {}", currentTime);
    // }
} 
package com.amfk.starfish.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot Application class for AMFK Starfish Sync Service
 * 
 * This application provides:
 * - REST endpoints for SAMS integration
 * - Background sync jobs for data synchronization
 * - Site-related synchronization with STARFISH API
 * - Job tracking and monitoring capabilities
 * - Email notifications for job failures/warnings
 * 
 * Key Features:
 * - @EnableScheduling: Enables Spring's scheduling capabilities for background jobs
 * - @EnableAsync: Enables asynchronous method execution for non-blocking operations
 * - @EnableRetry: Enables retry mechanisms for transient failures
 * 
 * The application integrates with:
 * - Master Service: For pulling site/cluster data
 * - STARFISH API: For retrieving detailed site information
 * - Database: For job execution tracking
 * - Email Service: For notifications
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableRetry
public class SchedulerApplication {

	/**
	 * Main method to start the Spring Boot application
	 * 
	 * This method initializes the Spring application context and starts
	 * the embedded web server. The application will be available on
	 * the configured port (default: 8080).
	 * 
	 * @param args Command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(SchedulerApplication.class, args);
	}

}

# AMFK Starfish Sync Service

A Spring Boot application for synchronizing site data between Master Service and STARFISH API. This service implements background jobs for data synchronization with proper error handling, logging, and monitoring capabilities.

## Features

- **Background Sync Jobs**: Automated site synchronization between Master Service and STARFISH API
- **REST Endpoints**: API endpoints for SAMS integration and job monitoring
- **Job Tracking**: Database-based job execution tracking with status monitoring
- **Error Handling**: Comprehensive error handling with retry mechanisms
- **Notifications**: Email notifications for job failures and warnings
- **Configuration**: Environment-specific configuration files
- **HTTPS Support**: Truststore configuration for secure API communication
- **Monitoring**: Health checks and metrics via Spring Actuator

## Project Structure

```
src/main/java/com/example/scheduler/
├── controller/
│   └── SchedulerController.java          # REST endpoints
├── service/
│   ├── ApiCallService.java              # Legacy API service
│   ├── MasterServiceClient.java         # Master service REST client
│   ├── StarfishApiClient.java           # STARFISH API REST client
│   ├── SiteSyncService.java             # Site synchronization service
│   └── ScheduledTaskService.java        # Scheduled job orchestrator
├── entity/
│   └── JobExecution.java                # Job tracking entity
├── repository/
│   └── JobExecutionRepository.java      # Job execution repository
├── dto/
│   ├── SiteDto.java                     # Site data transfer object
│   └── StarfishSiteDto.java             # STARFISH site DTO
├── config/
│   └── RestClientConfig.java            # REST client configuration
└── SchedulerApplication.java            # Main application class
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Access to Master Service API
- Access to STARFISH API
- SMTP server for email notifications (optional)

## Configuration

### Environment Configuration Files

The application supports different environment configurations:

- `application.properties` - Default configuration
- `application-dev.properties` - Development environment
- `application-prod.properties` - Production environment

### Key Configuration Properties

#### Master Service Configuration
```properties
master.service.base.url=http://localhost:8081
master.service.api.key=your-api-key
master.service.timeout=30000
```

#### STARFISH API Configuration
```properties
starfish.api.base.url=https://api.starfish.com
starfish.api.username=your-username
starfish.api.password=your-password
starfish.api.timeout=60000
```

#### Site Sync Job Configuration
```properties
site.sync.enabled=true
site.sync.cron=0 0 */2 * * ?  # Every 2 hours
```

#### Email Configuration (for notifications)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

#### Truststore Configuration (for HTTPS)
```properties
rest.client.truststore.path=/path/to/truststore.jks
rest.client.truststore.password=truststore-password
```

## Running the Application

### Development Mode
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production Mode
```bash
mvn spring-boot:run -Dspring.profiles.active=prod
```

### Using JAR
```bash
mvn clean package
java -jar target/amfk-starfish-sync-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## API Endpoints

### Health Check
- `GET /api/scheduler/health` - Application health status

### Site Sync Endpoints
- `POST /api/scheduler/site-sync/trigger` - Manually trigger site sync
- `GET /api/scheduler/site-sync/status` - Get site sync status and last execution details

### Job Monitoring
- `GET /api/scheduler/jobs?limit=10` - Get recent job executions

### Legacy Endpoints (for backward compatibility)
- `POST /api/scheduler/trigger` - Trigger legacy API call
- `GET /api/scheduler/status` - Get scheduler status

### Actuator Endpoints
- `GET /actuator/health` - Detailed health information
- `GET /actuator/metrics` - Application metrics
- `GET /actuator/info` - Application information

## Database

The application uses H2 database for development and can be configured for other databases in production.

### H2 Console (Development)
Access the H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

## Job Execution Tracking

The application tracks job executions in the `job_executions` table with the following information:
- Job ID and name
- Start and end times
- Duration
- Status (RUNNING, COMPLETED, FAILED, CANCELLED)
- Records processed
- Error messages (if any)

## Monitoring and Notifications

### Email Notifications
The application sends email notifications for:
- Job failures
- Jobs completed with warnings (partial failures)

### Logging
Comprehensive logging is configured for:
- Job execution details
- API call results
- Error conditions
- Performance metrics

## Security Considerations

### HTTPS Configuration
For production environments with HTTPS STARFISH API:
1. Provision a truststore JKS file
2. Configure the truststore path and password in properties
3. The application will automatically use the truststore for HTTPS connections

### API Authentication
- Master Service: API key authentication
- STARFISH API: Basic authentication (configurable)

## Development Guidelines

### Adding New Sync Jobs
1. Create a new service class implementing the sync logic
2. Add the service to `ScheduledTaskService`
3. Configure the cron expression in properties
4. Add monitoring endpoints if needed

### Error Handling
- Use `@Retryable` annotation for transient failures
- Implement proper exception handling in services
- Log errors with appropriate levels
- Send notifications for critical failures

### Testing
- Unit tests for individual services
- Integration tests for API clients
- End-to-end tests for complete workflows

## Troubleshooting

### Common Issues

1. **Connection Timeouts**
   - Check network connectivity
   - Verify API endpoints are accessible
   - Adjust timeout configurations

2. **Authentication Failures**
   - Verify API credentials
   - Check API key format
   - Ensure proper authentication headers

3. **Job Failures**
   - Check application logs
   - Verify external service availability
   - Review error messages in job execution table

4. **Email Notifications Not Working**
   - Verify SMTP configuration
   - Check email credentials
   - Ensure network access to SMTP server

### Log Analysis
- Application logs: `./logs/amfk-starfish-sync.log` (production)
- Console logs: Check application startup logs
- Job execution logs: Query `job_executions` table

## Support

For issues and questions:
1. Check the application logs
2. Review the job execution table
3. Verify external service connectivity
4. Contact the development team

## License

This project is proprietary and confidential. 
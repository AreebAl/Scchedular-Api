# AMFK Starfish Sync Service

Spring Boot application for synchronizing site data between master services and the Starfish API.

## Recent Changes

- Starfish API calls and email notifications temporarily disabled
- Added endpoint to test Starfish API connectivity: `GET /api/scheduler/starfish/sites`
- Configured Bearer token authentication for Master Service
- Updated Master Service URL to: `https://linpubah043.gl.avaya.com:9003`

## Configuration

```properties
master.service.base.url=https://linpubah043.gl.avaya.com:9003
master.service.bearer.token=eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJCT1NDSF9VU0VSIiwiQk9TQ0hfQURNSU4iLCJBVkFZQV9BRE1JTiIsIkFWQVlBX0hPVExJTkUiLCJBVkFZQV9PUFMiXSwibmFtZSI6InNoZGh1bWFsIiwibGFuZ3VhZ2UiOiJlbiIsInN1YiI6InNoZGh1bWFsIiwiaWF0IjoxNzU0NTM2NTEzLCJleHAiOjE3NTQ1Mzc0MTN9.YhPp4w39YJD37w3iJymI-krfmFyseIZNYZy0m1zIUWU
```

## API Endpoints

### Test Starfish API
```bash
GET /api/scheduler/starfish/sites
```

### Trigger Site Sync (POST request)
```bash
POST /api/scheduler/site-synch/trigger
```

### Check Site Sync Status (GET request)
```bash
GET /api/scheduler/site-synch/status
```

### Health Check
```bash
GET /api/scheduler/health
```

## Running

1. Build: `./mvnw clean install`
2. Run: `./mvnw spring-boot:run`
3. Test Starfish API: `curl http://localhost:8080/api/scheduler/starfish/sites`
4. Trigger site sync: `curl -X POST http://localhost:8080/api/scheduler/site-synch/trigger`
5. Check status: `curl http://localhost:8080/api/scheduler/site-synch/status`

## CURL Reference
```bash
curl 'https://linpubah043.gl.avaya.com:9003/amsp/api/masterdata/v1/sites' \
  -H 'Accept: application/json' \
  -H 'Accept-Language: en-US,en;q=0.9' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJCT1NDSF9VU0VSIiwiQk9TQ0hfQURNSU4iLCJBVkFZQV9BRE1JTiIsIkFWQVlBX0hPVExJTkUiLCJBVkFZQV9PUFMiXSwibmFtZSI6InNoZGh1bWFsIiwibGFuZ3VhZ2UiOiJlbiIsInN1YiI6InNoZGh1bWFsIiwiaWF0IjoxNzU0NTM2NTEzLCJleHAiOjE3NTQ1Mzc0MTN9.YhPp4w39YJD37w3iJymI-krfmFyseIZNYZy0m1zIUWU' \
  -H 'Connection: keep-alive' \
  -H 'Referer: https://linpubah043.gl.avaya.com:9003/' \
  -H 'Sec-Fetch-Dest: empty' \
  -H 'Sec-Fetch-Mode: cors' \
  -H 'Sec-Fetch-Site: same-origin' \
  -H 'User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36' \
  -H 'sec-ch-ua: "Not)A;Brand";v="8", "Chromium";v="138", "Google Chrome";v="138"' \
  -H 'sec-ch-ua-mobile: ?0' \
  -H 'sec-ch-ua-platform: "Windows"'
```

## Database

MySQL Database Configuration:
- **Default**: `jdbc:mysql://localhost:3306/amfk_starfish_sync`
- **Development**: `jdbc:mysql://localhost:3306/amfk_starfish_sync_dev`
- **Production**: Uses environment variables for connection details

### MySQL Setup
1. Install MySQL Server
2. Create database: `CREATE DATABASE amfk_starfish_sync;`
3. Update credentials in `application.properties` or use environment variables
4. For production, set these environment variables:
   - `DB_HOST` - MySQL host (default: localhost)
   - `DB_PORT` - MySQL port (default: 3306)
   - `DB_USERNAME` - Database username (default: root)
   - `DB_PASSWORD` - Database password (default: password) 
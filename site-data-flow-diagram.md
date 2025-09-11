# Site Data Flow - UML Class Diagram

## Complete Flow Sequence
```
AMSP API → SiteService → Admin Portal → ProvisioningService → Admin Portal Table
```

## UML Class Diagram

```mermaid
classDiagram
    %% Data Models
    class Site {
        -String id
        -String name
        -String city
        -String clusterName
        -String status
        -String siteCode
        -String street
        -String location
        -String clusterId
        +getId() String
        +getName() String
        +getCity() String
        +getClusterName() String
        +getStatus() String
        +getSiteCode() String
        +getStreet() String
        +getLocation() String
        +getClusterId() String
        +setId(String) void
        +setName(String) void
        +setCity(String) void
        +setClusterName(String) void
        +setStatus(String) void
        +setSiteCode(String) String
        +setStreet(String) void
        +setLocation(String) void
        +setClusterId(String) void
    }

    class ProvisioningSite {
        -String siteName
        -String siteId
        -String status
        -List~Range~ ranges
        -List~Extension~ extensions
        +getSiteName() String
        +getSiteId() String
        +getStatus() String
        +getRanges() List~Range~
        +getExtensions() List~Extension~
        +setSiteName(String) void
        +setSiteId(String) void
        +setStatus(String) void
        +setRanges(List~Range~) void
        +setExtensions(List~Extension~) void
    }

    class Range {
        -String type
        -String lowerbound
        -String upperbound
        -String prefix
        -int availableExtensions
        +getType() String
        +getLowerbound() String
        +getUpperbound() String
        +getPrefix() String
        +getAvailableExtensions() int
        +setType(String) void
        +setLowerbound(String) void
        +setUpperbound(String) void
        +setPrefix(String) void
        +setAvailableExtensions(int) void
    }

    class Extension {
        -String type
        -String value
        -String status
        -String description
        +getType() String
        +getValue() String
        +getStatus() String
        +getDescription() String
        +setType(String) void
        +setValue(String) void
        +setStatus(String) void
        +setDescription(String) void
    }

    %% Controllers
    class SiteController {
        -SiteService siteService
        -ProvisioningService provisioningService
        +getAllSites() ResponseEntity~List~Site~~
        +getSiteDetails(String siteName) ResponseEntity~ProvisioningSite~
        +displaySitesInTable() String
        +handleSiteSelection(String siteName) String
    }

    %% Services
    class SiteService {
        -AMSPClient amspClient
        -List~Site~ cachedSites
        +fetchAllSites() List~Site~
        +getSiteByName(String name) Site
        +refreshSitesCache() void
        +validateSiteData(List~Site~ sites) boolean
    }

    class ProvisioningService {
        -ProvisioningClient provisioningClient
        -ErrorHandler errorHandler
        +getSiteDetails(String siteName) ProvisioningSite
        +handleProvisioningError(Exception e) ErrorResponse
        +validateSiteName(String siteName) boolean
        +processProvisioningResponse(ProvisioningSite site) ProvisioningSite
    }

    %% API Clients
    class AMSPClient {
        -RestTemplate restTemplate
        -String baseUrl
        -HttpHeaders headers
        +getSites() List~Site~
        +makeHttpRequest(String endpoint) ResponseEntity~String~
        +handleHttpError(HttpStatusCode status) void
        +setAuthentication(String token) void
    }

    class ProvisioningClient {
        -RestTemplate restTemplate
        -String baseUrl
        -HttpHeaders headers
        +getSiteDetails(String siteName) ProvisioningSite
        +makeHttpRequest(String endpoint) ResponseEntity~String~
        +handleHttpError(HttpStatusCode status) void
        +setAuthentication(String token) void
    }

    %% Error Handling
    class ErrorHandler {
        +handle400Error(String message) ErrorResponse
        +handle404Error(String message) ErrorResponse
        +handle500Error(String message) ErrorResponse
        +logError(String operation, Exception e) void
        +createErrorResponse(String code, String message) ErrorResponse
    }

    class ErrorResponse {
        -String errorCode
        -String message
        -String timestamp
        -String operation
        +getErrorCode() String
        +getMessage() String
        +getTimestamp() String
        +getOperation() String
        +setErrorCode(String) void
        +setMessage(String) void
        +setTimestamp(String) void
        +setOperation(String) void
    }

    %% Admin Portal Components
    class AdminPortal {
        -SiteController siteController
        -String currentView
        +displaySitesTable() void
        +handleSiteSelection(String siteName) void
        +displaySiteDetails(ProvisioningSite site) void
        +showError(String errorMessage) void
        +refreshData() void
    }

    %% Relationships
    SiteController --> SiteService : uses
    SiteController --> ProvisioningService : uses
    SiteService --> AMSPClient : uses
    ProvisioningService --> ProvisioningClient : uses
    ProvisioningService --> ErrorHandler : uses
    AMSPClient --> Site : creates
    ProvisioningClient --> ProvisioningSite : creates
    ProvisioningSite --> Range : contains
    ProvisioningSite --> Extension : contains
    ErrorHandler --> ErrorResponse : creates
    AdminPortal --> SiteController : uses

    %% Flow Annotations
    note for SiteController "1. Receives request for sites list<br/>2. Calls SiteService.fetchAllSites()<br/>3. Returns sites to Admin Portal"
    note for SiteService "1. Calls AMSPClient.getSites()<br/>2. Validates and caches results<br/>3. Returns List~Site~"
    note for AMSPClient "1. Makes HTTP GET to /amsp/api/masterdata/v1/sites<br/>2. Maps response to Site objects<br/>3. Handles HTTP errors"
    note for ProvisioningService "1. Validates siteName parameter<br/>2. Calls ProvisioningClient.getSiteDetails()<br/>3. Handles errors (400, 404, 500)<br/>4. Returns ProvisioningSite"
    note for ProvisioningClient "1. Makes HTTP GET to /sps/v1/site?SiteName={siteName}<br/>2. Maps response to ProvisioningSite<br/>3. Handles HTTP errors"
```

## Visual Flow Diagram

```mermaid
flowchart TD
    %% Start
    Start([User Opens Admin Portal]) --> LoadSites[Load Sites List]
    
    %% AMSP API Flow
    LoadSites --> AMSPClient[AMSP Client]
    AMSPClient --> AMSPAPI[AMSP API<br/>/amsp/api/masterdata/v1/sites]
    AMSPAPI --> SitesData[Site Data<br/>id, name, city, clusterName, etc.]
    SitesData --> SiteService[Site Service]
    SiteService --> SiteController[Site Controller]
    SiteController --> AdminPortal[Admin Portal<br/>Display Sites Table/Dropdown]
    
    %% User Selection
    AdminPortal --> UserSelects{User Selects Site}
    UserSelects -->|Site Name| ProvisioningService[Provisioning Service]
    
    %% Provisioning API Flow
    ProvisioningService --> ValidateSite{Validate Site Name}
    ValidateSite -->|Valid| ProvisioningClient[Provisioning Client]
    ValidateSite -->|Invalid| ErrorHandler[Error Handler]
    
    ProvisioningClient --> ProvisioningAPI[Provisioning API<br/>/sps/v1/site?SiteName={siteName}]
    ProvisioningAPI --> APIResponse{API Response}
    
    %% Success Path
    APIResponse -->|Success 200| ProvisioningData[Provisioning Data<br/>Site, CM, Ranges, Extensions]
    ProvisioningData --> ProcessData[Process & Format Data]
    ProcessData --> DisplayDetails[Display Site Details<br/>in Admin Portal Table]
    DisplayDetails --> End([End])
    
    %% Error Paths
    APIResponse -->|400 Bad Request| Error400[400 Error Handler]
    APIResponse -->|404 Not Found| Error404[404 Error Handler]
    APIResponse -->|500 Server Error| Error500[500 Error Handler]
    
    Error400 --> ErrorHandler
    Error404 --> ErrorHandler
    Error500 --> ErrorHandler
    ErrorHandler --> ShowError[Show Error Message<br/>in Admin Portal]
    ShowError --> End
    
    %% Styling
    classDef apiBox fill:#e1f5fe,stroke:#01579b,stroke-width:2px
    classDef serviceBox fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef controllerBox fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef dataBox fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef errorBox fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef userBox fill:#f1f8e9,stroke:#33691e,stroke-width:2px
    
    class AMSPAPI,ProvisioningAPI apiBox
    class SiteService,ProvisioningService serviceBox
    class SiteController controllerBox
    class SitesData,ProvisioningData,ProcessData dataBox
    class ErrorHandler,Error400,Error404,Error500,ShowError errorBox
    class Start,End,AdminPortal,UserSelects,DisplayDetails userBox
```

## Detailed Component Flow Diagram

```mermaid
flowchart LR
    subgraph "External APIs"
        AMSP[AMSP API<br/>📡 Master Data]
        PROV[Provisioning API<br/>📡 Site Details]
    end
    
    subgraph "Backend Services"
        subgraph "API Clients"
            AMSPC[AMSP Client<br/>🔌 HTTP Client]
            PROVC[Provisioning Client<br/>🔌 HTTP Client]
        end
        
        subgraph "Business Logic"
            SS[Site Service<br/>⚙️ Business Logic]
            PS[Provisioning Service<br/>⚙️ Business Logic]
        end
        
        subgraph "Controllers"
            SC[Site Controller<br/>🎮 REST Endpoints]
        end
        
        subgraph "Error Handling"
            EH[Error Handler<br/>⚠️ Error Management]
        end
    end
    
    subgraph "Frontend"
        AP[Admin Portal<br/>🖥️ User Interface]
    end
    
    subgraph "Data Models"
        SITE[Site Model<br/>📋 id, name, city, etc.]
        PROVSITE[Provisioning Site<br/>📋 Site, CM, Ranges, Extensions]
        ERROR[Error Response<br/>📋 code, message, timestamp]
    end
    
    %% Flow connections
    AP -->|1. Request Sites| SC
    SC -->|2. Fetch Sites| SS
    SS -->|3. Call API| AMSPC
    AMSPC -->|4. HTTP GET| AMSP
    AMSP -->|5. Site Data| AMSPC
    AMSPC -->|6. Site Objects| SS
    SS -->|7. List<Site>| SC
    SC -->|8. JSON Response| AP
    
    AP -->|9. User Selects Site| SC
    SC -->|10. Get Details| PS
    PS -->|11. Call API| PROVC
    PROVC -->|12. HTTP GET| PROV
    PROV -->|13. Site Details| PROVC
    PROVC -->|14. Provisioning Data| PS
    PS -->|15. Processed Data| SC
    SC -->|16. Final Response| AP
    
    %% Error flows
    PROVC -.->|Error| EH
    PS -.->|Validation Error| EH
    EH -.->|Error Response| SC
    SC -.->|Error| AP
    
    %% Data model connections
    AMSPC -.->|Creates| SITE
    PROVC -.->|Creates| PROVSITE
    EH -.->|Creates| ERROR
    
    %% Styling
    classDef apiStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:3px
    classDef serviceStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef controllerStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef dataStyle fill:#fff8e1,stroke:#f57c00,stroke-width:2px
    classDef errorStyle fill:#ffebee,stroke:#d32f2f,stroke-width:2px
    classDef frontendStyle fill:#f1f8e9,stroke:#689f38,stroke-width:3px
    
    class AMSP,PROV apiStyle
    class SS,PS,AMSPC,PROVC serviceStyle
    class SC controllerStyle
    class SITE,PROVSITE,ERROR dataStyle
    class EH errorStyle
    class AP frontendStyle
```

## Step-by-Step Process Flow

```mermaid
flowchart TD
    Step1[Step 1: User Opens Admin Portal] --> Step2[Step 2: Load Sites List]
    Step2 --> Step3[Step 3: Call AMSP API]
    Step3 --> Step4[Step 4: Receive Site Data]
    Step4 --> Step5[Step 5: Display Sites in Table/Dropdown]
    Step5 --> Step6[Step 6: User Selects a Site]
    Step6 --> Step7[Step 7: Call Provisioning API]
    Step7 --> Step8{Step 8: Check API Response}
    Step8 -->|Success| Step9[Step 9: Process Site Details]
    Step8 -->|Error 400| Step10[Step 10: Handle Bad Request]
    Step8 -->|Error 404| Step11[Step 11: Handle Not Found]
    Step8 -->|Error 500| Step12[Step 12: Handle Server Error]
    Step9 --> Step13[Step 13: Display Site Details in Table]
    Step10 --> Step14[Step 14: Show Error Message]
    Step11 --> Step14
    Step12 --> Step14
    Step13 --> Step15[Step 15: Process Complete]
    Step14 --> Step15
    
    %% Styling
    classDef stepBox fill:#e8f5e8,stroke:#2e7d32,stroke-width:2px
    classDef errorStep fill:#ffebee,stroke:#c62828,stroke-width:2px
    classDef successStep fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    
    class Step1,Step2,Step3,Step4,Step5,Step6,Step7 stepBox
    class Step8,Step9,Step13,Step15 successStep
    class Step10,Step11,Step12,Step14 errorStep
```

## API Endpoints

### AMSP API
- **Endpoint**: `/amsp/api/masterdata/v1/sites`
- **Method**: GET
- **Response**: Array of Site objects with fields: id, name, city, clusterName, status, siteCode, street, location, clusterId

### Provisioning Service API
- **Endpoint**: `/sps/v1/site?SiteName={SiteName}`
- **Method**: GET
- **Response**: ProvisioningSite object containing:
  - Site details
  - CM (Communication Manager) details
  - Ranges array with Type, Lowerbound, Upperbound, Prefix, AvailableExtensions
  - Extensions array with Type, Value, Status, Description

## Error Handling Strategy

1. **400 Bad Request**: Invalid siteName parameter
2. **404 Not Found**: Site not found in provisioning service
3. **500 Internal Server Error**: Server-side issues
4. **Network Errors**: Connection timeouts, DNS resolution failures
5. **Data Validation**: Invalid response format, missing required fields

## Key Design Patterns

1. **Service Layer Pattern**: Separation of business logic from controllers
2. **Client Pattern**: Dedicated clients for external API communication
3. **Error Handler Pattern**: Centralized error handling and response formatting
4. **DTO Pattern**: Data Transfer Objects for API communication
5. **Repository Pattern**: Data access abstraction (if database caching is needed)

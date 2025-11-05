# OpenL Tablets Public API Reference

**Last Updated**: 2025-11-05
**Version**: 6.0.0-SNAPSHOT
**API Type**: REST (JSON)

---

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [API Conventions](#api-conventions)
- [Studio REST API](#studio-rest-api)
- [Rule Services API](#rule-services-api)
- [Admin API](#admin-api)
- [OpenAPI Integration](#openapi-integration)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Examples](#examples)

---

## Overview

OpenL Tablets provides comprehensive REST APIs for:

1. **Studio API**: Project management, deployments, users
2. **Rule Services API**: Rule execution, service management
3. **Admin API**: System administration, monitoring

### API Base URLs

| Service | Base URL | Description |
|---------|----------|-------------|
| **Studio** | `/api` | Project and user management |
| **Rule Services** | `/` | Rule execution endpoints |
| **Admin** | `/admin` | System administration |

### API Versioning

OpenL Tablets uses path-based versioning for stable APIs:

```
/api/v1/projects    # Version 1 API
/api/v2/projects    # Version 2 API
```

**Note**: APIs without version prefix are considered **Beta** and may change.

---

## Authentication

### Authentication Methods

| Method | Use Case | Header |
|--------|----------|--------|
| **Basic Auth** | Simple authentication | `Authorization: Basic base64(user:pass)` |
| **OAuth2** | Enterprise SSO | `Authorization: Bearer <token>` |
| **API Key** | Service-to-service | `X-API-Key: <key>` |

### Basic Authentication

```bash
# Encode credentials
echo -n "admin:admin" | base64
# Output: YWRtaW46YWRtaW4=

# Use in request
curl -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  http://localhost:8080/api/projects
```

### OAuth2 Authentication

```bash
# 1. Get access token
TOKEN=$(curl -X POST \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=openl-client" \
  -d "client_secret=secret" \
  https://auth.example.com/oauth/token \
  | jq -r '.access_token')

# 2. Use token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/projects
```

---

## API Conventions

### Request Format

```http
GET /api/projects HTTP/1.1
Host: localhost:8080
Accept: application/json
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/json
```

### Response Format

```json
{
  "status": "success",
  "data": {
    "id": "project-123",
    "name": "my-project"
  },
  "meta": {
    "timestamp": "2025-11-05T10:00:00Z",
    "version": "6.0.0"
  }
}
```

### Pagination

```http
GET /api/projects?page=0&size=20&sort=name,asc HTTP/1.1
```

Response:
```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalPages": 5,
  "totalElements": 100,
  "first": true,
  "last": false
}
```

### Filtering

```http
GET /api/projects?name=my-project&status=active HTTP/1.1
```

### Field Selection

```http
GET /api/projects?fields=id,name,version HTTP/1.1
```

---

## Studio REST API

### Projects API

#### List Projects

```http
GET /api/projects HTTP/1.1
```

**Response**:
```json
[
  {
    "id": "project-1",
    "name": "Insurance Rules",
    "version": "1.0.0",
    "status": "active",
    "created": "2025-01-01T00:00:00Z",
    "modified": "2025-11-05T10:00:00Z"
  }
]
```

#### Get Project

```http
GET /api/projects/{id} HTTP/1.1
```

**Response**:
```json
{
  "id": "project-1",
  "name": "Insurance Rules",
  "version": "1.0.0",
  "description": "Insurance calculation rules",
  "status": "active",
  "modules": [
    {
      "name": "premiums",
      "path": "rules/premiums.xlsx"
    }
  ],
  "dependencies": [],
  "properties": {
    "author": "John Doe",
    "category": "Insurance"
  },
  "created": "2025-01-01T00:00:00Z",
  "modified": "2025-11-05T10:00:00Z"
}
```

#### Create Project

```http
POST /api/projects HTTP/1.1
Content-Type: application/json

{
  "name": "New Project",
  "version": "1.0.0",
  "description": "Project description"
}
```

**Response**: `201 Created`
```json
{
  "id": "project-2",
  "name": "New Project",
  "version": "1.0.0",
  "status": "draft"
}
```

#### Update Project

```http
PATCH /api/projects/{id} HTTP/1.1
Content-Type: application/json

{
  "description": "Updated description",
  "version": "1.1.0"
}
```

**Response**: `200 OK`

#### Delete Project

```http
DELETE /api/projects/{id} HTTP/1.1
```

**Response**: `204 No Content`

### Deployments API

#### List Deployments

```http
GET /api/deployments HTTP/1.1
```

**Response**:
```json
[
  {
    "id": "deployment-1",
    "projectName": "Insurance Rules",
    "projectVersion": "1.0.0",
    "environment": "production",
    "status": "deployed",
    "deployedAt": "2025-11-05T10:00:00Z"
  }
]
```

#### Create Deployment

```http
POST /api/deployments HTTP/1.1
Content-Type: application/json

{
  "projectId": "project-1",
  "environment": "production",
  "deployPath": "/insurance"
}
```

**Response**: `201 Created`

#### Get Deployment Status

```http
GET /api/deployments/{id}/status HTTP/1.1
```

**Response**:
```json
{
  "id": "deployment-1",
  "status": "deployed",
  "health": "healthy",
  "uptime": 3600,
  "requestCount": 1234
}
```

### Users API

#### List Users

```http
GET /api/users HTTP/1.1
```

**Response**:
```json
[
  {
    "id": "user-1",
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "Admin",
    "lastName": "User",
    "roles": ["ADMIN", "USER"],
    "active": true
  }
]
```

#### Create User

```http
POST /api/users HTTP/1.1
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@example.com",
  "firstName": "New",
  "lastName": "User",
  "password": "password123",
  "roles": ["USER"]
}
```

**Response**: `201 Created`

#### Update User

```http
PATCH /api/users/{id} HTTP/1.1
Content-Type: application/json

{
  "email": "newemail@example.com",
  "roles": ["USER", "DEVELOPER"]
}
```

**Response**: `200 OK`

#### Delete User

```http
DELETE /api/users/{id} HTTP/1.1
```

**Response**: `204 No Content`

### Repositories API

#### List Repositories

```http
GET /api/repositories HTTP/1.1
```

**Response**:
```json
[
  {
    "id": "design-repo",
    "name": "Design Repository",
    "type": "git",
    "uri": "file:///path/to/repo",
    "status": "connected"
  }
]
```

#### Test Connection

```http
POST /api/repositories/{id}/test-connection HTTP/1.1
```

**Response**:
```json
{
  "success": true,
  "message": "Connection successful"
}
```

### Settings API

#### Get Settings

```http
GET /api/settings HTTP/1.1
```

**Response**:
```json
{
  "userMode": "multi",
  "demoMode": false,
  "maxUploadSize": 52428800,
  "sessionTimeout": 1800
}
```

#### Update Settings

```http
PATCH /api/settings HTTP/1.1
Content-Type: application/json

{
  "sessionTimeout": 3600
}
```

**Response**: `200 OK`

---

## Rule Services API

### Execute Rules

#### POST - Execute Rule

```http
POST /{deployPath}/{serviceName}/{methodName} HTTP/1.1
Content-Type: application/json

{
  "age": 25,
  "gender": "M",
  "smokingStatus": "non-smoker"
}
```

**Response**:
```json
{
  "premium": 120.50,
  "riskCategory": "low",
  "approved": true
}
```

#### GET - Execute Rule (Query Parameters)

```http
GET /{deployPath}/{serviceName}/{methodName}?age=25&gender=M HTTP/1.1
```

**Response**: Same as POST

### Service Discovery

#### List Services

```http
GET /admin/services HTTP/1.1
```

**Response**:
```json
[
  {
    "name": "InsuranceRulesService",
    "url": "/insurance",
    "methods": [
      {
        "name": "calculatePremium",
        "parameters": [
          {
            "name": "age",
            "type": "int"
          },
          {
            "name": "gender",
            "type": "String"
          }
        ],
        "returnType": "PremiumResult"
      }
    ],
    "status": "active"
  }
]
```

#### Get Service Info

```http
GET /admin/ui/info/{serviceName} HTTP/1.1
```

**Response**:
```json
{
  "name": "InsuranceRulesService",
  "version": "1.0.0",
  "project": "Insurance Rules",
  "deployPath": "/insurance",
  "status": "active",
  "uptime": 3600,
  "statistics": {
    "requestCount": 1234,
    "errorCount": 5,
    "avgResponseTime": 50
  }
}
```

### OpenAPI Specification

#### Get OpenAPI Spec

```http
GET /{deployPath}/openapi.json HTTP/1.1
```

**Response**: OpenAPI 3.0 specification

```json
{
  "openapi": "3.0.0",
  "info": {
    "title": "InsuranceRulesService",
    "version": "1.0.0"
  },
  "paths": {
    "/calculatePremium": {
      "post": {
        "operationId": "calculatePremium",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/PremiumRequest"
              }
            }
          }
        },
        "responses": {
          "200": {
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/PremiumResult"
                }
              }
            }
          }
        }
      }
    }
  }
}
```

---

## Admin API

### System Information

#### Get System Info

```http
GET /admin/info/sys.json HTTP/1.1
```

**Response**:
```json
{
  "jvm": {
    "version": "21.0.1",
    "vendor": "Eclipse Adoptium",
    "heapMemory": {
      "used": 524288000,
      "max": 2147483648,
      "percentage": 24.4
    },
    "nonHeapMemory": {
      "used": 104857600,
      "max": 536870912
    },
    "threads": {
      "live": 42,
      "peak": 50,
      "daemon": 38
    }
  },
  "os": {
    "name": "Linux",
    "version": "5.15.0",
    "arch": "amd64",
    "processors": 8
  },
  "runtime": {
    "uptime": 3600000,
    "startTime": "2025-11-05T10:00:00Z"
  }
}
```

#### Get OpenL Info

```http
GET /admin/info/openl.json HTTP/1.1
```

**Response**:
```json
{
  "version": "6.0.0-SNAPSHOT",
  "buildDate": "2025-11-05",
  "gitCommit": "1abf2b0",
  "environment": "production",
  "configuration": {
    "userMode": "multi",
    "repositoryType": "git"
  }
}
```

### Service Management

#### Deploy Service

```http
POST /admin/services/{serviceName}/deploy HTTP/1.1
Content-Type: application/json

{
  "projectName": "Insurance Rules",
  "version": "1.0.0",
  "deployPath": "/insurance"
}
```

**Response**: `201 Created`

#### Undeploy Service

```http
DELETE /admin/services/{serviceName} HTTP/1.1
```

**Response**: `204 No Content`

#### Reload Service

```http
POST /admin/services/{serviceName}/reload HTTP/1.1
```

**Response**: `200 OK`

### Health Checks

#### Health Check

```http
GET /admin/health HTTP/1.1
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP"
    },
    "repository": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "free": 107374182400,
        "threshold": 10485760
      }
    }
  }
}
```

#### Readiness Check

```http
GET /admin/health/readiness HTTP/1.1
```

#### Liveness Check

```http
GET /admin/health/liveness HTTP/1.1
```

### Metrics

#### Get Metrics

```http
GET /admin/metrics HTTP/1.1
```

**Response**:
```json
{
  "jvm.memory.used": 524288000,
  "jvm.threads.live": 42,
  "http.server.requests": 1234,
  "rule.execution.time": 50
}
```

#### Get Specific Metric

```http
GET /admin/metrics/jvm.memory.used HTTP/1.1
```

**Response**:
```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 524288000
    }
  ],
  "availableTags": [
    {
      "tag": "area",
      "values": ["heap", "nonheap"]
    }
  ]
}
```

---

## OpenAPI Integration

### Swagger UI

Access Swagger UI at:
```
http://localhost:8080/{deployPath}/swagger-ui/
```

### OpenAPI Annotations

```java
@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project management API")
public class ProjectsController {

    @Operation(
        summary = "Get project by ID",
        description = "Returns a single project by its ID"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Project found",
            content = @Content(
                schema = @Schema(implementation = ProjectDTO.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Project not found"
        )
    })
    @GetMapping("/{id}")
    public ProjectDTO getProject(
        @Parameter(description = "Project ID")
        @PathVariable String id
    ) {
        return projectService.findById(id);
    }
}
```

---

## Error Handling

### Error Response Format

```json
{
  "timestamp": "2025-11-05T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid project name",
  "path": "/api/projects",
  "details": [
    {
      "field": "name",
      "message": "Name cannot be empty"
    }
  ]
}
```

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| **200** | OK | Successful GET, PATCH |
| **201** | Created | Successful POST |
| **204** | No Content | Successful DELETE |
| **400** | Bad Request | Invalid input |
| **401** | Unauthorized | Missing/invalid credentials |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Resource not found |
| **409** | Conflict | Resource already exists |
| **422** | Unprocessable Entity | Validation failed |
| **500** | Internal Server Error | Server error |
| **503** | Service Unavailable | Service temporarily unavailable |

### Common Error Codes

| Error Code | Message | Resolution |
|------------|---------|------------|
| `PROJECT_NOT_FOUND` | Project not found | Check project ID |
| `INVALID_PROJECT_NAME` | Invalid project name | Use alphanumeric characters |
| `PROJECT_ALREADY_EXISTS` | Project already exists | Use different name |
| `COMPILATION_FAILED` | Rule compilation failed | Check rule syntax |
| `DEPLOYMENT_FAILED` | Deployment failed | Check deployment logs |
| `PERMISSION_DENIED` | Permission denied | Check user permissions |

---

## Rate Limiting

### Rate Limit Headers

```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1609459200
```

### Rate Limit Exceeded

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/json

{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Please try again in 60 seconds.",
  "retryAfter": 60
}
```

---

## Examples

### Example: Create and Deploy Project

```bash
#!/bin/bash

# 1. Create project
PROJECT_ID=$(curl -s -X POST \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Insurance Rules",
    "version": "1.0.0",
    "description": "Insurance calculation rules"
  }' \
  http://localhost:8080/api/projects \
  | jq -r '.id')

echo "Created project: $PROJECT_ID"

# 2. Upload rules
curl -X POST \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -F "file=@rules/insurance.xlsx" \
  http://localhost:8080/api/projects/$PROJECT_ID/files

# 3. Deploy project
curl -X POST \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "'$PROJECT_ID'",
    "environment": "production",
    "deployPath": "/insurance"
  }' \
  http://localhost:8080/api/deployments

# 4. Test rule execution
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "age": 25,
    "gender": "M",
    "smokingStatus": "non-smoker"
  }' \
  http://localhost:8080/insurance/InsuranceRulesService/calculatePremium

echo "Deployment complete!"
```

### Example: List Projects with Pagination

```javascript
// JavaScript example
async function listProjects(page = 0, size = 20) {
  const response = await fetch(
    `http://localhost:8080/api/projects?page=${page}&size=${size}`,
    {
      headers: {
        'Authorization': 'Basic ' + btoa('admin:admin'),
        'Accept': 'application/json'
      }
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  const data = await response.json();
  console.log(`Total projects: ${data.totalElements}`);
  return data.content;
}

// Usage
listProjects(0, 20)
  .then(projects => {
    projects.forEach(project => {
      console.log(`${project.name} (${project.version})`);
    });
  })
  .catch(error => console.error('Error:', error));
```

### Example: Execute Rule with Error Handling

```python
import requests
import json

def execute_rule(service_name, method_name, params):
    """Execute OpenL rule with error handling"""
    url = f"http://localhost:8080/insurance/{service_name}/{method_name}"

    try:
        response = requests.post(
            url,
            json=params,
            headers={'Content-Type': 'application/json'},
            timeout=30
        )

        response.raise_for_status()
        return response.json()

    except requests.exceptions.HTTPError as e:
        if e.response.status_code == 404:
            print(f"Rule not found: {method_name}")
        elif e.response.status_code == 400:
            print(f"Invalid input: {e.response.json()}")
        else:
            print(f"HTTP Error: {e}")
        raise

    except requests.exceptions.Timeout:
        print("Request timeout")
        raise

    except requests.exceptions.RequestException as e:
        print(f"Request failed: {e}")
        raise

# Usage
try:
    result = execute_rule(
        "InsuranceRulesService",
        "calculatePremium",
        {
            "age": 25,
            "gender": "M",
            "smokingStatus": "non-smoker"
        }
    )
    print(f"Premium: ${result['premium']}")
except Exception as e:
    print(f"Failed to calculate premium: {e}")
```

---

## SDK and Client Libraries

### Java Client

```java
// Example using Spring RestTemplate
@Service
public class OpenLClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OpenLClient() {
        this.baseUrl = "http://localhost:8080";
        this.restTemplate = new RestTemplate();

        // Add basic auth
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBasicAuth("admin", "admin");
            return execution.execute(request, body);
        });
    }

    public List<Project> listProjects() {
        String url = baseUrl + "/api/projects";
        return Arrays.asList(
            restTemplate.getForObject(url, Project[].class)
        );
    }

    public <T> T executeRule(String serviceName, String methodName, Object params, Class<T> responseType) {
        String url = String.format("%s/%s/%s/%s",
            baseUrl, "insurance", serviceName, methodName);

        return restTemplate.postForObject(url, params, responseType);
    }
}
```

### TypeScript Client

```typescript
// TypeScript client
export class OpenLClient {
  private baseUrl: string;
  private auth: string;

  constructor(baseUrl: string, username: string, password: string) {
    this.baseUrl = baseUrl;
    this.auth = btoa(`${username}:${password}`);
  }

  async listProjects(): Promise<Project[]> {
    const response = await fetch(`${this.baseUrl}/api/projects`, {
      headers: {
        'Authorization': `Basic ${this.auth}`,
        'Accept': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }

  async executeRule<T>(
    serviceName: string,
    methodName: string,
    params: any
  ): Promise<T> {
    const url = `${this.baseUrl}/insurance/${serviceName}/${methodName}`;

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return response.json();
  }
}
```

---

## Related Documentation

- [Testing Guide](../guides/testing-guide.md) - API testing
- [CI/CD Pipeline](../operations/ci-cd.md) - API deployment
- [Docker Guide](../operations/docker-guide.md) - API containerization
- [STUDIO CLAUDE.md](/STUDIO/CLAUDE.md) - REST API development guidelines

---

**Last Updated**: 2025-11-05
**Maintainer**: OpenL Tablets Team

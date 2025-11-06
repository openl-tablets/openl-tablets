# OpenL Tablets - API Integration Guide

**Version**: 6.0.0-SNAPSHOT
**Last Updated**: 2025-11-06

---

## Table of Contents

- [Overview](#overview)
- [Quick Start](#quick-start)
- [Authentication](#authentication)
- [REST API Reference](#rest-api-reference)
- [SOAP API Reference](#soap-api-reference)
- [OpenAPI/Swagger Documentation](#openapiswagger-documentation)
- [Rule Service Endpoints](#rule-service-endpoints)
- [Studio API Endpoints](#studio-api-endpoints)
- [Integration Examples](#integration-examples)
- [Error Handling](#error-handling)
- [Rate Limiting](#rate-limiting)
- [Best Practices](#best-practices)

---

## Overview

OpenL Tablets provides multiple API interfaces for integrating business rules into your applications:

| API Type | Use Case | Protocol | Documentation |
|----------|----------|----------|---------------|
| **REST** | Modern applications, microservices | HTTP/JSON | OpenAPI 3.0 |
| **SOAP** | Legacy enterprise integration | SOAP/XML | WSDL |
| **Java** | Embedded rules in Java apps | Native Java | JavaDoc |
| **MCP** | AI assistant integration | Model Context Protocol | MCP Schema |

### Architecture

```
┌─────────────────┐
│  Your App       │
│                 │
└────────┬────────┘
         │ HTTP Request
         │
┌────────▼────────────────────────────────────────┐
│        OpenL Rule Service                       │
│                                                  │
│  ┌────────────┐  ┌────────────┐  ┌──────────┐ │
│  │ REST       │  │ SOAP       │  │ OpenAPI  │ │
│  │ Controller │  │ Endpoint   │  │ Docs     │ │
│  └─────┬──────┘  └─────┬──────┘  └──────────┘ │
│        │               │                        │
│  ┌─────▼───────────────▼──────┐                │
│  │  Rules Engine               │                │
│  │  (Compiled Java Bytecode)   │                │
│  └─────────────────────────────┘                │
└──────────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Deploy Rule Service

```bash
# Using Docker
docker run -d -p 8080:8080 openltablets/rule-service:latest

# Or using Docker Compose
docker compose up rule-service
```

### 2. Access OpenAPI Documentation

Open browser: `http://localhost:8080/swagger-ui.html`

### 3. Call a Rule (Example)

```bash
# REST API call
curl -X POST http://localhost:8080/rules/InsurancePremium/calculatePremium \
  -H "Content-Type: application/json" \
  -d '{
    "driver": {
      "age": 30,
      "gender": "M",
      "maritalStatus": "Single"
    },
    "vehicle": {
      "year": 2020,
      "make": "Toyota",
      "model": "Camry",
      "value": 25000
    }
  }'

# Response
{
  "premium": 1250.50,
  "discounts": ["SafeDriver", "GoodStudent"],
  "effectiveDate": "2025-11-06"
}
```

---

## Authentication

### Basic Authentication

**Configuration**:
```properties
# application.properties
security.basic.enabled=true
security.user.name=admin
security.user.password=admin123
```

**Usage**:
```bash
curl -u admin:admin123 \
  http://localhost:8080/rules/MyService/myMethod
```

**Java Example**:
```java
import org.apache.commons.codec.binary.Base64;

String auth = "admin:admin123";
String encodedAuth = Base64.encodeBase64String(auth.getBytes());
String authHeader = "Basic " + encodedAuth;

HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestProperty("Authorization", authHeader);
```

### Bearer Token (JWT)

**Configuration**:
```properties
security.oauth2.resource.jwt.key-value=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----
```

**Usage**:
```bash
# Obtain token
TOKEN=$(curl -X POST https://auth-server.com/oauth/token \
  -d "grant_type=client_credentials" \
  -d "client_id=your-client-id" \
  -d "client_secret=your-secret" \
  | jq -r '.access_token')

# Use token
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/rules/MyService/myMethod
```

### API Key Authentication

**Configuration**:
```properties
security.api-key.enabled=true
security.api-key.header-name=X-API-Key
```

**Usage**:
```bash
curl -H "X-API-Key: your-api-key-here" \
  http://localhost:8080/rules/MyService/myMethod
```

### OAuth 2.0

**Configuration**:
```properties
spring.security.oauth2.client.registration.okta.client-id=${OAUTH2_CLIENT_ID}
spring.security.oauth2.client.registration.okta.client-secret=${OAUTH2_CLIENT_SECRET}
spring.security.oauth2.client.registration.okta.scope=openid,profile,email
spring.security.oauth2.client.provider.okta.issuer-uri=https://your-domain.okta.com/oauth2/default
```

---

## REST API Reference

### Base URL

```
http://localhost:8080
```

### Content Types

- Request: `application/json`
- Response: `application/json`

### Standard Headers

| Header | Required | Description |
|--------|----------|-------------|
| `Content-Type` | Yes | `application/json` |
| `Accept` | No | `application/json` (default) |
| `Authorization` | Depends | Authentication token |
| `X-Request-ID` | No | Unique request identifier for tracing |

### Rule Execution Endpoint

#### Execute Rule Method

```http
POST /rules/{serviceName}/{methodName}
Content-Type: application/json

{
  "param1": "value1",
  "param2": "value2"
}
```

**Path Parameters**:
- `serviceName`: Name of the deployed rule service
- `methodName`: Name of the rule method to execute

**Request Body**: JSON object with parameters matching method signature

**Response**: JSON object with method return value

#### Example: Calculate Insurance Premium

**Request**:
```http
POST /rules/InsurancePremium/calculatePremium
Content-Type: application/json

{
  "driver": {
    "age": 35,
    "gender": "F",
    "maritalStatus": "Married",
    "yearsLicensed": 15,
    "accidentCount": 0
  },
  "vehicle": {
    "year": 2022,
    "make": "Honda",
    "model": "Civic",
    "value": 28000,
    "safetyRating": 5
  },
  "coverage": {
    "liability": 500000,
    "collision": true,
    "comprehensive": true,
    "deductible": 500
  }
}
```

**Response**:
```json
{
  "basePremium": 1200.00,
  "discounts": [
    {
      "name": "SafeDriver",
      "amount": 120.00
    },
    {
      "name": "MultiPolicy",
      "amount": 60.00
    }
  ],
  "totalPremium": 1020.00,
  "effectiveDate": "2025-11-06",
  "expirationDate": "2026-11-06"
}
```

### Service Information Endpoints

#### List Available Services

```http
GET /rules
Accept: application/json
```

**Response**:
```json
{
  "services": [
    {
      "name": "InsurancePremium",
      "version": "1.0.0",
      "methods": [
        "calculatePremium",
        "getRiskScore"
      ],
      "url": "/rules/InsurancePremium"
    },
    {
      "name": "LoanOrigination",
      "version": "2.1.0",
      "methods": [
        "evaluateApplication",
        "calculateInterestRate"
      ],
      "url": "/rules/LoanOrigination"
    }
  ]
}
```

#### Get Service Details

```http
GET /rules/{serviceName}
Accept: application/json
```

**Response**:
```json
{
  "name": "InsurancePremium",
  "version": "1.0.0",
  "description": "Insurance premium calculation rules",
  "methods": [
    {
      "name": "calculatePremium",
      "description": "Calculate insurance premium based on driver and vehicle",
      "parameters": [
        {
          "name": "driver",
          "type": "Driver",
          "required": true
        },
        {
          "name": "vehicle",
          "type": "Vehicle",
          "required": true
        }
      ],
      "returnType": "PremiumQuote"
    }
  ],
  "types": {
    "Driver": {
      "properties": {
        "age": "integer",
        "gender": "string",
        "maritalStatus": "string"
      }
    }
  }
}
```

### Health and Monitoring Endpoints

#### Health Check

```http
GET /actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000
      }
    }
  }
}
```

#### Metrics

```http
GET /actuator/metrics
```

**Response**:
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.gc.pause",
    "http.server.requests",
    "rule.execution.time"
  ]
}
```

#### Get Specific Metric

```http
GET /actuator/metrics/rule.execution.time
```

**Response**:
```json
{
  "name": "rule.execution.time",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 12543
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 45.234
    },
    {
      "statistic": "MAX",
      "value": 0.125
    }
  ]
}
```

---

## SOAP API Reference

### WSDL Access

```
http://localhost:8080/rules/{serviceName}?wsdl
```

### SOAP Endpoint

```
http://localhost:8080/rules/{serviceName}
```

### Example SOAP Request

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ins="http://openl.org/insurance">
   <soapenv:Header/>
   <soapenv:Body>
      <ins:calculatePremium>
         <driver>
            <age>35</age>
            <gender>F</gender>
            <maritalStatus>Married</maritalStatus>
         </driver>
         <vehicle>
            <year>2022</year>
            <make>Honda</make>
            <model>Civic</model>
            <value>28000</value>
         </vehicle>
      </ins:calculatePremium>
   </soapenv:Body>
</soapenv:Envelope>
```

### Example SOAP Response

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
   <soapenv:Body>
      <ns:calculatePremiumResponse xmlns:ns="http://openl.org/insurance">
         <return>
            <basePremium>1200.00</basePremium>
            <totalPremium>1020.00</totalPremium>
            <effectiveDate>2025-11-06</effectiveDate>
         </return>
      </ns:calculatePremiumResponse>
   </soapenv:Body>
</soapenv:Envelope>
```

### Java SOAP Client Example

```java
import javax.xml.ws.Service;
import javax.xml.namespace.QName;
import java.net.URL;

// Create service reference
URL wsdlURL = new URL("http://localhost:8080/rules/InsurancePremium?wsdl");
QName qname = new QName("http://openl.org/insurance", "InsurancePremiumService");
Service service = Service.create(wsdlURL, qname);

// Get port
InsurancePremium port = service.getPort(InsurancePremium.class);

// Call method
Driver driver = new Driver();
driver.setAge(35);
driver.setGender("F");
driver.setMaritalStatus("Married");

Vehicle vehicle = new Vehicle();
vehicle.setYear(2022);
vehicle.setMake("Honda");
vehicle.setModel("Civic");
vehicle.setValue(28000);

PremiumQuote quote = port.calculatePremium(driver, vehicle);
System.out.println("Premium: " + quote.getTotalPremium());
```

---

## OpenAPI/Swagger Documentation

### Accessing Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### OpenAPI JSON

```
http://localhost:8080/v3/api-docs
```

### Example OpenAPI Specification

```yaml
openapi: 3.0.1
info:
  title: OpenL Rule Services API
  description: REST API for executing business rules
  version: 6.0.0
servers:
  - url: http://localhost:8080
    description: Local development server
paths:
  /rules/InsurancePremium/calculatePremium:
    post:
      summary: Calculate insurance premium
      operationId: calculatePremium
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PremiumRequest'
      responses:
        '200':
          description: Premium calculated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PremiumQuote'
        '400':
          description: Invalid input
        '500':
          description: Internal server error
components:
  schemas:
    PremiumRequest:
      type: object
      required:
        - driver
        - vehicle
      properties:
        driver:
          $ref: '#/components/schemas/Driver'
        vehicle:
          $ref: '#/components/schemas/Vehicle'
    Driver:
      type: object
      properties:
        age:
          type: integer
          minimum: 16
          maximum: 100
        gender:
          type: string
          enum: [M, F]
        maritalStatus:
          type: string
          enum: [Single, Married, Divorced, Widowed]
    Vehicle:
      type: object
      properties:
        year:
          type: integer
        make:
          type: string
        model:
          type: string
        value:
          type: number
          format: double
    PremiumQuote:
      type: object
      properties:
        basePremium:
          type: number
          format: double
        totalPremium:
          type: number
          format: double
        discounts:
          type: array
          items:
            $ref: '#/components/schemas/Discount'
    Discount:
      type: object
      properties:
        name:
          type: string
        amount:
          type: number
          format: double
```

---

## Rule Service Endpoints

### Project Management

#### Deploy Project

```http
POST /admin/deploy
Content-Type: application/json

{
  "projectName": "InsurancePremium",
  "version": "1.0.0",
  "url": "https://github.com/your-org/insurance-rules.git"
}
```

#### Redeploy Project

```http
POST /admin/redeploy/{serviceName}
```

#### Undeploy Project

```http
DELETE /admin/undeploy/{serviceName}
```

### Runtime Management

#### Clear Cache

```http
POST /admin/cache/clear
```

#### Reload Rules

```http
POST /admin/reload
```

#### Get Runtime Statistics

```http
GET /admin/stats
```

**Response**:
```json
{
  "uptime": "5d 12h 34m",
  "totalRequests": 1250000,
  "averageResponseTime": 45,
  "cacheHitRatio": 0.85,
  "activeConnections": 12,
  "compiledProjects": [
    {
      "name": "InsurancePremium",
      "version": "1.0.0",
      "compilationTime": "2.5s",
      "lastUsed": "2025-11-06T10:30:00Z"
    }
  ]
}
```

---

## Studio API Endpoints

### Project Operations

#### List Projects

```http
GET /api/projects
Authorization: Bearer {token}
```

**Response**:
```json
{
  "projects": [
    {
      "id": "insurance-rules",
      "name": "Insurance Premium Rules",
      "version": "1.0.0",
      "lastModified": "2025-11-05T14:30:00Z",
      "author": "john.doe"
    }
  ]
}
```

#### Get Project Details

```http
GET /api/projects/{projectId}
```

#### Create Project

```http
POST /api/projects
Content-Type: application/json

{
  "name": "New Project",
  "description": "Project description",
  "template": "decision-table"
}
```

#### Update Project

```http
PUT /api/projects/{projectId}
Content-Type: application/json

{
  "name": "Updated Project Name",
  "description": "Updated description"
}
```

#### Delete Project

```http
DELETE /api/projects/{projectId}
```

### Rule Operations

#### Get Rule

```http
GET /api/projects/{projectId}/rules/{ruleId}
```

#### Update Rule

```http
PUT /api/projects/{projectId}/rules/{ruleId}
Content-Type: application/json

{
  "content": "... Excel rule content ...",
  "comment": "Updated discount calculation logic"
}
```

#### Test Rule

```http
POST /api/projects/{projectId}/rules/{ruleId}/test
Content-Type: application/json

{
  "testData": {
    "driver": { "age": 30 },
    "vehicle": { "value": 25000 }
  }
}
```

### Git Operations

#### Commit Changes

```http
POST /api/projects/{projectId}/git/commit
Content-Type: application/json

{
  "message": "Updated premium calculation rules",
  "files": ["rules.xlsx", "datatypes.xlsx"]
}
```

#### Push to Remote

```http
POST /api/projects/{projectId}/git/push
```

#### Pull from Remote

```http
POST /api/projects/{projectId}/git/pull
```

---

## Integration Examples

### JavaScript/TypeScript

#### Using Fetch API

```typescript
interface Driver {
  age: number;
  gender: string;
  maritalStatus: string;
}

interface Vehicle {
  year: number;
  make: string;
  model: string;
  value: number;
}

interface PremiumQuote {
  basePremium: number;
  totalPremium: number;
  discounts: Array<{ name: string; amount: number }>;
}

async function calculatePremium(
  driver: Driver,
  vehicle: Vehicle
): Promise<PremiumQuote> {
  const response = await fetch(
    'http://localhost:8080/rules/InsurancePremium/calculatePremium',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      body: JSON.stringify({ driver, vehicle })
    }
  );

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  return await response.json();
}

// Usage
const quote = await calculatePremium(
  { age: 30, gender: 'M', maritalStatus: 'Single' },
  { year: 2020, make: 'Toyota', model: 'Camry', value: 25000 }
);

console.log(`Premium: $${quote.totalPremium}`);
```

#### Using Axios

```typescript
import axios from 'axios';

const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add auth interceptor
client.interceptors.request.use(config => {
  config.headers.Authorization = `Bearer ${getToken()}`;
  return config;
});

async function calculatePremium(driver: Driver, vehicle: Vehicle) {
  const { data } = await client.post(
    '/rules/InsurancePremium/calculatePremium',
    { driver, vehicle }
  );
  return data;
}
```

### Python

```python
import requests
from typing import Dict, Any

class OpenLClient:
    def __init__(self, base_url: str, api_key: str = None):
        self.base_url = base_url
        self.session = requests.Session()
        if api_key:
            self.session.headers['X-API-Key'] = api_key

    def execute_rule(self, service: str, method: str, params: Dict[str, Any]) -> Dict[str, Any]:
        url = f"{self.base_url}/rules/{service}/{method}"
        response = self.session.post(url, json=params)
        response.raise_for_status()
        return response.json()

# Usage
client = OpenLClient('http://localhost:8080', api_key='your-api-key')

premium_quote = client.execute_rule(
    'InsurancePremium',
    'calculatePremium',
    {
        'driver': {
            'age': 30,
            'gender': 'M',
            'maritalStatus': 'Single'
        },
        'vehicle': {
            'year': 2020,
            'make': 'Toyota',
            'model': 'Camry',
            'value': 25000
        }
    }
)

print(f"Premium: ${premium_quote['totalPremium']}")
```

### Java

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

public class OpenLClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public OpenLClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public <T> T executeRule(String service, String method,
                             Object params, Class<T> responseType) {
        String url = String.format("%s/rules/%s/%s", baseUrl, service, method);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getToken());

        HttpEntity<Object> request = new HttpEntity<>(params, headers);

        ResponseEntity<T> response = restTemplate.postForEntity(
            url, request, responseType
        );

        return response.getBody();
    }
}

// Usage
OpenLClient client = new OpenLClient("http://localhost:8080");

PremiumRequest request = new PremiumRequest();
request.setDriver(new Driver(30, "M", "Single"));
request.setVehicle(new Vehicle(2020, "Toyota", "Camry", 25000));

PremiumQuote quote = client.executeRule(
    "InsurancePremium",
    "calculatePremium",
    request,
    PremiumQuote.class
);

System.out.println("Premium: $" + quote.getTotalPremium());
```

### C# / .NET

```csharp
using System;
using System.Net.Http;
using System.Net.Http.Json;
using System.Threading.Tasks;

public class OpenLClient
{
    private readonly HttpClient _httpClient;

    public OpenLClient(string baseUrl, string apiKey = null)
    {
        _httpClient = new HttpClient
        {
            BaseAddress = new Uri(baseUrl)
        };

        if (!string.IsNullOrEmpty(apiKey))
        {
            _httpClient.DefaultRequestHeaders.Add("X-API-Key", apiKey);
        }
    }

    public async Task<TResponse> ExecuteRuleAsync<TRequest, TResponse>(
        string service,
        string method,
        TRequest parameters)
    {
        var url = $"/rules/{service}/{method}";
        var response = await _httpClient.PostAsJsonAsync(url, parameters);
        response.EnsureSuccessStatusCode();
        return await response.Content.ReadFromJsonAsync<TResponse>();
    }
}

// Usage
var client = new OpenLClient("http://localhost:8080", "your-api-key");

var request = new PremiumRequest
{
    Driver = new Driver { Age = 30, Gender = "M", MaritalStatus = "Single" },
    Vehicle = new Vehicle { Year = 2020, Make = "Toyota", Model = "Camry", Value = 25000 }
};

var quote = await client.ExecuteRuleAsync<PremiumRequest, PremiumQuote>(
    "InsurancePremium",
    "calculatePremium",
    request
);

Console.WriteLine($"Premium: ${quote.TotalPremium}");
```

### Go

```go
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
)

type OpenLClient struct {
    BaseURL string
    APIKey  string
}

func (c *OpenLClient) ExecuteRule(service, method string, params interface{}, result interface{}) error {
    url := fmt.Sprintf("%s/rules/%s/%s", c.BaseURL, service, method)

    jsonData, err := json.Marshal(params)
    if err != nil {
        return err
    }

    req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
    if err != nil {
        return err
    }

    req.Header.Set("Content-Type", "application/json")
    if c.APIKey != "" {
        req.Header.Set("X-API-Key", c.APIKey)
    }

    client := &http.Client{}
    resp, err := client.Do(req)
    if err != nil {
        return err
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return fmt.Errorf("HTTP %d: %s", resp.StatusCode, resp.Status)
    }

    return json.NewDecoder(resp.Body).Decode(result)
}

// Usage
func main() {
    client := &OpenLClient{
        BaseURL: "http://localhost:8080",
        APIKey:  "your-api-key",
    }

    request := PremiumRequest{
        Driver: Driver{Age: 30, Gender: "M", MaritalStatus: "Single"},
        Vehicle: Vehicle{Year: 2020, Make: "Toyota", Model: "Camry", Value: 25000},
    }

    var quote PremiumQuote
    err := client.ExecuteRule("InsurancePremium", "calculatePremium", request, &quote)
    if err != nil {
        panic(err)
    }

    fmt.Printf("Premium: $%.2f\n", quote.TotalPremium)
}
```

---

## Error Handling

### Standard Error Response

```json
{
  "timestamp": "2025-11-06T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid parameter: driver.age must be between 16 and 100",
  "path": "/rules/InsurancePremium/calculatePremium",
  "requestId": "abc123-def456-ghi789"
}
```

### HTTP Status Codes

| Status Code | Meaning | Description |
|-------------|---------|-------------|
| **200** | OK | Request successful |
| **400** | Bad Request | Invalid parameters or malformed request |
| **401** | Unauthorized | Missing or invalid authentication |
| **403** | Forbidden | Insufficient permissions |
| **404** | Not Found | Service or method not found |
| **429** | Too Many Requests | Rate limit exceeded |
| **500** | Internal Server Error | Rule execution error |
| **503** | Service Unavailable | Service temporarily unavailable |

### Error Handling Example (TypeScript)

```typescript
async function executeRuleWithRetry(
  service: string,
  method: string,
  params: any,
  maxRetries: number = 3
): Promise<any> {
  let lastError: Error;

  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await fetch(`/rules/${service}/${method}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
      });

      if (!response.ok) {
        const error = await response.json();

        // Don't retry client errors (4xx)
        if (response.status >= 400 && response.status < 500) {
          throw new Error(error.message);
        }

        // Retry server errors (5xx)
        throw new Error(`Server error: ${error.message}`);
      }

      return await response.json();

    } catch (error) {
      lastError = error as Error;

      // Wait before retry (exponential backoff)
      if (attempt < maxRetries - 1) {
        await new Promise(resolve =>
          setTimeout(resolve, Math.pow(2, attempt) * 1000)
        );
      }
    }
  }

  throw lastError!;
}
```

---

## Rate Limiting

### Configuration

```properties
# application.properties
ratelimit.enabled=true
ratelimit.requests-per-minute=100
ratelimit.burst-size=20
```

### Response Headers

When rate limiting is enabled, responses include:

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1699275600
```

### Rate Limit Exceeded Response

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 45 seconds.",
  "retryAfter": 45
}
```

---

## Best Practices

### 1. Connection Pooling

```java
// Use connection pooling for better performance
CloseableHttpClient httpClient = HttpClients.custom()
    .setMaxConnTotal(200)
    .setMaxConnPerRoute(20)
    .setConnectionTimeToLive(1, TimeUnit.MINUTES)
    .build();
```

### 2. Timeouts

```typescript
const controller = new AbortController();
const timeout = setTimeout(() => controller.abort(), 5000);

try {
  const response = await fetch(url, {
    signal: controller.signal,
    // ... other options
  });
} finally {
  clearTimeout(timeout);
}
```

### 3. Caching

```typescript
// Client-side caching
const cache = new Map<string, { data: any; timestamp: number }>();

async function cachedExecuteRule(
  service: string,
  method: string,
  params: any,
  cacheTTL: number = 60000
): Promise<any> {
  const key = `${service}.${method}.${JSON.stringify(params)}`;
  const cached = cache.get(key);

  if (cached && Date.now() - cached.timestamp < cacheTTL) {
    return cached.data;
  }

  const data = await executeRule(service, method, params);
  cache.set(key, { data, timestamp: Date.now() });
  return data;
}
```

### 4. Request Batching

For multiple rule executions, batch requests when possible:

```http
POST /rules/batch
Content-Type: application/json

{
  "requests": [
    {
      "service": "InsurancePremium",
      "method": "calculatePremium",
      "params": { ... }
    },
    {
      "service": "InsurancePremium",
      "method": "getRiskScore",
      "params": { ... }
    }
  ]
}
```

### 5. Monitoring and Logging

```typescript
// Add request ID for tracing
const requestId = generateUUID();

const response = await fetch(url, {
  headers: {
    'X-Request-ID': requestId,
    // ... other headers
  }
});

console.log(`[${requestId}] Request completed in ${elapsed}ms`);
```

---

## Additional Resources

- [OpenL Tablets Website](https://openl-tablets.org)
- [OpenAPI Specification](http://localhost:8080/v3/api-docs)
- [Swagger UI](http://localhost:8080/swagger-ui.html)
- [GitHub Repository](https://github.com/openl-tablets/openl-tablets)
- [Integration Examples](https://github.com/openl-tablets/openl-tablets/tree/master/DEMO)

---

**Need Help?** Open an issue on [GitHub](https://github.com/openl-tablets/openl-tablets/issues) or consult the [community forums](https://github.com/openl-tablets/openl-tablets/discussions).

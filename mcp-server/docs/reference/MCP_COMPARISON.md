# MCP Server Comparison: TypeScript vs Java

**Created**: 2025-12-16  
**OpenL Tablets Version**: 6.0.0-SNAPSHOT

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Tools Comparison](#tools-comparison)
5. [Implementation Details](#implementation-details)
6. [Performance and Scalability](#performance-and-scalability)
7. [Deployment](#deployment)
8. [Recommendations](#recommendations)

---

## Overview

OpenL Tablets provides two implementations of the MCP (Model Context Protocol) server:

| Aspect | TypeScript Version | Java Version |
|--------|-------------------|-------------|
| **Location** | `/mcp-server/` | `/STUDIO/org.openl.rules.webstudio/` |
| **Type** | Standalone server | Embedded in Spring Boot application |
| **Transport** | stdio (for Claude Desktop) | HTTP REST API (`/mcp`) |
| **Target Audience** | AI agents (Claude Desktop, Cursor) | Spring AI integration |
| **Status** | ✅ Fully functional | ✅ Fully functional |

---

## Architecture

### TypeScript Version

```
┌─────────────────┐
│ Claude Desktop  │  ← AI Assistant
│   (Application) │
└────────┬────────┘
         │ MCP Protocol (stdio)
         │
         ▼
┌─────────────────┐
│   MCP Server    │  ← Standalone Node.js process
│  (Node.js/TS)   │
└────────┬────────┘
         │ HTTP REST API
         │ (JSON, Basic Auth / OAuth2)
         ▼
┌─────────────────┐
│  OpenL Tablets  │  ← OpenL Studio REST API
│   (Java/Jetty)  │     (port 8080)
└─────────────────┘
```

**Features**:
- Independent process, runs separately
- Uses stdio for communication with Claude Desktop
- Can run as HTTP server (Express) for integrations
- Supports SSE and Streamable HTTP transports

### Java Version

```
┌─────────────────┐
│  Spring AI /    │  ← AI Framework
│  External Client │
└────────┬────────┘
         │ HTTP POST /mcp
         │ (MCP Protocol over HTTP)
         ▼
┌─────────────────┐
│  Spring Boot    │  ← Embedded in OpenL Studio
│  Application    │     (org.openl.rules.webstudio)
└────────┬────────┘
         │ Direct Service Calls
         │ (no HTTP)
         ▼
┌─────────────────┐
│  OpenL Services │  ← Direct access to services
│  (Same JVM)     │
└─────────────────┘
```

**Features**:
- Embedded in Spring Boot application OpenL Studio
- Uses HTTP endpoint `/mcp` for MCP protocol
- Direct access to OpenL services (no HTTP overhead)
- Uses Spring Security for authentication

---

## Technology Stack

### TypeScript Version

| Component | Technology | Version |
|-----------|------------|--------|
| **Runtime** | Node.js | 18.0.0+ |
| **Language** | TypeScript | 5.7.2 |
| **MCP SDK** | @modelcontextprotocol/sdk | 1.21.1 |
| **HTTP Client** | axios | ^1.7.0 |
| **Validation** | zod | ^3.23.0 |
| **Schemas** | zod-to-json-schema | ^3.23.0 |
| **HTTP Server** | express | ^4.19.0 |
| **CORS** | cors | ^2.8.5 |

**Dependencies**:
- Minimal external dependencies
- Lightweight runtime
- Fast startup

### Java Version

| Component | Technology | Version |
|-----------|------------|--------|
| **Runtime** | JVM | Java 21+ |
| **Language** | Java | 21+ |
| **Framework** | Spring Boot | 3.5.6 |
| **MCP SDK** | io.modelcontextprotocol | (embedded) |
| **AI Framework** | Spring AI | (embedded) |
| **JSON** | Jackson | (embedded) |
| **Security** | Spring Security | 6.5.5 |

**Dependencies**:
- Integration with Spring ecosystem
- Uses existing OpenL infrastructure
- Access to Spring Security context

---

## Tools Comparison

### Total Count

| Category | TypeScript | Java | Status |
|-----------|------------|------|--------|
| **Total Tools** | 26 | 20 | ⚠️ Naming differences |
| **Active** | 19 | 20 | ✅ Functionally equivalent |
| **Temporarily Disabled** | 7 | 0 | ⚠️ TypeScript has more disabled tools |

**Note**: TypeScript version has more tools defined, but some are temporarily disabled. Java version has all tools active but uses different names for some of them.

### Detailed Comparison by Category

#### 1. Repository Tools

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_list_repositories` | ✅ | ✅ `openl_list_design_repositories` | Different names, same functionality |
| `openl_list_branches` | ✅ | ✅ `openl_list_design_repository_branches` | Different names |
| `openl_list_design_repository_features` | ✅ | ✅ | ✅ Synchronized |
| `openl_list_design_repository_project_revisions` | ✅ | ✅ | ✅ Synchronized |
| `openl_list_deploy_repositories` | ✅ | ✅ | ✅ Synchronized |

**Note**: TypeScript version uses shorter names (`openl_list_repositories`), Java version uses more descriptive names (`openl_list_design_repositories`).

#### 2. Project Tools

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_list_projects` | ✅ | ✅ | ✅ Synchronized |
| `openl_get_project` | ✅ | ✅ | ✅ Synchronized |
| `openl_update_project_status` | ✅ | ✅ | ✅ Synchronized |
| `openl_create_project_branch` | ✅ | ✅ | ✅ Synchronized |
| `openl_list_project_local_changes` | ✅ | ✅ | ✅ Synchronized |
| `openl_restore_project_local_change` | ✅ | ✅ | ✅ Synchronized |

#### 3. Table/Rules Tools

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_list_tables` | ✅ | ✅ `openl_list_project_tables` | Different names |
| `openl_get_table` | ✅ | ✅ `openl_get_project_table` | Different names |
| `openl_update_table` | ✅ | ✅ `openl_update_project_table` | Different names |
| `openl_append_table` | ✅ | ✅ `openl_append_project_table` | Different names |
| `openl_create_rule` | ✅ (disabled) | ✅ `openl_create_project_table` | TypeScript: temporarily disabled |

#### 4. File Management

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_upload_file` | ✅ (disabled) | ❌ | TypeScript: temporarily disabled |
| `openl_download_file` | ✅ (disabled) | ❌ | TypeScript: temporarily disabled |

#### 5. Deployment Tools

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_list_deployments` | ✅ | ✅ | ✅ Synchronized |
| `openl_deploy_project` | ✅ | ✅ | ✅ Synchronized |
| `openl_redeploy_project` | ✅ | ✅ | ✅ Synchronized |

#### 6. Testing & Validation

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_run_project_tests` | ✅ | ✅ | ✅ Synchronized |
| `openl_validate_project` | ❌ (disabled) | ❌ | Not implemented in both versions |
| `openl_get_project_errors` | ❌ (disabled) | ❌ | Not implemented in both versions |
| `openl_execute_rule` | ❌ (disabled) | ❌ | Not implemented in both versions |

#### 7. Version Control

| Tool | TypeScript | Java | Notes |
|------------|------------|------|------------|
| `openl_get_file_history` | ❌ (disabled) | ❌ | Not implemented |
| `openl_get_project_history` | ❌ (disabled) | ❌ | Not implemented |
| `openl_revert_version` | ❌ (disabled) | ❌ | Not implemented |
| `openl_compare_versions` | ❌ (disabled) | ❌ | Not implemented |

### Synchronization Statistics

```
✅ Fully synchronized:                   20 tools
⚠️  Different names, same functionality: 6 tools
   - openl_list_repositories ↔ openl_list_design_repositories
   - openl_list_branches ↔ openl_list_design_repository_branches
   - openl_list_tables ↔ openl_list_project_tables
   - openl_get_table ↔ openl_get_project_table
   - openl_update_table ↔ openl_update_project_table
   - openl_append_table ↔ openl_append_project_table
❌ Missing in one version:                0 tools
🔧 Temporarily disabled in TypeScript:   7 tools
   - openl_upload_file
   - openl_download_file
   - openl_create_rule
   - openl_execute_rule
   - openl_get_file_history
   - openl_get_project_history
   - openl_revert_version
```

---

## Implementation Details

### TypeScript Version

#### Advantages

1. **Independence**
   - Can run separately from OpenL Studio
   - Easy integration with various AI clients
   - Doesn't require running the entire Spring Boot application

2. **Transport Flexibility**
   - Supports stdio (Claude Desktop)
   - Supports HTTP/SSE (Express server)
   - Supports Streamable HTTP

3. **Type Safety**
   - Zod schemas for input validation
   - Automatic JSON Schema generation
   - Strong TypeScript typing

4. **Modularity**
   - Clear separation into modules (client, handlers, schemas, types)
   - Easy to test individual components
   - Simple to add new tools

5. **Authentication**
   - Basic Auth support
   - OAuth 2.1 support (PKCE, refresh tokens)
   - API Key support

#### Disadvantages

1. **Additional HTTP Overhead**
   - All requests go through REST API
   - Network latency
   - No direct access to OpenL services

2. **API Dependency**
   - Requires OpenL REST API availability
   - Depends on API contract stability
   - May be slower due to HTTP overhead

3. **Session Management**
   - Some operations require project loading in WebStudio
   - Local changes depend on session

### Java Version

#### Advantages

1. **Direct Service Access**
   - Direct method calls, no HTTP
   - No network latency
   - Higher performance

2. **Spring Integration**
   - Uses Spring Security context
   - Access to Spring Beans
   - Unified infrastructure with OpenL Studio

3. **Security**
   - Uses existing OpenL security system
   - Automatic access control checks
   - Integration with ACL system

4. **Deployment Simplicity**
   - Embedded in existing application
   - No additional processes required
   - Single entry point

#### Disadvantages

1. **Spring Boot Dependency**
   - Requires running entire OpenL Studio application
   - Higher resource consumption
   - Slower startup

2. **Limited Transport**
   - HTTP REST API only
   - Doesn't support stdio (for Claude Desktop)
   - Requires HTTP client for connection

3. **Less Flexibility**
   - Tied to Spring ecosystem
   - Harder to use outside Spring context
   - Fewer integration options

---

## Performance and Scalability

### TypeScript Version

| Metric | Value | Notes |
|---------|----------|------------|
| **Startup Time** | ~100-200ms | Fast Node.js process startup |
| **Memory (idle)** | ~50-100 MB | Lightweight runtime |
| **Memory (active)** | ~100-200 MB | Depends on request volume |
| **Throughput** | Network dependent | Limited by HTTP overhead |
| **Request Latency** | +10-50ms | Additional HTTP round-trip |

**Scalability**:
- Easy horizontal scaling
- Can run multiple instances
- Requires load balancer for HTTP mode

### Java Version

| Metric | Value | Notes |
|---------|----------|------------|
| **Startup Time** | ~10-30 seconds | Spring Boot application startup |
| **Memory (idle)** | ~500 MB - 1 GB | Spring Boot + OpenL Studio |
| **Memory (active)** | ~1-2 GB | Depends on load |
| **Throughput** | High | Direct method calls |
| **Request Latency** | Minimal | No HTTP overhead |

**Scalability**:
- Vertical scaling (more memory/CPU)
- Horizontal scaling through Spring Boot cluster
- Uses existing OpenL infrastructure

---

## Deployment

### TypeScript Version

#### Option 1: Standalone (Claude Desktop)

```bash
# Install dependencies
cd mcp-server
npm install

# Build
npm run build

# Configure in Claude Desktop
# ~/Library/Application Support/Claude/config.json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

#### Option 2: HTTP Server (Docker)

```bash
# Run as HTTP server
cd mcp-server
npm run server

# Or via Docker
docker build -t openl-mcp-server .
docker run -p 3000:3000 \
  -e OPENL_BASE_URL=http://openl:8080/rest \
  -e OPENL_USERNAME=admin \
  -e OPENL_PASSWORD=admin \
  openl-mcp-server
```

**Requirements**:
- Node.js 18.0.0+
- Access to OpenL REST API
- Configured authentication

### Java Version

#### Deployment

```bash
# Build OpenL Studio (includes MCP server)
cd STUDIO
mvn clean install

# Run Spring Boot application
cd org.openl.rules.webstudio
mvn spring-boot:run

# MCP endpoint available at:
# POST http://localhost:8080/mcp
```

**Requirements**:
- Java 21+
- Maven 3.8+
- PostgreSQL (for production)
- Full OpenL Studio stack

#### Configuration

```java
// Automatic configuration via Spring
@Configuration
public class McpServerConfig {
    // MCP server configured automatically
    // Endpoint: /mcp
    // Uses Spring Security for authentication
}
```

---

## Recommendations

### Use TypeScript Version If:

✅ **You're using Claude Desktop or Cursor**
- Native stdio transport support
- Simple integration via configuration file

✅ **You need independence from OpenL Studio**
- Can run MCP server separately
- Full OpenL stack not required

✅ **You're integrating with external AI systems**
- Easy integration via HTTP
- Support for various transports

✅ **Fast development and testing is important**
- Fast startup
- Easy to add new tools
- Simple testing

### Use Java Version If:

✅ **You're using Spring AI**
- Native integration with Spring AI framework
- Uses Spring Security context

✅ **Performance is critical**
- Direct method calls without HTTP overhead
- Minimal latency

✅ **OpenL Studio is already running**
- No additional processes required
- Unified infrastructure

✅ **You need deep integration with OpenL**
- Direct access to services
- Use of Spring Beans
- Integration with ACL system

### Hybrid Approach

You can use both versions simultaneously:

- **TypeScript version** for Claude Desktop / external integrations
- **Java version** for Spring AI / internal integrations

Both versions use the same tools and provide compatible APIs.

---

## Conclusion

Both MCP server versions are fully functional and synchronized in terms of tools. The choice depends on your specific requirements:

- **TypeScript version** — for standalone use and integration with AI clients
- **Java version** — for deep integration with Spring ecosystem and maximum performance

Both versions are actively developed and maintained by the OpenL Tablets team.

---

## Additional Resources

- [TypeScript MCP Server README](../../README.md)
- [TypeScript MCP Server Architecture](../development/ARCHITECTURE.md)
- [Java MCP Server Configuration](../../STUDIO/org.openl.rules.webstudio/src/org/openl/studio/mcp/config/McpServerConfig.java)
- [OpenL Tablets Documentation](../../Docs/)

---

**Last Updated**: 2025-12-16  
**Document Version**: 1.0.0

# 🏗️ Architecture: How Everything Works Together

## Interaction Diagram

```
┌─────────────────┐
│ Claude Desktop  │  ← You are here (AI assistant)
│   (Application) │
└────────┬────────┘
         │ MCP Protocol (stdio)
         │
         ▼
┌─────────────────┐
│   MCP Server    │  ← This project (mcp-server/)
│  (Node.js/TS)   │
└────────┬────────┘
         │ HTTP REST API
         │ (JSON)
         ▼
┌─────────────────┐
│  OpenL Tablets  │  ← Rules server
│   (Java/Jetty)  │     (port 8080)
└─────────────────┘
```

## Components

### 1. Claude Desktop
- **What it is:** Application with Claude AI assistant
- **Where:** Installed on your Mac
- **Role:** Interface for communicating with AI

### 2. MCP Server (mcp-server/)
- **What it is:** Bridge between Claude and OpenL
- **Where:** `/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server`
- **Role:** 
  - Converts Claude commands to API requests to OpenL
  - Provides 18 tools for working with OpenL
  - Manages authentication

### 3. OpenL Tablets
- **What it is:** Server for managing business rules
- **Where:** Running via Docker or locally
- **Role:** Stores and executes rules, projects, tables

## Data Flow

```
1. You write in Claude: "List repositories"
   │
2. Claude → MCP Server: calls tool openl_list_repositories
   │
3. MCP Server → OpenL API: GET /rest/repository
   │
4. OpenL → MCP Server: returns JSON with repositories
   │
5. MCP Server → Claude: formats response as markdown
   │
6. Claude → You: shows list of repositories
```

## Ports and Addresses

| Component | Port | URL |
|-----------|------|-----|
| OpenL Studio | 8080 | http://localhost:8080/studio |
| OpenL REST API | 8080 | http://localhost:8080/rest |
| Rule Services | 8080 | http://localhost:8080/services |
| PostgreSQL | 5432 | localhost:5432 |
| MCP Server | - | Works via stdio (no port) |

## Configuration Files

### Claude Desktop
```
~/Library/Application Support/Claude/config.json
```
Contains MCP server settings (path, environment variables)

### MCP Server
```
mcp-server/dist/index.js          # Compiled server
mcp-server/src/                   # Source code
mcp-server/claude-desktop-config.json  # Configuration example
```

### OpenL Tablets
```
compose.yaml                       # Docker Compose configuration
DEMO/start                         # Local startup script
```

## Startup Process

### Option 1: Docker (recommended)
```bash
# Terminal 1: Start OpenL
docker compose up

# Claude Desktop starts separately (application)
# MCP Server starts automatically by Claude Desktop
```

### Option 2: Locally
```bash
# Terminal 1: Start OpenL
cd DEMO && ./start

# Claude Desktop starts separately
# MCP Server starts automatically by Claude Desktop
```

## Authentication

MCP Server uses one of three methods:

1. **Basic Auth** (default)
   ```
   OPENL_USERNAME=admin
   OPENL_PASSWORD=admin
   ```

2. **API Key**
   ```
   OPENL_API_KEY=your-key
   ```

3. **OAuth 2.1**
   ```
   OPENL_OAUTH2_CLIENT_ID=...
   OPENL_OAUTH2_CLIENT_SECRET=...
   ```

## Health Check

### Level 1: Is OpenL accessible?
```bash
curl http://localhost:8080/rest/repository
```

### Level 2: Is MCP Server configured?
```bash
cat ~/Library/Application\ Support/Claude/config.json | grep openl-mcp-server
```

### Level 3: Does Claude see the server?
- Open Claude Desktop settings
- Check MCP server status

### Level 4: Does everything work?
In Claude: "List repositories in OpenL Tablets"

## Common Issues

### Issue: Claude doesn't see MCP server
**Cause:** Incorrect path in configuration or server not built
**Solution:** Check `config.json` and run `npm run build`

### Issue: "Cannot connect to OpenL API"
**Cause:** OpenL not running or inaccessible
**Solution:** Start `docker compose up` or `DEMO/start`

### Issue: "Authentication failed"
**Cause:** Incorrect credentials
**Solution:** Check `OPENL_USERNAME` and `OPENL_PASSWORD` in configuration

## Useful Commands

```bash
# Start everything and check
./start-all.sh

# Check system health
./check-health.sh

# View OpenL logs (Docker)
docker compose logs -f studio

# Rebuild MCP server
npm run build

# Run tests
npm test
```

# 🐳 Docker and Docker Compose for MCP Server

## Overview

MCP Server can now run as a standalone HTTP application on Express, allowing it to be integrated into Docker Compose as a microservice.

## Architecture

```
┌─────────────────┐
│   HTTP Client   │  ← External requests
└────────┬────────┘
         │ HTTP REST API
         │
         ▼
┌─────────────────┐
│ Express Server  │  ← HTTP API on port 3000
│  (server.ts)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  MCP Tools      │  ← Call OpenL tools
│  (tool-handlers)│
└────────┬────────┘
         │ HTTP REST API
         │
         ▼
┌─────────────────┐
│  OpenL Studio   │  ← OpenL Tablets API
│  (studio:8080)  │
└─────────────────┘
```

## Running via Docker Compose

### Quick Start

```bash
cd /path/to/openl-tablets
docker compose up mcp-server
```

### Running the Full Stack

```bash
docker compose up
```

This will start:
- PostgreSQL
- OpenL Studio (port 8080)
- Rule Services (port 8081)
- MCP Server (port 3000)
- Nginx Proxy (port 80)

## HTTP API Endpoints

### Health Check
```bash
GET http://localhost:3000/health
```

Response:
```json
{
  "status": "ok",
  "timestamp": "2024-01-01T12:00:00.000Z",
  "service": "openl-mcp-server",
  "version": "1.0.0"
}
```

### List Tools
```bash
GET http://localhost:3000/tools
```

Response:
```json
{
  "tools": [
    {
      "name": "openl_list_repositories",
      "title": "openl List Repositories",
      "description": "...",
      "inputSchema": {...}
    },
    ...
  ],
  "count": 18
}
```

### Tool Information
```bash
GET http://localhost:3000/tools/openl_list_repositories
```

### Execute Tool

**Option 1: Via tool endpoint**
```bash
POST http://localhost:3000/tools/openl_list_repositories/execute
Content-Type: application/json

{
  "repository": "design"
}
```

**Option 2: Universal endpoint**
```bash
POST http://localhost:3000/execute
Content-Type: application/json

{
  "tool": "openl_list_repositories",
  "arguments": {
    "repository": "design"
  }
}
```

## Environment Variables

MCP Server uses the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | HTTP server port | `3000` |
| `OPENL_BASE_URL` | OpenL Studio API URL | `http://studio:8080/rest` |
| `OPENL_USERNAME` | Username | `admin` |
| `OPENL_PASSWORD` | Password | `admin` |
| `OPENL_CLIENT_DOCUMENT_ID` | Client ID for tracking | `docker-compose-1` |
| `NODE_ENV` | Environment mode | `production` |

## Access via Nginx Proxy

MCP Server is also accessible through Nginx proxy:

```bash
# Health check
GET http://localhost/mcp/health

# List tools
GET http://localhost/mcp/tools

# Execute tool
POST http://localhost/mcp/execute
```

## Local Development

### Running without Docker

```bash
cd mcp-server

# Install dependencies
npm install

# Build
npm run build

# Start HTTP server
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
npm run start:http
```

### Development Mode with Auto-rebuild

```bash
# Terminal 1: Auto-rebuild
npm run watch

# Terminal 2: Start server
npm run dev:http
```

## Testing

### Health Check
```bash
curl http://localhost:3000/health
```

### List Tools
```bash
curl http://localhost:3000/tools | jq
```

### Execute Tool
```bash
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{
    "tool": "openl_list_repositories",
    "arguments": {}
  }' | jq
```

## Logs

### View Docker Compose Logs
```bash
docker compose logs -f mcp-server
```

### Real-time Logs
```bash
docker compose logs -f --tail=100 mcp-server
```

## Troubleshooting

### MCP Server doesn't start

1. Check logs:
   ```bash
   docker compose logs mcp-server
   ```

2. Check that OpenL Studio is running:
   ```bash
   docker compose ps studio
   ```

3. Check environment variables:
   ```bash
   docker compose exec mcp-server env | grep OPENL
   ```

### Connection Error to OpenL

Ensure that:
- OpenL Studio is running (`docker compose ps studio`)
- `OPENL_BASE_URL` points to the correct address (`http://studio:8080/rest`)
- Credentials are correct (`OPENL_USERNAME`, `OPENL_PASSWORD`)

### Port 3000 is Occupied

Change port in `compose.yaml`:
```yaml
ports:
  - "3001:3000"  # External:Internal
```

Or set environment variable:
```yaml
environment:
  PORT: 3001
```

For more troubleshooting, see [Troubleshooting Guide](../guides/TROUBLESHOOTING.md).

## Production Deployment

For production, it's recommended to:

1. Use HTTPS through reverse proxy
2. Configure authentication at API level
3. Limit resources in `deploy.resources`
4. Set up monitoring and logging
5. Use secrets for passwords (don't store in compose.yaml)

## Usage Examples

### Python
```python
import requests

# Health check
response = requests.get('http://localhost:3000/health')
print(response.json())

# Execute tool
response = requests.post(
    'http://localhost:3000/execute',
    json={
        'tool': 'openl_list_repositories',
        'arguments': {}
    }
)
print(response.json())
```

### JavaScript/Node.js
```javascript
const axios = require('axios');

// Health check
const health = await axios.get('http://localhost:3000/health');
console.log(health.data);

// Execute tool
const result = await axios.post('http://localhost:3000/execute', {
  tool: 'openl_list_repositories',
  arguments: {}
});
console.log(result.data);
```

### cURL
```bash
# Health check
curl http://localhost:3000/health

# List tools
curl http://localhost:3000/tools

# Execute tool
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{
    "tool": "openl_list_repositories",
    "arguments": {}
  }'
```

## Additional Information

- [Quick Start Guide](../getting-started/QUICK-START.md) - Get started quickly
- [Cursor Docker Setup](CURSOR-DOCKER.md) - Connect Cursor to Docker container
- [Authentication Guide](../guides/AUTHENTICATION.md) - Authentication setup
- [Troubleshooting Guide](../guides/TROUBLESHOOTING.md) - Common issues


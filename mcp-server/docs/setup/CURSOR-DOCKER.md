# 🐳 Connecting Cursor to MCP Server in Docker

## Overview

This document describes how to connect Cursor IDE to MCP Server running in a Docker container.

## Architecture

```text
┌─────────────┐
│   Cursor    │  ← Cursor IDE
└──────┬──────┘
       │ MCP Protocol (SSE/HTTP)
       │ Direct HTTP connection
       │
       ▼
┌─────────────┐
│ MCP Server  │  ← Docker container (port 3000)
│  (Docker)   │     Express HTTP API with SSE transport
└──────┬──────┘
       │ HTTP REST API
       │
       ▼
┌─────────────┐
│ OpenL Studio│  ← OpenL Tablets (port 8080)
│  (Docker)   │
└─────────────┘
```

**Key advantage:** No local proxy needed! Cursor connects directly to Docker container via HTTP.

## Quick Setup

### Step 1: Start Docker Containers

**Note:** Replace `<project-root>` with the absolute path to your local OpenL Tablets project directory. You can find this by running `pwd` from the project root directory.

```bash
cd <project-root>
docker compose up mcp-server
```

Or the entire stack:

```bash
docker compose up
```

### Step 2: Check HTTP API Availability

```bash
curl http://localhost:3000/health
```

Should return:
```json
{
  "status": "ok",
  "timestamp": "...",
  "service": "openl-mcp-server",
  "version": "1.0.0"
}
```

### Step 3: Configure Cursor for Direct HTTP Connection

**No local proxy needed!** Cursor connects directly to the Docker container via HTTP.

1. Open Cursor settings (`Cmd + ,`)
2. Find "MCP" or "Model Context Protocol" section
3. Click "Add New Global MCP Server"
4. Enter name: `openl-mcp-server-docker`
5. Configure:
   - **Type:** Select `sse` or `streamablehttp`
   - **URL:** `http://localhost:3000/mcp/sse`

   Or use JSON configuration:
   ```json
   {
     "url": "http://localhost:3000/mcp/sse",
     "transport": "sse"
   }
   ```

6. Save and check connection status

**Note:** This method doesn't require Node.js or any local files on the client machine. Cursor connects directly to the Docker container via HTTP.

## Configuration

### Direct HTTP Connection

Cursor connects directly to the Docker container via HTTP SSE transport:

```json
{
  "mcpServers": {
    "openl-mcp-server-docker": {
      "url": "http://localhost:3000/mcp/sse",
      "transport": "sse"
    }
  }
}
```

### Authentication Configuration

The MCP server in Docker supports multiple authentication methods. Configure authentication via environment variables in `compose.yaml`:

#### Personal Access Token (PAT) - Recommended for Docker

Personal Access Token is the recommended authentication method for Docker deployments. It's simple, secure, and doesn't require OAuth2 setup:

```yaml
mcp-server:
  environment:
    OPENL_BASE_URL: http://studio:8080/rest
    OPENL_PERSONAL_ACCESS_TOKEN: <your-pat-token>
    OPENL_CLIENT_DOCUMENT_ID: docker-compose-1
```

**To use PAT:**

1. **Create a PAT in OpenL Tablets UI:**
   - Log in to OpenL Tablets Studio
   - Go to **User Settings** → **Personal Access Tokens**
   - Click **Create Token**
   - Copy the token (shown only once!)

2. **Set environment variable:**
   ```bash
   export OPENL_PERSONAL_ACCESS_TOKEN=<your-pat-token>
   ```

3. **Update compose.yaml:**
   ```yaml
   mcp-server:
     environment:
       OPENL_PERSONAL_ACCESS_TOKEN: ${OPENL_PERSONAL_ACCESS_TOKEN}
   ```

4. **Restart container:**
   ```bash
   docker compose up -d mcp-server
   ```

**Benefits:**
- ✅ No OAuth2 configuration needed
- ✅ Simple to set up and rotate
- ✅ User-scoped tokens
- ✅ Optional expiration dates
- ✅ Can be revoked from UI

#### Basic Authentication

For development or when PAT is not available:

```yaml
mcp-server:
  environment:
    OPENL_BASE_URL: http://studio:8080/rest
    OPENL_USERNAME: admin
    OPENL_PASSWORD: admin
    OPENL_CLIENT_DOCUMENT_ID: docker-compose-1
```

#### OAuth 2.1 with PKCE (Recommended for Public Clients)

For public clients without `client_secret`, use PKCE (Proof Key for Code Exchange):

```yaml
mcp-server:
  environment:
    OPENL_BASE_URL: http://studio:8080/rest
    OPENL_OAUTH2_CLIENT_ID: your-public-client-id
    OPENL_OAUTH2_TOKEN_URL: https://auth.example.com/oauth/token
    OPENL_OAUTH2_AUTHORIZATION_URL: https://auth.example.com/oauth/authorize
    OPENL_OAUTH2_AUTHORIZATION_CODE: ${AUTHORIZATION_CODE}
    OPENL_OAUTH2_CODE_VERIFIER: ${CODE_VERIFIER}
    OPENL_OAUTH2_REDIRECT_URI: https://your-app.com/callback
    OPENL_OAUTH2_GRANT_TYPE: authorization_code
    OPENL_OAUTH2_SCOPE: "openl:read openl:write"
```

**Generating Code Verifier:**

```bash
# Generate a secure code_verifier (128 characters)
node -e "const crypto = require('crypto'); const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'; const random = crypto.randomBytes(128); let result = ''; for (let i = 0; i < 128; i++) { result += charset[random[i] % charset.length]; } console.log(result);"
```

**PKCE Flow:**
1. Generate `code_verifier` (store securely)
2. Generate authorization URL with `code_challenge` (auto-generated by MCP server)
3. User authorizes and receives `authorization_code`
4. Exchange `authorization_code` + `code_verifier` for access token
5. MCP server automatically refreshes tokens using refresh_token

#### OAuth 2.1 with Client Credentials (Service-to-Service)

For service-to-service authentication:

```yaml
mcp-server:
  environment:
    OPENL_BASE_URL: http://studio:8080/rest
    OPENL_OAUTH2_CLIENT_ID: your-client-id
    OPENL_OAUTH2_CLIENT_SECRET: your-client-secret
    OPENL_OAUTH2_TOKEN_URL: https://auth.example.com/oauth/token
    OPENL_OAUTH2_GRANT_TYPE: client_credentials
    OPENL_OAUTH2_SCOPE: "openl:read openl:write"
```

#### OAuth 2.1 with Refresh Token

For long-lived refresh tokens:

```yaml
mcp-server:
  environment:
    OPENL_BASE_URL: http://studio:8080/rest
    OPENL_OAUTH2_CLIENT_ID: your-client-id
    OPENL_OAUTH2_CLIENT_SECRET: your-client-secret
    OPENL_OAUTH2_TOKEN_URL: https://auth.example.com/oauth/token
    OPENL_OAUTH2_REFRESH_TOKEN: your-refresh-token
    OPENL_OAUTH2_GRANT_TYPE: refresh_token
```

See [Authentication Guide](../guides/AUTHENTICATION.md) for detailed authentication setup.

### Changing URL

If Docker container is accessible at a different address:

```json
{
  "url": "http://your-docker-host:3000/mcp/sse",
  "transport": "sse"
}
```

Or if using Nginx proxy:

```json
{
  "url": "http://localhost/mcp/sse",
  "transport": "sse"
}
```

### Available Endpoints

- **SSE Endpoint:** `GET http://localhost:3000/mcp/sse` - Establishes SSE connection for MCP protocol
- **Messages Endpoint:** `POST http://localhost:3000/mcp/messages?sessionId=xxx` - Sends MCP messages
- **REST API:** `GET http://localhost:3000/tools` - List tools (for debugging)
- **Health Check:** `GET http://localhost:3000/health` - Server health status

## Verification

### 1. Check Status in Cursor

- Open Cursor settings
- Find MCP section
- Ensure `openl-mcp-server-docker` is connected (green dot)

### 2. Test in Cursor Chat

In Cursor chat, try:

```text
List repositories in OpenL Tablets
```

or

```text
Show projects in the design repository
```

### 3. Check Logs

If something doesn't work, check proxy logs:

```bash
# Proxy outputs logs to stderr
# They should be visible in Cursor logs
```

## Troubleshooting

### Issue: "Failed to connect to MCP HTTP API"

**Solution:**
1. Ensure Docker container is running:
   ```bash
   docker compose ps mcp-server
   ```

2. Check HTTP API availability:
   ```bash
   curl http://localhost:3000/health
   ```

3. Check that port 3000 is not occupied by another process:
   ```bash
   lsof -i :3000
   ```

### Issue: "Cannot connect to SSE endpoint"

**Solution:**
1. Ensure Docker container is running:
   ```bash
   docker compose ps mcp-server
   ```

2. Check SSE endpoint is accessible:
   ```bash
   curl http://localhost:3000/mcp/sse
   ```
   Should return SSE stream headers

3. Verify URL in Cursor configuration is correct:
   ```text
   http://localhost:3000/mcp/sse
   ```

### Issue: Connection fails

**Solution:**
1. Check URL in Cursor configuration (should be `http://localhost:3000/mcp/sse`)
2. Verify transport type is set to `sse`
3. Check Cursor logs for errors
4. Ensure Docker container is accessible from your machine

### Issue: Tools don't work

**Solution:**
1. Check that Docker container is running:
   ```bash
   docker compose logs mcp-server
   ```

2. Check OpenL Studio availability:
   ```bash
   curl http://localhost:8080/rest/projects
   ```

3. Ensure SSE endpoint is accessible:
   ```bash
   curl -N http://localhost:3000/mcp/sse
   ```
   Should start SSE stream

## Benefits of Using Docker

✅ **Isolation**: MCP Server runs in an isolated environment  
✅ **Scalability**: Easy to scale via Docker Compose  
✅ **Consistency**: Same environment on all machines  
✅ **Management**: Easy to update and restart  
✅ **Monitoring**: Built-in logs and health checks  

## Alternative: Using Local Proxy

If you prefer to use a local proxy instead of direct HTTP connection:

1. **Copy the example configuration:**
   ```bash
   cp docs/setup/examples/cursor-docker-config.example.json cursor-docker-config.json
   ```

2. **Replace the placeholder** in `cursor-docker-config.json`:
   - Replace `<path-to-project>` with the absolute path to your OpenL Tablets project directory
   - Example: `/Users/username/projects/openl-tablets` or `/path/to/openl-tablets`
   - You can find your path by running `pwd` from the project root directory

3. **Ensure the proxy is built:**
   ```bash
   cd <path-to-project>/mcp-server
   npm run build
   ```

4. **Use the configuration** in Cursor IDE settings

**Note:** This method requires Node.js on the client machine. The proxy runs locally and connects to Docker via HTTP.

However, **direct HTTP connection is recommended** as it doesn't require any local setup.

## Additional Information

- [Claude Desktop & Cursor Setup](CLAUDE-DESKTOP.md) - General setup guide for Claude Desktop and Cursor
- [Docker Setup](DOCKER.md) - Docker deployment information
- [Main README](../../README.md) - Complete MCP server documentation

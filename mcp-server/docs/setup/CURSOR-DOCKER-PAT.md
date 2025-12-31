# 🐳 Setting Up Cursor with MCP Server in Docker and PAT

This guide describes how to connect Cursor IDE to the OpenL Tablets MCP server running in a Docker container using Personal Access Token (PAT) for authentication.

## Architecture

```text
┌─────────────┐
│   Cursor    │  ← Cursor IDE (locally or remotely)
└──────┬──────┘
       │ HTTP SSE (MCP Protocol)
       │ Connection to Docker container
       │
       ▼
┌─────────────┐
│ MCP Server  │  ← Docker container (port 3333)
│  (Docker)   │     Express HTTP API with SSE transport
└──────┬──────┘
       │ HTTP REST API
       │ Authorization: Token <PAT>
       │
       ▼
┌─────────────┐
│ OpenL Studio│  ← OpenL Tablets backend (port 8080)
│  (Docker)   │
└─────────────┘
```

**Advantages:**
- ✅ Cursor connects directly to Docker container via HTTP
- ✅ No Node.js required on client machine
- ✅ PAT provides secure authentication
- ✅ Easy to scale and manage

## Step 1: Create Personal Access Token

1. Open OpenL Tablets Studio in your browser
2. Log in using OAuth2/SAML
3. Go to **User Settings** → **Personal Access Tokens**
4. Click **Create Token**
5. Enter token name (e.g., "Docker MCP Server")
6. Select expiration date (or leave blank for no expiration)
7. **Copy the token immediately** - it's shown only once!

Your token has the format:
```
<your-pat-token>
```

## Step 2: Configure Docker Container

**IMPORTANT**: Authentication credentials should NOT be set in Docker configuration. They must be provided via MCP client configuration (query parameters or headers when connecting via HTTP).

The Docker configuration should only include the base URL:

```yaml
mcp-server:
  environment:
    PORT: 3000
    OPENL_BASE_URL: http://studio:8080/rest
    NODE_ENV: production
    # Authentication is NOT set here!
```

Start the container:
```bash
cd /path/to/openl-tablets
docker compose up -d mcp-server
```

## Step 3: Verify MCP Server Availability

### Local Docker

```bash
curl http://localhost:3333/health
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

### Remote Docker

If Docker container is on a remote server:

```bash
curl http://your-docker-host:3333/health
```

**Important:** Make sure port 3333 is open in firewall and accessible from network.

## Step 4: Configure Cursor

### Method 1: PAT via HTTP Headers (Recommended)

PAT is passed through a separate `headers` field in configuration - cleaner than query parameters.

**Important:** Base URL (`OPENL_BASE_URL`) must be configured on the server side (Docker environment variable `OPENL_BASE_URL`). Only authentication token is passed from client.

1. Open Cursor
2. Press `Cmd + ,` (or `File` → `Preferences` → `Settings`)
3. Find the **"MCP"** or **"Model Context Protocol"** section
4. Click **"Add New Global MCP Server"** or **"+"**
5. Enter name: `openl-mcp-server-docker`
6. Configure via JSON:

```json
{
  "url": "http://localhost:3333/mcp/sse",
  "transport": "sse",
  "headers": {
    "Authorization": "Token <your-pat-token>"
  }
}
```

**Important:** 
- Replace token with your own PAT
- For remote Docker, replace `localhost` with server IP/domain
- Base URL is configured in Docker container (see Step 2)

7. Save and check connection status

**Advantages of passing PAT via Authorization header:**
- ✅ Standard HTTP authentication header (RFC 7235)
- ✅ Clean URL without query parameters
- ✅ Token in separate configuration field
- ✅ Each user can use their own PAT
- ✅ Easy to change tokens without restarting container
- ✅ Base URL configured once on server side

### Method 2: PAT via Query Parameters URL (Alternative)

If Cursor doesn't support passing headers, use query parameters:

**Note:** Base URL must be configured on the server side. Only authentication token is passed via query parameter.

```json
{
  "url": "http://localhost:3333/mcp/sse?OPENL_PERSONAL_ACCESS_TOKEN=<your-pat-token>",
  "transport": "sse"
}
```

### Method 3: Through Configuration File

If Cursor uses a configuration file:

1. Open the file:
   ```bash
   ~/Library/Application Support/Cursor/User/settings.json
   ```

2. Add or update the `mcpServers` section:

**For local Docker:**
```json
{
  "mcpServers": {
    "openl-mcp-server-docker": {
      "url": "http://localhost:3333/mcp/sse",
      "transport": "sse",
      "headers": {
        "Authorization": "Token your-token-here"
      }
    }
  }
}
```

**For remote Docker:**
```json
{
  "mcpServers": {
    "openl-mcp-server-docker": {
      "url": "http://your-docker-host:3333/mcp/sse",
      "transport": "sse",
      "headers": {
        "Authorization": "Token your-token-here"
      }
    }
  }
}
```

**Note:** Base URL (`OPENL_BASE_URL`) must be configured in Docker container environment variables (see Step 2).

## Step 5: Verify Connection

1. Save settings
2. Restart Cursor (if necessary)
3. Check MCP server status in settings - should be "Connected" or green dot

## Step 6: Test

In Cursor chat, try:

```
List repositories in OpenL Tablets
```

or

```
Show projects in the design repository
```

Cursor should use MCP tools to execute the request.

## Troubleshooting

### MCP Server Not Connecting

1. **Check that container is running:**
   ```bash
   docker compose ps mcp-server
   ```
   or
   ```bash
   docker ps | grep mcp-server
   ```

2. **Check container logs:**
   ```bash
   docker compose logs mcp-server
   ```
   Look for authentication errors or connection issues to OpenL Studio

3. **Check HTTP API availability:**
   ```bash
   curl http://localhost:3333/health
   ```

4. **Check that port is not occupied:**
   ```bash
   lsof -i :3333
   ```

### 401 Unauthorized Error

1. **Check container logs:**
   ```bash
   docker compose logs mcp-server | grep -i auth
   ```
   Look for authentication error messages

2. **Check that token is correct:**
   - Make sure token is copied completely
   - Check that token hasn't expired (if expiration was set)
   - Make sure token wasn't deleted in UI

3. **Check OpenL Studio URL:**
   ```bash
   docker compose exec mcp-server env | grep OPENL_BASE_URL
   ```
   Should be: `http://studio:8080/rest`

### Cannot Connect to Remote Docker

1. **Check port availability:**
   ```bash
   telnet your-docker-host 3333
   ```
   or
   ```bash
   nc -zv your-docker-host 3333
   ```

2. **Check firewall:**
   - Make sure port 3333 is open in firewall
   - Check security rules in cloud provider (if used)

3. **Check URL in Cursor:**
   - Use correct IP or domain name
   - Make sure correct port is used (3333 for compose.yaml, 3000 for docker-compose.yml)

### Verify PAT Configuration

```bash
# Check connection to OpenL Studio
docker compose exec mcp-server curl -H "Authorization: Token <your-pat-token>" http://studio:8080/rest/repos
```

## Security

### Recommendations

1. **Store tokens securely:**
   - Use MCP client configuration instead of hardcoding in files
   - Don't commit tokens to git
   - Use secrets managers for production

2. **Limit access:**
   - Use firewall to limit access to port 3333
   - Use HTTPS with reverse proxy for production
   - Regularly rotate tokens

3. **Monitoring:**
   - Track token usage in OpenL Tablets UI
   - Check logs for suspicious activity
   - Delete unused tokens

## Additional Information

- [Full Docker Guide](./CURSOR-DOCKER.md)
- [Authentication Guide](../guides/AUTHENTICATION.md)
- [Configuration Examples](./examples/cursor-docker-pat-config.example.json)

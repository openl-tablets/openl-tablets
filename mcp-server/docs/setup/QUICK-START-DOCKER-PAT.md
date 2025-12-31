# 🚀 Quick Start: Cursor + Docker + PAT

Quick guide for setting up Cursor to connect to MCP server in Docker container using Personal Access Token.

## Your Token

```
<your-pat-token>
```

## Step 1: Configure Docker Container

**IMPORTANT**: Authentication credentials should NOT be set in Docker configuration. They must be provided via MCP client configuration.

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
docker compose up -d mcp-server
```

## Step 2: Verify Availability

```bash
curl http://localhost:3333/health
```

Should return `{"status":"ok",...}`

## Step 3: Configure Cursor

### Option A: PAT via HTTP Headers (Recommended)

PAT is passed through a separate `headers` field - cleaner approach.

**Important:** Base URL (`OPENL_BASE_URL`) must be configured on the server side (Docker environment variable or server config). Only authentication token is passed from client.

1. Open Cursor Settings (`Cmd + ,`)
2. Find the **"MCP"** or **"Model Context Protocol"** section
3. Click **"Add New Global MCP Server"**
4. Enter name: `openl-mcp-server-docker`
5. Paste JSON configuration:

```json
{
  "url": "http://localhost:3333/mcp/sse",
  "transport": "sse",
  "headers": {
    "Authorization": "Token <your-pat-token>"
  }
}
```

6. Save and check connection status

### Option B: PAT via Query Parameters URL

If Cursor doesn't support headers, use query parameters:

**Note:** Base URL must be configured on the server side. Only authentication token is passed via query parameter.

```json
{
  "url": "http://localhost:3333/mcp/sse?OPENL_PERSONAL_ACCESS_TOKEN=<your-pat-token>",
  "transport": "sse"
}
```

## Step 4: Test

In Cursor chat:
```
List repositories in OpenL Tablets
```

## For Remote Docker

If Docker container is on a remote server:

1. Replace `localhost` with server IP or domain name:
   ```json
   {
     "url": "http://your-server-ip:3333/mcp/sse",
     "transport": "sse",
     "headers": {
       "Authorization": "Token your-token-here"
     }
   }
   ```
   
   **Note:** Base URL (`OPENL_BASE_URL`) must be configured in Docker container environment variables on the server.

2. Make sure port 3333 is open in firewall

## Troubleshooting

### Check Container
```bash
docker compose ps mcp-server
docker compose logs mcp-server | grep -i auth
```

### Check Connection to OpenL Studio
```bash
docker compose exec mcp-server curl http://studio:8080/rest/repos
```

## Additional Information

- [Full Docker + PAT Guide](./CURSOR-DOCKER-PAT.md)
- [Authentication Guide](../guides/AUTHENTICATION.md)

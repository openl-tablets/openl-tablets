# Authentication Configuration for MCP Server

**IMPORTANT**: Authentication variables (tokens, passwords, OAuth credentials) **MUST NOT** be set in Docker configuration or server environment variables. They should be configured **only** in the MCP client configuration when connecting through Cursor or Claude Desktop.

## How It Works

The MCP server supports two modes of operation:

1. **stdio transport** (for Cursor/Claude Desktop) - authentication is set in the MCP client configuration via environment variables in the config file
2. **HTTP transport** (for Docker) - authentication is passed via query parameters or HTTP headers when connecting

## Authentication Setup

### For Cursor IDE or Claude Desktop (stdio transport)

Configure authentication in the MCP client configuration file:

**Example for Cursor** (`~/.cursor/mcp.json` or Cursor settings):

```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["/path/to/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/rest",
        "OPENL_PERSONAL_ACCESS_TOKEN": "<your-pat-token>",
        "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
      }
    }
  }
}
```

**Example for Claude Desktop** (`~/Library/Application Support/Claude/claude_desktop_config.json` on macOS):

```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["/path/to/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/rest",
        "OPENL_PERSONAL_ACCESS_TOKEN": "<your-pat-token>",
        "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-1"
      }
    }
  }
}
```

### For HTTP Transport (Docker)

When connecting via HTTP, authentication is passed through:

1. **Query parameters** in URL:
   ```
   http://localhost:3333/mcp/sse?OPENL_BASE_URL=http://studio:8080/rest&OPENL_PERSONAL_ACCESS_TOKEN=your_token_here
   ```

2. **HTTP headers** (X-OPENL-*):
   ```
   X-OPENL-BASE-URL: http://studio:8080/rest
   X-OPENL-PERSONAL-ACCESS-TOKEN: your_token_here
   ```

## Docker Configuration

In Docker configuration (`compose.yaml` or `docker-compose.yml`), **only** the base URL is set:

```yaml
environment:
  PORT: 3000
  OPENL_BASE_URL: http://studio:8080/rest
  NODE_ENV: production
  # Authentication is NOT set here!
```

## Authentication Methods

### Personal Access Token (PAT) - Recommended

```json
{
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_PERSONAL_ACCESS_TOKEN": "openl_pat_...",
    "OPENL_CLIENT_DOCUMENT_ID": "client-id"
  }
}
```

### Basic Auth (username/password)

```json
{
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_USERNAME": "admin",
    "OPENL_PASSWORD": "admin",
    "OPENL_CLIENT_DOCUMENT_ID": "client-id"
  }
}
```

### OAuth 2.1

```json
{
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_OAUTH2_CLIENT_ID": "your-client-id",
    "OPENL_OAUTH2_CLIENT_SECRET": "your-client-secret",
    "OPENL_OAUTH2_TOKEN_URL": "https://auth.example.com/oauth/token",
    "OPENL_OAUTH2_GRANT_TYPE": "client_credentials",
    "OPENL_CLIENT_DOCUMENT_ID": "client-id"
  }
}
```

## Verification

### Check health endpoint

```bash
curl http://localhost:3333/health
```

### Check logs

```bash
docker compose logs mcp-server
```

Should show:
```
✅ Default OpenL client initialized with base URL: http://studio:8080/rest
ℹ️  Authentication will be provided per-session via query parameters or headers
ℹ️  Do NOT set authentication credentials in Docker/environment variables
ℹ️  Configure authentication in your MCP client (Cursor/Claude Desktop) settings
```

## Security

⚠️ **Important**: Never set tokens, passwords, or other secrets in:
- Docker compose files
- Host environment variables (for Docker)
- Git repository
- Logs

✅ **Correct**: Set secrets only in:
- MCP client configuration files (Cursor/Claude Desktop)
- Query parameters or headers when connecting via HTTP (for one-time connections)

## Configuration Examples

Full configuration examples are available in:
- `docs/setup/examples/claude-desktop-config.example.json`
- `docs/setup/examples/cursor-pat-config.example.json`
- `docs/setup/examples/cursor-docker-config.example.json`


# 🎯 Setting up MCP Server in Cursor IDE

## Overview

Cursor IDE supports Model Context Protocol (MCP) for integration with external services. This document describes how to connect OpenL Tablets MCP Server to Cursor.

## 🐳 Connecting to Docker Container (Direct HTTP Connection)

If MCP Server is running in a Docker container, you can connect directly via HTTP using SSE transport:

### Direct HTTP Connection Setup

1. **Ensure Docker container is running:**
   ```bash
   docker compose up mcp-server
   # or
   docker compose up
   ```

2. **Check HTTP API availability:**
   ```bash
   curl http://localhost:3000/health
   ```

3. **Configure Cursor to connect via HTTP:**
   - In Cursor settings, add MCP server
   - **Type:** Select `sse` or `streamablehttp`
   - **URL:** `http://localhost:3000/mcp/sse`

   Or use JSON configuration:
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

**Note:** This connects directly to the Docker container without requiring a local proxy or Node.js on the client machine.

---

## Method 1: Via Cursor UI (Recommended)

### Step 1: Open Cursor Settings

1. Launch Cursor
2. Press `Cmd + ,` (or `File` → `Preferences` → `Settings`)
3. Search for "MCP"

### Step 2: Add MCP Server

1. Find the **"MCP"** or **"Model Context Protocol"** section
2. Click **"Add New Global MCP Server"** or **"+"**
3. Enter name: `openl-mcp-server`

### Step 3: Configure

Paste the following configuration:

```json
{
  "command": "node",
  "args": [
    "/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"
  ],
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_USERNAME": "admin",
    "OPENL_PASSWORD": "admin",
    "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
  }
}
```

**Important:** Replace the path with the absolute path to your project if it differs.

### Step 4: Save and Activate

1. Save settings
2. Ensure server status shows "Connected" or a green dot
3. Restart Cursor if necessary

## Method 2: Via Configuration File

### Configuration Location

Cursor may store MCP configuration in one of the following locations:

1. **User settings:**
   ```
   ~/Library/Application Support/Cursor/User/settings.json
   ```

2. **Global storage:**
   ```
   ~/Library/Application Support/Cursor/User/globalStorage/
   ```

### Adding Configuration

If Cursor supports configuration via file, add to `settings.json`:

```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": [
        "/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"
      ],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin",
        "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
      }
    }
  }
}
```

## Connection Verification

### 1. Check Status in Settings

- Open Cursor settings
- Find MCP section
- Ensure `openl-mcp-server` is displayed and connected

### 2. Test via Cursor Chat

In Cursor chat, try:

```
List repositories in OpenL Tablets
```

or

```
Show projects in the design repository
```

Cursor should use MCP tools to execute the request.

### 3. Check Logs

If something doesn't work, check Cursor logs:

**macOS:**
```bash
tail -f ~/Library/Logs/Cursor/*.log
```

Look for errors related to:
- `openl-mcp-server`
- `OPENL_BASE_URL`
- `node dist/index.js`

## Requirements

Before setup, ensure:

1. ✅ **MCP Server is built:**
   ```bash
   cd /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server
   npm run build
   ```

2. ✅ **OpenL Tablets is running:**
   ```bash
   # Check availability
   curl http://localhost:8080/rest/projects
   ```

3. ✅ **Node.js is installed:**
   ```bash
   node --version  # Should be >= 18.0.0
   ```

## Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `OPENL_BASE_URL` | OpenL Studio API URL | `http://localhost:8080/rest` |
| `OPENL_USERNAME` | Username | `admin` |
| `OPENL_PASSWORD` | Password | `admin` |
| `OPENL_CLIENT_DOCUMENT_ID` | Client ID for tracking | `cursor-ide-1` |

## Troubleshooting

### Issue: MCP Server doesn't appear in list

**Solution:**
1. Check path to `dist/index.js` (must be absolute)
2. Ensure project is built: `npm run build`
3. Restart Cursor

### Issue: "Cannot connect to OpenL API"

**Solution:**
1. Ensure OpenL Tablets is running:
   ```bash
   curl http://localhost:8080/rest/projects
   ```
2. Check `OPENL_BASE_URL` in configuration
3. Ensure URL ends with `/rest` (not `/webstudio/rest`)

### Issue: "Authentication failed"

**Solution:**
1. Check credentials (`OPENL_USERNAME`, `OPENL_PASSWORD`)
2. Try logging into OpenL via browser with the same credentials
3. Ensure user has necessary permissions

### Issue: Tools don't work in chat

**Solution:**
1. Check MCP server status in settings (should be "Connected")
2. Try explicitly asking Cursor to use the tool
3. Check Cursor logs for errors

## Alternative: Using HTTP API

If stdio MCP doesn't work, you can use HTTP API via Express server:

1. Start HTTP server:
   ```bash
   cd mcp-server
   npm run start:http
   ```

2. Use HTTP API directly:
   ```bash
   curl http://localhost:3000/execute \
     -H "Content-Type: application/json" \
     -d '{"tool": "openl_list_repositories", "arguments": {}}'
   ```

However, for Cursor integration, stdio MCP is recommended.

## Additional Information

- [README.md](./README.md) - Main MCP server documentation
- [DOCKER.md](./DOCKER.md) - Docker deployment information
- [AUTHENTICATION.md](./AUTHENTICATION.md) - Authentication setup

## Useful Links

- [Cursor MCP Documentation](https://docs.cursor.com/context/model-context-protocol)
- [Model Context Protocol](https://modelcontextprotocol.io/)

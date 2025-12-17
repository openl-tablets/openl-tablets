# 🚀 Setting up MCP Server for Claude Desktop and Cursor IDE

This guide covers setting up the OpenL Tablets MCP Server for both Claude Desktop and Cursor IDE.

## Table of Contents

- [Claude Desktop Setup](#claude-desktop-setup)
- [Cursor IDE Setup](#cursor-ide-setup)
- [Path Determination](#path-determination)
- [Verification](#verification)
- [Troubleshooting](#troubleshooting)

---

## Claude Desktop Setup

### Quick Launch

#### Method 1: Via Finder
1. Open Finder
2. Go to "Applications" folder
3. Find "Claude"
4. Double-click to launch

#### Method 2: Via Spotlight
1. Press `Cmd + Space` (open Spotlight)
2. Type "Claude"
3. Press Enter

#### Method 3: Via Terminal
```bash
open -a Claude
```

### First Launch

On first launch of Claude Desktop:

1. **Sign in to Anthropic Account**
   - If you don't have an account, create one at https://claude.ai
   - Sign in with your credentials

2. **Configure MCP Servers**
   - Configuration file location:
     - **macOS**: `~/Library/Application Support/Claude/config.json`
     - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`
   - Add MCP server configuration (see below)

3. **Check MCP Connection**
   - Open settings (⚙️) or `Cmd + ,`
   - Find "MCP Servers" or "Model Context Protocol" section
   - Ensure `openl-mcp-server` is displayed

### Configuration

**macOS:**
```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["/absolute/path/to/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin",
        "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-1"
      }
    }
  }
}
```

**Windows:**
```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": ["C:\\absolute\\path\\to\\mcp-server\\dist\\index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin",
        "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-1"
      }
    }
  }
}
```

**Important:** Replace `/absolute/path/to/mcp-server` with your actual project path. See [Path Determination](#path-determination) section below.

### Verifying MCP Server Operation

After launching Claude Desktop:

1. **Check Status in Settings**
   - Open Claude Desktop
   - Click settings icon (⚙️) or `Cmd + ,`
   - Find "MCP Servers" section
   - Should see `openl-mcp-server` with status:
     - ✅ "Connected" - everything works!
     - ⚠️ "Disconnected" - check configuration
     - ❌ Error - see logs

2. **Test in Chat**
   - In Claude chat, write:
     ```
     List repositories in OpenL Tablets
     ```
   - Claude should use MCP tools and show the list of repositories

---

## Cursor IDE Setup

### Overview

Cursor IDE supports Model Context Protocol (MCP) for integration with external services. This section describes how to connect OpenL Tablets MCP Server to Cursor.

### Method 1: Via Cursor UI (Recommended)

#### Step 1: Open Cursor Settings

1. Launch Cursor
2. Press `Cmd + ,` (or `File` → `Preferences` → `Settings`)
3. Search for "MCP"

#### Step 2: Add MCP Server

1. Find the **"MCP"** or **"Model Context Protocol"** section
2. Click **"Add New Global MCP Server"** or **"+"**
3. Enter name: `openl-mcp-server`

#### Step 3: Configure

Paste the following configuration:

```json
{
  "command": "node",
  "args": [
    "<absolute-path-to-openl-tablets>/mcp-server/dist/index.js"
  ],
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_USERNAME": "admin",
    "OPENL_PASSWORD": "admin",
    "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
  }
}
```

**Important:** Replace `<absolute-path-to-openl-tablets>` with your actual project path. See [Path Determination](#path-determination) section below.

#### Step 4: Save and Activate

1. Save settings
2. Ensure server status shows "Connected" or a green dot
3. Restart Cursor if necessary

### Method 2: Via Configuration File

#### Configuration Location

Cursor may store MCP configuration in one of the following locations:

1. **User settings:**
   ```text
   ~/Library/Application Support/Cursor/User/settings.json
   ```

2. **Global storage:**
   ```text
   ~/Library/Application Support/Cursor/User/globalStorage/
   ```

#### Adding Configuration

If Cursor supports configuration via file, add to `settings.json`:

```json
{
  "mcpServers": {
    "openl-mcp-server": {
      "command": "node",
      "args": [
        "<absolute-path-to-openl-tablets>/mcp-server/dist/index.js"
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

**Note:** Replace `<absolute-path-to-openl-tablets>` with your actual project path.

### Connection Verification

#### 1. Check Status in Settings

- Open Cursor settings
- Find MCP section
- Ensure `openl-mcp-server` is displayed and connected

#### 2. Test via Cursor Chat

In Cursor chat, try:

```text
List repositories in OpenL Tablets
```

or

```text
Show projects in the design repository
```

Cursor should use MCP tools to execute the request.

#### 3. Check Logs

If something doesn't work, check Cursor logs:

**macOS:**
```bash
tail -f ~/Library/Logs/Cursor/*.log
```

Look for errors related to:
- `openl-mcp-server`
- `OPENL_BASE_URL`
- `node dist/index.js`

---

## Path Determination

Before configuring the MCP server, you need to determine the absolute path to your OpenL Tablets project directory.

### Finding Your Project Path

**macOS/Linux:**
```bash
# Navigate to the mcp-server directory
cd /path/to/openl-tablets/mcp-server
# Get the absolute path
pwd
# Output example: /Users/username/projects/openl-tablets/mcp-server
# Use: /Users/username/projects/openl-tablets/mcp-server/dist/index.js
```

**Windows:**
```powershell
# Navigate to the mcp-server directory
cd C:\path\to\openl-tablets\mcp-server
# Get the absolute path
pwd
# Output example: C:\Users\username\projects\openl-tablets\mcp-server
# Use: C:\Users\username\projects\openl-tablets\mcp-server\dist\index.js
```

**VS Code/Cursor:**
- Use `$(workspaceFolder)` variable if supported
- Or right-click the `mcp-server` folder → "Copy Path" → append `/dist/index.js`

**Quick Method:**
```bash
# From the project root
cd openl-tablets/mcp-server
echo "$(pwd)/dist/index.js"  # macOS/Linux
# or
echo "%cd%\dist\index.js"   # Windows CMD
```

---

## Requirements

Before setup, ensure:

1. ✅ **MCP Server is built:**
   ```bash
   cd <absolute-path-to-openl-tablets>/mcp-server
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

---

## Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `OPENL_BASE_URL` | OpenL Studio API URL | `http://localhost:8080/rest` |
| `OPENL_USERNAME` | Username | `admin` |
| `OPENL_PASSWORD` | Password | `admin` |
| `OPENL_CLIENT_DOCUMENT_ID` | Client ID for tracking | `claude-desktop-1` or `cursor-ide-1` |

For more authentication options, see [Authentication Guide](../guides/AUTHENTICATION.md).

---

## Troubleshooting

### Issue: MCP Server doesn't appear in list

**Solution:**
1. Check path to `dist/index.js` (must be absolute)
2. Ensure project is built: `npm run build`
3. Restart Claude Desktop or Cursor

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
2. Try explicitly asking the AI to use the tool
3. Check logs for errors

### Issue: File doesn't exist

**Solution:**
1. Ensure project is built:
   ```bash
   cd mcp-server
   npm run build
   ls -la dist/index.js  # Should exist
   ```
2. Verify the path in configuration matches the actual location

For more detailed troubleshooting, see [Troubleshooting Guide](../guides/TROUBLESHOOTING.md).

---

## Additional Information

- [Quick Start Guide](../getting-started/QUICK-START.md) - Get up and running quickly
- [Docker Setup](DOCKER.md) - Running MCP server in Docker
- [Cursor Docker Setup](CURSOR-DOCKER.md) - Connecting Cursor to Docker container
- [Authentication Guide](../guides/AUTHENTICATION.md) - Authentication setup
- [Main README](../../README.md) - Complete MCP server documentation

---

## Useful Links

- [Claude Desktop Website](https://claude.ai/download)
- [Cursor MCP Documentation](https://docs.cursor.com/context/model-context-protocol)
- [Model Context Protocol](https://modelcontextprotocol.io/)


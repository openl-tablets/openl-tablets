# Claude Desktop Setup Guide for OpenL Tablets MCP

This guide helps you set up and test the OpenL Tablets MCP server with Claude Desktop.

---

## Prerequisites

1. ✅ OpenL Tablets 6.0.0+ running at `http://localhost:8080/webstudio`
2. ✅ Node.js 18+ installed
3. ✅ Claude Desktop installed
4. ✅ Admin credentials for OpenL Tablets (default: `admin`/`admin`)

---

## Step 1: Build the MCP Server

```bash
cd /home/user/openl-tablets/mcp-server

# Install dependencies (if not already done)
npm install

# Build the TypeScript code
npm run build

# Verify build succeeded
ls -la dist/
```

Expected output: You should see `index.js` and other compiled files in the `dist/` directory.

---

## Step 2: Configure Claude Desktop

### Find Claude Desktop Config Location

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`
**Linux**: `~/.config/Claude/claude_desktop_config.json`

### Update Configuration

Add or update your config with the OpenL Tablets MCP server:

```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/home/user/openl-tablets/mcp-server/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

**Important**: Update the paths and credentials for your environment:
- Change `/home/user/openl-tablets/mcp-server/dist/index.js` to your actual path
- Update `OPENL_BASE_URL` if using a different host/port
- Update username/password if different from defaults

### Configuration Options

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `OPENL_BASE_URL` | OpenL WebStudio REST API base URL | `http://localhost:8080/webstudio/rest` |
| `OPENL_USERNAME` | Username for authentication | `admin` |
| `OPENL_PASSWORD` | Password for authentication | `admin` |
| `OPENL_TIMEOUT` | Request timeout in milliseconds | `30000` (30 seconds) |

---

## Step 3: Restart Claude Desktop

1. **Completely quit** Claude Desktop (don't just close the window)
   - macOS: `Cmd+Q` or right-click dock icon → Quit
   - Windows: Exit from system tray
   - Linux: Close all windows and background processes

2. **Start** Claude Desktop again

3. **Verify MCP connection** in Claude Desktop:
   - Look for the hammer/tool icon in the input area
   - Click it to see available tools
   - You should see OpenL Tablets tools listed

---

## Step 4: Test Core Functionality

Try these queries in Claude Desktop to test the working tools:

### Test 1: List Projects
```
List all OpenL Tablets projects
```

Expected: Claude should use the `list_projects` tool and show you a list of projects with their IDs, names, and repositories.

### Test 2: Get Project Details
```
Tell me about the "Example 1 - Bank Rating" project from the design repository
```

Expected: Claude should use `get_project` with a project ID like `design-Example 1 - Bank Rating` and show project details.

### Test 3: List Tables
```
What tables are in the design-Example 1 - Bank Rating project?
```

Expected: Claude should use `list_tables` and show the tables in that project.

### Test 4: Get Table Details
```
Show me details about the first table in that project
```

Expected: Claude should use `get_table` and display table information.

### Test 5: Get Table Properties
```
What are the properties of that table?
```

Expected: Claude should use `get_table_properties` and show the dimension properties.

---

## Available MCP Tools

### ✅ Core Working Tools (P0 - Critical)

| Tool | Description | Example Usage |
|------|-------------|---------------|
| `list_projects` | List all projects | "Show me all OpenL projects" |
| `get_project` | Get project details | "Get details for design-Example 1" |
| `list_tables` | List tables in a project | "What tables are in this project?" |
| `get_table` | Get table details | "Show me the DriverAgeType table" |
| `get_table_properties` | Get dimension properties | "What are the table properties?" |

### ✅ Repository Management (P1)

| Tool | Description | Example Usage |
|------|-------------|---------------|
| `list_repositories` | List all repositories | "Show me all repositories" |
| `list_branches` | List branches in a repository | "What branches exist in design repo?" |

### ✅ File Operations (P1)

| Tool | Description | Example Usage |
|------|-------------|---------------|
| `download_file` | Download Excel rules file | "Download the rules.xlsx file" |

### ⚠️ Limited Functionality Tools

| Tool | Status | Notes |
|------|--------|-------|
| `open_project` | ⚠️ No-op | Projects are always accessible (returns success) |
| `close_project` | ⚠️ No-op | Projects don't need closing (returns success) |
| `validate_project` | ❌ 404 | Endpoint doesn't exist (validation may be automatic) |
| `run_all_tests` | ❌ 404 | Endpoint doesn't exist (use WebStudio UI) |
| `get_project_history` | ❌ 404 | Endpoint doesn't exist (history not available via API) |

---

## Troubleshooting

### Problem: "No tools available" in Claude Desktop

**Solutions**:
1. Check that config path is correct for your OS
2. Verify the `args` path points to the compiled `dist/index.js`
3. Restart Claude Desktop completely (quit and reopen)
4. Check Claude Desktop logs for errors

### Problem: "Connection refused" or "500 Internal Server Error"

**Solutions**:
1. Verify OpenL Tablets is running: `http://localhost:8080/webstudio`
2. Check `OPENL_BASE_URL` in config is correct
3. Verify username/password are correct
4. Check network connectivity to OpenL instance

### Problem: "404 Not Found" for all requests

**Solutions**:
1. Verify base URL includes `/webstudio/rest` (not just `/webstudio`)
2. Check OpenL Tablets version is 6.0.0 or higher
3. Try accessing the API directly in browser: `http://localhost:8080/webstudio/rest/projects`

### Problem: "Authentication failed"

**Solutions**:
1. Verify username and password in config
2. Check OpenL Tablets user has proper permissions
3. Try logging into WebStudio UI with same credentials
4. Check if Basic Auth is enabled in OpenL configuration

### Problem: Claude says a tool failed

**Solutions**:
1. Check tool error message for specific issue
2. Verify project ID format (should be like `design-Example 1 - Bank Rating`)
3. Some tools return 404 because endpoints don't exist (see Limited Functionality table above)
4. Check that the project/table/file actually exists in OpenL

---

## Viewing MCP Server Logs

To debug issues, you can run the MCP server directly and see logs:

```bash
cd /home/user/openl-tablets/mcp-server

# Set environment variables
export OPENL_BASE_URL="http://localhost:8080/webstudio/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Run server directly
node dist/index.js
```

This will show:
- MCP server startup messages
- Tool invocations
- API requests/responses
- Errors and stack traces

Press `Ctrl+C` to stop the server.

---

## Testing with MCP Inspector

For advanced debugging, use the MCP Inspector:

```bash
# Install MCP Inspector globally
npm install -g @modelcontextprotocol/inspector

# Run inspector with OpenL MCP server
npx @modelcontextprotocol/inspector node dist/index.js
```

This provides a web UI for testing MCP tools directly.

---

## Project ID Formats

The MCP server accepts three project ID formats:

1. **Dash format** (user-friendly): `design-Example 1 - Bank Rating`
2. **Colon format** (decoded): `design:Example 1 - Bank Rating`
3. **Base64 format** (API native): `ZGVzaWduOkV4YW1wbGUgMSAtIEJhbmsgUmF0aW5n`

All three formats work interchangeably. Claude Desktop will typically use the dash format.

---

## Next Steps

Once working in Claude Desktop, you can:

1. **Explore projects**: Ask Claude to list and describe your OpenL projects
2. **Analyze tables**: Have Claude explain table structures and properties
3. **Download files**: Get Excel files for offline analysis
4. **Navigate repositories**: Explore different repos and branches

---

## Known Limitations

Based on OpenL Tablets REST API capabilities:

1. **No project validation endpoint** - Validation likely happens automatically
2. **No project-level test execution** - Tests must be run through WebStudio UI
3. **No history retrieval** - Git history not exposed via REST API
4. **No file history** - File-level history not available via API
5. **Properties are embedded** - No separate properties endpoint (properties included in table details)

See **API_FINDINGS.md** and **INVESTIGATION_PLAN.md** for complete technical details.

---

## Support

For issues:
1. Check troubleshooting section above
2. Review logs from MCP server
3. Verify OpenL Tablets is working via WebStudio UI
4. Check official OpenL Tablets documentation

---

**Ready to use!** 🎉

The OpenL Tablets MCP server provides Claude Desktop with powerful capabilities to interact with your business rules management system.

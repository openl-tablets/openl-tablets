# 🚀 Setting Up Cursor with Personal Access Token

This is a quick guide for setting up the OpenL Tablets MCP server in Cursor IDE using Personal Access Token (PAT).

## Step 1: Get Personal Access Token

1. Open OpenL Tablets Studio in your browser
2. Log in using OAuth2/SAML
3. Go to **User Settings** → **Personal Access Tokens**
4. Click **Create Token**
5. Enter token name (e.g., "Cursor MCP")
6. Select expiration date (or leave blank for no expiration)
7. **Copy the token immediately** - it's shown only once!

Your token has the format:
```
<your-pat-token>
```

## Step 2: Configure Cursor

### Method 1: Through Cursor UI (Recommended)

1. Open Cursor
2. Press `Cmd + ,` (or `File` → `Preferences` → `Settings`)
3. Find the **"MCP"** or **"Model Context Protocol"** section
4. Click **"Add New Global MCP Server"** or **"+"**
5. Enter name: `openl-mcp-server`
6. Paste the following configuration:

```json
{
  "command": "node",
  "args": [
    "/Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js"
  ],
  "env": {
    "OPENL_BASE_URL": "http://localhost:8080/rest",
    "OPENL_PERSONAL_ACCESS_TOKEN": "<your-pat-token>",
    "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
  }
}
```

**Important:** 
- Replace the path `/Users/asamuseu/IdeaProjects/openl-tablets` with your actual project path
- Replace the token with your own PAT (if you're using a different token)

### Method 2: Through Configuration File

If Cursor uses a configuration file:

1. Open the file:
   ```bash
   ~/Library/Application Support/Cursor/User/settings.json
   ```

2. Add or update the `mcpServers` section:

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
        "OPENL_PERSONAL_ACCESS_TOKEN": "<your-pat-token>",
        "OPENL_CLIENT_DOCUMENT_ID": "cursor-ide-1"
      }
    }
  }
}
```

## Step 3: Verify Connection

1. Save settings
2. Restart Cursor (if necessary)
3. Check MCP server status in settings - should be "Connected" or green dot

## Step 4: Test

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

1. **Check Node.js path:**
   ```bash
   which node
   ```
   Use full path if `node` is not found in PATH

2. **Check project path:**
   ```bash
   ls /Users/asamuseu/IdeaProjects/openl-tablets/mcp-server/dist/index.js
   ```
   File should exist

3. **Check that OpenL Tablets is running:**
   ```bash
   curl http://localhost:8080/rest/repos
   ```
   Should return list of repositories (or authentication error)

4. **Check token:**
   - Make sure token is copied completely
   - Check that token hasn't expired (if expiration was set)
   - Make sure token wasn't deleted in UI

### 401 Unauthorized Error

- Check that token is correct and hasn't expired
- Make sure OpenL Tablets is configured for OAuth2/SAML mode
- Check that `OPENL_BASE_URL` points to correct endpoint (`/rest`)

### Debug Logs

Check Cursor logs:
```bash
tail -f ~/Library/Logs/Cursor/*.log
```

Look for errors related to:
- `openl-mcp-server`
- `OPENL_BASE_URL`
- `OPENL_PERSONAL_ACCESS_TOKEN`
- `node dist/index.js`

## Additional Information

- [Full Authentication Guide](../guides/AUTHENTICATION.md)
- [Claude Desktop and Cursor Setup](./CLAUDE-DESKTOP.md)
- [Configuration Examples](./examples/cursor-pat-config.example.json)

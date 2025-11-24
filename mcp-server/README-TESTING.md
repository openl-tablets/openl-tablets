# How to Verify MCP Server is Working

## Quick Check

Run the health check script:

```bash
cd mcp-server
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
./check-health.sh
```

## Detailed Check

### 1. Compilation and Build Check

```bash
cd mcp-server
npm run build
```

If build is successful, you'll only see TypeScript compiler output without errors.

### 2. Configuration Check

Ensure environment variables are set:

```bash
echo $OPENL_BASE_URL    # Should be: http://localhost:8080/rest
echo $OPENL_USERNAME     # Should be: admin (or your login)
echo $OPENL_PASSWORD     # Should be: admin (or your password)
```

Or check Claude Desktop configuration:

```bash
cat ~/Library/Application\ Support/Claude/config.json | grep -A 10 "openl-mcp-server"
```

### 3. OpenL API Connection Check

```bash
curl -u admin:admin http://localhost:8080/rest/repository
```

If OpenL is running, you'll get JSON with repository list or authentication error (which is normal if different credentials are needed).

### 4. Run Unit Tests

```bash
cd mcp-server
npm run test:unit
```

This will run all unit tests (without integration tests that require a running OpenL server).

### 5. Check in Claude Desktop

#### Step 1: Restart Claude Desktop

After changing configuration, be sure to restart Claude Desktop.

#### Step 2: Check MCP Server Status

In Claude Desktop:
1. Open settings (Settings)
2. Find "MCP Servers" or "Model Context Protocol" section
3. Ensure `openl-mcp-server` is displayed and has "Connected" or "Ready" status

#### Step 3: Try Using Tools

In Claude chat, try:

```
List repositories in OpenL Tablets
```

or

```
Show me projects in the design repository
```

Claude should use MCP tools to execute the request.

### 6. Check via Logs

If something doesn't work, check Claude Desktop logs:

**macOS:**
```bash
tail -f ~/Library/Logs/Claude/*.log
```

Look for errors related to:
- `openl-mcp-server`
- `OPENL_BASE_URL`
- `authentication`
- `connection`

### 7. Manual Server Launch Test

MCP server uses stdio for communication, so it can't be simply launched in terminal. But you can check that it starts without errors:

```bash
cd mcp-server
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"

# Attempt launch (server will wait for stdin, so we'll terminate after a few seconds)
timeout 2 node dist/index.js 2>&1 || echo "Server started (timeout expected)"
```

If you see an error about environment variables or configuration, it means the server is trying to start but something is wrong with settings.

## Common Issues

### Issue: "OPENL_BASE_URL environment variable is required"

**Solution:** Set environment variables or check Claude Desktop configuration.

### Issue: "Cannot connect to OpenL API"

**Solution:** 
1. Ensure OpenL Tablets is running
2. Check URL: should end with `/rest`
3. Check availability: `curl http://localhost:8080/rest/repository`

### Issue: "Authentication failed" or HTTP 401/403

**Solution:**
1. Check login and password correctness
2. Ensure user has necessary permissions in OpenL
3. Try connecting via browser with the same credentials

### Issue: MCP Server doesn't appear in Claude Desktop

**Solution:**
1. Check path to `dist/index.js` in configuration (must be absolute)
2. Ensure project is built: `npm run build`
3. Restart Claude Desktop
4. Check JSON syntax in `config.json`

### Issue: Tools don't work in Claude

**Solution:**
1. Ensure MCP server is connected (see status in settings)
2. Check Claude Desktop logs for errors
3. Try explicitly asking Claude to use the tool: "Use tool openl_list_repositories"

## Successful Verification

If everything works correctly, you should:

✅ See `openl-mcp-server` in Claude Desktop MCP servers list  
✅ Be able to ask Claude to perform operations with OpenL Tablets  
✅ Receive correct responses from OpenL API  
✅ See all 18 tools available in Claude  

## Additional Information

- [README.md](./README.md) - Main documentation
- [AUTHENTICATION.md](./AUTHENTICATION.md) - Authentication setup
- [TESTING.md](./TESTING.md) - Detailed testing guide
- [EXAMPLES.md](./EXAMPLES.md) - Usage examples

# 🔍 Troubleshooting Guide

This guide covers common issues and their solutions when working with the OpenL Tablets MCP Server.

## Table of Contents

- [Viewing Debug Logs](#viewing-debug-logs)
- [Common Issues](#common-issues)
- [OAuth 2.1 Issues](#oauth-21-issues)
- [Connection Issues](#connection-issues)
- [Configuration Issues](#configuration-issues)

---

## Viewing Debug Logs

### Quick Method

#### 1. Claude Desktop Logs (macOS)

```bash
# View latest logs in real-time
tail -f ~/Library/Logs/Claude/*.log

# Or via Console.app (GUI)
open -a Console
# Then find Claude Desktop logs
```

#### 2. Cursor IDE Logs (macOS)

```bash
# View latest logs in real-time
tail -f ~/Library/Logs/Cursor/*.log
```

#### 3. Run Server Directly for Debugging

The easiest way to see all logs - run the server directly in terminal:

```bash
cd /path/to/openl-tablets/mcp-server

# Set environment variables
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
export OPENL_CLIENT_DOCUMENT_ID="debug-1"

# Run the server
node dist/index.js
```

All logs will be visible directly in the terminal! Press `Ctrl+C` to stop.

### Detailed Debugging

#### Enable Verbose Logging

For more detailed logs, set the `DEBUG` environment variable:

```bash
export DEBUG=1
export NODE_ENV=development
node dist/index.js
```

#### Search for Specific Errors

**OpenL Connection Errors:**
```bash
tail -f ~/Library/Logs/Claude/*.log | grep -i "openl\|401\|unauthorized\|connection"
```

**OAuth2 Errors:**
```bash
tail -f ~/Library/Logs/Claude/*.log | grep -i "oauth2\|token\|authorization"
```

**Configuration Errors:**
```bash
tail -f ~/Library/Logs/Claude/*.log | grep -i "config\|environment\|missing"
```

### Log Structure

All MCP server logs have prefixes for easy filtering:

- `[Config]` - configuration logs
- `[Auth]` - authentication logs
- `[OAuth2]` - OAuth2 logs
- `[Browser]` - browser interaction logs
- `[Error]` - errors

**Example Log Filtering:**
```bash
# Errors only
tail -f ~/Library/Logs/Claude/*.log | grep "\[Error\]"

# OAuth2 logs only
tail -f ~/Library/Logs/Claude/*.log | grep "\[OAuth2\]"

# Configuration logs only
tail -f ~/Library/Logs/Claude/*.log | grep "\[Config\]"
```

### Server Health Checks

#### Test 1: Build Check

```bash
cd /path/to/openl-tablets/mcp-server
npm run build
```

If build is successful, there will be no errors.

#### Test 2: Configuration Check

```bash
# Check Claude Desktop configuration
cat ~/Library/Application\ Support/Claude/config.json | jq '.mcpServers["openl-mcp-server"]'
```

#### Test 3: OpenL Connection Check

```bash
curl -u admin:admin http://localhost:8080/rest/repository
```

If OpenL is running, you'll get JSON with repositories.

#### Test 4: Run Server with Verification

```bash
cd /path/to/openl-tablets/mcp-server

# Set environment variables from your configuration
export OPENL_BASE_URL="http://localhost:8080/rest"
export OPENL_USERNAME="admin"
export OPENL_PASSWORD="admin"
export OPENL_CLIENT_DOCUMENT_ID="test-1"

# Run the server
node dist/index.js
```

You should see initialization logs:
```
[Config] Loading configuration from environment variables...
[Config] Authentication methods:
[Config]   - OAuth2: not configured
[Config]   - API Key: not configured
[Config]   - Basic Auth: configured (username: admin)
```

If the server starts without errors, everything is working correctly.

### Useful Commands

**View last 100 lines of logs:**
```bash
tail -n 100 ~/Library/Logs/Claude/*.log
```

**Search for all errors in the last hour:**
```bash
grep -i error ~/Library/Logs/Claude/*.log | tail -20
```

**Monitor logs in real-time:**
```bash
tail -f ~/Library/Logs/Claude/*.log | grep --color=always -E "error|Error|ERROR|\[Error\]|\[Auth\]|\[OAuth2\]|\[Config\]"
```

**Save logs to file:**
```bash
tail -f ~/Library/Logs/Claude/*.log > mcp-debug.log 2>&1
```

---

## Common Issues

### Issue: "require is not defined"

**Solution:** Make sure the project is rebuilt:
```bash
cd /path/to/openl-tablets/mcp-server
npm run build
```

### Issue: "Unexpected token 'C', '[Config] Lo'... is not valid JSON"

**Solution:** This means logs are going to stdout. Make sure you're using the latest code where all logs go to stderr via `console.error()`.

### Issue: Server doesn't start

**Solution:** Check logs directly:
```bash
cd /path/to/openl-tablets/mcp-server
node dist/index.js 2>&1 | tee debug.log
```

Then open `debug.log` to view all errors.

### Issue: No logs in Claude Desktop

**Solution:** 
1. Make sure Claude Desktop is completely restarted (not just minimized)
2. Check that logging is enabled in Claude Desktop settings
3. Try running the server directly to verify

### Issue: MCP Server doesn't appear in AI client

**Solution:**
1. Check path to `dist/index.js` (must be absolute)
2. Ensure project is built: `npm run build`
3. Restart your AI client

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

---

## OAuth 2.1 Issues

### Error: `unauthorized_client`

#### Symptoms
```
Failed to obtain OAuth 2.1 token: Request failed with status code 400
Response: {"error":"unauthorized_client"}
```

#### Root Causes

The `unauthorized_client` error typically indicates one of the following issues:

1. **Client not configured for client_credentials grant type**
   - The OAuth client is not configured to allow `client_credentials` grant type
   - **Solution**: Check OAuth provider admin console → Applications → Your Client → Grant Types → Enable "Client Credentials"

2. **Missing required parameters**
   - Some OAuth providers require additional parameters like `scope` or `audience`
   - **Solution**: Add required parameters to configuration:
     ```yaml
     OPENL_OAUTH2_SCOPE: "openl:read openl:write"
     # or
     OPENL_OAUTH2_AUDIENCE: "https://api.example.com"
     ```

3. **Incorrect client credentials**
   - Client ID or Client Secret is incorrect
   - **Solution**: Verify credentials in OAuth provider admin console

4. **Client not enabled**
   - The OAuth client is disabled
   - **Solution**: Enable the client in OAuth provider admin console

#### Diagnostic Steps

1. **Verify Client Configuration:**
   - Login to OAuth provider admin console
   - Navigate to: Applications → Your Client
   - Check:
     - ✓ Client is enabled
     - ✓ Grant Types includes "Client Credentials"
     - ✓ Client ID matches your configured value
     - ✓ Client Secret matches your configuration

2. **Check Required Parameters:**
   - Review OAuth provider documentation for required parameters
   - Common requirements:
     - `scope`: May be required even if empty
     - `audience`: May be required for API access
     - `resource`: May be required for resource-based access

3. **Test with curl:**
   
   **Note**: Set `CLIENT_ID` and `CLIENT_SECRET` environment variables before running these commands. Never commit credentials to version control.
   
   ```bash
   # Set your credentials (do not commit these values)
   export CLIENT_ID="your-client-id"
   export CLIENT_SECRET="your-client-secret"
   
   # Test Basic Auth with client credentials
   curl -X POST https://your-oauth-server.example.com/oauth/token \
     -H "Authorization: Basic $(echo -n '${CLIENT_ID}:${CLIENT_SECRET}' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials"
   
   # If scope is required:
   curl -X POST https://your-oauth-server.example.com/oauth/token \
     -H "Authorization: Basic $(echo -n '${CLIENT_ID}:${CLIENT_SECRET}' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials&scope=openl:read"
   ```

4. **Check MCP Server Logs:**
   - Look for detailed error messages
   - Verify Basic Auth header is being sent
   - Check request parameters being sent

#### Common Solutions

**Solution 1: Add Scope Parameter**
```yaml
environment:
  OPENL_OAUTH2_SCOPE: "openl:read openl:write"
```

**Solution 2: Add Audience Parameter**
```yaml
environment:
  OPENL_OAUTH2_AUDIENCE: "https://api.example.com"
```

**Solution 3: Verify Client Configuration**
- Ensure client is enabled
- Ensure `client_credentials` grant type is allowed
- Verify client ID and secret are correct

### Error: Token Endpoint 404

**Symptoms:**
```
Failed to obtain OAuth 2.1 token: Request failed with status code 404
```

**Solutions:**
1. Check the token endpoint URL is correct for your OAuth provider:
   - **Ping Identity**: Use `/as/token.oauth2` (e.g., `https://auth.example.com/as/token.oauth2`)
   - **Spring Security OAuth2**: Use `/oauth/token` (e.g., `https://auth.example.com/oauth/token`)
   - **Standard OAuth2**: Use `/token` (e.g., `https://auth.example.com/token`)
2. If using `OPENL_OAUTH2_ISSUER_URI`, override with explicit `OPENL_OAUTH2_TOKEN_URL`
3. Verify the issuer URI is correct (no trailing slash)
4. Check OAuth provider documentation for the correct token endpoint path

### Error: Token Endpoint 400

**Symptoms:**
```
Failed to obtain OAuth 2.1 token: Request failed with status code 400
```

**Solutions:**
1. **Try Basic Auth**: Some OAuth providers (like Ping Identity) require Basic Authentication header instead of credentials in the request body:
   ```bash
   OPENL_OAUTH2_USE_BASIC_AUTH=true
   ```

2. **Check request format**: Verify the Content-Type header is correct (`application/x-www-form-urlencoded`)

3. **Verify grant type**: Ensure `OPENL_OAUTH2_GRANT_TYPE` matches what your OAuth provider expects

4. **Check required parameters**: Some providers require additional parameters:
   - **Audience**: Some providers require an `audience` parameter:
     ```bash
     OPENL_OAUTH2_AUDIENCE=https://api.example.com
     ```
   - **Resource**: Some providers require a `resource` parameter:
     ```bash
     OPENL_OAUTH2_RESOURCE=https://api.example.com
     ```
   - **Scope**: Ensure required scopes are specified:
     ```bash
     OPENL_OAUTH2_SCOPE=openl:read openl:write
     ```

5. **Review error response**: Check logs for the actual error message from the OAuth provider

**Common Error Codes:**
- `unauthorized_client`: Client not authorized for this grant type
- `invalid_client`: Invalid client credentials
- `invalid_scope`: Invalid or missing scope
- `invalid_grant`: Invalid grant type or grant expired

For more OAuth 2.1 details, see [Authentication Guide](AUTHENTICATION.md).

---

## Connection Issues

### Issue: Docker containers don't start

**Solution:**
1. Check that Docker is running:
   ```bash
   docker ps
   ```
2. Check ports (8080, 5432 should not be occupied):
   ```bash
   lsof -i :8080
   lsof -i :5432
   ```
3. View logs:
   ```bash
   docker compose logs
   ```

### Issue: Port 3000 occupied (Docker)

**Solution:**
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

---

## Configuration Issues

### Issue: File doesn't exist

**Solution:**
1. Ensure project is built:
   ```bash
   cd mcp-server
   npm run build
   ls -la dist/index.js  # Should exist
   ```
2. Verify the path in configuration matches the actual location

### Issue: Environment variables not set

**Solution:**
1. Check configuration file for your AI client
2. Ensure all required variables are set:
   - `OPENL_BASE_URL` (required)
   - At least one authentication method (username/password, API key, or OAuth2)
3. Restart your AI client after changing configuration

---

## Additional Information

- All MCP server logs go to **stderr** (via `console.error()`) to avoid interfering with JSON-RPC protocol on stdout
- AI client logs are automatically saved to their respective log directories
- For debugging, it's recommended to run the server directly in terminal
- Use `DEBUG=1` environment variable for more verbose logs

For more help:
- [Quick Start Guide](../getting-started/QUICK-START.md)
- [Setup Guides](../setup/)
- [Authentication Guide](AUTHENTICATION.md)


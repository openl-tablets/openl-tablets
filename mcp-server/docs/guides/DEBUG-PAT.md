# Debugging Personal Access Token (PAT)

Guide for enabling detailed logging to debug PAT authentication.

## Enabling DEBUG Mode

### Option 1: Through Environment Variable in compose.yaml

Add to `mcp-server` section:

```yaml
environment:
  DEBUG_AUTH: "true"
  # or
  DEBUG: "true"
```

### Option 2: When Starting Container

```bash
docker compose run -e DEBUG_AUTH=true mcp-server
```

## What Gets Logged

### When Setting Authorization Header:

```
[Auth] ========================================
[Auth] 🔐 Personal Access Token Authentication
[Auth] ========================================
[Auth] PAT Configuration:
[Auth]   - PAT present: true
[Auth]   - PAT length: 64 characters
[Auth]   - PAT format valid: ✓
[Auth]   - PAT prefix: openl_pat_7gIBWMpyFih...
[Auth] Authorization Header:
[Auth]   - Header name: Authorization
[Auth]   - Header value format: Token <PAT>
[Auth]   - Header value (safe): Token [redacted]
[Auth]   - Header set in config: true
[Auth]   - Header verification: ✓ Correct format
[Auth] ========================================
```

### On Each HTTP Request:

```
[Auth] ========================================
[Auth] Request Interceptor:
[Auth]   Method: GET
[Auth]   URL: http://studio:8080/rest/repos
[Auth]   Headers present: true
[Auth]   Authorization header: Token [redacted]
[Auth]   Authorization header starts with: Token ✓
[Auth] ========================================
```

### On 401 Error:

```
[Auth] ========================================
[Auth] ❌ 401 Unauthorized Error:
[Auth]   Method: GET
[Auth]   URL: http://studio:8080/rest/repos
[Auth]   Auth method: Personal Access Token
[Auth]   Authorization header sent: Token [redacted]
[Auth]   Header format check: Token ✓
[Auth]   Response status: 401
[Auth]   Response data: {...}
[Auth] ========================================
```

## Header Verification

Header should be set in format:
```
Authorization: Token <your-pat-token>
```

**Important:** Uses `Token` prefix, not `Bearer`.

## Viewing Logs

```bash
# All logs
docker compose logs -f mcp-server

# Only authentication logs
docker compose logs -f mcp-server | grep "\[Auth\]"

# Logs with error context
docker compose logs -f mcp-server | grep -A 10 -B 5 "401\|ERROR\|Failed"
```

## Common Issues

### 1. PAT Not Set

```
[Auth] ⚠️  No authentication method configured!
[Auth]   personalAccessToken: not configured
```

**Solution:** Check that PAT is provided via MCP client configuration (query parameters or headers), not Docker environment variables.

### 2. Incorrect Header Format

If header doesn't start with `Token `, check:
- Correctness of PAT in configuration
- That correct authentication method is used

### 3. 401 Unauthorized

If you get 401, check:
- PAT hasn't expired (if expiration was set)
- PAT wasn't deleted in UI
- OpenL Tablets is configured for OAuth2/SAML mode
- Token format is correct

## Manual Header Testing

```bash
# Test via curl
curl -H "Authorization: Token <your-pat-token>" \
  http://localhost:8080/rest/repos
```


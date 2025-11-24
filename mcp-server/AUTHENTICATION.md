# Authentication Guide

This guide covers all authentication methods supported by the OpenL Tablets MCP Server, including OAuth 2.1, API Key, and Basic Authentication.

## Table of Contents
- [Authentication Methods](#authentication-methods)
- [Basic Authentication](#basic-authentication)
- [API Key Authentication](#api-key-authentication)
- [OAuth 2.1 Authentication](#oauth-21-authentication)
- [Client Document ID](#client-document-id)
- [Security Best Practices](#security-best-practices)
- [Troubleshooting](#troubleshooting)

## Authentication Methods

The MCP server supports three authentication methods:

1. **Basic Authentication** - Username and password
2. **API Key Authentication** - Pre-shared API key
3. **OAuth 2.1** - Modern OAuth 2.1 with token refresh

Choose the method that best fits your security requirements and infrastructure.

## Basic Authentication

Basic Authentication uses HTTP Basic Auth with username and password.

### Configuration

**Environment Variables:**
```bash
OPENL_BASE_URL=http://localhost:8080/webstudio/rest
OPENL_USERNAME=admin
OPENL_PASSWORD=admin
```

**Claude Desktop Config:**
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "http://localhost:8080/webstudio/rest",
        "OPENL_USERNAME": "admin",
        "OPENL_PASSWORD": "admin"
      }
    }
  }
}
```

### Use Cases
- Development and testing environments
- Internal networks with network-level security
- Quick setup and prototyping

### Security Considerations
- ❌ Credentials sent with every request
- ❌ Password stored in configuration
- ✅ Use HTTPS in production
- ✅ Rotate passwords regularly

## API Key Authentication

API Key Authentication uses a pre-shared key sent in the `X-API-Key` header.

### Configuration

**Environment Variables:**
```bash
OPENL_BASE_URL=https://openl.example.com/webstudio/rest
OPENL_API_KEY=your-api-key-here
```

**Claude Desktop Config:**
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "https://openl.example.com/webstudio/rest",
        "OPENL_API_KEY": "your-api-key-here"
      }
    }
  }
}
```

### Use Cases
- Service-to-service authentication
- API access for trusted applications
- Simpler than OAuth for single-tenant scenarios

### Security Considerations
- ✅ More secure than basic auth
- ✅ Can be rotated without password changes
- ✅ Single key per service/application
- ⚠️ Key must be kept secret
- ✅ Use HTTPS always

## OAuth 2.1 Authentication

OAuth 2.1 is the most secure and modern authentication method, supporting automatic token refresh and fine-grained scopes.

### Features

- ✅ **Automatic Token Management** - Tokens obtained and refreshed automatically
- ✅ **Token Caching** - Tokens cached until near expiration (60s buffer)
- ✅ **Automatic Refresh** - 401 responses trigger automatic token refresh
- ✅ **Scope Support** - Fine-grained access control
- ✅ **Multiple Grant Types** - Client credentials, authorization code, refresh token
- ✅ **PKCE Support** - Proof Key for Code Exchange for public clients (no client_secret)
- ✅ **Standards Compliant** - OAuth 2.1 specification

### Grant Types

#### Client Credentials Grant

Used for service-to-service authentication without user interaction.

**Flow:**
1. Application sends client ID and secret to token endpoint
2. Authorization server validates credentials
3. Server returns access token
4. Application uses token for API requests
5. Token automatically refreshed before expiration

**Configuration:**
```bash
# Required
OPENL_BASE_URL=https://openl.example.com/webstudio/rest
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_CLIENT_SECRET=your-client-secret

# Token URL - either specify directly or use issuer-uri
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
# OR use issuer-uri (token-url will be automatically set to {issuer-uri}/token)
OPENL_OAUTH2_ISSUER_URI=https://auth.example.com

# Optional
OPENL_OAUTH2_SCOPE=openl:read openl:write
OPENL_OAUTH2_GRANT_TYPE=client_credentials
```

**Claude Desktop Config (with token-url):**
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "https://openl.example.com/webstudio/rest",
        "OPENL_OAUTH2_CLIENT_ID": "mcp-server-client",
        "OPENL_OAUTH2_CLIENT_SECRET": "client-secret-here",
        "OPENL_OAUTH2_TOKEN_URL": "https://auth.example.com/oauth/token",
        "OPENL_OAUTH2_SCOPE": "openl:read openl:write",
        "OPENL_OAUTH2_GRANT_TYPE": "client_credentials"
      }
    }
  }
}
```

**Claude Desktop Config (with issuer-uri):**
```json
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "https://openl.example.com/webstudio/rest",
        "OPENL_OAUTH2_CLIENT_ID": "OpenL_Studio",
        "OPENL_OAUTH2_CLIENT_SECRET": "your-client-secret",
        "OPENL_OAUTH2_ISSUER_URI": "https://auth.example.com",
        "OPENL_OAUTH2_SCOPE": "openl:read openl:write",
        "OPENL_OAUTH2_GRANT_TYPE": "client_credentials"
      }
    }
  }
}
```

**Note:** When using `OPENL_OAUTH2_ISSUER_URI`, the token URL is automatically set to `{issuer-uri}/token`. This is convenient when working with OAuth providers that follow standard conventions (like Spring Security OAuth2).

**Important:** Some OAuth providers use non-standard token endpoint paths:
- **Ping Identity**: `/as/token.oauth2` (e.g., `https://auth.example.com/as/token.oauth2`) - also requires Basic Auth
- **Spring Security OAuth2**: `/oauth/token` (e.g., `https://auth.example.com/oauth/token`)
- **Standard OAuth2**: `/token` (e.g., `https://auth.example.com/token`)

If you get a 404 error when using `OPENL_OAUTH2_ISSUER_URI`, specify `OPENL_OAUTH2_TOKEN_URL` explicitly with the correct endpoint path.

**Ping Identity Configuration:**
Ping Identity typically requires Basic Authentication header instead of sending credentials in the request body:

```bash
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_CLIENT_SECRET=your-client-secret
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/as/token.oauth2
OPENL_OAUTH2_GRANT_TYPE=client_credentials
OPENL_OAUTH2_USE_BASIC_AUTH=true
```

When `OPENL_OAUTH2_USE_BASIC_AUTH=true`, the client credentials are sent as a Basic Auth header (`Authorization: Basic base64(client_id:client_secret)`) instead of in the request body.

#### Authorization Code Grant with PKCE

Used for public clients (without client_secret) or when you need user authorization. PKCE (Proof Key for Code Exchange) provides additional security for authorization code flow.

**Flow:**
1. Generate `code_verifier` (random 43-128 character URL-safe string)
2. Generate `code_challenge` (SHA256 hash of code_verifier, base64url encoded)
3. Redirect user to authorization endpoint with `code_challenge`
4. User authorizes and receives `authorization_code`
5. Exchange authorization_code + code_verifier for access token
6. Use access token for API requests
7. Token automatically refreshed using refresh_token

**Configuration:**
```bash
# Required
OPENL_BASE_URL=https://openl.example.com/webstudio/rest
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_AUTHORIZATION_URL=https://auth.example.com/oauth/authorize
OPENL_OAUTH2_AUTHORIZATION_CODE=authorization-code-from-provider
OPENL_OAUTH2_CODE_VERIFIER=code-verifier-used-in-authorization-request
OPENL_OAUTH2_REDIRECT_URI=https://your-app.com/callback

# Optional - client_secret not required for PKCE (public client)
# OPENL_OAUTH2_CLIENT_SECRET=your-client-secret  # Optional for PKCE

# Optional
OPENL_OAUTH2_SCOPE=openl:read openl:write
OPENL_OAUTH2_GRANT_TYPE=authorization_code
```

**Generating Code Verifier:**

You can generate a code_verifier using the MCP server's utility functions or manually:

```bash
# Using Node.js
node -e "const crypto = require('crypto'); const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~'; const random = crypto.randomBytes(128); let result = ''; for (let i = 0; i < 128; i++) { result += charset[random[i] % charset.length]; } console.log(result);"
```

**Docker Configuration:**
```yaml
environment:
  OPENL_BASE_URL: https://openl.example.com/webstudio/rest
  OPENL_OAUTH2_CLIENT_ID: your-public-client-id
  OPENL_OAUTH2_TOKEN_URL: https://auth.example.com/oauth/token
  OPENL_OAUTH2_AUTHORIZATION_URL: https://auth.example.com/oauth/authorize
  OPENL_OAUTH2_AUTHORIZATION_CODE: ${AUTHORIZATION_CODE}
  OPENL_OAUTH2_CODE_VERIFIER: ${CODE_VERIFIER}
  OPENL_OAUTH2_REDIRECT_URI: https://your-app.com/callback
  OPENL_OAUTH2_GRANT_TYPE: authorization_code
  OPENL_OAUTH2_SCOPE: "openl:read openl:write"
```

**Note:** The MCP server automatically generates `code_challenge` from `code_verifier` using SHA256 (S256 method). You don't need to provide `code_challenge` manually.

**Automatic Browser Flow (Recommended):**

The MCP server can automatically open a browser and intercept the authorization code when using a **localhost redirect URI**. This eliminates the need to manually copy authorization codes.

**Configuration for Automatic Browser Flow:**
```bash
# Required
OPENL_BASE_URL=https://openl.example.com/webstudio/rest
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_AUTHORIZATION_URL=https://auth.example.com/oauth/authorize
OPENL_OAUTH2_REDIRECT_URI=http://localhost:8080/oauth2/callback  # Must be localhost!
OPENL_OAUTH2_GRANT_TYPE=authorization_code

# Optional
OPENL_OAUTH2_SCOPE=openl:read openl:write
# OPENL_OAUTH2_CODE_VERIFIER is optional - will be auto-generated if not provided
```

**How It Works:**
1. On first API request, the MCP server detects missing authorization code
2. If redirect URI is `localhost`, starts a local HTTP server to intercept the callback
3. Automatically opens your default browser with the authorization URL
4. You complete the OAuth authorization in the browser
5. The authorization code is automatically intercepted from the redirect
6. The code is exchanged for an access token automatically
7. The local HTTP server shuts down after receiving the code

**Important Requirements:**
- ✅ Redirect URI **must** be `localhost` (e.g., `http://localhost:8080/oauth2/callback`)
- ✅ The redirect URI port must be available (not in use by another service)
- ✅ Browser must be accessible (works on host machine, not in headless Docker containers)

**Example with Automatic Flow:**
```bash
# Claude Desktop Config
{
  "mcpServers": {
    "openl-tablets": {
      "command": "node",
      "args": ["/path/to/dist/index.js"],
      "env": {
        "OPENL_BASE_URL": "https://openl.example.com/webstudio/rest",
        "OPENL_OAUTH2_CLIENT_ID": "your-client-id",
        "OPENL_OAUTH2_TOKEN_URL": "https://auth.example.com/oauth/token",
        "OPENL_OAUTH2_AUTHORIZATION_URL": "https://auth.example.com/oauth/authorize",
        "OPENL_OAUTH2_REDIRECT_URI": "http://localhost:8080/oauth2/callback",
        "OPENL_OAUTH2_GRANT_TYPE": "authorization_code",
        "OPENL_OAUTH2_SCOPE": "openl:read openl:write"
      }
    }
  }
}
```

**Manual Flow (Non-Localhost Redirect URI):**

If your redirect URI is not localhost (e.g., `https://your-app.com/callback`), you need to manually obtain the authorization code:

1. Open the authorization URL in your browser (the server will print it)
2. Complete the authorization
3. Copy the `code` parameter from the redirect URL
4. Set `OPENL_OAUTH2_AUTHORIZATION_CODE` environment variable

**Benefits of PKCE:**
- ✅ **No Client Secret Required** - Suitable for public clients (SPA, mobile apps)
- ✅ **Enhanced Security** - Prevents authorization code interception attacks
- ✅ **OAuth 2.1 Compliant** - Recommended for all authorization code flows
- ✅ **Automatic Browser Flow** - No manual code copying needed (with localhost redirect)
- ✅ **Works in Docker** - Perfect for containerized MCP servers (manual flow for non-localhost redirects)

#### Refresh Token Grant

Used when you have a long-lived refresh token from a previous authorization.

**Configuration:**
```bash
OPENL_BASE_URL=https://openl.example.com/webstudio/rest
OPENL_OAUTH2_CLIENT_ID=your-client-id
OPENL_OAUTH2_CLIENT_SECRET=your-client-secret
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
OPENL_OAUTH2_REFRESH_TOKEN=your-refresh-token
OPENL_OAUTH2_GRANT_TYPE=refresh_token
```

### Token Lifecycle

```
┌─────────────────────────────────────────────────────────┐
│ Initial Request                                         │
└──────────────┬──────────────────────────────────────────┘
               │
               ▼
         ┌───────────┐
         │ Has Token?│
         └─────┬─────┘
               │ No
               ▼
    ┌─────────────────┐
    │ Obtain Token    │──────┐
    │ (POST /token)   │      │
    └────────┬────────┘      │
             │ Success       │ Failure
             ▼               ▼
      ┌──────────┐    ┌───────────┐
      │ Use Token│    │ Throw Error│
      └─────┬────┘    └───────────┘
            │
            ▼
    ┌───────────────┐
    │ Token Valid?  │
    └───┬───────┬───┘
        │ Yes   │ No (< 60s to expiry)
        │       ▼
        │  ┌────────────┐
        │  │ Refresh    │
        │  │ Token      │
        │  └──────┬─────┘
        │         │
        ▼         ▼
    ┌──────────────┐
    │ API Request  │
    │ with Bearer  │
    └──────┬───────┘
           │
           ▼
    ┌─────────────┐
    │ 401 Error?  │
    └──┬────────┬─┘
       │ Yes    │ No
       │        ▼
       │   ┌─────────┐
       │   │ Success │
       │   └─────────┘
       ▼
  ┌────────────┐
  │ Retry Once │
  │ with Fresh │
  │ Token      │
  └────────────┘
```

### Token Expiration

- **Default Expiration**: Typically 3600 seconds (1 hour)
- **Refresh Buffer**: 60 seconds before expiration
- **Auto-Refresh**: Triggered when token is within 60s of expiry
- **401 Handling**: Automatic token refresh and request retry

### Scopes

Scopes define the permissions for the access token.

Common OpenL Tablets scopes:
- `openl:read` - Read access to projects, tables, and deployments
- `openl:write` - Write access to modify projects and tables
- `openl:deploy` - Permission to deploy to production
- `openl:admin` - Administrative access

**Example:**
```bash
OPENL_OAUTH2_SCOPE="openl:read openl:write openl:deploy"
```

### Token Storage

- Tokens are stored in memory only
- Not persisted to disk
- Cleared when server restarts
- New token obtained automatically on restart

## Client Document ID

The Client Document ID is a tracking identifier included in all API requests.

### Purpose

- **Request Correlation**: Track requests across systems
- **Debugging**: Identify requests from specific MCP instances
- **Auditing**: Trace actions back to source
- **Load Balancing**: Identify client instances

### Configuration

**Environment Variable:**
```bash
OPENL_CLIENT_DOCUMENT_ID=mcp-server-instance-1
```

**Claude Desktop Config:**
```json
{
  "env": {
    "OPENL_CLIENT_DOCUMENT_ID": "claude-desktop-user123"
  }
}
```

### Best Practices

- Use a unique ID per MCP server instance
- Include environment indicator: `mcp-prod-1`, `mcp-dev-1`
- Keep IDs short and descriptive
- Don't include sensitive information

### Header Format

The client document ID is sent as:
```
X-Client-Document-ID: mcp-server-instance-1
```

## Security Best Practices

### General

1. **Always Use HTTPS** - Never send credentials over HTTP
2. **Rotate Credentials** - Regularly rotate passwords, API keys, and client secrets
3. **Least Privilege** - Use minimum required scopes/permissions
4. **Separate Environments** - Different credentials for dev/staging/production
5. **Monitor Access** - Log and monitor authentication attempts

### OAuth 2.1 Specific

1. **Client Secret Protection**
   - Never commit client secrets to version control
   - Use environment variables or secret managers
   - Rotate secrets periodically

2. **Token Management**
   - Tokens cached in memory only
   - Not logged or persisted
   - Automatically refreshed

3. **Scope Management**
   - Request minimum required scopes
   - Review scope permissions regularly
   - Use different clients for different use cases

4. **PKCE for Public Clients**
   - Use PKCE for authorization_code flow (recommended for OAuth 2.1)
   - Generate code_verifier securely (128 characters recommended)
   - Never reuse code_verifier across requests
   - Store code_verifier securely until token exchange completes

### Configuration Management

1. **Environment Variables**
   ```bash
   # Good: Use env files
   cp .env.example .env
   # Edit .env with actual values
   ```

2. **Secret Managers**
   ```bash
   # Good: Fetch from secret manager
   export OPENL_OAUTH2_CLIENT_SECRET=$(vault read secret/openl/client-secret)
   ```

3. **Version Control**
   ```gitignore
   # .gitignore
   .env
   .env.local
   .env.*.local
   ```

## Troubleshooting

### OAuth 2.1 Issues

#### Token Acquisition Fails

**Symptoms:**
```
Error: Failed to obtain OAuth 2.1 token: invalid_client
```

**Solutions:**
1. Verify client ID and secret are correct
2. Check token URL is accessible
3. Ensure client is registered with OAuth provider
4. Verify client credentials are not expired

#### 401 Unauthorized

**Symptoms:**
```
OpenL Tablets API error (401): Unauthorized
```

**Solutions:**
1. Check token is being obtained successfully
2. Verify scopes include required permissions
3. Ensure token hasn't expired
4. Check OAuth provider is operational

#### Token Refresh Fails

**Symptoms:**
```
Error: Failed to refresh token: invalid_grant
```

**Solutions:**
1. Verify refresh token is valid
2. Check refresh token hasn't expired
3. Try client credentials grant instead
4. Obtain new refresh token from OAuth provider

### Basic Auth Issues

#### Authentication Failed

**Symptoms:**
```
OpenL Tablets API error (401): Authentication required
```

**Solutions:**
1. Verify username and password are correct
2. Check user account is not locked
3. Ensure user has required permissions
4. Verify OpenL Tablets is configured for basic auth

### API Key Issues

#### Invalid API Key

**Symptoms:**
```
OpenL Tablets API error (403): Invalid API key
```

**Solutions:**
1. Verify API key is correct
2. Check API key hasn't been revoked
3. Ensure API key has required permissions
4. Verify API key authentication is enabled in OpenL Tablets

### General Issues

#### Connection Timeout

**Symptoms:**
```
Error: timeout of 30000ms exceeded
```

**Solutions:**
1. Increase timeout: `OPENL_TIMEOUT=60000`
2. Check network connectivity
3. Verify OpenL Tablets is running
4. Check firewall rules

#### OAuth2 Token Endpoint 404 Error

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
5. Enable debug logging to see the exact URL being used

**Example for Ping Identity:**
```bash
# Instead of issuer-uri (which defaults to /token)
OPENL_OAUTH2_ISSUER_URI=https://testping-sso.eisgroup.com

# Use explicit token-url
OPENL_OAUTH2_TOKEN_URL=https://testping-sso.eisgroup.com/as/token.oauth2
```

#### OAuth2 Token Endpoint 400 Error

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
   - **Audience**: Some providers (like Auth0, Ping Identity) require an `audience` parameter:
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
- `unauthorized_client`: 
  - Client not authorized for this grant type (check OAuth provider configuration)
  - Missing required parameters (e.g., `audience`, `scope`)
  - Client not configured to use `client_credentials` grant type
  - **Solution**: Verify client configuration in OAuth provider admin console
- `invalid_client`: Invalid client credentials (check client_id and client_secret)
- `invalid_scope`: Invalid or missing scope (verify required scopes in OAuth provider)
- `invalid_grant`: Invalid grant type or grant expired (check grant type configuration)

**Example for Ping Identity with Basic Auth:**
```bash
OPENL_OAUTH2_CLIENT_ID=OpenL_Studio
OPENL_OAUTH2_CLIENT_SECRET=your-secret
OPENL_OAUTH2_TOKEN_URL=https://testping-sso.eisgroup.com/as/token.oauth2
OPENL_OAUTH2_GRANT_TYPE=client_credentials
OPENL_OAUTH2_USE_BASIC_AUTH=true
# If required by your Ping Identity configuration:
OPENL_OAUTH2_AUDIENCE=https://api.example.com
OPENL_OAUTH2_SCOPE=openl:read openl:write
```

#### SSL/TLS Errors

**Symptoms:**
```
Error: unable to verify the first certificate
```

**Solutions:**
1. Ensure valid SSL certificate
2. Update CA certificates
3. Check certificate chain
4. For development only: Configure to accept self-signed certs

## Environment Variable Reference

### Common
```bash
OPENL_BASE_URL          # OpenL Tablets API base URL
OPENL_CLIENT_DOCUMENT_ID # Client tracking identifier
OPENL_TIMEOUT           # Request timeout in milliseconds
```

### Basic Auth
```bash
OPENL_USERNAME          # Username for basic authentication
OPENL_PASSWORD          # Password for basic authentication
```

### API Key
```bash
OPENL_API_KEY           # API key for authentication
```

### OAuth 2.1
```bash
OPENL_OAUTH2_CLIENT_ID           # OAuth client ID
OPENL_OAUTH2_CLIENT_SECRET       # OAuth client secret
OPENL_OAUTH2_TOKEN_URL           # Token endpoint URL (required if issuer-uri not provided)
OPENL_OAUTH2_ISSUER_URI         # OAuth issuer URI (alternative to token-url, auto-appends /token)
OPENL_OAUTH2_AUTHORIZATION_URL   # Authorization endpoint (optional)
OPENL_OAUTH2_SCOPE               # Space-separated scopes
OPENL_OAUTH2_GRANT_TYPE          # Grant type (client_credentials, authorization_code, refresh_token)
OPENL_OAUTH2_REFRESH_TOKEN       # Refresh token (if using refresh grant)
OPENL_OAUTH2_USE_BASIC_AUTH      # Use Basic Auth header instead of form data (true/false, default: false)
OPENL_OAUTH2_AUDIENCE            # OAuth2 audience parameter (required by some providers)
OPENL_OAUTH2_RESOURCE            # OAuth2 resource parameter (required by some providers)
# PKCE parameters (for authorization_code grant)
OPENL_OAUTH2_CODE_VERIFIER       # PKCE code verifier (43-128 chars, URL-safe, required for PKCE)
OPENL_OAUTH2_CODE_CHALLENGE      # PKCE code challenge (auto-generated from code_verifier if not provided)
OPENL_OAUTH2_CODE_CHALLENGE_METHOD # PKCE method (S256 or plain, default: S256)
OPENL_OAUTH2_AUTHORIZATION_CODE  # Authorization code from authorization endpoint
OPENL_OAUTH2_REDIRECT_URI        # Redirect URI registered with OAuth provider
```

## Examples

### Local Development with Basic Auth
```bash
export OPENL_BASE_URL=http://localhost:8080/webstudio/rest
export OPENL_USERNAME=admin
export OPENL_PASSWORD=admin
export OPENL_CLIENT_DOCUMENT_ID=dev-laptop
npm start
```

### Production with OAuth 2.1
```bash
export OPENL_BASE_URL=https://openl-prod.example.com/webstudio/rest
export OPENL_OAUTH2_CLIENT_ID=mcp-server-prod
export OPENL_OAUTH2_CLIENT_SECRET=$(vault read -field=secret secret/openl/prod/client-secret)
export OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token
export OPENL_OAUTH2_SCOPE="openl:read openl:write"
export OPENL_CLIENT_DOCUMENT_ID=mcp-prod-instance-1
export OPENL_TIMEOUT=60000
npm start
```

### Docker with API Key
```bash
docker run -d \
  -e OPENL_BASE_URL=https://openl.example.com/webstudio/rest \
  -e OPENL_API_KEY=your-api-key \
  -e OPENL_CLIENT_DOCUMENT_ID=docker-container-1 \
  openl-mcp-server:latest
```

## Resources

- [OAuth 2.1 Specification](https://oauth.net/2.1/)
- [OpenL Tablets Documentation](https://openl-tablets.org/)
- [MCP Server README](./README.md)
- [Testing Guide](./TESTING.md)

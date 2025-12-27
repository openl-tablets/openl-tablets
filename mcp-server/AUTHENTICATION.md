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
- ✅ **Multiple Grant Types** - Client credentials, refresh token
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
OPENL_OAUTH2_TOKEN_URL=https://auth.example.com/oauth/token

# Optional
OPENL_OAUTH2_SCOPE=openl:read openl:write
OPENL_OAUTH2_GRANT_TYPE=client_credentials
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
OPENL_OAUTH2_TOKEN_URL           # Token endpoint URL
OPENL_OAUTH2_AUTHORIZATION_URL   # Authorization endpoint (optional)
OPENL_OAUTH2_SCOPE               # Space-separated scopes
OPENL_OAUTH2_GRANT_TYPE          # Grant type (client_credentials, refresh_token)
OPENL_OAUTH2_REFRESH_TOKEN       # Refresh token (if using refresh grant)
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

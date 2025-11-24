# OAuth2 Troubleshooting Guide

## Error: `unauthorized_client`

### Symptoms
```
Failed to obtain OAuth 2.1 token: Request failed with status code 400
Response: {"error":"unauthorized_client"}
```

### Root Causes

The `unauthorized_client` error typically indicates one of the following issues:

1. **Client not configured for client_credentials grant type**
   - The OAuth client in Ping Identity is not configured to allow `client_credentials` grant type
   - **Solution**: Check Ping Identity admin console → Applications → Your Client → Grant Types → Enable "Client Credentials"

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
   - **Solution**: Verify credentials in Ping Identity admin console

4. **Client not enabled**
   - The OAuth client is disabled in Ping Identity
   - **Solution**: Enable the client in Ping Identity admin console

### Diagnostic Steps

1. **Verify Client Configuration in Ping Identity:**
   ```
   - Login to Ping Identity admin console
   - Navigate to: Applications → Your Client (OpenL_Studio)
   - Check:
     ✓ Client is enabled
     ✓ Grant Types includes "Client Credentials"
     ✓ Client ID matches: OpenL_Studio
     ✓ Client Secret matches your configuration
   ```

2. **Check Required Parameters:**
   - Review Ping Identity documentation for required parameters
   - Common requirements:
     - `scope`: May be required even if empty
     - `audience`: May be required for API access
     - `resource`: May be required for resource-based access

3. **Test with curl:**
   ```bash
   # Test Basic Auth with client credentials
   curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
     -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials"
   
   # If scope is required:
   curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
     -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials&scope=openl:read"
   ```

4. **Check MCP Server Logs:**
   - Look for detailed error messages
   - Verify Basic Auth header is being sent
   - Check request parameters being sent

### Common Solutions

#### Solution 1: Add Scope Parameter
```yaml
environment:
  OPENL_OAUTH2_SCOPE: "openl:read openl:write"
```

#### Solution 2: Add Audience Parameter
```yaml
environment:
  OPENL_OAUTH2_AUDIENCE: "https://api.example.com"
```

#### Solution 3: Verify Client Configuration
- Ensure client is enabled in Ping Identity
- Ensure `client_credentials` grant type is allowed
- Verify client ID and secret are correct

### Ping Identity Specific Notes

Ping Identity typically requires:
- Basic Authentication header (already configured with `OPENL_OAUTH2_USE_BASIC_AUTH=true`)
- Token endpoint: `/as/token.oauth2` (already configured)
- May require `scope` parameter even if empty: `scope=`
- May require `audience` parameter for API access

### Next Steps

1. **Check Ping Identity admin console for client configuration:**
   - Login to Ping Identity admin console (https://testping-sso.eisgroup.com)
   - Navigate to: Applications → OpenL_Studio
   - Verify:
     - ✓ Client Status: **Enabled**
     - ✓ Grant Types: **Client Credentials** is checked/enabled
     - ✓ Client ID matches: `OpenL_Studio`
     - ✓ Client Secret matches: `Exigen/2024.02` (or verify it's correct)

2. **If client_credentials is not enabled:**
   - In Ping Identity admin console, edit the client
   - Go to "Grant Types" or "OAuth Settings"
   - Enable "Client Credentials" grant type
   - Save the changes

3. **Try adding scope parameter** (some Ping Identity configurations require it):
   ```bash
   # Test with empty scope
   curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
     -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials&scope="
   ```

4. **Check if client has required scopes configured:**
   - In Ping Identity, check if client has scopes assigned
   - If scopes are required, add them to the request

5. **Contact Ping Identity administrator** if:
   - You don't have access to admin console
   - Client configuration cannot be changed
   - You need help enabling client_credentials grant type

### Verification

After fixing the client configuration in Ping Identity, test again:

```bash
curl -X POST https://testping-sso.eisgroup.com/as/token.oauth2 \
  -H "Authorization: Basic $(echo -n 'OpenL_Studio:Exigen/2024.02' | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials"
```

Expected successful response:
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "expires_in": 3600
}
```


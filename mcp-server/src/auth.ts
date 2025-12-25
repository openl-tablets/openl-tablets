/**
 * Authentication module for OpenL Tablets MCP Server
 *
 * Supports multiple authentication methods:
 * - Basic Authentication (username/password)
 * - API Key authentication
 * - OAuth 2.1 (with automatic token management)
 */

import axios, { AxiosInstance, InternalAxiosRequestConfig } from "axios";
import * as Types from "./types.js";
import { DEFAULTS, HEADERS } from "./constants.js";
import { sanitizeError, openBrowser, generateAuthorizationUrl, generateCodeVerifier, generateCodeChallengeSync, performOAuthFlowWithBrowser, isLocalhostRedirect } from "./utils.js";
import { createHash } from "node:crypto";

/**
 * Check if debug logging is enabled (via environment variable)
 */
const DEBUG_AUTH = process.env.DEBUG_AUTH === "true" || process.env.DEBUG === "true";

/**
 * Safely log auth header information without exposing tokens
 * @param authHeader - Full auth header value
 * @returns Safe representation of auth header (scheme only or redacted)
 */
function safeAuthHeaderLog(authHeader: string | undefined): string {
  if (!authHeader || authHeader === "none") {
    return "none";
  }

  // Extract scheme (Bearer, Basic, etc.)
  const schemeMatch = authHeader.match(/^(\w+)\s+/);
  if (schemeMatch) {
    const scheme = schemeMatch[1];
    if (DEBUG_AUTH) {
      // In debug mode, log a hash fingerprint instead of actual token
      const hash = createHash("sha256").update(authHeader).digest("hex").substring(0, 8);
      return `${scheme} [${hash}...]`;
    }
    return `${scheme} [redacted]`;
  }

  // If no scheme found, it might be API key - just show it's present
  return DEBUG_AUTH ? `[${createHash("sha256").update(authHeader).digest("hex").substring(0, 8)}...]` : "[redacted]";
}

/**
 * Authentication manager for OpenL Tablets API
 *
 * Handles:
 * - Token lifecycle management
 * - Automatic token refresh
 * - Request/response interceptors
 * - Multiple authentication methods
 */
export class AuthenticationManager {
  private config: Types.OpenLConfig;
  private oauth2Token: Types.OAuth2Token | null = null;
  private tokenRefreshPromise: Promise<Types.OAuth2Token | null> | undefined;

  constructor(config: Types.OpenLConfig) {
    this.config = config;
  }

  /**
   * Configure authentication interceptors for an Axios instance
   *
   * @param axiosInstance - The Axios instance to configure
   */
  public setupInterceptors(axiosInstance: AxiosInstance): void {
    // Request interceptor: Add authentication headers
    axiosInstance.interceptors.request.use(
      async (config) => {
        const authConfig = await this.addAuthHeaders(config);
        // Log request details for debugging (safe - no token exposure)
        const fullUrl = `${authConfig.baseURL || ''}${authConfig.url || ''}`;
        const authHeader = (authConfig.headers && authConfig.headers[HEADERS.AUTHORIZATION]) || 
                          (authConfig.headers && authConfig.headers[HEADERS.API_KEY]) || 
                          undefined;
        console.error(`[Auth] Request: ${authConfig.method ? authConfig.method.toUpperCase() : ''} ${fullUrl}`);
        console.error(`[Auth] Auth header: ${safeAuthHeaderLog(authHeader)}`);
        return authConfig;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor: Handle 401 errors with token refresh
    axiosInstance.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // Log 401 errors with details
        if (error.response && error.response.status === 401) {
          const fullUrl = `${(error.config && error.config.baseURL) || ''}${(error.config && error.config.url) || ''}`;
          const authMethod = this.getAuthMethod();
          console.error(`[Auth] ❌ 401 Unauthorized: ${error.config && error.config.method ? error.config.method.toUpperCase() : ''} ${fullUrl}`);
          console.error(`[Auth]   Auth method: ${authMethod}`);
          console.error(`[Auth]   Response: ${JSON.stringify((error.response && error.response.data) || {}).substring(0, 200)}`);
        }

        // If 401 and we have OAuth2, try refreshing token
        if (
          error.response && error.response.status === 401 &&
          this.config.oauth2 &&
          !originalRequest._retry
        ) {
          originalRequest._retry = true;

          try {
            // Force token refresh
            this.oauth2Token = null;
            await this.getValidToken();

            // Retry original request with new token
            return axiosInstance(originalRequest);
          } catch (refreshError) {
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Add authentication headers to a request
   *
   * @param config - Axios request configuration
   * @returns Modified request configuration with auth headers
   */
  private async addAuthHeaders(
    config: InternalAxiosRequestConfig
  ): Promise<InternalAxiosRequestConfig> {
    if (!config.headers) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      config.headers = {} as any;
    }

    // Add Client Document ID if configured
    if (this.config.clientDocumentId) {
      config.headers[HEADERS.CLIENT_DOCUMENT_ID] = this.config.clientDocumentId;
    }

    // Add authentication based on method priority:
    // 1. OAuth 2.1
    // 2. API Key
    // 3. Basic Auth
    if (this.config.oauth2) {
      console.error(`[Auth] Using OAuth 2.1 authentication`);
      const token = await this.getValidToken();
      if (token) {
        config.headers[HEADERS.AUTHORIZATION] = `Bearer ${token.access_token}`;
      }
    } else if (this.config.apiKey) {
      console.error(`[Auth] Using API Key authentication`);
      config.headers[HEADERS.API_KEY] = this.config.apiKey;
    } else if (this.config.username && this.config.password) {
      console.error(`[Auth] Using Basic Auth: username=${this.config.username}, password=${this.config.password ? '***' : 'missing'}`);
      const auth = Buffer.from(
        `${this.config.username}:${this.config.password}`
      ).toString("base64");
      config.headers[HEADERS.AUTHORIZATION] = `Basic ${auth}`;
      // Safe logging - never expose auth tokens
      console.error(`[Auth] Basic Auth header set: ${safeAuthHeaderLog(`Basic ${auth}`)}`);
    } else {
      console.error(`[Auth] ⚠️  No authentication method configured!`);
      console.error(`[Auth]   oauth2: ${!!this.config.oauth2}`);
      console.error(`[Auth]   apiKey: ${!!this.config.apiKey}`);
      console.error(`[Auth]   username: ${this.config.username || 'not set'}`);
      console.error(`[Auth]   password: ${this.config.password ? 'set' : 'not set'}`);
    }

    return config;
  }

  /**
   * Get a valid OAuth 2.1 token, refreshing if necessary
   *
   * @returns Valid OAuth token or null if not using OAuth
   */
  private async getValidToken(): Promise<Types.OAuth2Token | null> {
    if (!this.config.oauth2) {
      return null;
    }

    // Return cached token if still valid
    if (this.oauth2Token && this.isTokenValid(this.oauth2Token)) {
      return this.oauth2Token;
    }

    // If refresh is already in progress, wait for it
    if (this.tokenRefreshPromise) {
      return this.tokenRefreshPromise;
    }

    // Start new token refresh
    this.tokenRefreshPromise = this.obtainOAuth2Token();
    try {
      this.oauth2Token = await this.tokenRefreshPromise;
      return this.oauth2Token;
    } finally {
      this.tokenRefreshPromise = undefined;
    }
  }

  /**
   * Check if an OAuth token is still valid
   *
   * @param token - OAuth token to validate
   * @returns true if token is valid, false otherwise
   */
  private isTokenValid(token: Types.OAuth2Token): boolean {
    if (!token.expires_at) {
      return true; // If no expiration, assume valid
    }

    const now = Math.floor(Date.now() / 1000);
    const bufferSeconds = DEFAULTS.TOKEN_EXPIRATION_BUFFER;

    // Token is valid if it hasn't expired and has buffer time remaining
    return token.expires_at > now + bufferSeconds;
  }

  /**
   * Obtain a new OAuth 2.1 access token
   *
   * @returns New OAuth token
   * @throws Error if token acquisition fails
   */
  private async obtainOAuth2Token(): Promise<Types.OAuth2Token> {
    if (!this.config.oauth2) {
      throw new Error("OAuth 2.1 configuration not provided");
    }

    const oauth2Config = this.config.oauth2;
    const grantType = oauth2Config.grantType || DEFAULTS.OAUTH2_GRANT_TYPE;
    const useBasicAuth = (oauth2Config.useBasicAuth !== undefined && oauth2Config.useBasicAuth !== null) ? oauth2Config.useBasicAuth : false;

    // Prepare request body
    const bodyParams = new URLSearchParams();
    bodyParams.append("grant_type", grantType);

    // For client_credentials with Basic Auth, don't include client_id/secret in body
    // For standard client_credentials (without Basic Auth), include both in body
    // For authorization_code with PKCE: some providers (like Ping Identity) may still require client_secret
    // For other grant types, always include client_id and client_secret in body
    // NOTE: For authorization_code, code_verifier will be appended AFTER getting authorization code
    //       to ensure we use the correct one that matches the code_challenge from browser flow
    if (useBasicAuth && grantType === "client_credentials") {
      // Credentials will be sent via Basic Auth header, not in body
    } else if (grantType === "authorization_code") {
      // PKCE flow: include client_id, and client_secret if provided (some providers require it)
      // code_verifier will be appended after authorization code is obtained
      bodyParams.append("client_id", oauth2Config.clientId);
      // Some OAuth providers (like Ping Identity) require client_secret even with PKCE
      if (oauth2Config.clientSecret) {
        bodyParams.append("client_secret", oauth2Config.clientSecret);
      }
    } else {
      // Standard flow: include client_id and client_secret
      bodyParams.append("client_id", oauth2Config.clientId);
      if (oauth2Config.clientSecret) {
        bodyParams.append("client_secret", oauth2Config.clientSecret);
      }
    }

    if (oauth2Config.scope) {
      bodyParams.append("scope", oauth2Config.scope);
    }

    // Some OAuth providers require audience parameter (e.g., Auth0, Ping Identity)
    if (oauth2Config.audience) {
      bodyParams.append("audience", oauth2Config.audience);
    }

    // Some OAuth providers require resource parameter
    if (oauth2Config.resource) {
      bodyParams.append("resource", oauth2Config.resource);
    }

    if (grantType === "refresh_token" && oauth2Config.refreshToken) {
      bodyParams.append("refresh_token", oauth2Config.refreshToken);
    }

    if (grantType === "authorization_code") {
      let authorizationCode = oauth2Config.authorizationCode;
      let codeVerifierToUse = oauth2Config.codeVerifier;
      
      // If authorization code is missing, try to get it automatically via browser
      if (!authorizationCode) {
        if (oauth2Config.authorizationUrl && oauth2Config.redirectUri) {
          // Check if redirect URI is localhost (can use automatic interception)
          if (isLocalhostRedirect(oauth2Config.redirectUri)) {
            console.error(`[OAuth2] 🔄 Authorization code not found. Starting automatic browser flow...`);
            try {
              const flowResult = await performOAuthFlowWithBrowser({
                authorizationUrl: oauth2Config.authorizationUrl,
                clientId: oauth2Config.clientId,
                scope: oauth2Config.scope,
                redirectUri: oauth2Config.redirectUri,
                codeVerifier: oauth2Config.codeVerifier,
              });
              
              authorizationCode = flowResult.authorizationCode;
              // ALWAYS use the code_verifier from browser flow (it matches the code_challenge used)
              // This is critical: the code_verifier must match the code_challenge sent in authorization request
              codeVerifierToUse = flowResult.codeVerifier;
              oauth2Config.codeVerifier = flowResult.codeVerifier;
              
              console.error(`[OAuth2] ✅ Authorization code obtained via browser flow`);
            } catch (flowError: unknown) {
              const flowErrorMessage = flowError instanceof Error ? flowError.message : String(flowError);
              throw new Error(
                `Failed to obtain authorization code via browser flow: ${flowErrorMessage}. ` +
                `Please set OPENL_OAUTH2_AUTHORIZATION_CODE manually or check your redirect URI configuration.`
              );
            }
          } else {
            // Redirect URI is not localhost - cannot use automatic interception
            throw new Error(
              `Authorization code is required for authorization_code grant type. ` +
              `Redirect URI is not localhost (${oauth2Config.redirectUri}), so automatic browser flow is not available. ` +
              `Please set OPENL_OAUTH2_AUTHORIZATION_CODE manually or use a localhost redirect URI for automatic flow.`
            );
          }
        } else {
          throw new Error(
            `Authorization code is required for authorization_code grant type. ` +
            `Set OPENL_OAUTH2_AUTHORIZATION_CODE or configure OPENL_OAUTH2_AUTHORIZATION_URL and OPENL_OAUTH2_REDIRECT_URI for automatic browser flow.`
          );
        }
      }
      
      bodyParams.append("code", authorizationCode);
      
      // Always append code_verifier here (after getting authorization code) to ensure we use
      // the correct one that matches the code_challenge sent in the authorization request
      // This is critical for PKCE flow: code_verifier must match code_challenge
      if (codeVerifierToUse) {
        bodyParams.append("code_verifier", codeVerifierToUse);
      } else if (oauth2Config.codeVerifier) {
        // Fallback to config code_verifier if browser flow didn't provide one
        bodyParams.append("code_verifier", oauth2Config.codeVerifier);
      }
    }

    if (grantType === "authorization_code" && oauth2Config.redirectUri) {
      bodyParams.append("redirect_uri", oauth2Config.redirectUri);
    }

    // Prepare headers
    const headers: Record<string, string> = {
      "Content-Type": "application/x-www-form-urlencoded",
    };

    // Add Basic Auth header if requested (common for Ping Identity)
    if (useBasicAuth && grantType === "client_credentials") {
      const basicAuth = Buffer.from(
        `${oauth2Config.clientId}:${oauth2Config.clientSecret}`
      ).toString("base64");
      headers["Authorization"] = `Basic ${basicAuth}`;
      console.error(`[OAuth2] Using Basic Auth for client credentials`);
    }

    try {
      console.error(`[OAuth2] Requesting token from: ${oauth2Config.tokenUrl}`);
      console.error(`[OAuth2] Grant type: ${grantType}, Use Basic Auth: ${useBasicAuth}`);
      console.error(`[OAuth2] Client ID: ${oauth2Config.clientId}`);
      if (grantType === "authorization_code" && oauth2Config.codeVerifier) {
        console.error(`[OAuth2] Using PKCE flow (code_verifier present)`);
      }
      const requestParams = [
        `grant_type=${grantType}`,
        oauth2Config.scope ? `scope=${oauth2Config.scope}` : null,
        oauth2Config.audience ? `audience=${oauth2Config.audience}` : null,
        oauth2Config.resource ? `resource=${oauth2Config.resource}` : null,
        grantType === "refresh_token" && oauth2Config.refreshToken ? "refresh_token=***" : null,
        grantType === "authorization_code" && oauth2Config.authorizationCode ? "code=***" : null,
        grantType === "authorization_code" && oauth2Config.codeVerifier ? "code_verifier=***" : null,
      ].filter(Boolean).join(", ");
      console.error(`[OAuth2] Request body params: ${requestParams}`);
      
      const response = await axios.post<Types.OAuth2Token>(
        oauth2Config.tokenUrl,
        bodyParams.toString(),
        {
          headers,
          timeout: this.config.timeout || DEFAULTS.TIMEOUT,
        }
      );

      const token = response.data;

      // Calculate absolute expiration time
      if (token.expires_in) {
        token.expires_at = Math.floor(Date.now() / 1000) + token.expires_in;
      }

      console.error(`[OAuth2] Token obtained successfully, expires in ${token.expires_in}s`);
      return token;
    } catch (error: unknown) {
      const sanitizedMessage = sanitizeError(error);
      const axiosError = error as any;
      const responseData = axiosError && axiosError.response && axiosError.response.data;
      const responseStatus = axiosError && axiosError.response && axiosError.response.status;
      const responseStatusText = axiosError && axiosError.response && axiosError.response.statusText;
      
      console.error(`[OAuth2] Token request failed:`, {
        url: oauth2Config.tokenUrl,
        grantType,
        useBasicAuth,
        status: responseStatus,
        statusText: responseStatusText,
        error: sanitizedMessage,
        responseBody: responseData ? JSON.stringify(responseData).substring(0, 500) : undefined,
      });
      
      // Provide specific guidance for common errors
      if (responseStatus === 400 && responseData && responseData.error === "unauthorized_client") {
        console.error(`[OAuth2] DIAGNOSIS: unauthorized_client error typically means:`);
        console.error(`[OAuth2]   1. Client not configured for ${grantType} grant type in OAuth provider`);
        console.error(`[OAuth2]   2. Missing required parameters (scope, audience, etc.)`);
        console.error(`[OAuth2]   3. Client credentials (ID/secret) may be incorrect`);
        console.error(`[OAuth2]   4. Check OAuth provider admin console for client configuration`);
        console.error(`[OAuth2]   - Verify client is enabled`);
        console.error(`[OAuth2]   - Verify ${grantType} grant type is allowed`);
        console.error(`[OAuth2]   - Check if scope or audience is required`);
      }
      
      // Handle invalid_client error (401) - don't retry authorization code flow
      if (responseStatus === 401 && responseData && responseData.error === "invalid_client") {
        console.error(`[OAuth2] DIAGNOSIS: invalid_client error (401) typically means:`);
        console.error(`[OAuth2]   1. Client ID or client secret is incorrect`);
        console.error(`[OAuth2]   2. Client secret may be required even for PKCE flow (some providers like Ping Identity)`);
        console.error(`[OAuth2]   3. Check that OPENL_OAUTH2_CLIENT_SECRET is set correctly`);
        console.error(`[OAuth2]   4. Verify client credentials in OAuth provider admin console`);
        // Don't clear authorization code - the code is valid, the problem is with client credentials
        throw new Error(
          `Invalid client credentials. Check OPENL_OAUTH2_CLIENT_ID and OPENL_OAUTH2_CLIENT_SECRET. ` +
          `Some providers (like Ping Identity) require client_secret even for PKCE authorization_code flow.`
        );
      }

      // Handle expired/invalid refresh_token - offer to get new one
      if (responseStatus === 400 && responseData && responseData.error === "invalid_grant" && grantType === "refresh_token") {
        console.error(`[OAuth2] ❌ Refresh token expired or invalid`);
        console.error(`[OAuth2] 🔄 Need to obtain a new refresh token`);
        
        if (oauth2Config.authorizationUrl && oauth2Config.redirectUri) {
          const authUrl = generateAuthorizationUrl({
            authorizationUrl: oauth2Config.authorizationUrl,
            clientId: oauth2Config.clientId,
            scope: oauth2Config.scope,
            redirectUri: oauth2Config.redirectUri,
          });
          
          console.error(`[OAuth2] 📋 Authorization URL:`);
          console.error(`[OAuth2] ${authUrl}`);
          console.error(`[OAuth2]`);
          console.error(`[OAuth2] 💡 Attempting to open browser automatically...`);
          
          // Try to open browser (works on host machine, not in Docker)
          openBrowser(authUrl).catch(() => {
            console.error(`[OAuth2] ⚠️  Could not open browser automatically. Please open the URL above manually.`);
          });
        } else {
          console.error(`[OAuth2] ⚠️  Authorization URL not configured. Set OPENL_OAUTH2_AUTHORIZATION_URL and OPENL_OAUTH2_REDIRECT_URI`);
        }
      }

      // Handle missing authorization_code or invalid_grant (code already used/expired)
      // Only retry if code is actually missing or if we got invalid_grant error
      const isInvalidGrant = responseStatus === 400 && responseData && responseData.error === "invalid_grant";
      const shouldRetryAuthCode = grantType === "authorization_code" && 
        (!oauth2Config.authorizationCode || isInvalidGrant);
      
      if (shouldRetryAuthCode) {
        if (isInvalidGrant) {
          console.error(`[OAuth2] ❌ Authorization code expired or already used`);
          // Clear the used authorization code so we get a new one
          oauth2Config.authorizationCode = undefined;
          // Also clear code_verifier to generate a new one with new challenge
          oauth2Config.codeVerifier = undefined;
        } else {
          console.error(`[OAuth2] ❌ Authorization code is missing`);
        }
        console.error(`[OAuth2] 🔄 Need to obtain authorization code`);
        
        if (oauth2Config.authorizationUrl && oauth2Config.redirectUri) {
          // Generate PKCE parameters if not provided
          let codeChallenge: string | undefined;
          if (oauth2Config.codeVerifier) {
            codeChallenge = generateCodeChallengeSync(oauth2Config.codeVerifier);
          } else {
            // Generate new code_verifier for PKCE
            const codeVerifier = generateCodeVerifier();
            codeChallenge = generateCodeChallengeSync(codeVerifier);
            // Store code_verifier securely in memory only (never log)
            oauth2Config.codeVerifier = codeVerifier;
            // Safe logging - only log that generation occurred, never the actual value
            if (DEBUG_AUTH) {
              // In debug mode, log a hash fingerprint for traceability only
              const verifierHash = createHash("sha256").update(codeVerifier).digest("hex");
              console.error(`[OAuth2] 🔐 Generated PKCE code_verifier (fingerprint: ${verifierHash.substring(0, 16)}...)`);
            } else {
              console.error(`[OAuth2] 🔐 Generated PKCE code_verifier`);
            }
          }

          const authUrl = generateAuthorizationUrl({
            authorizationUrl: oauth2Config.authorizationUrl,
            clientId: oauth2Config.clientId,
            scope: oauth2Config.scope,
            redirectUri: oauth2Config.redirectUri,
            codeChallenge,
          });
          
          console.error(`[OAuth2] 📋 Authorization URL:`);
          console.error(`[OAuth2] ${authUrl}`);
          console.error(`[OAuth2]`);
          console.error(`[OAuth2] 💡 Attempting to open browser automatically...`);
          
          // Try to open browser
          openBrowser(authUrl).catch(() => {
            console.error(`[OAuth2] ⚠️  Could not open browser automatically. Please open the URL above manually.`);
          });
        } else {
          console.error(`[OAuth2] ⚠️  Authorization URL not configured. Set OPENL_OAUTH2_AUTHORIZATION_URL and OPENL_OAUTH2_REDIRECT_URI`);
        }
      }
      
      throw new Error(
        `Failed to obtain OAuth 2.1 token from ${oauth2Config.tokenUrl}: ${sanitizedMessage}` +
        (responseData ? ` (Response: ${JSON.stringify(responseData)})` : "")
      );
    }
  }

  /**
   * Get the current authentication method being used
   *
   * @returns Human-readable authentication method name
   */
  public getAuthMethod(): string {
    if (this.config.oauth2) {
      return "OAuth 2.1";
    } else if (this.config.apiKey) {
      return "API Key";
    } else if (this.config.username) {
      return "Basic Auth";
    } else {
      return "None";
    }
  }
}

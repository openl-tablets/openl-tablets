/**
 * Utility functions for the OpenL Tablets MCP Server
 */

import { randomBytes, createHash } from "crypto";
import { exec } from "child_process";
import { promisify } from "util";

/**
 * Sanitize error messages to prevent sensitive data exposure
 *
 * @param error - Error object to sanitize
 * @returns Sanitized error message
 */
export function sanitizeError(error: unknown): string {
  if (error instanceof Error) {
    // Remove potential sensitive patterns from error messages
    let message = error.message;

    // Redact potential tokens (Bearer tokens, API keys, PAT tokens)
    message = message.replace(/Bearer\s+[A-Za-z0-9\-._~+/]+=*/gi, "Bearer [REDACTED]");
    message = message.replace(/Token\s+[A-Za-z0-9\-._~+/]+=*/gi, "Token [REDACTED]");
    message = message.replace(/openl_pat_[A-Za-z0-9\-._~+/]+/gi, "openl_pat_[REDACTED]");
    message = message.replace(/api[_-]?key["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "api_key=[REDACTED]");

    // Redact potential credentials in URLs
    message = message.replace(/(:\/\/)[^:@]+:[^@]+@/g, "$1[REDACTED]:[REDACTED]@");

    // Redact potential client secrets and authorization codes
    message = message.replace(/client[_-]?secret["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "client_secret=[REDACTED]");
    message = message.replace(/authorization[_-]?code["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "authorization_code=[REDACTED]");
    message = message.replace(/refresh[_-]?token["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "refresh_token=[REDACTED]");
    message = message.replace(/code[_-]?verifier["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "code_verifier=[REDACTED]");

    return message;
  }

  return "Unknown error";
}

/**
 * Type guard to check if an error is an Axios error
 *
 * @param error - Error to check
 * @returns True if error is an Axios error
 */
export function isAxiosError(error: unknown): error is import("axios").AxiosError {
  return (
    typeof error === "object" &&
    error !== null &&
    "isAxiosError" in error &&
    (error as { isAxiosError?: boolean }).isAxiosError === true
  );
}

/**
 * Validate timeout value
 *
 * @param timeout - Timeout value to validate
 * @param defaultTimeout - Default timeout to use if invalid
 * @returns Valid timeout value
 */
export function validateTimeout(timeout: number | undefined, defaultTimeout: number): number {
  if (timeout === undefined) {
    return defaultTimeout;
  }

  if (typeof timeout !== "number" || isNaN(timeout) || timeout <= 0) {
    return defaultTimeout;
  }

  // Cap at 10 minutes
  const MAX_TIMEOUT = 600000;
  return Math.min(timeout, MAX_TIMEOUT);
}

/**
 * Safe JSON stringify that handles circular references
 *
 * @param obj - Object to stringify
 * @param space - Number of spaces for indentation
 * @returns JSON string
 */
export function safeStringify(obj: unknown, space?: number): string {
  const seen = new WeakSet();

  return JSON.stringify(
    obj,
    (key, value) => {
      if (typeof value === "object" && value !== null) {
        if (seen.has(value)) {
          return "[Circular]";
        }
        seen.add(value);
      }
      return value;
    },
    space
  );
}

/**
 * Extract error details for logging without exposing sensitive data
 *
 * @param error - Error to extract details from
 * @returns Safe error details object
 */
export function extractErrorDetails(error: unknown): {
  type: string;
  message: string;
  code?: string;
  status?: number;
} {
  if (isAxiosError(error)) {
    return {
      type: "AxiosError",
      message: sanitizeError(error),
      code: error.code,
      status: error.response && error.response.status,
    };
  }

  if (error instanceof Error) {
    return {
      type: error.constructor.name,
      message: sanitizeError(error),
    };
  }

  return {
    type: "Unknown",
    message: "An unknown error occurred",
  };
}

/**
 * Parse project ID from OpenL API response
 *
 * OpenL Tablets API 6.0.0+ returns project IDs as base64-encoded strings in the format:
 * "repository:projectName" (e.g., "design:Example 1 - Bank Rating")
 *
 * Older versions may return project IDs as objects with {repository, projectName} structure.
 *
 * This function handles both formats and returns a consistent structure.
 *
 * @param id - Project ID from API (string or object)
 * @returns Parsed project ID with repository and projectName
 * @throws Error if the ID format is invalid
 */
export function parseProjectId(id: string | { repository: string; projectName: string }): {
  repository: string;
  projectName: string;
} {
  // Handle object format (older API versions or test mocks)
  if (typeof id === "object" && id !== null && "repository" in id && "projectName" in id) {
    return {
      repository: id.repository,
      projectName: id.projectName,
    };
  }

  // Handle string format (OpenL Tablets 6.0.0+)
  if (typeof id === "string") {
    try {
      // Decode base64
      const decoded = Buffer.from(id, "base64").toString("utf-8");

      // Parse "repository:projectName:hashCode" format
      const parts = decoded.split(":");
      if (parts.length === 3) {
        // Format: repository:projectName:hashCode
        const repository = parts[0];
        const projectName = parts[1];
        const hashCode = parts[2];

        if (!repository || !projectName || !hashCode) {
          throw new Error(`Invalid project ID format: empty repository, projectName, or hashCode in "${decoded}"`);
        }

        return { repository, projectName };
      } else if (parts.length === 2) {
        // Fallback: support old format "repository:projectName" for backward compatibility
        const repository = parts[0];
        const projectName = parts[1];

        if (!repository || !projectName) {
          throw new Error(`Invalid project ID format: empty repository or project name in "${decoded}"`);
        }

        return { repository, projectName };
      } else {
        throw new Error(`Invalid project ID format: expected "repository:projectName:hashCode" or "repository:projectName", got "${decoded}"`);
      }
    } catch (error) {
      // If base64 decode fails, it might be a plain string already
      // Try parsing as "repository:projectName" or "repository:projectName:hashCode" format
      const colonIndex = id.indexOf(":");
      if (colonIndex > 0 && colonIndex < id.length - 1) {
        const repository = id.substring(0, colonIndex);
        // Take everything after first colon as projectName
        // This handles cases like "repository:projectName", "repository:projectName:hashCode", 
        // or "repository:Project:With:Colons" (all colons after first are part of projectName)
        const projectName = id.substring(colonIndex + 1);

        if (repository && projectName) {
          return { repository, projectName };
        }
      }

      throw new Error(
        `Invalid project ID format: "${id}". Expected base64-encoded "repository:projectName:hashCode" or object {repository, projectName}`
      );
    }
  }

  throw new Error(
    `Invalid project ID type: ${typeof id}. Expected string or object with {repository, projectName}`
  );
}

/**
 * Create a user-friendly project ID string from repository and project name
 *
 * Format: "repository-projectName" (e.g., "design-Example 1 - Bank Rating")
 * This format is for backward compatibility. The default format is base64-encoded.
 * Note: This function creates a legacy format - prefer using base64 format from API responses.
 *
 * @param repository - Repository name
 * @param projectName - Project name
 * @returns User-friendly project ID string (legacy format, not base64)
 */
export function createProjectId(repository: string, projectName: string): string {
  return `${repository}-${projectName}`;
}

/**
 * Generate a random URL-safe string for PKCE code_verifier
 *
 * PKCE code_verifier must be:
 * - 43-128 characters long
 * - URL-safe (A-Z, a-z, 0-9, -, ., _, ~)
 *
 * @param length - Length of the code verifier (default: 128, min: 43, max: 128)
 * @returns Random URL-safe string
 */
export function generateCodeVerifier(length: number = 128): string {
  const minLength = 43;
  const maxLength = 128;
  const actualLength = Math.max(minLength, Math.min(maxLength, length));

  // URL-safe characters: A-Z, a-z, 0-9, -, ., _, ~
  const charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
  const randomValues = new Uint8Array(actualLength);
  
  // Use crypto.getRandomValues for secure random generation
  if (typeof crypto !== "undefined" && crypto.getRandomValues) {
    crypto.getRandomValues(randomValues);
  } else {
    // Fallback for Node.js environments without crypto.getRandomValues
    randomValues.set(randomBytes(actualLength));
  }

  let result = "";
  for (let i = 0; i < actualLength; i++) {
    result += charset[randomValues[i] % charset.length];
  }

  return result;
}

/**
 * Generate PKCE code_challenge from code_verifier
 *
 * Uses SHA256 hash and base64url encoding as per RFC 7636.
 *
 * @param codeVerifier - The code verifier string
 * @returns Base64url-encoded SHA256 hash of the code verifier
 */
export async function generateCodeChallenge(codeVerifier: string): Promise<string> {
  // Use Web Crypto API if available (browser/Node.js 18+)
  if (typeof crypto !== "undefined" && crypto.subtle) {
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const hashBuffer = await crypto.subtle.digest("SHA-256", data);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    
    // Convert to base64url (RFC 4648 §5)
    const base64 = btoa(String.fromCharCode(...hashArray));
    return base64.replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
  }

  // Fallback for Node.js environments
  const hash = createHash("sha256").update(codeVerifier).digest("base64");
  // Convert base64 to base64url
  return hash.replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

/**
 * Generate PKCE code_challenge synchronously (for Node.js)
 *
 * @param codeVerifier - The code verifier string
 * @returns Base64url-encoded SHA256 hash of the code verifier
 */
export function generateCodeChallengeSync(codeVerifier: string): string {
  const hash = createHash("sha256").update(codeVerifier).digest("base64");
  // Convert base64 to base64url
  return hash.replace(/\+/g, "-").replace(/\//g, "_").replace(/=/g, "");
}

/**
 * Open URL in default browser (cross-platform)
 * Works on macOS, Linux, and Windows
 *
 * @param url - URL to open
 * @returns Promise that resolves when browser is opened
 */
export async function openBrowser(url: string): Promise<void> {
  const execAsync = promisify(exec);

  const platform = process.platform;
  let command: string;

  if (platform === "darwin") {
    // macOS
    command = `open "${url}"`;
  } else if (platform === "linux") {
    // Linux - try xdg-open first, then gnome-open
    command = `xdg-open "${url}" 2>/dev/null || gnome-open "${url}" 2>/dev/null || echo "Browser not available"`;
  } else if (platform === "win32") {
    // Windows
    command = `start "" "${url}"`;
  } else {
    console.error(`[Browser] Platform ${platform} not supported for automatic browser opening`);
    return;
  }

  try {
    await execAsync(command);
    console.error(`[Browser] ✅ Opened browser with URL: ${url.substring(0, 80)}...`);
  } catch (error) {
    // Silently fail - browser opening is optional
    console.error(`[Browser] ⚠️  Could not automatically open browser. Please open manually: ${url}`);
  }
}

/**
 * Generate authorization URL for OAuth 2.1 authorization_code flow
 *
 * @param config - OAuth2 configuration
 * @param codeChallenge - PKCE code challenge (optional)
 * @returns Authorization URL
 */
export function generateAuthorizationUrl(
  config: {
    authorizationUrl: string;
    clientId: string;
    scope?: string;
    redirectUri?: string;
    codeChallenge?: string;
  }
): string {
  const params = new URLSearchParams();
  params.append("response_type", "code");
  params.append("client_id", config.clientId);

  if (config.scope) {
    params.append("scope", config.scope);
  }

  if (config.redirectUri) {
    params.append("redirect_uri", config.redirectUri);
  }

  if (config.codeChallenge) {
    params.append("code_challenge", config.codeChallenge);
    params.append("code_challenge_method", "S256");
  }

  return `${config.authorizationUrl}?${params.toString()}`;
}

/**
 * Check if a redirect URI is a localhost URL
 *
 * @param redirectUri - Redirect URI to check
 * @returns True if redirect URI is localhost
 */
export function isLocalhostRedirect(redirectUri: string): boolean {
  try {
    const url = new URL(redirectUri);
    return url.hostname === "localhost" || url.hostname === "127.0.0.1" || url.hostname === "::1";
  } catch {
    return false;
  }
}

/**
 * Extract port from redirect URI
 *
 * @param redirectUri - Redirect URI
 * @returns Port number or default 8080
 */
export function extractPortFromRedirectUri(redirectUri: string): number {
  try {
    const url = new URL(redirectUri);
    return url.port ? parseInt(url.port, 10) : (url.protocol === "https:" ? 443 : 80);
  } catch {
    return 8080;
  }
}

/**
 * Extract path from redirect URI
 *
 * @param redirectUri - Redirect URI
 * @returns Path or default "/oauth2/callback"
 */
export function extractPathFromRedirectUri(redirectUri: string): string {
  try {
    const url = new URL(redirectUri);
    return url.pathname || "/oauth2/callback";
  } catch {
    return "/oauth2/callback";
  }
}

/**
 * Start a local HTTP server to intercept OAuth authorization code
 * Opens browser automatically and waits for redirect
 *
 * @param redirectUri - Redirect URI to listen on
 * @param timeout - Timeout in milliseconds (default: 5 minutes)
 * @returns Promise resolving to authorization code
 */
export async function interceptAuthorizationCode(
  redirectUri: string,
  timeout: number = 300000
): Promise<string> {
  // Check if redirect URI is localhost
  if (!isLocalhostRedirect(redirectUri)) {
    throw new Error(
      `Redirect URI must be localhost for automatic interception. ` +
      `Current redirect URI: ${redirectUri}. ` +
      `Please use a localhost redirect URI (e.g., http://localhost:8080/oauth2/callback) or ` +
      `manually copy the authorization code from the browser.`
    );
  }

  // Dynamic import for ES modules compatibility
  const expressModule = await import("express");
  const httpModule = await import("http");
  const express = expressModule.default;
  const http = httpModule.default;
  const app = express();

  return new Promise((resolve, reject) => {

    const port = extractPortFromRedirectUri(redirectUri);
    const path = extractPathFromRedirectUri(redirectUri);

    let server: any = null;
    let timeoutId: NodeJS.Timeout | null = null;

    // Cleanup function
    const cleanup = () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
        timeoutId = null;
      }
      if (server) {
        server.close();
        server = null;
      }
    };

    // Set timeout
    timeoutId = setTimeout(() => {
      cleanup();
      reject(new Error(
        `Timeout waiting for authorization code. ` +
        `Please check the browser and manually copy the authorization code from the redirect URL.`
      ));
    }, timeout);

    // Handle OAuth callback
    app.get(path, (req: any, res: any) => {
      const code = req.query.code;
      const error = req.query.error;
      const errorDescription = req.query.error_description;

      if (error) {
        cleanup();
        res.status(400).send(`
          <html>
            <head><title>OAuth Error</title></head>
            <body>
              <h1>Authorization Failed</h1>
              <p>Error: ${error}</p>
              ${errorDescription ? `<p>Description: ${errorDescription}</p>` : ""}
              <p>You can close this window.</p>
            </body>
          </html>
        `);
        reject(new Error(`OAuth authorization failed: ${error}${errorDescription ? ` - ${errorDescription}` : ""}`));
        return;
      }

      if (code) {
        cleanup();
        res.status(200).send(`
          <html>
            <head><title>Authorization Successful</title></head>
            <body>
              <h1>✅ Authorization Successful!</h1>
              <p>Authorization code received. You can close this window.</p>
              <p>The MCP server will now exchange this code for an access token.</p>
            </body>
          </html>
        `);
        resolve(code);
      } else {
        res.status(400).send(`
          <html>
            <head><title>OAuth Error</title></head>
            <body>
              <h1>Authorization Error</h1>
              <p>No authorization code received in the callback.</p>
              <p>You can close this window.</p>
            </body>
          </html>
        `);
      }
    });

    // Start server
    try {
      server = http.createServer(app);
      server.listen(port, () => {
        console.error(`[OAuth2] 🌐 Started local HTTP server on http://localhost:${port}${path}`);
        console.error(`[OAuth2] ⏳ Waiting for authorization code (timeout: ${timeout / 1000}s)...`);
      });

      server.on("error", (err: Error) => {
        cleanup();
        if ((err as any).code === "EADDRINUSE") {
          reject(new Error(
            `Port ${port} is already in use. ` +
            `Please stop the service using this port or change the redirect URI port.`
          ));
        } else {
          reject(new Error(`Failed to start local HTTP server: ${err.message}`));
        }
      });
    } catch (err) {
      cleanup();
      reject(err);
    }
  });
}

/**
 * Result of OAuth flow with browser interaction
 */
export interface OAuthFlowResult {
  authorizationCode: string;
  codeVerifier: string;
}

/**
 * Perform OAuth 2.1 authorization code flow with automatic browser interaction
 * Opens browser, intercepts authorization code, and returns it with code verifier
 *
 * @param config - OAuth2 configuration
 * @returns Promise resolving to authorization code and code verifier
 */
export async function performOAuthFlowWithBrowser(
  config: {
    authorizationUrl: string;
    clientId: string;
    scope?: string;
    redirectUri: string;
    codeVerifier?: string;
  }
): Promise<OAuthFlowResult> {
  // Generate PKCE parameters if not provided
  let codeVerifier = config.codeVerifier;
  let codeChallenge: string;

  if (!codeVerifier) {
    codeVerifier = generateCodeVerifier();
    console.error(`[OAuth2] 🔐 Generated code_verifier for PKCE`);
  }
  codeChallenge = generateCodeChallengeSync(codeVerifier);

  // Generate authorization URL
  const authUrl = generateAuthorizationUrl({
    authorizationUrl: config.authorizationUrl,
    clientId: config.clientId,
    scope: config.scope,
    redirectUri: config.redirectUri,
    codeChallenge,
  });

  console.error(`[OAuth2] 📋 Authorization URL:`);
  console.error(`[OAuth2] ${authUrl}`);
  console.error(`[OAuth2]`);

  // Start local server to intercept callback
  const interceptPromise = interceptAuthorizationCode(config.redirectUri);

  // Open browser
  console.error(`[OAuth2] 🌐 Opening browser for authorization...`);
  await openBrowser(authUrl);

  // Wait for authorization code
  console.error(`[OAuth2] ⏳ Please complete the authorization in the browser...`);
  const authorizationCode = await interceptPromise;

  console.error(`[OAuth2] ✅ Authorization code received successfully!`);
  
  return {
    authorizationCode,
    codeVerifier,
  };
}

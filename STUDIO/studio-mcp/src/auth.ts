/**
 * Authentication module for OpenL MCP Server
 *
 * Authenticates with a Personal Access Token (PAT) under the `Token` scheme by
 * default, or with a `Bearer` credential when the config asks for it (a Studio
 * in oauth2 mode accepts an IdP access token that way). When no token is
 * configured, requests are sent without an Authorization header (OpenL Studio
 * single-user mode).
 */

import { AxiosInstance, InternalAxiosRequestConfig } from "axios";
import type * as Types from "./types.js";
import { HEADERS } from "./constants.js";
import { extractApiErrorInfo, parseBoolEnv } from "./utils.js";

/**
 * Check if debug logging is enabled (via environment variable).
 * Accepts any truthy value: 1, true, yes, on, y (case-insensitive).
 */
const DEBUG_AUTH = parseBoolEnv(process.env.DEBUG_AUTH) || parseBoolEnv(process.env.DEBUG);

/**
 * Cache of logged authentication configs to prevent duplicate logging
 * Key: hash of config (baseUrl + auth method)
 */
const loggedAuthConfigs = new Set<string>();

/**
 * Authentication manager for OpenL Studio API
 *
 * Handles:
 * - Optional Personal Access Token headers
 * - Anonymous single-user Studio access
 * - Request/response interceptors
 * - Actionable 401 diagnostics
 */
export class AuthenticationManager {
  private config: Types.OpenLConfig;
  private configuredInstances: WeakSet<AxiosInstance> = new WeakSet();

  constructor(config: Types.OpenLConfig) {
    this.config = config;
  }

  /**
   * Configure authentication interceptors for an Axios instance
   *
   * @param axiosInstance - The Axios instance to configure
   */
  public setupInterceptors(axiosInstance: AxiosInstance): void {
    // Prevent duplicate interceptor registration for the same instance
    if (this.configuredInstances.has(axiosInstance)) {
      return;
    }
    this.configuredInstances.add(axiosInstance);

    // Clear any existing interceptors to prevent duplication
    // Note: We check configuredInstances first to avoid clearing interceptors from other managers
    axiosInstance.interceptors.request.clear();
    axiosInstance.interceptors.response.clear();

    // Request interceptor: Add authentication headers
    axiosInstance.interceptors.request.use(
      async (config) => {
        // Early return if this config has already been processed
        // This prevents duplicate processing if interceptor is called multiple times
        if ((config as any)._authHeadersAdded) {
          return config;
        }

        const authConfig = await this.addAuthHeaders(config);

        return authConfig;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor: Handle 401 errors with token refresh
    axiosInstance.interceptors.response.use(
      (response) => {
        return response;
      },
      async (error) => {
        // Original request config available if needed for debugging
        void error.config;

        // Enhanced 401 error handling with API error extraction
        if (error.response && error.response.status === 401) {
          const fullUrl = `${(error.config && error.config.baseURL) || ''}${(error.config && error.config.url) || ''}`;
          const authMethod = this.getAuthMethod();
          const apiErrorInfo = extractApiErrorInfo(error.response.data, 401);

          const errorMessage = apiErrorInfo.message || 'Unauthorized';
          console.error(`[Auth] 401 Unauthorized: ${errorMessage} (${authMethod})`);

          if (DEBUG_AUTH) {
            console.error(`[Auth] URL: ${fullUrl}`);
            if (apiErrorInfo.code) {
              console.error(`[Auth] Error Code: ${apiErrorInfo.code}`);
            }
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
    // Check if this config has already been processed (to avoid duplicate logging)
    // Use a flag in the config object itself to track processing
    if ((config as any)._authHeadersAdded) {
      return config;
    }

    if (!config.headers) {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      config.headers = {} as any;
    }

    // Check if auth headers are already set (to avoid duplicate logging)
    const authHeaderAlreadySet = config.headers[HEADERS.AUTHORIZATION];

    // Create a unique key for this auth config to prevent duplicate logging
    const authConfigKey = `${this.config.baseUrl || ''}:${this.config.personalAccessToken ? 'PAT' : 'None'}`;
    // In CLI mode, suppress informational [Auth] lines so they don't pollute
    // shell pipelines. Set by src/cli.ts via OPENL_CLI_QUIET; accepts any truthy value
    // (1/true/yes/on). Genuine error logs (e.g. 401) are NOT gated and
    // continue to surface.
    const quietMode = parseBoolEnv(process.env.OPENL_CLI_QUIET);
    const shouldLogAuth = !authHeaderAlreadySet && !loggedAuthConfigs.has(authConfigKey) && !quietMode;

    // Add authentication when a Personal Access Token is configured; otherwise
    // send no Authorization header. Running without a token is a normal,
    // supported configuration (OpenL Studio single-user mode / anonymous
    // access), so the no-token case is deliberately not logged or warned about.
    if (this.config.personalAccessToken) {
      // Build authorization header
      const pat = this.config.personalAccessToken;
      const scheme = this.scheme();
      config.headers[HEADERS.AUTHORIZATION] = `${scheme} ${pat}`;

      // Log only once per unique config (to avoid duplicate logging)
      if (shouldLogAuth) {
        loggedAuthConfigs.add(authConfigKey);
        // The `openl_pat_` shape check applies to PATs only. A Bearer credential
        // is an IdP access token and never carries that prefix, so warning about
        // it there would fire on every correctly-configured OAuth session.
        if (scheme === "Bearer") {
          console.error(`[Auth] 🔐 Bearer Authentication | Header: Authorization: Bearer <token>`);
        } else {
          const isValidFormat = pat.startsWith('openl_pat_');
          console.error(`[Auth] 🔐 PAT Authentication (${isValidFormat ? 'valid format' : '⚠️  invalid format'}) | Header: Authorization: Token <PAT>`);
          if (!isValidFormat) {
            console.error(`[Auth]   ⚠️  WARNING: PAT should start with 'openl_pat_'`);
          }
        }
      }
    }

    // Mark this config as processed to prevent duplicate processing
    (config as any)._authHeadersAdded = true;

    return config;
  }

  /**
   * Build the `Authorization` header value this manager would set on outgoing
   * HTTP requests, or `undefined` when no auth is configured. Useful for
   * non-axios consumers (e.g. the STOMP WebSocket handshake) that need to
   * send the same authentication scheme as REST.
   *
   * Matches `addAuthHeaders`: PAT when configured, otherwise none.
   */
  public getAuthorizationHeader(): string | undefined {
    if (this.config.personalAccessToken) {
      return `${this.scheme()} ${this.config.personalAccessToken}`;
    }
    return undefined;
  }

  /**
   * The scheme to present, defaulting to `Token`.
   *
   * Deliberately read here rather than at construction: this is the ONLY place
   * the outgoing scheme is decided, and both emit sites — the REST interceptor
   * and {@link getAuthorizationHeader}, which is what the STOMP WebSocket
   * handshake sends — go through it. Splitting the decision is how the two ended
   * up able to disagree.
   */
  private scheme(): Types.OpenLAuthScheme {
    return this.config.authScheme ?? "Token";
  }

  /**
   * Get the current authentication method being used
   *
   * @returns Human-readable authentication method name
   */
  public getAuthMethod(): string {
    if (this.config.personalAccessToken) {
      return this.scheme() === "Bearer" ? "Bearer token" : "Personal Access Token";
    } else {
      return "No Auth";
    }
  }
}

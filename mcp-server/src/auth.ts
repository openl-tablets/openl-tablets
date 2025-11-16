/**
 * Authentication module for OpenL Tablets MCP Server
 *
 * Supports multiple authentication methods:
 * - Basic Authentication (username/password)
 * - API Key authentication
 * - OAuth 2.1 (with automatic token management)
 */

import axios, { AxiosInstance, InternalAxiosRequestConfig } from "axios";
import FormData from "form-data";
import * as Types from "./types.js";
import { DEFAULTS, HEADERS } from "./constants.js";
import { sanitizeError } from "./utils.js";

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
      async (config) => this.addAuthHeaders(config),
      (error) => Promise.reject(error)
    );

    // Response interceptor: Handle 401 errors with token refresh
    axiosInstance.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // If 401 and we have OAuth2, try refreshing token
        if (
          error.response?.status === 401 &&
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
      const token = await this.getValidToken();
      if (token) {
        config.headers[HEADERS.AUTHORIZATION] = `Bearer ${token.access_token}`;
      }
    } else if (this.config.apiKey) {
      config.headers[HEADERS.API_KEY] = this.config.apiKey;
    } else if (this.config.username && this.config.password) {
      const auth = Buffer.from(
        `${this.config.username}:${this.config.password}`
      ).toString("base64");
      config.headers[HEADERS.AUTHORIZATION] = `Basic ${auth}`;
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

    const formData = new FormData();
    formData.append("grant_type", grantType);
    formData.append("client_id", oauth2Config.clientId);
    formData.append("client_secret", oauth2Config.clientSecret);

    if (oauth2Config.scope) {
      formData.append("scope", oauth2Config.scope);
    }

    if (grantType === "refresh_token" && oauth2Config.refreshToken) {
      formData.append("refresh_token", oauth2Config.refreshToken);
    }

    try {
      const response = await axios.post<Types.OAuth2Token>(
        oauth2Config.tokenUrl,
        formData,
        {
          headers: formData.getHeaders(),
          timeout: this.config.timeout || DEFAULTS.TIMEOUT,
        }
      );

      const token = response.data;

      // Calculate absolute expiration time
      if (token.expires_in) {
        token.expires_at = Math.floor(Date.now() / 1000) + token.expires_in;
      }

      return token;
    } catch (error: unknown) {
      const sanitizedMessage = sanitizeError(error);
      throw new Error(`Failed to obtain OAuth 2.1 token: ${sanitizedMessage}`);
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

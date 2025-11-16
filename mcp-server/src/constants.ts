/**
 * Constants and configuration defaults for the OpenL Tablets MCP Server
 */

/**
 * Default configuration values
 */
export const DEFAULTS = {
  /** Default timeout for HTTP requests (30 seconds) */
  TIMEOUT: 30000,

  /** Token expiration buffer in seconds (refresh 60s before expiry) */
  TOKEN_EXPIRATION_BUFFER: 60,

  /** Default OAuth 2.1 grant type */
  OAUTH2_GRANT_TYPE: "client_credentials" as const,
} as const;

/**
 * API endpoint paths relative to base URL
 */
export const API_ENDPOINTS = {
  /** List all design repositories */
  REPOSITORIES: "/design-repositories",

  /** OAuth 2.1 token acquisition (placeholder - actual URL from config) */
  OAUTH2_TOKEN: "/oauth/token",
} as const;

/**
 * HTTP headers
 */
export const HEADERS = {
  /** Content type for JSON requests */
  CONTENT_TYPE_JSON: "application/json",

  /** Authorization header */
  AUTHORIZATION: "Authorization",

  /** API Key header */
  API_KEY: "X-API-Key",

  /** Client Document ID header for request tracking */
  CLIENT_DOCUMENT_ID: "X-Client-Document-Id",
} as const;

/**
 * Tool categories for metadata organization
 */
export const TOOL_CATEGORIES = {
  SYSTEM: "system",
  REPOSITORY: "repository",
  PROJECT: "project",
  RULES: "rules",
  VERSION_CONTROL: "version-control",
  DEPLOYMENT: "deployment",
} as const;

/**
 * Project ID format pattern: {repository-name}-{project-name}
 */
export const PROJECT_ID_PATTERN = /^([^-]+)-(.+)$/;

/**
 * Server information
 */
export const SERVER_INFO = {
  NAME: "openl-mcp-server",
  VERSION: "1.0.0",
  DESCRIPTION: "Model Context Protocol server for OpenL Tablets",
} as const;

/**
 * Response formatting limits
 */
export const RESPONSE_LIMITS = {
  /** Maximum response character count (~25,000) */
  MAX_CHARACTERS: 25000,

  /** Truncation warning message */
  TRUNCATION_MESSAGE: "Response truncated due to size. Use limit/offset parameters or narrower filters to retrieve full data.",
} as const;

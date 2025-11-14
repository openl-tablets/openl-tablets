/**
 * Configuration types for OpenL Studio MCP Server
 */

export interface OpenLStudioConfig {
  /** Base URL of OpenL Studio instance (e.g., http://localhost:8080) */
  baseUrl: string;

  /** OpenL Studio username */
  username: string;

  /** OpenL Studio password */
  password: string;

  /** Request timeout in milliseconds (default: 30000) */
  timeout?: number;

  /** Number of retry attempts for failed requests (default: 3) */
  retries?: number;
}

export interface ServerConfig {
  /** OpenL Studio connection configuration */
  openl: OpenLStudioConfig;

  /** Log level (ERROR, WARN, INFO, DEBUG) */
  logLevel?: 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
}

export const DEFAULT_CONFIG: Partial<ServerConfig> = {
  logLevel: 'INFO',
};

export const DEFAULT_OPENL_CONFIG: Partial<OpenLStudioConfig> = {
  timeout: 30000,
  retries: 3,
};

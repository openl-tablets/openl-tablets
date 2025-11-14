/**
 * Custom error classes for OpenL Studio MCP Server
 */

/**
 * Base error class for all OpenL Studio errors
 */
export class OpenLError extends Error {
  constructor(
    message: string,
    public readonly code?: string,
    public readonly details?: any
  ) {
    super(message);
    this.name = 'OpenLError';
    Object.setPrototypeOf(this, OpenLError.prototype);
  }
}

/**
 * Authentication error (401, 403)
 */
export class AuthenticationError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'AUTH_ERROR', details);
    this.name = 'AuthenticationError';
    Object.setPrototypeOf(this, AuthenticationError.prototype);
  }
}

/**
 * Not found error (404)
 */
export class NotFoundError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'NOT_FOUND', details);
    this.name = 'NotFoundError';
    Object.setPrototypeOf(this, NotFoundError.prototype);
  }
}

/**
 * Validation error (400)
 */
export class ValidationError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'VALIDATION_ERROR', details);
    this.name = 'ValidationError';
    Object.setPrototypeOf(this, ValidationError.prototype);
  }
}

/**
 * Permission denied error (403)
 */
export class PermissionError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'PERMISSION_DENIED', details);
    this.name = 'PermissionError';
    Object.setPrototypeOf(this, PermissionError.prototype);
  }
}

/**
 * Server error (500+)
 */
export class ServerError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'SERVER_ERROR', details);
    this.name = 'ServerError';
    Object.setPrototypeOf(this, ServerError.prototype);
  }
}

/**
 * Network error (connection failed, timeout, etc.)
 */
export class NetworkError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'NETWORK_ERROR', details);
    this.name = 'NetworkError';
    Object.setPrototypeOf(this, NetworkError.prototype);
  }
}

/**
 * Configuration error
 */
export class ConfigError extends OpenLError {
  constructor(message: string, details?: any) {
    super(message, 'CONFIG_ERROR', details);
    this.name = 'ConfigError';
    Object.setPrototypeOf(this, ConfigError.prototype);
  }
}

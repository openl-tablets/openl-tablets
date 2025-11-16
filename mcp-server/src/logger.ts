/**
 * Structured logging utilities for MCP server
 *
 * All logging goes to stderr to avoid interfering with MCP protocol on stdout.
 */

import { sanitizeError } from "./utils.js";

/**
 * Log levels
 */
export enum LogLevel {
  ERROR = "ERROR",
  WARN = "WARN",
  INFO = "INFO",
  DEBUG = "DEBUG",
}

/**
 * Log context type
 */
export type LogContext = Record<string, unknown>;

/**
 * Structured logger that outputs to stderr only
 */
export const logger = {
  /**
   * Log error message
   *
   * @param message - Error message
   * @param context - Additional context (error objects will be sanitized)
   */
  error: (message: string, context?: LogContext): void => {
    const sanitizedContext = context ? sanitizeContext(context) : undefined;
    console.error(
      `[${LogLevel.ERROR}] ${message}`,
      sanitizedContext ? JSON.stringify(sanitizedContext) : ""
    );
  },

  /**
   * Log warning message
   *
   * @param message - Warning message
   * @param context - Additional context
   */
  warn: (message: string, context?: LogContext): void => {
    console.error(
      `[${LogLevel.WARN}] ${message}`,
      context ? JSON.stringify(context) : ""
    );
  },

  /**
   * Log info message (use sparingly in production)
   *
   * @param message - Info message
   * @param context - Additional context
   */
  info: (message: string, context?: LogContext): void => {
    if (process.env.NODE_ENV === "development" || process.env.DEBUG) {
      console.error(
        `[${LogLevel.INFO}] ${message}`,
        context ? JSON.stringify(context) : ""
      );
    }
  },

  /**
   * Log debug message (only in development/debug mode)
   *
   * @param message - Debug message
   * @param context - Additional context
   */
  debug: (message: string, context?: LogContext): void => {
    if (process.env.NODE_ENV === "development" || process.env.DEBUG) {
      console.error(
        `[${LogLevel.DEBUG}] ${message}`,
        context ? JSON.stringify(context) : ""
      );
    }
  },
};

/**
 * Sanitize log context to prevent sensitive data exposure
 *
 * @param context - Log context to sanitize
 * @returns Sanitized context
 */
function sanitizeContext(context: LogContext): LogContext {
  const sanitized: LogContext = {};

  for (const [key, value] of Object.entries(context)) {
    if (value instanceof Error) {
      sanitized[key] = sanitizeError(value);
    } else if (typeof value === "object" && value !== null) {
      // Recursively sanitize nested objects
      sanitized[key] = sanitizeContext(value as LogContext);
    } else {
      sanitized[key] = value;
    }
  }

  return sanitized;
}

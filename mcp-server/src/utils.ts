/**
 * Utility functions for the OpenL Tablets MCP Server
 */

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

    // Redact potential tokens (Bearer tokens, API keys)
    message = message.replace(/Bearer\s+[A-Za-z0-9\-._~+/]+=*/gi, "Bearer [REDACTED]");
    message = message.replace(/api[_-]?key["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "api_key=[REDACTED]");

    // Redact potential credentials in URLs
    message = message.replace(/(:\/\/)[^:@]+:[^@]+@/g, "$1[REDACTED]:[REDACTED]@");

    // Redact potential client secrets
    message = message.replace(/client[_-]?secret["\s:=]+[A-Za-z0-9\-._~+/]+/gi, "client_secret=[REDACTED]");

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
      status: error.response?.status,
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

      // Parse "repository:projectName" format
      const colonIndex = decoded.indexOf(":");
      if (colonIndex === -1) {
        throw new Error(`Invalid project ID format: missing colon separator in "${decoded}"`);
      }

      const repository = decoded.substring(0, colonIndex);
      const projectName = decoded.substring(colonIndex + 1);

      if (!repository || !projectName) {
        throw new Error(`Invalid project ID format: empty repository or project name in "${decoded}"`);
      }

      return { repository, projectName };
    } catch (error) {
      // If base64 decode fails, it might be a plain string already
      // Try parsing as "repository:projectName" format
      const colonIndex = id.indexOf(":");
      if (colonIndex !== -1) {
        const repository = id.substring(0, colonIndex);
        const projectName = id.substring(colonIndex + 1);

        if (repository && projectName) {
          return { repository, projectName };
        }
      }

      throw new Error(
        `Invalid project ID format: "${id}". Expected base64-encoded "repository:projectName" or object {repository, projectName}`
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
 * This format is easier for humans to read than base64 encoding.
 *
 * @param repository - Repository name
 * @param projectName - Project name
 * @returns User-friendly project ID string
 */
export function createProjectId(repository: string, projectName: string): string {
  return `${repository}-${projectName}`;
}

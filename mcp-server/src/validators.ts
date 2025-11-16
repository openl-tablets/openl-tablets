/**
 * Input validation utilities
 */

import { McpError, ErrorCode } from "@modelcontextprotocol/sdk/types.js";
import { PROJECT_ID_PATTERN } from "./constants.js";

/**
 * Validate base64-encoded content
 *
 * @param content - Content to validate
 * @returns True if valid base64, false otherwise
 */
export function validateBase64(content: string): boolean {
  // Check if string matches base64 pattern
  const base64Regex = /^[A-Za-z0-9+/]*={0,2}$/;
  if (!base64Regex.test(content)) {
    return false;
  }

  // Try to decode to verify it's valid base64
  try {
    Buffer.from(content, "base64");
    return true;
  } catch {
    return false;
  }
}

/**
 * Validate and parse project ID
 *
 * Expected format: "repository-projectName" (e.g., "design-InsuranceRules")
 *
 * @param projectId - Project ID to validate
 * @returns Parsed repository and project name
 * @throws McpError if format is invalid
 */
export function validateProjectId(projectId: string): {
  repository: string;
  projectName: string;
} {
  const match = PROJECT_ID_PATTERN.exec(projectId);
  if (!match) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `Invalid projectId format. Expected 'repository-projectName', got '${projectId}'`
    );
  }

  return {
    repository: match[1],
    projectName: match[2],
  };
}

/**
 * Validate pagination parameters
 *
 * @param limit - Maximum items per page
 * @param offset - Starting position
 * @returns Validated pagination parameters
 * @throws McpError if parameters are invalid
 */
export function validatePagination(
  limit?: number,
  offset?: number
): { limit: number; offset: number } {
  const validatedLimit = limit ?? 50;
  const validatedOffset = offset ?? 0;

  if (validatedLimit < 1) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `limit must be positive, got ${validatedLimit}`
    );
  }

  if (validatedLimit > 200) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `limit must be <= 200, got ${validatedLimit}`
    );
  }

  if (validatedOffset < 0) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `offset must be non-negative, got ${validatedOffset}`
    );
  }

  return { limit: validatedLimit, offset: validatedOffset };
}

/**
 * Validate response format parameter
 *
 * @param format - Response format to validate
 * @returns Validated format ("json" or "markdown")
 */
export function validateResponseFormat(
  format?: string
): "json" | "markdown" {
  if (!format) {
    return "markdown"; // Default to markdown
  }

  if (format !== "json" && format !== "markdown") {
    throw new McpError(
      ErrorCode.InvalidParams,
      `response_format must be "json" or "markdown", got "${format}"`
    );
  }

  return format;
}

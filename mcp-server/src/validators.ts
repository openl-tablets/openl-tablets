/**
 * Input validation utilities
 */

import { McpError, ErrorCode } from "@modelcontextprotocol/sdk/types.js";
import { PROJECT_ID_PATTERN } from "./constants.js";

/**
 * Validate base64-encoded content
 *
 * Strips whitespace (spaces, newlines, tabs) before validation to match
 * Node.js Buffer.from() behavior, which ignores whitespace in base64 strings.
 *
 * @param content - Content to validate
 * @returns True if valid base64, false otherwise
 */
export function validateBase64(content: string): boolean {
  // Strip all whitespace (spaces, newlines, tabs, carriage returns)
  // Node.js Buffer.from() with 'base64' encoding automatically ignores whitespace,
  // so we should accept it too for consistency
  const stripped = content.replace(/\s/g, "");

  // Empty string is valid base64 (decodes to empty buffer)
  if (stripped === "") {
    return true;
  }

  // Check if string matches base64 pattern
  const base64Regex = /^[A-Za-z0-9+/]*={0,2}$/;
  if (!base64Regex.test(stripped)) {
    return false;
  }

  // Try to decode to verify it's valid base64
  try {
    Buffer.from(stripped, "base64");
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
      `Invalid projectId format. Expected 'repository-projectName', got '${projectId}'. ` +
      `To find valid project IDs, use: openl_list_projects()`
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
      `limit must be positive, got ${validatedLimit}. Set limit to a value between 1-200 (e.g., limit: 50)`
    );
  }

  if (validatedLimit > 200) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `limit exceeds maximum allowed value. Got ${validatedLimit}, but limit must be <= 200. ` +
      `To retrieve more data, use pagination with offset (e.g., limit: 200, offset: 200 for next page)`
    );
  }

  if (validatedOffset < 0) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `offset must be non-negative, got ${validatedOffset}. Start with offset: 0 for first page`
    );
  }

  return { limit: validatedLimit, offset: validatedOffset };
}

/**
 * Validate response format parameter
 *
 * @param format - Response format to validate
 * @returns Validated format ("json", "markdown", "markdown_concise", or "markdown_detailed")
 */
export function validateResponseFormat(
  format?: string
): "json" | "markdown" | "markdown_concise" | "markdown_detailed" {
  if (!format) {
    return "markdown"; // Default to markdown
  }

  const validFormats = ["json", "markdown", "markdown_concise", "markdown_detailed"];
  if (!validFormats.includes(format)) {
    throw new McpError(
      ErrorCode.InvalidParams,
      `response_format must be one of: ${validFormats.join(", ")}. Got "${format}". Use "markdown_concise" for brief summaries or "markdown_detailed" for full context.`
    );
  }

  return format as "json" | "markdown" | "markdown_concise" | "markdown_detailed";
}

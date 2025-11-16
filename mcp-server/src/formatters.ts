/**
 * Response formatting utilities
 *
 * Handles JSON and Markdown formatting, pagination, and character limit enforcement.
 */

import { RESPONSE_LIMITS } from "./constants.js";
import { safeStringify } from "./utils.js";

/**
 * Pagination metadata
 */
export interface PaginationMetadata {
  limit: number;
  offset: number;
  has_more: boolean;
  next_offset?: number;
  total_count?: number;
}

/**
 * Paginated response wrapper
 */
export interface PaginatedResponse<T> {
  data: T;
  pagination?: PaginationMetadata;
  truncated?: boolean;
  truncation_message?: string;
}

/**
 * Format response options
 */
export interface FormatOptions {
  /** Pagination metadata */
  pagination?: {
    limit: number;
    offset: number;
    total: number;
  };
  /** Character limit (defaults to RESPONSE_LIMITS.MAX_CHARACTERS) */
  characterLimit?: number;
  /** Data type hint for markdown formatting */
  dataType?: string;
}

/**
 * Format response data as JSON or Markdown (standard, concise, or detailed)
 *
 * @param data - Data to format
 * @param format - Output format: "json" (structured), "markdown" (default full format),
 *                 "markdown_concise" (1-2 paragraph summary), "markdown_detailed" (full + context)
 * @param options - Formatting options
 * @returns Formatted response string
 */
export function formatResponse<T>(
  data: T,
  format: "json" | "markdown" | "markdown_concise" | "markdown_detailed" = "markdown",
  options?: FormatOptions
): string {
  // Create paginated response structure
  const response: PaginatedResponse<T> = {
    data,
  };

  // Add pagination metadata if provided
  if (options?.pagination) {
    const { limit, offset, total } = options.pagination;
    const has_more = offset + limit < total;
    response.pagination = {
      limit,
      offset,
      has_more,
      next_offset: has_more ? offset + limit : undefined,
      total_count: total,
    };
  }

  // Convert to string
  let formattedString: string;
  if (format === "json") {
    formattedString = safeStringify(response, 2);
  } else if (format === "markdown_concise") {
    formattedString = toMarkdownConcise(response, options?.dataType);
  } else if (format === "markdown_detailed") {
    formattedString = toMarkdownDetailed(response, options?.dataType);
  } else {
    // Default markdown
    formattedString = toMarkdown(response, options?.dataType);
  }

  // Check character limit
  const charLimit = options?.characterLimit ?? RESPONSE_LIMITS.MAX_CHARACTERS;
  if (formattedString.length > charLimit) {
    const truncated = formattedString.slice(0, charLimit);
    const truncationNote = `\n\n${RESPONSE_LIMITS.TRUNCATION_MESSAGE}`;
    return truncated + truncationNote;
  }

  return formattedString;
}

/**
 * Convert data to markdown format
 *
 * @param response - Response data with pagination
 * @param dataType - Hint about the data type
 * @returns Markdown-formatted string
 */
export function toMarkdown<T>(
  response: PaginatedResponse<T>,
  dataType?: string
): string {
  const parts: string[] = [];

  // Format the main data
  const data = response.data;

  // Try to detect data type if not provided
  if (!dataType && Array.isArray(data) && data.length > 0) {
    const first = data[0];
    if (typeof first === "object" && first !== null) {
      if ("repository" in first && "type" in first) {
        dataType = "repositories";
      } else if ("projectId" in first || "projectName" in first) {
        dataType = "projects";
      } else if ("tableId" in first || ("name" in first && "tableType" in first)) {
        dataType = "tables";
      } else if ("deploymentName" in first) {
        dataType = "deployments";
      } else if ("commitHash" in first || "hash" in first) {
        dataType = "history";
      }
    }
  }

  // Format based on data type
  switch (dataType) {
    case "repositories":
      parts.push(formatRepositories(data as any));
      break;
    case "projects":
      parts.push(formatProjects(data as any));
      break;
    case "tables":
      parts.push(formatTables(data as any));
      break;
    case "deployments":
      parts.push(formatDeployments(data as any));
      break;
    case "history":
      parts.push(formatHistory(data as any));
      break;
    default:
      // Generic object/array formatting
      parts.push(formatGeneric(data));
  }

  // Add pagination info if present
  if (response.pagination) {
    parts.push(formatPagination(response.pagination));
  }

  return parts.join("\n\n");
}

/**
 * Convert data to concise markdown format (1-2 paragraph summary)
 *
 * @param response - Response data with pagination
 * @param dataType - Hint about the data type
 * @returns Concise markdown-formatted string
 */
export function toMarkdownConcise<T>(
  response: PaginatedResponse<T>,
  dataType?: string
): string {
  const data = response.data;
  const parts: string[] = [];

  // Detect data type if not provided
  if (!dataType && Array.isArray(data) && data.length > 0) {
    const first = data[0];
    if (typeof first === "object" && first !== null) {
      if ("repository" in first && "type" in first) dataType = "repositories";
      else if ("projectId" in first || "projectName" in first) dataType = "projects";
      else if ("tableId" in first || ("name" in first && "tableType" in first)) dataType = "tables";
      else if ("deploymentName" in first) dataType = "deployments";
      else if ("commitHash" in first || "hash" in first) dataType = "history";
    }
  }

  // Generate concise summary based on data type
  if (Array.isArray(data)) {
    const count = data.length;
    const total = response.pagination?.total_count ?? count;

    switch (dataType) {
      case "repositories":
        parts.push(`Found ${total} ${total === 1 ? 'repository' : 'repositories'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const names = data.slice(0, 3).map((r: any) => r.repository || r.name).join(", ");
          parts.push(`Repositories: ${names}${count > 3 ? `, and ${count - 3} more` : ''}.`);
        }
        break;
      case "projects":
        parts.push(`Found ${total} ${total === 1 ? 'project' : 'projects'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const opened = data.filter((p: any) => p.status === "OPENED").length;
          const names = data.slice(0, 3).map((p: any) => p.projectName || p.projectId).join(", ");
          parts.push(`${opened} opened. Projects: ${names}${count > 3 ? `, and ${count - 3} more` : ''}.`);
        }
        break;
      case "tables":
        parts.push(`Found ${total} ${total === 1 ? 'table' : 'tables'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const types = [...new Set(data.map((t: any) => t.tableType))].join(", ");
          parts.push(`Table types: ${types}.`);
        }
        break;
      case "deployments":
        parts.push(`Found ${total} ${total === 1 ? 'deployment' : 'deployments'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const names = data.slice(0, 3).map((d: any) => d.deploymentName).join(", ");
          parts.push(`Deployments: ${names}${count > 3 ? `, and ${count - 3} more` : ''}.`);
        }
        break;
      case "history":
        parts.push(`Found ${total} ${total === 1 ? 'commit' : 'commits'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const latest = data[0] as any;
          parts.push(`Latest: ${latest.comment || latest.message || 'No message'} by ${latest.author || 'Unknown'}.`);
        }
        break;
      default:
        parts.push(`Found ${total} ${total === 1 ? 'item' : 'items'}${count < total ? ` (showing ${count})` : ''}.`);
    }
  } else {
    // Single object summary
    parts.push("Retrieved details successfully.");
  }

  // Add pagination note if applicable
  if (response.pagination && response.pagination.has_more) {
    parts.push(`Use offset=${response.pagination.next_offset} to retrieve more results.`);
  }

  return parts.join(" ");
}

/**
 * Convert data to detailed markdown format (all fields + rich context)
 *
 * @param response - Response data with pagination
 * @param dataType - Hint about the data type
 * @returns Detailed markdown-formatted string
 */
export function toMarkdownDetailed<T>(
  response: PaginatedResponse<T>,
  dataType?: string
): string {
  const parts: string[] = [];
  const data = response.data;

  // Detect data type if not provided
  if (!dataType && Array.isArray(data) && data.length > 0) {
    const first = data[0];
    if (typeof first === "object" && first !== null) {
      if ("repository" in first && "type" in first) dataType = "repositories";
      else if ("projectId" in first || "projectName" in first) dataType = "projects";
      else if ("tableId" in first || ("name" in first && "tableType" in first)) dataType = "tables";
      else if ("deploymentName" in first) dataType = "deployments";
      else if ("commitHash" in first || "hash" in first) dataType = "history";
    }
  }

  // Add contextual header with metadata
  if (Array.isArray(data)) {
    const count = data.length;
    const total = response.pagination?.total_count ?? count;
    parts.push(`# ${dataType ? dataType.charAt(0).toUpperCase() + dataType.slice(1) : 'Results'}`);
    parts.push(`\n**Summary:** ${total} total ${total === 1 ? 'item' : 'items'}${count < total ? ` (showing ${count} on this page)` : ''}`);

    // Add timestamp
    parts.push(`**Retrieved:** ${new Date().toISOString()}`);
  }

  // Use standard markdown formatting (calls existing formatters)
  const standardMarkdown = toMarkdown(response, dataType);
  parts.push(standardMarkdown);

  // Add additional context based on data type
  if (Array.isArray(data) && data.length > 0) {
    switch (dataType) {
      case "projects":
        const opened = data.filter((p: any) => p.status === "OPENED").length;
        const closed = data.filter((p: any) => p.status === "CLOSED").length;
        parts.push(`\n---\n**Status Breakdown:** ${opened} opened, ${closed} closed`);
        break;
      case "tables":
        const typeCount: Record<string, number> = {};
        data.forEach((t: any) => {
          typeCount[t.tableType] = (typeCount[t.tableType] || 0) + 1;
        });
        const breakdown = Object.entries(typeCount).map(([type, count]) => `${type}: ${count}`).join(", ");
        parts.push(`\n---\n**Type Breakdown:** ${breakdown}`);
        break;
    }
  }

  return parts.join("\n");
}

/**
 * Format repositories as markdown table
 */
function formatRepositories(repos: any[]): string {
  if (!Array.isArray(repos) || repos.length === 0) {
    return "No repositories found.";
  }

  const lines = [
    "# Repositories",
    "",
    "| Name | Type | Status |",
    "|------|------|--------|",
  ];

  for (const repo of repos) {
    const name = repo.name || repo.repository || "N/A";
    const type = repo.type || "N/A";
    const status = repo.status || "N/A";
    lines.push(`| ${name} | ${type} | ${status} |`);
  }

  return lines.join("\n");
}

/**
 * Format projects as markdown list
 */
function formatProjects(projects: any[]): string {
  if (!Array.isArray(projects) || projects.length === 0) {
    return "No projects found.";
  }

  const lines = ["# Projects", ""];

  for (const project of projects) {
    const projectId = project.projectId || "N/A";
    const name = project.projectName || project.name || "N/A";
    const status = project.status || "N/A";
    const repository = project.repository || "N/A";

    lines.push(`## ${name}`);
    lines.push(`- **Project ID**: ${projectId}`);
    lines.push(`- **Repository**: ${repository}`);
    lines.push(`- **Status**: ${status}`);

    if (project.branch) {
      lines.push(`- **Branch**: ${project.branch}`);
    }
    if (project.tag) {
      lines.push(`- **Tag**: ${project.tag}`);
    }

    lines.push("");
  }

  return lines.join("\n");
}

/**
 * Format tables as markdown table
 */
function formatTables(tables: any[]): string {
  if (!Array.isArray(tables) || tables.length === 0) {
    return "No tables found.";
  }

  const lines = [
    "# Tables",
    "",
    "| Name | Type | File |",
    "|------|------|------|",
  ];

  for (const table of tables) {
    const name = table.name || "N/A";
    const type = table.tableType || table.type || "N/A";
    const file = table.file || "N/A";
    lines.push(`| ${name} | ${type} | ${file} |`);
  }

  return lines.join("\n");
}

/**
 * Format deployments as markdown list
 */
function formatDeployments(deployments: any[]): string {
  if (!Array.isArray(deployments) || deployments.length === 0) {
    return "No deployments found.";
  }

  const lines = ["# Deployments", ""];

  for (const deploy of deployments) {
    const name = deploy.deploymentName || deploy.name || "N/A";
    const repository = deploy.repository || "N/A";
    const version = deploy.version || "N/A";
    const status = deploy.status || "N/A";

    lines.push(`## ${name}`);
    lines.push(`- **Repository**: ${repository}`);
    lines.push(`- **Version**: ${version}`);
    lines.push(`- **Status**: ${status}`);
    lines.push("");
  }

  return lines.join("\n");
}

/**
 * Format history/commits as markdown list
 */
function formatHistory(commits: any[]): string {
  if (!Array.isArray(commits) || commits.length === 0) {
    return "No history found.";
  }

  const lines = ["# History", ""];

  for (const commit of commits) {
    const hash = commit.commitHash || commit.hash || "N/A";
    const author = commit.author || "N/A";
    const date = commit.date || commit.timestamp || "N/A";
    const message = commit.message || commit.comment || "N/A";

    lines.push(`## ${hash.substring(0, 8)}`);
    lines.push(`- **Author**: ${author}`);
    lines.push(`- **Date**: ${date}`);
    lines.push(`- **Message**: ${message}`);
    lines.push("");
  }

  return lines.join("\n");
}

/**
 * Format generic data as markdown
 */
function formatGeneric(data: any): string {
  if (Array.isArray(data)) {
    if (data.length === 0) {
      return "No items found.";
    }
    return "```json\n" + safeStringify(data, 2) + "\n```";
  }

  if (typeof data === "object" && data !== null) {
    return "```json\n" + safeStringify(data, 2) + "\n```";
  }

  return String(data);
}

/**
 * Format pagination metadata as markdown
 */
function formatPagination(pagination: PaginationMetadata): string {
  const lines = [
    "---",
    "**Pagination**",
    `- Showing items ${pagination.offset + 1}-${Math.min(pagination.offset + pagination.limit, pagination.total_count || 0)}`,
  ];

  if (pagination.total_count !== undefined) {
    lines.push(`- Total: ${pagination.total_count}`);
  }

  if (pagination.has_more) {
    lines.push(`- More results available (next offset: ${pagination.next_offset})`);
  }

  return lines.join("\n");
}

/**
 * Helper function for client-side pagination
 *
 * @param results - Array of results to paginate
 * @param limit - Maximum items per page
 * @param offset - Starting position
 * @returns Paginated results with metadata
 */
export function paginateResults<T>(
  results: T[],
  limit: number,
  offset: number
): {
  data: T[];
  has_more: boolean;
  next_offset?: number;
  total_count: number;
} {
  const total_count = results.length;
  const data = results.slice(offset, offset + limit);
  const has_more = offset + limit < total_count;
  const next_offset = has_more ? offset + limit : undefined;

  return { data, has_more, next_offset, total_count };
}

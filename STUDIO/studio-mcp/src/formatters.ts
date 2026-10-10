/**
 * Response formatting utilities
 *
 * Handles JSON and Markdown formatting, pagination, and character limit enforcement.
 */

import { RESPONSE_LIMITS } from "./constants.js";
import { safeStringify } from "./utils.js";
import * as Types from "./types.js";

/**
 * Pagination metadata
 */
interface PaginationMetadata {
  limit: number;
  offset: number;
  has_more: boolean;
  next_offset?: number;
  total_count?: number;
}

/**
 * Paginated response wrapper
 */
interface PaginatedResponse<T> {
  [key: string]: unknown;
  data: T;
  pagination?: PaginationMetadata;
  truncated?: boolean;
  truncation_message?: string;
}

const RESPONSE_WRAPPER_FIELDS = new Set([
  "data", "pagination", "truncated", "truncation_message",
]);

/**
 * Format response options
 */
interface FormatOptions {
  /** Pagination metadata */
  pagination?: {
    limit: number;
    offset: number;
    total?: number;
    hasMore?: boolean;
  };
  /** Backend fields returned alongside a collection page (for example list expansions). */
  responseMetadata?: Record<string, unknown>;
  /** Character limit (defaults to RESPONSE_LIMITS.MAX_CHARACTERS) */
  characterLimit?: number;
  /** Data type hint for markdown formatting */
  dataType?: string;
  /** Tool-specific context shown only in Markdown responses. */
  markdownContext?: Record<string, unknown>;
  /** Skip truncation for this response (useful for test results and other large data) */
  skipTruncation?: boolean;
}

/** Return the number of elements represented by one formatted page. */
function pageItemCount(data: unknown): number {
  if (Array.isArray(data)) {
    return data.length;
  }
  if (data && typeof data === "object") {
    const envelope = data as Record<string, unknown>;
    if (typeof envelope.numberOfElements === "number" && Number.isFinite(envelope.numberOfElements)) {
      return Math.max(0, envelope.numberOfElements);
    }
    if (Array.isArray(envelope.content)) {
      return envelope.content.length;
    }
  }
  return 1;
}

/** Align a page cursor with the items that are actually present in the response. */
function paginationForDeliveredItems(
  pagination: PaginationMetadata,
  deliveredCount: number,
  pageTruncated: boolean,
): PaginationMetadata {
  const nextOffset = pagination.offset + deliveredCount;
  const hasMore = pagination.total_count !== undefined
    ? nextOffset < pagination.total_count
    : pageTruncated || pagination.has_more;

  return {
    ...pagination,
    has_more: hasMore,
    next_offset: hasMore ? nextOffset : undefined,
  };
}

/** Keep backend metadata from overriding formatter-owned response fields. */
function withoutWrapperFields(record: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(record).filter(([key]) => !RESPONSE_WRAPPER_FIELDS.has(key)),
  );
}

/** Extract non-wrapper fields that should remain visible in Markdown responses. */
function getResponseMetadata<T>(response: PaginatedResponse<T>): Record<string, unknown> {
  return withoutWrapperFields(response);
}

/** Build a bounded, valid-JSON preview when one response value cannot be sliced structurally. */
function truncateJsonPreview(response: PaginatedResponse<unknown>, charLimit: number): string {
  const serializedData = safeStringify(response.data, 2);
  const metadata: Record<string, unknown> = { ...response };
  delete metadata.data;
  delete metadata.truncated;
  delete metadata.truncation_message;
  const build = (previewLength: number, includeMetadata: boolean): string => safeStringify({
    ...(includeMetadata ? metadata : {}),
    data: {
      preview_format: "json",
      truncated_json_preview: serializedData.slice(0, previewLength),
    },
    truncated: true,
    truncation_message: RESPONSE_LIMITS.TRUNCATION_MESSAGE,
  }, 2);

  // Preserve small pagination/expansion metadata when it fits; otherwise the
  // bounded preview and truncation notice are more important than that metadata.
  const includeMetadata = build(0, true).length <= charLimit;
  let low = 0;
  let high = serializedData.length;
  let best = build(0, includeMetadata);
  while (low <= high) {
    const middle = Math.floor((low + high) / 2);
    const candidate = build(middle, includeMetadata);
    if (candidate.length <= charLimit) {
      best = candidate;
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }

  if (best.length <= charLimit) return best;
  const minimal = safeStringify({ truncated: true }, 2);
  return minimal.length <= charLimit ? minimal : "null";
}

/**
 * Format response data as JSON or Markdown (standard, concise, or detailed)
 *
 * @param data - Data to format
 * @param format - Output format: "json" (default structured format), "markdown" (human-readable full format),
 *                 "markdown_concise" (1-2 paragraph summary), "markdown_detailed" (full + context)
 * @param options - Formatting options
 * @returns Formatted response string
 */
export function formatResponse<T>(
  data: T,
  format: "json" | "markdown" | "markdown_concise" | "markdown_detailed" = "json",
  options?: FormatOptions
): string {
  // Create paginated response structure
  const response: PaginatedResponse<T> = {
    ...withoutWrapperFields(options?.responseMetadata ?? {}),
    data,
  };

  // Add pagination metadata if provided
  if (options && options.pagination) {
    const { limit, offset, total } = options.pagination;
    const count = pageItemCount(data);
    const has_more = options.pagination.hasMore ?? (total !== undefined && offset + count < total);
    response.pagination = {
      limit,
      offset,
      has_more,
      next_offset: has_more ? offset + count : undefined,
      ...(total !== undefined ? { total_count: total } : {}),
    };
  }

  const charLimit = (options && options.characterLimit) || RESPONSE_LIMITS.MAX_CHARACTERS;

  // Convert to string
  let formattedString: string;
  if (format === "json") {
    formattedString = safeStringify(response, 2);
  } else if (
    options?.dataType === "table_dependencies"
    && Array.isArray(data)
    && (format === "markdown" || format === "markdown_detailed")
  ) {
    formattedString = formatTableDependencies(
      data as Types.TableNodeView[],
      options.markdownContext,
      {
        detailed: format === "markdown_detailed",
        characterLimit: options.skipTruncation ? undefined : charLimit,
      },
    );
  } else if (format === "markdown_concise") {
    formattedString = toMarkdownConcise(response, options?.dataType, options?.markdownContext);
  } else if (format === "markdown_detailed") {
    formattedString = toMarkdownDetailed(response, options?.dataType, options?.markdownContext);
  } else {
    // Standard markdown
    formattedString = toMarkdown(response, options?.dataType, options?.markdownContext);
  }

  // Check character limit (skip if skipTruncation is true)
  if (options && options.skipTruncation) {
    return formattedString;
  }

  if (formattedString.length > charLimit) {
    if (format === "json") {
      try {
        const parsedResponse = JSON.parse(formattedString) as PaginatedResponse<T>;
        if (Array.isArray(parsedResponse.data) && parsedResponse.data.length > 0) {
          const ratio = charLimit / formattedString.length;
          let itemCount = Math.max(1, Math.floor(parsedResponse.data.length * ratio * 0.9));
          let result: string;
          do {
            const truncatedData = parsedResponse.data.slice(0, itemCount);
            const truncatedWrapper = {
              ...parsedResponse,
              data: truncatedData,
              ...(parsedResponse.pagination ? {
                pagination: paginationForDeliveredItems(
                  parsedResponse.pagination,
                  truncatedData.length,
                  truncatedData.length < parsedResponse.data.length,
                ),
              } : {}),
              truncated: true,
              truncation_message: RESPONSE_LIMITS.TRUNCATION_MESSAGE,
            };
            result = safeStringify(truncatedWrapper, 2);
            if (result.length <= charLimit) break;
            itemCount = Math.max(1, Math.floor(itemCount * 0.8));
          } while (itemCount > 1);
          if (result.length <= charLimit) {
            return result;
          }

          const previewData = parsedResponse.data.slice(0, 1);
          const previewResponse = {
            ...parsedResponse,
            data: previewData,
            ...(parsedResponse.pagination ? {
              pagination: paginationForDeliveredItems(
                parsedResponse.pagination,
                previewData.length,
                previewData.length < parsedResponse.data.length,
              ),
            } : {}),
          };
          return truncateJsonPreview(previewResponse, charLimit);
        }
        return truncateJsonPreview(parsedResponse, charLimit);
      } catch {
        return safeStringify({
          truncated: true,
          truncation_message: RESPONSE_LIMITS.TRUNCATION_MESSAGE,
          error: "Response too large and could not be truncated properly",
        }, 2);
      }
    } else {
      // For non-JSON formats, use existing behavior (plain text truncation)
      const truncated = formattedString.slice(0, charLimit);
      const truncationNote = `\n\n${RESPONSE_LIMITS.TRUNCATION_MESSAGE}`;
      return truncated + truncationNote;
    }
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
  dataType?: string,
  markdownContext?: Record<string, unknown>,
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
    case "table_dependencies":
      parts.push(formatTableDependencies(data as Types.TableNodeView[], markdownContext));
      break;
    case "deployments":
      parts.push(formatDeployments(data as any));
      break;
    case "history":
      parts.push(formatHistory(data as any));
      break;
    case "test_results":
      parts.push(formatTestResults(data as any));
      break;
    case "test_results_summary":
      parts.push(formatTestResultsSummary(data as any));
      break;
    default:
      // Generic object/array formatting
      parts.push(formatGeneric(data));
  }

  // Add pagination info if present
  if (response.pagination) {
    parts.push(formatPagination(response.pagination, pageItemCount(data)));
  }

  const metadata = getResponseMetadata(response);
  if (Object.keys(metadata).length > 0) {
    parts.push(`## Response Metadata\n\n${formatGeneric(metadata)}`);
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
  dataType?: string,
  markdownContext?: Record<string, unknown>,
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
    const total = response.pagination ? response.pagination.total_count : count;

    if (total === undefined) {
      parts.push(`Showing ${count} ${count === 1 ? 'item' : 'items'} on this page; the total count is unavailable.`);
      if (response.pagination && response.pagination.has_more) {
        parts.push(`Use offset=${response.pagination.next_offset} to retrieve more results.`);
      }
      const metadata = getResponseMetadata(response);
      if (Object.keys(metadata).length > 0) {
        parts.push(`Response metadata: ${safeStringify(metadata)}.`);
      }
      return parts.join(" ");
    }

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
      case "table_dependencies": {
        const nodes = data as Types.TableNodeView[];
        const edges = collectDependencyEdges(nodes);
        const executableNodes = nodes.filter((node) => !isDatatypeNode(node));
        const datatypes = nodes.filter(isDatatypeNode);
        const executableIds = new Set(executableNodes.flatMap((node) => node.id ? [node.id] : []));
        const datatypeIds = new Set(datatypes.flatMap((node) => node.id ? [node.id] : []));
        const executableLinks = edges.filter(
          (edge) => executableIds.has(edge.source) && executableIds.has(edge.target),
        );
        const datatypeLinks = edges.filter(
          (edge) => datatypeIds.has(edge.source) && datatypeIds.has(edge.target),
        );
        const layerSummaries: string[] = [];
        if (executableNodes.length > 0) {
          layerSummaries.push(`${formatCount(executableNodes.length, "executable node")} with ${formatCount(executableLinks.length, "call link")}`);
        }
        if (datatypes.length > 0) {
          layerSummaries.push(`${formatCount(datatypes.length, "datatype/vocabulary node")} with ${formatCount(datatypeLinks.length, "model link")}`);
        }
        parts.push(`Dependency graph contains ${formatCount(total, "node")} and ${formatCount(edges.length, "link")}${layerSummaries.length > 0 ? `: ${layerSummaries.join("; ")}` : ""}.`);

        const executableFanOut = new Map<string, number>();
        for (const edge of executableLinks) {
          executableFanOut.set(edge.source, (executableFanOut.get(edge.source) ?? 0) + 1);
        }
        const fanOutOf = (node: Types.TableNodeView): number => node.id
          ? executableFanOut.get(node.id) ?? 0
          : 0;
        const highestFanOut = executableNodes
          .filter((node) => fanOutOf(node) > 0)
          .sort((left, right) => fanOutOf(right) - fanOutOf(left))
          .slice(0, 3)
          .map((node) => `${node.name ?? node.id ?? "unnamed"} (${formatCount(fanOutOf(node), "dependency", "dependencies")})`);
        if (highestFanOut.length > 0) {
          parts.push(`Highest executable fan-out: ${highestFanOut.join(", ")}.`);
        }

        if (datatypes.length > 0) {
          const fields = datatypes.flatMap((node) => node.fields ?? []);
          const references = fields.filter((field) => field.ref);
          const collections = references.filter((field) => field.collection);
          const vocabularies = datatypes.flatMap((node) => node.vocabulary ? [node.vocabulary] : []);
          const vocabularyValues = vocabularies.reduce((sum, vocabulary) => sum + (vocabulary.valueCount ?? vocabulary.valuesPreview?.length ?? 0), 0);
          const truncatedVocabularies = vocabularies.filter((vocabulary) => vocabulary.truncated).length;
          const inheritance = datatypes.filter((node) => node.extends).length;
          const connectedDatatypeIds = new Set(datatypeLinks.flatMap((edge) => [edge.source, edge.target]));
          const isolated = datatypes.filter((node) => !node.id || !connectedDatatypeIds.has(node.id)).length;
          parts.push(
            `Data model declares ${formatCount(fields.length, "field")}, including ${formatCount(references.length, "typed reference")} (${formatCount(collections.length, "collection")}), with ${formatCount(inheritance, "inheritance relation")}; ${isolated === 0 ? "every data-model node has a model-layer link" : `${formatCount(isolated, "data-model node")} ${isolated === 1 ? "has" : "have"} no links within the datatype layer`}.`,
          );
          if (vocabularies.length > 0) {
            parts.push(
              `It also contains ${formatCount(vocabularies.length, "vocabulary", "vocabularies")} with ${formatCount(vocabularyValues, "declared value")}${truncatedVocabularies > 0 ? `; ${formatCount(truncatedVocabularies, "preview")} ${truncatedVocabularies === 1 ? "is" : "are"} truncated` : "; all value previews are complete"}.`,
            );
          }
        }
        const query = formatDependencyQuery(markdownContext, true);
        if (query) parts.push(query);
        break;
      }
      case "deployments":
        parts.push(`Found ${total} ${total === 1 ? 'deployment' : 'deployments'}${count < total ? ` (showing ${count})` : ''}.`);
        if (count > 0) {
          const names = data.slice(0, 3).map((d: any) => d.name).join(", ");
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
    // Single object summary: its fields, so a concise answer still carries the data
    const fields = data !== null && typeof data === "object" ? conciseFields(data as Record<string, unknown>) : [];
    parts.push(fields.length > 0 ? `${fields.join("; ")}.` : "Retrieved details successfully.");
  }

  // Add pagination note if applicable
  if (response.pagination && response.pagination.has_more) {
    parts.push(`Use offset=${response.pagination.next_offset} to retrieve more results.`);
  }

  const metadata = getResponseMetadata(response);
  if (Object.keys(metadata).length > 0) {
    parts.push(`Response metadata: ${safeStringify(metadata)}.`);
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
  dataType?: string,
  markdownContext?: Record<string, unknown>,
): string {
  const parts: string[] = [];
  const data = response.data;

  if (dataType === "table_dependencies" && Array.isArray(data)) {
    return formatTableDependencies(data as Types.TableNodeView[], markdownContext, { detailed: true });
  }

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
    const total = response.pagination ? response.pagination.total_count : count;
    const title = dataType === "table_dependencies"
      ? "Table Dependency Graph"
      : dataType ? dataType.charAt(0).toUpperCase() + dataType.slice(1) : "Results";
    parts.push(`# ${title}`);
    parts.push(total === undefined
      ? `\n**Summary:** showing ${count} ${count === 1 ? 'item' : 'items'} on this page; total count unavailable`
      : `\n**Summary:** ${total} total ${total === 1 ? 'item' : 'items'}${count < total ? ` (showing ${count} on this page)` : ''}`);

    // Add timestamp
    parts.push(`**Retrieved:** ${new Date().toISOString()}`);
  }

  // Use standard markdown formatting (calls existing formatters)
  const standardMarkdown = toMarkdown(response, dataType, markdownContext);
  parts.push(standardMarkdown);

  // Add additional context based on data type
  if (Array.isArray(data) && data.length > 0) {
    switch (dataType) {
      case "projects": {
        const opened = data.filter((p: any) => p.status === "OPENED").length;
        const closed = data.filter((p: any) => p.status === "CLOSED").length;
        parts.push(`\n---\n**Status Breakdown:** ${opened} opened, ${closed} closed`);
        break;
      }
      case "tables": {
        const typeCount: Record<string, number> = {};
        data.forEach((t: any) => {
          typeCount[t.tableType] = (typeCount[t.tableType] || 0) + 1;
        });
        const breakdown = Object.entries(typeCount).map(([type, count]) => `${type}: ${count}`).join(", ");
        parts.push(`\n---\n**Type Breakdown:** ${breakdown}`);
        break;
      }
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
 * Escape markdown table cell content to prevent breaking table structure
 */
function escapeTableCell(value: string): string {
  if (!value) return "N/A";
  // Replace pipe characters and newlines that would break markdown tables
  return String(value).replace(/\|/g, "\\|").replace(/\n/g, " ").trim();
}

/**
 * Truncate long strings with ellipsis
 */
function truncate(value: string, maxLength: number): string {
  if (!value || value.length <= maxLength) return value || "N/A";
  return value.substring(0, maxLength - 3) + "...";
}

/**
 * Format properties object as a readable string
 */
function formatProperties(properties: any): string {
  if (!properties || typeof properties !== "object") return "N/A";
  const keys = Object.keys(properties);
  if (keys.length === 0) return "N/A";
  // Show property keys, limit to 3 for readability
  const displayKeys = keys.slice(0, 3);
  const result = displayKeys.join(", ");
  return keys.length > 3 ? `${result} (+${keys.length - 3} more)` : result;
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
    "| Name | Type | Kind | Table ID | Signature | Return Type | File | Properties |",
    "|------|------|------|----------|-----------|-------------|------|------------|",
  ];

  for (const table of tables) {
    const name = escapeTableCell(table.name || "N/A");
    const type = escapeTableCell(table.tableType || table.type || "N/A");
    const kind = escapeTableCell(table.kind || "N/A");
    const tableId = escapeTableCell(table.id || table.tableId || "N/A");
    // Truncate signature to 50 chars to prevent table breaking
    const signature = escapeTableCell(truncate(table.signature || "N/A", 50));
    const returnType = escapeTableCell(table.returnType || "N/A");
    const file = escapeTableCell(table.file || "N/A");
    const properties = escapeTableCell(formatProperties(table.properties));
    lines.push(`| ${name} | ${type} | ${kind} | ${tableId} | ${signature} | ${returnType} | ${file} | ${properties} |`);
  }

  return lines.join("\n");
}

/**
 * The fields of an object as `path: value`, for the concise summary of a single object. A nested object adds its
 * fields under a dotted path, three levels deep; an array is counted, not listed.
 */
function conciseFields(value: Record<string, unknown>, prefix = "", depth = 0): string[] {
  return Object.entries(value).flatMap(([key, field]) => {
    const path = prefix + key;
    if (field === null || field === undefined) {
      return [];
    }
    if (Array.isArray(field)) {
      return [`${path}: ${formatCount(field.length, "item")}`];
    }
    if (typeof field === "object") {
      return depth < 3 ? conciseFields(field as Record<string, unknown>, `${path}.`, depth + 1) : [];
    }
    return [`${path}: ${String(field)}`];
  });
}

function formatCount(count: number, singular: string, plural: string = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : plural}`;
}

function dependencyLabel(id: string, nodesById: ReadonlyMap<string, Types.TableNodeView>): string {
  const name = nodesById.get(id)?.name;
  return name ? `${String(name).replace(/\n/g, " ")} (\`${id}\`)` : `\`${id}\``;
}

function dependencyNodesById(nodes: Types.TableNodeView[]): Map<string, Types.TableNodeView> {
  return new Map(nodes.flatMap((node) => node.id ? [[node.id, node] as const] : []));
}

function isDatatypeNode(node: Types.TableNodeView): node is Types.DatatypeNodeView {
  return node.kind === "Datatype";
}

function hasDependencyNodeId<T extends Types.TableNodeView>(node: T): node is T & { id: string } {
  return typeof node.id === "string" && node.id.length > 0;
}

function formatDatatypeField(
  field: Types.DatatypeNodeFieldView,
  nodesById: ReadonlyMap<string, Types.TableNodeView>,
): string {
  const name = String(field.name ?? "unnamed").replace(/\n/g, " ");
  const type = String(field.type ?? "unknown").replace(/\n/g, " ");
  const target = field.ref
    ? ` -> ${dependencyLabel(field.ref, nodesById)}`
    : "";
  return `\`${name}: ${type}\`${target}${field.collection ? " (collection)" : ""}`;
}

interface VocabularyPreviewItem {
  omitted?: number;
  value?: unknown;
}

/** Insert a presentation-only gap between the first and last backend preview values. */
function vocabularyPreviewItems(vocabulary: Types.DatatypeNodeVocabularyView): VocabularyPreviewItem[] {
  const values = vocabulary.valuesPreview ?? [];
  const omitted = Math.max(0, (vocabulary.valueCount ?? values.length) - values.length);
  if (!vocabulary.truncated || omitted === 0) return values.map((value) => ({ value }));

  const head = Math.ceil(values.length / 2);
  return [
    ...values.slice(0, head).map((value) => ({ value })),
    { omitted },
    ...values.slice(head).map((value) => ({ value })),
  ];
}

function vocabularyValueText(value: unknown): string {
  return typeof value === "string" ? value : safeStringify(value);
}

function formatVocabularyPreview(vocabulary: Types.DatatypeNodeVocabularyView): string {
  const items = vocabularyPreviewItems(vocabulary);
  if (items.length === 0) return "None";
  return items.map((item) => item.omitted !== undefined
    ? `+ ${item.omitted} more`
    : `\`${vocabularyValueText(item.value).replace(/`/g, "\\`")}\``
  ).join(", ");
}

function formatDependencyQuery(
  context: Record<string, unknown> | undefined,
  inline: boolean = false,
): string {
  if (!context) return "";
  const values: string[] = [];
  if (context.scope) values.push(`scope ${String(context.scope)}`);
  if (context.tableId) values.push(`root table \`${String(context.tableId)}\``);
  if (context.module) values.push(`module \`${String(context.module)}\``);
  if (context.layer) values.push(`layer ${String(context.layer)}`);
  if (context.direction) values.push(`direction ${String(context.direction)}`);
  if (context.depth !== undefined) values.push(`depth ${String(context.depth)}`);
  if (values.length === 0) return "";
  return inline ? `Query: ${values.join(", ")}.` : `**Query:** ${values.join(", ")}`;
}

interface DependencyMarkdownOptions {
  detailed?: boolean;
  characterLimit?: number;
}

interface DependencyEdge {
  source: string;
  target: string;
}

interface DependencyRelations {
  dependencies: ReadonlyMap<string, string[]>;
  dependents: ReadonlyMap<string, string[]>;
}

function normalizeMermaidText(value: unknown, fallback: string): string {
  const normalized = String(value ?? fallback).replace(/[\r\n\t]+/g, " ").replace(/\s+/g, " ").trim();
  return normalized || fallback;
}

function escapeMermaidLabel(value: unknown, fallback: string): string {
  return normalizeMermaidText(value, fallback)
    .replace(/&/g, "&amp;")
    .replace(/%/g, "&#37;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function escapeMermaidRelationship(value: unknown, fallback: string): string {
  return normalizeMermaidText(value, fallback)
    .replace(/[%:;{}[\]<>|"'`]/g, " ")
    .replace(/\s+/g, " ")
    .trim() || fallback;
}

function mermaidErAttribute(value: unknown, fallback: string, isType: boolean): string {
  const allowed = isType ? /[^A-Za-z0-9_()[\]-]+/g : /[^A-Za-z0-9_-]+/g;
  const normalized = normalizeMermaidText(value, fallback)
    .replace(allowed, "_")
    .replace(/^_+|_+$/g, "");
  return /^[A-Za-z]/.test(normalized) ? normalized : `${fallback}_${normalized || fallback}`;
}

/**
 * Mermaid ER attributes require both a type and a name. A zero-width space is
 * accepted by the ER grammar and keeps the mandatory type column visually
 * empty for vocabulary values, whose shared type is already in the entity
 * header. The omitted-values row uses it for both mandatory columns so only
 * its `+ N more` comment is visible.
 */
const MERMAID_ER_EMPTY_ATTRIBUTE_PART = "\u200B";

function collectDependencyEdges(nodes: Types.TableNodeView[]): DependencyEdge[] {
  const nodeIds = new Set(nodes.flatMap((node) => node.id ? [node.id] : []));
  const edges = new Map<string, DependencyEdge>();
  const add = (source: string | undefined, target: string | undefined): void => {
    if (!source || !target || !nodeIds.has(source) || !nodeIds.has(target)) return;
    edges.set(`${source}\u0000${target}`, { source, target });
  };

  for (const node of nodes) {
    node.dependencies?.forEach((target) => add(node.id, target));
    node.dependents?.forEach((source) => add(source, node.id));
  }
  return [...edges.values()];
}

function dependencyRelations(nodes: Types.TableNodeView[]): DependencyRelations {
  const dependencies = new Map<string, string[]>();
  const dependents = new Map<string, string[]>();
  for (const edge of collectDependencyEdges(nodes)) {
    dependencies.set(edge.source, [...(dependencies.get(edge.source) ?? []), edge.target]);
    dependents.set(edge.target, [...(dependents.get(edge.target) ?? []), edge.source]);
  }
  return { dependencies, dependents };
}

function selectDependencyNodes(nodes: Types.TableNodeView[], count: number): Types.TableNodeView[] {
  if (count >= nodes.length) return nodes;

  const selected = new Set<Types.TableNodeView>();
  const datatype = nodes.find(isDatatypeNode);
  const executable = nodes.find((node) => !isDatatypeNode(node));
  if (datatype && selected.size < count) selected.add(datatype);
  if (executable && selected.size < count) selected.add(executable);
  for (const node of nodes) {
    if (selected.size >= count) break;
    selected.add(node);
  }
  return nodes.filter((node) => selected.has(node));
}

const MERMAID_RESERVED_IDENTIFIERS = new Set([
  "accdescr",
  "acctitle",
  "callback",
  "class",
  "classdef",
  "classdiagram",
  "click",
  "cssclass",
  "direction",
  "end",
  "erdiagram",
  "link",
  "namespace",
  "note",
  "style",
  "title",
]);

/**
 * Keep datatype identifiers human-readable and directly usable as entity names.
 * Do not switch to Mermaid's `id["Display name"]` entity-alias syntax here:
 * embedded Mermaid versions used by some MCP clients render that syntax literally
 * in ER diagrams and create a second, disconnected entity when a relationship
 * references `id`. Sanitize names instead, prefix reserved words, and suffix
 * collisions so the same compatible identifier is used in declarations and links.
 */
function dependencyAliases(nodes: Types.TableNodeView[]): Map<string, string> {
  const aliases = new Map<string, string>();
  const datatypeAliases = new Set<string>();
  let executableIndex = 0;
  for (const node of nodes) {
    if (!node.id) continue;
    if (!isDatatypeNode(node)) {
      aliases.set(node.id, `e${executableIndex++}`);
      continue;
    }

    const normalizedName = normalizeMermaidText(node.name ?? node.id, "Datatype")
      .replace(/[^A-Za-z0-9_]+/g, "_")
      .replace(/^_+|_+$/g, "");
    const safeName = /^[A-Za-z]/.test(normalizedName) && !MERMAID_RESERVED_IDENTIFIERS.has(normalizedName.toLowerCase())
      ? normalizedName
      : `Type_${normalizedName || "Datatype"}`;
    let alias = safeName;
    let suffix = 2;
    while (datatypeAliases.has(alias)) alias = `${safeName}_${suffix++}`;
    datatypeAliases.add(alias);
    aliases.set(node.id, alias);
  }
  return aliases;
}

/**
 * Vocabulary value types belong to the whole declaration, so show them once in
 * the ER entity header rather than repeating them beside every preview value.
 * These are quoted entity names, not Mermaid's incompatible `id["label"]` ER
 * alias syntax described above; the exact same name is used for declarations
 * and relationships so clients cannot create disconnected duplicate entities.
 */
function datatypeErEntityNames(
  nodes: Types.TableNodeView[],
  aliases: ReadonlyMap<string, string>,
): Map<string, string> {
  const names = new Map<string, string>();
  for (const node of nodes) {
    if (!isDatatypeNode(node) || !node.id) continue;
    const alias = aliases.get(node.id);
    if (!alias) continue;
    if (!node.vocabulary) {
      names.set(node.id, alias);
      continue;
    }

    const valueType = mermaidErAttribute(node.vocabulary.valueType, "Value", true);
    names.set(node.id, `"${alias}<${valueType}>"`);
  }
  return names;
}

function formatExecutableDiagram(
  nodes: Types.TableNodeView[],
  allEdges: DependencyEdge[],
  aliases: ReadonlyMap<string, string>,
): string | undefined {
  const executableNodes = nodes.filter(
    (node): node is Types.TableNodeView & { id: string } => !isDatatypeNode(node) && hasDependencyNodeId(node) && aliases.has(node.id),
  );
  if (executableNodes.length === 0) return undefined;

  const executableIds = new Set(executableNodes.map((node) => node.id));
  const lines = ["## Executable call graph", "", "```mermaid", "flowchart LR"];
  for (const node of executableNodes) {
    const alias = aliases.get(node.id);
    if (!alias) continue;
    const name = escapeMermaidLabel(node.name ?? node.id, "Unnamed table");
    const kind = escapeMermaidLabel(node.kind ?? node.tableType, "Executable");
    lines.push(`  ${alias}["${name}<br/>${kind}"]`);
  }
  for (const edge of allEdges) {
    if (!executableIds.has(edge.source) || !executableIds.has(edge.target)) continue;
    lines.push(`  ${aliases.get(edge.source)} --> ${aliases.get(edge.target)}`);
  }

  const dispatchers = executableNodes.filter(
    (node) => node.kind === "Dispatcher" || node.tableType === "Dispatcher",
  );
  const ordinary = executableNodes.filter((node) => !dispatchers.includes(node));
  lines.push("  classDef executable fill:#eef5ff,stroke:#2563eb,color:#172554");
  lines.push("  classDef dispatcher fill:#fff7ed,stroke:#ea580c,color:#431407,stroke-width:2px");
  if (ordinary.length > 0) {
    lines.push(`  class ${ordinary.map((node) => aliases.get(node.id)).join(",")} executable`);
  }
  if (dispatchers.length > 0) {
    lines.push(`  class ${dispatchers.map((node) => aliases.get(node.id)).join(",")} dispatcher`);
  }
  lines.push("```", "", "Arrows point from the caller to the dependency. Dispatcher nodes are highlighted.");
  return lines.join("\n");
}

function formatDatatypeDiagrams(
  nodes: Types.TableNodeView[],
  allEdges: DependencyEdge[],
  aliases: ReadonlyMap<string, string>,
): string | undefined {
  const datatypeNodes = nodes.filter(isDatatypeNode).filter(hasDependencyNodeId).filter((node) => aliases.has(node.id));
  if (datatypeNodes.length === 0) return undefined;

  const datatypeIds = new Set(datatypeNodes.map((node) => node.id));
  const entityNames = datatypeErEntityNames(datatypeNodes, aliases);
  const connectedDatatypeIds = new Set<string>();
  const representedEdges = new Set<string>();
  const relationshipLines: string[] = [];
  const inheritanceLines: string[] = [];
  const lines = ["## Data model", "", "```mermaid", "erDiagram", "  direction LR"];
  for (const node of datatypeNodes) {
    const entityName = entityNames.get(node.id);
    if (!entityName) continue;
    const vocabularyItems = node.vocabulary ? vocabularyPreviewItems(node.vocabulary) : [];
    if ((!node.fields || node.fields.length === 0) && vocabularyItems.length === 0) {
      lines.push(`  ${entityName}`);
      continue;
    }

    const usedMemberNames = new Set<string>();
    lines.push(`  ${entityName} {`);
    for (const field of node.fields ?? []) {
      const type = mermaidErAttribute(field.type, "Type", true);
      const baseName = mermaidErAttribute(field.name, "field", false);
      let name = baseName;
      let suffix = 2;
      while (usedMemberNames.has(name)) name = `${baseName}_${suffix++}`;
      usedMemberNames.add(name);
      lines.push(`    ${type} ${name}`);
    }
    for (const item of vocabularyItems) {
      if (item.omitted !== undefined) {
        lines.push(`    ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} "+ ${item.omitted} more"`);
        continue;
      }

      const baseName = mermaidErAttribute(vocabularyValueText(item.value), "value", false);
      let name = baseName;
      let suffix = 2;
      while (usedMemberNames.has(name)) name = `${baseName}_${suffix++}`;
      usedMemberNames.add(name);
      lines.push(`    ${MERMAID_ER_EMPTY_ATTRIBUTE_PART} ${name}`);
    }
    lines.push("  }");
  }

  for (const node of datatypeNodes) {
    if (node.extends && datatypeIds.has(node.extends)) {
      inheritanceLines.push(`  ${aliases.get(node.extends)} <|-- ${aliases.get(node.id)}`);
      connectedDatatypeIds.add(node.id);
      connectedDatatypeIds.add(node.extends);
      representedEdges.add(`${node.id}\u0000${node.extends}`);
    }
    for (const field of node.fields ?? []) {
      if (!field.ref || !datatypeIds.has(field.ref)) continue;
      const cardinality = field.collection ? "o{" : "o|";
      relationshipLines.push(
        `  ${entityNames.get(node.id)} ||--${cardinality} ${entityNames.get(field.ref)} : ${escapeMermaidRelationship(field.name, "field")}`,
      );
      connectedDatatypeIds.add(node.id);
      connectedDatatypeIds.add(field.ref);
      representedEdges.add(`${node.id}\u0000${field.ref}`);
    }
  }
  for (const edge of allEdges) {
    const edgeKey = `${edge.source}\u0000${edge.target}`;
    if (
      datatypeIds.has(edge.source)
      && datatypeIds.has(edge.target)
      && !representedEdges.has(edgeKey)
    ) {
      relationshipLines.push(`  ${entityNames.get(edge.source)} ||..o{ ${entityNames.get(edge.target)} : depends on`);
      connectedDatatypeIds.add(edge.source);
      connectedDatatypeIds.add(edge.target);
    }
  }
  const isolatedDatatypes = datatypeNodes.filter(
    (node) => !connectedDatatypeIds.has(node.id)
      && (!node.fields || node.fields.length === 0)
      && (!node.vocabulary || vocabularyPreviewItems(node.vocabulary).length === 0),
  );
  lines.push(
    ...relationshipLines,
    "```",
    "",
    "Datatype entities list declared fields. Vocabulary headers use `Name<Type>` and their rows list the bounded preview without repeating the shared type; a `+ N more` row marks values omitted between its first and last entries. Reference links point from one source instance to an optional (`0..1`) or collection (`0..*`) target.",
  );
  if (isolatedDatatypes.length > 0) {
    lines.push(
      "",
      `Unconnected datatypes/vocabularies without displayable members: ${isolatedDatatypes.map((node) => `\`${node.name ?? node.id}\``).join(", ")}.`,
    );
  }
  if (inheritanceLines.length > 0) {
    lines.push(
      "",
      "## Datatype inheritance",
      "",
      "```mermaid",
      "classDiagram",
      ...inheritanceLines,
      "```",
      "",
      "Inheritance uses a hollow triangle from the parent to the child datatype.",
    );
  }
  return lines.join("\n");
}

function formatDependencyOverview(
  allNodes: Types.TableNodeView[],
  visibleNodes: Types.TableNodeView[],
  context?: Record<string, unknown>,
): string {
  const lines = ["# Table Dependency Graph"];
  const query = formatDependencyQuery(context);
  if (query) lines.push("", query);

  const aliases = dependencyAliases(visibleNodes);
  const edges = collectDependencyEdges(allNodes);
  const diagrams = [
    formatExecutableDiagram(visibleNodes, edges, aliases),
    formatDatatypeDiagrams(visibleNodes, edges, aliases),
  ].filter((diagram): diagram is string => Boolean(diagram));
  if (diagrams.length > 0) lines.push("", diagrams.join("\n\n"));
  else lines.push("", "No renderable table nodes found.");

  if (visibleNodes.length < allNodes.length) {
    lines.push("", `Diagram shows ${visibleNodes.length} of ${allNodes.length} nodes; ${allNodes.length - visibleNodes.length} omitted to keep the response within the character limit.`);
  }
  return lines.join("\n");
}

function fitDependencyOverview(
  nodes: Types.TableNodeView[],
  context: Record<string, unknown> | undefined,
  characterLimit: number | undefined,
): string {
  if (characterLimit === undefined) return formatDependencyOverview(nodes, nodes, context);
  let lower = 1;
  let upper = nodes.length;
  let best: string | undefined;
  while (lower <= upper) {
    const count = Math.floor((lower + upper) / 2);
    const candidate = formatDependencyOverview(nodes, selectDependencyNodes(nodes, count), context);
    if (candidate.length <= characterLimit) {
      best = candidate;
      lower = count + 1;
    } else {
      upper = count - 1;
    }
  }
  if (best) return best;

  const fallback = "# Table Dependency Graph\n\nDiagram omitted because the configured character limit is too small.";
  return fallback.slice(0, characterLimit);
}

function formatDependencyNodeDetail(
  node: Types.TableNodeView,
  nodesById: ReadonlyMap<string, Types.TableNodeView>,
  relations: DependencyRelations,
): string {
  const name = String(node.name ?? node.id ?? "Unnamed table").replace(/\n/g, " ");
  const dependencies = (node.id ? relations.dependencies.get(node.id) : node.dependencies)
    ?.map((id) => dependencyLabel(id, nodesById)).join(", ") || "None";
  const dependents = (node.id ? relations.dependents.get(node.id) : node.dependents)
    ?.map((id) => dependencyLabel(id, nodesById)).join(", ") || "None";
  const lines = [`### ${name}`];
  if (node.id) lines.push(`- **Table ID:** \`${node.id}\``);
  if (node.tableType || node.kind) {
    lines.push(`- **Type:** ${node.tableType ?? "N/A"}${node.kind ? ` (${node.kind})` : ""}`);
  }
  if (node.project) lines.push(`- **Project:** ${node.project}`);
  if (node.file || node.pos) lines.push(`- **Location:** ${node.file ?? "N/A"}${node.pos ? ` at ${node.pos}` : ""}`);
  if (isDatatypeNode(node)) {
    if (node.extends) lines.push(`- **Extends:** ${dependencyLabel(node.extends, nodesById)}`);
    if (node.vocabulary) {
      const valueCount = node.vocabulary.valueCount ?? node.vocabulary.valuesPreview?.length ?? 0;
      lines.push(`- **Vocabulary:** ${formatCount(valueCount, `${node.vocabulary.valueType ?? "unknown"} value`)}`);
      lines.push(`- **Values preview:** ${formatVocabularyPreview(node.vocabulary)}`);
    } else if (node.fields && node.fields.length > 0) {
      lines.push("- **Declared fields:**", ...node.fields.map((field) => `  - ${formatDatatypeField(field, nodesById)}`));
    } else {
      lines.push("- **Declared fields:** None");
    }
  } else {
    if (node.signature) lines.push(`- **Signature:** \`${node.signature}\``);
    if (node.returnType) lines.push(`- **Return type:** \`${node.returnType}\``);
    if (node.dimensionProperties && Object.keys(node.dimensionProperties).length > 0) {
      lines.push(`- **Dimension properties:** \`${safeStringify(node.dimensionProperties)}\``);
    }
  }
  lines.push(`- **Depends on:** ${dependencies}`, `- **Used by:** ${dependents}`);
  if (node.properties && Object.keys(node.properties).length > 0) {
    lines.push(`- **Properties:** \`${safeStringify(node.properties)}\``);
  }
  return lines.join("\n");
}

function appendDependencyDetails(
  overview: string,
  nodes: Types.TableNodeView[],
  characterLimit: number | undefined,
): string {
  const nodesById = dependencyNodesById(nodes);
  const relations = dependencyRelations(nodes);
  const details = nodes.map((node) => formatDependencyNodeDetail(node, nodesById, relations));
  const detailsHeader = `${overview}\n\n## Node details\n\nDependencies and reverse uses are derived from the returned graph edges. Executable and datatype layers intentionally do not link to each other.`;
  const complete = `${detailsHeader}\n\n${details.join("\n\n")}`;
  if (characterLimit === undefined || complete.length <= characterLimit) return complete;

  let result = detailsHeader;
  let included = 0;
  for (const detail of details) {
    const omittedAfterAppend = nodes.length - included - 1;
    const omission = omittedAfterAppend > 0
      ? `\n\n${omittedAfterAppend} node detail${omittedAfterAppend === 1 ? "" : "s"} omitted to keep the response within the character limit.`
      : "";
    if (`${result}\n\n${detail}${omission}`.length > characterLimit) break;
    result += `\n\n${detail}`;
    included++;
  }
  const omitted = nodes.length - included;
  if (omitted > 0) {
    const omission = `\n\n${omitted} node detail${omitted === 1 ? "" : "s"} omitted to keep the response within the character limit.`;
    if ((result + omission).length <= characterLimit) result += omission;
  }
  return result.length <= characterLimit ? result : overview;
}

/** Render dependency layers as complete Mermaid diagrams with an optional metadata appendix. */
function formatTableDependencies(
  nodes: Types.TableNodeView[],
  context?: Record<string, unknown>,
  options: DependencyMarkdownOptions = {},
): string {
  if (!Array.isArray(nodes) || nodes.length === 0) {
    return "# Table Dependency Graph\n\nNo tables found.";
  }

  const overviewBudget = options.detailed && options.characterLimit !== undefined
    ? Math.max(Math.floor(options.characterLimit * 0.65), Math.min(options.characterLimit, 800))
    : options.characterLimit;
  const overview = fitDependencyOverview(nodes, context, overviewBudget);
  return options.detailed
    ? appendDependencyDetails(overview, nodes, options.characterLimit)
    : overview;
}

/**
 * Format deployments as markdown list
 */
function formatDeployments(deployments: Types.DeploymentViewModel_Short[]): string {
  if (!Array.isArray(deployments) || deployments.length === 0) {
    return "No deployments found.";
  }

  const lines = ["# Deployments", ""];

  for (const deploy of deployments) {
    lines.push(`## ${deploy.name}`);
    lines.push(`- **Repository**: ${deploy.repository}`);
    for (const item of deploy.items ?? []) {
      const design = item.designRevision ? `, from design revision ${item.designRevision.revision}` : "";
      lines.push(`- **${item.name}**: deployed by ${item.modifiedBy} at ${item.modifiedAt}${design}`);
    }
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
 * Format test results summary (without testCases) as markdown
 */
function formatTestResultsSummary(summary: Types.TestResultsSummary): string {
  if (!summary || typeof summary !== "object") {
    return "No test results summary found.";
  }

  const totalTests = summary.numberOfTests || 0;
  const totalFailures = summary.numberOfFailures || 0;
  const numberOfPassed = summary.numberOfPassed || (totalTests - totalFailures);
  const executionTime = typeof summary.executionTimeMs === 'number' && isFinite(summary.executionTimeMs)
    ? summary.executionTimeMs
    : 0;

  const lines = ["# Test Results Summary", ""];
  lines.push("## Summary");
  lines.push(`- **Total Tests**: ${totalTests}`);
  lines.push(`- **Passed**: ${numberOfPassed}`);
  lines.push(`- **Failed**: ${totalFailures}`);
  lines.push(`- **Execution Time**: ${executionTime.toFixed(2)} ms`);
  lines.push("");

  return lines.join("\n");
}

function formatTestStatus(status: Types.TestUnitExecutionResult["status"]): string {
  switch (status) {
    case "TR_OK":
      return "✅ PASSED";
    case "TR_NEQ":
      return "❌ FAILED";
    case "TR_EXCEPTION":
      return "⚠️ ERROR";
    default:
      return "N/A";
  }
}

function formatTestResultValue(value: unknown): string {
  if (value === undefined) return "N/A";
  const serialized = safeStringify(value) ?? String(value);
  const escaped = escapeTableCell(serialized).replace(/`/g, "\\`");
  return `\`${escaped}\``;
}

function formatTestIdentifier(value: string): string {
  return `\`${String(value).replace(/`/g, "\\`").replace(/\r?\n/g, " ")}\``;
}

function failedTestAssertions(
  testUnit: Types.TestUnitExecutionResult,
): Types.TestAssertionExecutionResult[] {
  return (testUnit.testAssertions ?? []).filter(
    (assertion) => assertion.status === "TR_NEQ" || assertion.status === "TR_EXCEPTION",
  );
}

/**
 * Format test results as a summary followed by test-unit and assertion failures.
 */
function formatTestResults(summary: Types.TestsExecutionSummary & { totalTestsInAllTables?: number }): string {
  if (!summary || typeof summary !== "object") {
    return "No test results found.";
  }

  const testCases = summary.testCases || [];
  const totalTests = summary.numberOfTests || 0;
  const totalFailures = summary.numberOfFailures || 0;
  const executionTime = typeof summary.executionTimeMs === 'number' && isFinite(summary.executionTimeMs)
    ? summary.executionTimeMs
    : 0;

  const lines = ["# Test Results", ""];

  // Summary section
  // Use totalTestsInAllTables if available (counts all tests from all test tables)
  // Otherwise, calculate total: if numberOfTests is 0 but there are failures,
  // it means validation/compilation errors, so total = failures
  // Otherwise, total = numberOfTests (which already includes passed + failed)
  let totalTestsCount: number;
  if (summary.totalTestsInAllTables !== undefined) {
    // Use the total count from all test tables
    totalTestsCount = summary.totalTestsInAllTables;
  } else {
    totalTestsCount = totalTests === 0 && totalFailures > 0
      ? totalFailures
      : totalTests;
  }
  const passedCount = totalTestsCount - totalFailures;

  lines.push("## Summary");
  lines.push(`- **Total Tests**: ${totalTestsCount}`);
  lines.push(`- **Passed**: ${passedCount}`);
  lines.push(`- **Failed**: ${totalFailures}`);
  lines.push(`- **Execution Time**: ${executionTime.toFixed(2)} ms`);
  lines.push("");

  // Test cases table
  if (testCases.length > 0) {
    lines.push("## Test Cases");
    lines.push("");
    lines.push("| Table Name | Total Tests | Passed | Failed | Status | Execution Time (ms) |");
    lines.push("|------------|-------------|--------|--------|--------|---------------------|");

    for (const testCase of testCases) {
      const name = escapeTableCell(testCase.name || "N/A");
      const numberOfTests = testCase.numberOfTests || 0;
      const numberOfFailures = testCase.numberOfFailures || 0;

      // Calculate total tests: if numberOfTests is 0 but there are failures,
      // it means validation/compilation errors, so total = failures
      // Otherwise, total = numberOfTests (which already includes passed + failed)
      const totalTestsForCase = numberOfTests === 0 && numberOfFailures > 0
        ? numberOfFailures
        : numberOfTests;

      const passed = totalTestsForCase - numberOfFailures;
      const status = numberOfFailures === 0 ? "✅ PASSED" : "❌ FAILED";
      const execTime = (typeof testCase.executionTimeMs === 'number' && isFinite(testCase.executionTimeMs)
        ? testCase.executionTimeMs
        : 0).toFixed(2);

      lines.push(`| ${name} | ${totalTestsForCase} | ${passed} | ${numberOfFailures} | ${status} | ${execTime} |`);
    }

    lines.push("", "## Test Units");

    for (const testCase of testCases) {
      const tableName = escapeTableCell(testCase.name || "N/A");
      const tableId = formatTestIdentifier(testCase.tableId);
      const testUnits = testCase.testUnits ?? [];
      const unitResults = testUnits.map((testUnit) => ({
        testUnit,
        failures: failedTestAssertions(testUnit),
      }));

      lines.push("", `### ${tableName}`, `- **Table ID:** ${tableId}`, "");

      if (testUnits.length === 0) {
        lines.push("No test-unit details returned.");
        continue;
      }

      lines.push("| Test | Description | Status | Assertions | Failed Assertions | Execution Time (ms) |");
      lines.push("|------|-------------|--------|------------|-------------------|---------------------|");

      for (const { testUnit, failures } of unitResults) {
        const id = escapeTableCell(testUnit.id || "N/A");
        const description = escapeTableCell(testUnit.description || "N/A");
        const assertions = testUnit.testAssertions ?? [];
        const execTime = typeof testUnit.executionTimeMs === "number" && isFinite(testUnit.executionTimeMs)
          ? testUnit.executionTimeMs.toFixed(2)
          : "N/A";
        lines.push(`| ${id} | ${description} | ${formatTestStatus(testUnit.status)} | ${assertions.length} | ${failures.length} | ${execTime} |`);
      }

      const failedUnits = unitResults.filter(({ testUnit, failures }) =>
        testUnit.status === "TR_NEQ"
        || testUnit.status === "TR_EXCEPTION"
        || failures.length > 0
        || (testUnit.errors?.length ?? 0) > 0
      );

      if (failedUnits.length === 0) continue;

      lines.push("", `#### Failure Details — ${tableName}`);

      for (const { testUnit, failures } of failedUnits) {
        const id = escapeTableCell(testUnit.id || "N/A");
        const description = testUnit.description
          ? ` — ${escapeTableCell(testUnit.description)}`
          : "";

        lines.push("", `##### Test ${id}${description}`);
        lines.push(`- **Status:** ${formatTestStatus(testUnit.status)}`);

        if (testUnit.errors && testUnit.errors.length > 0) {
          lines.push("- **Errors:**");
          for (const error of testUnit.errors) {
            lines.push(`  - ${error.severity}: ${String(error.summary).replace(/\r?\n/g, " ")}`);
          }
        }

        if (failures.length === 0) {
          lines.push("- No assertion-level failure details returned.");
          continue;
        }

        lines.push("", "| Assertion | Expected | Actual | Status |", "|-----------|----------|--------|--------|");
        for (const assertion of failures) {
          const assertionName = escapeTableCell(assertion.description || "N/A");
          lines.push(`| ${assertionName} | ${formatTestResultValue(assertion.expectedValue)} | ${formatTestResultValue(assertion.actualValue)} | ${formatTestStatus(assertion.status)} |`);
        }
      }
    }
  } else {
    lines.push("No test cases found.");
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
function formatPagination(pagination: PaginationMetadata, pageCount: number): string {
  const lines = [
    "---",
    "**Pagination**",
  ];

  // Handle empty results or undefined total_count
  if (pageCount === 0) {
    lines.push("- Showing items 0-0 (No items)");
  } else {
    // Calculate start and end positions
    const start = pagination.offset + 1;
    const end = pagination.offset + pageCount;
    lines.push(`- Showing items ${start}-${end}`);
  }

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
  next_offset?: number | null;
  total_count: number;
} {
  const total_count = results.length;
  const data = results.slice(offset, offset + limit);
  const deliveredEnd = offset + data.length;
  const has_more = deliveredEnd < total_count;
  const next_offset = has_more ? deliveredEnd : null;

  return { data, has_more, next_offset, total_count };
}

/**
 * Resolve pagination once for either a backend-paginated collection or a
 * legacy bare array. Backend pages are passed through untouched; bare arrays
 * are sliced locally for compatibility with older Studio versions.
 */
export function paginateCollection<T>(
  page: Types.CollectionPage<T>,
  limit: number,
  offset: number,
): {
  data: T[];
  pagination: { limit: number; offset: number; total?: number; hasMore: boolean };
} {
  if (!page.serverPaginated) {
    const paginated = paginateResults(page.items, limit, offset);
    return {
      data: paginated.data,
      pagination: {
        limit,
        offset,
        total: paginated.total_count,
        hasMore: paginated.has_more,
      },
    };
  }

  const pageLimit = page.pageSize ?? limit;
  // Current Studio collection endpoints accept a true item offset. Their
  // PageResponse may still expose a derived pageNumber (floor(offset / size)),
  // which cannot reconstruct a non-aligned requested offset. Preserve the
  // request value instead of replacing it with pageNumber * pageSize.
  const pageOffset = offset;
  const hasMore = page.total !== undefined
    ? pageOffset + page.items.length < page.total
    : page.totalPages !== undefined && page.pageNumber !== undefined
      ? page.pageNumber + 1 < page.totalPages
      : page.items.length === pageLimit;

  return {
    data: page.items,
    pagination: {
      limit: pageLimit,
      offset: pageOffset,
      total: page.total,
      hasMore,
    },
  };
}

/**
 * Fixed preamble for the aggregated AGENTS.md document. Explains the ordering and
 * the nearest-wins precedence so a consuming agent can apply the guidance correctly
 * from the document alone.
 */
export const AGENTS_DOCUMENT_NOTE =
  "*Important note about this document*\n" +
  "This is the aggregated content of all AGENTS.md files, ordered from the root folder to the project folder.\n" +
  "In case of conflicting instructions, each later AGENTS.md file takes precedence over the earlier ones. The root file has the lowest priority.";

/**
 * Render a project's applicable AGENTS.md files as a single markdown document.
 *
 * The input is the chain as returned by {@link OpenLClient.getProjectAgentContext},
 * which is ordered nearest-first (project folder first). The document is emitted
 * root-first — repository root at the top (lowest priority), project folder at the
 * bottom (highest priority) — so a reader applies later sections over earlier ones.
 *
 * @param files - The applicable AGENTS.md files, nearest-first.
 * @returns A markdown document, or a short note when the project has none.
 */
export function formatAgentsDocument(files: Types.AgentsFile[]): string {
  if (files.length === 0) {
    return "No AGENTS.md files apply to this project.";
  }
  const rootFirst = [...files].reverse();
  const sections = rootFirst.map((file) => {
    const path = file.path.startsWith("/") ? file.path : `/${file.path}`;
    // trimEnd so a file ending in newlines doesn't widen the gap before the next section.
    return `-----\n## ${path}\n\n${file.content.trimEnd()}`;
  });
  const document = `${AGENTS_DOCUMENT_NOTE}\n\n${sections.join("\n\n")}\n`;

  // Cap the aggregated document like every other response so a large monorepo
  // chain can't blow the client's token budget / response limits.
  const limit = RESPONSE_LIMITS.MAX_CHARACTERS;
  if (document.length > limit) {
    return `${document.slice(0, limit)}\n\n${RESPONSE_LIMITS.TRUNCATION_MESSAGE}`;
  }
  return document;
}

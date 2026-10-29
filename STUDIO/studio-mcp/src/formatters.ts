/**
 * The answer of a tool as JSON or Markdown: the pagination of a list and the character limit of a response.
 * The Markdown of a dependency graph is in `dependency-markdown.ts`.
 */

import { RESPONSE_LIMITS } from "./constants.js";
import type * as Types from "./types.js";
import { formatTableDependencies, summarizeDependencyGraph, type DependencyQuery } from "./dependency-markdown.js";
import { formatCount, largestFitting } from "./text.js";

interface PaginationMetadata {
  limit: number;
  offset: number;
  has_more: boolean;
  next_offset?: number;
  total_count?: number;
}

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

export interface FormatOptions {
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
  markdownContext?: DependencyQuery;
  /** Skip truncation for this response (useful for test results and other large data) */
  skipTruncation?: boolean;
  /** How a response over the character limit is cut; a list is cut by its leading items when this is omitted. */
  truncation?: Truncation;
}

/**
 * How a response over the character limit is cut: by its leading `items`, counted in `unit`. `cut` answers the data
 * with only the first `count` of them, and `next` how to read the ones after them.
 */
export interface Truncation {
  items: number;
  unit: string;
  cut: (count: number) => unknown;
  next: (count: number) => string;
  /** The metadata of a response of the first `count` items: the whole response holds all of them. */
  metadata?: (count: number) => Record<string, unknown>;
}

/** The note on a response that does not hold everything and has no way to read the rest. */
const NARROW_REQUEST = `${RESPONSE_LIMITS.TRUNCATION_MESSAGE} Narrow the request to read the rest.`;

/** A text cut at `limit` characters, followed by the note that it was cut. */
function truncateText(text: string, limit: number): string {
  return `${text.slice(0, limit)}\n\n${NARROW_REQUEST}`;
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
    if (Array.isArray(envelope.testCases)) {
      return envelope.testCases.length;
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

/** Build a bounded, valid-JSON preview when one response value cannot be sliced structurally. */
function truncateJsonPreview(response: PaginatedResponse<unknown>, charLimit: number): string {
  const { data, ...metadata } = response;
  const serializedData = JSON.stringify(data);
  const build = (previewLength: number, includeMetadata: boolean): string => JSON.stringify({
    ...(includeMetadata ? metadata : {}),
    data: {
      preview_format: "json",
      truncated_json_preview: serializedData.slice(0, previewLength),
    },
    truncated: true,
    truncation_message: NARROW_REQUEST,
  });

  // Preserve small pagination/expansion metadata when it fits; otherwise the
  // bounded preview and truncation notice are more important than that metadata.
  const includeMetadata = build(0, true).length <= charLimit;
  // The escaped preview is never shorter than the text it escapes: no longer preview than the limit can fit.
  const longest = Math.min(serializedData.length, charLimit);
  const best = largestFitting(0, longest, (length) => build(length, includeMetadata), charLimit);
  if (best !== undefined) return best;
  const minimal = JSON.stringify({ truncated: true });
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
  format: Types.ResponseFormat = "json",
  options?: FormatOptions
): string {
  const truncation = options?.truncation;
  const response: PaginatedResponse<T> = {
    ...withoutWrapperFields({ ...options?.responseMetadata, ...truncation?.metadata?.(truncation.items) }),
    data,
  };
  if (options?.pagination) {
    response.pagination = paginationOf(data, options.pagination);
  }

  const charLimit = options?.characterLimit || RESPONSE_LIMITS.MAX_CHARACTERS;
  const formattedString = render(response, format, options, charLimit);
  if (options?.skipTruncation || formattedString.length <= charLimit) {
    return formattedString;
  }
  const cut = truncation ?? listTruncation(response);
  const fitted = cut && fitLeadingItems(response, format, options, charLimit, cut);
  if (fitted) {
    return fitted;
  }
  return format === "json" ? truncateJsonPreview(response, charLimit) : truncateText(formattedString, charLimit);
}

/** The cut of a list: its leading items, continued from the next offset of its page, if it is paged. */
function listTruncation(response: PaginatedResponse<unknown>): Truncation | undefined {
  const items = response.data;
  if (!Array.isArray(items) || items.length === 0) {
    return undefined;
  }
  const offset = response.pagination?.offset;
  return {
    items: items.length,
    unit: "items",
    cut: (count) => items.slice(0, count),
    next: (count) => offset === undefined ? "narrow the request to read the rest" : `continue with offset=${offset + count}`,
  };
}

/**
 * A response cut to the character limit by whole items: as many leading items as fit, the pagination of a list
 * telling what was delivered, and a note on how to read the rest. When not even one item fits, JSON is a preview of
 * the first item, and Markdown is undefined.
 */
function fitLeadingItems(
  response: PaginatedResponse<unknown>,
  format: Types.ResponseFormat,
  options: FormatOptions | undefined,
  charLimit: number,
  truncation: Truncation,
): string | undefined {
  const page = (count: number): PaginatedResponse<unknown> => ({
    ...response,
    data: truncation.cut(count),
    ...(response.pagination ? {
      pagination: paginationForDeliveredItems(response.pagination, count, count < truncation.items),
    } : {}),
    ...truncation.metadata?.(count),
  });
  const message = (count: number): string =>
    `${RESPONSE_LIMITS.TRUNCATION_MESSAGE} It holds ${count} of ${truncation.items} ${truncation.unit}; ${truncation.next(count)}.`;
  const fit = format === "json"
    ? (count: number): string => JSON.stringify({ ...page(count), truncated: true, truncation_message: message(count) })
    : (count: number): string => `${render(page(count), format, options, charLimit)}\n\n${message(count)}`;
  // Probing 1, 2, 4… items first keeps every probe near the size of the answer, not of the whole response
  let fits = 1;
  while (fits * 2 < truncation.items && fit(fits * 2).length <= charLimit) {
    fits *= 2;
  }
  return largestFitting(fits, Math.min(fits * 2, truncation.items) - 1, fit, charLimit)
    ?? (format === "json" ? truncateJsonPreview(page(1), charLimit) : undefined);
}

/** The pagination of a page: where it starts, and where the next one does when more follow. */
function paginationOf(data: unknown, pagination: NonNullable<FormatOptions["pagination"]>): PaginationMetadata {
  const { limit, offset, total } = pagination;
  const count = pageItemCount(data);
  const has_more = pagination.hasMore ?? (total !== undefined && offset + count < total);
  return {
    limit,
    offset,
    has_more,
    next_offset: has_more ? offset + count : undefined,
    ...(total !== undefined ? { total_count: total } : {}),
  };
}

/** A response in the requested format, before it is cut to the character limit. */
function render<T>(
  response: PaginatedResponse<T>,
  format: Types.ResponseFormat,
  options: FormatOptions | undefined,
  charLimit: number,
): string {
  // An agent reads JSON as well without indentation, which would take more of its context than the data itself
  if (format === "json") {
    return JSON.stringify(response);
  }
  const graphMarkdown = format === "markdown" || format === "markdown_detailed";
  if (options?.dataType === "table_dependencies" && Array.isArray(response.data) && graphMarkdown) {
    return formatTableDependencies(response.data as Types.TableNodeView[], options.markdownContext, {
      detailed: format === "markdown_detailed",
      characterLimit: options.skipTruncation ? undefined : charLimit,
    });
  }
  if (format === "markdown_concise") {
    return toMarkdownConcise(response, options?.dataType, options?.markdownContext);
  }
  if (format === "markdown_detailed") {
    return toMarkdownDetailed(response, options?.dataType);
  }
  return toMarkdown(response, options?.dataType);
}

/** The Markdown of a response: its data as the view of its `dataType` renders it, its pagination and metadata. */
export function toMarkdown<T>(
  response: PaginatedResponse<T>,
  dataType?: string,
): string {
  const data = response.data;
  const view = dataType ? MARKDOWN_VIEWS.get(dataType) : undefined;
  const parts = [view ? view(data as never) : formatGeneric(data)];

  if (response.pagination) {
    parts.push(formatPagination(response.pagination, pageItemCount(data)));
  }

  const metadata = withoutWrapperFields(response);
  if (Object.keys(metadata).length > 0) {
    parts.push(`## Response Metadata\n\n${formatGeneric(metadata)}`);
  }

  return parts.join("\n\n");
}

/** The number of projects in each status, in the order the statuses first appear: `2 editing, 1 opened`. */
function statusBreakdown(projects: Array<{ status?: string }>): string {
  const byStatus = Map.groupBy(projects, ({ status }) => (status ?? "unknown").toLowerCase().replaceAll("_", " "));
  return Array.from(byStatus, ([name, group]) => `${group.length} ${name}`).join(", ");
}

/** `Found 12 tables (showing 5).` */
function foundText(count: number, total: number, singular: string, plural?: string): string {
  const showing = count < total ? ` (showing ${count})` : "";
  return `Found ${formatCount(total, singular, plural)}${showing}.`;
}

/** The first names of a page: `A, B, C, and 2 more`. */
function firstNames(items: Array<{ name?: string }>): string {
  const more = items.length > 3 ? `, and ${items.length - 3} more` : "";
  return `${items.slice(0, 3).map((item) => item.name).join(", ")}${more}`;
}

/** The features a repository has, by name: `branches, comments`. */
function featureNames(features?: Types.RepositoryFeatures): string {
  return Object.entries(features ?? {}).filter(([, enabled]) => enabled === true).map(([name]) => name).join(", ");
}

/** Every repository of a page by its name, its id and its features: `Design (design: branches, comments)`. */
function repositoryNames(repositories: Types.Repository[]): string {
  return repositories.map(({ name, id, features }) => {
    const names = featureNames(features);
    const listed = names ? `: ${names}` : "";
    return `${name} (${id}${listed})`;
  }).join(", ");
}

/** Every project of a page by its name and its id, which the other tools take: `Bank (p1), Rates (p2)`. */
function projectNames(projects: Array<{ name?: string; projectId?: string }>): string {
  return projects.map(({ name, projectId }) => `${name} (${projectId})`).join(", ");
}

/** The summary of a page of a list whose total is known, by the kind of its items. */
function listSummary(data: unknown[], total: number, dataType?: string, markdownContext?: DependencyQuery): string[] {
  const count = data.length;
  const named = data as Array<{ name?: string; status?: string }>;
  const ifAny = (line: () => string): string[] => (count > 0 ? [line()] : []);
  switch (dataType) {
    case "repositories":
      return [
        foundText(count, total, "repository", "repositories"),
        ...ifAny(() => `Repositories: ${repositoryNames(data as Types.Repository[])}.`),
      ];
    case "projects":
      return [
        foundText(count, total, "project"),
        ...ifAny(() => `${statusBreakdown(named)}. Projects: ${projectNames(named)}.`),
      ];
    case "tables": {
      const types = (): string => [...new Set((data as Types.TableMetadata[]).map((t) => t.tableType))].join(", ");
      return [foundText(count, total, "table"), ...ifAny(() => `Table types: ${types()}.`)];
    }
    case "table_dependencies":
      return summarizeDependencyGraph(data as Types.TableNodeView[], total, markdownContext);
    case "deployments":
      return [foundText(count, total, "deployment"), ...ifAny(() => `Deployments: ${firstNames(named)}.`)];
    default:
      return [foundText(count, total, "item")];
  }
}

/** The summary of a single object: its fields, so a concise answer still carries the data. */
function objectSummary(data: unknown): string {
  const fields = data !== null && typeof data === "object" ? conciseFields(data as Record<string, unknown>) : [];
  return fields.length > 0 ? `${fields.join("; ")}.` : "Retrieved details successfully.";
}

/** The concise Markdown of a response: a summary of one or two paragraphs. */
export function toMarkdownConcise<T>(
  response: PaginatedResponse<T>,
  dataType?: string,
  markdownContext?: DependencyQuery,
): string {
  const data = response.data;
  const parts: string[] = [];
  if (!Array.isArray(data)) {
    parts.push(objectSummary(data));
  } else {
    const total = response.pagination ? response.pagination.total_count : data.length;
    parts.push(...(total === undefined
      ? [`Showing ${formatCount(data.length, "item")} on this page; the total count is unavailable.`]
      : listSummary(data, total, dataType, markdownContext)));
  }

  if (response.pagination?.has_more) {
    parts.push(`Use offset=${response.pagination.next_offset} to retrieve more results.`);
  }

  const metadata = withoutWrapperFields(response);
  if (Object.keys(metadata).length > 0) {
    parts.push(`Response metadata: ${JSON.stringify(metadata)}.`);
  }

  return parts.join(" ");
}

/** `5 total items (showing 2 on this page)`, or the size of the page when the total is not known. */
function summaryText(count: number, total: number | undefined): string {
  if (total === undefined) {
    return `showing ${formatCount(count, "item")} on this page; total count unavailable`;
  }
  const showing = count < total ? ` (showing ${count} on this page)` : "";
  return `${total} total ${total === 1 ? "item" : "items"}${showing}`;
}

/** The breakdown a detailed list ends with: the projects by status, the tables by type. */
function detailedBreakdown(data: unknown[], dataType?: string): string[] {
  switch (dataType) {
    case "projects":
      return [`\n---\n**Status Breakdown:** ${statusBreakdown(data as Array<{ status?: string }>)}`];
    case "tables": {
      const breakdown = Object.entries(Object.groupBy(data as Types.TableMetadata[], (t) => String(t.tableType)))
        .map(([type, tables]) => `${type}: ${tables?.length}`)
        .join(", ");
      return [`\n---\n**Type Breakdown:** ${breakdown}`];
    }
    default:
      return [];
  }
}

/** The detailed Markdown of a response: the standard one with a header, the time it was read, and breakdowns. */
export function toMarkdownDetailed<T>(
  response: PaginatedResponse<T>,
  dataType?: string,
): string {
  const data = response.data;
  if (!Array.isArray(data)) {
    return toMarkdown(response, dataType);
  }
  const total = response.pagination ? response.pagination.total_count : data.length;
  const title = dataType ? dataType.charAt(0).toUpperCase() + dataType.slice(1) : "Results";
  return [
    `# ${title}`,
    `\n**Summary:** ${summaryText(data.length, total)}`,
    `**Retrieved:** ${new Date().toISOString()}`,
    toMarkdown(response, dataType),
    ...(data.length > 0 ? detailedBreakdown(data, dataType) : []),
  ].join("\n");
}

/**
 * Format repositories as markdown table
 */
function formatRepositories(repos: Types.Repository[]): string {
  if (repos.length === 0) {
    return "No repositories found.";
  }

  const lines = [
    "# Repositories",
    "",
    "| Name | ID | Type | Features |",
    "|------|----|------|----------|",
  ];

  for (const repo of repos) {
    const name = escapeTableCell(repo.name);
    const id = escapeTableCell(repo.id);
    const type = escapeTableCell(repo.type);
    lines.push(`| ${name} | ${id} | ${type} | ${featureNames(repo.features)} |`);
  }

  return lines.join("\n");
}

/**
 * Format projects as markdown list
 */
function formatProjects(projects: Array<Types.ProjectViewModel & { projectId?: string }>): string {
  if (projects.length === 0) {
    return "No projects found.";
  }

  const lines = ["# Projects", ""];

  for (const project of projects) {
    const projectId = project.projectId || "N/A";
    const name = project.name || "N/A";
    const status = project.status || "N/A";
    const repository = project.repository || "N/A";

    lines.push(
      `## ${name}`,
      `- **Project ID**: ${projectId}`,
      `- **Repository**: ${repository}`,
      `- **Status**: ${status}`,
      ...(project.branch ? [`- **Branch**: ${project.branch}`] : []),
      "",
    );
  }

  return lines.join("\n");
}

/**
 * Escape markdown table cell content to prevent breaking table structure
 */
function escapeTableCell(value: string | undefined): string {
  if (!value) return "N/A";
  // Replace pipe characters and newlines that would break markdown tables
  return String(value).replaceAll("|", String.raw`\|`).replaceAll("\n", " ").trim();
}

/**
 * Truncate long strings with ellipsis
 */
function truncate(value: string | undefined, maxLength: number): string {
  if (!value || value.length <= maxLength) return value || "N/A";
  return value.substring(0, maxLength - 3) + "...";
}

/**
 * Format properties object as a readable string
 */
function formatProperties(properties: Record<string, unknown> | undefined): string {
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
function formatTables(tables: Types.TableMetadata[]): string {
  if (tables.length === 0) {
    return "No tables found.";
  }

  const lines = [
    "# Tables",
    "",
    "| Name | Type | Kind | Table ID | Signature | Return Type | File | Properties |",
    "|------|------|------|----------|-----------|-------------|------|------------|",
  ];

  for (const table of tables) {
    const name = escapeTableCell(table.name);
    const type = escapeTableCell(table.tableType);
    const kind = escapeTableCell(table.kind);
    const tableId = escapeTableCell(table.id);
    // Truncate signature to 50 chars to prevent table breaking
    const signature = escapeTableCell(truncate(table.signature, 50));
    const returnType = escapeTableCell(table.returnType);
    const file = escapeTableCell(table.file);
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
    return [`${path}: ${String(field as string | number | boolean | bigint)}`];
  });
}

/**
 * Format deployments as markdown list
 */
function formatDeployments(deployments: Types.DeploymentViewModelShort[]): string {
  if (deployments.length === 0) {
    return "No deployments found.";
  }

  const lines = ["# Deployments", ""];

  for (const deploy of deployments) {
    lines.push(
      `## ${deploy.name}`,
      `- **ID**: ${deploy.id}`,
      `- **Repository**: ${deploy.repository}`,
      ...(deploy.items ?? []).map(deployedItemLine),
      "",
    );
  }

  return lines.join("\n");
}

/** The line of a deployed project: who deployed it, when, and from which design revision. */
function deployedItemLine(item: NonNullable<Types.DeploymentViewModelShort["items"]>[number]): string {
  const design = item.designRevision ? `, from design revision ${item.designRevision.revision}` : "";
  return `- **${item.name}**: deployed by ${item.modifiedBy} at ${item.modifiedAt}${design}`;
}

/** Milliseconds with two decimals, or `fallback` for a value that is not a finite number. */
function formatMs(value: unknown, fallback: string): string {
  return typeof value === "number" && Number.isFinite(value) ? value.toFixed(2) : fallback;
}

/** The heading of a test run and its totals: tests, passed, failed and the execution time. */
function testSummaryLines(title: string, summary: Types.TestResultsSummary | Types.TestsExecutionSummary): string[] {
  const totalTests = summary.numberOfTests || 0;
  const totalFailures = summary.numberOfFailures || 0;
  return [
    `# ${title}`,
    "",
    "## Summary",
    `- **Total Tests**: ${totalTests}`,
    `- **Passed**: ${totalTests - totalFailures}`,
    `- **Failed**: ${totalFailures}`,
    `- **Execution Time**: ${formatMs(summary.executionTimeMs, "0.00")} ms`,
    "",
  ];
}

/** Format test results summary (without testCases) as markdown */
function formatTestResultsSummary(summary: Types.TestResultsSummary): string {
  return testSummaryLines("Test Results Summary", summary).join("\n");
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
  const serialized = JSON.stringify(value);
  const escaped = escapeTableCell(serialized).replaceAll("`", "\\`");
  return `\`${escaped}\``;
}

function formatTestIdentifier(value: string): string {
  return `\`${String(value).replaceAll("`", "\\`").replace(/\r?\n/g, " ")}\``;
}

/** Whether a test or an assertion failed: its result differs from the expected one, or it threw. */
function isFailure(status: Types.TestUnitExecutionResult["status"]): boolean {
  return status === "TR_NEQ" || status === "TR_EXCEPTION";
}

function failedTestAssertions(
  testUnit: Types.TestUnitExecutionResult,
): Types.TestAssertionExecutionResult[] {
  return (testUnit.testAssertions ?? []).filter((assertion) => isFailure(assertion.status));
}

/** A test unit and the assertions of it that failed. */
interface UnitResult {
  testUnit: Types.TestUnitExecutionResult;
  failures: Types.TestAssertionExecutionResult[];
}

/** The Markdown of the test units of one test table: their table, then the details of the ones that failed. */
function testUnitLines(testCase: Types.TestCaseExecutionResult): string[] {
  const tableName = escapeTableCell(testCase.name);
  const lines = ["", `### ${tableName}`, `- **Table ID:** ${formatTestIdentifier(testCase.tableId)}`, ""];
  const unitResults = (testCase.testUnits ?? [])
    .map((testUnit) => ({ testUnit, failures: failedTestAssertions(testUnit) }));
  if (unitResults.length === 0) {
    return [...lines, "No test-unit details returned."];
  }

  lines.push(
    "| Test | Description | Status | Assertions | Failed Assertions | Execution Time (ms) |",
    "|------|-------------|--------|------------|-------------------|---------------------|",
    ...unitResults.map(unitRow),
  );

  const failedUnits = unitResults.filter(({ testUnit, failures }) =>
    isFailure(testUnit.status) || failures.length > 0 || (testUnit.errors?.length ?? 0) > 0);
  if (failedUnits.length > 0) {
    lines.push("", `#### Failure Details — ${tableName}`, ...failedUnits.flatMap(failureLines));
  }
  return lines;
}

/** The row of a test unit: its status, how many assertions it has and how many of them failed. */
function unitRow({ testUnit, failures }: UnitResult): string {
  const id = escapeTableCell(testUnit.id);
  const description = escapeTableCell(testUnit.description);
  const assertions = testUnit.testAssertions?.length ?? 0;
  const execTime = formatMs(testUnit.executionTimeMs, "N/A");
  return `| ${id} | ${description} | ${formatTestStatus(testUnit.status)} | ${assertions} | ${failures.length} | ${execTime} |`;
}

/** The Markdown of a failed test unit: its status, its errors and the assertions that failed. */
function failureLines({ testUnit, failures }: UnitResult): string[] {
  const id = escapeTableCell(testUnit.id);
  const description = testUnit.description ? ` — ${escapeTableCell(testUnit.description)}` : "";
  const lines = ["", `##### Test ${id}${description}`, `- **Status:** ${formatTestStatus(testUnit.status)}`];
  if (testUnit.errors && testUnit.errors.length > 0) {
    lines.push("- **Errors:**", ...testUnit.errors.map((error) => `  - ${error.severity}: ${String(error.summary).replace(/\r?\n/g, " ")}`));
  }
  if (failures.length === 0) {
    return [...lines, "- No assertion-level failure details returned."];
  }
  lines.push("", "| Assertion | Expected | Actual | Status |", "|-----------|----------|--------|--------|");
  for (const assertion of failures) {
    const assertionName = escapeTableCell(assertion.description);
    lines.push(`| ${assertionName} | ${formatTestResultValue(assertion.expectedValue)} | ${formatTestResultValue(assertion.actualValue)} | ${formatTestStatus(assertion.status)} |`);
  }
  return lines;
}

/**
 * Format test results as a summary followed by test-unit and assertion failures.
 */
function formatTestResults(summary: Types.TestsExecutionSummary): string {
  const testCases = summary.testCases || [];
  const lines = testSummaryLines("Test Results", summary);

  if (testCases.length === 0) {
    return [...lines, "No test cases found."].join("\n");
  }
  return [
    ...lines,
    "## Test Cases",
    "",
    "| Table Name | Total Tests | Passed | Failed | Status | Execution Time (ms) |",
    "|------------|-------------|--------|--------|--------|---------------------|",
    ...testCases.map(testCaseRow),
    "",
    "## Test Units",
    ...testCases.flatMap(testUnitLines),
  ].join("\n");
}

/** The row of a test table: how many of its tests passed and failed. */
function testCaseRow(testCase: Types.TestCaseExecutionResult): string {
  const name = escapeTableCell(testCase.name);
  const numberOfTests = testCase.numberOfTests || 0;
  const numberOfFailures = testCase.numberOfFailures || 0;
  const status = formatTestStatus(numberOfFailures === 0 ? "TR_OK" : "TR_NEQ");
  const execTime = formatMs(testCase.executionTimeMs, "0.00");
  return `| ${name} | ${numberOfTests} | ${numberOfTests - numberOfFailures} | ${numberOfFailures} | ${status} | ${execTime} |`;
}

/** The Markdown of the lists and the test results a tool names by its `dataType`. */
const MARKDOWN_VIEWS = new Map<string, (data: never) => string>([
  ["repositories", formatRepositories],
  ["projects", formatProjects],
  ["tables", formatTables],
  ["deployments", formatDeployments],
  ["test_results", formatTestResults],
  ["test_results_summary", formatTestResultsSummary],
]);

/**
 * Format generic data as markdown
 */
function formatGeneric(data: unknown): string {
  if (Array.isArray(data) && data.length === 0) {
    return "No items found.";
  }
  if (typeof data === "object" && data !== null) {
    return "```json\n" + JSON.stringify(data, null, 2) + "\n```";
  }
  return String(data);
}

/**
 * Format pagination metadata as markdown
 */
function formatPagination(pagination: PaginationMetadata, pageCount: number): string {
  const shown = pageCount === 0
    ? "- Showing items 0-0 (No items)"
    : `- Showing items ${pagination.offset + 1}-${pagination.offset + pageCount}`;
  return [
    "---",
    "**Pagination**",
    shown,
    ...(pagination.total_count === undefined ? [] : [`- Total: ${pagination.total_count}`]),
    ...(pagination.has_more ? [`- More results available (next offset: ${pagination.next_offset})`] : []),
  ].join("\n");
}

/**
 * Formats the page of a list the server holds whole: at most `limit` items from `offset`, with the pagination of the
 * whole list. A list OpenL Studio pages itself goes through {@link paginateCollection} instead.
 */
export function formatPage<T>(
  items: T[],
  format: Types.ResponseFormat,
  limit: number,
  offset: number,
  options: Omit<FormatOptions, "pagination"> = {},
): string {
  return formatResponse(items.slice(offset, offset + limit), format, {
    ...options,
    pagination: { limit, offset, total: items.length },
  });
}

/**
 * The pagination of a page of OpenL Studio, which pages the list itself: the page is passed through untouched.
 *
 * Studio takes a true item offset. The page number it answers (the offset divided by the size) cannot reconstruct
 * an offset that is not a multiple of the size, so the requested offset is reported instead.
 */
export function paginateCollection<T>(
  page: Types.CollectionPage<T>,
  offset: number,
): {
  data: T[];
  pagination: { limit: number; offset: number; total?: number; hasMore: boolean };
} {
  return {
    data: page.items,
    pagination: {
      limit: page.pageSize,
      offset,
      total: page.total,
      hasMore: hasMoreItems(offset, page.items.length, page.pageSize, page.total),
    },
  };
}

/** Whether more items follow a page of a Studio list: past the total when Studio tells it, else when the page is full. */
export function hasMoreItems(offset: number, delivered: number, pageSize: number, total: number | undefined): boolean {
  return total !== undefined ? offset + delivered < total : delivered >= pageSize;
}

/**
 * Fixed preamble for the aggregated AGENTS.md document. Explains the ordering and
 * the nearest-wins precedence so a consuming agent can apply the guidance correctly
 * from the document alone.
 */
export const AGENTS_DOCUMENT_NOTE =
  "*The AGENTS.md files that apply, from the root folder to the project folder: a later file overrides an earlier one.*";

/**
 * Render a project's applicable AGENTS.md files as a single markdown document.
 *
 * The input is the chain as returned by {@link OpenLClient.getProjectAgentContext},
 * which is ordered nearest-first (project folder first). The document is emitted
 * root-first — repository root at the top (lowest priority), project folder at the
 * bottom (highest priority) — so a reader applies later sections over earlier ones.
 *
 * A document over the size limit of an answer leaves out whole files from the root down and names them, so the
 * guidance of the highest priority stays; only a project file larger than the limit alone is cut.
 *
 * @param files - The applicable AGENTS.md files, nearest-first.
 * @returns A markdown document, or a short note when the project has none.
 */
export function formatAgentsDocument(files: Types.AgentsFile[]): string {
  if (files.length === 0) {
    return "No AGENTS.md files apply to this project.";
  }
  const sections = files.toReversed().map((file) => {
    const path = file.path.startsWith("/") ? file.path : `/${file.path}`;
    // trimEnd so a file ending in newlines doesn't widen the gap before the next section.
    return { path, text: `-----\n## ${path}\n\n${file.content.trimEnd()}` };
  });
  const limit = RESPONSE_LIMITS.MAX_CHARACTERS;
  const documentOf = (from: number): string => {
    const omitted = sections.slice(0, from).map(({ path }) => path);
    const note = omitted.length === 0 ? "" : `\n\n${omittedGuidanceNote(omitted)}`;
    return `${AGENTS_DOCUMENT_NOTE}${note}\n\n${sections.slice(from).map(({ text }) => text).join("\n\n")}\n`;
  };
  // Cap the document like every other response, leaving out the files farthest from the project first: the
  // nearer a file is to the project, the higher its priority
  let from = 0;
  while (from < sections.length - 1 && documentOf(from).length > limit) {
    from++;
  }
  const document = documentOf(from);
  return document.length > limit ? truncateText(document, limit) : document;
}

/** The note on the files of the guidance an answer leaves out to fit the limit, which have the lowest priority. */
function omittedGuidanceNote(paths: string[]): string {
  const files = paths.map((path) => "`" + path + "`").join(", ");
  return `The answer leaves out ${files} to fit its size: the files ` +
    "nearer the project take precedence over them.";
}

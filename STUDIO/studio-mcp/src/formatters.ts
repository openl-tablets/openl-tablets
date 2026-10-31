/**
 * The answer of a tool as JSON, cut to the character limit of a response. The lists and the test results are Markdown
 * tables of their own (`markdown-list.ts`), and the Markdown of a dependency graph is in `dependency-markdown.ts`.
 */

import { RESPONSE_LIMITS } from "./constants.js";
import type * as Types from "./types.js";
import { largestFitting } from "./text.js";
import { cutJson } from "./json-cut.js";

interface Response {
  [key: string]: unknown;
  data: unknown;
  truncated?: boolean;
  truncation_message?: string;
}

const RESPONSE_WRAPPER_FIELDS = new Set(["data", "truncated", "truncation_message"]);

export interface FormatOptions {
  /** Fields answered beside the data, such as what a search found. */
  responseMetadata?: Record<string, unknown>;
  /** Character limit (defaults to RESPONSE_LIMITS.MAX_CHARACTERS) */
  characterLimit?: number;
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

/** Keep backend metadata from overriding formatter-owned response fields. */
function withoutWrapperFields(record: Record<string, unknown>): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(record).filter(([key]) => !RESPONSE_WRAPPER_FIELDS.has(key)),
  );
}

/** The cuts an answer names, at most this many: the rest it counts. */
const NAMED_CUTS = 8;

/**
 * A response whose data does not fit even when cut by whole items, cut as JSON of the same structure: its long texts
 * shortened, its largest arrays cut in the middle, and the cuts named, so the agent reads what is left by the same paths.
 */
function cutJsonResponse(response: Response, charLimit: number): string {
  const { data, ...metadata } = response;
  const answer = (value: unknown, cuts: string[]): string => {
    const named = cuts.slice(0, NAMED_CUTS).join("; ");
    const more = cuts.length > NAMED_CUTS ? `; and ${cuts.length - NAMED_CUTS} more` : "";
    const message = `${RESPONSE_LIMITS.TRUNCATION_MESSAGE} Cut: ${named}${more}. Narrow the request to read the rest.`;
    return JSON.stringify({ ...metadata, data: value, truncated: true, truncation_message: message });
  };
  // The metadata and the note take their share of the limit: the data is cut again by what they take beyond it
  let budget = charLimit - answer(null, []).length;
  for (;;) {
    const { value, cuts } = cutJson(data, Math.max(0, budget));
    const text = answer(value, cuts);
    if (text.length <= charLimit || budget <= 0) {
      return text;
    }
    budget -= text.length - charLimit;
  }
}

/**
 * The data as JSON in a `{ data }` envelope, without indentation: an agent reads JSON as well without it, which would
 * take more of its context than the data itself. A response over the character limit is cut by its leading items
 * when it is a list or names its `truncation`, and in its structure otherwise.
 */
export function formatResponse(data: unknown, options?: FormatOptions): string {
  const truncation = options?.truncation;
  const response: Response = {
    ...withoutWrapperFields({ ...options?.responseMetadata }),
    // The metadata of a cut is the formatter's own, such as the pagination of what it holds
    ...truncation?.metadata?.(truncation.items),
    data,
  };
  const charLimit = options?.characterLimit || RESPONSE_LIMITS.MAX_CHARACTERS;
  const whole = JSON.stringify(response);
  if (whole.length <= charLimit) {
    return whole;
  }
  const cut = truncation ?? listTruncation(data);
  return (cut && fitLeadingItems(response, charLimit, cut)) ?? cutJsonResponse(response, charLimit);
}

/** The cut of a list: its leading items. */
function listTruncation(items: unknown): Truncation | undefined {
  if (!Array.isArray(items) || items.length === 0) {
    return undefined;
  }
  return {
    items: items.length,
    unit: "items",
    cut: (count) => items.slice(0, count),
    next: () => "narrow the request to read the rest",
  };
}

/**
 * A response cut to the character limit by whole items: as many leading items as fit and a note on how to read the
 * rest, or a cut of the first item in its structure when not even it fits.
 */
function fitLeadingItems(response: Response, charLimit: number, truncation: Truncation): string {
  const page = (count: number): Response => ({ ...response, data: truncation.cut(count), ...truncation.metadata?.(count) });
  const message = (count: number): string =>
    `${RESPONSE_LIMITS.TRUNCATION_MESSAGE} It holds ${count} of ${truncation.items} ${truncation.unit}; ${truncation.next(count)}.`;
  const fit = (count: number): string => JSON.stringify({ ...page(count), truncated: true, truncation_message: message(count) });
  // Probing 1, 2, 4… items first keeps every probe near the size of the answer, not of the whole response
  let fits = 1;
  while (fits * 2 < truncation.items && fit(fits * 2).length <= charLimit) {
    fits *= 2;
  }
  return largestFitting(fits, Math.min(fits * 2, truncation.items) - 1, fit, charLimit) ?? cutJsonResponse(page(1), charLimit);
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
 * The document is never cut: the guidance is what the agent must follow, and no argument could read a part of it left out.
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
    return `-----\n## ${path}\n\n${file.content.trimEnd()}`;
  });
  return `${AGENTS_DOCUMENT_NOTE}\n\n${sections.join("\n\n")}\n`;
}

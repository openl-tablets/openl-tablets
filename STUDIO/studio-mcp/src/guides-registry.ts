/**
 * Bundled OpenL reference guides and specifications.
 *
 * The guides/ directory at the package root is a BUILD ARTIFACT, never committed:
 * `npm run build` (via dist/build-guides.js) reads the markdown documentation of
 * this repository and writes an index.json plus one body file per document (see
 * {@link file://./build-guides.ts}).
 *
 * This module is the runtime reader of that bundle, backing the progressive-
 * disclosure tools: `openl_list_guides` serves index metadata only, and
 * `openl_get_guides` reads just the requested bodies. The index is parsed once
 * per directory and cached for the process lifetime — the bundle is immutable
 * at runtime, and keying the cache by directory lets tests point
 * OPENL_MCP_GUIDES_DIR at fixture bundles without any cache reset.
 */

import { readFileSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

/** Document kind: 'specification' (config-file/project-layout specs from Docs/ref) or 'guide' (reference-guide chapters). */
export type GuideType = "specification" | "guide";

/**
 * One bundled document's metadata — everything `openl_list_guides` returns.
 * Field names are snake_case because this is the schema of the index.json
 * build artifact, not an internal API shape.
 */
export interface GuideEntry {
  /**
   * Stable, human-readable id — the contract used by `openl_get_guides` and by
   * AGENTS.md files to reference a document (e.g. 'spec/rules.xml',
   * 'guide/introduction/basic-concepts'). Derived from the source path with the
   * numeric ordering prefixes stripped, so re-numbering chapters upstream does
   * not break ids.
   */
  id: string;
  type: GuideType;
  /** The document's first markdown header — doubles as its description. */
  title: string;
  /** Body file path relative to the bundle directory. */
  path: string;
  /** Path of the source document inside the openl-tablets repository. */
  source_path: string;
  /** Body size in bytes — lets an agent budget openl_get_guides calls. */
  size_bytes: number;
}

/** Shape of the bundle's index.json manifest. */
export interface GuidesIndex {
  schema_version: number;
  /** GitHub repository the documents come from (owner/name). */
  source_repo: string;
  /** Commit the documents were built from — the commit of this OpenL Studio build. */
  source_ref: string;
  /** ISO-8601 timestamp of when the bundle was generated. */
  generated_at: string;
  guides: GuideEntry[];
}

const __dirname = dirname(fileURLToPath(import.meta.url));

/**
 * Directory holding the guides bundle. Defaults to `<package root>/guides`
 * (sibling of dist/, same layout as prompts/); OPENL_MCP_GUIDES_DIR overrides
 * it — used by tests to load fixture bundles and available to ops to serve an
 * externally built bundle.
 */
export function resolveGuidesDir(): string {
  return process.env.OPENL_MCP_GUIDES_DIR || join(__dirname, "..", "guides");
}

/** Parsed index.json per bundle directory (the bundle is immutable at runtime). */
const indexCache = new Map<string, GuidesIndex>();

/** Actionable remediation appended to every "bundle unusable" error. */
const REBUILD_HINT =
  "The bundle is created at build time — run 'npm run build' (it reads the docs of the " +
  "openl-tablets repository), or point OPENL_MCP_GUIDES_DIR at an existing bundle.";

/**
 * Load and cache the bundle's index.json.
 *
 * @param dir - Bundle directory; defaults to {@link resolveGuidesDir}.
 * @returns The parsed index.
 * @throws Error with a rebuild hint when the bundle is missing or malformed.
 */
export function loadGuidesIndex(dir: string = resolveGuidesDir()): GuidesIndex {
  const cached = indexCache.get(dir);
  if (cached) {
    return cached;
  }

  let raw: string;
  try {
    raw = readFileSync(join(dir, "index.json"), "utf-8");
  } catch {
    throw new Error(`The OpenL guides bundle was not found at '${dir}'. ${REBUILD_HINT}`);
  }

  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error(`The OpenL guides index at '${dir}' is not valid JSON. ${REBUILD_HINT}`);
  }

  const index = parsed as GuidesIndex;
  const entriesValid =
    Array.isArray(index.guides) &&
    index.guides.every(
      (g) =>
        typeof g?.id === "string" &&
        typeof g?.title === "string" &&
        typeof g?.path === "string" &&
        (g?.type === "specification" || g?.type === "guide"),
    );
  if (!entriesValid) {
    throw new Error(`The OpenL guides index at '${dir}' has an unexpected shape. ${REBUILD_HINT}`);
  }

  indexCache.set(dir, index);
  return index;
}

/** Filters accepted by {@link listGuides} (all optional; omitted = match all). */
export interface ListGuidesFilter {
  type?: GuideType;
  /** Case-insensitive substring matched against each entry's id and title. */
  search?: string;
}

/**
 * List bundled documents' metadata (never bodies), optionally filtered.
 * Order is the index order: specifications first, then guides in the source
 * documentation's reading order.
 */
export function listGuides(filter: ListGuidesFilter = {}, dir?: string): GuideEntry[] {
  const { guides } = loadGuidesIndex(dir);
  const needle = filter.search?.toLowerCase();
  return guides.filter(
    (g) =>
      (!filter.type || g.type === filter.type) &&
      (!needle || g.id.toLowerCase().includes(needle) || g.title.toLowerCase().includes(needle)),
  );
}

/** One resolved document: its index entry plus the full markdown body. */
export interface GuideBody {
  entry: GuideEntry;
  body: string;
}

/**
 * Read the full markdown bodies for the requested ids.
 *
 * Ids are de-duplicated preserving first occurrence. Unknown ids do not throw
 * here — they are reported so the caller can compose an actionable error (the
 * tool must never silently return something else instead).
 *
 * @throws Error when the bundle itself is missing, or when an indexed body file
 *         is absent (a corrupt bundle, not a caller mistake).
 */
export function readGuideBodies(
  ids: string[],
  dir?: string,
): { found: GuideBody[]; unknown: string[] } {
  const resolvedDir = dir ?? resolveGuidesDir();
  const index = loadGuidesIndex(resolvedDir);
  const byId = new Map(index.guides.map((g) => [g.id, g]));

  const found: GuideBody[] = [];
  const unknown: string[] = [];
  for (const id of new Set(ids)) {
    const entry = byId.get(id);
    if (!entry) {
      unknown.push(id);
      continue;
    }
    let body: string;
    try {
      body = readFileSync(join(resolvedDir, entry.path), "utf-8");
    } catch {
      throw new Error(
        `The OpenL guides bundle at '${resolvedDir}' is corrupt: '${entry.path}' (guide '${id}') ` +
          `is listed in the index but missing on disk. ${REBUILD_HINT}`,
      );
    }
    found.push({ entry, body });
  }
  return { found, unknown };
}

/**
 * Scan free text (e.g. an aggregated AGENTS.md document) for ids of bundled
 * guides and return the ones it references, in index order. This is an
 * enrichment for `openl_get_project_agent_context` — when the bundle is
 * unavailable it returns [] rather than failing the caller.
 */
export function findKnownGuideIds(text: string, dir?: string): string[] {
  let index: GuidesIndex;
  try {
    index = loadGuidesIndex(dir);
  } catch {
    return [];
  }
  return index.guides.filter((g) => text.includes(g.id)).map((g) => g.id);
}

/** Aggregate shape of the bundle, rendered by `openl_get_started` as workspace orientation. */
export interface GuidesOverview {
  source_repo: string;
  source_ref: string;
  /** All specification entries — few enough to name individually. */
  specifications: GuideEntry[];
  guideCount: number;
  /** Top-level reference-guide sections with their document counts, in reading order. */
  guideSections: Array<{ section: string; count: number }>;
}

/**
 * Summarize the bundle for orientation: every specification, plus the guides
 * grouped by their top-level section (the first id segment after 'guide/'),
 * humanized for display.
 */
export function guidesOverview(dir?: string): GuidesOverview {
  const index = loadGuidesIndex(dir);
  const specifications = index.guides.filter((g) => g.type === "specification");
  const guides = index.guides.filter((g) => g.type === "guide");

  const sectionCounts = new Map<string, number>();
  for (const guide of guides) {
    // id shape: 'guide/<section>/...'; a hypothetical top-level document falls
    // into a 'General' bucket rather than being dropped from the overview.
    const segments = guide.id.split("/");
    const section = segments.length > 2 ? humanizeSlug(segments[1]) : "General";
    sectionCounts.set(section, (sectionCounts.get(section) ?? 0) + 1);
  }

  return {
    source_repo: index.source_repo,
    source_ref: index.source_ref,
    specifications,
    guideCount: guides.length,
    guideSections: Array.from(sectionCounts, ([section, count]) => ({ section, count })),
  };
}

/** Words kept lowercase inside humanized titles (unless leading). */
const SMALL_WORDS = new Set(["a", "an", "and", "as", "at", "by", "for", "from", "in", "of", "on", "or", "the", "to", "with"]);

/** Product-specific casing that plain capitalization would get wrong. */
const SPECIAL_CASE: Record<string, string> = { openl: "OpenL" };

/** Turn a kebab-case id segment into a display title: 'working-with-openl-tables' → 'Working with OpenL Tables'. */
export function humanizeSlug(slug: string): string {
  return slug
    .split("-")
    .map((word, i) => {
      const special = SPECIAL_CASE[word];
      if (special) {
        return special;
      }
      if (i > 0 && SMALL_WORDS.has(word)) {
        return word;
      }
      return word.charAt(0).toUpperCase() + word.slice(1);
    })
    .join(" ");
}

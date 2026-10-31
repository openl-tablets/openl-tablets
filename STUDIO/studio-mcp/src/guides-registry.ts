/**
 * The OpenL documentation the guide tools hand to agents: the specifications of the project files and the chapters of
 * the Reference Guide, which OpenL Studio serves at `/docs` with its user guides.
 *
 * The table of contents of OpenL Studio lists the documents with their titles and sizes: `openl_list_guides` returns
 * that metadata only, and `openl_get_guides` reads the bodies it is asked for, verbatim. Every call reads OpenL Studio,
 * so the documents are always those of the running Studio.
 *
 * A document has a stable id made of its path without the number prefixes of its folders and file, so renumbering the
 * chapters does not change the ids.
 */

import type { OpenLClient } from "./client.js";
import type { GUIDE_TYPES, UserGuideEntry } from "./types.js";

/** Document kind: 'specification' (the project files, from `docs/ref`) or 'guide' (Reference Guide chapters). */
type GuideType = (typeof GUIDE_TYPES)[number];

/** One document: everything `openl_list_guides` returns about it. */
interface GuideEntry {
  /**
   * Stable id — the contract used by `openl_get_guides` and by AGENTS.md files to reference a document (e.g.
   * 'spec/rules.xml', 'guide/introduction/basic-concepts').
   */
  id: string;
  type: GuideType;
  title: string;
  /** The path of the document from the address of OpenL Studio, such as `docs/ref/rules.xml.md`. */
  path: string;
  /** The size of the document in bytes — lets an agent budget openl_get_guides calls. */
  size: number;
}

/** The folder of the user guides that holds the Reference Guide. */
const GUIDE_FOLDER = "reference-guide/";

/** The folders of the user guides the documents come from, with the type and the id prefix of their documents. */
const SOURCES: ReadonlyArray<{ type: GuideType; folder: string; idPrefix: string }> = [
  { type: "specification", folder: "ref/", idPrefix: "spec" },
  { type: "guide", folder: GUIDE_FOLDER, idPrefix: "guide" },
];

/** Strip a numeric ordering prefix from a path segment: '03-basic-concepts' → 'basic-concepts'. */
function stripOrderPrefix(segment: string): string {
  return segment.replace(/^\d+-/, "");
}

/** The id of a document: its path in the folder, without `.md` and number prefixes, after the prefix of its type. */
function guideId(idPrefix: string, relPath: string): string {
  return `${idPrefix}/${relPath.replace(/\.md$/, "").split("/").map(stripOrderPrefix).join("/")}`;
}

/** Whether a page of the table of contents is a document of the folder: any page but the index of a folder. */
function isDocument(file: string, folder: string): boolean {
  return file.startsWith(folder) && !file.endsWith("/index.md");
}

type Page = Required<Pick<UserGuideEntry, "title" | "file">> & Pick<UserGuideEntry, "size">;

/** The pages of the table of contents, in its order. */
function pagesOf(entry: UserGuideEntry): Page[] {
  const own = entry.file === undefined ? [] : [{ title: entry.title, file: entry.file, size: entry.size }];
  return [...own, ...(entry.children ?? []).flatMap(pagesOf)];
}

/** The documents of a table of contents, specifications first, then the guides in its order. */
function documentsOf(contents: UserGuideEntry): GuideEntry[] {
  const pages = pagesOf(contents);
  return SOURCES.flatMap(({ type, folder, idPrefix }) =>
    pages
      .filter((page) => isDocument(page.file, folder))
      .map((page) => ({
        id: guideId(idPrefix, page.file.slice(folder.length)),
        type,
        title: page.title,
        path: `docs/${page.file}`,
        size: page.size ?? 0,
      })),
  );
}

/** Every document OpenL Studio serves. */
async function loadGuides(client: OpenLClient): Promise<GuideEntry[]> {
  return documentsOf(await client.getUserGuideContents());
}

/** Filters accepted by {@link listGuides} (all optional; omitted = match all). */
interface ListGuidesFilter {
  type?: GuideType;
  /** Case-insensitive substring matched against each entry's id and title. */
  search?: string;
}

/** The metadata of the documents (never bodies), optionally filtered. */
export async function listGuides(client: OpenLClient, filter: ListGuidesFilter = {}): Promise<GuideEntry[]> {
  const needle = filter.search?.toLowerCase();
  return (await loadGuides(client)).filter(
    (g) =>
      (!filter.type || g.type === filter.type) &&
      (!needle || g.id.toLowerCase().includes(needle) || g.title.toLowerCase().includes(needle)),
  );
}

/** One resolved document: its entry plus the full markdown body. */
interface GuideBody {
  entry: GuideEntry;
  body: string;
}

/**
 * The full markdown bodies of the requested ids, as OpenL Studio serves them.
 *
 * Ids are de-duplicated preserving first occurrence. Unknown ids do not throw here — they are reported so the caller
 * can compose an actionable error (the tool must never silently return something else instead).
 */
export async function readGuideBodies(
  client: OpenLClient,
  ids: string[],
): Promise<{ found: GuideBody[]; unknown: string[] }> {
  const byId = new Map((await loadGuides(client)).map((g) => [g.id, g]));
  const requested = [...new Set(ids)];
  const found = await Promise.all(
    requested
      .flatMap((id) => byId.get(id) ?? [])
      .map(async (entry) => ({ entry, body: await client.readUserGuide(entry.path) })),
  );
  return { found, unknown: requested.filter((id) => !byId.has(id)) };
}

/**
 * The ids of the documents a free text (e.g. an aggregated AGENTS.md document) references, in index order. This is
 * an enrichment for `openl_get_project_agent_context` — when the documents cannot be read it returns [] rather than
 * failing the caller.
 */
export async function findKnownGuideIds(client: OpenLClient, text: string): Promise<string[]> {
  try {
    return (await loadGuides(client)).filter((g) => text.includes(g.id)).map((g) => g.id);
  } catch {
    return [];
  }
}

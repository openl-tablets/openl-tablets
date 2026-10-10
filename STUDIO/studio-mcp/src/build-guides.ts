/**
 * Build-time generator of the bundled OpenL reference documentation.
 *
 * `npm run build` runs this (as dist/build-guides.js) after tsc. It reads the
 * documentation folders of this repository — the same commit OpenL Studio is
 * built from — and writes the guides/ bundle at the package root: an
 * index.json manifest plus one markdown body per document.
 * {@link file://./guides-registry.ts} reads that bundle at runtime for the
 * openl_list_guides / openl_get_guides tools.
 *
 * The bundle is a build artifact: git-ignored, shipped inside OpenL Studio
 * together with the server. Embedding it at build time keeps the server
 * offline-friendly and the docs version-matched to the product.
 */

import { mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { dirname, join, posix, relative, sep } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import { readBuildMetadata } from "./build-info.js";
import type { GuideEntry, GuidesIndex, GuideType } from "./guides-registry.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

/** The root of this repository: the package lives in STUDIO/studio-mcp. */
const REPOSITORY_ROOT = join(__dirname, "..", "..", "..");

/** The GitHub repository the bundled documents link back to. */
const DOCS_REPOSITORY = "openl-tablets/openl-tablets";

/** Where the documentation lives inside the product repository, per document type. */
const DOC_SOURCES: ReadonlyArray<{ type: GuideType; repoDir: string }> = [
  { type: "specification", repoDir: "Docs/ref" },
  { type: "guide", repoDir: "Docs/user-guides/reference-guide" },
];

/** Markdown files of the documentation folders that are not documents of the bundle. */
const EXCLUDED_FILES: ReadonlySet<string> = new Set(["index.md", "AGENTS.md"]);

/** The GitHub repository (owner/name) and the ref the bundled documents link to. */
export interface DocsOrigin {
  repo: string;
  ref: string;
}

/** One source markdown document handed to {@link buildGuideBundle}. */
export interface GuideSource {
  type: GuideType;
  /** Directory of this source set inside the product repo, e.g. 'Docs/ref'. */
  repoDir: string;
  /** Path of the file relative to repoDir, posix separators. */
  relPath: string;
  content: string;
}

/** Strip a numeric ordering prefix from a path segment: '03-basic-concepts' → 'basic-concepts'. */
export function stripOrderPrefix(segment: string): string {
  return segment.replace(/^\d+-/, "");
}

/**
 * Derive a document's stable id from its source path: drop the .md extension,
 * strip the numeric ordering prefix from every segment, and prefix with the
 * type namespace — 'guide/01-introduction/03-basic-concepts.md' →
 * 'guide/introduction/basic-concepts'. Ordering prefixes are stripped so that
 * re-numbering chapters upstream does not break published ids.
 */
export function deriveGuideId(type: GuideType, relPath: string): string {
  const prefix = type === "specification" ? "spec" : "guide";
  const slug = relPath
    .replace(/\.md$/, "")
    .split("/")
    .map(stripOrderPrefix)
    .join("/");
  return `${prefix}/${slug}`;
}

/**
 * Walk a markdown document's lines with fenced code blocks masked out, so
 * headers and links inside ``` fences (code examples) are never mistaken for
 * document structure. `fn` receives each prose line and returns its
 * replacement; fence lines and fenced content pass through unchanged.
 */
function mapProseLines(markdown: string, fn: (line: string) => string): string {
  let inFence = false;
  return markdown
    .split("\n")
    .map((line) => {
      if (/^\s*(```|~~~)/.test(line)) {
        inFence = !inFence;
        return line;
      }
      return inFence ? line : fn(line);
    })
    .join("\n");
}

/**
 * The document's first markdown (ATX) header outside code fences, with inline
 * code/emphasis markers stripped — per EPBDS-16156 this doubles as the
 * document's description. Undefined when the document has no header.
 */
export function extractFirstHeader(markdown: string): string | undefined {
  let title: string | undefined;
  mapProseLines(markdown, (line) => {
    if (title === undefined) {
      const match = line.match(/^#{1,6}\s+(.+?)\s*#*\s*$/);
      if (match) {
        title = match[1].replace(/[`*]/g, "").trim();
      }
    }
    return line;
  });
  return title;
}

/** Fallback title for a document with no header, from its file name: '02-what-is-openl.md' → 'What is openl'. */
export function titleFromFilename(relPath: string): string {
  const base = stripOrderPrefix(posix.basename(relPath, ".md"));
  const words = base.split("-").join(" ");
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/**
 * Rewrite relative markdown link/image targets to absolute GitHub URLs, so the
 * bundled bodies stay self-contained: the referenced images and neighbouring
 * documents are NOT part of the bundle, and a relative path would dangle.
 * Images point at raw content (renderable/fetchable), other links at the blob
 * page. Absolute URLs, anchors, and mailto:/data: targets are left untouched,
 * as is anything inside fenced code blocks. Targets escaping the repository
 * root are left unchanged (nothing sensible to point at).
 *
 * @param sourceDir - Directory of the document inside the product repo, used
 *                    to resolve relative targets.
 */
export function rewriteRelativeUrls(markdown: string, sourceDir: string, origin: DocsOrigin): string {
  const resolveTarget = (target: string, isImage: boolean): string => {
    if (/^[a-z][a-z0-9+.-]*:/i.test(target) || target.startsWith("#") || target.startsWith("//")) {
      return target;
    }
    const [path, fragment] = splitFragment(target);
    if (path === "") {
      return target;
    }
    const resolved = posix.normalize(posix.join(sourceDir, path));
    if (resolved.startsWith("..")) {
      return target;
    }
    const base = isImage
      ? `https://raw.githubusercontent.com/${origin.repo}/${origin.ref}/`
      : `https://github.com/${origin.repo}/blob/${origin.ref}/`;
    return base + resolved + fragment;
  };

  return mapProseLines(markdown, (line) =>
    line
      // Images first, then links; the lookbehind keeps the link pass off the
      // (already rewritten) image syntax.
      .replace(
        /!\[([^\]]*)\]\(([^)\s]+)(\s+"[^"]*")?\)/g,
        (_m, text: string, target: string, title: string | undefined) =>
          `![${text}](${resolveTarget(target, true)}${title ?? ""})`,
      )
      .replace(
        /(?<!!)\[([^\]]*)\]\(([^)\s]+)(\s+"[^"]*")?\)/g,
        (_m, text: string, target: string, title: string | undefined) =>
          `[${text}](${resolveTarget(target, false)}${title ?? ""})`,
      ),
  );
}

/** Split '#fragment' off a link target; returns ['path', '#fragment' | '']. */
function splitFragment(target: string): [string, string] {
  const hash = target.indexOf("#");
  return hash === -1 ? [target, ""] : [target.slice(0, hash), target.slice(hash)];
}

/**
 * Turn the source documents into the bundle: index entries plus the (URL-
 * rewritten) bodies keyed by id. Entries are ordered specifications first,
 * then guides in the source documentation's reading order (paths sort in
 * reading order thanks to the zero-padded numeric prefixes).
 *
 * @throws Error when two documents derive the same id (e.g. '01-foo.md' and
 *         '02-foo.md' in one folder) — ids are the public contract, so the
 *         build must fail rather than silently drop a document.
 */
export function buildGuideBundle(
  sources: GuideSource[],
  origin: DocsOrigin,
  generatedAt: string,
): { index: GuidesIndex; bodies: Map<string, string> } {
  const ordered = [...sources].sort((a, b) => {
    if (a.type !== b.type) {
      return a.type === "specification" ? -1 : 1;
    }
    return a.relPath < b.relPath ? -1 : a.relPath > b.relPath ? 1 : 0;
  });

  const entries: GuideEntry[] = [];
  const bodies = new Map<string, string>();
  const idSources = new Map<string, string>();

  for (const source of ordered) {
    const id = deriveGuideId(source.type, source.relPath);
    const sourcePath = `${source.repoDir}/${source.relPath}`;
    const conflict = idSources.get(id);
    if (conflict) {
      throw new Error(
        `Guide id collision: '${conflict}' and '${sourcePath}' both derive the id '${id}'. ` +
          `Ids are stable references — rename one of the source files upstream.`,
      );
    }
    idSources.set(id, sourcePath);

    const body = rewriteRelativeUrls(source.content, posix.dirname(sourcePath), origin);
    entries.push({
      id,
      type: source.type,
      title: extractFirstHeader(body) ?? titleFromFilename(source.relPath),
      path: `${id}.md`,
      source_path: sourcePath,
      size_bytes: Buffer.byteLength(body, "utf-8"),
    });
    bodies.set(id, body);
  }

  return {
    index: {
      schema_version: 1,
      source_repo: origin.repo,
      source_ref: origin.ref,
      generated_at: generatedAt,
      guides: entries,
    },
    bodies,
  };
}

/**
 * Where the bundled documents link to: this repository at the commit the build
 * records in build-info.json, so a link never points at a later revision of a
 * document. A build that knows no commit links to the main branch.
 */
function readDocsOrigin(): DocsOrigin {
  return { repo: DOCS_REPOSITORY, ref: readBuildMetadata()?.commit ?? "main" };
}

/**
 * Relative posix paths of every markdown document under `root`, sorted.
 * `index.md` files are tables of contents — openl_list_guides IS the index,
 * so they are excluded rather than bundled as near-duplicate content.
 * `AGENTS.md` files tell how to write the documentation, not how to use
 * OpenL, so they are excluded too.
 */
function listMarkdownFiles(root: string): string[] {
  return readdirSync(root, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith(".md") && !EXCLUDED_FILES.has(entry.name))
    .map((entry) => relative(root, join(entry.parentPath, entry.name)).split(sep).join("/"))
    .sort();
}

function writeBundle(outDir: string, index: GuidesIndex, bodies: Map<string, string>): void {
  rmSync(outDir, { recursive: true, force: true });
  for (const entry of index.guides) {
    const body = bodies.get(entry.id);
    if (body === undefined) {
      throw new Error(`No body built for guide '${entry.id}'.`);
    }
    const file = join(outDir, ...entry.path.split("/"));
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, body, "utf-8");
  }
  writeFileSync(join(outDir, "index.json"), `${JSON.stringify(index, null, 2)}\n`, "utf-8");
}

/** Build-script progress output (stdout is fine here — this never runs inside the MCP server). */
function print(message: string): void {
  process.stdout.write(`${message}\n`);
}

function main(): void {
  const origin = readDocsOrigin();
  const outDir = join(__dirname, "..", "guides");

  const sources: GuideSource[] = [];
  for (const { type, repoDir } of DOC_SOURCES) {
    const root = join(REPOSITORY_ROOT, ...repoDir.split("/"));
    for (const relPath of listMarkdownFiles(root)) {
      sources.push({
        type,
        repoDir,
        relPath,
        content: readFileSync(join(root, ...relPath.split("/")), "utf-8"),
      });
    }
  }

  const { index, bodies } = buildGuideBundle(sources, origin, new Date().toISOString());
  writeBundle(outDir, index, bodies);

  const specCount = index.guides.filter((g) => g.type === "specification").length;
  const totalKb = Math.round(index.guides.reduce((sum, g) => sum + g.size_bytes, 0) / 1024);
  print(
    `Bundled ${specCount} specifications and ${index.guides.length - specCount} guides ` +
      `(${totalKb} KB) into ${outDir}.`,
  );
}

// Run only when invoked directly (node dist/build-guides.js); importing the
// module for its pure functions (tests) must stay side-effect free.
if (process.argv[1] !== undefined && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    main();
  } catch (error) {
    console.error(`build-guides: ${error instanceof Error ? error.message : String(error)}`);
    process.exitCode = 1;
  }
}

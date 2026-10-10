/**
 * Runtime version and build identity of this server.
 *
 * The server ships inside OpenL Studio and reports the Studio version it was
 * built with. The version alone cannot identify a running server: every
 * snapshot built between two releases carries the same version, and a locally
 * built tree may carry uncommitted changes on top of it. Support and QA need to
 * tell those apart to decide whether an environment contains a given fix or hit
 * a regression.
 *
 * So the version and the build metadata captured from git are recorded AT BUILD
 * TIME in `build-info.json` at the package root by
 * {@link file://./generate-build-info.ts} — the same "build artifact at the
 * package root, git-ignored, shipped with the server" arrangement as the guides/
 * bundle. This module is its runtime reader; it derives the version and the
 * build id every diagnostics surface reports (the MCP `serverInfo`, the
 * `openl_get_version` tool, the CLI `--version` line, the HTTP `/health` probe,
 * the stdio startup log).
 *
 * Everything degrades gracefully: a missing or unreadable artifact (a source
 * checkout that was never built, an unusual install layout) yields
 * `source: "unavailable"` and the bare package version, never an error.
 *
 * Nothing here is environment-derived: only the version, the build's own git
 * coordinates, and the Node/OS identity are reported. No configuration, base
 * URL, credential, or file path is exposed.
 */

import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import { SERVER_INFO } from "./constants.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

/** Name of the build-metadata artifact, at the package root (sibling of dist/ and guides/). */
export const BUILD_METADATA_FILENAME = "build-info.json";

/**
 * Schema version of {@link BuildMetadata}. Bumped only on an incompatible shape
 * change; the reader accepts any version and simply ignores fields it does not
 * know, so a newer artifact never breaks an older reader.
 */
export const BUILD_METADATA_SCHEMA_VERSION = 1;

/**
 * Shape of the `build-info.json` artifact.
 *
 * Every git-derived field is optional: the build may run outside a git checkout
 * (or without git installed), and the generator then writes what it could
 * determine rather than failing the build. Field names are camelCase to match
 * the {@link BuildDescription} they feed directly, so the artifact needs no
 * translation layer at runtime.
 */
export interface BuildMetadata {
  schemaVersion: number;
  /** Version of OpenL Studio the server was built with; absent for a build outside Maven. */
  version?: string;
  /** Full 40-character commit sha the build was made from. */
  commit?: string;
  /** ISO-8601 committer date of {@link BuildMetadata.commit}, in UTC. */
  commitDate?: string;
  /** Tag or branch HEAD pointed at during the build; absent for a detached, untagged HEAD. */
  ref?: string;
  /** True when the working tree carried uncommitted tracked changes at build time. */
  dirty?: boolean;
  /** ISO-8601 timestamp of the build itself, in UTC. */
  builtAt?: string;
}

/** Where the build metadata came from — `unavailable` means the artifact was missing or unreadable. */
export type BuildMetadataSource = "build-metadata" | "unavailable";

/** Build identity of the running server, as reported by every diagnostics surface. */
export interface BuildDescription {
  /**
   * The single string to quote in a bug report, e.g. `1.1.0+a1b2c3d`,
   * `1.1.0+a1b2c3d.dirty`, `1.1.0+unknown`, or plain `1.1.0` when no build
   * metadata shipped. Semver build metadata, so it sorts as the version itself.
   */
  id: string;
  source: BuildMetadataSource;
  commit?: string;
  /** First 7 characters of {@link BuildDescription.commit} — what humans quote. */
  commitShort?: string;
  commitDate?: string;
  ref?: string;
  dirty?: boolean;
  builtAt?: string;
}

/** Full diagnostics payload: what this server is, and what it runs on. */
export interface VersionInfo {
  name: string;
  version: string;
  build: BuildDescription;
  runtime: {
    /** Node.js version of the running process, e.g. `v24.4.0`. */
    node: string;
    platform: string;
    arch: string;
  };
}

/**
 * Path of the build-metadata artifact. Defaults to `<package root>/build-info.json`
 * — the relative step resolves from both `dist/build-info.js` (production) and
 * `src/build-info.ts` (ts-jest), each one level below the root, matching how
 * {@link file://./constants.ts} locates package.json.
 * `OPENL_MCP_BUILD_INFO_FILE` overrides it (used by tests).
 */
export function resolveBuildMetadataFile(): string {
  return process.env.OPENL_MCP_BUILD_INFO_FILE || join(__dirname, "..", BUILD_METADATA_FILENAME);
}

/** Read `field` from `record` when it holds a non-empty string, else undefined. */
function stringField(record: Record<string, unknown>, field: string): string | undefined {
  const value = record[field];
  return typeof value === "string" && value.trim() !== "" ? value : undefined;
}

/**
 * Read and validate the build-metadata artifact.
 *
 * Field-by-field validation (rather than a blind cast) keeps a hand-edited or
 * partially written artifact from putting non-strings into a diagnostics
 * response. Any failure — missing file, malformed JSON, wrong root type —
 * returns undefined, because diagnostics must never be the reason a server
 * fails to start.
 *
 * @param file - Artifact path; defaults to {@link resolveBuildMetadataFile}.
 */
export function readBuildMetadata(file: string = resolveBuildMetadataFile()): BuildMetadata | undefined {
  let parsed: unknown;
  try {
    parsed = JSON.parse(readFileSync(file, "utf-8"));
  } catch {
    return undefined;
  }
  if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
    return undefined;
  }

  const record = parsed as Record<string, unknown>;
  const version = stringField(record, "version");
  const commit = stringField(record, "commit");
  const commitDate = stringField(record, "commitDate");
  const ref = stringField(record, "ref");
  const builtAt = stringField(record, "builtAt");

  return {
    schemaVersion: typeof record.schemaVersion === "number"
      ? record.schemaVersion
      : BUILD_METADATA_SCHEMA_VERSION,
    ...(version !== undefined && { version }),
    ...(commit !== undefined && { commit }),
    ...(commitDate !== undefined && { commitDate }),
    ...(ref !== undefined && { ref }),
    ...(typeof record.dirty === "boolean" && { dirty: record.dirty }),
    ...(builtAt !== undefined && { builtAt }),
  };
}

/**
 * Derive the reported build identity from a version and the build metadata.
 *
 * The build id is `<version>+<token>`, where the token identifies the build
 * within that version: the short commit, suffixed `.dirty` when the tree carried
 * uncommitted changes, or `unknown` when the build could not read git. With no
 * metadata at all the id is the bare version — an honest "this build cannot be
 * identified beyond its version" rather than an invented token.
 */
export function describeBuild(version: string, metadata: BuildMetadata | undefined): BuildDescription {
  if (!metadata) {
    return { id: version, source: "unavailable" };
  }

  const commitShort = metadata.commit?.slice(0, 7);
  const token = commitShort ? `${commitShort}${metadata.dirty ? ".dirty" : ""}` : "unknown";

  return {
    id: `${version}+${token}`,
    source: "build-metadata",
    ...(metadata.commit !== undefined && { commit: metadata.commit }),
    ...(commitShort !== undefined && { commitShort }),
    ...(metadata.commitDate !== undefined && { commitDate: metadata.commitDate }),
    ...(metadata.ref !== undefined && { ref: metadata.ref }),
    ...(metadata.dirty !== undefined && { dirty: metadata.dirty }),
    ...(metadata.builtAt !== undefined && { builtAt: metadata.builtAt }),
  };
}

/** Assemble the diagnostics payload from a version, a build description and the current process. */
export function buildVersionInfo(version: string, build: BuildDescription): VersionInfo {
  return {
    name: SERVER_INFO.NAME,
    version,
    build,
    runtime: {
      node: process.version,
      platform: process.platform,
      arch: process.arch,
    },
  };
}

/** Cached payload — the artifact and the process identity are both immutable at runtime. */
let cached: VersionInfo | undefined;

/**
 * The running server's version, build identity, and runtime — read once per
 * process. This is the value every diagnostics surface reports. The version is
 * the OpenL Studio version recorded at build time, else the package version.
 */
export function versionInfo(): VersionInfo {
  if (!cached) {
    const metadata = readBuildMetadata();
    const version = metadata?.version ?? SERVER_INFO.VERSION;
    cached = buildVersionInfo(version, describeBuild(version, metadata));
  }
  return cached;
}

/**
 * One-line human rendering of {@link versionInfo}, used by the CLI `--version`
 * flag and the stdio startup log.
 *
 * The first two whitespace-separated fields stay `<name> <version>` so existing
 * scripts that parse the version out of `--version` keep working; the build
 * details follow in a parenthetical.
 */
export function versionLine(info: VersionInfo = versionInfo()): string {
  const { name, version, build } = info;
  if (build.source === "unavailable") {
    return `${name} ${version}`;
  }

  const details = [
    `build ${build.commitShort ? `${build.commitShort}${build.dirty ? ".dirty" : ""}` : "unknown"}`,
    ...(build.builtAt ? [`built ${build.builtAt}`] : []),
  ];
  return `${name} ${version} (${details.join(", ")})`;
}

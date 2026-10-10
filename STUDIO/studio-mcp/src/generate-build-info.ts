/**
 * Build-time generator for the build-metadata artifact.
 *
 * `npm run build` runs this (as dist/generate-build-info.js) after tsc and
 * writes `build-info.json` at the package root: the OpenL Studio version the
 * server is built for, the git coordinates of the tree the build was made from,
 * and the build timestamp. {@link file://./build-info.ts} reads it at runtime so
 * every diagnostics surface reports the Studio version and identifies the exact
 * running build — snapshot builds of one version are otherwise
 * indistinguishable.
 *
 * Like the guides/ bundle this is a build artifact: git-ignored, shipped inside
 * OpenL Studio. It records only public repository coordinates (commit, its
 * date, the branch or tag, whether the tree was modified) and the version the
 * Maven build passes in `OPENL_VERSION` — never build paths, machine names, or
 * other environment values.
 *
 * Missing git coordinates never fail the build: a checkout without git, or an
 * exported source tree with no history, still gets an artifact carrying the
 * fields that could be determined.
 */

import { execFileSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

import {
  BUILD_METADATA_FILENAME,
  BUILD_METADATA_SCHEMA_VERSION,
  type BuildMetadata,
} from "./build-info.js";

const __dirname = dirname(fileURLToPath(import.meta.url));

/** The package root — parent of both `dist/` (production) and `src/` (ts-node). */
const PACKAGE_ROOT = join(__dirname, "..");

/**
 * Runs one git command in the package directory and returns its trimmed stdout.
 * Throws when git is absent, the directory is not a repository, or the command
 * fails — {@link collectBuildMetadata} treats every such failure as "this fact
 * is unavailable".
 */
export type GitRunner = (args: string[]) => string;

const runGit: GitRunner = (args) =>
  execFileSync("git", args, {
    cwd: PACKAGE_ROOT,
    encoding: "utf-8",
    stdio: ["ignore", "pipe", "ignore"],
  }).trim();

/** Outcome of one git query: the trimmed stdout, or a failure (git absent, not a repository, command error). */
type GitResult = { ok: true; value: string } | { ok: false };

/** Run one git query, turning any failure into `{ ok: false }`. */
function tryGit(git: GitRunner, args: string[]): GitResult {
  try {
    return { ok: true, value: git(args) };
  } catch {
    return { ok: false };
  }
}

/** The value of a successful, non-empty git query; undefined otherwise. */
function gitValue(git: GitRunner, args: string[]): string | undefined {
  const result = tryGit(git, args);
  return result.ok && result.value !== "" ? result.value : undefined;
}

/** Trailing milliseconds of an ISO-8601 stamp — git reports whole seconds, so both recorded timestamps drop them. */
const ISO_MILLISECONDS = /\.\d{3}Z$/;

/**
 * Normalize a git ISO-8601 timestamp to UTC.
 *
 * Git's `%cI` reports the committer's own offset (`2026-08-18T11:04:51+03:00`),
 * which makes two recorded builds awkward to compare by eye. Both timestamps in
 * the artifact are therefore UTC at second precision. Input git cannot have
 * produced (not a date at all) yields undefined rather than a value that merely
 * looks like UTC.
 */
export function toUtcIso(raw: string): string | undefined {
  const time = new Date(raw).getTime();
  if (Number.isNaN(time)) return undefined;
  return new Date(time).toISOString().replace(ISO_MILLISECONDS, "Z");
}

/** Now, in the same UTC second-precision form as the recorded commit date. */
function utcNow(): string {
  return new Date().toISOString().replace(ISO_MILLISECONDS, "Z");
}

/**
 * The human-facing name of the built revision: the tag when HEAD is exactly a
 * release tag (how CI builds a release — from a detached checkout where the
 * branch name is the useless literal `HEAD`), otherwise the branch name.
 */
function resolveRef(git: GitRunner): string | undefined {
  const tag = gitValue(git, ["describe", "--tags", "--exact-match", "HEAD"]);
  if (tag) return tag;
  const branch = gitValue(git, ["rev-parse", "--abbrev-ref", "HEAD"]);
  return branch === "HEAD" ? undefined : branch;
}

/**
 * Collect the build metadata to record. Each git fact is optional and resolved
 * independently, so a partial answer (e.g. a repository with no tags) still
 * yields everything else.
 *
 * Both timestamps are recorded in UTC — the commit date normalized from the
 * committer's offset by {@link toUtcIso}, `builtAt` supplied by the caller.
 *
 * `dirty` means "the tree was not exactly this commit" — any modified or
 * untracked file counts, deliberately erring towards flagging a build as
 * modified. Build artifacts (this file, the guides/ bundle, dist/) are
 * git-ignored, so they never mark a clean checkout as modified. When git cannot
 * report status at all the field is left unset, so "not modified" stays
 * distinguishable from "unknown".
 *
 * @param git - Git command runner (injected in tests).
 * @param builtAt - ISO-8601 build timestamp.
 * @param version - Version of OpenL Studio the server is built for; recorded
 *   when given.
 */
export function collectBuildMetadata(
  git: GitRunner,
  builtAt: string,
  version: string | undefined,
): BuildMetadata {
  const commit = gitValue(git, ["rev-parse", "HEAD"]);
  const localCommitDate = commit ? gitValue(git, ["log", "-1", "--format=%cI", commit]) : undefined;
  const commitDate = localCommitDate ? toUtcIso(localCommitDate) : undefined;
  const ref = commit ? resolveRef(git) : undefined;
  const status = commit ? tryGit(git, ["status", "--porcelain"]) : { ok: false } as GitResult;

  return {
    schemaVersion: BUILD_METADATA_SCHEMA_VERSION,
    ...(version !== undefined && { version }),
    ...(commit !== undefined && { commit }),
    ...(commitDate !== undefined && { commitDate }),
    ...(ref !== undefined && { ref }),
    ...(status.ok && { dirty: status.value !== "" }),
    builtAt,
  };
}

function main(): void {
  const file = join(PACKAGE_ROOT, BUILD_METADATA_FILENAME);
  const metadata = collectBuildMetadata(runGit, utcNow(), process.env.OPENL_VERSION?.trim() || undefined);

  writeFileSync(file, `${JSON.stringify(metadata, null, 2)}\n`, "utf-8");

  const identity = metadata.commit
    ? `${metadata.commit.slice(0, 7)}${metadata.dirty ? " (modified working tree)" : ""}`
    : "unknown revision (git metadata unavailable)";
  process.stdout.write(`Recorded build ${identity} in ${BUILD_METADATA_FILENAME}.\n`);
}

// Run only when invoked directly (node dist/generate-build-info.js); importing
// the module for its pure functions (tests) must stay side-effect free.
if (process.argv[1] !== undefined && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    main();
  } catch (error) {
    // Diagnostics metadata is never worth failing a build over: warn and leave
    // the runtime to report `source: "unavailable"`.
    console.error(
      `generate-build-info: could not write ${BUILD_METADATA_FILENAME}: ` +
        `${error instanceof Error ? error.message : String(error)}`,
    );
  }
}

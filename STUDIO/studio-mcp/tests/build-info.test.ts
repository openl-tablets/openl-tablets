/**
 * Unit tests for the runtime build-identity reader: artifact parsing and
 * validation, build-id derivation across released / modified / metadata-less
 * builds, and the one-line human rendering used by the CLI and startup logs.
 * All tests read fixture artifacts from a temp directory — never the real build
 * artifact, which differs per checkout.
 */

import { afterEach, describe, it, expect } from "@jest/globals";
import { mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import {
  BUILD_METADATA_SCHEMA_VERSION,
  buildVersionInfo,
  describeBuild,
  readBuildMetadata,
  versionInfo,
  versionLine,
  type BuildMetadata,
} from "../src/build-info.js";
import { SERVER_INFO } from "../src/constants.js";

/** Write `content` as a build-metadata artifact in a fresh temp directory and return its path. */
function writeArtifact(content: string): string {
  const dir = mkdtempSync(join(tmpdir(), "openl-build-info-"));
  const file = join(dir, "build-info.json");
  writeFileSync(file, content, "utf-8");
  return file;
}

const releasedBuild: BuildMetadata = {
  schemaVersion: BUILD_METADATA_SCHEMA_VERSION,
  version: "7.0.0",
  commit: "a1b2c3d4e5f60718293a4b5c6d7e8f9012345678",
  commitDate: "2026-08-18T11:04:51+03:00",
  ref: "1.2.0",
  dirty: false,
  builtAt: "2026-08-19T07:30:00Z",
};

describe("readBuildMetadata", () => {
  it("reads every recorded field from the artifact", () => {
    const file = writeArtifact(JSON.stringify(releasedBuild));
    expect(readBuildMetadata(file)).toEqual(releasedBuild);
  });

  it("returns undefined when the artifact is missing, so an unbuilt checkout still starts", () => {
    expect(readBuildMetadata(join(tmpdir(), "no-such-build-info.json"))).toBeUndefined();
  });

  it("returns undefined for malformed JSON instead of throwing", () => {
    expect(readBuildMetadata(writeArtifact("{nope"))).toBeUndefined();
  });

  it("returns undefined when the artifact root is not an object", () => {
    expect(readBuildMetadata(writeArtifact('["a1b2c3d"]'))).toBeUndefined();
  });

  it("drops fields of the wrong type or blank, keeping the usable ones", () => {
    const file = writeArtifact(
      JSON.stringify({ schemaVersion: 1, commit: 42, ref: "   ", dirty: "yes", builtAt: "2026-08-19T07:30:00Z" }),
    );
    expect(readBuildMetadata(file)).toEqual({ schemaVersion: 1, builtAt: "2026-08-19T07:30:00Z" });
  });

  it("assumes the current schema version when the artifact omits it", () => {
    const file = writeArtifact(JSON.stringify({ commit: "a1b2c3d" }));
    expect(readBuildMetadata(file)?.schemaVersion).toBe(BUILD_METADATA_SCHEMA_VERSION);
  });

  it("keeps an unknown newer schema version readable rather than rejecting it", () => {
    const file = writeArtifact(JSON.stringify({ ...releasedBuild, schemaVersion: 99, unknownField: "ignored" }));
    expect(readBuildMetadata(file)).toEqual({ ...releasedBuild, schemaVersion: 99 });
  });
});

describe("describeBuild", () => {
  it("identifies a clean build by version plus short commit", () => {
    const build = describeBuild("1.2.0", releasedBuild);
    expect(build.id).toBe("1.2.0+a1b2c3d");
    expect(build.commitShort).toBe("a1b2c3d");
    expect(build.commit).toBe(releasedBuild.commit);
    expect(build.source).toBe("build-metadata");
  });

  it("marks a build made from a modified working tree", () => {
    expect(describeBuild("1.2.0", { ...releasedBuild, dirty: true }).id).toBe("1.2.0+a1b2c3d.dirty");
  });

  it("reports an unknown revision when the build could not read git", () => {
    const build = describeBuild("1.2.0", { schemaVersion: 1, builtAt: "2026-08-19T07:30:00Z" });
    expect(build.id).toBe("1.2.0+unknown");
    expect(build.source).toBe("build-metadata");
    expect(build.commit).toBeUndefined();
    expect(build.commitShort).toBeUndefined();
  });

  it("falls back to the bare version when no build metadata shipped", () => {
    expect(describeBuild("1.2.0", undefined)).toEqual({ id: "1.2.0", source: "unavailable" });
  });

  it("omits fields the artifact did not record rather than reporting empty values", () => {
    const build = describeBuild("1.2.0", { schemaVersion: 1, commit: releasedBuild.commit });
    expect(Object.keys(build).sort()).toEqual(["commit", "commitShort", "id", "source"]);
  });
});

describe("buildVersionInfo", () => {
  it("reports the running Node.js and platform alongside the version and the build", () => {
    const build = describeBuild("7.0.0", releasedBuild);
    const info = buildVersionInfo("7.0.0", build);
    expect(info.build).toBe(build);
    expect(info.runtime).toEqual({ node: process.version, platform: process.platform, arch: process.arch });
    expect(info.name).toBe(SERVER_INFO.NAME);
    expect(info.version).toBe("7.0.0");
  });
});

describe("versionInfo", () => {
  afterEach(() => {
    delete process.env.OPENL_MCP_BUILD_INFO_FILE;
  });

  it("reports the OpenL Studio version recorded at build time instead of the package version", () => {
    process.env.OPENL_MCP_BUILD_INFO_FILE = writeArtifact(JSON.stringify(releasedBuild));
    const info = versionInfo();
    expect(info.version).toBe("7.0.0");
    expect(info.build.id).toBe("7.0.0+a1b2c3d");
  });
});

describe("versionLine", () => {
  // The line renders the running package's own version, so expectations are
  // built from it rather than hard-coding a number that a release would break.
  const { NAME, VERSION } = SERVER_INFO;
  const info = (metadata: BuildMetadata | undefined) =>
    buildVersionInfo(VERSION, describeBuild(VERSION, metadata));

  it("keeps '<name> <version>' as the first two fields so version parsing keeps working", () => {
    expect(versionLine(info(releasedBuild)).split(/\s+/).slice(0, 2)).toEqual([NAME, VERSION]);
  });

  it("appends the build token and timestamp", () => {
    expect(versionLine(info(releasedBuild))).toBe(
      `${NAME} ${VERSION} (build a1b2c3d, built 2026-08-19T07:30:00Z)`,
    );
  });

  it("flags a modified working tree in the build token", () => {
    expect(versionLine(info({ ...releasedBuild, dirty: true }))).toContain("build a1b2c3d.dirty");
  });

  it("reports an unknown revision when git metadata is missing from the artifact", () => {
    expect(versionLine(info({ schemaVersion: 1, builtAt: "2026-08-19T07:30:00Z" }))).toBe(
      `${NAME} ${VERSION} (build unknown, built 2026-08-19T07:30:00Z)`,
    );
  });

  it("omits the timestamp when the artifact did not record one", () => {
    expect(versionLine(info({ schemaVersion: 1, commit: releasedBuild.commit }))).toBe(
      `${NAME} ${VERSION} (build a1b2c3d)`,
    );
  });

  it("prints name and version alone when no build metadata shipped", () => {
    expect(versionLine(info(undefined))).toBe(`${NAME} ${VERSION}`);
  });
});

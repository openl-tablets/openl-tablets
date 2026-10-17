/**
 * Unit tests for the runtime guides-bundle reader: index loading/caching,
 * metadata filtering, body resolution with unknown-id reporting, free-text id
 * scanning, and the get_started overview aggregation. All tests run against a
 * fixture bundle in a temp directory — never the real build artifact.
 */

import { describe, it, expect, beforeAll } from "@jest/globals";
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import {
  findKnownGuideIds,
  guidesOverview,
  humanizeSlug,
  listGuides,
  loadGuidesIndex,
  readGuideBodies,
} from "../src/guides-registry.js";
import { FIXTURE_IDS, writeGuidesFixture } from "./mocks/guides-bundle-fixture.js";

let bundleDir: string;

beforeAll(() => {
  bundleDir = writeGuidesFixture();
});

describe("loadGuidesIndex", () => {
  it("throws an actionable 'run the build' error when the bundle is missing", () => {
    expect(() => loadGuidesIndex(join(tmpdir(), "no-such-bundle"))).toThrow(/npm run build/);
  });

  it("rejects a bundle whose index is not valid JSON", () => {
    const dir = mkdtempSync(join(tmpdir(), "openl-guides-broken-"));
    writeFileSync(join(dir, "index.json"), "{nope", "utf-8");
    expect(() => loadGuidesIndex(dir)).toThrow(/not valid JSON/);
  });

  it("rejects a bundle whose entries are malformed", () => {
    const dir = mkdtempSync(join(tmpdir(), "openl-guides-shape-"));
    writeFileSync(join(dir, "index.json"), JSON.stringify({ guides: [{ id: 1 }] }), "utf-8");
    expect(() => loadGuidesIndex(dir)).toThrow(/unexpected shape/);
  });

  it("caches the parsed index per directory (the bundle is immutable at runtime)", () => {
    const dir = writeGuidesFixture();
    const first = loadGuidesIndex(dir);
    rmSync(join(dir, "index.json"));
    // Same object back, no re-read of the (now deleted) file.
    expect(loadGuidesIndex(dir)).toBe(first);
  });
});

describe("listGuides", () => {
  it("returns every entry unfiltered, in index order", () => {
    expect(listGuides({}, bundleDir).map((g) => g.id)).toEqual(Object.values(FIXTURE_IDS));
  });

  it("filters by type", () => {
    const specs = listGuides({ type: "specification" }, bundleDir);
    expect(specs.map((g) => g.id)).toEqual([FIXTURE_IDS.rulesXml]);
  });

  it("matches 'search' case-insensitively against id and title", () => {
    // 'SMART' hits the smart-rules id/title only.
    expect(listGuides({ search: "SMART" }, bundleDir).map((g) => g.id)).toEqual([
      FIXTURE_IDS.smartRules,
    ]);
    // 'descriptor' appears only in the rules.xml TITLE (not its id).
    expect(listGuides({ search: "descriptor" }, bundleDir).map((g) => g.id)).toEqual([
      FIXTURE_IDS.rulesXml,
    ]);
    expect(listGuides({ search: "no-such-text" }, bundleDir)).toEqual([]);
  });

  it("combines type and search filters", () => {
    expect(listGuides({ type: "guide", search: "rules" }, bundleDir).map((g) => g.id)).toEqual([
      FIXTURE_IDS.smartRules,
    ]);
  });
});

describe("readGuideBodies", () => {
  it("returns full bodies for known ids and reports unknown ids without throwing", () => {
    const { found, unknown } = readGuideBodies(
      [FIXTURE_IDS.basicConcepts, "spec/does-not-exist"],
      bundleDir,
    );
    expect(found).toHaveLength(1);
    expect(found[0].entry.id).toBe(FIXTURE_IDS.basicConcepts);
    expect(found[0].body).toContain("# Basic Concepts");
    expect(unknown).toEqual(["spec/does-not-exist"]);
  });

  it("de-duplicates requested ids preserving first occurrence", () => {
    const { found } = readGuideBodies(
      [FIXTURE_IDS.rulesXml, FIXTURE_IDS.rulesXml],
      bundleDir,
    );
    expect(found.map((f) => f.entry.id)).toEqual([FIXTURE_IDS.rulesXml]);
  });

  it("treats an indexed-but-missing body file as a corrupt bundle", () => {
    const dir = writeGuidesFixture();
    rmSync(join(dir, "spec", "rules.xml.md"));
    expect(() => readGuideBodies([FIXTURE_IDS.rulesXml], dir)).toThrow(/corrupt/);
  });
});

describe("findKnownGuideIds", () => {
  it("returns the bundled ids referenced in the text, in index order", () => {
    const text =
      `Check ${FIXTURE_IDS.smartRules} first, then ${FIXTURE_IDS.rulesXml}. ` +
      `Ignore spec/unknown-thing.`;
    expect(findKnownGuideIds(text, bundleDir)).toEqual([
      FIXTURE_IDS.rulesXml,
      FIXTURE_IDS.smartRules,
    ]);
  });

  it("returns [] instead of failing when the bundle is unavailable (enrichment only)", () => {
    expect(findKnownGuideIds("mentions spec/rules.xml", join(tmpdir(), "no-bundle-here"))).toEqual([]);
  });
});

describe("guidesOverview", () => {
  it("lists every specification and groups guides by humanized top-level section", () => {
    const overview = guidesOverview(bundleDir);
    expect(overview.source_ref).toBe("fixture-ref");
    expect(overview.specifications.map((s) => s.id)).toEqual([FIXTURE_IDS.rulesXml]);
    expect(overview.guideCount).toBe(2);
    expect(overview.guideSections).toEqual([
      { section: "Introduction", count: 1 },
      { section: "Working with OpenL Tables", count: 1 },
    ]);
  });
});

describe("humanizeSlug", () => {
  it("title-cases words, keeps small words lowercase, and fixes product casing", () => {
    expect(humanizeSlug("working-with-openl-tables")).toBe("Working with OpenL Tables");
    expect(humanizeSlug("functions-and-data-types")).toBe("Functions and Data Types");
    // A small word leading the slug is still capitalized.
    expect(humanizeSlug("with-great-power")).toBe("With Great Power");
  });
});

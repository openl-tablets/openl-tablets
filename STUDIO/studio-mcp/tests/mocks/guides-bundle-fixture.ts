/**
 * Fixture guides bundle for tests.
 *
 * Tests must never depend on the real guides/ bundle: it is a build artifact
 * (CI runs `npm test` before `npm run build`), so every test that needs a
 * bundle writes this small fixture to a temp directory instead — either passed
 * to the registry functions directly or exposed via OPENL_MCP_GUIDES_DIR for
 * the tool handlers.
 */

import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";

import type { GuidesIndex } from "../../src/guides-registry.js";

/** Ids of the fixture documents, exported so tests reference one source of truth. */
export const FIXTURE_IDS = {
  rulesXml: "spec/rules.xml",
  basicConcepts: "guide/introduction/basic-concepts",
  smartRules: "guide/working-with-openl-tables/table-types/decision-table/smart-rules-tables",
} as const;

const FIXTURE_BODIES: Record<string, { title: string; body: string }> = {
  [FIXTURE_IDS.rulesXml]: {
    title: "Rules Project Descriptor (rules.xml)",
    body: "# Rules Project Descriptor (rules.xml)\n\nEvery project has one.\n",
  },
  [FIXTURE_IDS.basicConcepts]: {
    title: "Basic Concepts",
    body: "# Basic Concepts\n\nTables hold the rules.\n",
  },
  [FIXTURE_IDS.smartRules]: {
    title: "Smart Rules Table",
    body: "# Smart Rules Table\n\nConditions are matched by column headers.\n",
  },
};

/**
 * Write a complete fixture bundle (index.json + bodies) into a fresh temp
 * directory and return that directory.
 */
export function writeGuidesFixture(): string {
  const dir = mkdtempSync(join(tmpdir(), "openl-guides-fixture-"));

  const index: GuidesIndex = {
    schema_version: 1,
    source_repo: "openl-tablets/openl-tablets",
    source_ref: "fixture-ref",
    generated_at: "2026-01-01T00:00:00.000Z",
    guides: Object.entries(FIXTURE_BODIES).map(([id, { title, body }]) => ({
      id,
      type: id.startsWith("spec/") ? "specification" : "guide",
      title,
      path: `${id}.md`,
      source_path: `Docs/fixture/${id}.md`,
      size_bytes: Buffer.byteLength(body, "utf-8"),
    })),
  };

  for (const entry of index.guides) {
    const file = join(dir, ...entry.path.split("/"));
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, FIXTURE_BODIES[entry.id].body, "utf-8");
  }
  writeFileSync(join(dir, "index.json"), JSON.stringify(index, null, 2), "utf-8");

  return dir;
}

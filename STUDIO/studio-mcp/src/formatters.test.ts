/**
 * Unit tests for formatters.ts
 * Tests response formatting
 */

import { describe, it, expect } from "vitest";
import {
  formatResponse,
  formatAgentsDocument,
  AGENTS_DOCUMENT_NOTE,
} from "./formatters.js";
import { RESPONSE_LIMITS } from "./constants.js";
import type { AgentsFile } from "./types.js";

describe("formatters", () => {
  describe("formatResponse", () => {
    it.each([
      ["an object", { test: "value", nested: { array: [1, 2, 3], object: { key: "value" } }, flag: true, none: null }],
      ["an empty list", []],
      ["null", null],
      ["unicode text", { emoji: "😀🎉", chinese: "你好", arabic: "مرحبا" }],
    ])("answers %s as the data of the JSON envelope without indentation", (_case, data) => {
      expect(formatResponse(data)).toBe(JSON.stringify({ data }));
    });

    it("preserves backend collection metadata in JSON", () => {
      const options = {
        responseMetadata: {
          statusCounts: { OPENED: 2 },
          statuses: ["OPENED", "CLOSED"],
        },
      };
      const json = JSON.parse(formatResponse([{ projectId: "p1" }], options));

      expect(json.statusCounts).toEqual({ OPENED: 2 });
      expect(json.statuses).toEqual(["OPENED", "CLOSED"]);
    });

    it("does not let backend metadata override formatter-owned fields", () => {
      const parsed = JSON.parse(formatResponse([{ id: 1 }], {
        responseMetadata: {
          statusCounts: { OPENED: 1 },
          data: "spoofed",
          truncated: true,
          truncation_message: "spoofed",
        },
      }));

      expect(parsed.data).toEqual([{ id: 1 }]);
      expect(parsed.statusCounts).toEqual({ OPENED: 1 });
      expect(parsed).not.toHaveProperty("truncated");
      expect(parsed).not.toHaveProperty("truncation_message");
    });

    it("truncates a very long response and says so", () => {
      // Create data that will exceed 25,000 characters
      const largeArray = Array.from({ length: 1000 }, (_, i) => ({
        id: i,
        longText: "A".repeat(100),
      }));

      const result = formatResponse(largeArray);

      expect(result.length).toBeLessThanOrEqual(25500); // 25000 + some buffer for truncation message
      // This input is far over the limit, so the array-truncation path always runs.
      expect(result.length).toBeLessThanOrEqual(25_000);
      const parsed = JSON.parse(result);
      expect(parsed.truncated).toBe(true);
      // A list without paging has no way to continue: the note names none
      expect(parsed.truncation_message).toBe(
        `${RESPONSE_LIMITS.TRUNCATION_MESSAGE} It holds ${parsed.data.length} of 1000 items; narrow the request to read the rest.`,
      );
    });

    it("delivers a list over the limit when indented whole without indentation (EPBDS-16801)", () => {
      const definitions = Array.from({ length: 40 }, (_, i) => ({
        name: `property${i}`, displayName: `Property ${i}`, type: "string", multiple: false,
        allowedValues: ["CA", "NY", "TX", "WA", "OR"], description: "A".repeat(380),
      }));

      const result = formatResponse(definitions);

      expect(JSON.stringify({ data: definitions }, null, 2).length).toBeGreaterThan(25_000);
      expect(result).toBe(JSON.stringify({ data: definitions }));
    });

    it("keeps the first item whole when it is the only one that fits", () => {
      const backendPage = [
        { id: "first", text: "A".repeat(600) },
        ...Array.from({ length: 19 }, (_, i) => ({ id: `item-${i + 1}`, text: "B".repeat(300) })),
      ];
      const parsed = JSON.parse(formatResponse(backendPage, {
        characterLimit: 1_000,
      }));

      expect(parsed.data).toEqual([backendPage[0]]);
      expect(parsed.truncated).toBe(true);
    });

    it("cuts the first item in its structure when not even it fits", () => {
      const backendPage = [
        { id: "oversized", longText: "A".repeat(10_000) },
        { id: "next" },
        { id: "last" },
      ];
      const parsed = JSON.parse(formatResponse(backendPage, {
        characterLimit: 1_000,
      }));

      // The one item it holds is cut in its structure, its long text shortened
      expect(parsed.data[0].id).toBe("oversized");
      expect(parsed.data[0].longText.length).toBeLessThan(10_000);
    });

    it("cuts a large single-object JSON response in its structure and names the cuts", () => {
      const result = formatResponse({
        id: "large-table",
        source: Array.from({ length: 200 }, (_, row) => [{
          value: `row-${row}`,
          style: { font: "A".repeat(200), background: "#ffffff" },
        }]),
      }, { characterLimit: 2_000 });

      expect(result.length).toBeLessThanOrEqual(2_000);
      const parsed = JSON.parse(result);
      expect(parsed.truncated).toBe(true);
      expect(parsed.data.id).toBe("large-table");
      expect(parsed.data.source[0][0].value).toBe("row-0");
      expect(parsed.data.source.at(-1)[0].value).toBe("row-199");
      expect(parsed.truncation_message).toMatch(/Cut: .*source \(\d+ of 200 items from the middle\)/);
    });

  });

  describe("formatAgentsDocument", () => {
    // getProjectAgentContext returns nearest-first (project first); the document is root-first.
    const chain: AgentsFile[] = [
      { path: "foo/Project-1/AGENTS.md", content: "project guidance" },
      { path: "foo/AGENTS.md", content: "root guidance" },
    ];

    it("includes the precedence note header", () => {
      const doc = formatAgentsDocument(chain);
      // Assert against the source constant so the test auto-tracks any rewording
      // while still guarding that the precedence note actually ships.
      expect(doc).toContain(AGENTS_DOCUMENT_NOTE);
    });

    it("orders sections root-first (project folder last = highest priority)", () => {
      const doc = formatAgentsDocument(chain);
      expect(doc.indexOf("## /foo/AGENTS.md")).toBeLessThan(doc.indexOf("## /foo/Project-1/AGENTS.md"));
      expect(doc.indexOf("root guidance")).toBeLessThan(doc.indexOf("project guidance"));
    });

    it("prefixes each path with '/' and separates sections with '-----'", () => {
      const doc = formatAgentsDocument(chain);
      expect(doc).toContain("-----\n## /foo/AGENTS.md\n\nroot guidance");
      expect(doc).toContain("-----\n## /foo/Project-1/AGENTS.md\n\nproject guidance");
    });

    it("does not double-prefix a path that already starts with '/'", () => {
      const doc = formatAgentsDocument([{ path: "/abs/AGENTS.md", content: "x" }]);
      expect(doc).toContain("## /abs/AGENTS.md");
      expect(doc).not.toContain("## //abs/AGENTS.md");
    });

    it("trims trailing whitespace from content so section spacing stays uniform", () => {
      const doc = formatAgentsDocument([{ path: "a/AGENTS.md", content: "hello\n\n\n" }]);
      expect(doc).toContain("## /a/AGENTS.md\n\nhello\n");
      expect(doc).not.toContain("hello\n\n\n");
    });

    it("answers long guidance as the opening and the contents of each file, and its sections by their headings", () => {
      const project = [
        "# Project guide", "", "What this project is.", "",
        "## Rate factors", "", "x".repeat(8_000), "",
        "### Experience rating", "", "the blend", "",
        "## Gotchas", "", "```", "## not a heading", "```", "y".repeat(5_000),
      ].join("\n");
      const chain: AgentsFile[] = [{ path: "p/AGENTS.md", content: project }, { path: "AGENTS.md", content: "root guidance" }];

      const outline = formatAgentsDocument(chain);
      expect(outline).toContain("Read the sections your task needs with `sections`");
      expect(outline).toContain("## /p/AGENTS.md\n\n# Project guide\n\nWhat this project is.\n\nContents:\n"
        + "- Rate factors (8053 characters)\n  - Experience rating (34 characters)\n- Gotchas (5037 characters)");
      expect(outline).toContain("## /AGENTS.md\n\nroot guidance");
      expect(outline).not.toContain("xxx");

      const read = formatAgentsDocument(chain, ["experience RATING", "gotchas"]);
      expect(read).toContain("### Experience rating\n\nthe blend");
      expect(read).toContain("## Gotchas\n\n```\n## not a heading\n```\nyyy");
      expect(read).not.toContain("xxx");
      expect(read).toContain("## /AGENTS.md\n\n*No section of this file has such a heading.*");
    });

    it("keeps short guidance whole", () => {
      const doc = formatAgentsDocument([{ path: "AGENTS.md", content: "# Guide\n\n## Scope\n\nshort" }]);
      expect(doc).toContain("## Scope\n\nshort");
      expect(doc).not.toContain("Contents:");
    });

    it("returns a short note when there are no files", () => {
      expect(formatAgentsDocument([])).toBe("No AGENTS.md files apply to this project.");
    });
  });
});

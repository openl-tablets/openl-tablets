/**
 * Unit tests for formatters.ts
 * Tests response formatting and pagination functions
 */

import { describe, it, expect } from "vitest";
import {
  formatResponse,
  paginateCollection,
  formatPage,
  toMarkdown,
  toMarkdownConcise,
  toMarkdownDetailed,
  formatAgentsDocument,
  AGENTS_DOCUMENT_NOTE,
} from "./formatters.js";
import { RESPONSE_LIMITS } from "./constants.js";
import type { AgentsFile } from "./types.js";

describe("formatters", () => {
  describe("formatPage", () => {
    const page = (items: unknown[], limit: number, offset: number) =>
      JSON.parse(formatPage(items, "json", limit, offset));

    it("returns the page and where the next one starts", () => {
      const result = page(Array.from({ length: 100 }, (_, i) => ({ id: i })), 20, 40);

      expect(result.data).toHaveLength(20);
      expect(result.data[0]).toEqual({ id: 40 });
      expect(result.pagination).toEqual({ limit: 20, offset: 40, has_more: true, next_offset: 60, total_count: 100 });
    });

    it("reports no next page on a partial last page", () => {
      const result = page(Array.from({ length: 75 }, (_, i) => ({ id: i })), 50, 50);

      expect(result.data).toHaveLength(25);
      expect(result.pagination).toEqual({ limit: 50, offset: 50, has_more: false, total_count: 75 });
    });

    it("returns an empty page past the end of the list", () => {
      const result = page([{ id: 1 }, { id: 2 }], 50, 10);

      expect(result.data).toEqual([]);
      expect(result.pagination).toEqual({ limit: 50, offset: 10, has_more: false, total_count: 2 });
    });
  });

  describe("paginateCollection", () => {
    it("does not slice a backend page a second time", () => {
      const page = {
        items: Array.from({ length: 50 }, (_, i) => ({ id: i + 50 })),
        pageNumber: 1,
        pageSize: 50,
        total: 120,
      };

      const result = paginateCollection(page, 50);

      expect(result.data).toHaveLength(50);
      expect(result.data[0]).toEqual({ id: 50 });
      expect(result.pagination).toEqual({
        limit: 50,
        offset: 50,
        total: 120,
        hasMore: true,
      });
    });

    it("keeps an unknown total unknown and treats a full page as potentially incomplete", () => {
      const page = {
        items: Array.from({ length: 50 }, (_, i) => ({ id: i })),
        pageNumber: 0,
        pageSize: 50,
      };

      const result = paginateCollection(page, 0);
      const markdown = formatResponse(result.data, "markdown", {
        pagination: result.pagination,
      });

      expect(result.pagination.total).toBeUndefined();
      expect(result.pagination.hasMore).toBe(true);
      expect(markdown).not.toContain("Total: 50");
      expect(markdown).toContain("More results available (next offset: 50)");
    });

    it("preserves a non-page-aligned offset when the backend reports a derived page number", () => {
      const result = paginateCollection({
        items: Array.from({ length: 50 }, (_, i) => ({ id: i })),
        pageNumber: 0,
        pageSize: 50,
        total: 100,
      }, 25);

      expect(result.pagination).toEqual({
        limit: 50,
        offset: 25,
        total: 100,
        hasMore: true,
      });
    });
  });

  describe("toMarkdown", () => {
    it("lists repositories by name, id, type and features", () => {
      const markdown = toMarkdown(
        { data: [{
          id: "design", name: "Design | Main", type: "repo-git", aclId: "design",
          features: { branches: true, comments: true, mappedFolders: false },
        }] },
        "repositories",
      );

      expect(markdown).toContain("| Name | ID | Type | Features |");
      expect(markdown).toContain("| Design \\| Main | design | repo-git | branches, comments |");
    });

    const deployments = [{
      id: "cHJvZHVjdGlvbjpCYW5r",
      name: "Bank",
      repository: "production",
      items: [
        {
          name: "Bank",
          modifiedBy: "admin",
          modifiedAt: "2026-10-10T04:15:51-09:00",
          designRevision: { revision: "db8f4ba6", modifiedBy: "Ada", modifiedAt: "2026-10-10T04:15:49-09:00" },
        },
        { name: "Rates", modifiedBy: "admin", modifiedAt: "2026-10-10T04:16:02-09:00" },
      ],
    }];

    it("lists each deployed project with who deployed it, when, and from which design revision", () => {
      expect(toMarkdown({ data: deployments }, "deployments")).toBe([
        "# Deployments",
        "",
        "## Bank",
        "- **ID**: cHJvZHVjdGlvbjpCYW5r",
        "- **Repository**: production",
        "- **Bank**: deployed by admin at 2026-10-10T04:15:51-09:00, from design revision db8f4ba6",
        "- **Rates**: deployed by admin at 2026-10-10T04:16:02-09:00",
        "",
      ].join("\n"));
      expect(toMarkdownConcise({ data: deployments }, "deployments")).toContain("Deployments: Bank.");
    });

    it("should format simple object as markdown", () => {
      const data = { name: "Test", value: 123 };
      const result = toMarkdown({ data }, "test");

      expect(result).toContain("name");
      expect(result).toContain("Test");
      expect(result).toContain("value");
      expect(result).toContain("123");
    });

    it("should format array as markdown list", () => {
      const data = [
        { projectId: "design-project1", status: "OPENED" },
        { projectId: "design-project2", status: "CLOSED" },
      ];
      const result = toMarkdown({ data }, "projects");

      expect(result).toContain("project1");
      expect(result).toContain("project2");
      expect(result).toContain("OPENED");
      expect(result).toContain("CLOSED");
    });

    it("should handle empty array", () => {
      const result = toMarkdown({ data: [] }, "projects");
      expect(result).toContain("No");
    });

    it("should include pagination information when provided", () => {
      const data = [{ id: 1 }];
      const pagination = { limit: 50, offset: 0, has_more: true, next_offset: 50, total_count: 100 };
      const result = toMarkdown({ data, pagination }, "test");

      expect(result).toContain("Pagination");
      expect(result).toContain("offset");
    });
  });

  describe("toMarkdownConcise", () => {
    it("should create concise summary for projects", () => {
      const data = [
        { projectId: "ZGVzaWduOkJhbms=", name: "Bank", status: "EDITING" },
        { projectId: "ZGVzaWduOlJhdGVz", name: "Rates", status: "OPENED" },
        { projectId: "ZGVzaWduOkF1dG8=", name: "Auto", status: "EDITING" },
      ];
      const result = toMarkdownConcise({ data }, "projects");

      expect(result).toContain("Found 3");
      // Every project is named, with the id the other tools take, and counted in every status
      expect(result).toContain(
        "2 editing, 1 opened. Projects: Bank (ZGVzaWduOkJhbms=), Rates (ZGVzaWduOlJhdGVz), Auto (ZGVzaWduOkF1dG8=).",
      );
      expect(result.length).toBeLessThan(500); // Should be brief
    });

    it("names every table with its id, which the other tools take", () => {
      const data = [
        { id: "t1", name: "BankRatingGroup", tableType: "Rules" },
        { id: "t2", name: "BankRatingTest", tableType: "Test" },
      ];

      expect(toMarkdownConcise({ data }, "tables"))
        .toContain("Table types: Rules, Test. Tables: BankRatingGroup (t1), BankRatingTest (t2).");
    });

    it("names every repository with its id and its features", () => {
      const data = [
        { id: "design", name: "Design", features: { branches: true, comments: true, searchable: false } },
        { id: "local", name: "Local" },
      ];

      expect(toMarkdownConcise({ data }, "repositories"))
        .toContain("Repositories: Design (design: branches, comments), Local (local).");
    });

    it("should handle single item", () => {
      const data = [{ projectId: "design-p1", status: "OPENED" }];
      const result = toMarkdownConcise({ data }, "projects");

      expect(result).toContain("1 project");
    });

    it("lists the fields of a single object, nested ones by a dotted path and arrays by count", () => {
      const data = {
        name: "Bank",
        status: "EDITING",
        compileStatus: { compileState: "errors", compilation: { messages: { errors: 2, items: [{}, {}] } } },
        tags: ["a", "b"],
        comment: null,
      };

      expect(toMarkdownConcise({ data })).toBe(
        "name: Bank; status: EDITING; compileStatus.compileState: errors; " +
          "compileStatus.compilation.messages.errors: 2; compileStatus.compilation.messages.items: 2 items; tags: 2 items.",
      );
    });

    it("should handle empty results", () => {
      const result = toMarkdownConcise({ data: [] }, "projects");
      expect(result).toContain("0");
    });

    it("should include pagination hint when has_more is true", () => {
      const data = [{ id: 1 }];
      const pagination = { limit: 50, offset: 0, has_more: true, next_offset: 50, total_count: 100 };
      const result = toMarkdownConcise({ data, pagination }, "test");

      expect(result).toContain("offset=");
    });
  });

  describe("toMarkdownDetailed", () => {
    it("adds a summary and the ISO time of the retrieval", () => {
      const result = toMarkdownDetailed({ data: [{ projectId: "design-p1", status: "OPENED" }] }, "projects");

      expect(result).toContain("**Summary:**");
      expect(result).toMatch(/\*\*Retrieved:\*\* \d{4}-\d{2}-\d{2}T/);
    });

    it("should include status breakdown for projects", () => {
      const data = [
        { projectId: "design-p1", status: "OPENED" },
        { projectId: "design-p2", status: "VIEWING_VERSION" },
        { projectId: "design-p3", status: "CLOSED" },
      ];
      const result = toMarkdownDetailed({ data }, "projects");

      expect(result).toContain("**Status Breakdown:** 1 opened, 1 viewing version, 1 closed");
    });
  });

  describe("formatResponse", () => {
    it.each([
      ["an object", { test: "value", nested: { array: [1, 2, 3], object: { key: "value" } }, flag: true, none: null }],
      ["an empty list", []],
      ["null", null],
      ["unicode text", { emoji: "😀🎉", chinese: "你好", arabic: "مرحبا" }],
    ])("answers %s as the data of the JSON envelope without indentation, also by default", (_case, data) => {
      expect(formatResponse(data, "json")).toBe(JSON.stringify({ data }));
      expect(formatResponse(data)).toBe(formatResponse(data, "json"));
    });

    it("should format as markdown_concise", () => {
      const data = [{ projectId: "design-p1", status: "OPENED" }];
      const result = formatResponse(data, "markdown_concise", { dataType: "projects" });

      expect(result).toContain("Found");
      expect(result.length).toBeLessThan(1000);
    });

    it("should format as markdown_detailed", () => {
      const data = [{ projectId: "design-p1", status: "OPENED" }];
      const result = formatResponse(data, "markdown_detailed", { dataType: "projects" });

      expect(result).toContain("Summary");
      expect(result).toContain("Retrieved");
    });

    it("advances a short page by the number of delivered items", () => {
      const parsed = JSON.parse(formatResponse([{ id: 10 }, { id: 11 }], "json", {
        pagination: { limit: 50, offset: 10, total: 100, hasMore: true },
      }));

      expect(parsed.pagination).toMatchObject({
        limit: 50,
        offset: 10,
        has_more: true,
        next_offset: 12,
        total_count: 100,
      });
    });

    it("preserves backend collection metadata in JSON and Markdown", () => {
      const options = {
        responseMetadata: {
          statusCounts: { OPENED: 2 },
          statuses: ["OPENED", "CLOSED"],
        },
        dataType: "projects",
      };
      const json = JSON.parse(formatResponse([{ projectId: "p1" }], "json", options));
      const markdown = formatResponse([{ projectId: "p1" }], "markdown", options);

      expect(json.statusCounts).toEqual({ OPENED: 2 });
      expect(json.statuses).toEqual(["OPENED", "CLOSED"]);
      expect(markdown).toContain("Response Metadata");
      expect(markdown).toContain("statusCounts");
      expect(markdown).toContain("statuses");
    });

    it("does not let backend metadata override formatter-owned fields", () => {
      const parsed = JSON.parse(formatResponse([{ id: 1 }], "json", {
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

    it.each([
      ["the content of an envelope without an element count", { content: [{ id: 1 }, { id: 2 }] }, 12],
      ["a single object", { id: "t1" }, 11],
    ])("advances the next page by %s", (_case, data, nextOffset) => {
      const parsed = JSON.parse(formatResponse(data, "json", { pagination: { limit: 50, offset: 10, hasMore: true } }));

      expect(parsed.pagination.next_offset).toBe(nextOffset);
    });

    it("uses an envelope's element count to detect the final page", () => {
      const page = {
        content: Array.from({ length: 25 }, (_, i) => ({ id: i + 50 })),
        pageNumber: 1,
        pageSize: 50,
        numberOfElements: 25,
        total: 75,
      };

      const json = JSON.parse(formatResponse(page, "json", {
        pagination: { limit: 50, offset: 50, total: 75 },
      }));
      const markdown = formatResponse(page, "markdown", {
        pagination: { limit: 50, offset: 50, total: 75 },
      });

      expect(json.pagination).toMatchObject({
        limit: 50,
        offset: 50,
        total_count: 75,
        has_more: false,
      });
      expect(json.pagination).not.toHaveProperty("next_offset");
      expect(markdown).toContain("Showing items 51-75");
      expect(markdown).not.toContain("More results available");
    });

    it("truncates a very long response and says so", () => {
      // Create data that will exceed 25,000 characters
      const largeArray = Array.from({ length: 1000 }, (_, i) => ({
        id: i,
        longText: "A".repeat(100),
      }));

      const result = formatResponse(largeArray, "json");

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

    it("continues a truncated page from the first item not delivered", () => {
      const backendPage = Array.from({ length: 200 }, (_, i) => ({
        id: i,
        name: `Table${i}`,
        longText: "A".repeat(200),
      }));
      const parsed = JSON.parse(formatResponse(backendPage, "json", {
        pagination: { limit: 200, offset: 0, total: 275, hasMore: true },
      }));

      expect(parsed.truncated).toBe(true);
      expect(parsed.data.length).toBeLessThan(backendPage.length);
      expect(parsed.pagination).toMatchObject({
        limit: 200,
        offset: 0,
        has_more: true,
        next_offset: parsed.data.length,
        total_count: 275,
      });
    });

    /** Tables of a page whose Markdown is over the limit. */
    const tables = Array.from({ length: 200 }, (_, i) => ({
      id: `${String(i).padStart(4, "0")}b5eed6e9752f7ce3648d6560ba73f6a2`.slice(0, 32),
      name: `Table${i}`,
      tableType: "SimpleSpreadsheet",
      kind: "Spreadsheet",
      signature: "GenderPercentageByCensus (SpreadsheetResultCensus census)",
      returnType: "SpreadsheetResult",
      file: "STD Rating/rules/STD Algorithm-CW-20200101-20200101.xlsx",
    }));

    it.each(["markdown", "markdown_detailed"] as const)(
      "cuts a %s list on a whole row and continues it from the next item (EPBDS-16736)", (format) => {
        const result = formatResponse(tables, format, {
          dataType: "tables",
          pagination: { limit: 200, offset: 0, total: 275, hasMore: true },
        });

        expect(result.length).toBeLessThanOrEqual(25_000);
        const rows = result.split("\n").filter((line) => line.startsWith("| Table"));
        expect(rows.length).toBeGreaterThan(0);
        expect(rows.length).toBeLessThan(200);
        expect(rows.every((row) => row.endsWith(" |"))).toBe(true);
        expect(result).toContain(`- More results available (next offset: ${rows.length})`);
        expect(result.endsWith(`It holds ${rows.length} of 200 items; continue with offset=${rows.length}.`)).toBe(true);
      });

    it("delivers a list over the limit when indented whole without indentation (EPBDS-16801)", () => {
      const definitions = Array.from({ length: 40 }, (_, i) => ({
        name: `property${i}`, displayName: `Property ${i}`, type: "string", multiple: false,
        allowedValues: ["CA", "NY", "TX", "WA", "OR"], description: "A".repeat(380),
      }));

      const result = formatResponse(definitions, "json");

      expect(JSON.stringify({ data: definitions }, null, 2).length).toBeGreaterThan(25_000);
      expect(result).toBe(JSON.stringify({ data: definitions }));
    });

    it("makes a truncated final backend page resumable", () => {
      const backendPage = Array.from({ length: 75 }, (_, id) => ({ id, longText: "A".repeat(400) }));
      const parsed = JSON.parse(formatResponse(backendPage, "json", {
        pagination: { limit: 200, offset: 200, total: 275, hasMore: false },
      }));

      expect(parsed.truncated).toBe(true);
      expect(parsed.data.length).toBeLessThan(backendPage.length);
      expect(parsed.pagination.has_more).toBe(true);
      expect(parsed.pagination.next_offset).toBe(200 + parsed.data.length);
    });

    it("keeps the first item whole when it is the only one that fits", () => {
      const backendPage = [
        { id: "first", text: "A".repeat(600) },
        ...Array.from({ length: 19 }, (_, i) => ({ id: `item-${i + 1}`, text: "B".repeat(300) })),
      ];
      const parsed = JSON.parse(formatResponse(backendPage, "json", {
        characterLimit: 1_000,
        pagination: { limit: 20, offset: 0, total: 20 },
      }));

      expect(parsed.data).toEqual([backendPage[0]]);
      expect(parsed.truncated).toBe(true);
      expect(parsed.pagination.next_offset).toBe(1);
    });

    it("advances an oversized-item preview without skipping the rest of the page", () => {
      const backendPage = [
        { id: "oversized", longText: "A".repeat(10_000) },
        { id: "next" },
        { id: "last" },
      ];
      const parsed = JSON.parse(formatResponse(backendPage, "json", {
        characterLimit: 1_000,
        pagination: { limit: 200, offset: 0, total: 275, hasMore: true },
      }));

      // The one item it holds is cut in its structure, its long text shortened
      expect(parsed.data[0].id).toBe("oversized");
      expect(parsed.data[0].longText.length).toBeLessThan(10_000);
      expect(parsed.pagination.has_more).toBe(true);
      expect(parsed.pagination.next_offset).toBe(1);
    });

    it("cuts a large single-object JSON response in its structure and names the cuts", () => {
      const result = formatResponse({
        id: "large-table",
        source: Array.from({ length: 200 }, (_, row) => [{
          value: `row-${row}`,
          style: { font: "A".repeat(200), background: "#ffffff" },
        }]),
      }, "json", { characterLimit: 2_000 });

      expect(result.length).toBeLessThanOrEqual(2_000);
      const parsed = JSON.parse(result);
      expect(parsed.truncated).toBe(true);
      expect(parsed.data.id).toBe("large-table");
      expect(parsed.data.source[0][0].value).toBe("row-0");
      expect(parsed.data.source.at(-1)[0].value).toBe("row-199");
      expect(parsed.truncation_message).toMatch(/Cut: .*source \(\d+ of 200 items from the middle\)/);
    });

  });

  describe("table formatting", () => {
    it("should format tables with all metadata fields", () => {
      const tables = [
        {
          id: "test_1234",
          name: "testRule",
          tableType: "SimpleRules",
          kind: "Rules",
          signature: "double testRule(int x)",
          returnType: "double",
          file: "Rules.xlsx",
          properties: {
            category: "Test",
            version: "1.0",
          },
        },
      ];

      const result = formatResponse(tables, "markdown", { dataType: "tables" });

      expect(result).toContain("testRule");
      expect(result).toContain("Kind");
      expect(result).toContain("Signature");
      expect(result).toContain("Return Type");
      expect(result).toContain("Properties");
      expect(result).toContain("Rules");
      expect(result).toContain("double testRule");
      expect(result).toContain("category");
    });

    it("should handle tables with pipe characters in values", () => {
      const tables = [
        {
          id: "test_1234",
          name: "test|rule",
          tableType: "SimpleRules",
          kind: "Rules",
          signature: "double test(int x)",
          returnType: "double",
          file: "Rules.xlsx",
        },
      ];

      const result = formatResponse(tables, "markdown", { dataType: "tables" });

      // Should escape pipe characters
      expect(result).toContain("test\\|rule");
    });

    it("should truncate long signatures", () => {
      const longSignature = "double veryLongMethodNameWithManyParameters(int param1, int param2, int param3, int param4, int param5)";
      const tables = [
        {
          id: "test_1234",
          name: "testRule",
          tableType: "SimpleRules",
          kind: "Rules",
          signature: longSignature,
          returnType: "double",
          file: "Rules.xlsx",
        },
      ];

      const result = formatResponse(tables, "markdown", { dataType: "tables" });

      // Signature should be truncated
      expect(result).toContain("...");
      expect(result).not.toContain(longSignature);
    });

    it("should format properties correctly", () => {
      const tables = [
        {
          id: "test_1234",
          name: "testRule",
          tableType: "SimpleRules",
          kind: "Rules",
          signature: "double test()",
          returnType: "double",
          file: "Rules.xlsx",
          properties: {
            prop1: { value: 1 },
            prop2: { value: 2 },
            prop3: { value: 3 },
            prop4: { value: 4 },
          },
        },
      ];

      const result = formatResponse(tables, "markdown", { dataType: "tables" });

      // Should show first 3 properties and count
      expect(result).toContain("prop1");
      expect(result).toContain("prop2");
      expect(result).toContain("prop3");
      expect(result).toContain("+1 more");
    });
  });

  describe("test_results_summary formatting", () => {
    it("should format test results summary as markdown", () => {
      const summary = {
        executionTimeMs: 250.5,
        numberOfTests: 10,
        numberOfFailures: 2,
        numberOfPassed: 8,
      };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("# Test Results Summary");
      expect(result).toContain("## Summary");
      expect(result).toContain("**Total Tests**: 10");
      expect(result).toContain("**Passed**: 8");
      expect(result).toContain("**Failed**: 2");
      expect(result).toContain("**Execution Time**: 250.50 ms");
    });

    it("should handle test results summary with zero values", () => {
      const summary = {
        executionTimeMs: 0,
        numberOfTests: 0,
        numberOfFailures: 0,
        numberOfPassed: 0,
      };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("# Test Results Summary");
      expect(result).toContain("**Total Tests**: 0");
      expect(result).toContain("**Passed**: 0");
      expect(result).toContain("**Failed**: 0");
      expect(result).toContain("**Execution Time**: 0.00 ms");
    });

    it("should calculate numberOfPassed when not provided", () => {
      const summary = {
        executionTimeMs: 100,
        numberOfTests: 5,
        numberOfFailures: 1,
        // numberOfPassed not provided, should be calculated as numberOfTests - numberOfFailures
      };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("**Total Tests**: 5");
      expect(result).toContain("**Passed**: 4"); // 5 - 1 = 4
      expect(result).toContain("**Failed**: 1");
    });

    it.each([null, "invalid"])("reports an execution time of %j as 0", (executionTimeMs) => {
      const summary = { executionTimeMs: executionTimeMs as any, numberOfTests: 10, numberOfFailures: 2, numberOfPassed: 8 };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("**Execution Time**: 0.00 ms");
    });

  });

  describe("test_results formatting", () => {
    it("includes failed test-case assertions with expected and actual values in markdown", () => {
      const results = {
        testCases: [{
          name: "policyCommissionTest",
          tableId: "test-table-1",
          executionTimeMs: 5.45,
          numberOfTests: 2,
          numberOfFailures: 1,
          testUnits: [{
            id: "13",
            description: "policy issued",
            status: "TR_NEQ" as const,
            executionTimeMs: 2.17,
            testAssertions: [
              { description: "EventType", expectedValue: "policyIssued", actualValue: "policyIssued", status: "TR_OK" as const },
              { description: "AnnualAmt", expectedValue: 4200, actualValue: null, status: "TR_NEQ" as const },
              { description: "PremiumCode", expectedValue: "NWT|net", actualValue: "GWT\ngross", status: "TR_NEQ" as const },
            ],
          }, {
            id: "14",
            status: "TR_OK" as const,
            executionTimeMs: 1,
            testAssertions: [
              { description: "FlatAmount", expectedValue: 0, actualValue: 0, status: "TR_OK" as const },
            ],
          }],
        }],
        executionTimeMs: 5.45,
        numberOfTests: 2,
        numberOfFailures: 1,
        pageNumber: 0,
        pageSize: 50,
        numberOfElements: 1,
      };

      const markdown = formatResponse(results, "markdown", { dataType: "test_results" });

      expect(markdown).toContain("## Test Units");
      expect(markdown).toContain("| 13 | policy issued | ❌ FAILED | 3 | 2 | 2.17 |");
      expect(markdown).toContain("| 14 | N/A | ✅ PASSED | 1 | 0 | 1.00 |");
      expect(markdown).toContain("#### Failure Details — policyCommissionTest");
      expect(markdown).toContain("| AnnualAmt | `4200` | `null` | ❌ FAILED |");
      expect(markdown).toContain('| PremiumCode | `"NWT\\|net"` | `"GWT\\ngross"` | ❌ FAILED |');
      expect(markdown).not.toContain("| EventType | `\"policyIssued\"`");
      expect(markdown).not.toContain("| FlatAmount | `0`");
    });

    it("says so when the results hold no test case", () => {
      const results = { testCases: [], executionTimeMs: 0, numberOfTests: 0, numberOfFailures: 0, pageNumber: 0, pageSize: 50, numberOfElements: 0 };

      expect(formatResponse(results, "markdown", { dataType: "test_results" })).toContain("No test cases found.");
    });

    it("includes execution errors when a failed unit has no assertion details", () => {
      const results = {
        testCases: [{
          name: "brokenTest",
          tableId: "test-table-2",
          executionTimeMs: 1,
          numberOfTests: 1,
          numberOfFailures: 1,
          testUnits: [{
            id: "7",
            status: "TR_EXCEPTION" as const,
            errors: [{ severity: "ERROR" as const, summary: "Rule failed\nwhile executing" }],
          }],
        }],
        executionTimeMs: 1,
        numberOfTests: 1,
        numberOfFailures: 1,
        pageNumber: 0,
        pageSize: 50,
        numberOfElements: 1,
      };

      const markdown = formatResponse(results, "markdown_detailed", { dataType: "test_results" });

      expect(markdown).toContain("##### Test 7");
      expect(markdown).toContain("- **Status:** ⚠️ ERROR");
      expect(markdown).toContain("  - ERROR: Rule failed while executing");
      expect(markdown).toContain("- No assertion-level failure details returned.");
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

    it("keeps a document over the response limit whole", () => {
      const doc = formatAgentsDocument([
        { path: "foo/Project-1/AGENTS.md", content: "x".repeat(40_000) },
        { path: "AGENTS.md", content: "root guidance" },
      ]);

      expect(doc.length).toBeGreaterThan(RESPONSE_LIMITS.MAX_CHARACTERS);
      expect(doc).toContain("root guidance");
      expect(doc.trimEnd().endsWith("x")).toBe(true);
      expect(doc).not.toContain(RESPONSE_LIMITS.TRUNCATION_MESSAGE);
    });

    it("returns a short note when there are no files", () => {
      expect(formatAgentsDocument([])).toBe("No AGENTS.md files apply to this project.");
    });
  });
});

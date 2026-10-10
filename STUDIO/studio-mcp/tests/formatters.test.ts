/**
 * Unit tests for formatters.ts
 * Tests response formatting and pagination functions
 */

import { describe, it, expect } from "@jest/globals";
import {
  formatResponse,
  paginateCollection,
  paginateResults,
  toMarkdown,
  toMarkdownConcise,
  toMarkdownDetailed,
  formatAgentsDocument,
  AGENTS_DOCUMENT_NOTE,
} from "../src/formatters.js";
import { RESPONSE_LIMITS } from "../src/constants.js";
import type { AgentsFile, TableNodeView } from "../src/types.js";

describe("formatters", () => {
  describe("paginateResults", () => {
    it("should paginate array with default limit and offset", () => {
      const data = Array.from({ length: 100 }, (_, i) => ({ id: i }));
      const result = paginateResults(data, 50, 0);

      expect(result.data.length).toBe(50);
      expect(result.total_count).toBe(100);
      expect(result.has_more).toBe(true);
      expect(result.next_offset).toBe(50);
    });

    it("should paginate with custom limit and offset", () => {
      const data = Array.from({ length: 100 }, (_, i) => ({ id: i }));
      const result = paginateResults(data, 20, 40);

      expect(result.data.length).toBe(20);
      expect(result.data[0]).toEqual({ id: 40 });
      expect(result.total_count).toBe(100);
      expect(result.has_more).toBe(true);
      expect(result.next_offset).toBe(60);
    });

    it("should handle last page correctly", () => {
      const data = Array.from({ length: 100 }, (_, i) => ({ id: i }));
      const result = paginateResults(data, 50, 50);

      expect(result.data.length).toBe(50);
      expect(result.total_count).toBe(100);
      expect(result.has_more).toBe(false);
      expect(result.next_offset).toBeNull();
    });

    it("should handle empty array", () => {
      const result = paginateResults([], 50, 0);

      expect(result.data.length).toBe(0);
      expect(result.total_count).toBe(0);
      expect(result.has_more).toBe(false);
      expect(result.next_offset).toBeNull();
    });

    it("should handle offset beyond data length", () => {
      const data = [{ id: 1 }, { id: 2 }];
      const result = paginateResults(data, 50, 10);

      expect(result.data.length).toBe(0);
      expect(result.total_count).toBe(2);
      expect(result.has_more).toBe(false);
      expect(result.next_offset).toBeNull();
    });

    it("should handle partial last page", () => {
      const data = Array.from({ length: 75 }, (_, i) => ({ id: i }));
      const result = paginateResults(data, 50, 50);

      expect(result.data.length).toBe(25);
      expect(result.total_count).toBe(75);
      expect(result.has_more).toBe(false);
      expect(result.next_offset).toBeNull();
    });
  });

  describe("paginateCollection", () => {
    it("does not slice a backend page a second time", () => {
      const page = {
        items: Array.from({ length: 50 }, (_, i) => ({ id: i + 50 })),
        serverPaginated: true,
        pageNumber: 1,
        pageSize: 50,
        total: 120,
      };

      const result = paginateCollection(page, 50, 50);

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
        serverPaginated: true,
        pageNumber: 0,
        pageSize: 50,
      };

      const result = paginateCollection(page, 50, 0);
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
        serverPaginated: true,
        pageNumber: 0,
        pageSize: 50,
        total: 100,
      }, 50, 25);

      expect(result.pagination).toEqual({
        limit: 50,
        offset: 25,
        total: 100,
        hasMore: true,
      });
    });
  });

  describe("toMarkdown", () => {
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
        { projectId: "design-p1", projectName: "Project1", status: "OPENED" },
        { projectId: "design-p2", projectName: "Project2", status: "CLOSED" },
      ];
      const result = toMarkdownConcise({ data }, "projects");

      expect(result).toContain("Found 2");
      expect(result.length).toBeLessThan(500); // Should be brief
    });

    it("should handle single item", () => {
      const data = [{ projectId: "design-p1", status: "OPENED" }];
      const result = toMarkdownConcise({ data }, "projects");

      expect(result).toContain("1 project");
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
    it("should create detailed format with metadata", () => {
      const data = [
        { projectId: "design-p1", status: "OPENED" },
      ];
      const result = toMarkdownDetailed({ data }, "projects");

      expect(result).toContain("Summary");
      expect(result).toContain("Retrieved");
      expect(result.length).toBeGreaterThan(100); // Should have more content
    });

    it("should include timestamp", () => {
      const data = [{ id: 1 }];
      const result = toMarkdownDetailed({ data }, "test");

      expect(result).toMatch(/\d{4}-\d{2}-\d{2}T/); // ISO timestamp format
    });

    it("should include status breakdown for projects", () => {
      const data = [
        { projectId: "design-p1", status: "OPENED" },
        { projectId: "design-p2", status: "CLOSED" },
      ];
      const result = toMarkdownDetailed({ data }, "projects");

      expect(result).toContain("Status Breakdown");
      expect(result).toContain("opened");
      expect(result).toContain("closed");
    });
  });

  describe("formatResponse", () => {
    it("should format as JSON when format is json", () => {
      const data = { test: "value" };
      const result = formatResponse(data, "json");

      expect(() => JSON.parse(result)).not.toThrow();
      const parsed = JSON.parse(result);
      expect(parsed.data).toEqual(data);
    });

    it("should format as JSON by default", () => {
      const data = { test: "value" };
      const result = formatResponse(data);

      expect(JSON.parse(result)).toEqual({ data });
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

    it("should handle pagination metadata", () => {
      const data = [{ id: 1 }];
      const result = formatResponse(data, "json", {
        pagination: { limit: 50, offset: 0, total: 100 }
      });

      const parsed = JSON.parse(result);
      expect(parsed.pagination).toBeDefined();
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

    it("should truncate very long responses", () => {
      // Create data that will exceed 25,000 characters
      const largeArray = Array.from({ length: 1000 }, (_, i) => ({
        id: i,
        longText: "A".repeat(100),
      }));

      const result = formatResponse(largeArray, "json");

      expect(result.length).toBeLessThanOrEqual(25500); // 25000 + some buffer for truncation message
    });

    it("should include truncation message when truncated", () => {
      const largeArray = Array.from({ length: 1000 }, (_, i) => ({
        id: i,
        longText: "A".repeat(100),
      }));

      const result = formatResponse(largeArray, "json");

      // This input is far over the limit, so the array-truncation path always runs.
      expect(result).toContain("truncated");
      expect(result).toContain(RESPONSE_LIMITS.TRUNCATION_MESSAGE);
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

      expect(parsed.data.preview_format).toBe("json");
      expect(parsed.pagination.has_more).toBe(true);
      expect(parsed.pagination.next_offset).toBe(1);
    });

    it("caps a large single-object JSON response with an explicit valid-JSON preview", () => {
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
      expect(parsed.data.preview_format).toBe("json");
      expect(parsed.data.truncated_json_preview).toContain("large-table");
    });

    it("should handle empty data", () => {
      const result = formatResponse([], "json");
      expect(() => JSON.parse(result)).not.toThrow();
    });

    it("should handle null data", () => {
      const result = formatResponse(null, "json");
      expect(result).toContain("null");
    });

    it("should preserve data structure in JSON format", () => {
      const complexData = {
        nested: {
          array: [1, 2, 3],
          object: { key: "value" },
        },
        number: 123,
        boolean: true,
        null: null,
      };

      const result = formatResponse(complexData, "json");
      const parsed = JSON.parse(result);

      expect(parsed.data).toEqual(complexData);
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

  describe("table dependency formatting", () => {
    const graph: TableNodeView[] = [
      {
        id: "driver",
        name: "Driver",
        kind: "Datatype",
        tableType: "Datatype",
        extends: "party",
        dependencies: ["party", "vehicle", "driver"],
        dependents: ["driver"],
        fields: [
          { name: "name", type: "String" },
          { name: "vehicle", type: "Vehicle", ref: "vehicle" },
          { name: "otherDrivers", type: "Driver[]", ref: "driver", collection: true },
        ],
      },
      { id: "party", name: "Party", kind: "Datatype", tableType: "Datatype" },
      {
        id: "vehicle",
        name: "Vehicle",
        kind: "Datatype",
        tableType: "Vocabulary",
        vocabulary: {
          valueType: "Integer",
          valueCount: 8,
          valuesPreview: [100, 200, 300, 600, 700, 800],
          truncated: true,
        },
      },
      {
        id: "premium-dispatcher",
        name: "Premium",
        kind: "Dispatcher",
        tableType: "Dispatcher",
        dependencies: ["premium-ca", "premium-ca", "missing"],
      },
      {
        id: "premium-ca",
        name: "Premium",
        kind: "Rules",
        tableType: "SimpleRules",
        signature: "Double Premium(Driver driver)",
        returnType: "Double",
        dimensionProperties: { state: "CA" },
        dependents: ["premium-dispatcher"],
      },
    ];

    it("preserves discriminated graph nodes in JSON", () => {
      const result = JSON.parse(formatResponse(graph, "json", { dataType: "table_dependencies" }));

      expect(result.data).toEqual(graph);
    });

    it("renders mixed dependency layers as separate Mermaid diagrams", () => {
      const result = formatResponse(graph, "markdown", {
        dataType: "table_dependencies",
        markdownContext: { scope: "whole project", layer: "all" },
      });

      expect(result).toContain("## Executable call graph");
      expect(result).toContain("flowchart LR");
      expect(result).toContain('e0["Premium<br/>Dispatcher"]');
      expect(result).toContain("e0 --> e1");
      expect(result).toContain("class e0 dispatcher");
      expect(result).toContain("## Data model");
      expect(result).toContain("erDiagram");
      expect(result).toContain("Driver {");
      expect(result).toContain("String name");
      expect(result).toContain("Vehicle vehicle");
      expect(result).toContain("Driver[] otherDrivers");
      expect(result).toContain('"Vehicle<Integer>" {');
      expect(result).toContain("\u200B value_100");
      expect(result).toContain('\u200B \u200B "+ 2 more"');
      expect(result).toContain("\u200B value_800");
      expect(result).toContain('Driver ||--o| "Vehicle<Integer>" : vehicle');
      expect(result).toContain('Driver ||--o{ Driver : otherDrivers');
      expect(result).toContain("## Datatype inheritance");
      expect(result).toContain("classDiagram");
      expect(result).toContain("Party <|-- Driver");
      expect(result.match(/```mermaid/g)).toHaveLength(3);
      expect(result).toContain("layer all");
      expect(result).not.toContain("## Node details");
      expect(result).not.toContain("**Table ID:**");
    });

    it("adds node metadata after the Mermaid diagrams in detailed Markdown", () => {
      const result = formatResponse(graph, "markdown_detailed", {
        dataType: "table_dependencies",
        markdownContext: { scope: "whole project", layer: "all" },
      });

      expect(result).toContain("flowchart LR");
      expect(result).toContain("classDiagram");
      expect(result).toContain("## Node details");
      expect(result).toContain("**Extends:** Party (`party`)");
      expect(result).toContain("`vehicle: Vehicle`");
      expect(result).toContain("`otherDrivers: Driver[]`");
      expect(result).toContain("(collection)");
      expect(result).toContain("**Vocabulary:** 8 Integer values");
      expect(result).toContain("**Values preview:** `100`, `200`, `300`, + 2 more, `600`, `700`, `800`");
      expect(result).toContain("**Used by:** Driver (`driver`)");
      expect(result).toContain("**Signature:** `Double Premium(Driver driver)`");
      expect(result).toContain("**Dimension properties:** `{\"state\":\"CA\"}`");
    });

    it("keeps concise Markdown textual", () => {
      const result = formatResponse(graph, "markdown_concise", {
        dataType: "table_dependencies",
        markdownContext: { scope: "whole project", layer: "datatype" },
      });

      expect(result).not.toContain("```mermaid");
      expect(result).toContain("5 nodes and 4 links");
      expect(result).toContain("2 executable nodes with 1 call link");
      expect(result).toContain("3 datatype/vocabulary nodes with 3 model links");
      expect(result).toContain("Highest executable fan-out: Premium (1 dependency)");
      expect(result).toContain("3 fields, including 2 typed references (1 collection)");
      expect(result).toContain("1 inheritance relation; every data-model node has a model-layer link");
      expect(result).toContain("1 vocabulary with 8 declared values; 1 preview is truncated");
      expect(result).toContain("layer datatype");
    });

    it("escapes untrusted Mermaid labels without exposing executable syntax", () => {
      const unsafeGraph: TableNodeView[] = [
        {
          id: "unsafe",
          name: 'Unsafe"]\n%%{init: {"theme":"dark"}}%%',
          kind: "Datatype",
          tableType: "Datatype",
          dependencies: ["target"],
          fields: [{ name: 'owner:\n"quoted" %%', type: "Target", ref: "target" }],
        },
        {
          id: "target",
          name: "Target",
          kind: "Datatype",
          tableType: "Vocabulary",
          vocabulary: {
            valueType: "String\n}\n%%{init}%%",
            valueCount: 1,
            valuesPreview: ['value"}\n%%{init}%%'],
            truncated: false,
          },
        },
        { id: "unsafe-executable", name: 'Executable"]\n%%{init}%%', kind: "Rules", tableType: "SimpleRules" },
      ];

      const result = formatResponse(unsafeGraph, "markdown", { dataType: "table_dependencies" });

      expect(result).toContain("&quot;");
      expect(result).toContain("Unsafe_init_theme_dark {");
      expect(result).not.toContain("%%{init");
      expect(result).toContain("owner quoted");
      expect(result).toContain('"Target<String_init>" {');
      expect(result).toContain("\u200B value_init");
      expect((result.match(/```/g) ?? []).length % 2).toBe(0);
    });

    it("keeps datatype identifiers unique after sanitizing their names", () => {
      const duplicateNames: TableNodeView[] = [
        { id: "risk-space", name: "Risk Score", kind: "Datatype", tableType: "Datatype" },
        { id: "risk-dash", name: "Risk-Score", kind: "Datatype", tableType: "Datatype" },
      ];

      const result = formatResponse(duplicateNames, "markdown", { dataType: "table_dependencies" });

      expect(result).toMatch(/^  Risk_Score$/m);
      expect(result).toMatch(/^  Risk_Score_2$/m);
      expect(result).toContain("Unconnected datatypes/vocabularies without displayable members: `Risk Score`, `Risk-Score`.");
    });

    it("renders values for unconnected vocabularies that Mermaid would otherwise hide", () => {
      const vocabularies: TableNodeView[] = [
        {
          id: "eligibility",
          name: "EligibilityType",
          kind: "Datatype",
          tableType: "Vocabulary",
          vocabulary: {
            valueType: "String",
            valueCount: 3,
            valuesPreview: ["Not Eligible", "Provisional", "Eligible"],
            truncated: false,
          },
        },
        {
          id: "blank",
          name: "Blank",
          kind: "Datatype",
          tableType: "Vocabulary",
          vocabulary: { valueType: "String", valueCount: 0, truncated: false },
        },
      ];

      const result = formatResponse(vocabularies, "markdown", { dataType: "table_dependencies" });

      expect(result).toContain('"EligibilityType<String>" {');
      expect(result).toContain("\u200B Not_Eligible");
      expect(result).toContain("\u200B Provisional");
      expect(result).toContain("\u200B Eligible");
      expect(result).toMatch(/^  "Blank<String>"$/m);
      expect(result).toContain("Unconnected datatypes/vocabularies without displayable members: `Blank`.");
    });

    it("keeps Mermaid keywords in datatype display labels only", () => {
      const reservedNames: TableNodeView[] = [
        {
          id: "direction",
          name: "direction",
          kind: "Datatype",
          tableType: "Datatype",
          extends: "classDiagram",
          dependencies: ["classDiagram"],
          fields: [{ name: "value", type: "String" }],
        },
        { id: "classDiagram", name: "classDiagram", kind: "Datatype", tableType: "Datatype" },
      ];

      const result = formatResponse(reservedNames, "markdown", { dataType: "table_dependencies" });

      expect(result).toContain("Type_direction {");
      expect(result).toMatch(/^  Type_classDiagram$/m);
      expect(result).toContain("Type_classDiagram <|-- Type_direction");
      expect(result).not.toMatch(/^  direction \{/m);
    });

    it.each(["markdown", "markdown_detailed"] as const)(
      "omits whole nodes instead of cutting a Mermaid block at the character limit in %s",
      (responseFormat) => {
        const largeGraph: TableNodeView[] = Array.from({ length: 40 }, (_, index) => ({
          id: `node-${index}`,
          name: `Executable table with a deliberately long name ${index}`,
          kind: "Rules",
          tableType: "SimpleRules",
          dependencies: index < 39 ? [`node-${index + 1}`] : [],
        }));

        const result = formatResponse(largeGraph, responseFormat, {
          dataType: "table_dependencies",
          characterLimit: 900,
        });

        expect(result.length).toBeLessThanOrEqual(900);
        expect(result).toContain("nodes;");
        expect(result).toContain("omitted to keep the response within the character limit");
        expect((result.match(/```/g) ?? []).length % 2).toBe(0);
        expect(result).toMatch(/```mermaid[\s\S]*```/);
      },
    );

    it("fits a large dependency graph without rebuilding every smaller candidate", () => {
      const largeGraph: TableNodeView[] = Array.from({ length: 5_000 }, (_, index) => ({
        id: `node-${index}`,
        name: `Executable table ${index}`,
        kind: "Rules",
        tableType: "SimpleRules",
        dependencies: index < 4_999 ? [`node-${index + 1}`] : [],
      }));

      const startedAt = performance.now();
      const result = formatResponse(largeGraph, "markdown", { dataType: "table_dependencies" });

      expect(result.length).toBeLessThanOrEqual(RESPONSE_LIMITS.MAX_CHARACTERS);
      expect(result).toContain("omitted to keep the response within the character limit");
      expect(performance.now() - startedAt).toBeLessThan(2_000);
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

    it("should handle invalid executionTimeMs gracefully", () => {
      const summary = {
        executionTimeMs: null as any,
        numberOfTests: 10,
        numberOfFailures: 2,
        numberOfPassed: 8,
      };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("# Test Results Summary");
      expect(result).toContain("**Execution Time**: 0.00 ms"); // Should default to 0
    });

    it("should handle invalid executionTimeMs as string", () => {
      const summary = {
        executionTimeMs: "invalid" as any,
        numberOfTests: 10,
        numberOfFailures: 2,
        numberOfPassed: 8,
      };

      const result = formatResponse(summary, "markdown", { dataType: "test_results_summary" });

      expect(result).toContain("# Test Results Summary");
      expect(result).toContain("**Execution Time**: 0.00 ms"); // Should default to 0
    });

    it("should format test results summary in JSON format", () => {
      const summary = {
        executionTimeMs: 150.25,
        numberOfTests: 20,
        numberOfFailures: 3,
        numberOfPassed: 17,
      };

      const result = formatResponse(summary, "json", { dataType: "test_results_summary" });

      const parsed = JSON.parse(result);
      expect(parsed.data).toEqual(summary);
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

  describe("edge cases", () => {
    it("should handle circular references in JSON", () => {
      const circular: any = { a: 1 };
      circular.self = circular;

      // Should not throw, should handle gracefully
      expect(() => formatResponse(circular, "json")).not.toThrow();
    });

    it("should handle unicode characters", () => {
      const data = {
        emoji: "😀🎉",
        chinese: "你好",
        arabic: "مرحبا",
      };

      const result = formatResponse(data, "json");
      const parsed = JSON.parse(result);
      expect(parsed.data).toEqual(data);
    });

    it("should handle very deep nesting", () => {
      let deep: any = { value: 1 };
      for (let i = 0; i < 50; i++) {
        deep = { nested: deep };
      }

      expect(() => formatResponse(deep, "json")).not.toThrow();
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

    it("caps an oversized document and appends the truncation message", () => {
      const huge = formatAgentsDocument([{ path: "big/AGENTS.md", content: "x".repeat(60_000) }]);
      expect(huge.length).toBeLessThanOrEqual(RESPONSE_LIMITS.MAX_CHARACTERS + RESPONSE_LIMITS.TRUNCATION_MESSAGE.length + 2);
      expect(huge).toContain(RESPONSE_LIMITS.TRUNCATION_MESSAGE);
    });

    it("returns a short note when there are no files", () => {
      expect(formatAgentsDocument([])).toBe("No AGENTS.md files apply to this project.");
    });
  });
});

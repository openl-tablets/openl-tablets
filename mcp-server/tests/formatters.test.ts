/**
 * Unit tests for formatters.ts
 * Tests response formatting and pagination functions
 */

import { describe, it, expect } from "@jest/globals";
import {
  formatResponse,
  paginateResults,
  toMarkdown,
  toMarkdownConcise,
  toMarkdownDetailed,
} from "../src/formatters.js";

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

    it("should format as markdown by default", () => {
      const data = { test: "value" };
      const result = formatResponse(data, "markdown");

      expect(result).not.toMatch(/^\{/); // Should not start with JSON
      expect(result).toContain("test");
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

      if (result.length > 25000) {
        expect(result).toContain("truncated");
      }
    });

    it("should handle empty data", () => {
      const result = formatResponse([], "json");
      expect(() => JSON.parse(result)).not.toThrow();
    });

    it("should handle null data", () => {
      const result = formatResponse(null, "json");
      expect(result).toContain("null");
    });

    it("should handle undefined data", () => {
      const result = formatResponse(undefined, "json");
      expect(result).toBeDefined();
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

  describe("edge cases", () => {
    it("should handle circular references in JSON", () => {
      const circular: any = { a: 1 };
      circular.self = circular;

      // Should not throw, should handle gracefully
      expect(() => formatResponse(circular, "json")).not.toThrow();
    });

    it("should handle special characters in markdown", () => {
      const data = {
        text: "Test with **bold** and _italic_ and [link](url)",
      };

      const result = formatResponse(data, "markdown");
      expect(result).toBeDefined();
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
});

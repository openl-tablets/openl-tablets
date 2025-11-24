/**
 * Unit tests for validators.ts
 * Tests input validation functions for security and correctness
 */

import { describe, it, expect } from "@jest/globals";
import { McpError, ErrorCode } from "@modelcontextprotocol/sdk/types.js";
import {
  validateProjectId,
  validateBase64,
  validatePagination,
  validateResponseFormat,
} from "../src/validators.js";

describe("validators", () => {
  describe("validateProjectId", () => {
    it("should validate correct base64 projectId with format repository:projectName:hashCode", () => {
      // Example: "design9:Sample Project:ccedc692beec9bcfa7bfab96f76fa3af5419821d3c945da9f7ec76c6cd08e044"
      const base64Id = "ZGVzaWduOTpTYW1wbGUgUHJvamVjdDpjY2VkYzY5MmJlZWM5YmNmYTdiZmFiOTZmNzZmYTNhZjU0MTk4MjFkM2M5NDVkYTlmN2VjNzZjNmNkMDhlMDQ0";
      const result = validateProjectId(base64Id);
      expect(result).toEqual({
        repository: "design9",
        projectName: "Sample Project",
      });
    });

    it("should validate base64 projectId with simple project name", () => {
      // "design:InsuranceRules:abc123"
      const base64Id = Buffer.from("design:InsuranceRules:abc123").toString("base64");
      const result = validateProjectId(base64Id);
      expect(result).toEqual({
        repository: "design",
        projectName: "InsuranceRules",
      });
    });

    it("should validate base64 projectId with spaces in project name", () => {
      // "design:Example 1 - Bank Rating:hash123"
      const base64Id = Buffer.from("design:Example 1 - Bank Rating:hash123").toString("base64");
      const result = validateProjectId(base64Id);
      expect(result).toEqual({
        repository: "design",
        projectName: "Example 1 - Bank Rating",
      });
    });

    it("should validate base64 projectId with complex project name", () => {
      // "design:my-complex-project-name:hash456"
      const base64Id = Buffer.from("design:my-complex-project-name:hash456").toString("base64");
      const result = validateProjectId(base64Id);
      expect(result).toEqual({
        repository: "design",
        projectName: "my-complex-project-name",
      });
    });

    it("should throw error for invalid base64 format", () => {
      expect(() => validateProjectId("invalid@base64!")).toThrow(McpError);
      expect(() => validateProjectId("invalid@base64!")).toThrow(/Invalid projectId format/);
    });

    it("should throw error for base64 that doesn't decode to correct format", () => {
      // Valid base64 but invalid format (no colons)
      const invalidBase64 = Buffer.from("no-colons-here").toString("base64");
      expect(() => validateProjectId(invalidBase64)).toThrow(McpError);
      expect(() => validateProjectId(invalidBase64)).toThrow(/Invalid decoded format/);
    });

    it("should throw error for base64 with wrong number of parts", () => {
      // Only 2 parts instead of 3
      const twoPartsBase64 = Buffer.from("design:InsuranceRules").toString("base64");
      expect(() => validateProjectId(twoPartsBase64)).toThrow(McpError);
      expect(() => validateProjectId(twoPartsBase64)).toThrow(/3 parts/);
    });

    it("should throw error for empty projectId", () => {
      expect(() => validateProjectId("")).toThrow(McpError);
    });

    it("should throw error for base64 with empty parts", () => {
      // ":projectName:hashCode" - empty repository
      const emptyRepoBase64 = Buffer.from(":projectName:hashCode").toString("base64");
      expect(() => validateProjectId(emptyRepoBase64)).toThrow(McpError);
    });

    it("should include actionable error message", () => {
      expect(() => validateProjectId("invalid")).toThrow(/openl_list_projects/);
    });

    it("should accept base64 with whitespace (strips whitespace)", () => {
      // Base64 with spaces/newlines should be accepted
      const base64Id = Buffer.from("design:InsuranceRules:hash123").toString("base64");
      const withSpaces = base64Id.replace(/(.{10})/g, "$1 ");
      const result = validateProjectId(withSpaces);
      expect(result).toEqual({
        repository: "design",
        projectName: "InsuranceRules",
      });
    });
  });

  describe("validateBase64", () => {
    it("should validate correct base64 string", () => {
      const base64 = Buffer.from("Hello World").toString("base64");
      expect(validateBase64(base64)).toBe(true);
    });

    it("should validate empty base64 string", () => {
      expect(validateBase64("")).toBe(true);
    });

    it("should validate base64 with padding", () => {
      expect(validateBase64("SGVsbG8gV29ybGQ=")).toBe(true);
    });

    it("should validate base64 without padding", () => {
      expect(validateBase64("SGVsbG8gV29ybGQ")).toBe(true);
    });

    it("should validate long base64 string", () => {
      const longString = "A".repeat(10000);
      const base64 = Buffer.from(longString).toString("base64");
      expect(validateBase64(base64)).toBe(true);
    });

    it("should reject invalid base64 with special characters", () => {
      expect(validateBase64("Hello@World!")).toBe(false);
    });

    it("should accept base64 with spaces (strips whitespace)", () => {
      // Node.js Buffer.from() ignores whitespace in base64, so we should too
      expect(validateBase64("SGVs bG8=")).toBe(true);
    });

    it("should accept base64 with newlines (strips whitespace)", () => {
      // Node.js Buffer.from() ignores whitespace in base64, so we should too
      expect(validateBase64("SGVs\nbG8=")).toBe(true);
    });

    it("should accept base64 with tabs and carriage returns", () => {
      expect(validateBase64("SGVs\t\rbG8=")).toBe(true);
    });

    it("should accept multi-line formatted base64", () => {
      const multiline = "SGVsbG8g\nV29ybGQh\nVGhpcyBp\ncyBhIHRl\nc3Q=";
      expect(validateBase64(multiline)).toBe(true);
    });

    it("should reject invalid characters", () => {
      expect(validateBase64("SGVsbG8#V29ybGQ=")).toBe(false);
    });
  });

  describe("validatePagination", () => {
    it("should use defaults when no parameters provided", () => {
      const result = validatePagination();
      expect(result).toEqual({ limit: 50, offset: 0 });
    });

    it("should use provided limit and offset", () => {
      const result = validatePagination(100, 20);
      expect(result).toEqual({ limit: 100, offset: 20 });
    });

    it("should allow minimum limit of 1", () => {
      const result = validatePagination(1, 0);
      expect(result).toEqual({ limit: 1, offset: 0 });
    });

    it("should allow maximum limit of 200", () => {
      const result = validatePagination(200, 0);
      expect(result).toEqual({ limit: 200, offset: 0 });
    });

    it("should allow offset of 0", () => {
      const result = validatePagination(50, 0);
      expect(result).toEqual({ limit: 50, offset: 0 });
    });

    it("should allow large offset values", () => {
      const result = validatePagination(50, 1000);
      expect(result).toEqual({ limit: 50, offset: 1000 });
    });

    it("should use default limit when undefined", () => {
      const result = validatePagination(undefined, 20);
      expect(result).toEqual({ limit: 50, offset: 20 });
    });

    it("should use default offset when undefined", () => {
      const result = validatePagination(100, undefined);
      expect(result).toEqual({ limit: 100, offset: 0 });
    });

    it("should throw error for limit less than 1", () => {
      expect(() => validatePagination(0, 0)).toThrow(McpError);
      expect(() => validatePagination(-1, 0)).toThrow(McpError);
    });

    it("should throw error for limit greater than 200", () => {
      expect(() => validatePagination(201, 0)).toThrow(McpError);
      expect(() => validatePagination(1000, 0)).toThrow(McpError);
    });

    it("should throw error for negative offset", () => {
      expect(() => validatePagination(50, -1)).toThrow(McpError);
      expect(() => validatePagination(50, -100)).toThrow(McpError);
    });

    it("should include actionable error message for invalid limit", () => {
      expect(() => validatePagination(0, 0)).toThrow(/Set limit to a value between 1-200/);
      expect(() => validatePagination(300, 0)).toThrow(/use pagination with offset/);
    });

    it("should include actionable error message for invalid offset", () => {
      expect(() => validatePagination(50, -1)).toThrow(/Start with offset: 0/);
    });
  });

  describe("validateResponseFormat", () => {
    it("should default to markdown when no format provided", () => {
      expect(validateResponseFormat()).toBe("markdown");
    });

    it("should default to markdown when undefined", () => {
      expect(validateResponseFormat(undefined)).toBe("markdown");
    });

    it("should accept json format", () => {
      expect(validateResponseFormat("json")).toBe("json");
    });

    it("should accept markdown format", () => {
      expect(validateResponseFormat("markdown")).toBe("markdown");
    });

    it("should accept markdown_concise format", () => {
      expect(validateResponseFormat("markdown_concise")).toBe("markdown_concise");
    });

    it("should accept markdown_detailed format", () => {
      expect(validateResponseFormat("markdown_detailed")).toBe("markdown_detailed");
    });

    it("should throw error for invalid format", () => {
      expect(() => validateResponseFormat("xml")).toThrow(McpError);
      expect(() => validateResponseFormat("html")).toThrow(McpError);
      expect(() => validateResponseFormat("yaml")).toThrow(McpError);
    });

    it("should throw error for case-sensitive mismatch", () => {
      expect(() => validateResponseFormat("JSON")).toThrow(McpError);
      expect(() => validateResponseFormat("Markdown")).toThrow(McpError);
    });

    it("should include valid formats in error message", () => {
      expect(() => validateResponseFormat("invalid")).toThrow(/json, markdown, markdown_concise, markdown_detailed/);
    });

    it("should include actionable suggestion in error message", () => {
      expect(() => validateResponseFormat("invalid")).toThrow(/markdown_concise.*markdown_detailed/);
    });
  });
});

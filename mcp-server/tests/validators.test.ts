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
    it("should validate correct projectId with hyphen separator", () => {
      const result = validateProjectId("design-InsuranceRules");
      expect(result).toEqual({
        repository: "design",
        projectName: "InsuranceRules",
      });
    });

    it("should throw error for projectId with underscore separator (not supported)", () => {
      // Only hyphen is supported as separator, not underscore
      expect(() => validateProjectId("production_AutoPremium")).toThrow(McpError);
      expect(() => validateProjectId("production_AutoPremium")).toThrow(/Invalid projectId format/);
    });

    it("should validate projectId with spaces in project name", () => {
      const result = validateProjectId("design-Example 1 - Bank Rating");
      expect(result).toEqual({
        repository: "design",
        projectName: "Example 1 - Bank Rating",
      });
    });

    it("should validate projectId with multiple hyphens", () => {
      const result = validateProjectId("design-my-complex-project-name");
      expect(result).toEqual({
        repository: "design",
        projectName: "my-complex-project-name",
      });
    });

    it("should throw error for invalid format (no separator)", () => {
      expect(() => validateProjectId("invalidprojectid")).toThrow(McpError);
      expect(() => validateProjectId("invalidprojectid")).toThrow(/Invalid projectId format/);
    });

    it("should throw error for empty projectId", () => {
      expect(() => validateProjectId("")).toThrow(McpError);
    });

    it("should throw error for projectId with only separator", () => {
      expect(() => validateProjectId("-")).toThrow(McpError);
      expect(() => validateProjectId("_")).toThrow(McpError);
    });

    it("should throw error for projectId missing repository", () => {
      expect(() => validateProjectId("-ProjectName")).toThrow(McpError);
    });

    it("should throw error for projectId missing project name", () => {
      expect(() => validateProjectId("repository-")).toThrow(McpError);
    });

    it("should include actionable error message", () => {
      expect(() => validateProjectId("invalid")).toThrow(/openl_list_projects/);
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

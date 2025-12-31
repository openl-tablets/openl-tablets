/**
 * Unit tests for utils.ts
 * Tests utility functions for security, error handling, and data processing
 */

import { describe, it, expect } from "@jest/globals";
import {
  sanitizeError,
  isAxiosError,
  validateTimeout,
  safeStringify,
  extractErrorDetails,
  parseProjectId,
  createProjectId,
} from "../src/utils.js";
import { AxiosError } from "axios";

describe("utils", () => {
  describe("sanitizeError", () => {
    it("should sanitize Bearer tokens", () => {
      const error = new Error("Authorization failed: Bearer abc123def456");
      const result = sanitizeError(error);

      expect(result).toContain("Bearer [REDACTED]");
      expect(result).not.toContain("abc123");
    });


    it("should sanitize credentials in URLs", () => {
      const error = new Error("Failed to connect to http://user:password@example.com");
      const result = sanitizeError(error);

      expect(result).toContain("[REDACTED]:[REDACTED]@");
      expect(result).not.toContain("user:");
      expect(result).not.toContain(":password");
    });

    it("should sanitize client secrets", () => {
      const error = new Error("OAuth failed: client_secret=very_secret");
      const result = sanitizeError(error);

      expect(result).toContain("client_secret=[REDACTED]");
      expect(result).not.toContain("very_secret");
    });

    it("should sanitize client secrets with different formats", () => {
      const error1 = new Error('client-secret: "secret123"');
      const error2 = new Error("clientSecret=secret456");

      const result1 = sanitizeError(error1);
      const result2 = sanitizeError(error2);

      expect(result1).toContain("[REDACTED]");
      expect(result2).toContain("[REDACTED]");
    });

    it("should handle non-Error objects", () => {
      const result = sanitizeError("plain string");
      expect(result).toBe("Unknown error");
    });

    it("should handle null", () => {
      const result = sanitizeError(null);
      expect(result).toBe("Unknown error");
    });

    it("should handle undefined", () => {
      const result = sanitizeError(undefined);
      expect(result).toBe("Unknown error");
    });

    it("should preserve non-sensitive error messages", () => {
      const error = new Error("Invalid parameter value");
      const result = sanitizeError(error);

      expect(result).toBe("Invalid parameter value");
    });

    it("should handle multiple sensitive patterns", () => {
      const error = new Error(
        "Failed with Bearer token123 at http://user:pass@host"
      );
      const result = sanitizeError(error);

      expect(result).toContain("Bearer [REDACTED]");
      expect(result).toContain("[REDACTED]:[REDACTED]@");
      expect(result).not.toContain("token123");
      expect(result).not.toContain("user:");
      expect(result).not.toContain(":pass");
    });
  });

  describe("isAxiosError", () => {
    it("should identify Axios errors", () => {
      const axiosError = {
        isAxiosError: true,
        message: "Request failed",
        config: {},
      };

      expect(isAxiosError(axiosError)).toBe(true);
    });

    it("should reject regular errors", () => {
      const error = new Error("Not an axios error");
      expect(isAxiosError(error)).toBe(false);
    });

    it("should reject objects without isAxiosError flag", () => {
      const notAxiosError = {
        message: "Some error",
        config: {},
      };

      expect(isAxiosError(notAxiosError)).toBe(false);
    });

    it("should reject null", () => {
      expect(isAxiosError(null)).toBe(false);
    });

    it("should reject undefined", () => {
      expect(isAxiosError(undefined)).toBe(false);
    });

    it("should reject primitives", () => {
      expect(isAxiosError("string")).toBe(false);
      expect(isAxiosError(123)).toBe(false);
      expect(isAxiosError(true)).toBe(false);
    });

    it("should reject objects with isAxiosError = false", () => {
      const notAxiosError = {
        isAxiosError: false,
        message: "Not an axios error",
      };

      expect(isAxiosError(notAxiosError)).toBe(false);
    });
  });

  describe("validateTimeout", () => {
    it("should return default when timeout is undefined", () => {
      const result = validateTimeout(undefined, 30000);
      expect(result).toBe(30000);
    });

    it("should return valid timeout", () => {
      const result = validateTimeout(60000, 30000);
      expect(result).toBe(60000);
    });

    it("should cap at maximum timeout", () => {
      const result = validateTimeout(9999999, 30000);
      expect(result).toBe(600000); // Max is 600000 (10 minutes)
    });

    it("should return default for negative timeout", () => {
      const result = validateTimeout(-1000, 30000);
      expect(result).toBe(30000);
    });

    it("should return default for zero timeout", () => {
      const result = validateTimeout(0, 30000);
      expect(result).toBe(30000);
    });

    it("should return default for NaN", () => {
      const result = validateTimeout(NaN, 30000);
      expect(result).toBe(30000);
    });

    it("should accept minimum valid timeout", () => {
      const result = validateTimeout(1, 30000);
      expect(result).toBe(1);
    });

    it("should accept exactly max timeout", () => {
      const result = validateTimeout(600000, 30000);
      expect(result).toBe(600000);
    });

    it("should handle very small valid timeouts", () => {
      const result = validateTimeout(100, 30000);
      expect(result).toBe(100);
    });
  });

  describe("safeStringify", () => {
    it("should stringify simple objects", () => {
      const obj = { name: "test", value: 123 };
      const result = safeStringify(obj);

      expect(result).toBe('{"name":"test","value":123}');
    });

    it("should handle circular references", () => {
      const obj: any = { name: "test" };
      obj.self = obj;

      const result = safeStringify(obj);

      expect(result).toContain("[Circular]");
      expect(result).toContain("name");
    });

    it("should handle nested circular references", () => {
      const obj: any = {
        level1: {
          level2: {
            name: "test",
          },
        },
      };
      obj.level1.level2.back = obj;

      const result = safeStringify(obj);

      expect(result).toContain("[Circular]");
      expect(result).toContain("level1");
    });

    it("should format with indentation when space is provided", () => {
      const obj = { name: "test", value: 123 };
      const result = safeStringify(obj, 2);

      expect(result).toContain("\n");
      expect(result).toContain("  ");
    });

    it("should handle arrays", () => {
      const arr = [1, 2, 3];
      const result = safeStringify(arr);

      expect(result).toBe("[1,2,3]");
    });

    it("should handle nested arrays and objects", () => {
      const complex = {
        array: [1, 2, { nested: true }],
        object: { key: "value" },
      };

      const result = safeStringify(complex);

      expect(result).toContain("array");
      expect(result).toContain("nested");
    });

    it("should handle null values", () => {
      const obj = { value: null };
      const result = safeStringify(obj);

      expect(result).toBe('{"value":null}');
    });

    it("should handle undefined values", () => {
      const obj = { value: undefined };
      const result = safeStringify(obj);

      // JSON.stringify omits undefined values
      expect(result).toBe("{}");
    });

    it("should handle mixed circular references in array", () => {
      const obj: any = { name: "test" };
      const arr: any = [obj, obj];
      obj.arr = arr;

      const result = safeStringify(obj);

      expect(result).toContain("[Circular]");
    });
  });

  describe("extractErrorDetails", () => {
    it("should extract Axios error details", () => {
      const axiosError = {
        isAxiosError: true,
        message: "Request failed",
        code: "ECONNREFUSED",
        response: { status: 500 },
      } as any;

      const result = extractErrorDetails(axiosError);

      expect(result.type).toBe("AxiosError");
      expect(result.code).toBe("ECONNREFUSED");
      expect(result.status).toBe(500);
      expect(result.message).toBeDefined();
    });

    it("should extract regular Error details", () => {
      const error = new TypeError("Invalid type");
      const result = extractErrorDetails(error);

      expect(result.type).toBe("TypeError");
      expect(result.message).toContain("Invalid type");
    });

    it("should handle unknown error types", () => {
      const result = extractErrorDetails("string error");

      expect(result.type).toBe("Unknown");
      expect(result.message).toBe("An unknown error occurred");
    });

    it("should sanitize sensitive data in Axios errors", () => {
      // Create a proper Error instance that will pass isAxiosError check
      const axiosError = new Error("Failed with Bearer token123") as any;
      axiosError.isAxiosError = true;
      axiosError.code = "ERR_UNAUTHORIZED";

      const result = extractErrorDetails(axiosError);

      expect(result.message).toContain("[REDACTED]");
      expect(result.message).not.toContain("token123");
    });

    it("should handle Axios errors without response", () => {
      const axiosError = {
        isAxiosError: true,
        message: "Network error",
        code: "ENETUNREACH",
      } as any;

      const result = extractErrorDetails(axiosError);

      expect(result.type).toBe("AxiosError");
      expect(result.status).toBeUndefined();
    });

    it("should handle null", () => {
      const result = extractErrorDetails(null);

      expect(result.type).toBe("Unknown");
    });

    it("should preserve error name for custom errors", () => {
      class CustomError extends Error {
        constructor(message: string) {
          super(message);
          this.name = "CustomError";
        }
      }

      const error = new CustomError("Custom message");
      const result = extractErrorDetails(error);

      expect(result.type).toBe("CustomError");
    });
  });

  describe("parseProjectId", () => {
    it("should parse object format", () => {
      const id = { repository: "design", projectName: "Project1" };
      const result = parseProjectId(id);

      expect(result).toEqual({
        repository: "design",
        projectName: "Project1",
      });
    });

    it("should parse base64-encoded string", () => {
      const encoded = Buffer.from("design:Project1").toString("base64");
      const result = parseProjectId(encoded);

      expect(result).toEqual({
        repository: "design",
        projectName: "Project1",
      });
    });

    it("should parse plain colon-separated string", () => {
      const result = parseProjectId("design:Project1");

      expect(result).toEqual({
        repository: "design",
        projectName: "Project1",
      });
    });

    it("should handle project names with spaces", () => {
      const encoded = Buffer.from("design:Example 1 - Bank Rating").toString("base64");
      const result = parseProjectId(encoded);

      expect(result).toEqual({
        repository: "design",
        projectName: "Example 1 - Bank Rating",
      });
    });

    it("should handle plain string with spaces in project name", () => {
      const result = parseProjectId("design:Example 1 - Bank Rating");

      expect(result).toEqual({
        repository: "design",
        projectName: "Example 1 - Bank Rating",
      });
    });

    it("should throw error for string without colon", () => {
      expect(() => parseProjectId("invalid")).toThrow(/Invalid project ID format/);
    });

    it("should throw error for empty repository", () => {
      expect(() => parseProjectId(":ProjectName")).toThrow(/Invalid project ID format/);
    });

    it("should throw error for empty project name", () => {
      expect(() => parseProjectId("repository:")).toThrow(/Invalid project ID format/);
    });

    it("should throw error for invalid type", () => {
      expect(() => parseProjectId(123 as any)).toThrow(/Invalid project ID type/);
    });

    it("should throw error for null", () => {
      expect(() => parseProjectId(null as any)).toThrow(/Invalid project ID type/);
    });

    it("should handle multiple colons in project name", () => {
      const result = parseProjectId("design:Project:With:Colons");

      expect(result).toEqual({
        repository: "design",
        projectName: "Project:With:Colons",
      });
    });

    it("should handle special characters in project name", () => {
      const encoded = Buffer.from("design:Project #1 (Test)").toString("base64");
      const result = parseProjectId(encoded);

      expect(result.projectName).toBe("Project #1 (Test)");
    });

    it("should handle unicode in project name", () => {
      const encoded = Buffer.from("design:项目名称").toString("base64");
      const result = parseProjectId(encoded);

      expect(result.projectName).toBe("项目名称");
    });
  });

  describe("createProjectId", () => {
    it("should create project ID with hyphen separator", () => {
      const result = createProjectId("design", "Project1");
      expect(result).toBe("design-Project1");
    });

    it("should handle project names with spaces", () => {
      const result = createProjectId("design", "Example 1 - Bank Rating");
      expect(result).toBe("design-Example 1 - Bank Rating");
    });

    it("should handle special characters", () => {
      const result = createProjectId("production", "Project #1 (Test)");
      expect(result).toBe("production-Project #1 (Test)");
    });

    it("should handle empty strings", () => {
      const result = createProjectId("", "");
      expect(result).toBe("-");
    });

    it("should handle unicode", () => {
      const result = createProjectId("design", "项目名称");
      expect(result).toBe("design-项目名称");
    });

    it("should handle project names containing hyphens", () => {
      const result = createProjectId("design", "my-project-name");
      expect(result).toBe("design-my-project-name");
    });
  });

  describe("integration scenarios", () => {
    it("should round-trip sanitize and extract error", () => {
      const error = new Error("Failed with Bearer secret123");
      const sanitized = sanitizeError(error);

      expect(sanitized).toContain("[REDACTED]");
      expect(sanitized).not.toContain("secret123");
    });

    it("should handle complex error with multiple sensitive fields", () => {
      const error = new Error(
        "OAuth failed: client_secret=abc123 Bearer token456 at http://user:pass@host"
      );
      const details = extractErrorDetails(error);

      expect(details.message).toContain("[REDACTED]");
      expect(details.message).not.toContain("abc123");
      expect(details.message).not.toContain("token456");
      expect(details.message).not.toContain("user:");
    });

    it("should stringify object with circular reference after error extraction", () => {
      const circular: any = { name: "test" };
      circular.self = circular;

      const stringified = safeStringify(circular);
      expect(stringified).toContain("[Circular]");
    });
  });
});

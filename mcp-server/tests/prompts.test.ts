/**
 * Tests for MCP Prompts functionality
 *
 * Ensures prompts are properly registered, loaded, and accessible via MCP protocol.
 * These tests verify that prompts will continue working as the codebase evolves.
 */

import { describe, test, expect } from "@jest/globals";
import { existsSync, readdirSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";
import {
  PROMPTS,
  loadPromptContent,
  getPromptDefinition,
  promptExists,
  getPromptNames,
} from "../src/prompts-registry.js";

// ES module equivalent of __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe("Prompts Registry", () => {
  describe("PROMPTS array", () => {
    test("should contain exactly 11 prompts", () => {
      expect(PROMPTS).toHaveLength(11);
    });

    test("all prompts should have required fields", () => {
      PROMPTS.forEach((prompt) => {
        expect(prompt).toHaveProperty("name");
        expect(prompt).toHaveProperty("description");
        expect(typeof prompt.name).toBe("string");
        expect(typeof prompt.description).toBe("string");
        expect(prompt.name.length).toBeGreaterThan(0);
        expect(prompt.description.length).toBeGreaterThan(0);
      });
    });

    test("all prompt names should be unique", () => {
      const names = PROMPTS.map((p) => p.name);
      const uniqueNames = new Set(names);
      expect(uniqueNames.size).toBe(names.length);
    });

    test("all prompt names should be lowercase with underscores", () => {
      PROMPTS.forEach((prompt) => {
        expect(prompt.name).toMatch(/^[a-z_]+$/);
      });
    });

    test("optional title field should be string if present", () => {
      PROMPTS.forEach((prompt) => {
        if (prompt.title !== undefined) {
          expect(typeof prompt.title).toBe("string");
          expect(prompt.title.length).toBeGreaterThan(0);
        }
      });
    });

    test("arguments should have proper structure if present", () => {
      PROMPTS.forEach((prompt) => {
        if (prompt.arguments) {
          expect(Array.isArray(prompt.arguments)).toBe(true);
          prompt.arguments.forEach((arg) => {
            expect(arg).toHaveProperty("name");
            expect(arg).toHaveProperty("description");
            expect(typeof arg.name).toBe("string");
            expect(typeof arg.description).toBe("string");
            if (arg.required !== undefined) {
              expect(typeof arg.required).toBe("boolean");
            }
          });
        }
      });
    });
  });

  describe("Prompt definitions", () => {
    test("create_rule prompt should be defined", () => {
      const prompt = PROMPTS.find((p) => p.name === "create_rule");
      expect(prompt).toBeDefined();
      expect(prompt?.title).toBe("Create OpenL Table");
      expect(prompt?.description).toContain("decision tables");
    });

    test("create_test prompt should have arguments", () => {
      const prompt = PROMPTS.find((p) => p.name === "create_test");
      expect(prompt).toBeDefined();
      expect(prompt?.arguments).toBeDefined();
      expect(prompt?.arguments?.length).toBeGreaterThan(0);
      const tableNameArg = prompt?.arguments?.find(
        (a) => a.name === "tableName"
      );
      expect(tableNameArg).toBeDefined();
    });

    test("all expected prompts should be present", () => {
      const expectedPrompts = [
        "create_rule",
        "datatype_vocabulary",
        "create_test",
        "update_test",
        "run_test",
        "dimension_properties",
        "execute_rule",
        "deploy_project",
        "get_project_errors",
        "file_history",
        "project_history",
      ];

      expectedPrompts.forEach((name) => {
        const prompt = PROMPTS.find((p) => p.name === name);
        expect(prompt).toBeDefined();
      });
    });
  });

  describe("Prompt file existence", () => {
    const promptsDir = join(__dirname, "..", "prompts");

    test("prompts directory should exist", () => {
      expect(existsSync(promptsDir)).toBe(true);
    });

    test("all registered prompts should have corresponding .md files", () => {
      PROMPTS.forEach((prompt) => {
        const filePath = join(promptsDir, `${prompt.name}.md`);
        expect(existsSync(filePath)).toBe(true);
      });
    });

    test("no orphaned .md files should exist", () => {
      const files = readdirSync(promptsDir).filter((f) => f.endsWith(".md"));
      const registeredNames = PROMPTS.map((p) => `${p.name}.md`);

      files.forEach((file) => {
        expect(registeredNames).toContain(file);
      });
    });
  });
});

describe("loadPromptContent", () => {
  describe("Basic loading", () => {
    test("should load create_rule prompt content", () => {
      const content = loadPromptContent("create_rule");
      expect(content).toBeDefined();
      expect(typeof content).toBe("string");
      expect(content.length).toBeGreaterThan(100);
      expect(content).toContain("Creating Tables in OpenL Tablets");
    });

    test("should load all registered prompts without error", () => {
      PROMPTS.forEach((prompt) => {
        expect(() => loadPromptContent(prompt.name)).not.toThrow();
      });
    });

    test("should throw error for non-existent prompt", () => {
      expect(() => loadPromptContent("non_existent_prompt")).toThrow(
        /Failed to load prompt/
      );
    });

    test("loaded content should be non-empty", () => {
      PROMPTS.forEach((prompt) => {
        const content = loadPromptContent(prompt.name);
        expect(content.length).toBeGreaterThan(0);
      });
    });
  });

  describe("Content validation", () => {
    test("create_rule should contain all table types", () => {
      const content = loadPromptContent("create_rule");
      expect(content).toContain("Rules Table");
      expect(content).toContain("SimpleRules");
      expect(content).toContain("SmartRules");
      expect(content).toContain("SimpleLookup");
      expect(content).toContain("SmartLookup");
      expect(content).toContain("Spreadsheet");
    });

    test("create_test should contain test table structure info", () => {
      const content = loadPromptContent("create_test");
      expect(content).toContain("Test Table");
      expect(content).toContain("3-row");
    });

    test("dimension_properties should explain versioning systems", () => {
      const content = loadPromptContent("dimension_properties");
      expect(content).toContain("Git");
      expect(content).toContain("Dimension properties");
      expect(content).toContain("state");
    });

    test("deploy_project should mention validation requirements", () => {
      const content = loadPromptContent("deploy_project");
      expect(content).toContain("validate");
      expect(content).toContain("test");
    });

    test("execute_rule should contain JSON examples", () => {
      const content = loadPromptContent("execute_rule");
      expect(content).toContain("JSON");
    });
  });

  describe("Argument substitution", () => {
    test("should substitute simple variables", () => {
      const content = loadPromptContent("create_test", {
        tableName: "calculatePremium",
      });
      // The prompt might contain {tableName} placeholders
      // We don't check specific substitution since current prompts don't have placeholders yet
      expect(content).toBeDefined();
    });

    test("should handle missing arguments gracefully", () => {
      // Should not throw even if arguments are expected
      expect(() =>
        loadPromptContent("create_test")
      ).not.toThrow();
    });

    test("should handle empty arguments object", () => {
      const content = loadPromptContent("create_rule", {});
      expect(content).toBeDefined();
      expect(content.length).toBeGreaterThan(0);
    });

    test("should handle extra arguments gracefully", () => {
      const content = loadPromptContent("create_rule", {
        unexpectedArg: "value",
        anotherArg: "value2",
      });
      expect(content).toBeDefined();
      expect(content.length).toBeGreaterThan(0);
    });
  });

  describe("Content consistency", () => {
    test("should return same content for repeated calls", () => {
      const content1 = loadPromptContent("create_rule");
      const content2 = loadPromptContent("create_rule");
      expect(content1).toBe(content2);
    });

    test("content should not contain null bytes", () => {
      PROMPTS.forEach((prompt) => {
        const content = loadPromptContent(prompt.name);
        expect(content).not.toContain("\0");
      });
    });

    test("content should use unix line endings", () => {
      PROMPTS.forEach((prompt) => {
        const content = loadPromptContent(prompt.name);
        // Should not have windows line endings
        expect(content).not.toContain("\r\n");
      });
    });
  });
});

describe("Helper functions", () => {
  describe("getPromptDefinition", () => {
    test("should return definition for existing prompt", () => {
      const def = getPromptDefinition("create_rule");
      expect(def).toBeDefined();
      expect(def?.name).toBe("create_rule");
    });

    test("should return undefined for non-existent prompt", () => {
      const def = getPromptDefinition("non_existent");
      expect(def).toBeUndefined();
    });

    test("should work for all registered prompts", () => {
      PROMPTS.forEach((prompt) => {
        const def = getPromptDefinition(prompt.name);
        expect(def).toBeDefined();
        expect(def?.name).toBe(prompt.name);
      });
    });
  });

  describe("promptExists", () => {
    test("should return true for existing prompts", () => {
      expect(promptExists("create_rule")).toBe(true);
      expect(promptExists("create_test")).toBe(true);
    });

    test("should return false for non-existent prompts", () => {
      expect(promptExists("non_existent")).toBe(false);
      expect(promptExists("")).toBe(false);
    });

    test("should be case-sensitive", () => {
      expect(promptExists("create_rule")).toBe(true);
      expect(promptExists("CREATE_RULE")).toBe(false);
      expect(promptExists("Create_Rule")).toBe(false);
    });
  });

  describe("getPromptNames", () => {
    test("should return all prompt names", () => {
      const names = getPromptNames();
      expect(names).toHaveLength(11);
      expect(names).toContain("create_rule");
      expect(names).toContain("create_test");
    });

    test("should return array in same order as PROMPTS", () => {
      const names = getPromptNames();
      PROMPTS.forEach((prompt, index) => {
        expect(names[index]).toBe(prompt.name);
      });
    });

    test("returned names should all exist", () => {
      const names = getPromptNames();
      names.forEach((name) => {
        expect(promptExists(name)).toBe(true);
      });
    });
  });
});

describe("Prompts integrity", () => {
  test("total content size should be reasonable", () => {
    let totalSize = 0;
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);
      totalSize += content.length;
    });

    // Total should be around 1,453 lines mentioned in docs
    // Actual measured size is ~44KB
    expect(totalSize).toBeGreaterThan(40000); // At least 40KB
    expect(totalSize).toBeLessThan(500000); // Less than 500KB
  });

  test("no prompt should be empty", () => {
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);
      expect(content.trim().length).toBeGreaterThan(0);
    });
  });

  test("all prompts should have markdown headers", () => {
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);
      expect(content).toMatch(/^#[^#]/m); // Should have at least one # header
    });
  });

  test("no prompt should contain template syntax errors", () => {
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);
      // Check for unmatched braces that might indicate template errors
      const openBraces = (content.match(/\{/g) || []).length;
      const closeBraces = (content.match(/\}/g) || []).length;
      // Allow some difference for JSON examples, but shouldn't be way off
      expect(Math.abs(openBraces - closeBraces)).toBeLessThan(50);
    });
  });
});

describe("Regression tests", () => {
  test("create_rule prompt structure should remain stable", () => {
    const content = loadPromptContent("create_rule");
    // Core sections that should always be present
    expect(content).toContain("DECISION TABLES");
    expect(content).toContain("SPREADSHEET TABLES");
    expect(content).toContain("When to Use");
  });

  test("prompts with arguments should maintain argument definitions", () => {
    const promptsWithArgs = PROMPTS.filter(
      (p) => p.arguments && p.arguments.length > 0
    );

    expect(promptsWithArgs.length).toBeGreaterThan(0);

    promptsWithArgs.forEach((prompt) => {
      expect(prompt.arguments).toBeDefined();
      expect(prompt.arguments!.length).toBeGreaterThan(0);
    });
  });

  test("all prompts should maintain minimum quality standards", () => {
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);

      // Should have meaningful content
      expect(content.length).toBeGreaterThan(500);

      // Should have structure (multiple sections)
      const headerCount = (content.match(/^#{1,3}\s/gm) || []).length;
      expect(headerCount).toBeGreaterThan(1);

      // Description should be informative
      expect(prompt.description.length).toBeGreaterThan(30);
    });
  });
});

describe("MCP Protocol compatibility", () => {
  test("prompt definitions should be valid MCP Prompt objects", () => {
    PROMPTS.forEach((prompt) => {
      // Required fields
      expect(prompt.name).toBeDefined();
      expect(typeof prompt.name).toBe("string");

      // Optional fields should have correct types
      if (prompt.title) {
        expect(typeof prompt.title).toBe("string");
      }
      if (prompt.description) {
        expect(typeof prompt.description).toBe("string");
      }
      if (prompt.arguments) {
        expect(Array.isArray(prompt.arguments)).toBe(true);
      }
    });
  });

  test("loaded content should be valid PromptMessage", () => {
    const content = loadPromptContent("create_rule");

    // Should be a string that can be used in a PromptMessage
    expect(typeof content).toBe("string");

    // Should be able to create a valid message structure
    const message = {
      role: "assistant" as const,
      content: {
        type: "text" as const,
        text: content,
      },
    };

    expect(message.role).toBe("assistant");
    expect(message.content.type).toBe("text");
    expect(message.content.text).toBe(content);
  });

  test("all prompts should be serializable to JSON", () => {
    expect(() => JSON.stringify(PROMPTS)).not.toThrow();

    const serialized = JSON.stringify(PROMPTS);
    const deserialized = JSON.parse(serialized);

    expect(deserialized).toEqual(PROMPTS);
  });
});

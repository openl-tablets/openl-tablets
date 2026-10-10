/**
 * Tests for MCP Prompts functionality
 *
 * Ensures prompts are properly registered, loaded, and accessible via MCP protocol.
 * These tests verify that prompts will continue working as the codebase evolves.
 */

import { describe, test, expect } from "@jest/globals";
import { readdirSync, readFileSync, writeFileSync } from "fs";
import { basename, join, dirname } from "path";
import { fileURLToPath } from "url";
import YAML from "yaml";
import {
  PROMPTS,
  loadPromptContent,
  getPromptDefinition,
  promptExists,
  buildPromptDefinition,
} from "../src/prompts-registry.js";

// ES module equivalent of __dirname
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

describe("Prompts Registry", () => {
  describe("PROMPTS array", () => {
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
  });

  describe("Prompt definitions", () => {
    // Cross-validates the registry discovered from the prompts/ directory
    // against an independent hand-maintained list, so adding or removing a
    // prompt file without updating this list fails the build.
    test("registry contains exactly the expected prompts", () => {
      const expectedPrompts = [
        "append_table",
        "create_rule",
        "create_rule_decision_tables",
        "create_rule_spreadsheet",
        "create_test",
        "datatype_vocabulary",
        "deploy_project",
        "dimension_properties",
        "local_projects",
        "project_agents_md",
        "project_history",
        "run_test",
        "update_test",
        "validate_after_edit",
      ];

      expect(PROMPTS.map((p) => p.name).sort()).toEqual(
        [...expectedPrompts].sort()
      );
    });
  });

  describe("Discovery from prompts directory", () => {
    const promptsDir = join(__dirname, "..", "prompts");

    test("registry has one entry per .md file, named after the file", () => {
      const fileNames = readdirSync(promptsDir)
        .filter((f) => f.endsWith(".md"))
        .map((f) => basename(f, ".md"))
        .sort();

      expect(PROMPTS.map((p) => p.name).sort()).toEqual(fileNames);
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
  });

  describe("Argument substitution", () => {
    test("should substitute simple variables", () => {
      const content = loadPromptContent("create_test", {
        tableName: "calculatePremium",
      });
      expect(content).toContain("calculatePremium");
      expect(content).not.toContain("{tableName}");
    });

    test("should handle missing arguments gracefully", () => {
      // Should not throw even if arguments are expected
      expect(() =>
        loadPromptContent("create_test")
      ).not.toThrow();
    });

    test("should handle extra arguments gracefully", () => {
      const content = loadPromptContent("create_rule", {
        unexpectedArg: "value",
        anotherArg: "value2",
      });
      expect(content).toBeDefined();
      expect(content.length).toBeGreaterThan(0);
    });

    test("argument names with regex metacharacters do not break substitution", () => {
      // Exercises escapeRegex(): without escaping, building a pattern like
      // `new RegExp("\\{a[b\\}")` throws "Invalid regular expression:
      // Unterminated character class".
      expect(() =>
        loadPromptContent("create_rule", { "a[b": "x", "name(1)*": "y" })
      ).not.toThrow();
    });
  });

  describe("Content consistency", () => {
    test("should return same content for repeated calls", () => {
      const content1 = loadPromptContent("create_rule");
      const content2 = loadPromptContent("create_rule");
      expect(content1).toBe(content2);
    });
  });
});

describe("Helper functions", () => {
  describe("getPromptDefinition", () => {
    test("surfaces title and description from the file frontmatter", () => {
      const def = getPromptDefinition("create_rule");
      expect(def?.name).toBe("create_rule");
      expect(def?.title).toBe("Create OpenL Table");
      expect(def?.description).toContain("Comprehensive guide for creating");
    });

    test("surfaces declared arguments from the file frontmatter", () => {
      const def = getPromptDefinition("create_test");
      expect(def?.arguments?.map((a) => a.name)).toEqual([
        "tableName",
        "tableType",
      ]);
    });

    test("should return undefined for non-existent prompt", () => {
      const def = getPromptDefinition("non_existent");
      expect(def).toBeUndefined();
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

});

describe("Prompts integrity", () => {
  test("no prompt should be empty", () => {
    PROMPTS.forEach((prompt) => {
      const content = loadPromptContent(prompt.name);
      expect(content.trim().length).toBeGreaterThan(0);
    });
  });
});

describe("Frontmatter support (Approach 2)", () => {
  describe("YAML frontmatter parsing", () => {
    test("loadPromptContent should strip frontmatter from output", () => {
      PROMPTS.forEach((prompt) => {
        const content = loadPromptContent(prompt.name);

        // Content should not start with ---
        expect(content.startsWith("---")).toBe(false);

        // Should not contain frontmatter markers in the body
        expect(content.startsWith("#")).toBe(true); // Should start with markdown header
      });
    });
  });

  describe("Argument substitution with frontmatter", () => {
    // Use distinctive markers so the assertions test substitution mechanics,
    // not the surrounding prose (which is free to change).
    test("substitutes a single argument and leaves no placeholder", () => {
      const content = loadPromptContent("create_test", {
        tableName: "Zmarker_TableName",
      });

      expect(content).toContain("Zmarker_TableName");
      expect(content).not.toContain("{tableName}");
    });

    test("substitutes multiple arguments independently", () => {
      const content = loadPromptContent("create_test", {
        tableName: "Zmarker_Name",
        tableType: "Zmarker_Type",
      });

      expect(content).toContain("Zmarker_Name");
      expect(content).toContain("Zmarker_Type");
      expect(content).not.toContain("{tableName}");
      expect(content).not.toContain("{tableType}");
    });

    test("substitutes arguments in deploy_project", () => {
      const content = loadPromptContent("deploy_project", {
        projectId: "Zmarker_Project",
        environment: "Zmarker_Env",
      });

      expect(content).toContain("Zmarker_Project");
      expect(content).toContain("Zmarker_Env");
      expect(content).not.toContain("{projectId}");
      expect(content).not.toContain("{environment}");
    });

    test("removes a conditional block when its argument is absent, keeps it when present", () => {
      // Argument absent: the {if ...} block and its markers are stripped.
      const withoutArg = loadPromptContent("create_test");
      expect(withoutArg).not.toMatch(/\{if \w+\}/);
      expect(withoutArg).not.toContain("{end if}");

      // Argument present: the value (injected from inside the block) survives,
      // and no conditional markers remain.
      const withArg = loadPromptContent("create_test", {
        tableName: "Zmarker_Conditional",
      });
      expect(withArg).toContain("Zmarker_Conditional");
      expect(withArg).not.toMatch(/\{if \w+\}/);
      expect(withArg).not.toContain("{end if}");
    });
  });

  describe("Backward compatibility", () => {
    test("prompts without arguments should work unchanged", () => {
      const promptsWithoutArgs = ["create_rule", "datatype_vocabulary", "dimension_properties"];

      promptsWithoutArgs.forEach((promptName) => {
        expect(() => loadPromptContent(promptName)).not.toThrow();
        const content = loadPromptContent(promptName);
        expect(content.length).toBeGreaterThan(0);
      });
    });

    test("prompts with optional arguments should work without arguments", () => {
      const promptsWithOptionalArgs = ["create_test", "deploy_project"];

      promptsWithOptionalArgs.forEach((promptName) => {
        expect(() => loadPromptContent(promptName)).not.toThrow();
        const content = loadPromptContent(promptName);
        expect(content.length).toBeGreaterThan(0);

        // Should not have unprocessed placeholders (except in code examples)
        const placeholders = content.match(/\{if \w+\}/g);
        expect(placeholders).toBeNull(); // All should be processed
      });
    });

    test("should handle partial arguments gracefully", () => {
      // Provide only one of multiple arguments
      const content = loadPromptContent("create_test", {
        tableName: "testRule",
        // tableType omitted
      });

      expect(content).toContain("testRule");
      expect(content).not.toContain("{tableName}"); // Should be substituted
      // tableType conditional should be removed since not provided
    });
  });

});

describe("buildPromptDefinition", () => {
  test("resolves the name from the supplied filename, not the frontmatter", () => {
    const def = buildPromptDefinition("from_file", {
      title: "Some Title",
    });
    expect(def.name).toBe("from_file");
  });

  test("includes title, description, and arguments when present", () => {
    const def = buildPromptDefinition("full", {
      title: "Full Prompt",
      description: "Does everything",
      arguments: [{ name: "id", description: "the id", required: false }],
    });
    expect(def).toEqual({
      name: "full",
      title: "Full Prompt",
      description: "Does everything",
      arguments: [{ name: "id", description: "the id", required: false }],
    });
  });

  test("omits the title when the frontmatter has none", () => {
    const def = buildPromptDefinition("untitled", {
      description: "No title here",
    });
    expect(def).toEqual({ name: "untitled", description: "No title here" });
    expect("title" in def).toBe(false);
  });

  test("omits arguments when the frontmatter declares an empty list", () => {
    const def = buildPromptDefinition("no_args", {
      title: "No Args",
      arguments: [],
    });
    expect("arguments" in def).toBe(false);
  });

  test("returns a name-only definition when there is no frontmatter", () => {
    expect(buildPromptDefinition("bare", null)).toEqual({ name: "bare" });
  });
});

describe("Frontmatter is the single source of truth", () => {
  const promptsDir = join(__dirname, "..", "prompts");

  function readFrontmatter(name: string): Record<string, unknown> {
    const raw = readFileSync(join(promptsDir, `${name}.md`), "utf-8");
    const match = raw.match(/^---\n([\s\S]*?)\n---\n/);
    if (!match) {
      throw new Error(`Prompt '${name}' has no YAML frontmatter`);
    }
    return YAML.parse(match[1]);
  }

  // The name is derived from the filename, so it must not be duplicated in the
  // frontmatter where it could silently drift out of sync.
  test("no prompt frontmatter declares a name key", () => {
    PROMPTS.forEach((prompt) => {
      expect(readFrontmatter(prompt.name)).not.toHaveProperty("name");
    });
  });
});

describe("Prompt content caching", () => {
  const promptsDir = join(__dirname, "..", "prompts");

  test("serves the body cached at startup instead of re-reading the file", () => {
    // local_projects.md was read and cached when the module loaded. Overwrite
    // it on disk: loadPromptContent must still return the cached body, proving
    // the file is not re-read on each call. The original is restored afterward.
    const file = join(promptsDir, "local_projects.md");
    const original = readFileSync(file, "utf-8");
    try {
      writeFileSync(file, "---\ntitle: Mutated\n---\n# MUTATED ON DISK\n");
      expect(loadPromptContent("local_projects")).not.toContain(
        "MUTATED ON DISK"
      );
    } finally {
      writeFileSync(file, original);
    }
  });
});

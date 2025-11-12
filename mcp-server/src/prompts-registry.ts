/**
 * MCP Prompts Registry
 *
 * Registers and loads prompt templates from the prompts/ directory.
 * Prompts provide expert guidance for OpenL Tablets workflows.
 */

import { readFileSync } from "fs";
import { join, dirname } from "path";
import { fileURLToPath } from "url";

/**
 * MCP Prompt definition
 */
export interface PromptDefinition {
  /** Unique identifier for the prompt */
  name: string;
  /** Human-readable title for display */
  title?: string;
  /** Description of what this prompt helps with */
  description?: string;
  /** Optional arguments that can be passed to the prompt */
  arguments?: Array<{
    name: string;
    description: string;
    required?: boolean;
  }>;
}

/**
 * Registry of all available prompts
 *
 * Each prompt corresponds to a markdown file in the prompts/ directory.
 * Prompts provide contextual guidance for complex OpenL Tablets workflows.
 */
export const PROMPTS: PromptDefinition[] = [
  {
    name: "create_rule",
    title: "Create OpenL Table",
    description: "Comprehensive guide for creating OpenL decision tables, spreadsheets, and datatypes with examples for all table types (Rules, SimpleRules, SmartRules, SimpleLookup, SmartLookup, Spreadsheet)",
  },
  {
    name: "datatype_vocabulary",
    title: "Define Datatypes and Vocabularies",
    description: "Guide for creating custom datatypes (domain objects) and vocabularies (enumerations) in OpenL Tablets with inheritance, field types, and validation",
  },
  {
    name: "create_test",
    title: "Create Test Table",
    description: "Step-by-step guide for creating OpenL test tables with proper 3-row structure, test case design, and expected value validation",
    arguments: [
      {
        name: "tableName",
        description: "Name of the table being tested",
        required: false,
      },
      {
        name: "tableType",
        description: "Type of table being tested (Rules, SimpleRules, Spreadsheet, etc.)",
        required: false,
      },
    ],
  },
  {
    name: "update_test",
    title: "Update Test Table",
    description: "Guide for modifying existing test tables, adding test cases, updating expected values, and handling test failures",
    arguments: [
      {
        name: "testId",
        description: "ID of the test table to update",
        required: false,
      },
      {
        name: "tableName",
        description: "Name of the table being tested",
        required: false,
      },
    ],
  },
  {
    name: "run_test",
    title: "Run Tests",
    description: "Test selection logic and workflow for running OpenL tests efficiently based on scope (single table, multiple tables, or all tests)",
    arguments: [
      {
        name: "scope",
        description: "Test scope: 'single', 'multiple', or 'all'",
        required: false,
      },
      {
        name: "tableIds",
        description: "Comma-separated list of table IDs being tested",
        required: false,
      },
    ],
  },
  {
    name: "dimension_properties",
    title: "Dimension Properties",
    description: "Explanation of OpenL dimension properties for business versioning (state, country, lob, effectiveDate) vs Git versioning, with runtime selection logic",
  },
  {
    name: "execute_rule",
    title: "Execute Rule",
    description: "Guide for constructing test data and executing OpenL rules with proper JSON formatting for simple types, complex objects, and arrays",
    arguments: [
      {
        name: "ruleName",
        description: "Name of the rule to execute",
        required: false,
      },
      {
        name: "projectId",
        description: "ID of the project containing the rule",
        required: false,
      },
    ],
  },
  {
    name: "deploy_project",
    title: "Deploy Project",
    description: "OpenL deployment workflow with mandatory validation checks, test execution requirements, and environment selection (dev, test, staging, prod)",
    arguments: [
      {
        name: "projectId",
        description: "ID of project to deploy",
        required: false,
      },
      {
        name: "environment",
        description: "Target environment: 'dev', 'test', 'staging', or 'prod'",
        required: false,
      },
    ],
  },
  {
    name: "get_project_errors",
    title: "Analyze Project Errors",
    description: "OpenL error analysis workflow with pattern matching, categorization, and fix recommendations for common validation errors",
    arguments: [
      {
        name: "projectId",
        description: "ID of project to analyze",
        required: false,
      },
    ],
  },
  {
    name: "file_history",
    title: "File History",
    description: "Guide for viewing Git-based file version history in OpenL, including commit hash navigation and version comparison",
    arguments: [
      {
        name: "filePath",
        description: "Path to the file (e.g., 'rules/Insurance-CA-Auto.xlsx')",
        required: false,
      },
      {
        name: "projectId",
        description: "ID of the project containing the file",
        required: false,
      },
    ],
  },
  {
    name: "project_history",
    title: "Project History",
    description: "Guide for viewing project-wide Git commit history, comparing with file history, and understanding when to use each",
    arguments: [
      {
        name: "projectId",
        description: "ID of the project",
        required: false,
      },
    ],
  },
];

/**
 * Directory containing prompt markdown files
 */
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const promptsDir = join(__dirname, "..", "prompts");

/**
 * Load prompt content from markdown file
 *
 * @param name - Name of the prompt (without .md extension)
 * @param args - Optional arguments for variable substitution
 * @returns Prompt content with variables substituted
 * @throws Error if prompt file doesn't exist
 */
export function loadPromptContent(
  name: string,
  args?: Record<string, string>
): string {
  const filePath = join(promptsDir, `${name}.md`);

  let content: string;
  try {
    content = readFileSync(filePath, "utf-8");
  } catch (error) {
    throw new Error(
      `Failed to load prompt '${name}': ${error instanceof Error ? error.message : String(error)}`
    );
  }

  // Apply argument substitution if provided
  if (args) {
    content = substituteArguments(content, args);
  }

  return content;
}

/**
 * Substitute arguments in prompt content
 *
 * Supports:
 * - Simple variables: {variableName}
 * - Conditional blocks: {if variableName}...{end if}
 *
 * @param content - Prompt content with placeholders
 * @param args - Arguments to substitute
 * @returns Content with arguments substituted
 */
function substituteArguments(
  content: string,
  args: Record<string, string>
): string {
  let result = content;

  // Process each argument
  for (const [key, value] of Object.entries(args)) {
    // Replace simple variables: {key}
    const simplePattern = new RegExp(`\\{${key}\\}`, "g");
    result = result.replace(simplePattern, value);

    // Process conditionals: {if key}...{end if}
    // If value is truthy, include the content; otherwise, remove it
    const conditionalPattern = new RegExp(
      `\\{if ${key}\\}([\\s\\S]*?)\\{end if\\}`,
      "g"
    );
    result = result.replace(conditionalPattern, value ? "$1" : "");
  }

  // Remove any remaining unused conditionals
  result = result.replace(/\{if \w+\}[\s\S]*?\{end if\}/g, "");

  // Remove any remaining unused variables (keep placeholder as-is for debugging)
  // Don't remove them - this helps identify missing arguments

  return result;
}

/**
 * Get prompt definition by name
 *
 * @param name - Name of the prompt
 * @returns Prompt definition or undefined if not found
 */
export function getPromptDefinition(
  name: string
): PromptDefinition | undefined {
  return PROMPTS.find((p) => p.name === name);
}

/**
 * Check if a prompt exists
 *
 * @param name - Name of the prompt
 * @returns True if prompt exists
 */
export function promptExists(name: string): boolean {
  return PROMPTS.some((p) => p.name === name);
}

/**
 * Get all prompt names
 *
 * @returns Array of prompt names
 */
export function getPromptNames(): string[] {
  return PROMPTS.map((p) => p.name);
}

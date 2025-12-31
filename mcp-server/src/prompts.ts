/**
 * Prompt template loader and processor
 *
 * Loads markdown prompt templates from the prompts/ directory and
 * substitutes context variables to create contextual guidance for AI assistants.
 */

import { readFileSync } from "fs";
import { join, resolve, dirname } from "path";
import { fileURLToPath } from "url";

/**
 * Response that may include a prompt for user guidance
 */
export interface PromptResponse<T = unknown> {
  /** Status of the operation */
  status: "needs_input" | "success" | "error";
  /** Prompt template for user guidance (if needs_input) */
  prompt?: string;
  /** Available options for selection */
  options?: string[];
  /** Recommended option */
  recommended?: string;
  /** Actual data (if success) */
  data?: T;
  /** Error message (if error) */
  error?: string;
  /** Additional context */
  context?: Record<string, unknown>;
}

/**
 * Prompt template loader
 */
export class PromptLoader {
  private static promptsDir = join(dirname(fileURLToPath(import.meta.url)), "..", "prompts");

  /**
   * Load a prompt template by name
   *
   * @param templateName - Name of the template file (without .md extension)
   * @param context - Variables to substitute in the template
   * @returns Formatted prompt with variables substituted
   */
  static load(templateName: string, context: Record<string, unknown> = {}): string {
    try {
      // Validate templateName to prevent path traversal attacks
      // Allow only alphanumeric characters, dashes, and underscores
      const safeNamePattern = /^[a-zA-Z0-9_-]+$/;
      if (!safeNamePattern.test(templateName)) {
        throw new Error(`Invalid template name: ${templateName}`);
      }

      // Construct and resolve the file path
      const filePath = join(this.promptsDir, `${templateName}.md`);
      const resolvedPath = resolve(filePath);

      // Ensure the resolved path is within promptsDir (prevent path traversal)
      const resolvedPromptsDir = resolve(this.promptsDir);
      if (!resolvedPath.startsWith(resolvedPromptsDir)) {
        throw new Error(`Path traversal detected: ${templateName}`);
      }

      const template = readFileSync(resolvedPath, "utf-8");
      return this.substitute(template, context);
    } catch {
      // If template doesn't exist or validation fails, return a generic prompt
      return `Processing ${templateName}...\n\nContext: ${JSON.stringify(context, null, 2)}`;
    }
  }

  /**
   * Substitute variables in a template
   *
   * Supports:
   * - Simple variables: {variable_name}
   * - Conditional blocks: {if condition}...{end if}
   * - Loops: {for each item}...{end for}
   *
   * @param template - Template string with placeholders
   * @param vars - Variables to substitute
   * @returns Template with variables substituted
   */
  static substitute(template: string, vars: Record<string, unknown>): string {
    let result = template;

    // Process conditional blocks: {if variable}...{end if}
    result = this.processConditionals(result, vars);

    // Process loops: {for each items:}...{end for}
    result = this.processLoops(result, vars);

    // Substitute simple variables: {variable}
    result = this.substituteVariables(result, vars);

    return result;
  }

  /**
   * Process conditional blocks
   */
  private static processConditionals(template: string, vars: Record<string, unknown>): string {
    const ifPattern = /\{if\s+(\w+)\}([\s\S]*?)\{end\s+if\}/g;

    return template.replace(ifPattern, (match, condition, content) => {
      // Check if condition variable is truthy
      const value = vars[condition];
      if (value && value !== false && value !== 0 && value !== "") {
        return content;
      }
      return "";
    });
  }

  /**
   * Process loop blocks
   */
  private static processLoops(template: string, vars: Record<string, unknown>): string {
    const forPattern = /\{for\s+each\s+(\w+):?\}([\s\S]*?)\{end\s+for\}/g;

    return template.replace(forPattern, (match, arrayName, content) => {
      const array = vars[arrayName];
      if (!Array.isArray(array) || array.length === 0) {
        return "";
      }

      return array.map((item, index) => {
        const itemVars = {
          ...vars,
          index: index + 1,
          ...(item !== null && typeof item === "object" ? item : { item }),
        };
        return this.substituteVariables(content, itemVars);
      }).join("\n");
    });
  }

  /**
   * Substitute simple variables
   */
  private static substituteVariables(template: string, vars: Record<string, unknown>): string {
    return template.replace(/\{(\w+)\}/g, (match, varName) => {
      const value = vars[varName];
      if (value === undefined || value === null) {
        return match; // Keep placeholder if variable not found
      }
      return String(value);
    });
  }

  /**
   * Create a prompt response that needs user input
   */
  static createPromptResponse<T = unknown>(
    templateName: string,
    context: Record<string, unknown>,
    options?: string[],
    recommended?: string
  ): PromptResponse<T> {
    return {
      status: "needs_input",
      prompt: this.load(templateName, context),
      options,
      recommended,
      context,
    };
  }

  /**
   * Create a success response with data
   */
  static createSuccessResponse<T>(data: T): PromptResponse<T> {
    return {
      status: "success",
      data,
    };
  }

  /**
   * Create an error response
   */
  static createErrorResponse(error: string): PromptResponse {
    return {
      status: "error",
      error,
    };
  }
}

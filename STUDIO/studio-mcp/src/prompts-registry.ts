/**
 * MCP Prompts Registry
 *
 * Registers and loads prompt templates from the prompts/ directory.
 * Prompts provide expert guidance for OpenL Studio workflows.
 */

import { readFileSync, readdirSync } from "fs";
import { join, dirname, basename } from "path";
import { fileURLToPath } from "url";
import YAML from "yaml";

/**
 * A single argument a prompt accepts.
 */
interface PromptArgument {
  name: string;
  description: string;
  required?: boolean;
}

/**
 * MCP Prompt definition
 */
interface PromptDefinition {
  /** Unique identifier for the prompt — resolved from the markdown filename */
  name: string;
  /** Human-readable title for display */
  title?: string;
  /** Description of what this prompt helps with */
  description?: string;
  /** Optional arguments that can be passed to the prompt */
  arguments?: PromptArgument[];
}

/**
 * Prompt frontmatter metadata parsed from a prompt's YAML frontmatter.
 *
 * The prompt's `name` is intentionally NOT part of the frontmatter — it is
 * derived from the filename so the two can never drift apart. Every field here
 * is optional: a prompt file may omit the title, description, or arguments.
 */
interface PromptFrontmatter {
  title?: string;
  description?: string;
  arguments?: PromptArgument[];
}

/**
 * Directory containing prompt markdown files
 */
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const promptsDir = join(__dirname, "..", "prompts");

/**
 * Cache of parsed prompt bodies (with frontmatter stripped), keyed by prompt
 * name. Populated once by loadPromptDefinitions() at startup — from the same
 * read that builds PROMPTS — so each prompt file is read from disk exactly
 * once. Argument substitution still runs on every loadPromptContent() call.
 */
const promptBodyCache = new Map<string, string>();

/**
 * Build a prompt definition from its filename-derived name and parsed
 * frontmatter. Optional fields are only included when present, so a file that
 * omits the title (or description, or arguments) yields a definition without
 * that key rather than an `undefined` placeholder.
 *
 * @param name - Prompt name (filename without the .md extension)
 * @param frontmatter - Parsed frontmatter, or null when the file has none
 * @returns The prompt definition served to MCP clients
 */
export function buildPromptDefinition(
  name: string,
  frontmatter: PromptFrontmatter | null
): PromptDefinition {
  const definition: PromptDefinition = { name };
  if (frontmatter?.title) {
    definition.title = frontmatter.title;
  }
  if (frontmatter?.description) {
    definition.description = frontmatter.description;
  }
  if (frontmatter?.arguments?.length) {
    definition.arguments = frontmatter.arguments;
  }
  return definition;
}

/**
 * Discover every prompt by reading the markdown files in the prompts/
 * directory and deriving each definition from the file's frontmatter. The
 * filename (without .md) is the prompt's name; the frontmatter supplies the
 * title, description, and arguments.
 *
 * @returns Prompt definitions, ordered alphabetically by name for determinism
 */
function loadPromptDefinitions(): PromptDefinition[] {
  return readdirSync(promptsDir)
    .filter((file) => file.endsWith(".md"))
    .sort()
    .map((file) => {
      const name = basename(file, ".md");
      const { frontmatter, body } = parsePromptFile(
        readFileSync(join(promptsDir, file), "utf-8")
      );
      promptBodyCache.set(name, body);
      return buildPromptDefinition(name, frontmatter);
    });
}

/**
 * Registry of all available prompts
 *
 * Each prompt corresponds to a markdown file in the prompts/ directory and is
 * built from that file's frontmatter. Prompts provide contextual guidance for
 * complex OpenL Studio workflows.
 */
export const PROMPTS: PromptDefinition[] = loadPromptDefinitions();

/**
 * Parse prompt file with optional YAML frontmatter
 *
 * Frontmatter format (the name is derived from the filename, not the frontmatter):
 * ---
 * title: Human-readable title
 * description: Description
 * arguments:
 *   - name: argName
 *     description: Arg description
 * ---
 * Markdown content here...
 *
 * @param content - Raw file content
 * @returns Parsed frontmatter and body
 */
function parsePromptFile(content: string): {
  frontmatter: PromptFrontmatter | null;
  body: string;
} {
  // Match YAML frontmatter: ---\n...\n---
  const frontmatterPattern = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/;
  const match = content.match(frontmatterPattern);

  if (match) {
    try {
      const frontmatter = YAML.parse(match[1]) as PromptFrontmatter;
      const body = match[2].trim(); // Trim leading/trailing whitespace
      return { frontmatter, body };
    } catch (error) {
      // If YAML parsing fails, treat entire file as body
      console.warn(
        `Failed to parse YAML frontmatter: ${error instanceof Error ? error.message : String(error)}`
      );
      return { frontmatter: null, body: content };
    }
  }

  // No frontmatter found
  return { frontmatter: null, body: content };
}

/**
 * Load prompt content for a registered prompt.
 *
 * The body is served from promptBodyCache, which loadPromptDefinitions() fills
 * at startup, so no file is read here; argument substitution still runs on
 * every call.
 *
 * @param name - Name of the prompt (without .md extension)
 * @param args - Optional arguments for variable substitution
 * @returns Prompt content with variables substituted
 * @throws Error if the prompt is not registered
 */
export function loadPromptContent(
  name: string,
  args?: Record<string, string>
): string {
  const body = promptBodyCache.get(name);
  if (body === undefined) {
    throw new Error(`Failed to load prompt '${name}': prompt not found`);
  }

  // Always apply substitution to process conditionals
  // (even when no args provided, to remove unused conditional blocks)
  return substituteArguments(body, args || {});
}

/**
 * Escape regex metacharacters in a string to make it safe for use in RegExp
 *
 * @param str - String to escape
 * @returns Escaped string safe for use in RegExp
 */
function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
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
    // Escape regex metacharacters in key to prevent ReDoS
    const escapedKey = escapeRegex(key);
    // Safely coerce value to string (handle null/undefined)
    const safeValue = value != null ? String(value) : "";

    // Replace simple variables: {key}
    const simplePattern = new RegExp(`\\{${escapedKey}\\}`, "g");
    result = result.replace(simplePattern, safeValue);

    // Process conditionals: {if key}...{end if}
    // If value is truthy, include the content; otherwise, remove it
    const conditionalPattern = new RegExp(
      `\\{if ${escapedKey}\\}([\\s\\S]*?)\\{end if\\}`,
      "g"
    );
    result = result.replace(conditionalPattern, safeValue ? "$1" : "");
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

/**
 * The OpenL skills the server serves through the MCP skills extension (SEP-2640).
 *
 * Every file of a skill is a resource `skill://<skill>/<file>`. The `SKILL.md` of a
 * skill carries the name and the description of its frontmatter. `skills/list`
 * answers every skill, and `skills/get` one skill by the URI of its `SKILL.md`:
 * the frontmatter as it is written and every file with its SHA-256 digest and size.
 *
 * The skills are part of the server and never change while it runs.
 */

import { createHash } from "node:crypto";
import { ProtocolError, ProtocolErrorCode, type McpServer } from "@modelcontextprotocol/server";
import { z } from "zod";
import catalog from "virtual:skills";
import type { Skill } from "./build-skills.js";

/** The identifier the server declares the extension under in its capabilities. */
export const SKILLS_EXTENSION = "io.modelcontextprotocol/skills";

/** A file of a skill, as a skill entry lists it. */
interface SkillResource {
  uri: string;
  /** `sha256:` and the hex digest of the UTF-8 bytes of the file. */
  digest: string;
  /** The length of the file in bytes. */
  size: number;
}

/** The entry of a skill that `skills/list` and `skills/get` answer. */
export interface SkillEntry {
  /** The URI of the `SKILL.md` of the skill. */
  uri: string;
  frontmatter: Record<string, unknown>;
  resources: SkillResource[];
}

const MIME_TYPES: Record<string, string> = { md: "text/markdown", json: "application/json" };

const SKILL_FILE = "SKILL.md";

/** The URI of `file` of the skill at `path`. */
function skillUri(path: string, file: string): string {
  return `skill://${path}/${file}`;
}

/** The MIME type of a file by its extension; text for an unknown one. */
function mimeTypeOf(file: string): string {
  return MIME_TYPES[file.split(".").at(-1)?.toLowerCase() ?? ""] ?? "text/plain";
}

/** The entry of `skill`. */
function entryOf(skill: Skill): SkillEntry {
  return {
    uri: skillUri(skill.path, SKILL_FILE),
    frontmatter: skill.frontmatter,
    resources: skill.files.map(({ path, text }) => ({
      uri: skillUri(skill.path, path),
      digest: `sha256:${createHash("sha256").update(text).digest("hex")}`,
      size: Buffer.byteLength(text),
    })),
  };
}

const ENTRIES = new Map(catalog.map(entryOf).map((entry) => [entry.uri, entry]));

/** Every file of every skill as the resource it is served as. */
const RESOURCES = catalog.flatMap((skill) =>
  skill.files.map(({ path, text }) => {
    const uri = skillUri(skill.path, path);
    const mimeType = mimeTypeOf(path);
    const isSkillFile = path === SKILL_FILE;
    return {
      name: isSkillFile ? String(skill.frontmatter.name) : `${skill.path}/${path}`,
      uri,
      metadata: isSkillFile ? { description: String(skill.frontmatter.description), mimeType } : { mimeType },
      contents: { contents: [{ uri, mimeType, text }] },
    };
  }),
);

const LIST_PARAMS = z.looseObject({ cursor: z.string().optional() });
const GET_PARAMS = z.looseObject({ uri: z.string() });

/**
 * The skills, by name, URI and description, for the server instructions and
 * `openl_get_started`, which tell an agent about them when its client does not
 * support the extension.
 */
export const SKILLS_OVERVIEW = `## Skills

OpenL skills are step-by-step workflows for common tasks. Read the \`SKILL.md\` of the one that matches
the task as an MCP resource before you start, and the files it references the same way: a relative path
resolves against the folder of its \`SKILL.md\`.

${[...ENTRIES.values()]
  .map(({ uri, frontmatter }) => {
    const description = String(frontmatter.description).replaceAll(/\s+/g, " ").trim();
    return `- \`${String(frontmatter.name)}\` (\`${uri}\`) — ${description}`;
  })
  .join("\n")}`;

/**
 * Serve the skills from `server`: their files as resources, and the `skills/list`
 * and `skills/get` methods. The server declares {@link SKILLS_EXTENSION} itself.
 */
export function registerSkills(server: McpServer): void {
  for (const { name, uri, metadata, contents } of RESOURCES) {
    server.registerResource(name, uri, metadata, () => contents);
  }

  // Every skill fits in one page, so the listing has no cursor to follow.
  server.server.setRequestHandler("skills/list", { params: LIST_PARAMS }, () => ({
    skills: [...ENTRIES.values()],
    ttlMs: 0,
    cacheScope: "private",
  }));
  server.server.setRequestHandler("skills/get", { params: GET_PARAMS }, ({ uri }) => {
    const skill = ENTRIES.get(uri);
    if (skill === undefined) {
      throw new ProtocolError(ProtocolErrorCode.InvalidParams, `No skill has the URI ${uri}`);
    }
    return { skill };
  });
}

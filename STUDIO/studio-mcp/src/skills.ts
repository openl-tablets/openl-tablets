/**
 * The OpenL skills the server serves through the MCP skills extension (SEP-2640).
 *
 * The SDK serves the files the bundle carries. Every file of a skill is a resource
 * `skill://<skill>/<file>`, and `skills/list` and `skills/get` answer the entry of
 * a skill: the frontmatter of its `SKILL.md` and every file with its digest and size.
 *
 * The skills are part of the server and never change while it runs.
 */

import { SkillsExtension, memorySkillFiles } from "@modelcontextprotocol/server/ext/skills";
import files from "virtual:skills";

/**
 * The skills, which `attach` serves from a server before it connects. A skill the extension refuses fails the start
 * rather than going missing.
 */
export const SKILLS = await SkillsExtension.load(memorySkillFiles(files), { strict: true });

/**
 * The skills, by name, URI and description, for the server instructions and
 * `openl_get_started`, which tell an agent about them when its client does not
 * support the extension.
 */
export const SKILLS_OVERVIEW = `## Skills

OpenL skills are step-by-step workflows for common tasks. Read the \`SKILL.md\` of the one that matches
the task as an MCP resource before you start, and the files it references the same way: a relative path
resolves against the folder of its \`SKILL.md\`.

${SKILLS.entries
  .map(({ uri, frontmatter: { name, description } }) =>
    `- \`${name}\` (\`${uri}\`) — ${description.replaceAll(/\s+/g, " ").trim()}`)
  .join("\n")}`;

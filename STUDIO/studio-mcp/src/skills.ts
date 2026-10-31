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
 * The skills, by name, URI and description, for the server instructions, which tell an agent about them when its
 * client does not support the extension.
 */
export const SKILLS_OVERVIEW = `## Skills

OpenL skills are step-by-step workflows this server serves as MCP resources. They are no skills of your client:
never call its skill tool or a slash command for them. Before you start a task, read the \`SKILL.md\` of the skill
that matches it with the tool of your client that reads an MCP resource, such as \`ReadMcpResourceTool\` in Claude
Code, by its URI, and the files it references the same way: a relative path resolves against the
folder of its \`SKILL.md\`.

${SKILLS.entries
  .map(({ uri, frontmatter: { name, description } }) =>
    `- \`${name}\` (\`${uri}\`) — ${description.replaceAll(/\s+/g, " ").trim()}`)
  .join("\n")}`;

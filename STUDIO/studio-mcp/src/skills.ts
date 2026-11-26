/**
 * The OpenL skills the server serves through the MCP skills extension (SEP-2640).
 *
 * The SDK serves the files the bundle carries. Every file of a skill is a resource
 * `skill://<skill>/<file>`, and `skills/list` and `skills/get` answer the entry of
 * a skill: the frontmatter of its `SKILL.md` and every file with its digest and size.
 *
 * The guide tools serve the same files as documents of the ids `skill/<skill>` (see `guides-registry.ts`), for the
 * clients that read neither. The skills are part of the server and never change while it runs.
 */

import { SkillsExtension, memorySkillFiles } from '@modelcontextprotocol/server/ext/skills'
import files from 'virtual:skills'

/**
 * The skills, which `attach` serves from a server before it connects. A skill the extension refuses fails the start
 * rather than going missing.
 */
export const SKILLS = await SkillsExtension.load(memorySkillFiles(files), { strict: true })

/** The summary of a skill its `SKILL.md` gives in `metadata.summary` of its frontmatter, if any. */
export function summaryOf({ frontmatter }: (typeof SKILLS.entries)[number]): string | undefined {
    const { metadata } = frontmatter as { metadata?: Record<string, string> }
    return metadata?.summary
}

/**
 * The skills, by guide id and summary, for the server instructions, which tell every agent about them: the guide
 * tools read them in any client, while only some clients support the extension or read MCP resources.
 *
 * A skill is named by the summary of its frontmatter, not its description: Claude Code cuts the instructions after
 * about 4 KB, which the descriptions of the skills alone exceed. `openl_list_guides` finds a skill by its description.
 */
export const SKILLS_OVERVIEW = `## Skills

OpenL skills are step-by-step workflows of this server, no skills of your client: never call its skill tool or a
slash command for them. Before a task, read the skill that matches it with \`openl_get_guides\` by its id below, as
\`ids: ["skill/planning"]\`, and the files it links the same way: \`references/templates.md\` of \`skill/planning\` is
\`skill/planning/references/templates.md\`. \`openl_list_guides\` \`search\` finds a skill by its description.

${SKILLS.entries
    .map((entry) => `- \`skill/${entry.frontmatter.name}\` — ${summaryOf(entry)}`)
    .join('\n')}`

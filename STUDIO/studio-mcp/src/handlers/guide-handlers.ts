/**
 * Guidance tool handlers — the agent onboarding entry point (openl_get_started),
 * progressive-disclosure access to the OpenL reference documentation OpenL Studio
 * serves at /docs (openl_list_guides / openl_get_guides, see
 * {@link file://../guides-registry.ts}), and the per-project AGENTS.md hierarchy
 * resolver (openl_get_project_agent_context).
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatPage, formatAgentsDocument } from "../formatters.js";
import {
  findKnownGuideIds,
  guidesOverview,
  listGuides,
  readGuideBodies,
} from "../guides-registry.js";
import type { OpenLClient } from "../client.js";
import { SKILLS_OVERVIEW } from "../skills.js";
import { sanitizeError } from "../utils.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

/**
 * The fixed part of openl_get_started: the workflow protocol every agent is
 * expected to follow. The skills and the orientation are appended at call time, the orientation from the documents
 * OpenL Studio serves.
 */
const WORKFLOW_PROTOCOL = `# OpenL Studio MCP — Getting Started

This server connects you to OpenL Studio, the web IDE of the OpenL Tablets Business
Rules Management System (BRMS). Business logic lives in Excel-based rule tables
(decision tables, spreadsheets, datatypes, tests) inside versioned projects. You are
now oriented — do not call this tool again in this session.

## Workflow protocol

1. **Discover**: \`openl_list_repositories\` → \`openl_list_projects\` to locate the project.
2. **Load guidance BEFORE working on or creating ANY project**: call
   \`openl_get_project_agent_context\` with the project id — and with \`folder\` when you
   work deeper inside the project. It aggregates the AGENTS.md hierarchy that applies
   to that path (root first, nearest wins) and points out the reference guides that
   guidance mentions. Guidance is layered per target path — never assume it is global
   or reusable across projects.
3. **Consult reference documentation on demand**: \`openl_list_guides\` is a lightweight
   metadata index of the OpenL documentation OpenL Studio serves (filter with
   \`type\`/\`search\`, paginate). Fetch the full markdown of ONLY the entries you need with
   \`openl_get_guides(ids)\`. Do not guess OpenL table syntax, functions, or project
   layout from memory — look them up.
4. **Edit**: open the project first (\`openl_open_project\`; design repositories only),
   unless the \`capabilities\` of \`openl_get_project\` lack \`canOpen\` — a project edited in
   place, of a file repository in single-user mode, is always open and has every edit
   written straight into its files. Then use the table tools (\`openl_list_tables\`, \`openl_get_table\`,
   \`openl_update_table\`, ...) or the project-file tools.
5. **Validate after every edit**: \`openl_project_status\` for compile
   errors, then run tests (\`openl_start_project_tests\` →
   \`openl_get_test_results\` with \`summary: true\`).
6. **Persist / ship**: \`openl_save_project\` commits to Git; \`openl_deploy_project\`
   deploys. Neither applies to projects in the 'local' repository — those have no
   open/save/close or Git operations — and saving does not apply to a project edited in
   place either: its edits are in its files already, and it is never closed. The
   \`capabilities\` of \`openl_get_project\` tell which of these operations a project takes.`;

/**
 * Render the workspace-orientation section from the documents OpenL Studio serves:
 * which specification/guide categories exist and how to discover more — NOT
 * a dump of the index (that is openl_list_guides' job).
 */
async function renderGuidesOrientation(client: OpenLClient): Promise<string> {
  let overview;
  try {
    overview = await guidesOverview(client);
  } catch (error) {
    return (
      `## Reference documentation\n\n` +
      `OpenL Studio does not serve its documentation now, so \`openl_list_guides\` and ` +
      `\`openl_get_guides\` will fail: ${sanitizeError(error)}`
    );
  }

  const specifications = overview.specifications
    .map((s) => `- \`${s.id}\` — ${s.title}`)
    .join("\n");
  const sections = overview.guideSections
    .map((s) => `${s.section} (${s.count})`)
    .join(", ");

  return `## Reference documentation

OpenL Studio serves ${overview.specifications.length + overview.guideCount} documents of its own version.

Specifications (${overview.specifications.length}):
${specifications}

Reference-guide chapters (${overview.guideCount}) by section: ${sections}.

Discover entries with \`openl_list_guides\` (\`type\`, \`search\`, pagination); read full text with \`openl_get_guides(ids)\`.`;
}

export function registerGuideHandlers(): void {
  registerTool({
    name: "get_started",
    title: "Get Started",
    description:
      "Call this FIRST, once per session, before any other openl_ tool. Returns the mandatory workflow protocol, the OpenL skills (step-by-step workflows served as skill:// resources), and a workspace orientation: when to call openl_get_project_agent_context (before working on or creating any project), how to discover the OpenL reference documentation OpenL Studio serves (openl_list_guides / openl_get_guides), and the edit → validate → save loop.",
    schema: schemas.getStartedSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (_args, client): Promise<ToolResponse> => {
      return textResult(`${WORKFLOW_PROTOCOL}\n\n${SKILLS_OVERVIEW}\n\n${await renderGuidesOrientation(client)}`);
    },
  });

  registerTool({
    name: "list_guides",
    title: "List Guides",
    description:
      "List the OpenL reference documentation OpenL Studio serves for its own version — METADATA ONLY (id, title, size in bytes), never bodies; fetch bodies with openl_get_guides. 'specification' entries are config-file/project-layout specs (rules.xml, rules-deploy.xml, project structure, table types, openl-maven-plugin), 'guide' entries are the Reference Guide chapters (table types, table properties, functions and data types, projects, BEX/function appendices). An id starts with 'spec/' or 'guide/', its type; openl_get_guides names the path a person opens the document at. Filter with 'type' and/or case-insensitive 'search' over id+title; results are paginated (limit/offset).",
    schema: schemas.listGuidesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      // The id tells the type of an entry, and openl_get_guides the path a person opens it at
      const entries = (await listGuides(client, { type: args.type, search: args.search }))
        .map(({ type, path, ...entry }) => entry);
      return textResult(formatPage(entries, "json", limit, offset));
    },
  });

  registerTool({
    name: "get_guides",
    title: "Get Guides",
    description:
      "Return the FULL markdown bodies of 1-5 documents by the exact ids from openl_list_guides (e.g. 'spec/rules.xml', 'guide/introduction/basic-concepts'). Unknown ids fail with an error naming them — this tool never falls back to the index; look ids up with openl_list_guides first. Bodies are returned verbatim and are NOT truncated, so mind each entry's size from the index and request only what you need. Links and images in a body are relative to the path of its document.",
    schema: schemas.getGuidesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { found, unknown } = await readGuideBodies(client, args.ids);
      if (unknown.length > 0) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Unknown guide id(s): ${unknown.join(", ")}. Ids must match the index exactly — ` +
            `call openl_list_guides (optionally with 'search') to look them up.`
        );
      }

      // Bodies are returned verbatim and deliberately NOT truncated at the usual
      // 25K response cap: a cut-off reference document is worse than a large one,
      // and the caller opted in knowingly — the index publishes every entry's
      // size and the id count is capped at 5.
      const sections = found.map(
        ({ entry, body }) =>
          `-----\n**Guide \`${entry.id}\`** (path: ${entry.path})\n\n${body.trimEnd()}`
      );
      return textResult(sections.join("\n\n"));
    },
  });

  registerTool({
    name: "get_project_agent_context",
    title: "Get Project Agent Context",
    description:
      "Read the agent guidance that applies to a project; call this BEFORE working on or creating anything in it. The answer is one Markdown document of every AGENTS.md from the repository root down to the project folder, or to its 'folder', where a later file overrides an earlier one; a project with none answers a short note. The ids of the guides the guidance references end the document: read them with openl_get_guides.",
    schema: schemas.getProjectAgentContextSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const files = await client.getProjectAgentContext(args.projectId, {
        folder: args.folder,
        branch: args.branch,
      });

      let document = formatAgentsDocument(files);

      // Cross-reference the guidance with the documentation: AGENTS.md files
      // reference guides by their stable ids (e.g. 'spec/rules.xml'), which are
      // scanned from the UNtruncated file contents so a long chain cannot hide a
      // reference. Unavailable documentation → no section (enrichment only).
      const referenced = files.length > 0 ? await findKnownGuideIds(client, files.map((f) => f.content).join("\n")) : [];
      if (referenced.length > 0) {
        const list = referenced.map((id) => "- `" + id + "`").join("\n");
        document +=
          `\n### Referenced guides\n\nThe guidance above references these reference documents — ` +
          `fetch them with openl_get_guides:\n${list}\n`;
      }

      return textResult(document);
    },
  });
}

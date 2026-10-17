/**
 * Guidance tool handlers — the agent onboarding entry point (openl_get_started),
 * progressive-disclosure access to the bundled OpenL reference documentation
 * (openl_list_guides / openl_get_guides), and the per-project AGENTS.md
 * hierarchy resolver (openl_get_project_agent_context).
 *
 * The documentation tools serve the guides/ bundle embedded at build time (see
 * {@link file://../build-guides.ts}); they never call OpenL Studio or the
 * network. Only openl_get_project_agent_context talks to the studio.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatResponse, paginateResults, formatAgentsDocument } from "../formatters.js";
import {
  findKnownGuideIds,
  guidesOverview,
  listGuides,
  readGuideBodies,
} from "../guides-registry.js";
import { registerTool, type ToolResponse } from "./common.js";

/**
 * The fixed part of openl_get_started: the workflow protocol every agent is
 * expected to follow. The bundle-derived orientation is appended at call time.
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
   metadata index of the bundled OpenL documentation (filter with \`type\`/\`search\`,
   paginate). Fetch the full markdown of ONLY the entries you need with
   \`openl_get_guides(ids)\`. Do not guess OpenL table syntax, functions, or project
   layout from memory — look them up.
4. **Edit**: open the project first (\`openl_open_project\`; design repositories only),
   then use the table tools (\`openl_list_tables\`, \`openl_get_table\`,
   \`openl_update_table\`, ...) or the project-file tools.
5. **Validate after every edit**: \`openl_project_status\` (\`wait: true\`) for compile
   errors, then run tests (\`openl_start_project_tests\` →
   \`openl_get_test_results_summary\`).
6. **Persist / ship**: \`openl_save_project\` commits to Git; \`openl_deploy_project\`
   deploys. Neither applies to projects in the 'local' repository — those have no
   open/save/close or Git operations (see the \`local_projects\` prompt).

MCP prompts complement the tools with task-specific playbooks (e.g.
\`create_rule_decision_tables\`, \`create_test\`, \`validate_after_edit\`).`;

/**
 * Render the workspace-orientation section from the bundled guides index:
 * which specification/guide categories exist and how to discover more — NOT
 * a dump of the index (that is openl_list_guides' job).
 */
function renderGuidesOrientation(): string {
  let overview;
  try {
    overview = guidesOverview();
  } catch (error) {
    return (
      `## Reference documentation\n\n` +
      `The bundled documentation is unavailable in this build, so \`openl_list_guides\` and ` +
      `\`openl_get_guides\` will fail: ${error instanceof Error ? error.message : String(error)}`
    );
  }

  const specifications = overview.specifications
    .map((s) => `- \`${s.id}\` — ${s.title}`)
    .join("\n");
  const sections = overview.guideSections
    .map((s) => `${s.section} (${s.count})`)
    .join(", ");

  return `## Reference documentation

The bundle holds ${overview.specifications.length + overview.guideCount} documents from \`${overview.source_repo}\`@\`${overview.source_ref}\`, the same revision as this OpenL Studio.

Specifications (${overview.specifications.length}):
${specifications}

Reference-guide chapters (${overview.guideCount}) by section: ${sections}.

Discover entries with \`openl_list_guides\` (\`type\`, \`search\`, pagination); read full text with \`openl_get_guides(ids)\`.`;
}

export function registerGuideHandlers(): void {
  registerTool({
    name: "get_started",
    category: "Guidance",
    title: "Get Started",
    description:
      "Read-only. Call this FIRST, once per session, before any other openl_ tool. Returns the mandatory workflow protocol and a workspace orientation: when to call openl_get_project_agent_context (before working on or creating any project), how to discover the bundled OpenL reference documentation (openl_list_guides / openl_get_guides), and the edit → validate → save loop. Takes no arguments and never calls OpenL Studio.",
    schema: schemas.getStartedSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: false,
    },
    handler: async (): Promise<ToolResponse> => {
      return {
        content: [{ type: "text", text: `${WORKFLOW_PROTOCOL}\n\n${renderGuidesOrientation()}` }],
      };
    },
  });

  registerTool({
    name: "list_guides",
    category: "Guidance",
    title: "List Guides",
    description:
      "List the OpenL reference documentation bundled with this server — METADATA ONLY (id, type, title, source path, size in bytes), never bodies; fetch bodies with openl_get_guides. The bundle embeds the OpenL Tablets docs at the release tag matching the targeted OpenL Studio version: 'specification' entries are config-file/project-layout specs (rules.xml, rules-deploy.xml, project structure, openl-maven-plugin), 'guide' entries are the Reference Guide chapters (table types, table properties, functions and data types, projects, BEX/function appendices). Filter with 'type' and/or case-insensitive 'search' over id+title; results are paginated (limit/offset). Read-only, local — never calls OpenL Studio.",
    schema: schemas.listGuidesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: false,
    },
    handler: async (args): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      const entries = listGuides({ type: typedArgs.type, search: typedArgs.search });
      const paginated = paginateResults(entries, limit, offset);

      return {
        content: [{
          type: "text",
          text: formatResponse(paginated.data, format, {
            pagination: { limit, offset, total: paginated.total_count },
          }),
        }],
      };
    },
  });

  registerTool({
    name: "get_guides",
    category: "Guidance",
    title: "Get Guides",
    description:
      "Return the FULL markdown bodies of 1-5 bundled documents by the exact ids from openl_list_guides (e.g. 'spec/rules.xml', 'guide/introduction/basic-concepts'). Unknown ids fail with an error naming them — this tool never falls back to the index; look ids up with openl_list_guides first. Bodies are returned verbatim and are NOT truncated, so mind each entry's size_bytes from the index and request only what you need. Read-only, local — never calls OpenL Studio.",
    schema: schemas.getGuidesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: false,
    },
    handler: async (args): Promise<ToolResponse> => {
      const typedArgs = args;
      const ids = typedArgs.ids;

      const { found, unknown } = readGuideBodies(ids);
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
      // size_bytes and the id count is capped at 5.
      const sections = found.map(
        ({ entry, body }) =>
          `-----\n**Guide \`${entry.id}\`** (source: ${entry.source_path})\n\n${body.trimEnd()}`
      );
      return { content: [{ type: "text", text: sections.join("\n\n") }] };
    },
  });

  registerTool({
    name: "get_project_agent_context",
    category: "Guidance",
    title: "Get Project Agent Context",
    description:
      "Resolve the agent guidance (AGENTS.md hierarchy) that applies to a project — call this BEFORE working on or creating anything in the project. Starting at the project directory — or the optional 'folder' sub-directory — this walks UP through every parent folder to the repository root, collects every AGENTS.md found, and returns them concatenated in ONE markdown document ordered from the root folder (lowest priority) down to the project folder (highest priority); on conflicting instructions, each later section overrides the earlier ones. AGENTS.md files live not only in the project but often in a workspace/monorepo root above it. Levels with no AGENTS.md are skipped (not an error); a project with none returns a short 'no files' note. When the guidance references bundled reference guides by id, those ids are listed at the end — fetch them with openl_get_guides. The search direction is fixed — to search a project's own subtree by glob/content instead, use openl_search_project_files.",
    schema: schemas.getProjectAgentContextSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const files = await client.getProjectAgentContext(typedArgs.projectId, {
        folder: typedArgs.folder,
        branch: typedArgs.branch,
      });

      let document = formatAgentsDocument(files);

      // Cross-reference the guidance with the bundled documentation: AGENTS.md
      // files reference guides by their stable ids (e.g. 'spec/rules.xml'), which
      // are scanned from the UNtruncated file contents so a long chain cannot
      // hide a reference. Unavailable bundle → no section (enrichment only).
      const referenced = findKnownGuideIds(files.map((f) => f.content).join("\n"));
      if (referenced.length > 0) {
        document +=
          `\n### Referenced guides\n\nThe guidance above references these bundled reference documents — ` +
          `fetch them with openl_get_guides:\n${referenced.map((id) => `- \`${id}\``).join("\n")}\n`;
      }

      return {
        content: [{ type: "text", text: document }],
      };
    },
  });
}

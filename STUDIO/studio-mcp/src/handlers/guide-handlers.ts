/**
 * Guidance tool handlers — progressive-disclosure access to the OpenL reference documentation OpenL Studio
 * serves at /docs (openl_list_guides / openl_get_guides, see
 * {@link file://../guides-registry.ts}), and the per-project AGENTS.md hierarchy
 * resolver (openl_get_project_agent_context).
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatAgentsDocument } from "../formatters.js";
import { formatListOf } from "../markdown-list.js";
import {
  findKnownGuideIds,
  listGuides,
  readGuideBodies,
} from "../guides-registry.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

export function registerGuideHandlers(): void {
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
      const entries = await listGuides(client, { type: args.type, search: args.search });
      return textResult(formatListOf(entries, limit, offset, {
        noun: "documents",
        columns: [
          { header: "id", value: (entry) => entry.id },
          { header: "title", value: (entry) => entry.title },
          { header: "bytes", value: (entry) => entry.size },
        ],
      }));
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
      // reference guides by their stable ids (e.g. 'spec/rules.xml').
      // Unavailable documentation → no section (enrichment only).
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

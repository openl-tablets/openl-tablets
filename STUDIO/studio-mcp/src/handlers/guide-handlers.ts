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
      "List the OpenL reference documents Studio serves for its version: id, title and size in bytes, no bodies (openl_get_guides reads them). An id starting 'spec/' is a specification of a config file or the project layout (rules.xml, rules-deploy.xml, table types, openl-maven-plugin), one starting 'guide/' a chapter of the Reference Guide (table types and properties, functions, data types, BEX).",
    schema: schemas.listGuidesSchema,
    // The ids of the specifications start with 'spec', which agents take for their type
    valueAliases: { spec: "specification" },
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
      "Read the full Markdown of documents by their exact ids from openl_list_guides; an unknown id fails. A body is never cut: mind the sizes in the index. Links and images in a body are relative to its path.",
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
      "Read the AGENTS.md guidance of a project as one Markdown document, from the repository root down to the project or its folder, a later file overriding an earlier one; none answers a short note. Longer guidance answers the opening of each file and its sections with their sizes, to read with sections. The guide ids it references end it, for openl_get_guides.",
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

      let document = formatAgentsDocument(files, args.sections);

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

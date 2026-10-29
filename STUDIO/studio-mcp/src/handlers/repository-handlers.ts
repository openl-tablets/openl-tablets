/**
 * Repository tool handlers — list design repositories, branches, repository
 * features, and project revision history.
 */

import * as schemas from "../schemas.js";
import { formatPage, hasMoreItems } from "../formatters.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";

export function registerRepositoryHandlers(): void {
  registerTool({
    name: "list_repositories",
    title: "List Design Repositories",
    description:
      "List all design repositories in OpenL Studio. Returns repository information including 'id' (internal identifier) and 'name' (display name). Use the 'name' field when working with repositories in other tools. Either the 'id' or 'name' is accepted by other tools (case-insensitive). The actual values are usually short tokens like 'design' — never invent values such as 'Design Repository' or 'design-repo'.",
    schema: schemas.listRepositoriesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      const repositories = await client.listRepositories();
      return textResult(formatPage(repositories, args.response_format, limit, offset, { dataType: "repositories" }));
    },
  });

  registerTool({
    name: "list_branches",
    title: "List Git Branches",
    description:
      "List all Git branch names in a repository. Use this to see available branches before switching or comparing versions. Pass either the id or name from openl_list_repositories() — both are accepted (case-insensitive). Do not invent example values; call openl_list_repositories() first if not in context.",
    schema: schemas.listBranchesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(args.repository);
      const branches = await client.listBranches(repositoryId);

      return textResult(formatPage(branches, "json", limit, offset));
    },
  });

  registerTool({
    name: "list_repository_features",
    title: "Get Repository Features",
    description:
      "Get features supported by a design repository, including branching, search, and change comments. Check these flags before operations that depend on them. Pass either the id or name from openl_list_repositories() — both are accepted (case-insensitive). Do not invent example values; call openl_list_repositories() first if not in context.",
    schema: schemas.getRepositoryFeaturesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const features = await client.getRepositoryFeatures(args.repository);

      return formatResult(features);
    },
  });

  registerTool({
    name: "repository_project_revisions",
    title: "Get Project Revision History",
    description:
      "Get revision history (commit history) of a project in the branch it is currently on. Addressing the project by its stable ID keeps history available after an unsaved rename. Returns revisions with commit hashes, authors, timestamps, and commit types, with pagination and optional search and technical-revision filters. Use the projectId returned by openl_list_projects; do not construct it from the displayed project name.",
    schema: schemas.getProjectRevisionsSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const revisions = await client.getProjectRevisions(args.projectId, args);
      // A non-aligned item offset cannot be reconstructed from the backend's
      // floor-derived pageNumber, so preserve the exact request value.
      const offset = args.offset ?? revisions.pageNumber * revisions.pageSize;
      const { pageSize: limit, total } = revisions;

      return formatResult(revisions, "json", {
        pagination: { limit, offset, total, hasMore: hasMoreItems(offset, revisions.numberOfElements, limit, total) },
      });
    },
  });
}

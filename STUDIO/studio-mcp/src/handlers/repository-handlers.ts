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
      "List the design repositories, each with its 'id' and 'name' and the features it supports (branches, comments, searchable, mapped folders). Every tool takes a repository by its id or name in any case: use the values listed, never a made-up one.",
    schema: schemas.listRepositoriesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      const repositories = (await client.listRepositories()).map(({ aclId, ...repository }) => repository);
      return textResult(formatPage(repositories, args.response_format, limit, offset, { dataType: "repositories" }));
    },
  });

  registerTool({
    name: "list_branches",
    title: "List Git Branches",
    description:
      "List the Git branches of a repository, named by its id or name from openl_list_repositories, in any case.",
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

      // The revisions alone: the page Studio wraps them in is told by the pagination, a short revision is a prefix of
      // its revision, and a flag that is false tells nothing
      const items = revisions.content.map(({ shortRevisionNo, deleted, technicalRevision, ...revision }) => ({
        ...revision,
        ...(deleted ? { deleted } : {}),
        ...(technicalRevision ? { technicalRevision } : {}),
      }));
      return formatResult(items, "json", {
        pagination: { limit, offset, total, hasMore: hasMoreItems(offset, revisions.numberOfElements, limit, total) },
      });
    },
  });
}

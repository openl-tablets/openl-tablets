/**
 * Repository tool handlers — list design repositories, branches, repository
 * features, and project revision history.
 */

import * as schemas from "../schemas.js";
import { formatList, formatListOf, hasMoreItems } from "../markdown-list.js";
import { namesOfSet } from "../utils.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

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

      const repositories = await client.listRepositories();
      return textResult(formatListOf(repositories, limit, offset, {
        noun: "repositories", one: "repository",
        columns: [
          { header: "id", value: (repository) => repository.id },
          { header: "name", value: (repository) => repository.name },
          { header: "type", value: (repository) => repository.type },
          { header: "features", value: (repository) => namesOfSet(repository.features) },
        ],
      }));
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

      return textResult(formatListOf(branches, limit, offset, {
        noun: "branches", one: "branch", columns: [{ header: "branch", value: (branch) => branch }],
      }));
    },
  });

  registerTool({
    name: "repository_project_revisions",
    title: "Get Project Revision History",
    description:
      "Get revision history (commit history) of a project in the branch it is currently on. Addressing the project by its stable ID keeps history available after an unsaved rename. Returns a table of revisions with commit hashes, dates, authors, the first line of each comment and the deleted and technical flags, with pagination and optional search and technical-revision filters. Use the projectId returned by openl_list_projects; do not construct it from the displayed project name.",
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
      const { offset } = args;
      const { pageSize: limit, total } = revisions;

      return textResult(formatList({
        noun: "revisions",
        items: revisions.content,
        offset,
        total,
        hasMore: hasMoreItems(offset, revisions.numberOfElements, limit, total),
        columns: [
          { header: "revision", value: (revision) => revision.revisionNo },
          { header: "date", value: (revision) => revision.createdAt },
          { header: "author", value: (revision) => revision.author?.displayName ?? revision.author?.email },
          // The first line of a comment names the change; the rest is its detail
          { header: "comment", value: (revision) => revision.fullComment.split(/\r?\n/, 1)[0] },
          { header: "flags", value: (revision) => [
            ...(revision.deleted ? ["deleted"] : []), ...(revision.technicalRevision ? ["technical"] : []),
          ] },
        ],
      }));
    },
  });
}

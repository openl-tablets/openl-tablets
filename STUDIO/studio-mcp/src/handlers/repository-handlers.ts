/**
 * Repository tool handlers — list design repositories, branches, repository
 * features, and project revision history.
 */

import * as schemas from "../schemas.js";
import { formatList, formatListOf, hasMoreItems } from "../markdown-list.js";
import { authorOf, headlineOf, namesOfSet } from "../utils.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

export function registerRepositoryHandlers(): void {
  registerTool({
    name: "list_repositories",
    title: "List Design Repositories",
    description:
      "List the design repositories with their id, name, type and features (branches, comments, searchable, mapped folders). Tools take a repository by either, in any case.",
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
      "List the Git branches of a repository.",
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
      "List the revisions of a project on its branch or another: revision, date, author, the first line of the comment, and the deleted and technical flags. openl_open_project and openl_create_project_branch take a revision.",
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
          { header: "author", value: authorOf },
          { header: "comment", value: headlineOf },
          { header: "flags", value: (revision) => [
            ...(revision.deleted ? ["deleted"] : []), ...(revision.technicalRevision ? ["technical"] : []),
          ] },
        ],
      }));
    },
  });
}

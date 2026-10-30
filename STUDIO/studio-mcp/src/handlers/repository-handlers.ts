/**
 * Repository tool handlers — list design repositories, their branches, the revision history of a project and the files
 * a revision changed.
 */

import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatPage, hasMoreItems } from "../formatters.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";

/**
 * A revision as the tools answer it: a short revision is a prefix of its revision, and a flag that is false tells
 * nothing.
 */
type RevisionItem = Omit<Types.ProjectRevision, "shortRevisionNo" | "deleted" | "technicalRevision">
  & Partial<Pick<Types.ProjectRevision, "deleted" | "technicalRevision">>;

const revisionItem = ({ shortRevisionNo, deleted, technicalRevision, ...revision }: Types.ProjectRevision): RevisionItem => ({
  ...revision,
  ...(deleted ? { deleted } : {}),
  ...(technicalRevision ? { technicalRevision } : {}),
});

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

      // The revisions alone: the page Studio wraps them in is told by the pagination
      return formatResult(revisions.content.map(revisionItem), "json", {
        pagination: { limit, offset, total, hasMore: hasMoreItems(offset, revisions.numberOfElements, limit, total) },
      });
    },
  });

  registerTool({
    name: "repository_project_revision_changes",
    title: "Get Files Changed by a Revision",
    description:
      "List the files of a project a revision changed, each by its path in the project and whether it was added, modified or deleted, with both revisions as openl_repository_project_revisions answers them. A revision is compared with the one it was made on: in Git its first parent, so a merge lists what it brought in. Between two revisions a file changed several times is listed once. The first revision of a project lists every file as added; files outside the project folder and ignored files are not listed. Read a file as a revision holds it with openl_read_project_file and its 'version'. An unknown revision is refused.",
    schema: schemas.getProjectRevisionChangesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { base, revision, files } = await client.getProjectRevisionChanges(args.projectId, args.revision, args.base);
      return textResult(formatPage(files, "json", args.limit, args.offset, {
        responseMetadata: { base: base && revisionItem(base), revision: revisionItem(revision) },
      }));
    },
  });
}

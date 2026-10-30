/**
 * Repository tool handlers — list design repositories, their branches, the revision history of a project and the files
 * a revision changed.
 */

import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatList, formatListOf, hasMoreItems, markdownTable, type Column } from "../markdown-list.js";
import { authorOf, headlineOf, namesOfSet } from "../utils.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

/** The columns a revision is listed by: the first line of its comment names it, and only the flags that are set show. */
const REVISION_COLUMNS: Array<Column<Types.ProjectRevision>> = [
  { header: "revision", value: (revision) => revision.revisionNo },
  { header: "date", value: (revision) => revision.createdAt },
  { header: "author", value: authorOf },
  { header: "comment", value: headlineOf },
  { header: "flags", value: (revision) => [
    ...(revision.deleted ? ["deleted"] : []), ...(revision.technicalRevision ? ["technical"] : []),
  ] },
];

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
        columns: REVISION_COLUMNS,
      }));
    },
  });

  registerTool({
    name: "repository_project_revision_changes",
    title: "Get Files Changed by a Revision",
    description:
      "List the files of a project a revision changed, by path in the project and change (added, modified or deleted), then the revisions compared in the columns of openl_repository_project_revisions. A revision is compared with the one it was made on: in Git its first parent, so a merge lists what it brought in; between two revisions a file changed several times is listed once. The first revision of a project lists every file as added; files outside the project folder and ignored files are not listed. openl_read_project_file reads a file as a revision holds it. An unknown revision is refused.",
    schema: schemas.getProjectRevisionChangesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { base, revision, files } = await client.getProjectRevisionChanges(args.projectId, args.revision, args.base);
      const compared = [...(base ? [{ ...base, side: "base" }] : []), { ...revision, side: "revision" }];
      return textResult(formatListOf(files, args.limit, args.offset, {
        noun: "files",
        columns: [{ header: "path", value: (file) => file.path }, { header: "change", value: (file) => file.type }],
        notes: [markdownTable(compared, [{ header: "compared", value: (side) => side.side }, ...REVISION_COLUMNS])],
      }));
    },
  });
}

/**
 * Repository tool handlers — list design repositories, branches, repository
 * features, project revision history, and deploy repositories.
 */

import * as schemas from "../schemas.js";
import { formatResponse, paginateResults } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";


export function registerRepositoryHandlers(): void {
  registerTool({
    name: "list_repositories",
    category: "Repository",
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
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      const repositories = await client.listRepositories();
      const paginated = paginateResults(repositories, limit, offset);
      const formattedResult = formatResponse(paginated.data, format, {
        pagination: { limit, offset, total: paginated.total_count },
        dataType: "repositories",
      });

      return { content: [{ type: "text", text: formattedResult }] };
    },
  });

  registerTool({
    name: "list_branches",
    category: "Repository",
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
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);
      const branches = await client.listBranches(repositoryId);

      // Apply pagination
      const paginated = paginateResults(branches, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "list_repository_features",
    category: "Repository",
    title: "Get Repository Features",
    description:
      "Get features supported by a design repository (branching, searchable, etc.). Use this to check if a repository supports specific features like branching before performing operations that depend on those features. Pass either the id or name from openl_list_repositories() — both are accepted (case-insensitive). Do not invent example values; call openl_list_repositories() first if not in context.",
    schema: schemas.getRepositoryFeaturesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);
      const features = await client.getRepositoryFeatures(repositoryId);

      const formattedResult = formatResponse(features, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "repository_project_revisions",
    category: "Repository",
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
      const typedArgs = args;

      const format = typedArgs.response_format;

      const revisions = await client.getProjectRevisions(typedArgs.projectId, {
        search: typedArgs.search,
        techRevs: typedArgs.techRevs,
        offset: typedArgs.offset,
        page: typedArgs.page,
        size: typedArgs.size,
      });
      // A non-aligned item offset cannot be reconstructed from the backend's
      // floor-derived pageNumber, so preserve the exact request value.
      const revisionOffset = typedArgs.offset ?? revisions.pageNumber * revisions.pageSize;
      const total = typeof revisions.total === "number" ? revisions.total : undefined;
      const hasMore = total !== undefined
        ? revisionOffset + revisions.numberOfElements < total
        : revisions.numberOfElements >= revisions.pageSize;

      const formattedResult = formatResponse(revisions, format, {
        pagination: {
          limit: revisions.pageSize,
          offset: revisionOffset,
          total,
          hasMore,
        },
        dataType: "revisions",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "list_deploy_repositories",
    category: "Repository",
    title: "List Deployment Repositories",
    description:
      "List all deployment repositories in OpenL Studio. Returns repository names, their types, and status information. Use this to discover all available deployment repositories before deploying projects.",
    schema: schemas.listDeployRepositoriesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      const repositories = await client.listDeployRepositories();
      const paginated = paginateResults(repositories, limit, offset);
      const formattedResult = formatResponse(paginated.data, format, {
        pagination: { limit, offset, total: paginated.total_count },
        dataType: "deploy_repositories",
      });

      return { content: [{ type: "text", text: formattedResult }] };
    },
  });
}

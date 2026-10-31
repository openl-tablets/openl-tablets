/**
 * Deployment tool handlers — list deployment repositories and deployments, and deploy/redeploy projects.
 */

import * as schemas from "../schemas.js";
import { formatListOf } from "../markdown-list.js";
import type * as Types from "../types.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";

export function registerDeploymentHandlers(): void {
  registerTool({
    name: "list_deploy_repositories",
    title: "List Deployment Repositories",
    description:
      "List the production repositories a project is deployed to, each with its 'id', 'name' and type; openl_deploy_project takes either.",
    schema: schemas.pagedListSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      const repositories = await client.listDeployRepositories();
      return textResult(formatListOf(repositories, limit, offset, {
        noun: "production repositories", one: "production repository",
        columns: [
          { header: "id", value: (repository) => repository.id },
          { header: "name", value: (repository) => repository.name },
          { header: "type", value: (repository) => repository.type },
        ],
      }));
    },
  });

  registerTool({
    name: "list_deployments",
    title: "List Active Deployments",
    description:
      "List the deployments of the production repositories, filtered by repository and deployed project. Each deployment answers its 'id', which openl_deploy_project takes to deploy to it again, its name and repository; the revisions and authors of its projects come only with the project filter, the exact name of a deployed project.",
    schema: schemas.listDeploymentsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      const deployments = await client.listDeployments({
        repository: args.repository,
        project: args.project,
      });
      return textResult(formatListOf(deployments, limit, offset, {
        noun: "deployments",
        columns: [
          { header: "id", value: (deployment) => deployment.id },
          { header: "name", value: (deployment) => deployment.name },
          { header: "repository", value: (deployment) => deployment.repository },
          { header: "projects", value: (deployment) => (deployment.items ?? []).map(deployedItem).join("; ") },
        ],
      }));
    },
  });

  registerTool({
    name: "deploy_project",
    title: "Deploy Project",
    description:
      "Deploy a project to a production repository, where OpenL Rule Services runs its rules. A new deployment " +
      "takes deploymentName and the production repository; deploymentId, from openl_list_deployments, deploys the " +
      "project to that deployment again, as a newer version or a rollback. Save the project first: Studio refuses " +
      "one with unsaved changes, and deploys the projects it depends on with it.",
    schema: schemas.deployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, deploymentId, deploymentName, repository, comment } = args;
      if (deploymentId !== undefined) {
        await client.redeployProject(deploymentId, { projectId, comment });
        return formatResult({ redeployed: deploymentId });
      }
      // The schema has checked that a new deployment names both
      const name = deploymentName as string;
      // The caller names the repository; Studio takes its id
      const repositoryId = await client.getProductionRepositoryId(repository as string);
      await client.deployProject({ projectId, deploymentName: name, productionRepositoryId: repositoryId, comment });
      return formatResult({ deployed: name, repository: repositoryId });
    },
  });
}

/** A project of a deployment: who deployed it, when, and from which design revision, when it is known. */
function deployedItem(item: NonNullable<Types.DeploymentViewModelShort["items"]>[number]): string {
  const deployed = item.modifiedBy ? ` by ${item.modifiedBy} at ${item.modifiedAt}` : "";
  const design = item.designRevision ? ` from revision ${item.designRevision.revision}` : "";
  return `${item.name}${deployed}${design}`;
}

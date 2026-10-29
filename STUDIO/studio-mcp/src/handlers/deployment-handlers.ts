/**
 * Deployment tool handlers — list deployment repositories and deployments, and deploy/redeploy projects.
 */

import * as schemas from "../schemas.js";
import { formatPage } from "../formatters.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";

export function registerDeploymentHandlers(): void {
  registerTool({
    name: "list_deploy_repositories",
    title: "List Deployment Repositories",
    description:
      "List all deployment repositories in OpenL Studio. Returns repository names, their types, and status information. Use this to discover all available deployment repositories before deploying projects.",
    schema: schemas.pagedListSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { limit, offset } = args;

      const repositories = await client.listDeployRepositories();
      return textResult(formatPage(repositories, "json", limit, offset));
    },
  });

  registerTool({
    name: "list_deployments",
    title: "List Active Deployments",
    description:
      "List active deployments across production environments, optionally filtered by production repository ID and deployed project name. Returns deployment names, repositories, and deployed project revisions.",
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
      return textResult(formatPage(deployments, args.response_format, limit, offset, { dataType: "deployments" }));
    },
  });

  registerTool({
    name: "deploy_project",
    title: "Deploy Project",
    description:
      "Deploy a project to a production repository, where OpenL Rule Services runs its rules. A new deployment " +
      "takes deploymentName and productionRepositoryId; deploymentId, from openl_list_deployments, deploys the " +
      "project to that deployment again, as a newer version or a rollback.",
    schema: schemas.deployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, deploymentId, deploymentName, productionRepositoryId, comment } = args;
      if (deploymentId !== undefined) {
        await client.redeployProject(deploymentId, { projectId, comment });
        return formatResult({
          success: true,
          message: `Successfully redeployed ${deploymentId} with project ${projectId}`,
          deploymentId,
        });
      }
      // The schema has checked that a new deployment names both
      const name = deploymentName as string;
      const repository = productionRepositoryId as string;
      await client.deployProject({
        projectId,
        deploymentName: name,
        // The caller names the repository; Studio takes its id
        productionRepositoryId: await client.getProductionRepositoryId(repository),
        comment,
      });
      return formatResult({ success: true, message: `Successfully deployed ${name} to ${repository}` });
    },
  });
}

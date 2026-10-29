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
    title: "Deploy Project to Production",
    description:
      "Deploy a project to production environment. Publishes rules to a deployment repository for runtime execution. Use production repository name (not ID) - e.g., 'Production Deployment' instead of 'production-deploy'.",
    schema: schemas.deployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      // Convert production repository name to ID for API call
      const productionRepositoryId = await client.getProductionRepositoryIdByName(args.productionRepositoryId);

      await client.deployProject({
        projectId: args.projectId,
        deploymentName: args.deploymentName,
        productionRepositoryId: productionRepositoryId,
        comment: args.comment,
      });

      const result = {
        success: true,
        message: `Successfully deployed ${args.deploymentName} to ${args.productionRepositoryId}`,
      };

      return formatResult(result);
    },
  });

  registerTool({
    name: "redeploy_project",
    title: "Redeploy with New Version",
    description:
      "Redeploy an existing deployment with a new project version. Use this to update a deployment with a newer version of the project or rollback to a previous version.",
    schema: schemas.redeployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.redeployProject(args.deploymentId, {
        projectId: args.projectId,
        comment: args.comment,
      });

      const result = {
        success: true,
        message: `Successfully redeployed ${args.deploymentId} with project ${args.projectId}`,
        deploymentId: args.deploymentId,
      };

      return formatResult(result);
    },
  });
}

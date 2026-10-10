/**
 * Deployment tool handlers — list deployments and deploy/redeploy projects.
 */

import * as schemas from "../schemas.js";
import { formatResponse, paginateResults } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";


export function registerDeploymentHandlers(): void {
  registerTool({
    name: "list_deployments",
    category: "Deployment",
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
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      const deployments = await client.listDeployments({
        repository: typedArgs.repository,
        project: typedArgs.project,
      });
      const paginated = paginateResults(deployments, limit, offset);
      const formattedResult = formatResponse(paginated.data, format, {
        pagination: { limit, offset, total: paginated.total_count },
        dataType: "deployments",
      });

      return { content: [{ type: "text", text: formattedResult }] };
    },
  });

  registerTool({
    name: "deploy_project",
    category: "Deployment",
    title: "Deploy Project to Production",
    description:
      "Deploy a project to production environment. Publishes rules to a deployment repository for runtime execution. Use production repository name (not ID) - e.g., 'Production Deployment' instead of 'production-deploy'.",
    schema: schemas.deployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // Convert production repository name to ID for API call
      const productionRepositoryId = await client.getProductionRepositoryIdByName(typedArgs.productionRepositoryId);

      await client.deployProject({
        projectId: typedArgs.projectId,
        deploymentName: typedArgs.deploymentName,
        productionRepositoryId: productionRepositoryId,
        comment: typedArgs.comment,
      });

      const result = {
        success: true,
        message: `Successfully deployed ${typedArgs.deploymentName} to ${typedArgs.productionRepositoryId}`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "redeploy_project",
    category: "Deployment",
    title: "Redeploy with New Version",
    description:
      "Redeploy an existing deployment with a new project version. Use this to update a deployment with a newer version of the project or rollback to a previous version.",
    schema: schemas.redeployProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      await client.redeployProject(typedArgs.deploymentId, {
        projectId: typedArgs.projectId,
        comment: typedArgs.comment,
      });

      const result = {
        success: true,
        message: `Successfully redeployed ${typedArgs.deploymentId} with project ${typedArgs.projectId}`,
        deploymentId: typedArgs.deploymentId,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
}

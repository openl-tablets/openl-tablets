/**
 * MCP Tool: Close Project
 * Closes an OpenL Studio project
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';
import { validateProjectName, validateRepositoryName } from '../utils/validators.js';

export const CloseProjectSchema = z.object({
  project_name: z.string().describe('Name of the project to close'),
  repository: z.string().optional().describe('Repository containing the project (optional if project name is unique)'),
});

export async function closeProject(
  params: z.infer<typeof CloseProjectSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing close_project tool', params);

    // Validate inputs
    validateProjectName(params.project_name);
    if (params.repository) {
      validateRepositoryName(params.repository);
    }

    // If repository not specified, try to find the project
    let repository = params.repository;
    if (!repository) {
      const allProjects = await client.listProjects();
      const matches = allProjects.filter((p) => p.name === params.project_name);

      if (matches.length === 0) {
        return `Error: Project "${params.project_name}" not found.`;
      }

      if (matches.length > 1) {
        const repos = matches.map((p) => p.repository).join(', ');
        return `Error: Multiple projects named "${params.project_name}" found in repositories: ${repos}. Please specify the repository name.`;
      }

      repository = matches[0].repository;
    }

    // Close the project
    await client.closeProject({
      repositoryName: repository,
      projectName: params.project_name,
    });

    return `✓ Project "${params.project_name}" closed successfully in repository "${repository}".`;
  } catch (error: any) {
    logger.error('Error in close_project tool:', error);

    if (error.code === 'PERMISSION_DENIED') {
      return `Error: Permission denied. You don't have access to close project "${params.project_name}".`;
    }

    if (error.code === 'NOT_FOUND') {
      return `Error: Project "${params.project_name}" not found or is not currently open.`;
    }

    return `Error closing project: ${error.message}`;
  }
}

export const closeProjectTool = {
  name: 'close_project',
  description: 'Close an OpenL Studio project and release its resources',
  inputSchema: {
    type: 'object' as const,
    properties: {
      project_name: {
        type: 'string',
        description: 'Name of the project to close',
      },
      repository: {
        type: 'string',
        description: 'Repository containing the project (optional if project name is unique)',
      },
    },
    required: ['project_name'],
  },
};

/**
 * MCP Tool: Open Project
 * Opens an OpenL Studio project
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';
import { validateProjectName, validateRepositoryName } from '../utils/validators.js';

export const OpenProjectSchema = z.object({
  project_name: z.string().describe('Name of the project to open'),
  repository: z.string().optional().describe('Repository containing the project (optional if project name is unique)'),
  open_dependencies: z.boolean().optional().default(true).describe('Whether to automatically open project dependencies (default: true)'),
});

export async function openProject(
  params: z.infer<typeof OpenProjectSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing open_project tool', params);

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

    // Open the project
    await client.openProject({
      repositoryName: repository,
      projectName: params.project_name,
      openDependencies: params.open_dependencies,
    });

    let response = `✓ Project "${params.project_name}" opened successfully in repository "${repository}".\n`;

    if (params.open_dependencies) {
      response += '\nProject dependencies were also opened automatically.';
    }

    return response;
  } catch (error: any) {
    logger.error('Error in open_project tool:', error);

    if (error.code === 'PERMISSION_DENIED') {
      return `Error: Permission denied. You don't have access to open project "${params.project_name}".`;
    }

    if (error.code === 'NOT_FOUND') {
      return `Error: Project "${params.project_name}" not found.`;
    }

    return `Error opening project: ${error.message}`;
  }
}

export const openProjectTool = {
  name: 'open_project',
  description: 'Open an OpenL Studio project. You can optionally open its dependencies automatically.',
  inputSchema: {
    type: 'object' as const,
    properties: {
      project_name: {
        type: 'string',
        description: 'Name of the project to open',
      },
      repository: {
        type: 'string',
        description: 'Repository containing the project (optional if project name is unique)',
      },
      open_dependencies: {
        type: 'boolean',
        description: 'Whether to automatically open project dependencies (default: true)',
        default: true,
      },
    },
    required: ['project_name'],
  },
};

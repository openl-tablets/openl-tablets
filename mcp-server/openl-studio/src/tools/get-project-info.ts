/**
 * MCP Tool: Get Project Info
 * Get detailed information about a specific project
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';
import { validateProjectName, validateRepositoryName } from '../utils/validators.js';

export const GetProjectInfoSchema = z.object({
  project_name: z.string().describe('Name of the project'),
  repository: z.string().optional().describe('Repository containing the project (optional if project name is unique)'),
});

export async function getProjectInfo(
  params: z.infer<typeof GetProjectInfoSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing get_project_info tool', params);

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
        return `Project "${params.project_name}" not found.`;
      }

      if (matches.length > 1) {
        const repos = matches.map((p) => p.repository).join(', ');
        return `Multiple projects named "${params.project_name}" found in repositories: ${repos}. Please specify the repository name.`;
      }

      repository = matches[0].repository;
    }

    // Get project info
    const info = await client.getProjectInfo(repository, params.project_name);

    // Format response
    let result = `# Project: ${info.name}\n\n`;
    result += `- **Repository**: ${info.repository}\n`;
    result += `- **Status**: ${info.status}\n`;

    if (info.lastModified) {
      result += `- **Last Modified**: ${info.lastModified}\n`;
    }

    if (info.version) {
      result += `- **Version**: ${info.version}\n`;
    }

    if (info.branch) {
      result += `- **Branch**: ${info.branch}\n`;
    }

    if (info.dependencies && info.dependencies.length > 0) {
      result += `\n## Dependencies:\n`;
      info.dependencies.forEach((dep) => {
        result += `- ${dep}\n`;
      });
    } else {
      result += `\n## Dependencies:\nNone\n`;
    }

    return result;
  } catch (error: any) {
    logger.error('Error in get_project_info tool:', error);
    return `Error getting project info: ${error.message}`;
  }
}

export const getProjectInfoTool = {
  name: 'get_project_info',
  description: 'Get detailed information about a specific OpenL Studio project, including its status, version, and dependencies',
  inputSchema: {
    type: 'object' as const,
    properties: {
      project_name: {
        type: 'string',
        description: 'Name of the project',
      },
      repository: {
        type: 'string',
        description: 'Repository containing the project (optional if project name is unique)',
      },
    },
    required: ['project_name'],
  },
};

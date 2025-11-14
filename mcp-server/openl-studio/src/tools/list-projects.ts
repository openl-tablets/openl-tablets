/**
 * MCP Tool: List Projects
 * Lists all available OpenL Studio projects
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';

export const ListProjectsSchema = z.object({
  repository: z.string().optional().describe('Filter projects by repository name (optional)'),
});

export async function listProjects(
  params: z.infer<typeof ListProjectsSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing list_projects tool', params);

    const projects = await client.listProjects(params.repository);

    if (projects.length === 0) {
      const repoMsg = params.repository ? ` in repository "${params.repository}"` : '';
      return `No projects found${repoMsg}.`;
    }

    // Group projects by repository
    const byRepo: Record<string, typeof projects> = {};
    projects.forEach((proj) => {
      if (!byRepo[proj.repository]) {
        byRepo[proj.repository] = [];
      }
      byRepo[proj.repository].push(proj);
    });

    // Format response
    let result = `Found ${projects.length} project(s):\n\n`;

    Object.entries(byRepo).forEach(([repoName, repoProjects]) => {
      result += `## Repository: ${repoName}\n\n`;
      repoProjects.forEach((proj) => {
        result += `- **${proj.name}**\n`;
        result += `  - Status: ${proj.status}\n`;
        if (proj.lastModified) {
          result += `  - Last Modified: ${proj.lastModified}\n`;
        }
        if (proj.version) {
          result += `  - Version: ${proj.version}\n`;
        }
        if (proj.branch) {
          result += `  - Branch: ${proj.branch}\n`;
        }
      });
      result += '\n';
    });

    return result;
  } catch (error: any) {
    logger.error('Error in list_projects tool:', error);
    return `Error listing projects: ${error.message}`;
  }
}

export const listProjectsTool = {
  name: 'list_projects',
  description: 'List all available OpenL Studio projects, optionally filtered by repository',
  inputSchema: {
    type: 'object' as const,
    properties: {
      repository: {
        type: 'string',
        description: 'Filter projects by repository name (optional)',
      },
    },
  },
};

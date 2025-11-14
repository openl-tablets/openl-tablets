/**
 * MCP Tool: Export Project
 * Exports an OpenL Studio project as a ZIP file
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';
import { validateProjectName, validateRepositoryName, validateFilePath } from '../utils/validators.js';

export const ExportProjectSchema = z.object({
  project_name: z.string().describe('Name of the project to export'),
  repository: z.string().optional().describe('Repository containing the project (optional if project name is unique)'),
  version: z.string().optional().describe('Specific version to export (optional, defaults to latest)'),
  output_path: z.string().optional().describe('Path where to save the ZIP file (optional, defaults to current directory)'),
});

export async function exportProject(
  params: z.infer<typeof ExportProjectSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing export_project tool', params);

    // Validate inputs
    validateProjectName(params.project_name);
    if (params.repository) {
      validateRepositoryName(params.repository);
    }
    if (params.output_path) {
      validateFilePath(params.output_path);
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

    // Export the project
    const outputPath = await client.exportProject({
      repositoryName: repository,
      projectName: params.project_name,
      version: params.version,
      outputPath: params.output_path,
    });

    let response = `✓ Project "${params.project_name}" exported successfully!\n\n`;
    response += `**Location**: ${outputPath}\n`;

    if (params.version) {
      response += `**Version**: ${params.version}\n`;
    }

    return response;
  } catch (error: any) {
    logger.error('Error in export_project tool:', error);

    if (error.code === 'PERMISSION_DENIED') {
      return `Error: Permission denied. You don't have access to export project "${params.project_name}".`;
    }

    if (error.code === 'NOT_FOUND') {
      return `Error: Project "${params.project_name}" not found.`;
    }

    return `Error exporting project: ${error.message}`;
  }
}

export const exportProjectTool = {
  name: 'export_project',
  description: 'Export an OpenL Studio project as a ZIP file. You can optionally specify the version and output path.',
  inputSchema: {
    type: 'object' as const,
    properties: {
      project_name: {
        type: 'string',
        description: 'Name of the project to export',
      },
      repository: {
        type: 'string',
        description: 'Repository containing the project (optional if project name is unique)',
      },
      version: {
        type: 'string',
        description: 'Specific version to export (optional, defaults to latest)',
      },
      output_path: {
        type: 'string',
        description: 'Path where to save the ZIP file (optional, defaults to current directory)',
      },
    },
    required: ['project_name'],
  },
};

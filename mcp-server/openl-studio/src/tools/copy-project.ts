/**
 * MCP Tool: Copy Project
 * Copies an OpenL Studio project to a new name/repository
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';
import { validateProjectName, validateRepositoryName } from '../utils/validators.js';

export const CopyProjectSchema = z.object({
  source_project: z.string().describe('Name of the source project to copy'),
  source_repository: z.string().optional().describe('Repository containing the source project (optional if project name is unique)'),
  destination_project: z.string().describe('Name for the new (copied) project'),
  destination_repository: z.string().optional().describe('Destination repository (optional, defaults to source repository)'),
  copy_history: z.boolean().optional().default(false).describe('Whether to copy version history (default: false)'),
  comment: z.string().optional().describe('Comment for the copy operation'),
});

export async function copyProject(
  params: z.infer<typeof CopyProjectSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing copy_project tool', params);

    // Validate inputs
    validateProjectName(params.source_project);
    validateProjectName(params.destination_project);
    if (params.source_repository) {
      validateRepositoryName(params.source_repository);
    }
    if (params.destination_repository) {
      validateRepositoryName(params.destination_repository);
    }

    // If source repository not specified, try to find the project
    let sourceRepository = params.source_repository;
    if (!sourceRepository) {
      const allProjects = await client.listProjects();
      const matches = allProjects.filter((p) => p.name === params.source_project);

      if (matches.length === 0) {
        return `Error: Source project "${params.source_project}" not found.`;
      }

      if (matches.length > 1) {
        const repos = matches.map((p) => p.repository).join(', ');
        return `Error: Multiple projects named "${params.source_project}" found in repositories: ${repos}. Please specify the source repository name.`;
      }

      sourceRepository = matches[0].repository;
    }

    // Default destination repository to source repository
    const destinationRepository = params.destination_repository || sourceRepository;

    // Copy the project
    await client.copyProject({
      sourceRepository,
      sourceProject: params.source_project,
      destinationRepository,
      destinationProject: params.destination_project,
      copyHistory: params.copy_history,
      comment: params.comment,
    });

    let response = `✓ Project copied successfully!\n\n`;
    response += `**Source**: ${params.source_project} (${sourceRepository})\n`;
    response += `**Destination**: ${params.destination_project} (${destinationRepository})\n`;

    if (params.copy_history) {
      response += `\nVersion history was copied.`;
    } else {
      response += `\nOnly the latest version was copied.`;
    }

    return response;
  } catch (error: any) {
    logger.error('Error in copy_project tool:', error);

    if (error.code === 'PERMISSION_DENIED') {
      return `Error: Permission denied. You don't have access to copy projects.`;
    }

    if (error.code === 'NOT_FOUND') {
      return `Error: Source project "${params.source_project}" not found.`;
    }

    if (error.code === 'VALIDATION_ERROR') {
      return `Error: ${error.message}. Please check project names and repository names.`;
    }

    return `Error copying project: ${error.message}`;
  }
}

export const copyProjectTool = {
  name: 'copy_project',
  description: 'Copy an OpenL Studio project to a new name and/or repository. You can optionally copy the version history.',
  inputSchema: {
    type: 'object' as const,
    properties: {
      source_project: {
        type: 'string',
        description: 'Name of the source project to copy',
      },
      source_repository: {
        type: 'string',
        description: 'Repository containing the source project (optional if project name is unique)',
      },
      destination_project: {
        type: 'string',
        description: 'Name for the new (copied) project',
      },
      destination_repository: {
        type: 'string',
        description: 'Destination repository (optional, defaults to source repository)',
      },
      copy_history: {
        type: 'boolean',
        description: 'Whether to copy version history (default: false)',
        default: false,
      },
      comment: {
        type: 'string',
        description: 'Comment for the copy operation',
      },
    },
    required: ['source_project', 'destination_project'],
  },
};

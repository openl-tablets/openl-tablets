/**
 * MCP Tool: List Repositories
 * Lists all configured OpenL Studio repositories
 */

import { z } from 'zod';
import type { OpenLClient } from '../client/openl-client.js';
import { logger } from '../utils/logger.js';

// No parameters for this tool
export const ListRepositoriesSchema = z.object({});

export async function listRepositories(
  _params: z.infer<typeof ListRepositoriesSchema>,
  client: OpenLClient
): Promise<string> {
  try {
    logger.info('Executing list_repositories tool');

    const repositories = await client.listRepositories();

    if (repositories.length === 0) {
      return 'No repositories found in OpenL Studio.';
    }

    // Format response as a readable list
    let result = `Found ${repositories.length} repository(ies):\n\n`;
    repositories.forEach((repo, index) => {
      result += `${index + 1}. **${repo.name}**\n`;
      result += `   - Type: ${repo.type}\n`;
      if (repo.path) {
        result += `   - Path: ${repo.path}\n`;
      }
      result += '\n';
    });

    return result;
  } catch (error: any) {
    logger.error('Error in list_repositories tool:', error);
    return `Error listing repositories: ${error.message}`;
  }
}

export const listRepositoriesTool = {
  name: 'list_repositories',
  description: 'List all configured OpenL Studio repositories',
  inputSchema: {
    type: 'object' as const,
    properties: {},
  },
};

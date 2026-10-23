/**
 * Mock data for OpenL Studio API responses
 */

import type * as Types from '../types.js';

/** A page of OpenL Studio holding `content`: by default the first page of 50 items, without a total. */
export function pageOf<T>(content: T[], page: Partial<Types.PageResponse<T>> = {}): Types.PageResponse<T> {
  return { content, pageNumber: 0, pageSize: 50, numberOfElements: content.length, ...page };
}

/** A project of the design repository as OpenL Studio describes it, in the given status. */
export function projectView(
  status: Types.ProjectStatus,
  overrides: Partial<Types.ProjectViewModel> = {},
): Partial<Types.ProjectViewModel> {
  return {
    id: 'design:project1:hash123',
    name: 'project1',
    repository: 'design',
    status,
    path: 'project1',
    modifiedBy: 'admin',
    modifiedAt: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

/** A debug session suspended at the entry of the spreadsheet `calc_42`. */
export const suspendedStack: Types.DebugStackView = {
  status: 'suspended',
  frames: [
    {
      index: 0,
      depth: 1,
      uri: 'P/Rules.xlsx?sheet=Main&range=B2:D8',
      tableId: 'calc_42',
      name: 'CalcRule',
      kind: 'spreadsheet',
      active: true,
      completed: false,
      error: false,
    },
  ],
};

export const mockRepositories: Types.Repository[] = [
  {
    id: 'design',
    name: 'Design Repository',
    type: 'git',
    features: {
      branches: true,
      comments: true,
      mappedFolders: false,
      searchable: true,
    },
  },
  {
    id: 'production',
    name: 'Production Repository',
    type: 'git',
    features: {
      branches: false,
      comments: true,
      mappedFolders: false,
      searchable: false,
    },
  },
];

export const mockDeployments: Types.DeploymentViewModelShort[] = [
  {
    id: 'deploy-001',
    name: 'insurance-rules-v1.2.3',
    repository: 'production',
    items: [{
      name: 'insurance-rules',
      modifiedAt: '2025-11-09T16:00:00Z',
      modifiedBy: 'admin',
    }],
  },
  {
    id: 'deploy-002',
    name: 'loan-calculator-v2.0.0',
    repository: 'production',
    items: [{
      name: 'loan-calculator',
      modifiedAt: '2025-11-08T10:30:00Z',
      modifiedBy: 'user1',
    }],
  },
];

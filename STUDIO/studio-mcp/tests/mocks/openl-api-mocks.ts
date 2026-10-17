/**
 * Mock data for OpenL Studio API responses
 */

import type * as Types from '../../src/types.js';

export const mockRepositories: Types.RepositoryInfo[] = [
  {
    aclId: 'design',
    id: 'design',
    name: 'Design Repository',
    type: 'git',
    features: {
      branches: true,
      mappedFolders: false,
      searchable: true,
    },
  },
  {
    aclId: 'production',
    id: 'production',
    name: 'Production Repository',
    type: 'git',
    features: {
      branches: false,
      mappedFolders: false,
      searchable: false,
    },
  },
];

export const mockProjects: Types.ProjectViewModel[] = [
  {
    name: 'insurance-rules',
    modifiedBy: 'admin',
    modifiedAt: '2025-11-10T10:30:00Z',
    path: 'insurance-rules',
    id: 'design:insurance-rules',
    revision: 'abc123',
    status: 'OPENED',
    repository: 'design',
    comment: 'Updated premium calculation',
    tags: {
      category: 'insurance',
      version: 'v1.2.3',
    },
  },
  {
    name: 'loan-calculator',
    modifiedBy: 'user1',
    modifiedAt: '2025-11-09T15:20:00Z',
    path: 'loan-calculator',
    id: 'design:loan-calculator',
    revision: 'def456',
    status: 'CLOSED',
    repository: 'design',
    comment: 'Initial version',
  },
];

export const mockProjectInfo: Types.ProjectInfo = {
  name: 'insurance-rules',
  repository: 'design',
  path: 'insurance-rules',
  branch: 'main',
  modules: [
    {
      name: 'Rules Module',
      rulesRootPath: 'Rules.xlsx',
    },
    {
      name: 'Datatypes Module',
      rulesRootPath: 'Datatypes.xlsx',
    },
  ],
  dependencies: [
    {
      name: 'common-datatypes',
      autoIncluded: true,
    },
  ],
  classpath: ['lib/commons.jar'],
  tags: {
    category: 'insurance',
  },
};

export const mockTables: Types.SummaryTableView[] = [
  {
    id: 'Rules.xls_1234',
    tableType: 'SimpleRules',
    kind: 'Rules',
    name: 'CalculatePremium',
    returnType: 'Double',
    signature: 'Double CalculatePremium(String vehicleType, Integer age)',
    file: 'Rules.xlsx',
    pos: '1',
    properties: {
      category: 'Premium Calculation',
    },
  },
  {
    id: 'Datatypes.xls_5678',
    tableType: 'Datatype',
    kind: 'Datatype',
    name: 'Policy',
    file: 'Datatypes.xlsx',
    pos: '1',
  },
];

export const mockBranches: string[] = ['main', 'develop', 'feature/new-rules'];

export const mockDeployments: Types.DeploymentViewModel_Short[] = [
  {
    id: 'deploy-001',
    name: 'insurance-rules-v1.2.3',
    repository: 'production',
    items: [{
      name: 'insurance-rules',
      revision: 'v1.2.3',
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
      revision: 'v2.0.0',
      modifiedAt: '2025-11-08T10:30:00Z',
      modifiedBy: 'user1',
    }],
  },
];

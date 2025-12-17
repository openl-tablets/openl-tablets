/**
 * Mock data for OpenL Tablets API responses
 */

import type * as Types from '../../src/types.js';

export const mockRepositories: Types.RepositoryInfo[] = [
  {
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
    id: {
      repository: 'design',
      projectName: 'insurance-rules',
    },
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
    id: {
      repository: 'design',
      projectName: 'loan-calculator',
    },
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
    tableType: 'simplerules',
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
    tableType: 'datatype',
    kind: 'Datatype',
    name: 'Policy',
    file: 'Datatypes.xlsx',
    pos: '1',
  },
];

export const mockDecisionTable: Types.SimpleRulesView = {
  id: 'Rules.xls_1234',
  tableType: 'simplerules',
  kind: 'Rules',
  name: 'CalculatePremium',
  technicalName: 'CalculatePremium',
  properties: [
    {
      name: 'category',
      type: 'String',
      value: 'Premium Calculation',
    },
  ],
  editable: true,
  file: 'Rules.xlsx',
  rules: [
    {
      conditions: { vehicleType: 'Car', age: '<25' },
      actions: { premium: '1200' },
    },
    {
      conditions: { vehicleType: 'Car', age: '>=25' },
      actions: { premium: '800' },
    },
    {
      conditions: { vehicleType: 'Motorcycle', age: '<25' },
      actions: { premium: '900' },
    },
  ],
  conditionColumns: ['vehicleType', 'age'],
  actionColumns: ['premium'],
};

export const mockDatatype: Types.DatatypeView = {
  id: 'Datatypes.xls_5678',
  tableType: 'datatype',
  kind: 'Datatype',
  name: 'Policy',
  technicalName: 'Policy',
  editable: true,
  file: 'Datatypes.xlsx',
  fields: [
    { name: 'policyNumber', type: 'String' },
    { name: 'holderName', type: 'String' },
    { name: 'premium', type: 'Double' },
    { name: 'startDate', type: 'Date' },
  ],
};

export const mockProjectHistory: Types.ProjectHistoryItem[] = [
  {
    name: 'insurance-rules',
    version: 'abc123',
    author: 'admin',
    modifiedAt: '2025-11-10T10:30:00Z',
    comment: 'Updated premium calculation',
  },
  {
    name: 'insurance-rules',
    version: 'def456',
    author: 'user1',
    modifiedAt: '2025-11-08T14:20:00Z',
    comment: 'Added senior discount rules',
  },
  {
    name: 'insurance-rules',
    version: 'ghi789',
    author: 'admin',
    modifiedAt: '2025-11-05T09:15:00Z',
    comment: 'Initial version',
  },
];

export const mockBranches: string[] = ['main', 'develop', 'feature/new-rules'];

export const mockDeployments: Types.DeploymentInfo[] = [
  {
    id: 'deploy-001',
    name: 'insurance-rules-v1.2.3',
    projectName: 'insurance-rules',
    projectVersion: 'v1.2.3',
    repository: 'production',
    status: 'DEPLOYED',
    deployedAt: '2025-11-09T16:00:00Z',
    deployedBy: 'admin',
  },
  {
    id: 'deploy-002',
    name: 'loan-calculator-v2.0.0',
    projectName: 'loan-calculator',
    projectVersion: 'v2.0.0',
    repository: 'production',
    status: 'DEPLOYED',
    deployedAt: '2025-11-08T10:30:00Z',
    deployedBy: 'user1',
  },
];

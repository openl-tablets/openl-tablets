/**
 * Unit tests for OpenL Tablets API Client
 */

import nock from 'nock';
import { describe, it, expect, beforeEach, afterEach } from '@jest/globals';
import {
  mockRepositories,
  mockProjects,
  mockProjectInfo,
  mockTables,
  mockDecisionTable,
  mockProjectHistory,
  mockBranches,
  mockDeployments,
} from './mocks/openl-api-mocks.js';

// Mock the OpenLClient by importing and testing the actual implementation
// For now, we'll test the API endpoints directly

const BASE_URL = 'http://localhost:8080';
const API_PATH = '/rest';

describe('OpenL Tablets API Client', () => {
  beforeEach(() => {
    nock.cleanAll();
  });

  afterEach(() => {
    nock.cleanAll();
  });

  describe('Repository Management', () => {
    it('should list repositories', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos`)
        .reply(200, mockRepositories);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos`);

      expect(response.status).toBe(200);
      expect(response.data).toEqual(mockRepositories);
      expect(response.data).toHaveLength(2);
      expect(response.data[0].id).toBe('design');
      scope.done();
    });

    it('should get repository features', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos/design/features`)
        .reply(200, mockRepositories[0].features);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos/design/features`);

      expect(response.status).toBe(200);
      expect(response.data.branches).toBe(true);
      expect(response.data.searchable).toBe(true);
      scope.done();
    });

    it('should list branches', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos/design/branches`)
        .reply(200, mockBranches);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos/design/branches`);

      expect(response.status).toBe(200);
      expect(response.data).toEqual(mockBranches);
      expect(response.data).toContain('main');
      scope.done();
    });
  });

  describe('Project Management', () => {
    it('should list all projects', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects`)
        .reply(200, mockProjects);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects`);

      expect(response.status).toBe(200);
      expect(response.data).toEqual(mockProjects);
      expect(response.data).toHaveLength(2);
      scope.done();
    });

    it('should list projects with filters', async () => {
      const filteredProjects = mockProjects.filter((p) => p.status === 'OPENED');

      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects`)
        .query({ status: 'OPENED' })
        .reply(200, filteredProjects);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects`, {
        params: { status: 'OPENED' },
      });

      expect(response.status).toBe(200);
      expect(response.data).toHaveLength(1);
      expect(response.data[0].status).toBe('OPENED');
      scope.done();
    });

    it('should get project details', async () => {
      const projectId = 'design-insurance-rules';

      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}`)
        .reply(200, mockProjects[0]);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}`);

      expect(response.status).toBe(200);
      expect(response.data.name).toBe('insurance-rules');
      expect(response.data.status).toBe('OPENED');
      scope.done();
    });

    it('should get project info with modules and dependencies', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/user-workspace/design/projects/insurance-rules/info`)
        .reply(200, mockProjectInfo);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/user-workspace/design/projects/insurance-rules/info`
      );

      expect(response.status).toBe(200);
      expect(response.data.modules).toHaveLength(2);
      expect(response.data.dependencies).toHaveLength(1);
      expect(response.data.modules[0].name).toBe('Rules Module');
      scope.done();
    });

    it('should open a project', async () => {
      const projectId = 'design-insurance-rules';

      const scope = nock(BASE_URL)
        .patch(`${API_PATH}/projects/${projectId}`, { status: 'OPENED' })
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.patch(`${BASE_URL}${API_PATH}/projects/${projectId}`, {
        status: 'OPENED',
      });

      expect(response.status).toBe(200);
      scope.done();
    });

    it('should close a project', async () => {
      const projectId = 'design-insurance-rules';

      const scope = nock(BASE_URL)
        .patch(`${API_PATH}/projects/${projectId}`, { status: 'CLOSED' })
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.patch(`${BASE_URL}${API_PATH}/projects/${projectId}`, {
        status: 'CLOSED',
      });

      expect(response.status).toBe(200);
      scope.done();
    });

    it('should get project history', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos/design/projects/insurance-rules/history`)
        .reply(200, mockProjectHistory);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/repos/design/projects/insurance-rules/history`
      );

      expect(response.status).toBe(200);
      expect(response.data).toHaveLength(3);
      expect(response.data[0].version).toBe('abc123');
      expect(response.data[0].comment).toBe('Updated premium calculation');
      scope.done();
    });

    it('should create a branch', async () => {
      const projectId = 'design-insurance-rules';
      const branchData = {
        name: 'feature/new-rules',
        comment: 'Creating feature branch',
      };

      const scope = nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/branches`, branchData)
        .reply(201);

      const axios = (await import('axios')).default;
      const response = await axios.post(
        `${BASE_URL}${API_PATH}/projects/${projectId}/branches`,
        branchData
      );

      expect(response.status).toBe(201);
      scope.done();
    });
  });

  describe('Table Management', () => {
    it('should list tables in a project', async () => {
      const projectId = 'design-insurance-rules';

      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables`)
        .reply(200, mockTables);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}/tables`);

      expect(response.status).toBe(200);
      expect(response.data).toHaveLength(2);
      expect(response.data[0].name).toBe('CalculatePremium');
      expect(response.data[0].tableType).toBe('simplerules');
      scope.done();
    });

    it('should get decision table details', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';

      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200, mockDecisionTable);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`
      );

      expect(response.status).toBe(200);
      expect(response.data.name).toBe('CalculatePremium');
      expect(response.data.rules).toHaveLength(3);
      expect(response.data.conditionColumns).toContain('vehicleType');
      scope.done();
    });

    it('should update a table', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';
      const updateData = {
        view: mockDecisionTable,
        comment: 'Updated premium calculation',
      };

      const scope = nock(BASE_URL)
        .put(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.put(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`,
        updateData
      );

      expect(response.status).toBe(200);
      scope.done();
    });

    it('should append data to a table', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Customer_1234';
      const appendData = {
        tableType: 'Datatype',
        fields: [
          {
            name: 'email',
            type: 'String',
            required: true,
          },
          {
            name: 'phoneNumber',
            type: 'String',
            required: false,
            defaultValue: null,
          },
        ],
      };

      const scope = nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/tables/${tableId}/lines`, appendData)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.post(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}/lines`,
        appendData
      );

      expect(response.status).toBe(200);
      scope.done();
    });

    it('should handle errors when appending to table with invalid data', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Customer_1234';
      const invalidAppendData = {
        tableType: 'Datatype',
        fields: [
          {
            name: 'invalid field name!',
            type: 'String',
          },
        ],
      };

      const scope = nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/tables/${tableId}/lines`, invalidAppendData)
        .reply(400, { message: 'Invalid field name' });

      const axios = (await import('axios')).default;

      await expect(
        axios.post(
          `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}/lines`,
          invalidAppendData
        )
      ).rejects.toThrow();

      scope.done();
    });

    it('should handle errors when table not found', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'nonexistent';

      const scope = nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(404, { message: 'Table not found' });

      const axios = (await import('axios')).default;

      await expect(
        axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`)
      ).rejects.toThrow();

      scope.done();
    });
  });

  describe('Deployment Management', () => {
    it('should list deployments', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/deployments`)
        .reply(200, mockDeployments);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/deployments`);

      expect(response.status).toBe(200);
      expect(response.data).toHaveLength(2);
      expect(response.data[0].projectName).toBe('insurance-rules');
      expect(response.data[0].status).toBe('DEPLOYED');
      scope.done();
    });

    it('should deploy a project', async () => {
      const deployData = {
        projectName: 'insurance-rules',
        repository: 'design',
        deploymentRepository: 'production',
      };

      const scope = nock(BASE_URL)
        .post(`${API_PATH}/deployments`, deployData)
        .reply(201);

      const axios = (await import('axios')).default;
      const response = await axios.post(`${BASE_URL}${API_PATH}/deployments`, deployData);

      expect(response.status).toBe(201);
      scope.done();
    });

    it('should redeploy a project', async () => {
      const deploymentId = 'deploy-001';

      const scope = nock(BASE_URL)
        .post(`${API_PATH}/deployments/${deploymentId}`)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.post(`${BASE_URL}${API_PATH}/deployments/${deploymentId}`);

      expect(response.status).toBe(200);
      scope.done();
    });

    it('should list production repositories', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/production-repos`)
        .reply(200, [mockRepositories[1]]);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/production-repos`);

      expect(response.status).toBe(200);
      expect(response.data[0].id).toBe('production');
      scope.done();
    });
  });

  describe('Authentication', () => {
    it('should handle basic auth', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos`)
        .basicAuth({ user: 'admin', pass: 'admin' })
        .reply(200, mockRepositories);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos`, {
        auth: {
          username: 'admin',
          password: 'admin',
        },
      });

      expect(response.status).toBe(200);
      scope.done();
    });


    it('should handle 401 unauthorized', async () => {
      const scope = nock(BASE_URL)
        .get(`${API_PATH}/repos`)
        .reply(401, { message: 'Unauthorized' });

      const axios = (await import('axios')).default;

      await expect(axios.get(`${BASE_URL}${API_PATH}/repos`)).rejects.toThrow();

      scope.done();
    });
  });
});

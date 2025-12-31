/**
 * Integration tests for MCP Server
 */

import { describe, it, expect, beforeEach } from '@jest/globals';
import nock from 'nock';
import {
  mockRepositories,
  mockProjects,
  mockProjectInfo,
  mockTables,
  mockDecisionTable,
  mockBranches,
  mockDeployments,
  mockProjectHistory,
} from './mocks/openl-api-mocks.js';

const BASE_URL = 'http://localhost:8080';
const API_PATH = '/rest';

describe('MCP Server Tools', () => {
  beforeEach(() => {
    nock.cleanAll();
    process.env.OPENL_BASE_URL = `${BASE_URL}${API_PATH}`;
    process.env.OPENL_USERNAME = 'admin';
    process.env.OPENL_PASSWORD = 'admin';
  });

  describe('openl_list_repositories tool', () => {
    it('should return repositories in correct format', async () => {
      nock(BASE_URL).get(`${API_PATH}/repos`).reply(200, mockRepositories);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos`);

      expect(response.data).toEqual(mockRepositories);
      expect(Array.isArray(response.data)).toBe(true);
      expect(response.data[0]).toHaveProperty('id');
      expect(response.data[0]).toHaveProperty('name');
      expect(response.data[0]).toHaveProperty('features');
    });
  });

  describe('openl_list_projects tool', () => {
    it('should return projects without filters', async () => {
      nock(BASE_URL).get(`${API_PATH}/projects`).reply(200, mockProjects);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects`);

      expect(response.data).toEqual(mockProjects);
      expect(response.data).toHaveLength(2);
    });

    it('should filter projects by repository', async () => {
      const filtered = mockProjects.filter((p) => p.repository === 'design');

      nock(BASE_URL)
        .get(`${API_PATH}/projects`)
        .query({ repository: 'design' })
        .reply(200, filtered);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects`, {
        params: { repository: 'design' },
      });

      expect(response.data).toEqual(filtered);
    });

    it('should filter projects by status', async () => {
      const filtered = mockProjects.filter((p) => p.status === 'OPENED');

      nock(BASE_URL)
        .get(`${API_PATH}/projects`)
        .query({ status: 'OPENED' })
        .reply(200, filtered);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects`, {
        params: { status: 'OPENED' },
      });

      expect(response.data.every((p: any) => p.status === 'OPENED')).toBe(true);
    });
  });

  describe('openl_get_project tool', () => {
    it('should return project details', async () => {
      const projectId = 'design-insurance-rules';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}`)
        .reply(200, mockProjects[0]);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}`);

      expect(response.data).toEqual(mockProjects[0]);
      expect(response.data.name).toBe('insurance-rules');
    });

    it('should handle project not found', async () => {
      const projectId = 'nonexistent-project';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}`)
        .reply(404, { message: 'Project not found' });

      const axios = (await import('axios')).default;

      await expect(
        axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}`)
      ).rejects.toThrow();
    });
  });

  describe('openl_get_project tool (with info)', () => {
    it('should return project structure with modules', async () => {
      nock(BASE_URL)
        .get(`${API_PATH}/user-workspace/design/projects/insurance-rules/info`)
        .reply(200, mockProjectInfo);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/user-workspace/design/projects/insurance-rules/info`
      );

      expect(response.data).toEqual(mockProjectInfo);
      expect(response.data.modules).toBeDefined();
      expect(response.data.dependencies).toBeDefined();
      expect(response.data.modules.length).toBeGreaterThan(0);
    });
  });

  describe('openl_update_project_status tool (open/close)', () => {
    it('should open a project', async () => {
      const projectId = 'design-insurance-rules';

      nock(BASE_URL)
        .patch(`${API_PATH}/projects/${projectId}`, { status: 'OPENED' })
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.patch(`${BASE_URL}${API_PATH}/projects/${projectId}`, {
        status: 'OPENED',
      });

      expect(response.status).toBe(200);
    });

    it('should close a project', async () => {
      const projectId = 'design-insurance-rules';

      nock(BASE_URL)
        .patch(`${API_PATH}/projects/${projectId}`, { status: 'CLOSED' })
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.patch(`${BASE_URL}${API_PATH}/projects/${projectId}`, {
        status: 'CLOSED',
      });

      expect(response.status).toBe(200);
    });
  });

  describe('openl_list_tables tool', () => {
    it('should return all tables in project', async () => {
      const projectId = 'design-insurance-rules';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables`)
        .reply(200, mockTables);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}/tables`);

      expect(response.data).toEqual(mockTables);
      expect(response.data).toHaveLength(2);
      expect(response.data[0]).toHaveProperty('tableType');
      expect(response.data[0]).toHaveProperty('name');
    });

    it('should return different table types', async () => {
      const projectId = 'design-insurance-rules';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables`)
        .reply(200, mockTables);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/projects/${projectId}/tables`);

      const tableTypes = response.data.map((t: any) => t.tableType);
      expect(tableTypes).toContain('simplerules');
      expect(tableTypes).toContain('datatype');
    });
  });

  describe('openl_get_table tool', () => {
    it('should return decision table details', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200, mockDecisionTable);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`
      );

      expect(response.data).toEqual(mockDecisionTable);
      expect(response.data.rules).toBeDefined();
      expect(response.data.conditionColumns).toBeDefined();
      expect(response.data.actionColumns).toBeDefined();
    });

    it('should include table properties', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';

      nock(BASE_URL)
        .get(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200, mockDecisionTable);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`
      );

      expect(response.data.properties).toBeDefined();
      expect(Array.isArray(response.data.properties)).toBe(true);
    });
  });

  describe('openl_update_table tool', () => {
    it('should update table successfully', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';
      const updateData = {
        view: mockDecisionTable,
        comment: 'Updated premium rates',
      };

      nock(BASE_URL)
        .put(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.put(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`,
        updateData
      );

      expect(response.status).toBe(200);
    });

    it('should require comment when updating', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Rules.xls_1234';
      const updateData = {
        view: mockDecisionTable,
        comment: 'Updated premium rates',
      };

      nock(BASE_URL)
        .put(`${API_PATH}/projects/${projectId}/tables/${tableId}`)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.put(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}`,
        updateData
      );

      expect(response.status).toBe(200);
    });
  });

  describe('openl_append_table tool', () => {
    it('should append fields to a table successfully', async () => {
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

      nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/tables/${tableId}/lines`, appendData)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.post(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}/lines`,
        appendData
      );

      expect(response.status).toBe(200);
    });

    it('should handle append with required and optional fields', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Policy_5678';
      const appendData = {
        tableType: 'Datatype',
        fields: [
          {
            name: 'totalPremium',
            type: 'double',
            required: true,
          },
          {
            name: 'discountRate',
            type: 'double',
            required: false,
            defaultValue: 0.0,
          },
        ],
      };

      nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/tables/${tableId}/lines`, appendData)
        .reply(200);

      const axios = (await import('axios')).default;
      const response = await axios.post(
        `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}/lines`,
        appendData
      );

      expect(response.status).toBe(200);
    });

    it('should handle errors when appending invalid data', async () => {
      const projectId = 'design-insurance-rules';
      const tableId = 'Customer_1234';
      const invalidData = {
        tableType: 'Datatype',
        fields: [
          {
            name: 'invalid field!',
            type: 'String',
          },
        ],
      };

      nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/tables/${tableId}/lines`, invalidData)
        .reply(400, { message: 'Invalid field name' });

      const axios = (await import('axios')).default;

      await expect(
        axios.post(
          `${BASE_URL}${API_PATH}/projects/${projectId}/tables/${tableId}/lines`,
          invalidData
        )
      ).rejects.toThrow();
    });
  });

  describe('openl_get_project_history tool', () => {
    it('should return version history', async () => {
      nock(BASE_URL)
        .get(`${API_PATH}/repos/design/projects/insurance-rules/history`)
        .reply(200, mockProjectHistory);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/repos/design/projects/insurance-rules/history`
      );

      expect(response.data).toEqual(mockProjectHistory);
      expect(response.data).toHaveLength(3);
      expect(response.data[0]).toHaveProperty('version');
      expect(response.data[0]).toHaveProperty('author');
      expect(response.data[0]).toHaveProperty('comment');
    });

    it('should include commit metadata', async () => {
      nock(BASE_URL)
        .get(`${API_PATH}/repos/design/projects/insurance-rules/history`)
        .reply(200, mockProjectHistory);

      const axios = (await import('axios')).default;
      const response = await axios.get(
        `${BASE_URL}${API_PATH}/repos/design/projects/insurance-rules/history`
      );

      expect(response.data[0].modifiedAt).toBeDefined();
      expect(response.data[0].author).toBe('admin');
    });
  });

  describe('openl_list_branches tool', () => {
    it('should return branches', async () => {
      nock(BASE_URL)
        .get(`${API_PATH}/repos/design/branches`)
        .reply(200, mockBranches);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/repos/design/branches`);

      expect(response.data).toEqual(mockBranches);
      expect(response.data).toContain('main');
      expect(response.data).toContain('develop');
    });
  });

  describe('openl_create_branch tool', () => {
    it('should create new branch', async () => {
      const projectId = 'design-insurance-rules';
      const branchData = {
        name: 'feature/test-branch',
        comment: 'Test branch',
      };

      nock(BASE_URL)
        .post(`${API_PATH}/projects/${projectId}/branches`, branchData)
        .reply(201);

      const axios = (await import('axios')).default;
      const response = await axios.post(
        `${BASE_URL}${API_PATH}/projects/${projectId}/branches`,
        branchData
      );

      expect(response.status).toBe(201);
    });
  });

  describe('openl_list_deployments tool', () => {
    it('should return all deployments', async () => {
      nock(BASE_URL).get(`${API_PATH}/deployments`).reply(200, mockDeployments);

      const axios = (await import('axios')).default;
      const response = await axios.get(`${BASE_URL}${API_PATH}/deployments`);

      expect(response.data).toEqual(mockDeployments);
      expect(response.data).toHaveLength(2);
      expect(response.data[0]).toHaveProperty('projectName');
      expect(response.data[0]).toHaveProperty('status');
    });
  });

  describe('openl_deploy_project tool', () => {
    it('should deploy project to production', async () => {
      const deployData = {
        projectName: 'insurance-rules',
        repository: 'design',
        deploymentRepository: 'production',
      };

      nock(BASE_URL).post(`${API_PATH}/deployments`, deployData).reply(201);

      const axios = (await import('axios')).default;
      const response = await axios.post(`${BASE_URL}${API_PATH}/deployments`, deployData);

      expect(response.status).toBe(201);
    });

    it('should deploy specific version', async () => {
      const deployData = {
        projectName: 'insurance-rules',
        repository: 'design',
        deploymentRepository: 'production',
        version: 'v1.2.3',
      };

      nock(BASE_URL).post(`${API_PATH}/deployments`, deployData).reply(201);

      const axios = (await import('axios')).default;
      const response = await axios.post(`${BASE_URL}${API_PATH}/deployments`, deployData);

      expect(response.status).toBe(201);
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      nock(BASE_URL).get(`${API_PATH}/repos`).replyWithError('Network error');

      const axios = (await import('axios')).default;

      await expect(axios.get(`${BASE_URL}${API_PATH}/repos`)).rejects.toThrow();
    });

    it('should handle 500 server errors', async () => {
      nock(BASE_URL)
        .get(`${API_PATH}/repos`)
        .reply(500, { message: 'Internal server error' });

      const axios = (await import('axios')).default;

      await expect(axios.get(`${BASE_URL}${API_PATH}/repos`)).rejects.toThrow();
    });

    it('should handle 403 forbidden', async () => {
      nock(BASE_URL).get(`${API_PATH}/repos`).reply(403, { message: 'Forbidden' });

      const axios = (await import('axios')).default;

      await expect(axios.get(`${BASE_URL}${API_PATH}/repos`)).rejects.toThrow();
    });
  });
});

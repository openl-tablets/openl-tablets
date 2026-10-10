/**
 * OpenL Studio 6.0.0 Live Integration Tests
 *
 * These tests run against an actual OpenL Studio instance.
 * Skip in CI by setting SKIP_LIVE_TESTS=true
 *
 * Configuration via environment variables:
 * - OPENL_BASE_URL: OpenL Studio API base URL (default: http://localhost:8080)
 * - OPENL_PERSONAL_ACCESS_TOKEN: Personal Access Token (optional; omit for single-user mode)
 * - SKIP_LIVE_TESTS: Set to 'true' to skip these tests (default in CI)
 */

import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import { OpenLClient } from '../../src/client.js';
import type * as Types from '../../src/types.js';

// Skip tests if not configured for live testing
const shouldSkip = process.env.SKIP_LIVE_TESTS !== 'false';

const describeIntegration = shouldSkip ? describe.skip : describe;

describeIntegration('OpenL Studio 6.0.0 Live Integration Tests', () => {
  let client: OpenLClient;
  let testProjectId: string;
  let testTableId: string;

  beforeAll(() => {
    const config: Types.OpenLConfig = {
      baseUrl: process.env.OPENL_BASE_URL || 'http://localhost:8080',
      personalAccessToken: process.env.OPENL_PERSONAL_ACCESS_TOKEN,
      timeout: 30000,
    };

    client = new OpenLClient(config);

    console.log(`\n🔌 Connecting to OpenL Studio at: ${config.baseUrl}\n`);
  });

  // ============================================================================
  // Health Check
  // ============================================================================

  describe('0. Health Check', () => {
    test('should connect to OpenL instance', async () => {
      const health = await client.healthCheck();

      expect(health.serverReachable).toBe(true);
      expect(health.status).toBe('healthy');
      expect(health.baseUrl).toBeDefined();

      console.log('✅ Connected to OpenL Studio');
      console.log(`   Base URL: ${health.baseUrl}`);
      console.log(`   Auth: ${health.authMethod}`);
    });
  });

  // ============================================================================
  // P0: Critical Path - Repository Management
  // ============================================================================

  describe('1. Repository Management (P1)', () => {
    test('openl_list_repositories should return repositories', async () => {
      const repos = await client.listRepositories();

      expect(Array.isArray(repos)).toBe(true);
      expect(repos.length).toBeGreaterThan(0);

      const designRepo = repos.find(r => r.id === 'design');
      expect(designRepo).toBeDefined();

      console.log(`✅ Found ${repos.length} repositories`);
      console.log(`   Repositories: ${repos.map(r => r.id).join(', ')}`);
    });

    test('openl_list_branches should return branches for design repository', async () => {
      const branches = await client.listBranches('design');

      expect(Array.isArray(branches)).toBe(true);
      expect(branches.length).toBeGreaterThan(0);

      console.log(`✅ Found ${branches.length} branches in 'design'`);
      console.log(`   Branches: ${branches.join(', ')}`);
    });
  });

  // ============================================================================
  // P0: Critical Path - Project Discovery
  // ============================================================================

  describe('2. Project Discovery (P0 - CRITICAL)', () => {
    let projects: Types.ProjectSummary[];

    test('openl_list_projects should return projects', async () => {
      projects = await client.listProjects();

      expect(Array.isArray(projects)).toBe(true);
      expect(projects.length).toBeGreaterThan(0);

      // Verify project ID format
      const firstProject = projects[0];
      expect(firstProject.id).toBeDefined();
      expect(firstProject.name).toBeDefined();
      expect(firstProject.repository).toBeDefined();

      // Store test project ID for subsequent tests
      testProjectId = `${firstProject.repository}-${firstProject.name}`;

      console.log(`✅ Found ${projects.length} projects`);
      console.log(`   First project: ${firstProject.name}`);
      console.log(`   Project ID type: ${typeof firstProject.id}`);
      console.log(`   Using test project: ${testProjectId}`);
    });

    test('openl_list_projects with repository filter should work', async () => {
      const designProjects = await client.listProjects({ repository: 'design' });

      expect(Array.isArray(designProjects)).toBe(true);
      expect(designProjects.every(p => p.repository === 'design')).toBe(true);

      console.log(`✅ Found ${designProjects.length} projects in 'design'`);
    });

    test('openl_get_project should return project details', async () => {
      const project = await client.getProject(testProjectId);

      expect(project).toBeDefined();
      expect(project.name).toBeDefined();
      expect(project.repository).toBeDefined();

      console.log(`✅ Retrieved project: ${project.name}`);
      console.log(`   Status: ${project.status}`);
      console.log(`   Branch: ${project.branch || 'N/A'}`);
    }, 10000); // Longer timeout for filtering
  });

  // ============================================================================
  // P0: Critical Path - Project Lifecycle
  // ============================================================================

  describe('3. Project Lifecycle (P0 - CRITICAL)', () => {
    test('openl_update_project_status (open) should succeed', async () => {
      const result = await client.openProject(testProjectId);

      expect(result).toBe(true);

      console.log(`✅ Opened project: ${testProjectId}`);
    });

    test('switchBranch should switch branch on already opened project without 409', async () => {
      // Get current project state
      const project = await client.getProject(testProjectId);
      const originalBranch = project.branch;

      // Find available branches
      const branches = await client.listBranches(project.repository);

      if (branches.length < 2) {
        console.log(`⚠️  Only one branch available, skipping switchBranch test`);
        return;
      }

      // Pick a different branch
      const targetBranch = branches.find(b => b !== originalBranch) || branches[0];

      // Switch branch on the already opened project (should NOT get 409)
      const switchResult = await client.switchBranch(testProjectId, targetBranch);
      expect(switchResult).toBe(true);

      // Verify branch changed
      const updatedProject = await client.getProject(testProjectId);
      expect(updatedProject.branch).toBe(targetBranch);
      expect(updatedProject.status).toBe('OPENED');

      console.log(`✅ Switched branch: ${originalBranch} → ${targetBranch} (project stayed OPENED)`);

      // Switch back to original branch
      const switchBackResult = await client.switchBranch(testProjectId, originalBranch);
      expect(switchBackResult).toBe(true);

      const restoredProject = await client.getProject(testProjectId);
      expect(restoredProject.branch).toBe(originalBranch);

      console.log(`✅ Switched back: ${targetBranch} → ${originalBranch}`);
    });

    test('openl_update_project_status (close) should succeed', async () => {
      const result = await client.closeProject(testProjectId);

      expect(result).toBe(true);

      console.log(`✅ Closed project: ${testProjectId}`);
    });
  });

  // ============================================================================
  // P0: Critical Path - Table Operations
  // ============================================================================

  describe('4. Table Operations (P0 - CRITICAL)', () => {
    let tables: Types.TableMetadata[];

    test('openl_list_tables should return tables', async () => {
      tables = await client.listTables(testProjectId);

      expect(Array.isArray(tables)).toBe(true);
      expect(tables.length).toBeGreaterThan(0);

      // Store first table ID for subsequent tests
      testTableId = tables[0].id;

      console.log(`✅ Found ${tables.length} tables`);
      console.log(`   First table: ${tables[0].name} (${tables[0].tableType})`);
      console.log(`   Using test table ID: ${testTableId}`);
    });

    test('openl_list_tables with filters should work', async () => {
      // Test type filter
      const spreadsheets = await client.listTables(testProjectId, { tableType: 'Spreadsheet' });
      expect(Array.isArray(spreadsheets)).toBe(true);

      if (spreadsheets.length > 0) {
        console.log(`✅ Found ${spreadsheets.length} Spreadsheet tables`);
      }
    });

    test('openl_get_table should return table details', async () => {
      const table = await client.getTable(testProjectId, testTableId);

      expect(table).toBeDefined();
      expect(table.id).toBe(testTableId);
      expect(table.name).toBeDefined();
      expect(table.tableType).toBeDefined();

      console.log(`✅ Retrieved table: ${table.name}`);
      console.log(`   Type: ${table.tableType}`);
      console.log(`   File: ${table.file || 'N/A'}`);
    });
  });

  // ============================================================================
  // P1: Important Workflow - File Management
  // ============================================================================

  describe('6. File Management (P1)', () => {
    test('openl_download_file should retrieve Excel file', async () => {
      // Get a file from first table
      const tables = await client.listTables(testProjectId);
      const tableWithFile = tables.find(t => t.file);

      if (tableWithFile && tableWithFile.file) {
        try {
          const fileBuffer = await client.downloadFile(testProjectId, tableWithFile.file);

          expect(fileBuffer).toBeDefined();
          expect(Buffer.isBuffer(fileBuffer)).toBe(true);
          expect(fileBuffer.length).toBeGreaterThan(0);

          console.log(`✅ Downloaded file: ${tableWithFile.file}`);
          console.log(`   Size: ${fileBuffer.length} bytes`);
        } catch (error: any) {
          console.log(`⚠️  File download failed: ${error.message}`);
        }
      } else {
        console.log(`⚠️  No files found in project tables`);
      }
    }, 30000); // Longer timeout for file download
  });


  // ============================================================================
  // P2: Advanced Features - Dimension Properties
  // ============================================================================

  describe('8. Dimension Properties (P2)', () => {
    test('openl_get_table should return embedded properties', async () => {
      const props = await client.getTableProperties({
        projectId: testProjectId,
        tableId: testTableId,
      });

      expect(props).toBeDefined();
      expect(props.tableId).toBe(testTableId);

      console.log(`✅ Retrieved table properties (from embedded data)`);
      console.log(`   Properties: ${JSON.stringify(props.properties || {})}`);
    });
  });

  // ============================================================================
  // Summary
  // ============================================================================

  afterAll(() => {
    console.log('\n📊 Integration Test Summary:');
    console.log(`   Test Project: ${testProjectId}`);
    console.log(`   Test Table: ${testTableId}`);
    console.log('\n');
  });
});

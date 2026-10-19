/**
 * Test-execution tool handlers — start project tests and read their results
 * (summary, full, and by-table views).
 */

import * as schemas from "../schemas.js";
import { hasMoreItems, type Truncation } from "../formatters.js";
import type * as Types from "../types.js";
import { registerTool, formatResult, type ToolResponse } from "./common.js";
import { waitForResult, type WaitOptions } from "./wait.js";

const TEST_RESULTS_DEFAULT_TIMEOUT_MS = 120_000;

/** How the result tools wait while Studio is still running the tests. */
const TEST_RESULTS_WAIT: WaitOptions = {
  timeoutMs: TEST_RESULTS_DEFAULT_TIMEOUT_MS,
  progressMessage: "Test execution is still running…",
  timeoutMessage: `Test execution did not finish within the internal ${TEST_RESULTS_DEFAULT_TIMEOUT_MS} ms safety limit. The execution may still be running in Studio.`,
};

export function registerTestingHandlers(): void {
  registerTool({
    name: "start_project_tests",
    title: "Start Project Tests",
    description:
      "Start project test execution. For design repositories the project is automatically opened if closed; for repository 'local' the project is not opened (tests run directly). Returns execution status and metadata. Retrieve results with openl_get_test_results_summary, openl_get_test_results, or openl_get_test_results_by_table; each result tool waits internally until Studio finishes instead of requiring agent-side polling.",
    schema: schemas.startProjectTestsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const result = await client.startProjectTests(args.projectId, args);

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "get_test_results_summary",
    title: "Get Test Results Summary",
    description:
      "Get brief test execution summary without detailed test cases. Returns aggregated statistics (execution time, total tests, passed, failed) without the testCases array. Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsSummarySchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { response_format: format, projectId, ...options } = args;

      const summary = await waitForResult(
        (request) => client.getTestResultsSummary(projectId, { ...options, ...request }),
        TEST_RESULTS_WAIT,
        extra,
      );

      return formatResult(summary, format, {
        dataType: "test_results_summary",
      });
    },
  });

  registerTool({
    name: "get_test_results",
    title: "Get Full Test Results",
    description:
      "Get full test execution results with pagination support. Returns complete test execution summary including testCases array grouped by table. IMPORTANT: Pagination applies to test tables (not individual test cases). Each page returns test results aggregated by table (e.g., 'TestTable1' with 7 tests, 'TestTable2' with 8 tests). Supports filtering failures and pagination (page/offset/size). A page is cut by whole test tables to the response limit of 25,000 characters, and pagination.next_offset names the test table to continue from; a test table larger than the limit arrives as a preview. Expected and actual values of spreadsheet results make test tables large: failuresOnly reads only the failed tests, and unpaged: true reads every test table in one answer without a limit. Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { response_format: format, projectId, ...options } = args;

      const results = await waitForResult(
        (request) => client.getTestResults(projectId, { ...options, ...request }),
        TEST_RESULTS_WAIT,
        extra,
      );

      const pageSize = results.pageSize || args.size || args.limit || 50;
      const offset = args.offset ?? (results.pageNumber || 0) * pageSize;
      const hasMore = args.unpaged !== true
        && hasMoreItems(offset, results.numberOfElements, pageSize, results.total);
      return formatResult(results, format, {
        pagination: {
          limit: pageSize,
          offset,
          hasMore,
        },
        dataType: "test_results",
        // Only an unpaged read asks for everything; a page is cut to the response limit by whole test tables
        skipTruncation: args.unpaged === true,
        truncation: testTableTruncation(results, offset),
      });
    },
  });

  registerTool({
    name: "get_test_results_by_table",
    title: "Get Test Results By Table",
    description:
      "Get test execution results filtered by specific table ID. Returns filtered test execution summary with only test cases for the specified table. Supports pagination (page/offset/size) for efficient data retrieval. Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsByTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { response_format: format, projectId, ...options } = args;

      const results = await waitForResult(
        (request) => client.getTestResults(projectId, { ...options, ...request }),
        TEST_RESULTS_WAIT,
        extra,
      );

      return formatResult(results, format, {
        dataType: "test_results",
        skipTruncation: true,
      });
    },
  });
}

/** The cut of a page of test results: its leading test tables, continued from the offset of the next one. */
function testTableTruncation(results: Types.TestsExecutionSummary, offset: number): Truncation {
  return {
    items: results.testCases.length,
    unit: "test tables",
    cut: (count) => ({ ...results, testCases: results.testCases.slice(0, count), numberOfElements: count }),
    next: (count) => `continue with offset=${offset + count}`,
  };
}

/**
 * Test-execution tool handlers — start project tests and read their results
 * (summary, full, and by-table views).
 */

import * as schemas from "../schemas.js";
import { formatResponse } from "../formatters.js";
import { registerTool, waitForResult, type ToolResponse, type WaitOptions } from "./common.js";

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
    category: "Project",
    title: "Start Project Tests",
    description:
      "Start project test execution. For design repositories the project is automatically opened if closed; for repository 'local' the project is not opened (tests run directly). Returns execution status and metadata. Retrieve results with openl_get_test_results_summary, openl_get_test_results, or openl_get_test_results_by_table; each result tool waits internally until Studio finishes instead of requiring agent-side polling.",
    schema: schemas.startProjectTestsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const result = await client.startProjectTests(typedArgs.projectId, {
        tableId: typedArgs.tableId,
        testRanges: typedArgs.testRanges,
        fromModule: typedArgs.fromModule,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "get_test_results_summary",
    category: "Project",
    title: "Get Test Results Summary",
    description:
      "Get brief test execution summary without detailed test cases. Returns aggregated statistics (execution time, total tests, passed, failed) without the testCases array. Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsSummarySchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const summary = await waitForResult(
        (requestTimeoutMs) => client.getTestResultsSummary(typedArgs.projectId, {
          failuresOnly: typedArgs.failuresOnly,
          failures: typedArgs.failures,
          unpaged: typedArgs.unpaged,
          signal: extra?.signal,
          timeoutMs: requestTimeoutMs,
        }),
        TEST_RESULTS_WAIT,
        extra,
      );

      const formattedResult = formatResponse(summary, format, {
        dataType: "test_results_summary",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "get_test_results",
    category: "Project",
    title: "Get Full Test Results",
    description:
      "Get full test execution results with pagination support. Returns complete test execution summary including testCases array grouped by table. IMPORTANT: Pagination applies to test tables (not individual test cases). Each page returns test results aggregated by table (e.g., 'TestTable1' with 7 tests, 'TestTable2' with 8 tests). Supports filtering failures and pagination (page/offset/size). Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const results = await waitForResult(
        (requestTimeoutMs) => client.getTestResults(typedArgs.projectId, {
          failuresOnly: typedArgs.failuresOnly,
          failures: typedArgs.failures,
          page: typedArgs.page,
          offset: typedArgs.offset,
          size: typedArgs.size,
          limit: typedArgs.limit,
          unpaged: typedArgs.unpaged,
          signal: extra?.signal,
          timeoutMs: requestTimeoutMs,
        }),
        TEST_RESULTS_WAIT,
        extra,
      );

      const pageSize = results.pageSize || typedArgs.size || typedArgs.limit || 50;
      const offset = typedArgs.offset ?? (results.pageNumber || 0) * pageSize;
      const hasMore = typedArgs.unpaged !== true && results.numberOfElements >= pageSize;
      const formattedResult = formatResponse(results, format, {
        pagination: {
          limit: pageSize,
          offset,
          hasMore,
        },
        dataType: "test_results",
        skipTruncation: true,
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "get_test_results_by_table",
    category: "Project",
    title: "Get Test Results By Table",
    description:
      "Get test execution results filtered by specific table ID. Returns filtered test execution summary with only test cases for the specified table. Supports pagination (page/offset/size) for efficient data retrieval. Use openl_start_project_tests() first. The tool waits internally through Studio's 202 notReady responses until results are ready, the MCP call is cancelled, or the internal safety timeout is reached.",
    schema: schemas.getTestResultsByTableSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const results = await waitForResult(
        (requestTimeoutMs) => client.getTestResultsByTable(typedArgs.projectId, typedArgs.tableId, {
          failuresOnly: typedArgs.failuresOnly,
          failures: typedArgs.failures,
          page: typedArgs.page,
          offset: typedArgs.offset,
          size: typedArgs.size,
          limit: typedArgs.limit,
          unpaged: typedArgs.unpaged,
          signal: extra?.signal,
          timeoutMs: requestTimeoutMs,
        }),
        TEST_RESULTS_WAIT,
        extra,
      );

      const formattedResult = formatResponse(results, format, {
        dataType: "test_results",
        skipTruncation: true,
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
}

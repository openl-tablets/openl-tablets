/**
 * Test-execution tool handlers — start project tests and read their results
 * (summary, full, and by-table views).
 */

import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatResponse } from "../formatters.js";
import { isAxiosError, isResultNotReady } from "../utils.js";
import { registerTool, type ToolHandlerExtra, type ToolResponse } from "./common.js";
import type * as Types from "../types.js";

const TEST_RESULTS_DEFAULT_TIMEOUT_MS = 120_000;
const TEST_RESULTS_POLL_INITIAL_INTERVAL_MS = 250;
const TEST_RESULTS_POLL_MAX_INTERVAL_MS = 2_000;

function makeTestResultsAbortError(): Error {
  const error = new Error("test results wait aborted");
  error.name = "AbortError";
  return error;
}

function makeTestResultsTimeoutError(): ProtocolError {
  return new ProtocolError(
    ProtocolErrorCode.InvalidRequest,
    `Test execution did not finish within the internal ${TEST_RESULTS_DEFAULT_TIMEOUT_MS} ms safety limit. The execution may still be running in Studio.`,
  );
}

function remainingWaitTime(deadline: number): number {
  const remainingMs = deadline - Date.now();
  if (remainingMs <= 0) {
    throw makeTestResultsTimeoutError();
  }
  return remainingMs;
}

function isRequestTimeout(error: unknown): boolean {
  return isAxiosError(error) && (error.code === "ECONNABORTED" || error.code === "ETIMEDOUT");
}

function delay(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(makeTestResultsAbortError());
      return;
    }
    const onAbort = (): void => {
      clearTimeout(timer);
      reject(makeTestResultsAbortError());
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    if (typeof (timer as { unref?: () => void }).unref === "function") {
      (timer as unknown as { unref: () => void }).unref();
    }
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

async function waitForTestResults<T extends Types.TestResultsSummary | Types.TestsExecutionSummary>(
  readResults: (requestTimeoutMs: number) => Promise<T | Types.ResultNotReadyView>,
  extra?: ToolHandlerExtra,
): Promise<T> {
  const startedAt = Date.now();
  const deadline = startedAt + TEST_RESULTS_DEFAULT_TIMEOUT_MS;
  const progressToken = extra?._meta?.progressToken;
  let pollIntervalMs = TEST_RESULTS_POLL_INITIAL_INTERVAL_MS;

  for (;;) {
    if (extra?.signal.aborted) {
      throw makeTestResultsAbortError();
    }

    let result: T | Types.ResultNotReadyView;
    try {
      result = await readResults(remainingWaitTime(deadline));
    } catch (error) {
      if (extra?.signal.aborted) {
        throw makeTestResultsAbortError();
      }
      if (Date.now() >= deadline || isRequestTimeout(error)) {
        throw makeTestResultsTimeoutError();
      }
      throw error;
    }
    if (!isResultNotReady(result)) {
      return result;
    }

    if (progressToken !== undefined && extra?.sendNotification) {
      void extra.sendNotification({
        method: "notifications/progress",
        params: {
          progressToken,
          progress: Math.round((Date.now() - startedAt) / 1000),
          message: "Test execution is still running…",
        },
      }).catch(() => { /* progress reporting is best-effort */ });
    }

    const remainingMs = deadline - Date.now();
    if (remainingMs <= 0) {
      continue;
    }
    await delay(Math.min(pollIntervalMs, remainingMs), extra?.signal);
    pollIntervalMs = Math.min(pollIntervalMs * 2, TEST_RESULTS_POLL_MAX_INTERVAL_MS);
  }
}

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

      const summary = await waitForTestResults(
        (requestTimeoutMs) => client.getTestResultsSummary(typedArgs.projectId, {
          failuresOnly: typedArgs.failuresOnly,
          failures: typedArgs.failures,
          unpaged: typedArgs.unpaged,
          signal: extra?.signal,
          timeoutMs: requestTimeoutMs,
        }),
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

      const results = await waitForTestResults(
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

      const results = await waitForTestResults(
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

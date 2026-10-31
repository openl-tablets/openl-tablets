/**
 * Test-execution tool handlers — start project tests and read their results
 * (summary, full, and by-table views).
 */

import * as schemas from "../schemas.js";
import { hasMoreItems, type FormatOptions, type Truncation } from "../formatters.js";
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
    compiled: true,
    title: "Start Project Tests",
    description:
      "Start project test execution. For design repositories the project is automatically opened if closed; for repository 'local' the project is not opened (tests run directly). Answers that the tests started, and projectOpened when it opened the project. Retrieve results with openl_get_test_results, which waits until Studio finishes instead of requiring agent-side polling.",
    schema: schemas.startProjectTestsSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const result = await client.startProjectTests(args.projectId, args);

      return formatResult(result);
    },
  });

  registerTool({
    name: "get_test_results",
    title: "Get Test Results",
    description:
      "Read the results of the tests openl_start_project_tests started, waiting until Studio has run them: with " +
      "summary: true the time of the run and the counts of passed and failed tests, with tableId the tests of one " +
      "test table, with caseId as well one test with its inputs whole, otherwise every test table. A page is cut to " +
      "25,000 characters by whole test tables, or by whole tests of one test table. Expected and actual values of " +
      "spreadsheet results make test tables large: read the summary, the failures and the test tables you need " +
      "rather than unpaged: true.",
    schema: schemas.getTestResultsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { response_format: format, projectId, summary, caseId, ...options } = args;
      if (caseId !== undefined) {
        const unit = await waitForResult(
          (request) => client.getTestCase(projectId, args.tableId as string, caseId, request),
          TEST_RESULTS_WAIT,
          extra,
        );
        return formatResult(withoutUnitSchemas(unit), format);
      }
      if (summary) {
        const counts = await waitForResult(
          (request) => client.getTestResultsSummary(projectId, { ...options, ...request }),
          TEST_RESULTS_WAIT,
          extra,
        );
        return formatResult(counts, format, { dataType: "test_results_summary" });
      }

      const read = await waitForResult(
        (request) => client.getTestResults(projectId, { ...options, ...request }),
        TEST_RESULTS_WAIT,
        extra,
      );
      // Only a read of the failed tests is cut to `failures` of them
      const results = withoutValueSchemas(args.failuresOnly ? withOmittedFailures(read) : read);
      // The page Studio read is told once, by the pagination of the answer
      const { pageNumber, pageSize: studioPageSize, numberOfElements, total, ...answer } = results;
      if (args.tableId !== undefined) {
        const { page, options: pageOptions } = testUnitPage(answer, args.offset ?? 0, args.unpaged ? undefined : args.limit ?? 50);
        return formatResult(page, format, pageOptions);
      }

      const pageSize = studioPageSize || args.limit || 50;
      const offset = args.offset ?? (pageNumber || 0) * pageSize;
      const hasMore = args.unpaged !== true && hasMoreItems(offset, numberOfElements, pageSize, total);
      return formatResult(answer, format, {
        pagination: {
          limit: pageSize,
          offset,
          total,
          hasMore,
        },
        dataType: "test_results",
        // Only an unpaged read asks for everything; a page is cut to the response limit by whole test tables
        skipTruncation: args.unpaged === true,
        truncation: testTableTruncation(answer, offset),
      });
    },
  });
}

/** The results of the tests without the page of them Studio read, which the pagination of the answer tells. */
type TestResultsAnswer = Omit<Types.TestsExecutionSummary, "pageNumber" | "pageSize" | "numberOfElements" | "total">;

/**
 * The results with the number of failed tests each test table leaves out: with `failuresOnly`, Studio lists at most
 * `failures` of them per test table, and the agent must not take a cut list for every failure.
 */
function withOmittedFailures(results: Types.TestsExecutionSummary): Types.TestsExecutionSummary {
  return {
    ...results,
    testCases: results.testCases.map((testCase) => {
      const failed = (testCase.testUnits ?? []).filter((unit) => unit.status !== "TR_OK").length;
      const omitted = testCase.numberOfFailures - failed;
      return omitted > 0 ? { ...testCase, omittedFailures: omitted } : testCase;
    }),
  };
}

/**
 * The results without the JSON schema Studio writes beside every parameter of every test: the same schema of a type
 * again for each test, often most of a page, which the values themselves already show.
 */
function withoutValueSchemas(results: Types.TestsExecutionSummary): Types.TestsExecutionSummary {
  return {
    ...results,
    testCases: results.testCases.map((testCase) => ({
      ...testCase,
      testUnits: testCase.testUnits?.map(withoutUnitSchemas),
    })),
  };
}

/** A test without the JSON schema Studio writes beside each of its parameters. */
function withoutUnitSchemas(unit: Types.TestUnitExecutionResult): Types.TestUnitExecutionResult {
  const plain = (values?: Types.TraceParameterValue[]): Types.TraceParameterValue[] | undefined =>
    values?.map(({ schema, ...value }) => value);
  return {
    ...unit,
    ...(unit.parameters ? { parameters: plain(unit.parameters) } : {}),
    ...(unit.contextParameters ? { contextParameters: plain(unit.contextParameters) } : {}),
  };
}

/**
 * A page of the tests of one test table: Studio answers them all, so the server pages them, at most `limit` from
 * `offset`, every one when no limit is given, and cuts the page to the response limit by whole tests.
 */
function testUnitPage(
  answer: TestResultsAnswer,
  offset: number,
  limit: number | undefined,
): { page: TestResultsAnswer; options: FormatOptions } {
  const [table] = answer.testCases;
  const units = table?.testUnits ?? [];
  const tests = units.slice(offset, limit === undefined ? undefined : offset + limit);
  const withUnits = (count: number): TestResultsAnswer =>
    ({ ...answer, testCases: table ? [{ ...table, testUnits: tests.slice(0, count) }] : [] });
  return {
    page: withUnits(tests.length),
    options: {
      dataType: "test_results",
      skipTruncation: limit === undefined,
      truncation: {
        items: tests.length,
        unit: "tests",
        cut: withUnits,
        next: (count) => `continue with offset=${offset + count}`,
        metadata: (count) => ({ pagination: testPagination(offset, count, units.length) }),
      },
    },
  };
}

/** Where a page of the tests of one test table starts, how many tests it has, and where the next page starts. */
function testPagination(offset: number, count: number, total: number): Record<string, unknown> {
  const hasMore = offset + count < total;
  return { offset, count, total_count: total, has_more: hasMore, ...(hasMore ? { next_offset: offset + count } : {}) };
}

/** The cut of a page of test results: its leading test tables, continued from the offset of the next one. */
function testTableTruncation(answer: TestResultsAnswer, offset: number): Truncation {
  return {
    items: answer.testCases.length,
    unit: "test tables",
    cut: (count) => ({ ...answer, testCases: answer.testCases.slice(0, count) }),
    next: (count) => `continue with offset=${offset + count}`,
  };
}

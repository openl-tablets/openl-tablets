/**
 * Test-execution tool handlers — start project tests and read their results
 * (summary, full, and by-table views).
 */

import * as schemas from "../schemas.js";
import { RESPONSE_LIMITS } from "../constants.js";
import { cutJson } from "../json-cut.js";
import { countLine, fitItems, hasMoreItems, markdownTable, type Column } from "../markdown-list.js";
import { formatCount } from "../text.js";
import type * as Types from "../types.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";
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
      "Read the results of the tests openl_start_project_tests started, waiting until Studio has run them: the counts of the run, a table of the test tables with their tests and failures, and, per test table, its failed tests, each with its inputs once and its failed assertions with their expected and actual values; an input with inner structure is named by its type and key. failuresOnly, true by default, lists at most 'failures' failed tests of each test table and counts the rest; false lists every test. summary: true answers the counts alone; tableId reads the tests of one test table, page by page, and caseId with it one test with its inputs as JSON, whole up to 12,500 characters, the cuts named beyond. A page holds up to 25,000 characters, cut by whole test tables or tests; its first lines name the offset of the next page.",
    schema: schemas.getTestResultsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { projectId, summary, caseId, ...options } = args;
      if (caseId !== undefined) {
        const unit = await waitForResult(
          (request) => client.getTestCase(projectId, args.tableId as string, caseId, request),
          TEST_RESULTS_WAIT,
          extra,
        );
        return textResult(testCaseMarkdown(unit));
      }
      if (summary) {
        const counts = await waitForResult(
          // The counts are of every test, whichever the list would hold
          (request) => client.getTestResultsSummary(projectId, { ...options, failuresOnly: undefined, ...request }),
          TEST_RESULTS_WAIT,
          extra,
        );
        return textResult(countsLine(counts));
      }

      const read = await waitForResult(
        (request) => client.getTestResults(projectId, { ...options, ...request }),
        TEST_RESULTS_WAIT,
        extra,
      );
      // Only a read of the failed tests is cut to `failures` of them
      const results = args.failuresOnly ? withOmittedFailures(read) : read;
      if (args.tableId !== undefined) {
        return textResult(testsOfTable(results, args.offset ?? 0, args.unpaged ? undefined : args.limit ?? 50));
      }
      const pageSize = results.pageSize || args.limit || 50;
      const offset = args.offset ?? (results.pageNumber || 0) * pageSize;
      const hasMore = args.unpaged !== true && hasMoreItems(offset, results.numberOfElements, pageSize, results.total);
      return textResult(testTables(results, offset, hasMore, args.unpaged === true));
    },
  });
}

/** The time and the counts of a run: `159 tests: 158 passed, 1 failed, in 6.13 s.` */
function countsLine(run: { numberOfTests: number; numberOfFailures: number; executionTimeMs: number }): string {
  const passed = run.numberOfTests - run.numberOfFailures;
  return `${formatCount(run.numberOfTests, "test")}: ${passed} passed, ${run.numberOfFailures} failed, in ` +
    `${(run.executionTimeMs / 1_000).toFixed(2)} s.`;
}

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

/** The longest text of an input the table of tests shows; caseId reads the test with its inputs as JSON. */
const INPUT_TEXT = 120;

/** The inputs of a test on one line: `age=25; policy=Policy MP000001`, a long value cut. */
function inputsOf(unit: Types.TestUnitExecutionResult): string {
  return (unit.parameters ?? []).map(({ name, lazy, type, key, value }) => {
    const text = lazy ? `${type ?? "value"} ${key ?? ""}`.trim() : JSON.stringify(value) ?? "";
    const shown = text.length > INPUT_TEXT ? `${text.slice(0, INPUT_TEXT)}…` : text;
    return `${name}=${shown}`;
  }).join("; ");
}

/** The status of a test as a word. */
function statusOf(status: Types.TestUnitExecutionResult["status"]): string {
  switch (status) {
    case "TR_OK": return "passed";
    case "TR_EXCEPTION": return "error";
    default: return "failed";
  }
}

/** What an assertion compares: its description, and the field when another assertion of the test has the same. */
function assertionName(assertion: Types.TestAssertionExecutionResult): string {
  return assertion.field ? `${assertion.description} (${assertion.field})` : assertion.description ?? "";
}

/** The failures of a test: each error, and each failed assertion with its values. */
function failuresOf(unit: Types.TestUnitExecutionResult): string {
  return [
    ...(unit.errors ?? []).map((error) => `error: ${error.summary}`),
    ...(unit.testAssertions ?? []).filter((assertion) => assertion.status !== "TR_OK").map((assertion) => {
      const name = assertionName(assertion);
      return `${name ? `${name}: ` : ""}expected ${JSON.stringify(assertion.expectedValue)}, `
        + `actual ${JSON.stringify(assertion.actualValue)}`;
    }),
  ].join("; ");
}

/** The columns of the table of the tests of a test table: a test with its inputs once and its failures. */
const TEST_COLUMNS: Array<Column<Types.TestUnitExecutionResult>> = [
  { header: "test", value: (unit) => unit.id },
  { header: "status", value: (unit) => statusOf(unit.status) },
  { header: "inputs", value: inputsOf },
  { header: "failures", value: failuresOf },
];

/** The table of the tests of test tables, and the note on the failed tests a list leaves out. */
function testsSection(testCases: Types.TestCaseExecutionResult[]): string[] {
  const omitted = testCases.filter((testCase) => (testCase.omittedFailures ?? 0) > 0)
    .map((testCase) => `${testCase.name} has ${formatCount(testCase.omittedFailures ?? 0, "more failed test")}: ` +
      `read them with tableId ${testCase.tableId}.`);
  const tables = testCases.filter((testCase) => testCase.testUnits?.length)
    .map((testCase) => `test table: ${testCase.name}\n\n${markdownTable(testCase.testUnits ?? [], TEST_COLUMNS)}`);
  return [...tables, ...omitted];
}

/** A page of the test tables of a run: their counts, and the tests they list; cut by whole test tables. */
function testTables(results: Types.TestsExecutionSummary, offset: number, hasMore: boolean, unpaged: boolean): string {
  const list = { noun: "test tables", one: "test table", items: results.testCases, offset, total: results.total, hasMore };
  const render = (count: number): string => {
    const shown = results.testCases.slice(0, count);
    return [
      countsLine(results),
      countLine(list, count),
      ...(count > 0 ? [markdownTable(shown, [
        { header: "test table", value: (testCase) => testCase.name },
        { header: "tableId", value: (testCase) => testCase.tableId },
        { header: "tests", value: (testCase) => testCase.numberOfTests },
        { header: "failed", value: (testCase) => testCase.numberOfFailures },
      ])] : []),
      ...testsSection(shown),
    ].join("\n\n");
  };
  return fitItems(render, results.testCases.length, unpaged ? Infinity : undefined);
}

/** A page of the tests of one test table, at most `limit` from `offset`: cut by whole tests. */
function testsOfTable(results: Types.TestsExecutionSummary, offset: number, limit: number | undefined): string {
  const [table] = results.testCases;
  if (!table) {
    return "No test table of the run has that id.";
  }
  const units = table.testUnits ?? [];
  const counts = `${table.name} (${table.tableId}): ${formatCount(table.numberOfTests, "test")}, ${table.numberOfFailures} failed.`;
  // With only the failures asked for, a test table without them lists no test
  if (units.length === 0) {
    return counts;
  }
  const page = units.slice(offset, limit === undefined ? undefined : offset + limit);
  const list = { noun: "tests listed", one: "test listed", items: page, offset, total: units.length };
  const render = (count: number): string => [
    counts,
    countLine(list, count),
    ...testsSection([{ ...table, testUnits: page.slice(0, count) }]),
  ].join("\n\n");
  return fitItems(render, page.length, limit === undefined ? Infinity : undefined);
}

/**
 * One test with its inputs as JSON without indentation, whole unless they exceed half the response limit, and every
 * assertion of it: the inputs are cut as they are written, so the answer stays within the limit.
 */
function testCaseMarkdown(unit: Types.TestUnitExecutionResult): string {
  const inputs = Object.fromEntries((unit.parameters ?? []).map(({ name, value }) => [name, value]));
  const { value, cuts } = cutJson(inputs, RESPONSE_LIMITS.MAX_CHARACTERS / 2);
  const cutNote = cuts.length > 0 ? `, cut to fit: ${cuts.join("; ")}` : "";
  return [
    `Test ${unit.id}: ${statusOf(unit.status)}.`,
    `Inputs${cutNote}:\n\n\`\`\`json\n${JSON.stringify(value)}\n\`\`\``,
    markdownTable(unit.testAssertions ?? [], [
      { header: "assertion", value: assertionName },
      { header: "status", value: (assertion) => statusOf(assertion.status) },
      { header: "expected", value: (assertion) => JSON.stringify(assertion.expectedValue) },
      { header: "actual", value: (assertion) => JSON.stringify(assertion.actualValue) },
    ]),
    ...(unit.errors ?? []).map((error) => `Error: ${error.summary}`),
  ].join("\n\n");
}

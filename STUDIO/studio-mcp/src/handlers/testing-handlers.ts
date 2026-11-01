/**
 * Test-execution tool handlers — run project tests and read their results
 * (summary, full, and by-table views).
 */

import * as schemas from "../schemas.js";
import { RESPONSE_LIMITS } from "../constants.js";
import { cutJson } from "../json-cut.js";
import { countLine, fitItems, hasMoreItems, markdownTable, type Column } from "../markdown-list.js";
import { cutText, formatCount } from "../text.js";
import type * as Types from "../types.js";
import { registerTool, textResult, type ToolHandlerExtra, type ToolResponse } from "./common.js";
import type { OpenLClient } from "../client.js";
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
    name: "run_project_tests",
    compiled: true,
    title: "Run Project Tests",
    description:
      "Run the tests of a project, of a module, or of one table (testRanges: '1-3,5' of a test table), wait until Studio has run them, and answer their results as openl_get_test_results reads them by default: the counts of the run, the test tables with their tests and failures, and the first failed tests of each with their inputs and failed assertions. A closed project is opened first. Read more of the results with openl_get_test_results.",
    schema: schemas.runProjectTestsSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { projectOpened } = await client.startProjectTests(args.projectId, args);
      const results = await testResultsText(client, args.projectId, { failuresOnly: true }, extra);
      return textResult(projectOpened ? `Opened the project to run its tests.\n\n${results}` : results);
    },
  });

  registerTool({
    name: "get_test_results",
    title: "Get Test Results",
    description:
      "Read the results of the tests openl_run_project_tests ran, waiting while Studio still runs them: the counts of the run, a table of the test tables with their tests and failures, and, per test table, its failed tests, each with its inputs once and its failed assertions with their expected and actual values; an input with inner structure is named by its type and key. failuresOnly, true by default, lists at most 5 failed tests of each test table and counts the rest; false lists every test. summary: true answers the counts alone; tableId reads the tests of one test table, page by page, every failed one, and caseId with it one test with its inputs as JSON, whole up to 12,500 characters, the cuts named beyond. A page holds up to 25,000 characters, cut by whole test tables or tests; its first lines name the offset of the next page.",
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
          (request) => client.getTestResultsSummary(projectId, request),
          TEST_RESULTS_WAIT,
          extra,
        );
        return textResult(countsLine(counts));
      }

      return textResult(await testResultsText(client, projectId, options, extra));
    },
  });
}

/** How many failed tests of each test table a read of all of them lists; a test table with more counts the others. */
const FAILURES_LISTED = 5;

/**
 * The results of the last run of the tests as text, waiting while Studio still runs them: a page of its test tables, or
 * with `tableId` of the tests of one test table.
 */
async function testResultsText(
  client: OpenLClient,
  projectId: string,
  options: { tableId?: string; failuresOnly: boolean; offset?: number; limit?: number },
  extra: ToolHandlerExtra | undefined,
): Promise<string> {
  const read = await waitForResult(
    (request) => client.getTestResults(projectId, { ...options, failures: FAILURES_LISTED, ...request }),
    TEST_RESULTS_WAIT,
    extra,
  );
  // Only a read of the failed tests is cut to some of them
  const results = options.failuresOnly ? withOmittedFailures(read) : read;
  if (options.tableId !== undefined) {
    return testsOfTable(results, options.offset ?? 0, options.limit ?? 50);
  }
  const pageSize = results.pageSize || options.limit || 50;
  const offset = options.offset ?? (results.pageNumber || 0) * pageSize;
  return testTables(results, offset, hasMoreItems(offset, results.numberOfElements, pageSize, results.total));
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
    return `${name}=${cutText(text, INPUT_TEXT)}`;
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
  if (!assertion.field) {
    return assertion.description ?? "";
  }
  return assertion.description ? `${assertion.description} (${assertion.field})` : assertion.field;
}

/** The failures of a test: each error, and each failed assertion with its values. */
function failuresOf(unit: Types.TestUnitExecutionResult): string {
  return [
    ...(unit.errors ?? []).map((error) => `error: ${error.summary}`),
    ...(unit.testAssertions ?? []).filter((assertion) => assertion.status !== "TR_OK").map((assertion) => {
      const name = assertionName(assertion);
      const label = name ? `${name}: ` : "";
      return `${label}expected ${JSON.stringify(assertion.expectedValue)}, `
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
function testTables(results: Types.TestsExecutionSummary, offset: number, hasMore: boolean): string {
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
  return fitItems(render, results.testCases.length);
}

/** A page of the tests of one test table, at most `limit` from `offset`: cut by whole tests. */
function testsOfTable(results: Types.TestsExecutionSummary, offset: number, limit: number): string {
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
  const page = units.slice(offset, offset + limit);
  const list = { noun: "tests listed", one: "test listed", items: page, offset, total: units.length };
  const render = (count: number): string => [
    counts,
    countLine(list, count),
    ...testsSection([{ ...table, testUnits: page.slice(0, count) }]),
  ].join("\n\n");
  return fitItems(render, page.length);
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

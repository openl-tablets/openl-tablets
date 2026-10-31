/**
 * Integration tests of the test tools of src/handlers/testing-handlers.ts: a test run started, and its results read as
 * a summary, in full, or for one test table, waiting while Studio still runs the tests.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import type * as Types from "../types.js";
import { bodyOf } from "../testing/mock-http.js";
import { projectView } from "../testing/openl-api-mocks.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const projectPath = "/projects/design-project1";
const summaryPath = `${projectPath}/tests/summary`;

type TestCase = Types.TestsExecutionSummary["testCases"][number];

const testCase = (name: string, tableId: string, numberOfTests: number, numberOfFailures: number): TestCase => ({
  name, tableId, executionTimeMs: 10, numberOfTests, numberOfFailures, testUnits: [],
});

/** A page of the results of a run. */
const resultsPage = (results: Partial<Types.TestsExecutionSummary>): Types.TestsExecutionSummary => ({
  testCases: [],
  executionTimeMs: 100,
  numberOfTests: 0,
  numberOfFailures: 0,
  pageNumber: 0,
  pageSize: 50,
  numberOfElements: results.testCases?.length ?? 0,
  ...results,
});

describe("openl_start_project_tests", () => {
  it("starts the tests the arguments select", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, projectView("OPENED"));
    harness.mockHttp.onPost(`${projectPath}/tests/run`).reply(202);

    const result = await executeTool("start_project_tests", {
      projectId: "design-project1",
      tableId: "Test_calculatePremium_1234",
      testRanges: "1-3",
      module: "Rules",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params).toEqual({
      tableId: "Test_calculatePremium_1234", testRanges: "1-3", fromModule: "Rules",
    });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
    expect(dataOf(result)).toEqual({ started: true });
  });

  it("opens a closed project before starting its tests", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, projectView("CLOSED"));
    harness.mockHttp.onPatch(projectPath).reply(204);
    harness.mockHttp.onPost(`${projectPath}/tests/run`).reply(202);

    const result = await executeTool("start_project_tests", { projectId: "design-project1" }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "OPENED" }]);
    expect(dataOf(result)).toEqual({ started: true, projectOpened: true });
  });
});

describe("test results", () => {
  it("waits through 202 notReady for every test-result view", async () => {
    let resultReads = 0;
    harness.mockHttp.onGet(summaryPath).reply(() => {
      resultReads += 1;
      if (resultReads % 2 === 1) {
        return [202, { status: "notReady" }];
      }
      return [200, resultsPage({
        testCases: [testCase("Test_calculatePremium", "Test_calculatePremium_1234", 2, 0)],
        numberOfTests: 2,
      })];
    });

    for (const args of [{ summary: true }, {}, { tableId: "Test_calculatePremium_1234" }]) {
      const text = textOf(await executeTool("get_test_results", { projectId: "design-project1", ...args }, harness.client));
      expect(text).toMatch(/^(2 tests: 2 passed, 0 failed|Test_calculatePremium \(Test_calculatePremium_1234\): 2 tests, 0 failed)/);
    }
    expect(resultReads).toBe(6);
  });

  it("stops waiting when the MCP request is cancelled", async () => {
    const controller = new AbortController();
    harness.mockHttp.onGet(summaryPath).reply(() => {
      controller.abort();
      return [202, { status: "notReady" }];
    });

    await expect(executeTool("get_test_results", {
      projectId: "design-project1",
      summary: true,
    }, harness.client, { signal: controller.signal })).rejects.toThrow(/aborted/i);
  });

  it.each([
    [{ summary: true }, { failures: 5 }],
    [{ summary: true, failures: 3, unpaged: true }, { failures: 3, unpaged: true }],
    [{}, { failuresOnly: true, failures: 5, lazyValues: true }],
    [{ failuresOnly: false, unpaged: true }, { failures: 5, unpaged: true, lazyValues: true }],
  ])("openl_get_test_results asks Studio for the results %j select", async (args, params) => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({}));

    await executeTool("get_test_results", { projectId: "design-project1", ...args }, harness.client);

    expect(harness.mockHttp.history.get[0].params).toEqual(params);
  });

  it.each([
    [{ summary: true, tableId: "t1" }, /A summary takes no tableId/],
    [{ summary: true, limit: 10 }, /A summary takes no tableId, offset/],
    [{ page: 0 }, /page/],
    [{ unpaged: true, limit: 10 }, /unpaged is mutually exclusive/],
    [{ caseId: "1" }, /caseId names a test of the test table tableId names/],
  ])("openl_get_test_results refuses %j before any request", async (args, message) => {
    await expect(executeTool("get_test_results", { projectId: "design-project1", ...args }, harness.client))
      .rejects.toThrow(message);
    expect(harness.mockHttp.history.get).toHaveLength(0);
  });

  const failed = (id: string, extra: object = {}): Types.TestUnitExecutionResult => ({
    id, status: "TR_NEQ", executionTimeMs: 1,
    testAssertions: [
      { description: "Premium", expectedValue: 100, actualValue: 90, status: "TR_NEQ" },
      { description: "Tax", expectedValue: 5, actualValue: 5, status: "TR_OK" },
    ],
    ...extra,
  });

  it("answers the counts, the test tables, and a row per failed assertion with the inputs of its test", async () => {
    // Studio lists no test units for a test table without failures when only the failed tests are asked for
    const { testUnits: _, ...passing } = testCase("Test_discount", "Test_discount_1", 3, 0);
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      executionTimeMs: 1_500,
      numberOfTests: 5,
      numberOfFailures: 1,
      total: 2,
      testCases: [passing, {
        ...testCase("Test_premium", "Test_premium_1", 2, 1),
        testUnits: [failed("2", {
          parameters: [
            { name: "age", description: "Age", value: 25 },
            { name: "policy", description: "Policy", lazy: true, type: "Policy", key: "P-1" },
          ],
        })],
      }],
    }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1" }, harness.client));

    expect(text).toBe([
      "5 tests: 4 passed, 1 failed, in 1.50 s.",
      "2 test tables.",
      "| test table | tableId | tests | failed |\n| --- | --- | --- | --- |\n" +
        "| Test_discount | Test_discount_1 | 3 | 0 |\n| Test_premium | Test_premium_1 | 2 | 1 |",
      "| test table | test | status | inputs | assertion | expected | actual |\n" +
        "| --- | --- | --- | --- | --- | --- | --- |\n" +
        "| Test_premium | 2 | failed | age=25; policy=Policy P-1 | Premium | 100 | 90 |",
    ].join("\n\n"));
  });

  it("names the failed tests a test table leaves out of its list", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      testCases: [{ ...testCase("Test_premium", "Test_premium_1", 9, 7), testUnits: ["1", "2", "3", "4", "5"].map((id) => failed(id)) }],
    }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1" }, harness.client));

    expect(text).toContain("Test_premium has 2 more failed tests: read them with tableId Test_premium_1.");
  });

  it("lists a passed test as a row of its own, and the errors of a test that threw, when every test is asked for", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      testCases: [{
        ...testCase("Test_premium", "Test_premium_1", 2, 1),
        testUnits: [
          { id: "1", status: "TR_OK", executionTimeMs: 1 },
          { id: "2", status: "TR_EXCEPTION", executionTimeMs: 1, errors: [{ severity: "ERROR", summary: "Division by zero" }] },
        ],
      }],
    }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1", failuresOnly: false }, harness.client));

    expect(text).toContain("| Test_premium | 1 | passed |  |  |  |  |");
    expect(text).toContain('| Test_premium | 2 | error |  | error |  | "Division by zero" |');
  });

  it("answers the summary of the run on one line", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ executionTimeMs: 250.5, numberOfTests: 10, numberOfFailures: 2 }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1", summary: true }, harness.client));

    expect(text).toBe("10 tests: 8 passed, 2 failed, in 0.25 s.");
  });

  it.each([
    ["a partial last page, without a total", { pageNumber: 1, pageSize: 50, numberOfElements: 25 }, "75 test tables; 51–75 below."],
    ["a full last page, by the total Studio tells", { pageNumber: 1, pageSize: 50, numberOfElements: 50, total: 100 },
      "100 test tables; 51–100 below."],
    ["a full page before more", { pageNumber: 2, pageSize: 25, numberOfElements: 25 },
      "test tables; 51–75 below, the next page at offset=75."],
  ])("pages %s", async (_case, page, line) => {
    const testCases = Array.from({ length: page.numberOfElements }, (_, i) => testCase(`Test${i}`, `t${i}`, 1, 0));
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ ...page, testCases }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1",
      offset: page.pageNumber * page.pageSize,
      limit: page.pageSize,
    }, harness.client));

    expect(text.split("\n\n")[1]).toBe(line);
  });

  /** A test table of `count` failed tests, each with an assertion of `size` characters. */
  const tableOfTests = (count: number, size = 10): TestCase => ({
    ...testCase("Test_calculatePremium", "Test_calculatePremium_1234", count, count),
    testUnits: Array.from({ length: count }, (_, i) => ({
      id: String(i + 1), status: "TR_NEQ" as const, executionTimeMs: 1,
      testAssertions: [{ status: "TR_NEQ" as const, expectedValue: "E".repeat(size), actualValue: "A".repeat(size) }],
    })),
  });

  it("pages the tests of the one table tableId names", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ testCases: [tableOfTests(30)] }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1",
      tableId: "Test_calculatePremium_1234",
      offset: 20,
      limit: 5,
    }, harness.client));

    expect(text.split("\n\n").slice(0, 2)).toEqual([
      "Test_calculatePremium (Test_calculatePremium_1234): 30 tests, 30 failed.",
      "30 tests listed; 21–25 below, the next page at offset=25.",
    ]);
    expect(text).toContain("| Test_calculatePremium | 21 |");
    expect(text).not.toContain("| Test_calculatePremium | 26 |");
    // Studio answers every test of the table, its inputs named by their type and key
    expect(harness.mockHttp.history.get[0].params)
      .toEqual({ tableId: "Test_calculatePremium_1234", failuresOnly: true, failures: 5, allFailures: true, lazyValues: true });
  });

  it("answers the counts alone of one table whose tests all passed, when only failures are asked for", async () => {
    const { testUnits: _, ...passed } = testCase("Test_calculatePremium", "Test_calculatePremium_1234", 3, 0);
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ testCases: [passed] }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1", tableId: "Test_calculatePremium_1234",
    }, harness.client));

    expect(text).toBe("Test_calculatePremium (Test_calculatePremium_1234): 3 tests, 0 failed.");
  });

  it("cuts the tests of one table to the limit by whole tests", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ testCases: [tableOfTests(40, 500)] }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1",
      tableId: "Test_calculatePremium_1234",
    }, harness.client));

    const rows = text.split("\n").filter((line) => line.startsWith("| Test_calculatePremium |"));
    expect(text.length).toBeLessThanOrEqual(25_000);
    expect(rows.length).toBeGreaterThan(0);
    expect(rows.length).toBeLessThan(40);
    expect(text).toContain(`1–${rows.length} below, the next page at offset=${rows.length}.`);
  });

  it("reads one test with its inputs whole", async () => {
    const casePath = `${summaryPath}/Test_calculatePremium_1234/cases/3`;
    harness.mockHttp.onGet(casePath).reply(200, {
      ...failed("3"),
      parameters: [{
        name: "policy", description: "Policy", value: { state: "CA", note: "n".repeat(300) }, schema: { type: "object" },
      }],
    });

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1", tableId: "Test_calculatePremium_1234", caseId: "3",
    }, harness.client));

    expect(text).toContain("Test 3: failed.");
    expect(text).toContain("Inputs:");
    expect(text).toContain('"state":"CA"');
    // A long text of inputs that fit stays whole
    expect(text).toContain(`"note":"${"n".repeat(300)}"`);
    expect(text).toContain("| Tax | passed | 5 | 5 |");
  });

  it("reads one test with inputs over half the limit cut, naming the cuts", async () => {
    harness.mockHttp.onGet(`${summaryPath}/Test_calculatePremium_1234/cases/3`).reply(200, {
      ...failed("3"),
      parameters: [{ name: "drivers", value: Array.from({ length: 2_000 }, (_, i) => ({ id: i, name: `Driver ${i}` })) }],
    });

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1", tableId: "Test_calculatePremium_1234", caseId: "3",
    }, harness.client));

    expect(text).toMatch(/Inputs, cut to fit: drivers \(\d+ of 2000 items from the middle\):/);
    expect(text.length).toBeLessThanOrEqual(25_000);
  });

  it("cuts a page over the limit by whole test tables and continues it (EPBDS-16807)", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      testCases: Array.from({ length: 13 }, (_, i) => ({ ...tableOfTests(3, 1_000), name: `Test_${i}` })), total: 13,
    }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1" }, harness.client));

    expect(text.length).toBeLessThanOrEqual(25_000);
    const tables = text.split("\n").filter((line) => /^\| Test_\d+ \| Test_calculatePremium_1234 \|/.test(line)).length;
    expect(tables).toBeGreaterThan(0);
    expect(tables).toBeLessThan(13);
    expect(text).toContain(`13 test tables; 1–${tables} below, the next page at offset=${tables}.`);
  });

  it("reads every test table without a limit only when unpaged (EPBDS-16807)", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      testCases: Array.from({ length: 13 }, (_, i) => ({ ...tableOfTests(3, 1_000), name: `Test_${i}` })), total: 13,
    }));

    const text = textOf(await executeTool("get_test_results", { projectId: "design-project1", unpaged: true }, harness.client));

    expect(text.length).toBeGreaterThan(25_000);
    expect(text).toContain("13 test tables.");
  });
});

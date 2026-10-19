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
      fromModule: "Rules",
      response_format: "json",
    }, harness.client);

    expect(harness.mockHttp.history.post[0].params).toEqual({
      tableId: "Test_calculatePremium_1234", testRanges: "1-3", fromModule: "Rules",
    });
    expect(harness.mockHttp.history.patch).toHaveLength(0);
    expect(dataOf(result)).toMatchObject({ status: "started", projectWasOpened: false });
  });

  it("opens a closed project before starting its tests", async () => {
    harness.mockHttp.onGet(projectPath).reply(200, projectView("CLOSED"));
    harness.mockHttp.onPatch(projectPath).reply(204);
    harness.mockHttp.onPost(`${projectPath}/tests/run`).reply(202);

    const result = await executeTool("start_project_tests", { projectId: "design-project1" }, harness.client);

    expect(harness.mockHttp.history.patch.map(bodyOf)).toEqual([{ status: "OPENED" }]);
    expect(textOf(result)).toContain("automatically opened");
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

    const summary = await executeTool("get_test_results_summary", {
      projectId: "design-project1",
      response_format: "json",
    }, harness.client);
    const full = await executeTool("get_test_results", {
      projectId: "design-project1",
      response_format: "json",
    }, harness.client);
    const byTable = await executeTool("get_test_results_by_table", {
      projectId: "design-project1",
      tableId: "Test_calculatePremium_1234",
      response_format: "json",
    }, harness.client);

    for (const response of [summary, full, byTable]) {
      expect(dataOf(response)).toMatchObject({
        numberOfTests: 2,
        numberOfFailures: 0,
      });
    }
    expect(resultReads).toBe(6);
  });

  it("stops waiting when the MCP request is cancelled", async () => {
    const controller = new AbortController();
    harness.mockHttp.onGet(summaryPath).reply(() => {
      controller.abort();
      return [202, { status: "notReady" }];
    });

    await expect(executeTool("get_test_results_summary", {
      projectId: "design-project1",
    }, harness.client, { signal: controller.signal })).rejects.toThrow(/aborted/i);
  });

  it.each([
    ["get_test_results_summary", {}, { failures: 5 }],
    ["get_test_results_summary", { failures: 3, unpaged: true }, { failures: 3, unpaged: true }],
    ["get_test_results", { failuresOnly: true }, { failuresOnly: true, failures: 5 }],
    ["get_test_results", { unpaged: true }, { failures: 5, unpaged: true }],
  ])("%s asks Studio for the results %j select", async (tool, args, params) => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({}));

    await executeTool(tool, { projectId: "design-project1", ...args }, harness.client);

    expect(harness.mockHttp.history.get[0].params).toEqual(params);
  });

  it("openl_get_test_results_summary renders the summary of the run", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ executionTimeMs: 250.5, numberOfTests: 10, numberOfFailures: 2 }));

    const text = textOf(await executeTool("get_test_results_summary", {
      projectId: "design-project1",
      response_format: "markdown",
    }, harness.client));

    expect(text).toContain("# Test Results Summary");
    expect(text).toContain("**Passed**: 8");
  });

  it("openl_get_test_results renders the failed assertions of the run", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({
      testCases: [
        testCase("Test_calculatePremium", "Test_calculatePremium_1234", 5, 0),
        {
          ...testCase("Test_calculateDiscount", "Test_calculateDiscount_5678", 3, 1),
          testUnits: [{
            id: "discount-case-3",
            status: "TR_NEQ",
            executionTimeMs: 10,
            testAssertions: [{ description: "Discount", expectedValue: 0.2, actualValue: 0.1, status: "TR_NEQ" }],
          }],
        },
      ],
      numberOfTests: 8,
      numberOfFailures: 1,
    }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1",
      response_format: "markdown",
    }, harness.client));

    expect(text).toContain("Test_calculatePremium");
    expect(text).toContain("discount-case-3");
    expect(text).toContain("| Discount | `0.2` | `0.1` | ❌ FAILED |");
  });

  it.each([
    ["a partial last page, without a total", { pageNumber: 1, pageSize: 50, numberOfElements: 25 },
      { limit: 50, offset: 50, has_more: false }],
    ["a full last page, by the total Studio tells", { pageNumber: 1, pageSize: 50, numberOfElements: 50, total: 100 },
      { limit: 50, offset: 50, has_more: false }],
    ["a full page before more", { pageNumber: 2, pageSize: 25, numberOfElements: 25 },
      { limit: 25, offset: 50, has_more: true, next_offset: 75 }],
  ])("openl_get_test_results pages %s", async (_case, page, pagination) => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage(page));

    const result = await executeTool("get_test_results", {
      projectId: "design-project1",
      page: page.pageNumber,
      size: page.pageSize,
      response_format: "json",
    }, harness.client);

    expect(JSON.parse(textOf(result)).pagination).toEqual(pagination);
  });

  it.each([
    [{ size: 20 }, 20],
    [{ limit: 30 }, 30],
    [{}, 50],
  ])("openl_get_test_results takes the page size from %j when Studio leaves it out", async (args, limit) => {
    harness.mockHttp.onGet(summaryPath).reply(200, { ...resultsPage({}), pageSize: undefined });

    const result = await executeTool("get_test_results", { projectId: "design-project1", ...args, response_format: "json" }, harness.client);

    expect(JSON.parse(textOf(result)).pagination).toMatchObject({ limit, offset: 0 });
  });

  it("openl_get_test_results_by_table lets Studio filter and page the results of one table", async () => {
    const allResults = [
      testCase("Test_calculatePremium", "Test_calculatePremium_1234", 5, 0),
      testCase("Test_calculateDiscount", "Test_calculateDiscount_5678", 3, 1),
    ];
    // Studio answers the results of the test table the request names.
    harness.mockHttp.onGet(summaryPath).reply((config) => {
      const testCases = allResults.filter((result) => result.tableId === config.params?.tableId);
      return [200, resultsPage({ testCases })];
    });

    const text = textOf(await executeTool("get_test_results_by_table", {
      projectId: "design-project1",
      tableId: "Test_calculatePremium_1234",
      page: 1,
      size: 20,
    }, harness.client));

    expect(text).toContain("Test_calculatePremium");
    expect(text).not.toContain("Test_calculateDiscount");
    // The paging of the caller goes to Studio with the table, not over the whole run.
    expect(harness.mockHttp.history.get[0].params).toEqual({ tableId: "Test_calculatePremium_1234", failures: 5, page: 1, size: 20 });
  });

  /** 13 test tables whose expected and actual values make each of them about 5,000 characters long. */
  const largeTables = Array.from({ length: 13 }, (_, i) => ({
    ...testCase(`Test_${i}`, `Test_${i}_id`, 1, 0),
    testUnits: [{
      id: "1", description: "case", status: "TR_OK" as const, executionTimeMs: 1,
      testAssertions: [{ status: "TR_OK" as const, expectedValue: "E".repeat(2_500), actualValue: "A".repeat(2_500) }],
    }],
  }));

  it("openl_get_test_results cuts a page over the limit by whole test tables and continues it (EPBDS-16807)", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ testCases: largeTables, numberOfTests: 13, total: 13 }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1", response_format: "json",
    }, harness.client));

    expect(text.length).toBeLessThanOrEqual(25_000);
    const response = JSON.parse(text);
    const delivered = response.data.testCases.length;
    expect(delivered).toBeGreaterThan(0);
    expect(delivered).toBeLessThan(13);
    expect(response.data.numberOfElements).toBe(delivered);
    expect(response.pagination).toMatchObject({ offset: 0, has_more: true, next_offset: delivered });
    expect(response.truncation_message).toBe(
      `Response truncated due to size. It holds ${delivered} of 13 test tables; continue with offset=${delivered}.`);
  });

  it("openl_get_test_results reads every test table without a limit only when unpaged (EPBDS-16807)", async () => {
    harness.mockHttp.onGet(summaryPath).reply(200, resultsPage({ testCases: largeTables, numberOfTests: 13, total: 13 }));

    const text = textOf(await executeTool("get_test_results", {
      projectId: "design-project1", unpaged: true, response_format: "json",
    }, harness.client));

    expect(text.length).toBeGreaterThan(25_000);
    expect(JSON.parse(text).data.testCases).toHaveLength(13);
  });
});

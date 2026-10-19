/**
 * The setup of a suite that calls the tools through `executeTool`: the tools registered once, and for every test a new
 * `OpenLClient` whose HTTP transport a {@link MockHttp} answers.
 *
 * A new client per test keeps what a client holds, such as its list of repositories, from leaking into the next test.
 * A suite reads the client and the mock of the current test from {@link harness}.
 */
import { beforeAll, beforeEach } from "vitest";

import { OpenLClient } from "../client.js";
import { registerAllTools } from "../handlers/index.js";
import type { ToolResponse } from "../handlers/common.js";
import { MockHttp } from "./mock-http.js";

/** The client of the current test, and the mock answering its requests. */
export const harness = {} as { client: OpenLClient; mockHttp: MockHttp };

/** Registers the tools for the suite and gives every test a new client and mock. */
export function useToolHarness(): void {
  beforeAll(() => registerAllTools());
  beforeEach(() => {
    harness.client = new OpenLClient({ baseUrl: "http://localhost:8080", authorization: "Token openl_pat_test" });
    harness.mockHttp = new MockHttp(harness.client);
  });
}

/** The text a tool answered. */
export function textOf(result: ToolResponse): string {
  return result.content[0].text;
}

/** The `data` of a tool answer in the JSON format. */
export function dataOf<T = any>(result: ToolResponse): T {
  return (JSON.parse(textOf(result)) as { data: T }).data;
}

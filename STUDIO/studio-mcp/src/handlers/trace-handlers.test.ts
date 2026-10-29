/**
 * Integration tests of the trace tools of src/handlers/trace-handlers.ts, the glue over the debug API of OpenL Studio:
 * the breakpoints set before a start, the status polled on resume, the fields trimmed and the highlights merged on an
 * inspection, the series of watched cells, and the actionable messages of a 404 or a 409.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { bodyOf, queryOf } from "../testing/mock-http.js";
import { suspendedStack } from "../testing/openl-api-mocks.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const projectId = "design:insurance-rules:hash123";
const projectPath = `/projects/${encodeURIComponent(projectId)}`;

describe("trace tools", () => {
  it.each([
    ["expand_trace_tree", 404, "get", "/trace/tree/children", { uri: "u", instance: 0, step: "R1C0" }, /openl_start_trace|profiling/],
    ["expand_trace_tree", 409, "get", "/trace/tree/children", { uri: "u", instance: 0, step: "R1C0" }, /still in progress|openl_resume_trace/],
    ["step_trace", 409, "post", "/trace/step", { type: "into" }, /not suspended/i],
    ["step_trace", 404, "post", "/trace/step", { type: "into" }, /openl_start_trace/],
    ["inspect_trace_frame", 409, "get", "/trace/frames/3/variables", { frameIndex: 3 }, /not suspended/i],
    ["get_trace_value", 404, "get", "/trace/parameters/9", { parameterId: 9 }, /openl_start_trace/],
  ] as const)("openl_%s maps a %s answer to an actionable message", async (tool, status, method, path, args, message) => {
    (method === "get" ? harness.mockHttp.onGet(`${projectPath}${path}`) : harness.mockHttp.onPost(`${projectPath}${path}`))
      .reply(status, { message: "trace.execution.task.message" });

    await expect(executeTool(tool, { projectId, ...args }, harness.client)).rejects.toThrow(message);
  });

  it("openl_start_trace replaces the breakpoint set BEFORE starting when breakpoints are given", async () => {
    const calls: string[] = [];
    harness.mockHttp.onPut(`${projectPath}/trace/breakpoints`).reply((config) => {
      calls.push(`put:${config.data}`);
      return [204];
    });
    harness.mockHttp.onPost(`${projectPath}/trace`).reply(() => {
      calls.push("start");
      return [200, suspendedStack];
    });

    const result = await executeTool(
      "start_trace",
      { projectId, tableId: "calc_42", breakpoints: ["MyDT#rule"] },
      harness.client
    );

    expect(calls[0]).toBe('put:{"uris":["MyDT#rule"]}');
    expect(calls[1]).toBe("start");
    expect(textOf(result)).toContain("suspended");
  });

  it("openl_start_trace profiling asks the backend for the bounded profile (includeTree=false) and returns it", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace`).reply(200, {
      status: "completed",
      frames: [],
      profile: {
        hotspots: [{ uri: "u", name: "VehiclePriceFactor", kind: "spreadsheet", selfMillis: 83.4, totalMillis: 120.1, count: 6 }],
        distinctTables: 42,
        nodeCount: 3571,
        totalMillis: 128.4,
        truncated: true,
      },
    });

    const result = await executeTool(
      "start_trace",
      { projectId, tableId: "calc_42", stopAtEntry: false, profiling: true, profileTop: 30 },
      harness.client
    );

    expect(queryOf(harness.mockHttp.history.post[0]).includeTree).toBe("false"); // never pull the >1MB tree by default
    expect(queryOf(harness.mockHttp.history.post[0]).profileTop).toBe("30");
    expect(textOf(result)).toContain("VehiclePriceFactor");
    expect(textOf(result)).toContain("hotspots");
  });

  it("openl_start_trace with includeTree: true asks the backend for the full tree", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace`).reply(200, { status: "completed", frames: [], tree: { uri: "u", name: "root", kind: "spreadsheet", durationMillis: 5, selfMillis: 1, steps: [] } });

    await executeTool(
      "start_trace",
      { projectId, tableId: "calc_42", stopAtEntry: false, profiling: true, includeTree: true },
      harness.client
    );

    expect(queryOf(harness.mockHttp.history.post[0]).includeTree).toBe("true");
  });

  it("openl_expand_trace_tree loads one level of the lazy tree and flags the next page", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/tree/children`).reply(200, {
      children: [
        { uri: "u1", name: "AgeFactor", kind: "decisionTable", instance: 0, durationMillis: 1, selfMillis: 1, steps: [{ ref: "R1C0", status: "executed", childrenTotal: 0 }] },
        { uri: "u2", name: "AgeFactor", kind: "decisionTable", instance: 1, durationMillis: 1, selfMillis: 1, steps: [{ ref: "R1C0", status: "executed", childrenTotal: 2 }], notRetained: 7 },
      ],
      total: 4,
    });

    const result = await executeTool(
      "expand_trace_tree",
      { projectId, uri: "P/Rules.xlsx?sheet=Main&range=B2:D8", instance: 0, step: "R2C0" },
      harness.client
    );

    // uri/instance/step forwarded (URI kept intact for the query string to percent-encode).
    expect(queryOf(harness.mockHttp.history.get[0])).toMatchObject({ uri: "P/Rules.xlsx?sheet=Main&range=B2:D8", instance: "0", step: "R2C0" });
    const parsed = dataOf(result);
    expect(parsed.total).toBe(4);
    expect(parsed.children).toHaveLength(2);
    // total 4 > offset 0 + 2 returned → more pages, with the next offset computed.
    expect(parsed.hasMore).toBe(true);
    expect(parsed.nextOffset).toBe(2);
    expect(parsed.children[1].notRetained).toBe(7);
  });

  it("openl_expand_trace_tree does not flag hasMore when the page is the last", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/tree/children`).reply(200, {
      children: [{ uri: "u1", name: "AgeFactor", kind: "decisionTable", instance: 2, durationMillis: 1, selfMillis: 1, steps: [] }],
      total: 3,
    });

    const result = await executeTool(
      "expand_trace_tree",
      { projectId, uri: "u", instance: 0, step: "R1C0", offset: 2 },
      harness.client
    );

    const parsed = dataOf(result);
    expect(parsed.hasMore).toBe(false);
    expect(parsed.nextOffset).toBeUndefined();
  });

  it("openl_expand_trace_tree normalizes a childless step (backend returns { total: 0 }, no children) to an empty list", async () => {
    // A step that made no sub-calls answers 200 with just { total: 0 } — the
    // backend omits the children array — so the tool fills in children: [].
    harness.mockHttp.onGet(`${projectPath}/trace/tree/children`).reply(200, { total: 0 });

    const result = await executeTool(
      "expand_trace_tree",
      { projectId, uri: "u", instance: 0, step: "R9C9" },
      harness.client
    );

    const parsed = dataOf(result);
    expect(parsed.children).toEqual([]);
    expect(parsed.total).toBe(0);
    expect(parsed.hasMore).toBe(false);
  });

  it("openl_expand_trace_tree stops paging when a page comes back empty even though total still exceeds offset (dropped sub-calls)", async () => {
    // A step whose sub-calls were dropped at the retained-tree size cap: total
    // counts them but they can never be paged to, so an offset past the retained
    // children returns []. hasMore must be false — otherwise nextOffset freezes
    // and an agent following it re-requests the same page forever.
    harness.mockHttp.onGet(`${projectPath}/trace/tree/children`).reply(200, { children: [], total: 500 });

    const result = await executeTool(
      "expand_trace_tree",
      { projectId, uri: "u", instance: 0, step: "R1C0", offset: 100 },
      harness.client
    );

    const parsed = dataOf(result);
    expect(parsed.hasMore).toBe(false);
    expect(parsed.nextOffset).toBeUndefined();
    expect(parsed.total).toBe(500);
  });

  it("openl_step_trace steps compact by default and returns the new stack", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/step`).reply(200, suspendedStack);

    const result = await executeTool(
      "step_trace",
      { projectId, type: "over" },
      harness.client
    );
    expect(queryOf(harness.mockHttp.history.post[0])).toEqual({ type: "over", view: "compact", includeTree: "false" });
    expect(textOf(result)).toContain("CalcRule");
  });

  it("openl_step_trace with withValues bundles the active frame's variables", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/step`).reply(200, suspendedStack);
    harness.mockHttp.onGet(`${projectPath}/trace/frames/0/variables`).reply(200, {
      parameters: [{ name: "policy", description: "AutoPolicy", value: { age: 30 } }],
      steps: [],
      errors: [],
    });

    const result = await executeTool(
      "step_trace",
      { projectId, type: "out", withValues: true },
      harness.client
    );

    const data = dataOf<{ variables?: { parameters: Array<{ name: string }> } }>(result);
    expect(data.variables?.parameters[0].name).toBe("policy");
  });

  it("openl_resume_trace resumes, polls the status until it leaves running, then returns the stack", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/resume`).reply(202);
    const statuses = ["running", "running", "suspended"];
    harness.mockHttp.onGet(`${projectPath}/trace/status`).reply(() => [200, { status: statuses.shift() ?? "suspended" }]);
    harness.mockHttp.onGet(`${projectPath}/trace/stack`).reply(200, suspendedStack);

    const result = await executeTool(
      "resume_trace",
      { projectId },
      harness.client
    );

    expect(statuses).toHaveLength(0);
    expect(textOf(result)).toContain("suspended");
  });

  it("openl_resume_trace re-attaches to a still-running session instead of failing on the resume 409", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/resume`).reply(409, { message: "trace.execution.not.suspended.message" });
    const statuses = ["running", "completed"];
    harness.mockHttp.onGet(`${projectPath}/trace/status`).reply(() => [200, { status: statuses.shift() ?? "completed" }]);
    harness.mockHttp.onGet(`${projectPath}/trace/stack`).reply(200, { status: "completed", frames: [] });

    const result = await executeTool(
      "resume_trace",
      { projectId },
      harness.client
    );

    expect(textOf(result)).toContain("completed");
  });

  it("openl_resume_trace rejects the resume 409 when the session is terminal (not merely running)", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/resume`).reply(409, { message: "trace.execution.not.suspended.message" });
    harness.mockHttp.onGet(`${projectPath}/trace/status`).reply(200, { status: "completed" });

    await expect(
      executeTool("resume_trace", { projectId }, harness.client)
    ).rejects.toThrow(/not suspended/i);
  });

  it("openl_resume_trace reports a still-running trace on timeout as a structured status, not erroring", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/resume`).reply(202);
    harness.mockHttp.onGet(`${projectPath}/trace/status`).reply(200, { status: "running" });

    const result = await executeTool(
      "resume_trace",
      { projectId, timeoutMs: 1 },
      harness.client
    );

    const data = dataOf<{ status?: string; message?: string }>(result);
    // The actual status is carried through (not lost), and JSON consumers get a real object.
    expect(data.status).toBe("running");
    expect(data.message).toMatch(/still running/);
    expect(data.message).toContain("openl_stop_trace");
  });

  it("openl_resume_trace maps a session reaped mid-poll (404) to the actionable no-session message", async () => {
    harness.mockHttp.onPost(`${projectPath}/trace/resume`).reply(202);
    // First poll shows running; the session is then reaped and status 404s.
    harness.mockHttp
      .onGet(`${projectPath}/trace/status`).replyOnce(200, { status: "running" })
      .onGet(`${projectPath}/trace/status`).reply(404, { message: "trace.execution.task.message" });

    await expect(
      executeTool("resume_trace", { projectId, timeoutMs: 5000 }, harness.client)
    ).rejects.toThrow(/openl_start_trace/);
  });

  it("openl_inspect_trace_frame trims the response via ?fields by default and lifts the trim with full: true", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/frames/0/variables`).reply(200, { parameters: [], steps: [], errors: [], decision: { firedRules: ["R10"], conditions: [] } });

    const result = await executeTool(
      "inspect_trace_frame",
      { projectId, frameIndex: 0 },
      harness.client
    );
    expect(textOf(result)).toContain("R10");
    expect(harness.mockHttp.history.get[0].params?.fields).toContain("decision");
    expect(harness.mockHttp.history.get[0].params?.fields).toContain("parameters(name,description,lazy,parameterId,value)");
    expect(harness.mockHttp.history.get[0].params?.fields).not.toContain("schema");

    await executeTool("inspect_trace_frame", { projectId, frameIndex: 0, full: true }, harness.client);
    expect(harness.mockHttp.history.get[1].params).toEqual({ includeSchema: true });
  });

  it("openl_inspect_trace_frame filters steps by onlyExecutedSteps and excludeStepValues, resolving lazy values", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/frames/0/variables`).reply(200, {
      parameters: [],
      errors: [],
      steps: [
        { ref: "R0C0", label: "$AgeFactor", status: "executed", value: { name: "$AgeFactor", value: 1 } },
        // A neutral factor delivered lazily — must still be dropped by [1].
        { ref: "R3C0", label: "$BaseRate", status: "executed", value: { name: "$BaseRate", lazy: true, parameterId: 7 } },
        { ref: "R1C0", label: "$VehiclePriceFactor", status: "executed", value: { name: "$VehiclePriceFactor", value: 83.372 } },
        { ref: "R2C0", label: "$Pending", status: "pending" },
      ],
    });
    // The lazy $BaseRate resolves to 1 via the parameter endpoint.
    harness.mockHttp.onGet(`${projectPath}/trace/parameters/7`).reply(200, { name: "$BaseRate", value: 1 });

    const result = await executeTool(
      "inspect_trace_frame",
      { projectId, frameIndex: 0, onlyExecutedSteps: true, excludeStepValues: [1] },
      harness.client
    );

    const data = dataOf<{ steps: Array<{ label: string }> }>(result);
    // The neutral factors (inline 1 and lazy→1) and the pending step are gone; the outlier stays.
    expect(data.steps.map((s) => s.label)).toEqual(["$VehiclePriceFactor"]);
  });

  it("openl_inspect_trace_frame with withHighlights merges the overlay and the raw grid of the frame's table", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/frames/0/variables`).reply(200, { parameters: [], steps: [], errors: [] });
    harness.mockHttp.onGet(`${projectPath}/trace/frames/0/highlights`).reply(200, [{ cell: "C5", state: "current" }]);
    harness.mockHttp.onGet(`${projectPath}/trace/stack`).reply(200, suspendedStack);
    harness.mockHttp.onGet(`${projectPath}/tables/calc_42`).reply(200, { view: "raw", cells: [["Header"], ["=A1*2"]] });

    const result = await executeTool(
      "inspect_trace_frame",
      { projectId, frameIndex: 0, withHighlights: true },
      harness.client
    );

    expect(harness.mockHttp.history.get.find((request) => request.url.endsWith("/tables/calc_42"))?.params?.raw).toBe(true);
    const text = textOf(result);
    expect(text).toContain('"C5"');
    expect(text).toContain("=A1*2");
  });

  it("openl_set_trace_breakpoints reads the current set and the available targets", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/breakpoints`).reply(200, ["CalcRule"]);
    harness.mockHttp.onGet(`${projectPath}/trace/breakpoint-tables`).reply(200, [
      { name: "CalcRule", kind: "spreadsheet" },
      { name: "BankRatingGroup", kind: "decisionTable" },
    ]);

    const result = await executeTool(
      "set_trace_breakpoints",
      { projectId },
      harness.client
    );

    const text = textOf(result);
    expect(text).toContain("BankRatingGroup");
    expect(harness.mockHttp.history.put).toHaveLength(0);
  });

  it("openl_set_trace_breakpoints with breakpoints replaces the whole set before reading it back", async () => {
    harness.mockHttp.onPut(`${projectPath}/trace/breakpoints`).reply(204);
    harness.mockHttp.onGet(`${projectPath}/trace/breakpoints`).reply(200, ["uri#R0C1"]);
    harness.mockHttp.onGet(`${projectPath}/trace/breakpoint-tables`).reply(200, []);

    const result = await executeTool(
      "set_trace_breakpoints",
      { projectId, breakpoints: ["uri#R0C1"] },
      harness.client
    );

    expect(bodyOf(harness.mockHttp.history.put[0])).toEqual({ uris: ["uri#R0C1"] });
    expect(textOf(result)).toContain("uri#R0C1");
  });

  it("openl_get_trace_value expands a lazy parameter, dropping the value's JSON Schema unless withSchema is set", async () => {
    harness.mockHttp.onGet(`${projectPath}/trace/parameters/5`).reply(200, { name: "premium", description: "Double", value: 1000 });

    const result = await executeTool(
      "get_trace_value",
      { projectId, parameterId: 5 },
      harness.client
    );
    expect(textOf(result)).toContain("1000");
    // The default ?fields projection excludes the token-heavy `schema`.
    expect(harness.mockHttp.history.get[0].params?.fields).toBe("name,description,value");

    await executeTool("get_trace_value", { projectId, parameterId: 5, withSchema: true }, harness.client);
    expect(harness.mockHttp.history.get[1].params).toEqual({ includeSchema: true });
  });

  it("openl_stop_trace terminates the session", async () => {
    harness.mockHttp.onDelete(`${projectPath}/trace`).reply(204);

    const result = await executeTool("stop_trace", { projectId }, harness.client);
    expect(textOf(result)).toContain("terminated");
    expect(harness.mockHttp.history.delete).toHaveLength(1);
  });

  it("openl_watch_trace_cells sets the watches, runs the table, and returns the collected series", async () => {
    const calls: string[] = [];
    // Watch clears breakpoints first so the run reaches completion.
    harness.mockHttp.onPut(`${projectPath}/trace/breakpoints`).reply(204);
    harness.mockHttp.onPut(`${projectPath}/trace/watches`).reply(() => {
      calls.push("put");
      return [204];
    });
    harness.mockHttp.onPost(`${projectPath}/trace`).reply(() => {
      calls.push("run");
      return [200, { status: "completed", frames: [] }];
    });
    harness.mockHttp.onGet(`${projectPath}/trace/watch`).reply(() => {
      calls.push("watch");
      return [200, {
        series: [{
          name: "$VehiclePriceFactor",
          table: "VehiclePremiumCalculation",
          points: [
            { instance: 0, value: { name: "$VehiclePriceFactor", description: "Double", value: 1.0 } },
            { instance: 1, ref: "R7C1", value: { name: "$VehiclePriceFactor", description: "Double", value: 83.372 } },
          ],
        }],
      }];
    });

    const result = await executeTool(
      "watch_trace_cells",
      { projectId, tableId: "calc_42", cells: ["$VehiclePriceFactor"], testRanges: "1" },
      harness.client
    );

    expect(calls).toEqual(["put", "run", "watch"]); // set → run → read
    // The breakpoints are cleared before the watches are set.
    expect(harness.mockHttp.history.put.map(bodyOf)).toEqual([{ uris: [] }, { cells: ["$VehiclePriceFactor"] }]);
    const startParams = queryOf(harness.mockHttp.history.post[0]);
    const watchFields = harness.mockHttp.history.get[0].params?.fields;
    expect(startParams.stopAtEntry).toBe("false");
    // The run to completion materializes lazy branches on its own — no
    // profiling (which would make the studio build a tree this tool discards).
    expect(startParams.profiling).toBeUndefined();
    expect(startParams.includeTree).toBeUndefined();
    // The point value is a ParameterValue; its JSON Schema is trimmed by default.
    expect(watchFields).toContain("value(name,description,lazy,parameterId,value)");
    expect(watchFields).not.toContain("schema");
    expect(textOf(result)).toContain("83.372");
  });

  it("openl_watch_trace_cells surfaces the server's truncation (total + a note) when the series is capped", async () => {
    harness.mockHttp.onPut(`${projectPath}/trace/breakpoints`).reply(204);
    harness.mockHttp.onPut(`${projectPath}/trace/watches`).reply(204);
    harness.mockHttp.onPost(`${projectPath}/trace`).reply(200, { status: "completed", frames: [] });
    // The server caps points per series and reports the full execution count via `total`.
    const points = Array.from({ length: 200 }, (_, i) => ({ instance: i, value: { name: "$ClaimCost", value: i } }));
    harness.mockHttp.onGet(`${projectPath}/trace/watch`).reply(200, {
      truncated: true,
      series: [{ name: "$ClaimCost", table: "ClaimCostPerBenefitPerAgeBand", total: 11200, points }],
    });

    const result = await executeTool(
      "watch_trace_cells",
      { projectId, tableId: "calc_42", cells: ["$ClaimCost"] },
      harness.client
    );

    const body = JSON.parse(textOf(result)) as {
      data: { truncated: boolean; series: Array<{ total: number; points: unknown[] }> };
      truncated?: boolean;
    };
    // Over the limit when indented, the answer fits without indentation: every point and Studio's total arrive
    expect(body.truncated).toBeUndefined();
    expect(body.data.truncated).toBe(true);
    expect(body.data.series[0].total).toBe(11200);
    expect(body.data.series[0].points).toHaveLength(200);
  });
});

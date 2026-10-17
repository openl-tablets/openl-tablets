/**
 * Unit tests for the waitForCompilation orchestration in stomp-waits.ts.
 *
 * The STOMP subscription is injected via the `subscribeImpl` test seam, so
 * these tests run entirely in-memory — no network, no axios mocks.
 */

import { describe, it, expect, beforeEach, jest } from "@jest/globals";

import { waitForCompilation, isResolvedCompileState } from "../src/stomp-waits.js";
import type { OpenLClient } from "../src/client.js";
import type { SubscribeProjectStatusOpts, Subscription } from "../src/stomp-client.js";
import type * as Types from "../src/types.js";

function makeStatus(compileState: Types.CompileState, overrides: Partial<Types.ProjectStatusView> = {}): Types.ProjectStatusView {
  return {
    projectId: "test:proj",
    branch: "main",
    compileState,
    compilation: {
      messages: { items: [], total: 0, errors: 0, warnings: 0 },
      modules: { total: 2, compiled: compileState === "ok" ? 2 : 0 },
      tests: { total: 0 },
    },
    ...overrides,
  };
}

interface FakeStomp {
  subscribe: jest.Mock<(opts: SubscribeProjectStatusOpts) => Promise<Subscription>>;
  emit(status: Types.ProjectStatusView): void;
  closeSpy: jest.Mock<() => Promise<void>>;
}

function makeFakeStomp(): FakeStomp {
  let onMessage: ((s: Types.ProjectStatusView) => void) | null = null;
  const closeSpy = jest.fn<() => Promise<void>>().mockResolvedValue(undefined);
  const subscribe = jest.fn(async (opts: SubscribeProjectStatusOpts) => {
    onMessage = opts.onMessage;
    return { close: closeSpy } as Subscription;
  });
  return {
    subscribe: subscribe as unknown as FakeStomp["subscribe"],
    emit(status: Types.ProjectStatusView) {
      if (!onMessage) throw new Error("emit called before subscribe");
      onMessage(status);
    },
    closeSpy,
  };
}

interface MockClient {
  client: OpenLClient;
  getProjectStatus: jest.Mock;
  triggerProjectCompilation: jest.Mock;
  getProject: jest.Mock;
  switchBranch: jest.Mock;
  openProject: jest.Mock;
}

function makeClient(
  scriptedFetches: Types.ProjectStatusView[],
  projectOverrides: Partial<{ status: string; branch: string }> = {},
): MockClient {
  let i = 0;
  const getProjectStatus = jest.fn(async (_projectId: string, _branch?: string) => {
    if (i >= scriptedFetches.length) {
      throw new Error(`getProjectStatus called more than ${scriptedFetches.length} times`);
    }
    return scriptedFetches[i++];
  });
  const getProject = jest.fn(async () => ({
    id: "p1",
    name: "Test",
    status: projectOverrides.status ?? "OPENED",
    branch: projectOverrides.branch ?? "main",
  } as unknown as Types.ComprehensiveProject));
  const switchBranch = jest.fn(async () => true);
  const openProject = jest.fn(async () => true);
  const triggerProjectCompilation = jest.fn(async () => undefined);
  const client = {
    getProjectStatus,
    triggerProjectCompilation,
    getProject,
    switchBranch,
    openProject,
    getBaseUrl: () => "http://localhost:8080/rest",
    getSessionCookie: () => "abc123",
    getAuthorizationHeader: () => "Token openl_pat_test",
  } as unknown as OpenLClient;
  return { client, getProjectStatus, triggerProjectCompilation, getProject, switchBranch, openProject };
}

describe("isResolvedCompileState", () => {
  it("treats ok/warnings/errors as resolved (terminal outcomes)", () => {
    expect(isResolvedCompileState("ok")).toBe(true);
    expect(isResolvedCompileState("warnings")).toBe(true);
    expect(isResolvedCompileState("errors")).toBe(true);
  });
  it("treats idle as resolved — no compile registered, nothing to wait for", () => {
    expect(isResolvedCompileState("idle")).toBe(true);
  });
  it("treats compiling as the only state that warrants waiting", () => {
    expect(isResolvedCompileState("compiling")).toBe(false);
  });
});

describe("waitForCompilation", () => {
  let stomp: FakeStomp;

  beforeEach(() => {
    stomp = makeFakeStomp();
  });

  it("returns the initial status when it is already terminal — no STOMP subscription", async () => {
    const { client } = makeClient([makeStatus("ok")]);
    const result = await waitForCompilation(client, "p1", "main", {}, stomp.subscribe);
    expect(result.compileState).toBe("ok");
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("returns immediately when initial state is idle — no STOMP subscription, no hang", async () => {
    // Regression: previously `idle` was treated as non-terminal, so the wait
    // flow subscribed to STOMP and blocked until the 2-minute timeout because
    // no compile event ever fires for an idle project.
    const { client } = makeClient([makeStatus("idle")]);
    const result = await waitForCompilation(client, "p1", "main", {}, stomp.subscribe);
    expect(result.compileState).toBe("idle");
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("starts lazy compilation on idle when requested and returns its terminal status", async () => {
    const { client, triggerProjectCompilation } = makeClient([
      makeStatus("idle"),
      makeStatus("ok"),
    ]);

    const result = await waitForCompilation(
      client,
      "p1",
      "main",
      { compileOnIdle: true },
      stomp.subscribe,
    );

    expect(triggerProjectCompilation).toHaveBeenCalledWith("p1", {
      signal: undefined,
      timeoutMs: expect.any(Number),
    });
    const triggerTimeout = triggerProjectCompilation.mock.calls[0][1].timeoutMs as number;
    expect(triggerTimeout).toBeGreaterThan(0);
    expect(triggerTimeout).toBeLessThanOrEqual(120_000);
    expect(result.compileState).toBe("ok");
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("switches to the requested branch before starting lazy compilation", async () => {
    const { client, switchBranch, triggerProjectCompilation } = makeClient([
      makeStatus("idle", { branch: "main" }),
      makeStatus("idle", { branch: "develop" }),
      makeStatus("warnings", { branch: "develop" }),
    ]);

    const result = await waitForCompilation(
      client,
      "p1",
      "develop",
      { compileOnIdle: true },
      stomp.subscribe,
    );

    expect(switchBranch).toHaveBeenCalledWith("p1", "develop");
    expect(triggerProjectCompilation).toHaveBeenCalledTimes(1);
    expect(result).toMatchObject({ compileState: "warnings", branch: "develop" });
  });

  it("subtracts pre-trigger work from the compilation timeout budget", async () => {
    const { client, getProjectStatus, triggerProjectCompilation } = makeClient([
      makeStatus("ok"),
    ]);
    getProjectStatus.mockImplementationOnce(async () => {
      await new Promise((resolve) => setTimeout(resolve, 50));
      return makeStatus("idle");
    });

    const result = await waitForCompilation(
      client,
      "p1",
      "main",
      { compileOnIdle: true, timeoutMs: 1_000 },
      stomp.subscribe,
    );

    const triggerTimeout = triggerProjectCompilation.mock.calls[0][1].timeoutMs as number;
    expect(triggerTimeout).toBeGreaterThan(0);
    expect(triggerTimeout).toBeLessThan(1_000);
    expect(result.compileState).toBe("ok");
  });

  it("returns the latest snapshot when the lazy compilation request times out", async () => {
    const { client, triggerProjectCompilation } = makeClient([
      makeStatus("idle"),
      makeStatus("compiling"),
    ]);
    triggerProjectCompilation.mockRejectedValueOnce(Object.assign(new Error("timeout"), {
      isAxiosError: true,
      code: "ECONNABORTED",
    }));

    const result = await waitForCompilation(
      client,
      "p1",
      "main",
      { compileOnIdle: true, timeoutMs: 50 },
      stomp.subscribe,
    );

    expect(result.compileState).toBe("compiling");
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("returns the post-subscribe status when the race-close fetch catches a terminal flip", async () => {
    const { client } = makeClient([makeStatus("compiling"), makeStatus("errors")]);
    const result = await waitForCompilation(client, "p1", "main", {}, stomp.subscribe);
    expect(result.compileState).toBe("errors");
    expect(stomp.subscribe).toHaveBeenCalledTimes(1);
    expect(stomp.closeSpy).toHaveBeenCalledTimes(1);
  });

  it("waits for a terminal STOMP message and resolves with it", async () => {
    const { client } = makeClient([makeStatus("compiling"), makeStatus("compiling")]);
    const waitPromise = waitForCompilation(client, "p1", "main", {}, stomp.subscribe);

    // Give the orchestration a tick to subscribe + do the second fetch.
    await new Promise((r) => setImmediate(r));
    // Emit one progress frame, then a terminal frame.
    stomp.emit(makeStatus("compiling", { compilation: { messages: { items: [], total: 0, errors: 0, warnings: 0 }, modules: { total: 2, compiled: 1 }, tests: { total: 0 } } }));
    stomp.emit(makeStatus("ok"));

    const result = await waitPromise;
    expect(result.compileState).toBe("ok");
    expect(stomp.closeSpy).toHaveBeenCalledTimes(1);
  });

  it("calls onProgress for non-terminal STOMP messages only", async () => {
    const { client } = makeClient([makeStatus("compiling"), makeStatus("compiling")]);
    const onProgress = jest.fn();
    const waitPromise = waitForCompilation(
      client,
      "p1",
      "main",
      { onProgress },
      stomp.subscribe,
    );

    await new Promise((r) => setImmediate(r));
    stomp.emit(makeStatus("compiling"));
    stomp.emit(makeStatus("compiling"));
    stomp.emit(makeStatus("warnings"));

    const result = await waitPromise;
    expect(result.compileState).toBe("warnings");
    // Two non-terminal frames → two onProgress calls; the terminal frame resolves without onProgress.
    expect(onProgress).toHaveBeenCalledTimes(2);
  });

  it("returns last-seen status when the timeout expires", async () => {
    const { client } = makeClient([makeStatus("compiling"), makeStatus("compiling")]);
    const waitPromise = waitForCompilation(client, "p1", "main", { timeoutMs: 20 }, stomp.subscribe);

    await new Promise((r) => setImmediate(r));
    // Send one non-terminal update so lastSeen advances past the second fetch.
    stomp.emit(makeStatus("compiling", {
      compilation: { messages: { items: [], total: 0, errors: 0, warnings: 0 }, modules: { total: 3, compiled: 1 }, tests: { total: 0 } },
    }));

    const result = await waitPromise;
    expect(result.compileState).toBe("compiling");
    expect(result.compilation?.modules.compiled).toBe(1);
    expect(stomp.closeSpy).toHaveBeenCalledTimes(1);
  });

  it("detaches the abort listener from the caller's signal after a timeout-resolve", async () => {
    // Long-lived AbortControllers (e.g. session-scoped) would otherwise keep our
    // listener attached after the wait completes; verify the timeout path cleans up.
    const { client } = makeClient([makeStatus("compiling"), makeStatus("compiling")]);
    const controller = new AbortController();
    const removeSpy = jest.spyOn(controller.signal, "removeEventListener");

    const result = await waitForCompilation(
      client,
      "p1",
      "main",
      { timeoutMs: 20, signal: controller.signal },
      stomp.subscribe,
    );
    expect(result.compileState).toBe("compiling");
    expect(removeSpy).toHaveBeenCalledWith("abort", expect.any(Function));

    // Aborting after the timeout-resolve must not throw or surface anywhere —
    // the listener is gone, so the post-settlement reject() can't fire.
    expect(() => controller.abort()).not.toThrow();
  });

  it("rejects with AbortError when the signal fires mid-wait and tears down the subscription", async () => {
    const { client } = makeClient([makeStatus("compiling"), makeStatus("compiling")]);
    const controller = new AbortController();
    const waitPromise = waitForCompilation(client, "p1", "main", { signal: controller.signal }, stomp.subscribe);

    await new Promise((r) => setImmediate(r));
    controller.abort();

    await expect(waitPromise).rejects.toMatchObject({ name: "AbortError" });
    expect(stomp.closeSpy).toHaveBeenCalledTimes(1);
  });

  it("rejects immediately if the signal is already aborted before the call", async () => {
    const { client } = makeClient([]);
    const controller = new AbortController();
    controller.abort();
    await expect(
      waitForCompilation(client, "p1", "main", { signal: controller.signal }, stomp.subscribe),
    ).rejects.toMatchObject({ name: "AbortError" });
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("falls back to the initial snapshot when no session cookie is available", async () => {
    const { client } = makeClient([makeStatus("compiling")]);
    // Override getSessionCookie to simulate a studio that issued no cookie.
    (client as unknown as { getSessionCookie: () => string | null }).getSessionCookie = () => null;
    const result = await waitForCompilation(client, "p1", "main", {}, stomp.subscribe);
    expect(result.compileState).toBe("compiling");
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("subscribes to the project's actual branch and ignores the user-provided branch when they match", async () => {
    const { client } = makeClient([makeStatus("compiling", { branch: "main" }), makeStatus("compiling", { branch: "main" })]);
    const waitPromise = waitForCompilation(client, "p1", "main", {}, stomp.subscribe);
    await new Promise((r) => setImmediate(r));
    stomp.emit(makeStatus("ok", { branch: "main" }));
    await waitPromise;
    // STOMP subscribe was called with the actual branch from the status response.
    expect(stomp.subscribe).toHaveBeenCalledTimes(1);
    const firstCallArgs = stomp.subscribe.mock.calls[0][0];
    expect(firstCallArgs.branch).toBe("main");
  });

  it("subscribes with no branch when the project does not support branches", async () => {
    // Status response has no `branch` field — backend signals project doesn't support branches.
    const noBranchStatus: Types.ProjectStatusView = {
      projectId: "p1",
      compileState: "compiling",
      compilation: { messages: { items: [], total: 0, errors: 0, warnings: 0 }, modules: { total: 1, compiled: 0 }, tests: { total: 0 } },
    };
    const { client } = makeClient([noBranchStatus, noBranchStatus]);
    const waitPromise = waitForCompilation(client, "p1", undefined, {}, stomp.subscribe);
    await new Promise((r) => setImmediate(r));
    stomp.emit({ ...noBranchStatus, compileState: "ok" });
    await waitPromise;
    expect(stomp.subscribe).toHaveBeenCalledTimes(1);
    expect(stomp.subscribe.mock.calls[0][0].branch).toBeUndefined();
  });

  it("switches branches via switchBranch when project is already opened and re-fetches before subscribing", async () => {
    // First fetch: project is on main. After switch, second fetch: project is on develop (and still compiling).
    // Third fetch (race-close): still compiling on develop.
    const onMain = makeStatus("compiling", { branch: "main" });
    const onDevelop = makeStatus("compiling", { branch: "develop" });
    const { client, switchBranch, openProject, getProject } = makeClient(
      [onMain, onDevelop, onDevelop],
      { status: "OPENED" }, // project is currently open → switchBranch dispatch
    );
    const waitPromise = waitForCompilation(client, "p1", "develop", {}, stomp.subscribe);
    await new Promise((r) => setImmediate(r));
    stomp.emit(makeStatus("ok", { branch: "develop" }));
    await waitPromise;

    expect(getProject).toHaveBeenCalledTimes(1);
    expect(switchBranch).toHaveBeenCalledWith("p1", "develop");
    expect(openProject).not.toHaveBeenCalled();
    // STOMP subscribed to the post-switch branch.
    expect(stomp.subscribe.mock.calls[0][0].branch).toBe("develop");
  });

  it("switches to the requested branch before returning an already-terminal status", async () => {
    const { client, switchBranch } = makeClient([
      makeStatus("ok", { branch: "main" }),
      makeStatus("warnings", { branch: "develop" }),
    ]);

    const result = await waitForCompilation(client, "p1", "develop", {}, stomp.subscribe);

    expect(switchBranch).toHaveBeenCalledWith("p1", "develop");
    expect(result).toMatchObject({ compileState: "warnings", branch: "develop" });
    expect(stomp.subscribe).not.toHaveBeenCalled();
  });

  it("uses openProject when project is CLOSED and a branch switch is needed", async () => {
    const onMain = makeStatus("compiling", { branch: "main" });
    const onDevelop = makeStatus("compiling", { branch: "develop" });
    const { client, switchBranch, openProject } = makeClient(
      [onMain, onDevelop, onDevelop],
      { status: "CLOSED" },
    );
    const waitPromise = waitForCompilation(client, "p1", "develop", {}, stomp.subscribe);
    await new Promise((r) => setImmediate(r));
    stomp.emit(makeStatus("ok", { branch: "develop" }));
    await waitPromise;

    expect(switchBranch).not.toHaveBeenCalled();
    expect(openProject).toHaveBeenCalledWith("p1", { branch: "develop" });
  });

  it("does not switch branches when the project does not support branches", async () => {
    const noBranchStatus: Types.ProjectStatusView = {
      projectId: "p1",
      compileState: "compiling",
      compilation: { messages: { items: [], total: 0, errors: 0, warnings: 0 }, modules: { total: 1, compiled: 0 }, tests: { total: 0 } },
    };
    const { client, switchBranch, openProject, getProject } = makeClient(
      [noBranchStatus, noBranchStatus],
    );
    const waitPromise = waitForCompilation(client, "p1", "develop", {}, stomp.subscribe);
    await new Promise((r) => setImmediate(r));
    stomp.emit({ ...noBranchStatus, compileState: "ok" });
    await waitPromise;
    // No-branch project → no switch attempted even with mismatched requestedBranch.
    expect(getProject).not.toHaveBeenCalled();
    expect(switchBranch).not.toHaveBeenCalled();
    expect(openProject).not.toHaveBeenCalled();
  });
});

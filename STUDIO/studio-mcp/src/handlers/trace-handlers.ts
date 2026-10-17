/**
 * Trace tool handlers (BETA — interactive Trace Debug API) — drive a real
 * suspended execution: start a session, step into/over/out, run to a
 * breakpoint, inspect a frame's variables, manage breakpoints, expand lazy
 * values, and terminate.
 *
 * The debug session is server-side and bound to the HTTP session — the shared
 * OpenLClient carries the JSESSIONID cookie across calls, so the whole flow
 * must go through one server/CLI process (or a CLI --cookie-jar). Only
 * `/resume` is asynchronous (202): openl_resume_trace polls the status
 * endpoint inside the tool call and returns the stack of the next stop, so
 * the agent never has to poll.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import { formatResponse } from "../formatters.js";
import { isAxiosError, isNotFoundError } from "../utils.js";
import { registerTool, type ToolResponse, type ToolHandlerExtra } from "./common.js";
import type { OpenLClient } from "../client.js";
import type * as Types from "../types.js";

/** How often openl_resume_trace polls GET /status while the worker runs. */
const RESUME_POLL_INTERVAL_MS = 250;
/** Default bound on the openl_resume_trace wait (the backend's own step wait is ~30s). */
const DEFAULT_RESUME_TIMEOUT_MS = 30_000;

/**
 * Default `?fields=` projection for openl_inspect_trace_frame: everything the agent
 * needs to reason (decision outcome, rule names, parameter/step/result values,
 * errors, grid axis names) minus the bulk — value JSON schemas and profiling
 * sub-trees. `full: true` lifts the trim.
 */
const INSPECT_FIELDS =
  "decision,ruleNames,errors,gridColumns,gridRows," +
  "result(name,description,value)," +
  // Keep lazy/parameterId on step values so a large factor still carries its id
  // (for openl_get_trace_value, and for excludeStepValues to resolve it).
  "steps(ref,label,status,value(name,lazy,parameterId,value))," +
  "parameters(name,description,lazy,parameterId,value)," +
  "context(name,description,lazy,parameterId,value)";

/**
 * Default `?fields=` projection for openl_watch_trace_cells: the whole series
 * minus each point value's JSON Schema (large; a lazy value still keeps its
 * parameterId so openl_get_trace_value can expand it). `withSchema` lifts it.
 */
const WATCH_FIELDS =
  "truncated,series(name,table,tableUri,total," +
  "points(instance,label,ref,path,value(name,description,lazy,parameterId,value)))";

function isScalar(v: unknown): v is number | string | boolean {
  return typeof v === "number" || typeof v === "string" || typeof v === "boolean";
}

/** Max concurrent lazy-value fetches when resolving steps for excludeStepValues. */
const LAZY_RESOLVE_CONCURRENCY = 8;

/** Map over `items` running at most `limit` async calls at once (order preserved). */
async function mapLimit<T, R>(
  items: T[],
  limit: number,
  fn: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  const results = new Array<R>(items.length);
  let next = 0;
  const worker = async (): Promise<void> => {
    while (next < items.length) {
      const i = next++;
      results[i] = await fn(items[i], i);
    }
  };
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, worker));
  return results;
}

/**
 * Optionally thin out a frame's `steps` so an anomaly stands out among many
 * neutral factors: `onlyExecuted` keeps only steps that actually computed a
 * value, and `excludeValues` drops steps whose scalar value equals one of the
 * caller-supplied neutral constants (e.g. 1 in rating). Returns the variables
 * unchanged when neither filter is requested.
 *
 * A step's value often arrives lazy (`lazy: true` + `parameterId`, no inline
 * value) — the key multipliers (BaseRate, VehiclePriceFactor, Group*) usually
 * do. Those are resolved (in parallel, via the parameter endpoint) BEFORE the
 * `excludeValues` comparison, so a neutral factor that came lazy is dropped too
 * rather than surviving unmatched.
 */
async function filterSteps(
  variables: Types.DebugFrameVariables,
  opts: { onlyExecuted?: boolean; excludeValues?: Array<number | string | boolean> },
  client: OpenLClient,
  projectId: string,
): Promise<Types.DebugFrameVariables> {
  const exclude = new Set<number | string | boolean>(opts.excludeValues ?? []);
  if (!opts.onlyExecuted && exclude.size === 0) {
    return variables;
  }

  let steps = variables.steps ?? [];
  if (opts.onlyExecuted) {
    steps = steps.filter((step) => step.status === "executed");
  }

  if (exclude.size === 0) {
    return { ...variables, steps };
  }

  // Resolve the scalar of each step (materializing lazy values) so the compare
  // sees the real number, not an absent lazy placeholder. Bounded concurrency —
  // a wide table can have hundreds of lazy steps, and firing them all at once
  // would swamp the single server-side debug session.
  const scalars = await mapLimit(steps, LAZY_RESOLVE_CONCURRENCY, async (step): Promise<unknown> => {
    const value = step.value;
    if (!value) return undefined;
    if (value.value !== undefined) return value.value;
    if (value.lazy && value.parameterId != null) {
      try {
        const resolved = await client.getTraceParameter(projectId, value.parameterId, "value");
        return resolved.value;
      } catch {
        return undefined; // couldn't resolve → keep the step (don't drop blindly)
      }
    }
    return undefined;
  });

  const kept = steps.filter((_step, i) => {
    const scalar = scalars[i];
    return !(isScalar(scalar) && exclude.has(scalar));
  });
  return { ...variables, steps: kept };
}

function is409(error: unknown): boolean {
  return isAxiosError(error) && error.response?.status === 409;
}

/** Abortable sleep for the resume poll loop. */
function delay(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(makeAbortError());
      return;
    }
    const onAbort = (): void => {
      clearTimeout(timer);
      reject(makeAbortError());
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

function makeAbortError(): Error {
  const err = new Error("resume_trace aborted");
  err.name = "AbortError";
  return err;
}

/**
 * Rethrow the trace API's two signature failures as actionable ProtocolErrors:
 * 404 = no active debug session (or it was reaped / lives in another HTTP
 * session), or the referenced frame/parameter does not exist in it;
 * 409 = the session is not suspended. Anything else passes through to the
 * shared error handler.
 */
/**
 * Shared 404 guidance: the debug session is per HTTP session and idle-reaped.
 * Interpolated by both rethrow helpers so the wording stays in one place.
 */
const NO_ACTIVE_SESSION =
  "there is no active debug session (sessions are per HTTP session, reaped after ~10 minutes idle, " +
  "and one started by another process is not visible here — start one with openl_start_trace)";

function rethrowTraceStateError(error: unknown, action: string): never {
  if (isNotFoundError(error)) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidRequest,
      `The ${action} found no target: either ${NO_ACTIVE_SESSION}, or the referenced frame index / parameter id ` +
        `does not exist in the current session (lazy parameter ids are registered when openl_inspect_trace_frame freezes a frame, and cleared on restart).`,
    );
  }
  if (is409(error)) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidRequest,
      `The debug session is not suspended, so ${action} is not possible right now. ` +
        `While running, wait for the stop with openl_resume_trace; on a terminal status (completed/error/terminated) ` +
        `the final state is in the last returned stack — restart with openl_start_trace or finish with openl_stop_trace.`,
    );
  }
  throw error;
}

/**
 * Map the tree-children endpoint's signature failures to actionable ProtocolErrors.
 * Unlike a frame read, expanding the tree does NOT need a suspended session (it
 * reads the retained tree of a completed profiling run), so 404 is about the
 * session or the node reference, and 409 means the tree is not ready yet.
 */
function rethrowTraceTreeError(error: unknown): never {
  if (isNotFoundError(error)) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidRequest,
      `Call-tree expansion found no target: either ${NO_ACTIVE_SESSION}, ` +
        "or the uri/instance/step does not name a retained node. The executed tree exists only for a profiling run (openl_start_trace with profiling: true), and a step is expandable only when its childrenTotal > 0; take the uri/instance from the /stack `tree` root (includeTree: true) or an earlier openl_expand_trace_tree page.",
    );
  }
  if (is409(error)) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidRequest,
      "The executed call tree is not available yet — the profiling run is still in progress. Wait for it to finish with openl_resume_trace, then expand the tree.",
    );
  }
  throw error;
}

/**
 * Poll the session status until it leaves running/pending, then read and return
 * the stack (`{ stack }`). Bounded by `timeoutMs`; on expiry returns the last
 * still-running status (`{ stillRunning }`) so the caller can report it.
 */
async function waitForStop(
  client: OpenLClient,
  projectId: string,
  timeoutMs: number,
  extra?: ToolHandlerExtra,
  stackOptions?: { view?: "full" | "compact"; includeTree?: boolean; profileTop?: number },
): Promise<{ stack: Types.DebugStackView } | { stillRunning: Types.DebugStatus }> {
  const startedAt = Date.now();
  const progressToken = extra?._meta?.progressToken;
  const sendNotification = extra?.sendNotification;

  for (;;) {
    const { status } = await client.getTraceStatus(projectId);
    if (status !== "running" && status !== "pending") {
      return { stack: await client.getTraceStack(projectId, stackOptions) };
    }
    if (Date.now() - startedAt >= timeoutMs) {
      return { stillRunning: status };
    }
    if (progressToken !== undefined && sendNotification) {
      // Notification failures are non-fatal — the wait resolves on the status flip.
      void sendNotification({
        method: "notifications/progress",
        params: {
          progressToken,
          progress: Math.round((Date.now() - startedAt) / 1000),
          message: `Trace ${status} — waiting for the next stop…`,
        },
      }).catch(() => { /* ignore */ });
    }
    await delay(RESUME_POLL_INTERVAL_MS, extra?.signal);
  }
}

export function registerTraceHandlers(): void {
  registerTool({
    name: "start_trace",
    category: "Trace",
    title: "Start Debug Session",
    description:
      "Start an interactive debug session for a table and run to the first stop. Returns the execution stack (status + frames root→current). " +
      "Default stopAtEntry: true suspends at the entry of the first frame; from there use openl_step_trace / openl_resume_trace and openl_inspect_trace_frame. " +
      "For test tables pass testRanges (e.g. '2'); for regular rules pass inputJson { params, runtimeContext? }; omitting both replays the previous run's remembered input. " +
      "Cheapest way to understand a whole run: profiling: true with stopAtEntry: false and no breakpoints — completes in this one call and returns 'profile', a constant-size overview of the top-N slowest tables (selfMillis/totalMillis/count) plus nodeCount/distinctTables/totalMillis. " +
      "For a profiling overview pass inputJson (or testRanges) together with profiling: true and stopAtEntry: false EXPLICITLY every time — do not rely on replay (omitting the input): a replay only reproduces the compact profile if the remembered run was itself a profiling run, otherwise it can return a much larger stack that overflows the response limit. " +
      "Find the hot or unexpected table in profile.hotspots, then replay into it with a breakpoint to inspect live values. The executed 'tree' is omitted by default; set includeTree: true to get its ROOT node (one level — each step carries a childrenTotal count) and browse a branch level by level with openl_expand_trace_tree. " +
      "One active session per user — starting a new one terminates the previous. Idle sessions are reaped after ~10 minutes.",
    schema: schemas.startTraceSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      if (typedArgs.breakpoints) {
        await client.setTraceBreakpoints(typedArgs.projectId, typedArgs.breakpoints);
      }

      const stack = await client.startTrace({
        projectId: typedArgs.projectId,
        tableId: typedArgs.tableId,
        testRanges: typedArgs.testRanges,
        fromModule: typedArgs.fromModule,
        inputJson: typedArgs.inputJson,
        stopAtEntry: typedArgs.stopAtEntry,
        profiling: typedArgs.profiling,
        detailedTitles: typedArgs.detailedTitles,
        breakOnErrors: typedArgs.breakOnErrors,
        // Never pull the >1 MB full tree unless explicitly asked; the bounded
        // `profile` overview covers the "understand the whole run" case.
        includeTree: typedArgs.profiling ? (typedArgs.includeTree ?? false) : undefined,
        profileTop: typedArgs.profileTop,
      });

      return { content: [{ type: "text", text: formatResponse(stack, format) }] };
    },
  });

  registerTool({
    name: "step_trace",
    category: "Trace",
    title: "Step (Into / Over / Out)",
    description:
      "Step the suspended debug session once and return the new stack. For declarative rules (decision tables, spreadsheets, rating) the main move is type: 'out' — run the current frame to its own exit " +
      "so its result is inspectable — combined with breakpoints; 'into'/'over' are advanced (imperative TBasic/loops). A step that finishes a frame first suspends at that frame's exit — the frame is still on the stack " +
      "with completed: true and its result readable via openl_inspect_trace_frame (or pass withValues: true to bundle those variables into this response); the next step continues in the caller. " +
      "An exception suspends at the throwing frame before it propagates. The stack is returned compact — steps only for the active frame; use openl_inspect_trace_frame for another frame's detail. Valid only while suspended. " +
      "(openl_resume_trace differs: it runs to the next breakpoint or completion, not just to this frame's exit.)",
    schema: schemas.stepTraceSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      let stack: Types.DebugStackView;
      try {
        stack = await client.traceStep(typedArgs.projectId, typedArgs.type, {
          view: "compact",
          includeTree: typedArgs.includeTree ?? false,
          profileTop: typedArgs.profileTop,
        });
      } catch (error) {
        rethrowTraceStateError(error, "step");
      }

      // Bundle the active frame's variables so a `step out → inspect` cycle is
      // one call. Only meaningful while suspended with a frame on the stack.
      let result: unknown = stack;
      if (typedArgs.withValues && stack.status === "suspended" && stack.frames.length > 0) {
        const active = stack.frames.find((f) => f.active) ?? stack.frames[stack.frames.length - 1];
        const variables = await client.getTraceFrameVariables(typedArgs.projectId, active.index, INSPECT_FIELDS);
        result = { ...stack, variables };
      }

      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });

  registerTool({
    name: "resume_trace",
    category: "Trace",
    title: "Resume to Next Stop",
    description:
      "Resume the suspended debug session and wait (inside this call — no agent-side polling) until it stops again: at the next breakpoint, at an exception, or at completion. Unlike openl_step_trace(out), which only runs the current frame to its exit, resume runs to the NEXT breakpoint or the end. " +
      "Returns the stack (compact — steps for the active frame only) at the stop; on a terminal 'error' status it carries the structured 'error', and on 'completed' of a profiling run the constant-size 'profile' overview (set includeTree: true for the one-level 'tree' root, then drill in with openl_expand_trace_tree). " +
      "On timeout (default 30s) the still-running status is returned — call openl_resume_trace again to keep waiting (it re-attaches without re-resuming), or openl_stop_trace to give up.",
    schema: schemas.resumeTraceSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const timeoutMs = typedArgs.timeoutMs ?? DEFAULT_RESUME_TIMEOUT_MS;

      try {
        await client.traceResume(typedArgs.projectId);
      } catch (error) {
        // Re-attach path: a previous resume timed out and the worker is still
        // running — the 409 then only means "not suspended", so fall through
        // to the wait instead of failing.
        if (is409(error)) {
          const { status } = await client.getTraceStatus(typedArgs.projectId);
          if (status !== "running" && status !== "pending") {
            rethrowTraceStateError(error, "resume");
          }
        } else {
          rethrowTraceStateError(error, "resume");
        }
      }

      let outcome: { stack: Types.DebugStackView } | { stillRunning: Types.DebugStatus };
      try {
        outcome = await waitForStop(client, typedArgs.projectId, timeoutMs, extra, {
          view: "compact",
          includeTree: typedArgs.includeTree ?? false,
          profileTop: typedArgs.profileTop,
        });
      } catch (error) {
        // A client cancellation surfaces as an AbortError from the poll's delay.
        if (error instanceof Error && error.name === "AbortError") {
          throw new ProtocolError(ProtocolErrorCode.InvalidRequest, "resume_trace: request cancelled while waiting for the next stop.");
        }
        // The session can be reaped or the studio can blip mid-poll — map the
        // 404/409 to the same actionable message the other trace tools give.
        rethrowTraceStateError(error, "resume");
      }

      if ("stillRunning" in outcome) {
        // Timed out — report the actual status in the same format as a stop, so
        // JSON consumers stay consistent and can read `status`.
        const timedOut = {
          status: outcome.stillRunning,
          message:
            `Trace is still ${outcome.stillRunning} after waiting ${Math.round(timeoutMs / 1000)}s — the rule may be computing or looping. ` +
            `Call openl_resume_trace again to keep waiting (optionally with a larger timeoutMs), or openl_stop_trace to terminate.`,
        };
        return { content: [{ type: "text", text: formatResponse(timedOut, format) }] };
      }

      return { content: [{ type: "text", text: formatResponse(outcome.stack, format) }] };
    },
  });

  registerTool({
    name: "inspect_trace_frame",
    category: "Trace",
    title: "Inspect Stack Frame",
    description:
      "Freeze and read the full state of one suspended stack frame: input parameters, runtime context, result (for a completed frame), sub-steps with computed values, and for a " +
      "decision table the killer feature — 'decision' (which rule fired and how each condition evaluated per rule) plus 'ruleNames' (all rules, for per-rule breakpoints). " +
      "Values may come lazy (lazy: true + parameterId) — expand with openl_get_trace_value. By default the response is trimmed (no value JSON schemas); full: true lifts the trim. " +
      "To surface an anomaly among many neutral factors, filter the steps: onlyExecutedSteps drops not-yet-computed ones, and excludeStepValues drops steps whose value is a neutral constant (e.g. [1] in rating) — lazy step values are resolved before the comparison, so a neutral factor that came lazy is dropped too. " +
      "withHighlights: true additionally returns the A1-keyed cell highlight overlay and the raw table grid to merge it with. Valid only while suspended (a terminal session answers 409 — read its final state from the last returned stack).",
    schema: schemas.inspectTraceFrameSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // The highlight overlay and the stack only need projectId/frameIndex, so
      // fire them alongside the variables fetch (which may itself do lazy-value
      // round-trips) instead of after it. (compact omits every frame's steps —
      // only the frame's tableId is needed.)
      const highlightsP = typedArgs.withHighlights
        ? client.getTraceFrameHighlights(typedArgs.projectId, typedArgs.frameIndex)
        : undefined;
      const stackP = typedArgs.withHighlights
        ? client.getTraceStack(typedArgs.projectId, { view: "compact" })
        : undefined;

      let variables: Types.DebugFrameVariables;
      try {
        variables = await client.getTraceFrameVariables(
          typedArgs.projectId,
          typedArgs.frameIndex,
          typedArgs.full ? undefined : INSPECT_FIELDS,
          typedArgs.full ?? false,
        );
      } catch (error) {
        // Don't leak the in-flight parallel fetches as unhandled rejections.
        void Promise.allSettled([highlightsP, stackP]);
        rethrowTraceStateError(error, "frame inspection");
      }

      variables = await filterSteps(
        variables,
        { onlyExecuted: typedArgs.onlyExecutedSteps, excludeValues: typedArgs.excludeStepValues },
        client,
        typedArgs.projectId,
      );

      let result: unknown = variables;
      if (highlightsP && stackP) {
        const [highlights, stack] = await Promise.all([highlightsP, stackP]);
        // The overlay is keyed by A1 cell address; the grid to paint it on comes
        // from the shared Tables API raw view, resolved via the frame's tableId.
        const frame = stack.frames.find((f) => f.index === typedArgs.frameIndex);
        const grid = frame
          ? await client.getTable(typedArgs.projectId, frame.tableId)
          : undefined;
        result = { ...variables, highlights, grid };
      }

      return {
        content: [{ type: "text", text: formatResponse(result, format) }],
      };
    },
  });

  registerTool({
    name: "set_trace_breakpoints",
    category: "Trace",
    title: "Read / Replace Breakpoints",
    description:
      "Read the active breakpoint keys and the available targets (rule tables, deduplicated by name; with an active session only tables reachable from the traced one). " +
      "When 'set' is provided it REPLACES the whole set first (empty array clears all). Key forms: '<name>' stops at entry of every same-named table version; '<uri>' at that exact table " +
      "(uri from frames[].uri); '<uri>#R{r}C{c}' at a spreadsheet cell; '<uri>#rule' when ANY rule of that decision table fires; '<uri>#<ruleName>' when a specific rule fires " +
      "(rule names from openl_inspect_trace_frame ruleNames/decision). Append '@N' to any key to break only on the table's N-th execution (0-based) — e.g. '<uri>#R48C0@3'; without it a cell breakpoint hits every pass of a table that runs many times (one per coverage/iteration). N matches frames[].instance and the 'instance' of an openl_watch_trace_cells series, so a watch outlier at instance 3 is reached with '@3'. " +
      "Works without a session — set breakpoints before openl_start_trace; changes during a session apply at the next frame enter or line change.",
    schema: schemas.setTraceBreakpointsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      if (typedArgs.set) {
        await client.setTraceBreakpoints(typedArgs.projectId, typedArgs.set);
      }

      const [breakpoints, targets] = await Promise.all([
        client.getTraceBreakpoints(typedArgs.projectId),
        client.getTraceBreakpointTables(typedArgs.projectId),
      ]);

      return {
        content: [{ type: "text", text: formatResponse({ breakpoints, targets }, format) }],
      };
    },
  });

  registerTool({
    name: "get_trace_value",
    category: "Trace",
    title: "Expand Lazy Value",
    description:
      "Fetch the full value of a parameter that openl_inspect_trace_frame returned lazily (lazy: true with a parameterId). Valid while the debug session is alive. " +
      "By default only name, description, and value are returned; withSchema: true adds the value's JSON Schema (large — request it only when the type structure itself matters).",
    schema: schemas.getTraceValueSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      let param: Types.TraceParameterValue;
      try {
        param = await client.getTraceParameter(
          typedArgs.projectId,
          typedArgs.parameterId,
          typedArgs.withSchema ? undefined : "name,description,value",
          typedArgs.withSchema ?? false,
        );
      } catch (error) {
        rethrowTraceStateError(error, "lazy value fetch");
      }

      return {
        content: [{ type: "text", text: formatResponse(param, format) }],
      };
    },
  });

  registerTool({
    name: "expand_trace_tree",
    category: "Trace",
    title: "Expand Call-Tree Node",
    description:
      "Load one level of a profiling run's executed call tree on demand. The tree is lazy: the /stack `tree` root (a profiling openl_start_trace / openl_resume_trace with includeTree: true) and every node returned here are ONE level deep — each step carries a `childrenTotal` count instead of nested `children`. " +
      "Expand a step whose childrenTotal > 0 by naming its node (uri + instance) and the step's ref; the reply is a TreeChildrenView { children, total } where each child is itself shallow (its own steps' childrenTotal), expanded by calling this tool again. " +
      "Page a loop's many sub-calls with offset/limit (default 100): when total > offset + children.length the reply sets hasMore: true and nextOffset — call again with that offset to get the next page. A returned node may also carry `notRetained` — sub-calls that ran but were dropped once the retained tree hit its size limit (report as '+N not retained'). " +
      "Requires a profiling run (profiling: true retains the tree). To find what's slow use the constant-size `profile` overview instead; use this to walk one branch's call structure. Valid while the debug session is alive (including after the run completes).",
    schema: schemas.expandTraceTreeSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      let view: Types.TreeChildrenView;
      try {
        view = await client.getTraceTreeChildren(typedArgs.projectId, {
          uri: typedArgs.uri,
          instance: typedArgs.instance,
          step: typedArgs.step,
          offset: typedArgs.offset,
          limit: typedArgs.limit,
        });
      } catch (error) {
        rethrowTraceTreeError(error);
      }

      // Normalize the page defensively: the backend omits `children` for a
      // childless step (reporting total 0), a 200 body could even be literal null,
      // and a non-compliant client could send a non-numeric offset. Then surface
      // pagination so the agent can page a loop's children (a step can make
      // thousands) without redoing the offset math.
      const children = Array.isArray(view?.children) ? view.children : [];
      const total = typeof view?.total === "number" ? view.total : 0;
      const offset = Number(typedArgs.offset) || 0;
      // Stop as soon as a page returns nothing: `total` counts a step's FULL
      // sub-calls, INCLUDING ones dropped at the tree's size cap that can never be
      // paged to, so keying `hasMore` on `total` alone would loop forever with a
      // frozen nextOffset. An empty page means there is no next page.
      const hasMore = children.length > 0 && offset + children.length < total;
      const payload = hasMore
        ? { children, total, hasMore, nextOffset: offset + children.length }
        : { children, total, hasMore };

      return { content: [{ type: "text", text: formatResponse(payload, format) }] };
    },
  });

  registerTool({
    name: "stop_trace",
    category: "Trace",
    title: "Terminate Debug Session",
    description:
      "Terminate the debug session and free its worker and lazy-value registry. Idempotent — succeeds even when no session is active. Breakpoints survive (they are session-scoped, not run-scoped).",
    schema: schemas.stopTraceSchema,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      await client.stopTrace(typedArgs.projectId);

      return {
        content: [{ type: "text", text: "Debug session terminated." }],
      };
    },
  });

  registerTool({
    name: "watch_trace_cells",
    category: "Trace",
    title: "Watch Cells Across a Run",
    description:
      "Answer 'show me this factor across all coverages/iterations' in one call, without dumping frames. Watch SCALAR cells (a single number/string factor, e.g. '$VehiclePriceFactor') — NOT a cell whose value is a big aggregate object (a whole spreadsheet result like '$RateCardPremium'), which makes every captured point huge and can overflow the response; drill into an aggregate with a breakpoint + openl_inspect_trace_frame instead. " +
      "Runs the table to completion and returns a WatchView: one 'series' per cell with a 'points' array holding the cell's value at each execution of its table (each point carries instance/label/ref/path; value is serialized like any traced value and may come lazy — expand a large one with openl_get_trace_value using its parameterId). " +
      "Read the series, spot the outlier (e.g. 83.372 among 1.0s), then jump straight to that pass: set a breakpoint '<point.ref>@<point.instance>' (the '@N' suffix targets the N-th execution — same 0-based numbering as the series' 'instance') and replay + openl_inspect_trace_frame to see why. Value JSON Schemas are omitted by default (withSchema: true restores them). " +
      "Captures cells inside lazy result branches too (nested SpreadsheetResult[]) — the run materializes the whole result. The server caps points per series for a cell deep in a combinatorial branch (benefit × gender × age-band …); each series reports 'total' (the full execution count) and WatchView.truncated flags that some late executions were dropped — inspect a specific one with a '<ref>@N' breakpoint. Pass testRanges for a test table or inputJson for a regular rule (omit both to replay the remembered input). This starts a fresh session (terminates any previous one) and clears breakpoints so the run reaches completion.",
    schema: schemas.watchTraceCellsSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // Clear any breakpoints left set earlier in this session — they are
      // session-scoped and would suspend the run mid-way, so the watch would
      // capture only a partial series. The watch needs a run to completion.
      await client.setTraceBreakpoints(typedArgs.projectId, []);

      // Set the watch cells, run the whole table to completion, then read the
      // collected series. The run fully materializes the traced method's result,
      // so cells inside lazy result branches (nested SpreadsheetResult[]) are
      // evaluated and captured — no profiling needed (profiling would only make
      // the studio build and retain a large call tree this tool discards). The
      // server caps points per series, so the response is bounded.
      await client.setTraceWatches(typedArgs.projectId, typedArgs.cells);
      await client.startTrace({
        projectId: typedArgs.projectId,
        tableId: typedArgs.tableId,
        testRanges: typedArgs.testRanges,
        fromModule: typedArgs.fromModule,
        inputJson: typedArgs.inputJson,
        stopAtEntry: false,
      });

      let watch: Types.WatchView;
      try {
        watch = await client.getTraceWatch(
          typedArgs.projectId,
          typedArgs.withSchema ? undefined : WATCH_FIELDS,
          typedArgs.withSchema ?? false,
        );
      } catch (error) {
        rethrowTraceStateError(error, "watch read");
      }

      // The server truncates when a cell fires more times than the capture cap.
      // Surface guidance to reach a dropped execution (each series' `total` gives
      // the full count).
      let payload: unknown = watch;
      if (watch.truncated) {
        payload = {
          note:
            "Some series were truncated by the server (their 'total' exceeds the returned points, so late executions are missing). " +
            "Inspect a specific execution with a '<ref>@N' breakpoint, or watch a higher-level cell.",
          ...watch,
        };
      }

      return {
        content: [{ type: "text", text: formatResponse(payload, format) }],
      };
    },
  });
}

/**
 * Trace tool handlers (BETA — interactive Trace Debug API) — drive a real
 * suspended execution: start a session, step into/over/out, run to a
 * breakpoint, inspect a frame's variables, manage breakpoints, expand lazy
 * values, and terminate.
 *
 * The debug session is server-side: Studio keeps it for the client — its
 * Personal Access Token, or the one user in single-user mode — so every call of
 * the flow that comes with it finds the session. Only
 * `/resume` is asynchronous (202): openl_resume_trace polls the status
 * endpoint inside the tool call and returns the stack of the next stop, so
 * the agent never has to poll.
 */
import { setTimeout as sleep } from "node:timers/promises";
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";

import { isConflictError, isNotFoundError } from "../http.js";
import { registerTool, formatResult, textResult, type ToolResponse, type ToolHandlerExtra } from "./common.js";
import { fitLines } from "../markdown-list.js";
import { cutText, formatCount, tableLabel } from "../text.js";
import { reportProgress } from "./wait.js";
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
  // Each worker takes the next item once it is done with the previous one
  const worker = async (): Promise<void> => {
    if (next < items.length) {
      const i = next++;
      results[i] = await fn(items[i], i);
      return worker();
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
  const exclude = new Set<unknown>(opts.excludeValues ?? []);
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

  return { ...variables, steps: steps.filter((_step, i) => !exclude.has(scalars[i])) };
}

/**
 * The variables of a frame without the fields of the runtime context the run left unset: a context has a dozen
 * fields, mostly null, which every inspection would repeat.
 */
function withoutUnsetContext(variables: Types.DebugFrameVariables): Types.DebugFrameVariables {
  const value = variables.context?.value;
  if (!variables.context || value === null || typeof value !== "object" || Array.isArray(value)) {
    return variables;
  }
  const set = Object.fromEntries(Object.entries(value).filter(([, field]) => field !== null));
  return { ...variables, context: { ...variables.context, value: set } };
}

/**
 * The stack with the variables of its active frame, whose steps the frame lists in part already: the frame lists every
 * step once, with its value from the variables and its cell from the frame, and the variables leave their own copy of
 * the steps out. A frame without steps keeps the variables whole.
 */
function withStepValues(
  stack: Types.DebugStackView,
  frameIndex: number,
  variables: Types.DebugFrameVariables,
): Types.DebugStackView & { variables: Partial<Types.DebugFrameVariables> } {
  const frame = stack.frames.find((candidate) => candidate.index === frameIndex);
  if (!frame?.steps) {
    return { ...stack, variables };
  }
  const { steps, ...rest } = variables;
  const listed = new Map(frame.steps.map((step) => [step.ref, step]));
  const merged = steps.map((step) => ({ ...listed.get(step.ref), ...step }));
  const known = new Set(steps.map((step) => step.ref));
  const onlyListed = frame.steps.filter((step) => !known.has(step.ref));
  return {
    ...stack,
    frames: stack.frames.map((candidate) => (candidate === frame ? { ...frame, steps: [...merged, ...onlyListed] } : candidate)),
    variables: rest,
  };
}

/**
 * The note on the watched cells no series answers. A name the run never computed reads as an empty series. A run that
 * stopped early or reached its capture limit may not have reached the cell, so its name is wrong only possibly.
 */
function unseenCellsNote(cells: string[], watch: Types.WatchView, whole: boolean): string[] {
  const seen = new Set(watch.series.map((series) => series.name));
  const unseen = cells.filter((cell) => !seen.has(cell));
  const names = unseen.map((cell) => "'" + cell + "'").join(", ");
  const check = "check the name as openl_inspect_trace_frame lists the steps of the table, such as '$Value$BankRating'.";
  if (unseen.length === 0) {
    return [];
  }
  return [whole
    ? `The run computed no cell named ${names}: ${check}`
    : `No value of ${names} was captured before the capture ended: if the run should have reached it, ${check}`];
}

/** A traced value without `lazy: false`, its default, and without a name that `repeated` already tells. */
function trimmedValue(value: Types.TraceParameterValue, repeated?: string): Partial<Types.TraceParameterValue> {
  return Object.fromEntries(Object.entries(value)
    .filter(([key, field]) => !(key === "lazy" && field === false) && !(key === "name" && field === repeated)));
}

/** The most rule names a frame lists unless all are asked for: a lookup of 900 rows has 900, R1 to R900. */
const LISTED_RULE_NAMES = 50;

/**
 * The variables of a frame as the trace tools answer them: each value without its defaults, a step value without the
 * ref of its step as its name, and without a runtime context that holds no field, the schema of the runtime context,
 * or a list of no errors. The rule names of a table of more than {@link LISTED_RULE_NAMES} rules are counted instead,
 * unless `allRuleNames`, and so are rules numbered R1 to Rn, which a table that names none of its rules has.
 */
function trimmedVariables(
  variables: Types.DebugFrameVariables,
  allRuleNames?: boolean,
): Partial<Types.DebugFrameVariables> & { ruleCount?: number } {
  const trimmed = (key: string, value: unknown): [string, unknown] | [] => {
    switch (key) {
      case "parameters":
        return [key, (value as Types.TraceParameterValue[]).map((parameter) => trimmedValue(parameter))];
      case "context": {
        // The schema of the runtime context lists every country, currency and language it takes, which no frame needs
        const { schema: _, ...context } = value as Types.TraceParameterValue;
        const fields = context.value;
        const empty = typeof fields === "object" && fields !== null && !Array.isArray(fields) && Object.keys(fields).length === 0;
        return empty ? [] : [key, trimmedValue(context)];
      }
      case "result":
        return [key, trimmedValue(value as Types.TraceParameterValue)];
      case "steps":
        return [key, (value as Types.DebugFrameVariables["steps"]).map((step) =>
          (step.value ? { ...step, value: trimmedValue(step.value, step.ref) } : step))];
      case "errors":
        return (value as unknown[]).length === 0 ? [] : [key, value];
      case "ruleNames": {
        const names = value as string[];
        // The rules of a table that names none are R1 to Rn, which their count tells
        const numbered = names.every((name, index) => name === `R${index + 1}`);
        return numbered || (names.length > LISTED_RULE_NAMES && !allRuleNames) ? ["ruleCount", names.length] : [key, names];
      }
      default:
        return [key, value];
    }
  };
  return Object.fromEntries(Object.entries(variables).map(([key, value]) => trimmed(key, value)).filter((entry) => entry.length > 0));
}

/** The longest text of a traced value a step line shows: openl_get_trace_value or openl_inspect_trace_frame read it whole. */
const VALUE_TEXT = 200;

/** A time in milliseconds as an agent reads it: `0.26 ms`. */
function millis(value: number): string {
  return `${Number(value.toFixed(2))} ms`;
}

/** A step or a place by its ref, which a breakpoint takes, and its label when it says more: `R1C0 $Factor`. */
function refLabel(ref: string | undefined, label: string | undefined): string {
  return label === undefined || label === ref ? ref ?? "" : `${ref} ${label}`;
}

/** A traced value on one line: its JSON, cut, or the parameterId openl_get_trace_value reads a lazy one by. */
function valueText(value: Partial<Types.TraceParameterValue>): string {
  return value.lazy ? `lazy, parameterId ${value.parameterId}` : cutText(JSON.stringify(value.value) ?? "null", VALUE_TEXT);
}

/** A step of a frame or of a call-tree node on one line: `- R1C0 $Factor C11 executed 0.02 ms = 1.4, 2 calls`. */
function stepLine(step: Types.StepValueView): string {
  const head = [`- ${refLabel(step.ref, step.label)}`, step.cell, step.status, step.decision,
    step.durationMillis === undefined ? undefined : millis(step.durationMillis)].filter(Boolean).join(" ");
  const value = step.value ? ` = ${valueText(step.value)}` : "";
  return `${head}${value}${step.childrenTotal ? `, ${formatCount(step.childrenTotal, "call")}` : ""}`;
}

/** The version a dispatcher chose, when it had several to choose from. */
function chosenVersion(dispatch: Types.DispatchInfo | undefined): string | undefined {
  return (dispatch?.candidates.length ?? 0) > 1 ? dispatch?.candidates.find((candidate) => candidate.chosen)?.label : undefined;
}

/**
 * A frame on one line: `#3 AreaFactor (decisionTable, QFVRFV3YHVHG7S4E) @2 at R4C0 $Zip completed in 0.3 ms`. `#N` is the
 * frameIndex the frame tools take, `@N` the execution of the table a `<name>@N` breakpoint stops at, left out for the
 * first; its depth is told only when it does not follow the index, and its place only when no listed step is current.
 */
function frameLine(frame: Types.DebugFrameView): string {
  const { location } = frame;
  const listed = frame.steps?.some((step) => step.status === "current" && step.ref === location?.ref);
  const own = frame.selfMillis !== undefined && frame.selfMillis !== frame.durationMillis ? `, own ${millis(frame.selfMillis)}` : "";
  const version = chosenVersion(frame.dispatch);
  return [
    `#${frame.index} ${tableLabel(frame.name, frame.kind, frame.tableId)}`,
    frame.instance ? `@${frame.instance}` : undefined,
    frame.depth === frame.index + 1 ? undefined : `depth ${frame.depth}`,
    location && !listed ? `at ${refLabel(location.ref, location.label)}` : undefined,
    frame.error ? "error" : undefined,
    frame.completed ? "completed" : undefined,
    frame.durationMillis === undefined ? undefined : `in ${millis(frame.durationMillis)}${own}`,
    version && `version ${version}`,
  ].filter(Boolean).join(" ");
}

/** The profile of a run: its time and calls, and its slowest tables by their own time. */
function profileLines(profile: Types.ProfileSummaryView): string[] {
  const cut = profile.truncated ? ", the call tree cut at its size limit" : "";
  return [
    "",
    `Profile: ${millis(profile.totalMillis)}, ${formatCount(profile.nodeCount, "call")} of ${formatCount(profile.distinctTables, "table")}${cut}; the slowest by own time:`,
    ...profile.hotspots.map((hot) =>
      `- ${tableLabel(hot.name, hot.kind)} own ${millis(hot.selfMillis)}, total ${millis(hot.totalMillis)}, ${formatCount(hot.count, "call")}`),
  ];
}

/** The root of the call tree, by the uri and instance openl_expand_trace_tree takes. */
function treeLine(tree: Types.CallNodeView): string {
  const dropped = tree.notRetained ? `; ${tree.notRetained} sub-calls not retained` : "";
  return `Call tree root: ${tableLabel(tree.name, tree.kind)} in ${millis(tree.durationMillis)}, own ${millis(tree.selfMillis)}${dropped}; ` +
    `uri ${tree.uri}, instance ${tree.instance ?? 0}.`;
}

/**
 * A stack as text, which an agent reads after every step of a debug session: the status, a frame a line from the root,
 * the profile and the root of the call tree of a profiling run, the steps of the frames listing them and of the root,
 * cut by whole steps to the size of an answer, then the values of the active frame when read with it, and a note. JSON
 * spent most of a stack on keys and on the uri of every frame, which no breakpoint needs: Studio takes a table by name.
 */
function stackText(stack: Types.DebugStackView & { variables?: Partial<Types.DebugFrameVariables> }, note?: string): string {
  const { status, frames, error, tree, profile, variables } = stack;
  const active = frames.find((frame) => frame.active) ?? frames.at(-1);
  const where = error ? [error.table, error.location].filter(Boolean).join(", ") : "";
  return fitLines({
    head: [
      `Status: ${status}.`,
      ...(error ? [`Error: ${error.summary}${where ? ` (${where})` : ""}${error.detail ? `: ${error.detail}` : ""}`] : []),
      ...(frames.length ? ["", "Frames, from the root:", ...frames.map(frameLine)] : []),
      ...(profile ? profileLines(profile) : []),
      ...(tree ? ["", treeLine(tree)] : []),
    ],
    lines: [
      ...frames.flatMap((frame) => (frame.steps?.length ? ["", `Steps of #${frame.index}:`, ...frame.steps.map(stepLine)] : [])),
      ...(tree?.steps.length ? ["", "Steps of the call-tree root:", ...tree.steps.map(stepLine)] : []),
    ],
    tail: [
      ...(variables && active ? ["", `Values of #${active.index}: ${JSON.stringify(variables)}`] : []),
      ...(note ? ["", note] : []),
    ],
    more: (left) => `… ${left} more lines of steps: read a frame with openl_inspect_trace_frame.`,
  });
}

/**
 * Shared 404 guidance: the debug session belongs to the client and is idle-reaped.
 * Interpolated by both rethrow helpers so the wording stays in one place.
 */
const NO_ACTIVE_SESSION =
  "there is no active debug session (a session belongs to the token of the client, or to the one user in " +
  "single-user mode, and is reaped after ~10 minutes idle — start one with openl_start_trace)";

/**
 * Rethrow the trace API's two signature failures as actionable ProtocolErrors:
 * 404 = no active debug session (or it was reaped / belongs to another client),
 * or the referenced frame/parameter does not exist in it;
 * 409 = the session is not suspended. Anything else passes through to the
 * shared error handler.
 */
function rethrowTraceStateError(error: unknown, action: string): never {
  if (isNotFoundError(error)) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidRequest,
      `The ${action} found no target: either ${NO_ACTIVE_SESSION}, or the referenced frame index / parameter id ` +
        `does not exist in the current session (lazy parameter ids are registered when openl_inspect_trace_frame freezes a frame, and cleared on restart).`,
    );
  }
  if (isConflictError(error)) {
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
        "or the uri/instance/step does not name a retained node. The executed tree exists only for a profiling run (openl_start_trace with profiling: true), and a step is expandable only when its childrenTotal > 0; take the uri/instance from the call-tree root of the stack (includeTree: true) or an earlier openl_expand_trace_tree page.",
    );
  }
  if (isConflictError(error)) {
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
  stackOptions?: Types.TraceStackOptions,
): Promise<{ stack: Types.DebugStackView } | { stillRunning: Types.DebugStatus }> {
  const startedAt = Date.now();
  const poll = async (): Promise<{ stack: Types.DebugStackView } | { stillRunning: Types.DebugStatus }> => {
    const { status } = await client.getTraceStatus(projectId);
    if (status !== "running" && status !== "pending") {
      return { stack: await client.getTraceStack(projectId, stackOptions) };
    }
    if (Date.now() - startedAt >= timeoutMs) {
      return { stillRunning: status };
    }
    reportProgress(extra, startedAt, `Trace ${status} — waiting for the next stop…`);
    // An unreferenced timer does not keep the process alive; a cancelled call rejects with an AbortError.
    await sleep(RESUME_POLL_INTERVAL_MS, undefined, { signal: extra?.signal, ref: false });
    return poll();
  };
  return poll();
}

export function registerTraceHandlers(): void {
  registerTool({
    name: "start_trace",
    compiled: true,
    title: "Start Debug Session",
    description:
      "Start a debug session on a table and run to its first stop. Example: {\"projectId\":\"…\",\"tableId\":\"CalculatePremium\",\"inputJson\":{\"age\":25},\"stopAtEntry\":false,\"breakpoints\":[\"AgeFactor#rule\"]}. The answer is the stack: the status and a line per frame from the root, its '#N' the frameIndex the frame tools take and its '@N' the execution of the table a breakpoint takes. Go on with openl_step_trace, openl_resume_trace and openl_inspect_trace_frame. profiling: true with stopAtEntry: false and breakpoints: [] runs to the end in this call and answers 'profile', the slowest tables; pass the input again for it, as a replay of a run that did not profile answers the stack.",
    schema: schemas.startTraceSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      if (args.breakpoints) {
        await client.setTraceBreakpoints(args.projectId, args.breakpoints);
      }

      const stack = await client.startTrace({
        ...args,
        // Never pull the >1 MB full tree unless explicitly asked; the bounded
        // `profile` overview covers the "understand the whole run" case.
        includeTree: args.profiling ? (args.includeTree ?? false) : undefined,
      });

      return textResult(stackText(stack));
    },
  });

  registerTool({
    name: "step_trace",
    title: "Step (Into / Over / Out)",
    description:
      "Step the suspended debug session once and answer the new stack, as openl_start_trace does. A finished frame stops first at its exit, marked completed, so its result can be read; the next step goes on in the caller. An exception stops at the throwing frame. openl_resume_trace runs to the next breakpoint or the end instead.",
    schema: schemas.stepTraceSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const stack = await client.traceStep(args.projectId, args.type, {
        includeTree: args.includeTree ?? false,
        profileTop: args.profileTop,
      }).catch((error: unknown) => rethrowTraceStateError(error, "step"));

      // Bundle the active frame's variables so a `step out → inspect` cycle is
      // one call. Only meaningful while suspended with a frame on the stack.
      const active = stack.frames.find((f) => f.active) ?? stack.frames.at(-1);
      if (args.withValues && stack.status === "suspended" && active) {
        const variables = await client.getTraceFrameVariables(args.projectId, active.index, INSPECT_FIELDS);
        const values = trimmedVariables(withoutUnsetContext(variables)) as Types.DebugFrameVariables;
        return textResult(stackText(withStepValues(stack, active.index, values)));
      }

      return textResult(stackText(stack));
    },
  });

  registerTool({
    name: "resume_trace",
    title: "Resume to Next Stop",
    description:
      "Resume the suspended debug session and wait in this call for the next stop: a breakpoint, an exception or the end. The answer is the stack, as openl_start_trace answers it, with the 'error' of a failed run and the 'profile' at the end of a profiling run. A timeout answers the running status and how to keep waiting.",
    schema: schemas.resumeTraceSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const timeoutMs = args.timeoutMs ?? DEFAULT_RESUME_TIMEOUT_MS;

      // Attaching waits for the run an earlier call resumed: resuming again would run past a stop it reached since
      try {
        if (!args.attach) {
          await client.traceResume(args.projectId);
        }
      } catch (error) {
        // Re-attach path: a previous resume timed out and the worker is still
        // running — the 409 then only means "not suspended", so fall through
        // to the wait instead of failing.
        if (isConflictError(error)) {
          const { status } = await client.getTraceStatus(args.projectId);
          if (status !== "running" && status !== "pending") {
            rethrowTraceStateError(error, "resume");
          }
        } else {
          rethrowTraceStateError(error, "resume");
        }
      }

      let outcome: { stack: Types.DebugStackView } | { stillRunning: Types.DebugStatus };
      try {
        outcome = await waitForStop(client, args.projectId, timeoutMs, extra, {
          includeTree: args.includeTree ?? false,
          profileTop: args.profileTop,
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
        return textResult(stackText({ status: outcome.stillRunning, frames: [] },
          `Trace is still ${outcome.stillRunning} after waiting ${Math.round(timeoutMs / 1000)}s — the rule may be computing or looping. ` +
          "Call openl_resume_trace again with attach: true to keep waiting (optionally with a larger timeoutMs), or openl_stop_trace to terminate."));
      }

      return textResult(stackText(outcome.stack));
    },
  });

  registerTool({
    name: "inspect_trace_frame",
    title: "Inspect Stack Frame",
    description:
      "Read the state of one frame of the suspended debug session: parameters, runtime context, steps with their values, the result of a completed frame, and for a decision table 'decision', which rule fired and how each condition evaluated, and 'ruleNames', which per-rule breakpoints take. A table of more than 50 rules answers 'ruleCount' instead, unless full: true, and a table naming none of its rules answers 'ruleCount', its rules being R1 to R<ruleCount>. A large value comes lazy with a parameterId, which openl_get_trace_value reads.",
    schema: schemas.inspectTraceFrameSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      // The highlight overlay and the stack only need projectId/frameIndex, so
      // fire them alongside the variables fetch (which may itself do lazy-value
      // round-trips) instead of after it. (compact omits every frame's steps —
      // only the frame's tableId is needed.)
      const overlay = args.withHighlights
        ? Promise.all([client.getTraceFrameHighlights(args.projectId, args.frameIndex), client.getTraceStack(args.projectId)])
        : undefined;
      // The inspection may fail before the overlay is awaited: a failure of the overlay is then no unhandled rejection
      overlay?.catch(() => { /* reported where the overlay is awaited */ });

      const frameVariables = await client.getTraceFrameVariables(
        args.projectId,
        args.frameIndex,
        args.full ? undefined : INSPECT_FIELDS,
      ).catch((error: unknown) => rethrowTraceStateError(error, "frame inspection"));

      const variables = await filterSteps(
        withoutUnsetContext(frameVariables),
        { onlyExecuted: args.onlyExecutedSteps, excludeValues: args.excludeStepValues },
        client,
        args.projectId,
      );

      const trimmed = trimmedVariables(variables, args.full);
      let result: unknown = trimmed;
      if (overlay) {
        const [highlights, stack] = await overlay;
        // The overlay is keyed by A1 cell address; the grid to paint it on comes
        // from the shared Tables API raw view, resolved via the frame's tableId.
        const frame = stack.frames.find((f) => f.index === args.frameIndex);
        const grid = frame
          ? await client.getTable(args.projectId, frame.tableId)
          : undefined;
        result = { ...trimmed, highlights, grid };
      }

      return formatResult(result);
    },
  });

  registerTool({
    name: "set_trace_breakpoints",
    title: "Read / Replace Breakpoints",
    description:
      "Read or replace the breakpoints, with or without a debug session; a change applies from the next frame entered. Example: {\"projectId\":\"…\",\"breakpoints\":[\"AgeFactor#rule\",\"Premium#R48C0@3\"]}. A read answers the set and the targets by kind. Keys: '<name>' stops at the entry of every version of the table, '<name>#R{r}C{c}' at a spreadsheet cell, '<name>#rule' when any rule fires, '<name>#<ruleName>' when that rule fires; a uri in place of the name stops at that one version; '@N' stops only at the N-th execution of the table, from 0, a frame's '@N'.",
    schema: schemas.setTraceBreakpointsSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      if (args.breakpoints) {
        // A change answers the set alone; a read answers the targets too
        await client.setTraceBreakpoints(args.projectId, args.breakpoints);
        return formatResult({ breakpoints: await client.getTraceBreakpoints(args.projectId) });
      }

      const [breakpoints, targets] = await Promise.all([
        client.getTraceBreakpoints(args.projectId),
        client.getTraceBreakpointTables(args.projectId),
      ]);

      // A project of hundreds of tables repeats the kind on each: the names are listed under their kind
      const wanted = args.name?.toLowerCase();
      const named = wanted === undefined ? targets : targets.filter((target) => target.name.toLowerCase().includes(wanted));
      const byKind = Map.groupBy(named, (target) => target.kind);
      return formatResult({
        breakpoints,
        targets: Object.fromEntries(byKind.entries().map(([kind, tables]) => [kind, tables.map((table) => table.name)])),
      });
    },
  });

  registerTool({
    name: "get_trace_value",
    title: "Expand Lazy Value",
    description:
      "Read the whole of a lazy trace value (lazy: true) while the debug session lives: its name, description and value.",
    schema: schemas.getTraceValueSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const param = await client.getTraceParameter(
        args.projectId,
        args.parameterId,
        args.withSchema ? undefined : "name,description,value",
      ).catch((error: unknown) => rethrowTraceStateError(error, "lazy value fetch"));

      return formatResult(param);
    },
  });

  registerTool({
    name: "expand_trace_tree",
    title: "Expand Call-Tree Node",
    description:
      "Load one level of the call tree of a profiling run, which lives as long as the debug session: the sub-calls one step of a node made, each a node with its uri, instance and steps, and each step counting its own sub-calls in 'childrenTotal'. Start from the call-tree root line of a stack read with includeTree: true. The answer { children, total, hasMore, nextOffset } pages the sub-calls of a loop; 'notRetained' counts the sub-calls dropped at the size limit of the tree. To find what is slow, read the 'profile' instead.",
    schema: schemas.expandTraceTreeSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const view = await client.getTraceTreeChildren(args.projectId, args).catch(rethrowTraceTreeError);

      // The backend omits `children` for a childless step (reporting total 0).
      // Surface pagination so the agent can page a loop's children (a step can
      // make thousands) without redoing the offset math.
      const children = view.children ?? [];
      const total = view.total;
      const offset = args.offset ?? 0;
      // Stop as soon as a page returns nothing: `total` counts a step's FULL
      // sub-calls, INCLUDING ones dropped at the tree's size cap that can never be
      // paged to, so keying `hasMore` on `total` alone would loop forever with a
      // frozen nextOffset. An empty page means there is no next page.
      const hasMore = children.length > 0 && offset + children.length < total;
      const payload = hasMore
        ? { children, total, hasMore, nextOffset: offset + children.length }
        : { children, total, hasMore };
      // Studio answers a step it does not hold as one that made no sub-calls
      return formatResult(total === 0
        ? { ...payload, note: `Step '${args.step}' of this node made no sub-calls, or the node has no such step: expand a step whose childrenTotal is above 0.` }
        : payload);
    },
  });

  registerTool({
    name: "stop_trace",
    title: "Terminate Debug Session",
    description:
      "End the debug session, which frees its lazy values; it succeeds without a session too. The breakpoints stay set.",
    schema: schemas.projectOnlySchema,
    annotations: {
      destructiveHint: false,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.stopTrace(args.projectId);

      return formatResult({ stopped: true });
    },
  });

  registerTool({
    name: "watch_trace_cells",
    compiled: true,
    title: "Watch Cells Across a Run",
    description:
      "Run a table to the end and answer one 'series' per watched cell: a point for each execution of its table, with its instance, ref, path and value; a large value comes lazy, for openl_get_trace_value. The call starts a new debug session and clears the breakpoints. Watch scalar cells: every point of an aggregate, such as a whole spreadsheet result, carries all of it; break on it instead. A series lists its first 100 points. To see why a point stands out, replay with the breakpoint '<point.ref>@<point.instance>'.",
    schema: schemas.watchTraceCellsSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      // Clear any breakpoints left set earlier in this session — they are
      // session-scoped and would suspend the run mid-way, so the watch would
      // capture only a partial series. The watch needs a run to completion.
      await client.setTraceBreakpoints(args.projectId, []);

      // Set the watch cells, run the whole table to completion, then read the
      // collected series. The run fully materializes the traced method's result,
      // so cells inside lazy result branches (nested SpreadsheetResult[]) are
      // evaluated and captured — no profiling needed (profiling would only make
      // the studio build and retain a large call tree this tool discards). The
      // server caps points per series, so the response is bounded.
      await client.setTraceWatches(args.projectId, args.cells);
      const stack = await client.startTrace({ ...args, stopAtEntry: false });

      const watch = await client.getTraceWatch(
        args.projectId,
        args.withSchema ? undefined : WATCH_FIELDS,
      ).catch((error: unknown) => rethrowTraceStateError(error, "watch read"));
      // A watch set stays set for the session: a later trace would capture the cells again, up to the capture limit
      await client.setTraceWatches(args.projectId, []).catch(() => undefined);

      // Studio cuts each series to its first points and says so only by its total, and it stops capturing at a
      // limit for the whole run, after which the totals count what was captured: both are told
      // A point names its value as the series does, and counts its executions by its 0-based instance alone
      const series = watch.series.map((one) => ({
        ...one,
        points: one.points.map(({ label, value, ...point }) => (value ? { ...point, value: trimmedValue(value, one.name) } : point)),
        ...(one.points.length < (one.total ?? 0) ? { truncated: true } : {}),
      }));
      const notes = [
        ...(series.some((one) => one.truncated) ? [
          "A series marked truncated lists its first points only, its 'total' counting them all: reach a later " +
            "execution with a '<ref>@N' breakpoint.",
        ] : []),
        ...(watch.truncated ? [
          "The run reached its capture limit, so a 'total' counts only the executions captured before it: watch " +
            "fewer cells, or a cell of a higher level.",
        ] : []),
        ...unseenCellsNote(args.cells, watch, stack.status === "completed" && !watch.truncated),
        ...(stack.status === "completed" ? [] : [
          `The run stopped before its end, ${stack.status}: the series hold the executions before the stop.`,
        ]),
      ];
      const stop = stack.status === "completed" ? {} : { status: stack.status, ...(stack.error ? { error: stack.error } : {}) };
      const payload = notes.length > 0 ? { note: notes.join(" "), ...stop, ...watch, series } : { ...watch, series };

      return formatResult(payload);
    },
  });
}

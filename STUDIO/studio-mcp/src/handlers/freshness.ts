/**
 * Keeping what a tool works with current with the files of the project.
 *
 * Studio answers from what it compiled. Another program, such as Excel or an IDE, may change the files of the project
 * since: edit a workbook, add one or take one away, or change rules.xml. Studio tells it in the status of the project
 * (`changedOnDisk`) but compiles nothing again by itself. A tool that works with what Studio compiled has the project
 * compiled again from its files first, so that the agent never reads what the files held before.
 */
import { setTimeout as sleep } from "node:timers/promises";

import type { OpenLClient } from "../client.js";

/** How long a refresh waits for Studio to take the files of the project again. */
const REFRESH_WAIT_MS = 60_000;

const REFRESH_POLL_MS = 250;
const REFRESH_POLL_MAX_MS = 2_000;

/**
 * Compiles the project again from its files when another program changed them since Studio compiled it.
 *
 * Asks the short status of the project, which is cheap. When it tells a change, compiles the project again and waits
 * until the compilation ends: what is read next is compiled from the files as they are, the modules a workbook added
 * included.
 *
 * @returns whether the project was compiled again
 */
export async function refreshWhenChangedOnDisk(
  client: OpenLClient,
  projectId: string,
  signal?: AbortSignal,
): Promise<boolean> {
  return (await client.getProjectStatusSummary(projectId, signal)).changedOnDisk === true
    && refreshFromDisk(client, projectId, signal);
}

/**
 * Compiles the project again after its files changed on disk, and waits until the compilation ends, or for as long as
 * it is given.
 *
 * @returns whether the project was compiled again; false for a project with no module to compile
 */
export async function refreshFromDisk(
  client: OpenLClient,
  projectId: string,
  signal?: AbortSignal,
  waitMs = REFRESH_WAIT_MS,
): Promise<boolean> {
  const module = (await client.listProjectModules(projectId)).find((declared) => declared.name)?.name;
  if (!module) {
    return false;
  }
  await client.recompileProject(projectId, module);
  await untilCompiled(client, projectId, Date.now() + waitMs, signal);
  return true;
}

/**
 * Waits, one pause and one status read at a time, until Studio compiled the project from its files again or the
 * deadline passed.
 *
 * Studio lets go of what it compiled as soon as the compilation starts, so a status that tells no change is not enough:
 * the tables of the other modules of the project come with the end of the compilation.
 */
export async function untilCompiled(
  client: OpenLClient,
  projectId: string,
  deadline: number,
  signal?: AbortSignal,
): Promise<void> {
  // A pause twice as long as the previous one, up to a ceiling: a long compilation is read a few times a minute
  for (let pauseMs = REFRESH_POLL_MS; Date.now() < deadline; pauseMs = Math.min(pauseMs * 2, REFRESH_POLL_MAX_MS)) {
    // An unreferenced timer does not keep the process alive; a cancelled call rejects with an AbortError
    await sleep(Math.min(pauseMs, deadline - Date.now()), undefined, { signal, ref: false });
    const status = await client.getProjectStatusSummary(projectId, signal);
    if (status.changedOnDisk !== true && status.compileState !== "compiling") {
      return;
    }
  }
}

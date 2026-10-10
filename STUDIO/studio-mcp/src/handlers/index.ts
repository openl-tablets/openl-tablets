/**
 * Public entry point for the OpenL tool registry.
 *
 * The registry core (`registerTool` / `getAllTools` / `executeTool`) lives in
 * `common.ts`; each per-category handler module in this directory registers its
 * tools through it. `registerAllTools` assembles them. Both transports
 * ({@link file://../mcp-core.ts}) and the CLI ({@link file://../cli.ts}) import
 * the registry surface from here, so the per-category split stays an internal
 * detail of this directory.
 */

import { registerGuideHandlers } from "./guide-handlers.js";
import { registerDiagnosticsHandlers } from "./diagnostics-handlers.js";
import { registerDeploymentHandlers } from "./deployment-handlers.js";
import { registerRepositoryHandlers } from "./repository-handlers.js";
import { registerTestingHandlers } from "./testing-handlers.js";
import { registerLocalChangeHandlers } from "./local-change-handlers.js";
import { registerFileHandlers } from "./file-handlers.js";
import { registerProjectHandlers } from "./project-handlers.js";
import { registerProjectMergeHandlers } from "./project-merge-handlers.js";
import { registerTraceHandlers } from "./trace-handlers.js";
import { registerTableHandlers } from "./table-handlers.js";
import { registerTableActionHandlers } from "./table-action-handlers.js";
import { registerTableWorkflowHandlers } from "./table-workflow-handlers.js";

export { getAllTools, executeTool, hasTool } from "./common.js";

/**
 * Register every OpenL Studio tool into the shared registry by delegating to the
 * per-category handler modules. Tools receive their client at call time via
 * `executeTool`, so registration needs no server or client. Safe to call
 * repeatedly — each module just re-sets its entries in the registry map.
 */
export function registerAllTools(): void {
  registerGuideHandlers();
  registerDiagnosticsHandlers();
  registerDeploymentHandlers();
  registerRepositoryHandlers();
  registerTestingHandlers();
  registerLocalChangeHandlers();
  registerFileHandlers();
  registerProjectHandlers();
  registerProjectMergeHandlers();
  registerTraceHandlers();
  registerTableHandlers();
  registerTableActionHandlers();
  registerTableWorkflowHandlers();
}

/**
 * Diagnostics tool handlers — which OpenL Studio the server works with: its version and the address its users open,
 * and on request its build and the machine it runs on.
 */
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { registerTool, textResult, type ToolResponse } from "./common.js";

const UNKNOWN = "unknown";

/** A size in bytes in the largest binary unit that keeps it at least 1: `16106127360` → `15.0 GiB`. */
function formatBytes(bytes: number | null | undefined): string {
  if (typeof bytes !== "number" || bytes < 0) {
    return UNKNOWN;
  }
  const units = ["B", "KiB", "MiB", "GiB", "TiB"];
  const exponent = Math.min(bytes > 0 ? Math.floor(Math.log(bytes) / Math.log(1024)) : 0, units.length - 1);
  return exponent === 0 ? `${bytes} B` : `${(bytes / 1024 ** exponent).toFixed(1)} ${units[exponent]}`;
}

/** A load from 0 to 1 as a percentage; a negative load means the JVM cannot tell it. */
function formatLoad(load: number | null | undefined): string {
  return typeof load === "number" && load >= 0 ? `${(load * 100).toFixed(1)}%` : UNKNOWN;
}

/**
 * The Markdown report of {@link registerDiagnosticsHandlers}: the version and the address of OpenL Studio, then, when
 * the machine is given, the build, the times, the system, Java, CPUs, threads and memory.
 */
function renderServerInfo(url: string, build: Types.StudioBuildInfo, sys?: Types.StudioSysInfo): string {
  const lines = [`Server: OpenL Studio ${build["openl.version"] ?? UNKNOWN}`, `URL: ${url}`];
  if (sys) {
    lines.push(
      `Build#: ${build["openl.build.number"] ?? UNKNOWN}`,
      `Start Time: ${build["openl.start.time"] ?? UNKNOWN}`,
      `Current Time: ${sys["time.now"] ?? UNKNOWN}`,
      `OS: ${sys["os.name"] ?? UNKNOWN} ${sys["os.version"] ?? ""}`.trimEnd(),
      `Arch: ${sys["os.arch"] ?? UNKNOWN}`,
      `Java: ${sys["vm.name"] ?? UNKNOWN} ${sys["vm.version"] ?? ""} (${sys["vm.vendor"] ?? UNKNOWN})`,
      `CPU: ${sys.cpu ?? UNKNOWN} (load: ${formatLoad(sys["os.SystemCpuLoad"])})`,
      `Threads: ${sys["thread.count"] ?? UNKNOWN} (daemon: ${sys["thread.daemon"] ?? UNKNOWN})`,
      "RAM:",
      "",
      `* total: ${formatBytes(sys["os.TotalPhysicalMemorySize"])}`,
      `* free: ${formatBytes(sys["os.FreePhysicalMemorySize"])}`,
      "",
      "JVM Memory:",
      "",
      `* max: ${formatBytes(sys.maxMemory)}`,
      `* total: ${formatBytes(sys.totalMemory)}`,
      `* free: ${formatBytes(sys.freeMemory)}`,
    );
  }
  return lines.join("\n");
}

export function registerDiagnosticsHandlers(): void {
  registerTool({
    name: "get_server_info",
    title: "Get Server Info",
    description:
      "Report the OpenL Studio this server works with, as Markdown: its version and the address its users open it at. With full: true, also its build number, start and current time, operating system, architecture, Java, CPUs and their load, threads, and the memory of the machine and of the JVM. Quote it in bug reports. The 'path' of a document from openl_list_guides is relative to this address, so a person can open the document in a browser. Read-only.",
    schema: schemas.getServerInfoSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const [build, sys] = await Promise.all([
        client.getBuildInfo(),
        args.full ? client.getSysInfo() : undefined,
      ]);
      return textResult(renderServerInfo(client.publicUrl, build, sys));
    },
  });
}

/**
 * Integration tests of the diagnostics tool of src/handlers/diagnostics-handlers.ts: the version, the build and the
 * machine of OpenL Studio.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

describe("diagnostics tools", () => {
  const build = { "openl.version": "7.0.0", "openl.build.number": "228587f7d364", "openl.start.time": "2026-10-14T07:19:38Z[GMT]" };

  it("openl_get_server_info names the version and the address of OpenL Studio", async () => {
    harness.mockHttp.onGet("/public/info/build.json").reply(200, build);

    const text = textOf(await executeTool("get_server_info", {}, harness.client));

    expect(text).toBe("Server: OpenL Studio 7.0.0\nURL: http://localhost:8080/");
    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual(["/public/info/build.json"]);
  });

  it("openl_get_server_info adds the build and the machine OpenL Studio runs on when asked for everything", async () => {
    harness.mockHttp.onGet("/public/info/build.json").reply(200, build);
    harness.mockHttp.onGet("/public/info/sys.json").reply(200, {
      "time.now": "2026-10-18T01:12:45Z[GMT]",
      "os.name": "Linux",
      "os.version": "5.14.0-570.52.1.el9_6.x86_64",
      "os.arch": "amd64",
      "vm.name": "OpenJDK 64-Bit Server VM",
      "vm.version": "25.0.1+8",
      "vm.vendor": "Eclipse Adoptium",
      cpu: 8,
      "os.SystemCpuLoad": 0.1234,
      "thread.count": 77,
      "thread.daemon": 31,
      "os.TotalPhysicalMemorySize": 16 * 1024 ** 3,
      "os.FreePhysicalMemorySize": null,
      maxMemory: 4 * 1024 ** 3,
      totalMemory: 1536 * 1024 ** 2,
      freeMemory: 512,
    });

    const text = textOf(await executeTool("get_server_info", { full: true }, harness.client));

    expect(text).toBe([
      "Server: OpenL Studio 7.0.0",
      "URL: http://localhost:8080/",
      "Build#: 228587f7d364",
      "Start Time: 2026-10-14T07:19:38Z[GMT]",
      "Current Time: 2026-10-18T01:12:45Z[GMT]",
      "OS: Linux 5.14.0-570.52.1.el9_6.x86_64",
      "Arch: amd64",
      "Java: OpenJDK 64-Bit Server VM 25.0.1+8 (Eclipse Adoptium)",
      "CPU: 8 (load: 12.3%)",
      "Threads: 77 (daemon: 31)",
      "RAM:",
      "",
      "* total: 16.0 GiB",
      "* free: unknown",
      "",
      "JVM Memory:",
      "",
      "* max: 4.0 GiB",
      "* total: 1.5 GiB",
      "* free: 512 B",
    ].join("\n"));
  });

  it("openl_get_server_info reports what OpenL Studio does not tell as unknown", async () => {
    harness.mockHttp.onGet("/public/info/build.json").reply(200, {});
    harness.mockHttp.onGet("/public/info/sys.json").reply(200, { "os.SystemCpuLoad": -1 });

    const text = textOf(await executeTool("get_server_info", { full: true }, harness.client));

    expect(text).toContain("Server: OpenL Studio unknown");
    expect(text).toContain("OS: unknown\n");
    expect(text).toContain("CPU: unknown (load: unknown)");
  });
});

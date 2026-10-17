import { afterEach, beforeAll, beforeEach, describe, expect, it } from "@jest/globals";
import MockAdapter from "axios-mock-adapter";

import { OpenLClient } from "../../src/client.js";
import { executeTool, registerAllTools } from "../../src/handlers/index.js";

function jsonResult<T>(text: string): T {
  const envelope = JSON.parse(text) as { data: T };
  return envelope.data;
}

describe("local change handlers", () => {
  let client: OpenLClient;
  let mockAxios: MockAdapter;

  beforeAll(() => {
    registerAllTools();
  });

  beforeEach(() => {
    client = new OpenLClient({ baseUrl: "http://localhost:8080" });
    // @ts-expect-error Test-only access to the client's HTTP adapter.
    mockAxios = new MockAdapter(client.axiosInstance);
  });

  afterEach(() => {
    mockAxios.restore();
  });

  it("lists history for the requested project module", async () => {
    const items = [
      { id: "history-1", modifiedOn: "08/27/2026 12:00 PM", current: true },
      { id: "Revision Version", modifiedOn: "Revision Version" },
    ];
    mockAxios.onGet("/projects/design%3AProject/local-history", {
      params: { module: "Main Rules" },
    }).reply(200, items);

    const response = await executeTool("list_project_local_changes", {
      projectId: "design:Project",
      moduleName: "Main Rules",
      response_format: "json",
    }, client);

    expect(jsonResult(response.content[0].text)).toEqual(items);
  });

  it("restores the requested project module and identifies it in the result", async () => {
    mockAxios.onPost("/projects/design%3AProject/local-history/restore", {
      version: "Revision Version",
    }, {
      params: { module: "Main Rules" },
    }).reply(204);

    const response = await executeTool("restore_project_local_change", {
      projectId: "design:Project",
      moduleName: "Main Rules",
      historyId: "Revision Version",
      response_format: "json",
    }, client);

    expect(jsonResult(response.content[0].text)).toEqual({
      success: true,
      message: "Successfully restored project module to local history version 'Revision Version'",
      projectId: "design:Project",
      moduleName: "Main Rules",
      historyId: "Revision Version",
    });
  });

  it("requires an explicit project and module for both local-history operations", async () => {
    await expect(executeTool("list_project_local_changes", {
      projectId: "design:Project",
    }, client)).rejects.toThrow(/moduleName/);
    await expect(executeTool("restore_project_local_change", {
      moduleName: "Main Rules",
      historyId: "Revision Version",
    }, client)).rejects.toThrow(/projectId/);

    expect(mockAxios.history.get).toHaveLength(0);
    expect(mockAxios.history.post).toHaveLength(0);
  });
});

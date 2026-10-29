/**
 * Integration tests of the deployment tools of src/handlers/deployment-handlers.ts: the deployment repositories, the
 * deployments, and a deploy or redeploy of a project.
 */

import { describe, it, expect } from "vitest";
import { executeTool } from "./index.js";
import { mockDeployments } from "../testing/openl-api-mocks.js";
import { bodyOf } from "../testing/mock-http.js";
import { dataOf, harness, textOf, useToolHarness } from "../testing/harness.js";

useToolHarness();

const deployRepositories = [
  { id: "production-deploy", name: "Production Deployment" },
  { id: "staging-deploy", name: "Staging Deployment" },
];

describe("deployment tools", () => {
  it("openl_list_deploy_repositories reads the deployment repositories, not the design ones, on every call", async () => {
    harness.mockHttp.onGet("/production-repos").reply(200, deployRepositories);

    const first = await executeTool("list_deploy_repositories", {}, harness.client);
    await executeTool("list_deploy_repositories", {}, harness.client);

    expect(dataOf(first)).toEqual(deployRepositories);
    expect(harness.mockHttp.history.get.map((request) => request.url)).toEqual(["/production-repos", "/production-repos"]);
  });

  it("openl_list_deployments lists the deployments by the filters given", async () => {
    harness.mockHttp.onGet("/deployments", { params: { repository: "production", project: "insurance-rules" } })
      .reply(200, mockDeployments);

    const result = await executeTool("list_deployments", {
      repository: "production",
      project: "insurance-rules",
      response_format: "markdown",
    }, harness.client);

    expect(textOf(result)).toContain("# Deployments");
    expect(textOf(result)).toContain("insurance-rules-v1.2.3");
  });

  it("openl_deploy_project deploys to the repository it names by its display name", async () => {
    harness.mockHttp.onGet("/production-repos").reply(200, deployRepositories);
    harness.mockHttp.onPost("/deployments").reply(204);

    const result = await executeTool("deploy_project", {
      projectId: "design-project1",
      deploymentName: "project1",
      productionRepositoryId: "Production Deployment",
      comment: "Deploy test",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({
      projectId: "design-project1",
      deploymentName: "project1",
      productionRepositoryId: "production-deploy",
      comment: "Deploy test",
    });
    expect(dataOf(result)).toEqual({ deployed: "project1", productionRepositoryId: "production-deploy" });
  });

  it.each(["production-deploy", "PRODUCTION DEPLOYMENT"])(
    "openl_deploy_project deploys to the repository %j names by its id or name, in any case", async (repository) => {
      harness.mockHttp.onGet("/production-repos").reply(200, deployRepositories);
      harness.mockHttp.onPost("/deployments").reply(204);

      await executeTool("deploy_project", {
        projectId: "design-project1", deploymentName: "project1", productionRepositoryId: repository,
      }, harness.client);

      expect(bodyOf(harness.mockHttp.history.post[0])).toMatchObject({ productionRepositoryId: "production-deploy" });
    });

  it("openl_deploy_project names the available repositories when the name is unknown", async () => {
    harness.mockHttp.onGet("/production-repos").reply(200, deployRepositories);

    await expect(executeTool("deploy_project", {
      projectId: "design-project1",
      deploymentName: "project1",
      productionRepositoryId: "Nope",
    }, harness.client)).rejects.toThrow(/"Nope" not found.*\(Production Deployment\), .*\(Staging Deployment\)/);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });

  it("openl_deploy_project deploys the project again to the deployment deploymentId names", async () => {
    harness.mockHttp.onPost("/deployments/deploy%20001").reply(204);

    const result = await executeTool("deploy_project", {
      deploymentId: "deploy 001",
      projectId: "design-project1",
      comment: "Redeploy latest",
    }, harness.client);

    expect(bodyOf(harness.mockHttp.history.post[0])).toEqual({ projectId: "design-project1", comment: "Redeploy latest" });
    expect(dataOf(result)).toEqual({ redeployed: "deploy 001" });
  });

  it.each([
    [{ deploymentId: "d1", deploymentName: "project1" }, /takes no deploymentName/],
    [{ deploymentName: "project1" }, /needs deploymentName and productionRepositoryId/],
    [{}, /needs deploymentName and productionRepositoryId/],
  ])("openl_deploy_project refuses %j before any request", async (args, message) => {
    await expect(executeTool("deploy_project", { projectId: "design-project1", ...args }, harness.client))
      .rejects.toThrow(message);
    expect(harness.mockHttp.history.post).toHaveLength(0);
  });
});

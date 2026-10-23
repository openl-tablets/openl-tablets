/**
 * Tests for the skills the server serves (src/skills.ts), over an in-memory
 * transport as a client of the MCP skills extension reads them: the capability,
 * `skills/list`, `skills/get`, and the files as resources whose digests and sizes
 * match the entries.
 */

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { createHash } from "node:crypto";
import { InMemoryTransport, type McpServer } from "@modelcontextprotocol/server";
import { Client } from "@modelcontextprotocol/client";
import { z } from "zod";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { SKILLS_EXTENSION, SKILLS_OVERVIEW, type SkillEntry } from "./skills.js";
import { mcpToolName } from "./constants.js";
import { getAllTools } from "./handlers/index.js";
import catalog from "virtual:skills";

const ListResult = z.looseObject({ skills: z.array(z.custom<SkillEntry>()) });
const GetResult = z.looseObject({ skill: z.custom<SkillEntry>() });

describe("skills", () => {
  let server: McpServer;
  let client: Client;

  beforeEach(async () => {
    server = createConfiguredServer(new OpenLClient({ baseUrl: "http://localhost:8080" }));
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    client = new Client({ name: "test-client", version: "0.0.0" });
    await client.connect(clientTransport);
  });

  afterEach(async () => {
    await client.close();
    await server.close();
  });

  const listSkills = async () => (await client.request({ method: "skills/list", params: {} }, ListResult)).skills;

  it("declares the extension and fixed resources in the capabilities", () => {
    expect(client.getServerCapabilities()).toMatchObject({
      resources: { listChanged: false },
      extensions: { [SKILLS_EXTENSION]: {} },
    });
  });

  it("lists every skill with the files its resources serve, digests and sizes matching the bytes read", async () => {
    const skills = await listSkills();

    expect(skills.map((skill) => skill.uri)).toContain("skill://planning/SKILL.md");
    for (const skill of skills) {
      const name = skill.uri.split("/").at(-2);
      expect(skill.frontmatter.name).toBe(name);
      expect(skill.resources[0].uri).toBe(skill.uri);
      for (const { uri, digest, size } of skill.resources) {
        expect(uri.startsWith(`skill://${name}/`)).toBe(true);
        const [content] = (await client.readResource({ uri })).contents;
        const bytes = Buffer.from((content as { text: string }).text, "utf-8");
        expect(bytes).toHaveLength(size);
        expect(digest).toBe(`sha256:${createHash("sha256").update(bytes).digest("hex")}`);
      }
    }
  });

  it("names and describes a SKILL.md resource by its frontmatter, and types the other files", async () => {
    const [planning] = (await listSkills()).filter((skill) => skill.frontmatter.name === "planning");
    const { resources } = await client.listResources();

    expect(resources.find((resource) => resource.uri === "skill://planning/SKILL.md")).toEqual({
      uri: "skill://planning/SKILL.md",
      name: "planning",
      description: planning.frontmatter.description,
      mimeType: "text/markdown",
    });
    expect(resources.find((resource) => resource.uri.endsWith(".schema.json"))).toMatchObject({
      name: "change-log-and-reapply/assets/openl-change-set.schema.json",
      mimeType: "application/json",
    });
  });

  it("gets the entry of a skill by the URI of its SKILL.md", async () => {
    const [testing] = (await listSkills()).filter((skill) => skill.frontmatter.name === "testing");

    const { skill } = await client.request(
      { method: "skills/get", params: { uri: "skill://testing/SKILL.md" } },
      GetResult,
    );

    expect(skill).toEqual(testing);
  });

  it.each(["skill://unknown/SKILL.md", "skill://trace-investigation/references/root-cause-catalog.md"])(
    "refuses to get %s, which is no skill, as invalid params",
    async (uri) => {
      await expect(client.request({ method: "skills/get", params: { uri } }, GetResult)).rejects.toMatchObject({
        code: -32602,
      });
    },
  );

  it("names in the skills only tools the server has", () => {
    const tools = new Set(getAllTools().map((tool) => mcpToolName(tool.name)));
    const unknown = catalog.flatMap((skill) =>
      skill.files.flatMap((file) =>
        [...file.text.matchAll(/\bopenl_[a-z_]+/g)]
          .map(([name]) => name)
          .filter((name) => !tools.has(name))
          .map((name) => `${skill.path}/${file.path}: ${name}`),
      ),
    );

    expect(unknown).toEqual([]);
  });

  it("points the agent to openl_get_started and the skills in the server instructions", () => {
    const instructions = client.getInstructions() ?? "";

    expect(instructions).toMatch(/^This server connects you to OpenL Studio[\s\S]*Call `openl_get_started` once/);
    expect(instructions.endsWith(SKILLS_OVERVIEW)).toBe(true);
  });

  it("tells the agent of every skill, its URI and its description on one line", async () => {
    const overview = SKILLS_OVERVIEW;

    for (const { uri, frontmatter } of await listSkills()) {
      const description = String(frontmatter.description).replaceAll(/\s+/g, " ").trim();
      expect(`${overview}\n`).toContain(`- \`${String(frontmatter.name)}\` (\`${uri}\`) — ${description}\n`);
    }
  });
});

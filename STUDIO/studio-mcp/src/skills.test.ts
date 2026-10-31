/**
 * Tests for the skills the server serves (src/skills.ts), over an in-memory
 * transport as a client of the MCP skills extension reads them: the capability,
 * every file of `skills/` verified against its entry, and the overview agents
 * without the extension read.
 */

import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { InMemoryTransport, type McpServer } from "@modelcontextprotocol/server";
import { Client } from "@modelcontextprotocol/client";
import { SKILLS_EXTENSION_ID, SkillsClient } from "@modelcontextprotocol/client/ext/skills";
import { OpenLClient } from "./client.js";
import { createConfiguredServer } from "./mcp-core.js";
import { SKILLS, SKILLS_OVERVIEW } from "./skills.js";
import { mcpToolName } from "./constants.js";
import { getAllTools } from "./handlers/index.js";
import files from "virtual:skills";

describe("skills", () => {
  let server: McpServer;
  let client: Client;
  let skills: SkillsClient;

  beforeEach(async () => {
    server = createConfiguredServer(new OpenLClient({ baseUrl: "http://localhost:8080" }));
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
    await server.connect(serverTransport);
    client = new Client({ name: "test-client", version: "0.0.0" });
    await client.connect(clientTransport);
    skills = new SkillsClient(client);
  });

  afterEach(async () => {
    await client.close();
    await server.close();
  });

  it("declares the extension and fixed resources in the capabilities", () => {
    expect(client.getServerCapabilities()).toMatchObject({
      resources: { listChanged: false },
      extensions: { [SKILLS_EXTENSION_ID]: { directoryRead: true } },
    });
  });

  it("serves every file of skills/ with the text the bundle carries, verified against its entry", async () => {
    const served: Record<string, string | undefined> = {};
    for (const skill of (await skills.list()).skills) {
      for (const { uri } of skill.resources === "dynamic" ? [] : skill.resources) {
        const file = await skills.read(skill, uri);
        expect(file.verified).toBe(true);
        served[uri] = file.text;
      }
    }

    expect(Object.keys(served)).toContain("skill://planning/SKILL.md");
    expect(served).toEqual(Object.fromEntries(Object.entries(files).map(([path, text]) => [`skill://${path}`, text])));
  });

  it("names in the skills only tools the server has", () => {
    const tools = new Set(getAllTools().map((tool) => mcpToolName(tool.name)));
    const unknown = Object.entries(files).flatMap(([path, text]) =>
      [...text.matchAll(/\bopenl_[a-z_]+/g)]
        .map(([name]) => name)
        .filter((name) => !tools.has(name))
        .map((name) => `${path}: ${name}`),
    );

    expect(unknown).toEqual([]);
  });

  it("tells the agent how to work and which skills to read in the server instructions, with every tool it names", () => {
    const instructions = client.getInstructions() ?? "";
    const tools = new Set(getAllTools().map((tool) => mcpToolName(tool.name)));

    expect(instructions).toMatch(/^This server connects you to OpenL Studio[\s\S]*## How to work/);
    expect(instructions.endsWith(SKILLS_OVERVIEW)).toBe(true);
    expect([...instructions.matchAll(/\bopenl_[a-z_]+/g)].map(([name]) => name).filter((name) => !tools.has(name)))
      .toEqual([]);
  });

  it("tells the agent of every skill, its URI and its description on one line", () => {
    const lines = SKILLS_OVERVIEW.split("\n").filter((line) => line.startsWith("- "));

    expect(lines).toHaveLength(SKILLS.entries.length);
    for (const { uri, frontmatter: { name, description } } of SKILLS.entries) {
      const words = description.split(/\s+/).filter(Boolean).join(" ");
      expect(lines.filter((line) => line.includes(`(\`${uri}\`)`))).toEqual([
        expect.stringMatching(new RegExp(`^- \`${name}\` .*${RegExp.escape(words)}$`)),
      ]);
    }
  });
});

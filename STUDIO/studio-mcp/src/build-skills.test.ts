/**
 * Unit tests for the embedding of the skills in src/build-skills.ts: which files
 * of the folder it embeds, which skills fail the build, and the module the
 * bundler plugin answers. Skills are written to a temp directory in the layout of
 * `skills/`.
 */

import { describe, it, expect } from "vitest";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";

import { readSkills, skills } from "./build-skills.js";

const SKILL_MD = "---\nname: rating\ndescription: >\n  Rates a policy\n  by its factors.\n---\n\n# Rating\n";

/** Write `files` of a skill folder `name` under `root`. */
function writeSkill(root: string, name: string, files: Record<string, string | Buffer>): void {
  for (const [path, content] of Object.entries(files)) {
    const file = join(root, name, path);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, content);
  }
}

/** Run `body` with a temp directory, removed afterwards. */
async function withRoot(body: (root: string) => Promise<void>): Promise<void> {
  const root = mkdtempSync(join(tmpdir(), "openl-skills-"));
  try {
    await body(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

describe("readSkills", () => {
  it("reads every file of a skill as text with its byte order mark, hidden and loose files left out", async () => {
    await withRoot(async (root) => {
      writeSkill(root, "rating", { "SKILL.md": SKILL_MD, "references/factors.md": "\uFEFF# Factors\n" });
      writeSkill(root, "audit", { "SKILL.md": "---\nname: audit\ndescription: Audits.\n---\n", ".DS_Store": "x" });
      writeFileSync(join(root, "README.md"), "not a skill");
      mkdirSync(join(root, ".idea"));

      expect(await readSkills(root)).toEqual({
        "audit/SKILL.md": "---\nname: audit\ndescription: Audits.\n---\n",
        "rating/SKILL.md": SKILL_MD,
        "rating/references/factors.md": "\uFEFF# Factors\n",
      });
    });
  });

  it.each([
    ["without a description", { "SKILL.md": "---\nname: rating\n---\n" }, "description"],
    ["named other than its folder", { "SKILL.md": "---\nname: rates\ndescription: R.\n---\n" }, "\"rates\""],
    [
      "with a binary file",
      { "SKILL.md": SKILL_MD, "logo.png": Buffer.from([0xff, 0xfe, 0x00]) },
      "logo.png: cannot be read: not UTF-8 text",
    ],
  ])("fails a skill %s", async (_, files, message) => {
    await withRoot(async (root) => {
      writeSkill(root, "rating", files);

      await expect(readSkills(root)).rejects.toThrow(message);
    });
  });
});

describe("skills plugin", () => {
  it("answers virtual:skills with the skills of its folder and leaves other modules alone", async () => {
    await withRoot(async (root) => {
      writeSkill(root, "rating", { "SKILL.md": SKILL_MD });
      const plugin = skills(root) as unknown as {
        resolveId: (id: string) => string | null;
        load: (id: string) => Promise<string | null>;
      };

      const id = plugin.resolveId("virtual:skills");
      expect(plugin.resolveId("./client.js")).toBeNull();
      expect(await plugin.load("./client.js")).toBeNull();
      expect(id).not.toBeNull();
      expect(await plugin.load(id!)).toBe(`export default ${JSON.stringify({ "rating/SKILL.md": SKILL_MD })};`);
    });
  });
});

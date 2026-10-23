/**
 * Unit tests for the embedding of the skills in src/build-skills.ts: what it reads
 * of a skill, which skills fail the build, and the module the bundler plugin
 * answers. Skills are written to a temp directory in the layout of `skills/`.
 */

import { describe, it, expect } from "vitest";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";

import { readSkills, skills } from "./build-skills.js";

const SKILL_MD = "---\nname: rating\ndescription: >\n  Rates a policy\n  by its factors.\nmetadata:\n  version: \"1.0\"\n---\n\n# Rating\n";

/** Write `files` of a skill folder `name` under `root`. */
function writeSkill(root: string, name: string, files: Record<string, string | Buffer>): void {
  for (const [path, content] of Object.entries(files)) {
    const file = join(root, name, path);
    mkdirSync(dirname(file), { recursive: true });
    writeFileSync(file, content);
  }
}

/** Run `body` with a temp directory, removed afterwards. */
function withRoot(body: (root: string) => void): void {
  const root = mkdtempSync(join(tmpdir(), "openl-skills-"));
  try {
    body(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

describe("readSkills", () => {
  it("reads every skill with its frontmatter as JSON and its files, SKILL.md first", () => {
    withRoot((root) => {
      writeSkill(root, "rating", {
        "SKILL.md": SKILL_MD,
        "assets/schema.json": "{}",
        "references/factors.md": "# Factors\n",
      });
      writeSkill(root, "audit", { "SKILL.md": "---\nname: audit\ndescription: Audits.\n--- \n", ".DS_Store": Buffer.from([0x00, 0xff]) });
      writeFileSync(join(root, "README.md"), "not a skill");
      mkdirSync(join(root, ".idea"));

      expect(readSkills(root)).toEqual([
        {
          path: "audit",
          frontmatter: { name: "audit", description: "Audits." },
          files: [{ path: "SKILL.md", text: "---\nname: audit\ndescription: Audits.\n--- \n" }],
        },
        {
          path: "rating",
          frontmatter: { name: "rating", description: "Rates a policy by its factors.\n", metadata: { version: "1.0" } },
          files: [
            { path: "SKILL.md", text: SKILL_MD },
            { path: "assets/schema.json", text: "{}" },
            { path: "references/factors.md", text: "# Factors\n" },
          ],
        },
      ]);
    });
  });

  it.each([
    ["without SKILL.md", { "README.md": "# Rating" }, "rating has no SKILL.md"],
    ["without a frontmatter", { "SKILL.md": "# Rating" }, "does not begin with a YAML frontmatter"],
    ["without a description", { "SKILL.md": "---\nname: rating\n---\n" }, "has no description"],
    ["with a description too long", { "SKILL.md": `---\nname: rating\ndescription: ${"x".repeat(1025)}\n---\n` }, "over 1024"],
    ["with a compatibility too long", { "SKILL.md": `---\nname: rating\ndescription: R.\ncompatibility: ${"x".repeat(501)}\n---\n` }, "compatibility"],
    ["with an empty compatibility", { "SKILL.md": "---\nname: rating\ndescription: R.\ncompatibility: \"\"\n---\n" }, "compatibility"],
    ["with metadata that is not text", { "SKILL.md": "---\nname: rating\ndescription: R.\nmetadata:\n  version: 1\n---\n" }, "not a map of texts"],
    ["named other than its folder", { "SKILL.md": "---\nname: rates\ndescription: R.\n---\n" }, "'rates'"],
    ["with an invalid name", { "SKILL.md": "---\nname: Rating\ndescription: R.\n---\n" }, "'Rating'"],
    ["with a binary file", { "SKILL.md": SKILL_MD, "logo.png": Buffer.from([0xff, 0xfe, 0x00]) }, "logo.png is not UTF-8"],
  ])("fails a skill %s", (_, files, message) => {
    withRoot((root) => {
      writeSkill(root, "rating", files);

      expect(() => readSkills(root)).toThrow(message);
    });
  });

  it("fails a skill over the file limit of the extension", () => {
    withRoot((root) => {
      const files: Record<string, string> = { "SKILL.md": SKILL_MD };
      for (let i = 0; i < 512; i++) {
        files[`references/${i}.md`] = "";
      }
      writeSkill(root, "rating", files);

      expect(() => readSkills(root)).toThrow("rating has 513 files");
    });
  });
});

describe("skills plugin", () => {
  it("answers virtual:skills with the skills of its folder and leaves other modules alone", () => {
    withRoot((root) => {
      writeSkill(root, "rating", { "SKILL.md": SKILL_MD });
      const plugin = skills(root) as unknown as {
        resolveId: (id: string) => string | null;
        load: (id: string) => string | null;
      };

      const id = plugin.resolveId("virtual:skills");
      expect(plugin.resolveId("./client.js")).toBeNull();
      expect(plugin.load("./client.js")).toBeNull();
      expect(id).not.toBeNull();
      expect(plugin.load(id!)).toBe(`export default ${JSON.stringify(readSkills(root))};`);
    });
  });
});

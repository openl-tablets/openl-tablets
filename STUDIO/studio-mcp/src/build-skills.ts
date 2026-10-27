/**
 * The bundler plugin that embeds the skills of `skills/` into the server.
 *
 * It answers the module `virtual:skills` with the text of every file the skills
 * are served with, by its path in `skills/`. The bundle carries them, so the
 * server reads no file.
 *
 * The SDK reads the folder as the server serves it: hidden files and folders are
 * left out, and a skill the Agent Skills specification or the limits of the MCP
 * skills extension refuse fails the build. A file that is not UTF-8 text fails it
 * too.
 */

import { SkillsExtension } from "@modelcontextprotocol/server/ext/skills";
import { nodeSkillFiles } from "@modelcontextprotocol/server/ext/skills/node";
import type { Plugin } from "rolldown";

const VIRTUAL_ID = "virtual:skills";
const RESOLVED_ID = `\0${VIRTUAL_ID}`;

/** The text of every file of the skills under `root`, by its path there with `/` separators. */
export async function readSkills(root: string): Promise<Record<string, string>> {
  const folder = nodeSkillFiles(root);
  // A byte order mark stays part of the text, so the served bytes are the bytes of the file.
  const decoder = new TextDecoder("utf-8", { fatal: true, ignoreBOM: true });
  const files: Record<string, string> = {};
  // The SDK reads every file of a skill once, and refuses the skill when the read fails.
  await SkillsExtension.load({
    list: folder.list,
    async read(path) {
      const bytes = await folder.read(path);
      try {
        files[path] = decoder.decode(bytes);
      } catch {
        throw new Error("not UTF-8 text");
      }
      return bytes;
    },
  }, { strict: true });
  return files;
}

/** The plugin answering `virtual:skills` with the skills of `root`. */
export function skills(root = "skills"): Plugin {
  return {
    name: "skills",
    resolveId(id) {
      return id === VIRTUAL_ID ? RESOLVED_ID : null;
    },
    async load(id) {
      return id === RESOLVED_ID ? `export default ${JSON.stringify(await readSkills(root))};` : null;
    },
  };
}

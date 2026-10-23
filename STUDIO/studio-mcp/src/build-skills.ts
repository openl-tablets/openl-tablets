/**
 * The bundler plugin that embeds the skills of `skills/` into the server.
 *
 * It answers the module `virtual:skills` with every skill of the folder: the
 * path of the skill, the frontmatter of its `SKILL.md` as JSON, and the text of
 * each of its files. The bundle carries them, so the server reads no file.
 *
 * A skill the Agent Skills specification or the limits of the MCP skills
 * extension refuse fails the build: one without `SKILL.md`, a frontmatter without
 * `name` or `description`, a description over 1024 characters, a compatibility
 * empty or over 500 characters, metadata that is not a map of texts, a `name` other
 * than the name of its folder, a file that is not UTF-8 text, more than 512 files
 * or 16 MiB in all. Hidden files and folders are left out.
 */

import { readFileSync, readdirSync } from "node:fs";
import { join, relative, sep } from "node:path";
import { parse } from "yaml";
import type { Plugin } from "rolldown";

/** A file of a skill. */
export interface SkillFile {
  /** The path of the file in the folder of the skill, with `/` separators. */
  path: string;
  text: string;
}

/** A skill the server serves. */
export interface Skill {
  /** The name of the folder of the skill in `skills/`, which is the name of the skill. */
  path: string;
  /** The YAML frontmatter of `SKILL.md`, as it is written. */
  frontmatter: Record<string, unknown>;
  /** Every file of the skill, `SKILL.md` first. */
  files: SkillFile[];
}

const VIRTUAL_ID = "virtual:skills";
const RESOLVED_ID = `\0${VIRTUAL_ID}`;
const SKILL_FILE = "SKILL.md";
const FRONTMATTER = /^---[ \t]*\r?\n([\s\S]*?)\r?\n---[ \t]*(?:\r?\n|$)/;
/** The name rule of the Agent Skills specification: lowercase letters, digits and single inner hyphens. */
const SKILL_NAME = /^(?!-)(?!.*--)[a-z0-9-]{1,64}(?<!-)$/;
const MAX_DESCRIPTION = 1024;
const MAX_COMPATIBILITY = 500;
const MAX_FILES = 512;
const MAX_BYTES = 16 * 1024 * 1024;

/**
 * The paths of the files under `folder`, relative to it, with `/` separators, ordered. A hidden file or folder, such
 * as the .DS_Store a desktop leaves behind, is no part of the skill.
 */
function filesUnder(folder: string): string[] {
  return readdirSync(folder, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => relative(folder, join(entry.parentPath, entry.name)).split(sep).join("/"))
    .filter((path) => !path.split("/").some((segment) => segment.startsWith(".")))
    .sort();
}

/** The frontmatter of the `SKILL.md` text of the skill at `path`, checked against the specification. */
function frontmatterOf(path: string, text: string): Record<string, unknown> {
  const yaml = FRONTMATTER.exec(text)?.[1];
  const frontmatter: unknown = yaml === undefined ? undefined : parse(yaml);
  if (typeof frontmatter !== "object" || frontmatter === null || Array.isArray(frontmatter)) {
    throw new Error(`${path}/${SKILL_FILE} does not begin with a YAML frontmatter`);
  }
  const fields = frontmatter as Record<string, unknown>;
  const { name, description } = fields;
  if (typeof description !== "string" || description.trim() === "") {
    throw new Error(`${path}/${SKILL_FILE} has no description`);
  }
  if (description.length > MAX_DESCRIPTION) {
    throw new Error(
      `${path}/${SKILL_FILE} has a description of ${description.length} characters, over ${MAX_DESCRIPTION}`,
    );
  }
  const { compatibility, metadata } = fields;
  const isCompatibility = typeof compatibility === "string" && compatibility.trim() !== ""
    && compatibility.length <= MAX_COMPATIBILITY;
  if (compatibility !== undefined && !isCompatibility) {
    throw new Error(
      `${path}/${SKILL_FILE} has a compatibility that is not a text of 1 to ${MAX_COMPATIBILITY} characters`,
    );
  }
  const isTextMap = typeof metadata === "object" && metadata !== null && !Array.isArray(metadata)
    && Object.values(metadata).every((value) => typeof value === "string");
  if (metadata !== undefined && !isTextMap) {
    throw new Error(`${path}/${SKILL_FILE} has metadata that is not a map of texts`);
  }
  if (typeof name !== "string" || !SKILL_NAME.test(name) || name !== path) {
    throw new Error(`${path}/${SKILL_FILE} names the skill '${String(name)}', not the name of its folder`);
  }
  return fields;
}

/** The skill in the folder `path` of `root`. */
function readSkill(root: string, path: string): Skill {
  const folder = join(root, path);
  const decoder = new TextDecoder("utf-8", { fatal: true });
  const files = filesUnder(folder).map((file) => {
    try {
      return { path: file, text: decoder.decode(readFileSync(join(folder, file))) };
    } catch {
      throw new Error(`${path}/${file} is not UTF-8 text`);
    }
  });
  const bytes = files.reduce((total, file) => total + Buffer.byteLength(file.text), 0);
  if (files.length > MAX_FILES || bytes > MAX_BYTES) {
    throw new Error(
      `${path} has ${files.length} files of ${bytes} bytes, over ${MAX_FILES} files or ${MAX_BYTES} bytes`,
    );
  }
  const skillFile = files.find((file) => file.path === SKILL_FILE);
  if (skillFile === undefined) {
    throw new Error(`${path} has no ${SKILL_FILE}`);
  }
  return {
    path,
    frontmatter: frontmatterOf(path, skillFile.text),
    files: [skillFile, ...files.filter((file) => file !== skillFile)],
  };
}

/** The skills in the folders of `root`, one skill per folder, ordered by path. A hidden folder holds no skill. */
export function readSkills(root: string): Skill[] {
  return readdirSync(root, { withFileTypes: true })
    .filter((entry) => entry.isDirectory() && !entry.name.startsWith("."))
    .map((entry) => entry.name)
    .sort()
    .map((name) => readSkill(root, name));
}

/** The plugin answering `virtual:skills` with the skills of `root`. */
export function skills(root = "skills"): Plugin {
  return {
    name: "skills",
    resolveId(id) {
      return id === VIRTUAL_ID ? RESOLVED_ID : null;
    },
    load(id) {
      return id === RESOLVED_ID ? `export default ${JSON.stringify(readSkills(root))};` : null;
    },
  };
}

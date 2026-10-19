/**
 * The bundler plugin that lists the libraries the server ships, for the About
 * dialog of OpenL Studio.
 *
 * It writes `mcp-licenses.json` beside the bundle: one entry per package whose
 * code the bundle carries, in the shape of the other lists of the dialog:
 * `name`, `version`, `identifier` (the SPDX expression the package declares),
 * and the `text` of its license file and its NOTICE (`notice`) when it ships
 * them.
 *
 * A package declaring a license `studio-ui` does not accept, or none, fails the
 * build.
 */

import { readFileSync, readdirSync } from "node:fs";
import { join, resolve } from "node:path";
import type { Plugin } from "rolldown";

/** A library the server ships, and the license it is distributed under. */
export interface Library {
  name: string;
  version: string;
  /** The SPDX expression the package declares, such as `MIT` or `(MIT OR Apache-2.0)`. */
  identifier?: string;
  /** The text of the license file the package ships. */
  text?: string;
  /** The NOTICE the package ships beside its license. */
  notice?: string;
}

/** The fields of a package.json the list reads. */
interface PackageJson {
  name: string;
  version: string;
  license?: unknown;
}

const LICENSE_FILE = /^(licen[cs]e|copying)\b/i;
const NOTICE_FILE = /^notice(\.(md|txt))?$/i;
/** The folder of the package a module belongs to: the one right under the last `node_modules`. */
const PACKAGE_FOLDER = /^(.*[\\/]node_modules[\\/](?:@[^\\/]+[\\/])?[^\\/]+)[\\/]/;

/** The licenses an SPDX expression names, without the operators and the parentheses. */
export function licensesOf(expression: string): string[] {
  return expression
    .split(/\s+|[()]/)
    .filter((part) => part !== "" && !["AND", "OR", "WITH"].includes(part));
}

/** The licenses `--onlyAllow` of the build of `studio-ui` lets its libraries have, and the server's too. */
function allowedLicenses(): Set<string> {
  const file = resolve(process.cwd(), "../studio-ui/package.json");
  const { scripts } = JSON.parse(readFileSync(file, "utf-8")) as { scripts: Record<string, string> };
  return new Set(/--onlyAllow "([^"]+)"/.exec(scripts.build ?? "")?.[1].split(";"));
}

/** The libraries declaring a license outside `allowed`, or declaring none. */
export function disallowed(libraries: Library[], allowed: ReadonlySet<string>): Library[] {
  return libraries.filter(
    ({ identifier }) => !identifier || !licensesOf(identifier).every((license) => allowed.has(license)),
  );
}

/** The text of the first file of `folder` whose name matches `pattern`, trimmed. */
function readFirst(folder: string, files: string[], pattern: RegExp): string | undefined {
  const file = files.filter((name) => pattern.test(name)).toSorted((a, b) => a.localeCompare(b))[0];
  return file === undefined ? undefined : readFileSync(join(folder, file), "utf-8").trim();
}

/** The names of the files of `folder`. */
function filesOf(folder: string): string[] {
  return readdirSync(folder, { withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => entry.name);
}

/** The library installed in `folder`. */
function readLibrary(folder: string): Library {
  const pkg = JSON.parse(readFileSync(join(folder, "package.json"), "utf-8")) as PackageJson;
  const files = filesOf(folder);
  return {
    name: pkg.name,
    version: pkg.version,
    identifier: typeof pkg.license === "string" ? pkg.license : undefined,
    text: readFirst(folder, files, LICENSE_FILE),
    notice: readFirst(folder, files, NOTICE_FILE),
  };
}

/**
 * The packages of the bundled modules `moduleIds`, ordered by name and version.
 *
 * A package bundled from several folders is listed once, as the modules name it
 * first. A virtual module of the bundler belongs to no package.
 */
export function readLibraries(moduleIds: string[]): Library[] {
  const folders = new Set(moduleIds.flatMap((id) => (id.startsWith("\0") ? [] : (PACKAGE_FOLDER.exec(id)?.[1] ?? []))));
  const libraries = new Map<string, Library>();
  for (const folder of folders) {
    const library = readLibrary(folder);
    const key = `${library.name}@${library.version}`;
    if (!libraries.has(key)) {
      libraries.set(key, library);
    }
  }
  return [...libraries.values()].sort((a, b) => a.name.localeCompare(b.name) || a.version.localeCompare(b.version));
}

/** The plugin writing `mcp-licenses.json` of the packages the bundle takes code of. */
export function licenses(): Plugin {
  return {
    name: "licenses",
    generateBundle(_, bundle) {
      const modules = Object.values(bundle).flatMap((output) => (output.type === "chunk" ? output.moduleIds : []));
      const libraries = readLibraries(modules);
      const refused = disallowed(libraries, allowedLicenses());
      if (refused.length > 0) {
        const names = refused.map(({ name, version, identifier }) => `${name}@${version} (${identifier ?? "none"})`);
        this.error(`Packages with a license outside the allowed ones: ${names.join(", ")}`);
      }
      const source = `${JSON.stringify(libraries, null, 2)}\n`;
      this.emitFile({ type: "asset", fileName: "mcp-licenses.json", source });
    },
  };
}

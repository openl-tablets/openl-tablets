/**
 * Build-time generator of the list of libraries the server ships, for the About
 * dialog of OpenL Studio.
 *
 * The Maven build runs it (as dist/build-licenses.js, through `npm run licenses`)
 * in the bundle OpenL Studio carries, once the production dependencies are
 * installed there, so the list names exactly the packages that ship. It writes
 * one entry per package in the shape of the other lists of the dialog: `name`,
 * `version`, `identifier` (the SPDX expression the package declares), and the
 * `text` of its license file and its NOTICE (`notice`) when it ships them.
 *
 * `--onlyAllow` names the licenses a package may have, separated by `;`. Every
 * license an expression names must be one of them: a package declaring another
 * license, or none, fails the build.
 */

import { existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

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

/** A package of the lockfile: production unless marked `dev`. */
interface LockedPackage {
  dev?: boolean;
}

/** The fields of a package.json the list reads. */
interface PackageJson {
  name: string;
  version: string;
  license?: unknown;
}

const LICENSE_FILE = /^(licen[cs]e|copying)\b/i;
const NOTICE_FILE = /^notice(\.(md|txt))?$/i;

/** The licenses an SPDX expression names, without the operators and the parentheses. */
export function licensesOf(expression: string): string[] {
  return expression
    .split(/\s+|[()]/)
    .filter((part) => part !== "" && !["AND", "OR", "WITH"].includes(part));
}

/** The libraries declaring a license outside `allowed`, or declaring none. */
export function disallowed(libraries: Library[], allowed: ReadonlySet<string>): Library[] {
  return libraries.filter(
    ({ identifier }) => !identifier || !licensesOf(identifier).every((license) => allowed.has(license)),
  );
}

/** The text of the first file of `folder` whose name matches `pattern`, trimmed. */
function readFirst(folder: string, files: string[], pattern: RegExp): string | undefined {
  const file = files.filter((name) => pattern.test(name)).sort()[0];
  return file === undefined ? undefined : readFileSync(join(folder, file), "utf-8").trim();
}

/** The names of the files of `folder`. */
function filesOf(folder: string): string[] {
  return readdirSync(folder, { withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => entry.name);
}

/** The library installed in `folder`, or nothing when no package is installed there. */
function readLibrary(folder: string): Library | undefined {
  if (!existsSync(join(folder, "package.json"))) {
    return undefined;
  }
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
 * The production packages installed under `root`, ordered by name and version.
 *
 * The packages are the ones `package-lock.json` of `root` lists without the
 * `dev` mark and that are installed: an optional package the platform did not
 * install is not shipped. A package installed in several places is listed once,
 * as the lockfile names it first.
 */
export function readLibraries(root: string): Library[] {
  const lock = JSON.parse(readFileSync(join(root, "package-lock.json"), "utf-8")) as {
    packages: Record<string, LockedPackage>;
  };
  const libraries = new Map<string, Library>();
  for (const [path, locked] of Object.entries(lock.packages)) {
    const library = path === "" || locked.dev ? undefined : readLibrary(join(root, path));
    if (library && !libraries.has(`${library.name}@${library.version}`)) {
      libraries.set(`${library.name}@${library.version}`, library);
    }
  }
  return [...libraries.values()].sort((a, b) =>
    a.name === b.name ? a.version.localeCompare(b.version) : a.name.localeCompare(b.name),
  );
}

function main(): void {
  const [option, licenses, output] = process.argv.slice(2);
  if (option !== "--onlyAllow" || !licenses || !output) {
    throw new Error("Usage: build-licenses --onlyAllow <license;...> <output file>");
  }
  const allowed = new Set(licenses.split(";"));
  const libraries = readLibraries(join(__dirname, ".."));

  const refused = disallowed(libraries, allowed);
  if (refused.length > 0) {
    const names = refused.map(({ name, version, identifier }) => `${name}@${version} (${identifier ?? "none"})`);
    throw new Error(`Packages with a license outside --onlyAllow: ${names.join(", ")}`);
  }
  mkdirSync(dirname(output), { recursive: true });
  writeFileSync(output, `${JSON.stringify(libraries, null, 2)}\n`, "utf-8");
  process.stdout.write(`Listed the licenses of ${libraries.length} libraries in ${output}.\n`);
}

// Run only when invoked directly (node dist/build-licenses.js); importing the
// module for its pure functions (tests) must stay side-effect free.
if (process.argv[1] !== undefined && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    main();
  } catch (error) {
    console.error(`build-licenses: ${error instanceof Error ? error.message : String(error)}`);
    process.exitCode = 1;
  }
}

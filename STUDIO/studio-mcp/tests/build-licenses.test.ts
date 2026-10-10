/**
 * Unit tests for the list of shipped libraries in src/build-licenses.ts: which
 * packages it lists, what it reads of each, and which licenses it refuses.
 * Packages are written to a temp directory in the layout npm installs them in.
 */

import { describe, it, expect } from "@jest/globals";
import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { disallowed, licensesOf, readLibraries, type Library } from "../src/build-licenses.js";

/** Write a package at `path` of `root`, with `files` beside its package.json. */
function writePackage(root: string, path: string, pkg: object, files: Record<string, string> = {}): void {
  const folder = join(root, path);
  mkdirSync(folder, { recursive: true });
  writeFileSync(join(folder, "package.json"), JSON.stringify(pkg));
  for (const [name, content] of Object.entries(files)) {
    writeFileSync(join(folder, name), content);
  }
}

describe("readLibraries", () => {
  it("lists the installed production packages once each, with their license texts, ordered by name", () => {
    const root = mkdtempSync(join(tmpdir(), "openl-licenses-"));
    writeFileSync(join(root, "package-lock.json"), JSON.stringify({
      packages: {
        "": { name: "openl-mcp" },
        "node_modules/zod": {},
        "node_modules/axios": {},
        "node_modules/jest": { dev: true },
        "node_modules/fsevents": { optional: true },
        "node_modules/axios/node_modules/form-data": {},
        "node_modules/express/node_modules/form-data": {},
      },
    }));
    writePackage(root, "node_modules/zod", { name: "zod", version: "4.4.3", license: "MIT" }, {
      "LICENSE": "MIT License\n",
      "NOTICE.md": "Zod notice\n",
    });
    writePackage(root, "node_modules/axios", { name: "axios", version: "1.18.1", license: "MIT" });
    writePackage(root, "node_modules/jest", { name: "jest", version: "30.4.2", license: "MIT" });
    writePackage(root, "node_modules/axios/node_modules/form-data", {
      name: "form-data", version: "4.0.6", license: { type: "MIT" },
    }, { "License.md": "Form-Data License" });
    writePackage(root, "node_modules/express/node_modules/form-data", { name: "form-data", version: "4.0.6" });

    expect(readLibraries(root)).toEqual([
      { name: "axios", version: "1.18.1", identifier: "MIT" },
      // The copy the lockfile names first; a license field of another shape names no identifier.
      { name: "form-data", version: "4.0.6", text: "Form-Data License" },
      { name: "zod", version: "4.4.3", identifier: "MIT", text: "MIT License", notice: "Zod notice" },
    ]);
  });
});

describe("licensesOf", () => {
  it("names the licenses of an expression without its operators and parentheses", () => {
    expect(licensesOf("MIT")).toEqual(["MIT"]);
    expect(licensesOf("(MIT OR Apache-2.0)")).toEqual(["MIT", "Apache-2.0"]);
    expect(licensesOf("GPL-2.0-only WITH Classpath-exception-2.0 AND BSD-3-Clause"))
      .toEqual(["GPL-2.0-only", "Classpath-exception-2.0", "BSD-3-Clause"]);
  });
});

describe("disallowed", () => {
  const allowed = new Set(["MIT", "Apache-2.0"]);
  const library = (identifier?: string): Library => ({ name: "lib", version: "1.0.0", identifier });

  it("accepts a library whose every license is allowed", () => {
    expect(disallowed([library("MIT"), library("(MIT OR Apache-2.0)")], allowed)).toEqual([]);
  });

  it("refuses a library naming any other license, or naming none", () => {
    const refused = [library("(MIT OR GPL-3.0-only)"), library("BSD-3-Clause"), library(undefined)];
    expect(disallowed([library("MIT"), ...refused], allowed)).toEqual(refused);
  });
});

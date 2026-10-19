/**
 * Unit tests for the list of shipped libraries in src/build-licenses.ts: which
 * packages it lists, what it reads of each, which licenses it refuses, and the
 * file the bundler plugin writes. Packages are written to a temp directory in the
 * layout npm installs them in, and the bundle takes modules of some of them.
 */

import { describe, it, expect, vi } from "vitest";
import { mkdirSync, mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { disallowed, licenses, licensesOf, readLibraries, type Library } from "./build-licenses.js";

/** Write a package at `path` of `root`, with `files` beside its package.json. */
function writePackage(root: string, path: string, pkg: object, files: Record<string, string> = {}): void {
  const folder = join(root, path);
  mkdirSync(folder, { recursive: true });
  writeFileSync(join(folder, "package.json"), JSON.stringify(pkg));
  for (const [name, content] of Object.entries(files)) {
    writeFileSync(join(folder, name), content);
  }
}

/** Run `body` with a temp directory, removed afterwards. */
function withRoot(body: (root: string) => void): void {
  const root = mkdtempSync(join(tmpdir(), "openl-licenses-"));
  try {
    body(root);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

describe("readLibraries", () => {
  it("lists the packages of the bundled modules once each, with their license texts, ordered by name", () => {
    withRoot((root) => {
      writePackage(root, "node_modules/zod", { name: "zod", version: "4.4.3", license: "MIT" }, {
        "LICENSE": "MIT License\n",
        "NOTICE.md": "Zod notice\n",
      });
      writePackage(root, "node_modules/@modelcontextprotocol/server", {
        name: "@modelcontextprotocol/server", version: "2.3.1", license: "Apache-2.0",
      });
      writePackage(root, "node_modules/jest", { name: "jest", version: "30.4.2", license: "MIT" });
      writePackage(root, "node_modules/axios/node_modules/form-data", {
        name: "form-data", version: "4.0.6", license: { type: "MIT" },
      }, { "License.md": "Form-Data License" });
      writePackage(root, "node_modules/express/node_modules/form-data", { name: "form-data", version: "4.0.6" });

      expect(readLibraries([
        join(root, "src/index.ts"),
        "\0rolldown/runtime.js",
        join(root, "node_modules/zod/v4/core/util.js"),
        join(root, "node_modules/zod/v4/classic/schemas.js"),
        join(root, "node_modules/@modelcontextprotocol/server/dist/index.mjs"),
        join(root, "node_modules/axios/node_modules/form-data/lib/form_data.js"),
        join(root, "node_modules/express/node_modules/form-data/lib/form_data.js"),
      ])).toEqual([
        { name: "@modelcontextprotocol/server", version: "2.3.1", identifier: "Apache-2.0" },
        // The copy the modules name first; a license field of another shape names no identifier.
        { name: "form-data", version: "4.0.6", text: "Form-Data License" },
        { name: "zod", version: "4.4.3", identifier: "MIT", text: "MIT License", notice: "Zod notice" },
      ]);
    });
  });
});

describe("licenses", () => {
  /** Run the plugin on a bundle of one chunk taking `moduleIds`, beside an asset. */
  function generate(moduleIds: string[]): { emitFile: ReturnType<typeof vi.fn> } {
    const context = { emitFile: vi.fn(), error: (message: string) => { throw new Error(message); } };
    const bundle = {
      "index.mjs": { type: "chunk", moduleIds },
      "notes.txt": { type: "asset" },
    };
    (licenses().generateBundle as (this: unknown, options: unknown, bundle: unknown) => void)
      .call(context, {}, bundle);
    return context;
  }

  it("writes mcp-licenses.json of the packages the bundle takes code of", () => {
    withRoot((root) => {
      writePackage(root, "node_modules/zod", { name: "zod", version: "4.4.3", license: "MIT" });

      const { emitFile } = generate([join(root, "node_modules/zod/index.js")]);

      expect(emitFile).toHaveBeenCalledWith({
        type: "asset",
        fileName: "mcp-licenses.json",
        source: `${JSON.stringify([{ name: "zod", version: "4.4.3", identifier: "MIT" }], null, 2)}\n`,
      });
    });
  });

  it("fails the build on a package of a license outside the allowed ones", () => {
    withRoot((root) => {
      writePackage(root, "node_modules/gpl", { name: "gpl", version: "1.0.0", license: "GPL-3.0-only" });
      writePackage(root, "node_modules/bare", { name: "bare", version: "2.0.0" });

      expect(() => generate([join(root, "node_modules/gpl/index.js"), join(root, "node_modules/bare/index.js")]))
        .toThrow("Packages with a license outside the allowed ones: bare@2.0.0 (none), gpl@1.0.0 (GPL-3.0-only)");
    });
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

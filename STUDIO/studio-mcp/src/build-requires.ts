/**
 * The bundler plugin that bundles the packages a dependency loads through a `require` of its own.
 *
 * A dependency published as ES modules by a bundler may load a CommonJS package with `__require("<package>")`,
 * a function its runtime makes with `createRequire(import.meta.url)`. The bundler sees no import there and leaves the
 * call as it is. The bundle then looks for the package in a `node_modules` folder at run time, and the server, which
 * OpenL Studio runs from `WEB-INF/mcp` without one, fails to load the module. The OpenTelemetry SDK loads
 * `forwarded-parse` and `@grpc/grpc-js` so.
 *
 * The plugin turns each such call of a package into a `require` the bundler resolves, so the package is bundled and
 * still loaded only when the call runs. A built-in module of Node.js is left to the runtime.
 */

import { isBuiltin } from "node:module";
import type { Plugin } from "rolldown";

/** A call of the `require` of a dependency, with the name of the module it loads; never a method of an object. */
const OWN_REQUIRE = /(?<![\w.$])__require\((["'])([^"']+)\1\)/g;

/** The code with every `__require` of a package turned into a `require` the bundler resolves. */
export function bundledRequires(code: string): string {
  return code.replaceAll(OWN_REQUIRE, (call: string, quote: string, module: string) =>
    isBuiltin(module) ? call : `require(${quote}${module}${quote})`);
}

/** The plugin, applied to the modules of `node_modules` that call `__require`. */
export function requires(): Plugin {
  return {
    name: "requires",
    transform: {
      filter: { id: /[\\/]node_modules[\\/]/, code: "__require(" },
      handler: (code) => ({ code: bundledRequires(code) }),
    },
  };
}

import { defineConfig } from "rolldown";
import { licenses } from "./src/build-licenses.ts";

// The server with its production dependencies in one minified ES module, which OpenL Studio runs from
// WEB-INF/mcp/index.mjs without a package.json, and the list of the licenses of the packages it takes code of,
// dist/mcp-licenses.json.
export default defineConfig({
  input: "src/index.ts",
  platform: "node",
  plugins: [licenses()],
  output: { dir: "dist", entryFileNames: "[name].mjs", cleanDir: true, minify: true },
});

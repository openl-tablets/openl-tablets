import { defineConfig } from "rolldown";
import { licenses } from "./src/build-licenses.ts";
import { requires } from "./src/build-requires.ts";
import { skills } from "./src/build-skills.ts";

// The server with its production dependencies and the skills of skills/ in minified ES modules, which OpenL Studio
// runs from WEB-INF/mcp/index.mjs without a package.json, and the list of the licenses of the packages it takes code
// of, dist/mcp-licenses.json. The modules the server imports only when it needs them, such as the OpenTelemetry SDK,
// are chunks of their own beside index.mjs: .mjs too, since no package.json makes a .js file an ES module.
export default defineConfig({
  input: "src/index.ts",
  platform: "node",
  plugins: [skills(), requires(), licenses()],
  output: {
    dir: "dist",
    entryFileNames: "[name].mjs",
    chunkFileNames: "[name]-[hash].mjs",
    cleanDir: true,
    minify: true,
  },
});

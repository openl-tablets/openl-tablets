import { defineConfig } from "vitest/config";
import { skills } from "./src/build-skills.ts";

// The tests import the skills of skills/ as the bundle does. Coverage counts every source file, the ones no test
// imports included, and writes the lcov.info Sonar reads.
export default defineConfig({
  plugins: [skills()],
  test: {
    testTimeout: 10000,
    coverage: { include: ["src/**/*.ts"], reporter: ["lcov", "text-summary"] },
  },
});

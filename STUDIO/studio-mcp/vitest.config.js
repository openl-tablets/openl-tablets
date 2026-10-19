import { defineConfig } from "vitest/config";

// Coverage counts every source file, the ones no test imports included, and writes the lcov.info Sonar reads.
export default defineConfig({
  test: {
    testTimeout: 10000,
    coverage: { include: ["src/**/*.ts"], reporter: ["lcov", "text-summary"] },
  },
});

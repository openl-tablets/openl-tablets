import { describe, expect, it } from "vitest";

import { bundledRequires } from "./build-requires.js";

describe("bundledRequires", () => {
  it("turns the require of a package into one the bundler resolves, keeping where it runs", () => {
    const code = [
      `const forwardedParse = __require("forwarded-parse");`,
      `function client() { return __require('@grpc/grpc-js'); }`,
    ].join("\n");

    expect(bundledRequires(code)).toBe([
      `const forwardedParse = require("forwarded-parse");`,
      `function client() { return require('@grpc/grpc-js'); }`,
    ].join("\n"));
  });

  it("leaves a method of an object of the same name as it is", () => {
    const code = `loader.__require("forwarded-parse"); $__require("x");`;

    expect(bundledRequires(code)).toBe(code);
  });

  it("leaves a built-in module of Node.js to the runtime", () => {
    const code = `const http = __require("node:http"), zlib = __require("zlib");`;

    expect(bundledRequires(code)).toBe(code);
  });
});

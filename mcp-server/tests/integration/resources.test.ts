/**
 * Integration tests for MCP Resource URIs
 * Tests the resource URI implementation with real OpenL client
 */

import { describe, it, expect, beforeAll, afterAll } from "@jest/globals";
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { ReadResourceRequestSchema, ListResourcesRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { OpenLClient } from "../../src/client.js";
import type { OpenLConfig } from "../../src/types.js";

describe.skip("Resource URI Integration Tests", () => {
  let server: Server;
  let client: OpenLClient;

  beforeAll(() => {
    // Initialize with test configuration
    const config: OpenLConfig = {
      baseUrl: process.env.OPENL_BASE_URL || "http://localhost:8080/rest",
      username: process.env.OPENL_USERNAME || "admin",
      password: process.env.OPENL_PASSWORD || "admin",
    };

    client = new OpenLClient(config);
    server = new Server(
      { name: "openl-test", version: "1.0.0" },
      { capabilities: { resources: {} } }
    );
  });

  afterAll(async () => {
    // Cleanup if needed
  });

  describe("ListResources", () => {
    it("should list all available resource URIs", async () => {
      const handler = server.getRequestHandler(ListResourcesRequestSchema);
      expect(handler).toBeDefined();

      if (handler) {
        const result = await handler({ method: "resources/list", params: {} }, {});
        expect(result).toHaveProperty("resources");
        expect(Array.isArray(result.resources)).toBe(true);
        expect(result.resources.length).toBeGreaterThan(0);

        // Check for required resources
        const uris = result.resources.map((r: any) => r.uri);
        expect(uris).toContain("openl://repositories");
        expect(uris).toContain("openl://projects");
        expect(uris).toContain("openl://deployments");
        expect(uris).toContain("openl://projects/{projectId}");
        expect(uris).toContain("openl://projects/{projectId}/tables");
      }
    });

    it("should include proper metadata for each resource", async () => {
      const handler = server.getRequestHandler(ListResourcesRequestSchema);
      if (handler) {
        const result = await handler({ method: "resources/list", params: {} }, {});

        result.resources.forEach((resource: any) => {
          expect(resource).toHaveProperty("uri");
          expect(resource).toHaveProperty("name");
          expect(resource).toHaveProperty("description");
          expect(resource).toHaveProperty("mimeType");
          expect(typeof resource.uri).toBe("string");
          expect(typeof resource.name).toBe("string");
          expect(typeof resource.description).toBe("string");
        });
      }
    });
  });

  describe("ReadResource - Static URIs", () => {
    it("should read openl://repositories", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      expect(handler).toBeDefined();

      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: "openl://repositories" },
        }, {});

        expect(result).toHaveProperty("contents");
        expect(Array.isArray(result.contents)).toBe(true);
        expect(result.contents.length).toBe(1);

        const content = result.contents[0];
        expect(content.uri).toBe("openl://repositories");
        expect(content.mimeType).toBe("application/json");
        expect(content.text).toBeDefined();

        // Should be valid JSON
        const data = JSON.parse(content.text);
        expect(Array.isArray(data)).toBe(true);
      }
    });

    it("should read openl://projects", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: "openl://projects" },
        }, {});

        expect(result.contents[0].uri).toBe("openl://projects");
        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(Array.isArray(data)).toBe(true);
      }
    });

    it("should read openl://deployments", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: "openl://deployments" },
        }, {});

        expect(result.contents[0].uri).toBe("openl://deployments");
        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(Array.isArray(data)).toBe(true);
      }
    });
  });

  describe("ReadResource - Parameterized URIs", () => {
    it("should read openl://projects/{projectId}", async () => {
      // This test requires a known project to exist
      const projectId = "design-TestProject";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}` },
        }, {});

        expect(result.contents[0].uri).toBe(`openl://projects/${projectId}`);
        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(data).toHaveProperty("projectId");
        expect(data).toHaveProperty("projectName");
      }
    });

    it("should read openl://projects/{projectId}/tables", async () => {
      const projectId = "design-TestProject";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}/tables` },
        }, {});

        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(Array.isArray(data)).toBe(true);
      }
    });

    it("should read openl://projects/{projectId}/tables/{tableId}", async () => {
      const projectId = "design-TestProject";
      const tableId = "TestTable_123";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}/tables/${tableId}` },
        }, {});

        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(data).toHaveProperty("id");
        expect(data).toHaveProperty("name");
      }
    });

    it("should read openl://projects/{projectId}/history", async () => {
      const projectId = "design-TestProject";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}/history` },
        }, {});

        expect(result.contents[0].mimeType).toBe("application/json");

        const data = JSON.parse(result.contents[0].text);
        expect(Array.isArray(data)).toBe(true);
      }
    });

    it("should read openl://projects/{projectId}/files/{filePath}", async () => {
      const projectId = "design-TestProject";
      const filePath = "Rules.xlsx";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}/files/${filePath}` },
        }, {});

        expect(result.contents[0].mimeType).toBe("application/octet-stream");

        // File content should be base64 encoded
        const base64Content = result.contents[0].text;
        expect(typeof base64Content).toBe("string");
        expect(base64Content.length).toBeGreaterThan(0);

        // Should be valid base64
        expect(() => Buffer.from(base64Content, "base64")).not.toThrow();
      }
    });
  });

  describe("Error Handling", () => {
    it("should return error for invalid URI format", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        await expect(
          handler({
            method: "resources/read",
            params: { uri: "invalid-uri" },
          }, {})
        ).rejects.toThrow();
      }
    });

    it("should return error for unknown resource type", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        await expect(
          handler({
            method: "resources/read",
            params: { uri: "openl://unknown" },
          }, {})
        ).rejects.toThrow();
      }
    });

    it("should return error for non-existent project", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        await expect(
          handler({
            method: "resources/read",
            params: { uri: "openl://projects/nonexistent-project" },
          }, {})
        ).rejects.toThrow();
      }
    });

    it("should return error for missing file path", async () => {
      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        await expect(
          handler({
            method: "resources/read",
            params: { uri: "openl://projects/design-Test/files/" },
          }, {})
        ).rejects.toThrow();
      }
    });
  });

  describe("URI Template Edge Cases", () => {
    it("should handle projectId with special characters", async () => {
      const projectId = "design-Example%201%20-%20Bank%20Rating";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        // Should properly decode URI components
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}` },
        }, {});

        expect(result.contents[0].uri).toContain(projectId);
      }
    });

    it("should handle file paths with subdirectories", async () => {
      const projectId = "design-TestProject";
      const filePath = "rules/subfolder/Rules.xlsx";

      const handler = server.getRequestHandler(ReadResourceRequestSchema);
      if (handler) {
        const result = await handler({
          method: "resources/read",
          params: { uri: `openl://projects/${projectId}/files/${filePath}` },
        }, {});

        expect(result.contents[0].mimeType).toBe("application/octet-stream");
      }
    });
  });
});

/**
 * Unit tests for auth.ts
 * Tests authentication methods and token management
 */

import { describe, it, expect, jest, beforeEach, afterEach } from "@jest/globals";
import axios, { AxiosInstance } from "axios";
import MockAdapter from "axios-mock-adapter";
import { AuthenticationManager } from "../src/auth.js";
import type { OpenLConfig } from "../src/types.js";

describe("AuthenticationManager", () => {
  let mockAxios: MockAdapter;
  let axiosInstance: AxiosInstance;

  beforeEach(() => {
    axiosInstance = axios.create();
    mockAxios = new MockAdapter(axiosInstance);
  });

  afterEach(() => {
    mockAxios.reset();
    mockAxios.restore();
  });

  describe("Basic Authentication", () => {
    it("should add Basic auth header when username/password provided", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "admin",
        password: "admin",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply(200, { success: true });

      const response = await axiosInstance.get("/test");

      expect(response.config.headers?.Authorization).toMatch(/^Basic /);
    });

    it("should encode credentials correctly", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "testuser",
        password: "testpass123",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization as string;
        const expectedToken = Buffer.from("testuser:testpass123").toString("base64");
        expect(authHeader).toBe(`Basic ${expectedToken}`);
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });

    it("should handle special characters in password", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "admin",
        password: "p@ssw0rd!#$%",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization as string;
        expect(authHeader).toMatch(/^Basic /);
        expect(authHeader.length).toBeGreaterThan(10);
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });
  });

  describe("API Key Authentication", () => {
    it("should add API key header when apiKey provided", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        apiKey: "test-api-key-12345",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        expect(config.headers?.["X-API-Key"]).toBe("test-api-key-12345");
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });

    it("should prefer API key over basic auth", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        apiKey: "test-api-key",
        username: "admin",
        password: "admin",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        expect(config.headers?.["X-API-Key"]).toBe("test-api-key");
        expect(config.headers?.Authorization).toBeUndefined();
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });
  });

  describe("OAuth 2.1 Authentication", () => {
    it("should fetch OAuth token on first request", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      // Mock token endpoint
      mockAxios.onPost("/oauth/token").reply(200, {
        access_token: "test-access-token",
        token_type: "Bearer",
        expires_in: 3600,
      });

      // Mock actual request
      mockAxios.onGet("/test").reply((config) => {
        expect(config.headers?.Authorization).toBe("Bearer test-access-token");
        return [200, { success: true }];
      });

      await axiosInstance.get("/test");
    });

    it("should reuse valid OAuth token", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      // Mock token endpoint (should only be called once)
      let tokenCallCount = 0;
      mockAxios.onPost("/oauth/token").reply(() => {
        tokenCallCount++;
        return [
          200,
          {
            access_token: "test-access-token",
            token_type: "Bearer",
            expires_in: 3600,
          },
        ];
      });

      // Mock multiple requests
      mockAxios.onGet("/test1").reply(200, {});
      mockAxios.onGet("/test2").reply(200, {});

      await axiosInstance.get("/test1");
      await axiosInstance.get("/test2");

      expect(tokenCallCount).toBe(1); // Token should be reused
    });

    it("should refresh expired OAuth token", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      let tokenCallCount = 0;

      // Mock token endpoint
      mockAxios.onPost("/oauth/token").reply(() => {
        tokenCallCount++;
        return [
          200,
          {
            access_token: `token-${tokenCallCount}`,
            token_type: "Bearer",
            expires_in: 1, // Short expiry for testing
          },
        ];
      });

      // First request
      mockAxios.onGet("/test1").reply(200, {});
      await axiosInstance.get("/test1");

      // Wait for token to expire
      await new Promise((resolve) => setTimeout(resolve, 1500));

      // Second request should trigger refresh
      mockAxios.onGet("/test2").reply(200, {});
      await axiosInstance.get("/test2");

      expect(tokenCallCount).toBeGreaterThanOrEqual(2);
    });

    it("should handle token refresh on 401 error", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      let requestCount = 0;

      // Mock token endpoint
      mockAxios.onPost("/oauth/token").reply(200, {
        access_token: "new-access-token",
        token_type: "Bearer",
        expires_in: 3600,
      });

      // Mock request that returns 401 first, then succeeds
      mockAxios.onGet("/test").reply(() => {
        requestCount++;
        if (requestCount === 1) {
          return [401, { error: "Unauthorized" }];
        }
        return [200, { success: true }];
      });

      const response = await axiosInstance.get("/test");
      expect(response.data.success).toBe(true);
      expect(requestCount).toBe(2); // Should retry once
    });

    it("should include scope when provided", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
          scope: "read write",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onPost("/oauth/token").reply((config) => {
        const body = config.data;
        expect(body).toContain("scope=read%20write");
        return [
          200,
          {
            access_token: "test-token",
            token_type: "Bearer",
            expires_in: 3600,
          },
        ];
      });

      mockAxios.onGet("/test").reply(200, {});
      await axiosInstance.get("/test");
    });
  });

  describe("Error Handling", () => {
    it("should handle network errors", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "admin",
        password: "admin",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").networkError();

      await expect(axiosInstance.get("/test")).rejects.toThrow();
    });

    it("should handle OAuth token endpoint errors", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "invalid-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onPost("/oauth/token").reply(401, {
        error: "invalid_client",
      });

      mockAxios.onGet("/test").reply(200, {});

      await expect(axiosInstance.get("/test")).rejects.toThrow();
    });

    it("should not retry non-401 errors", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onPost("/oauth/token").reply(200, {
        access_token: "test-token",
        token_type: "Bearer",
        expires_in: 3600,
      });

      let requestCount = 0;
      mockAxios.onGet("/test").reply(() => {
        requestCount++;
        return [403, { error: "Forbidden" }];
      });

      await expect(axiosInstance.get("/test")).rejects.toThrow();
      expect(requestCount).toBe(1); // Should not retry
    });
  });

  describe("No Authentication", () => {
    it("should work without any auth configuration", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        expect(config.headers?.Authorization).toBeUndefined();
        expect(config.headers?.["X-API-Key"]).toBeUndefined();
        return [200, { success: true }];
      });

      const response = await axiosInstance.get("/test");
      expect(response.data.success).toBe(true);
    });
  });

  describe("Edge Cases", () => {
    it("should handle empty username/password", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        username: "",
        password: "",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization as string;
        const expectedToken = Buffer.from(":").toString("base64");
        expect(authHeader).toBe(`Basic ${expectedToken}`);
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });

    it("should handle malformed OAuth token response", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onPost("/oauth/token").reply(200, {
        // Missing access_token field
        token_type: "Bearer",
      });

      mockAxios.onGet("/test").reply(200, {});

      await expect(axiosInstance.get("/test")).rejects.toThrow();
    });

    it("should handle concurrent requests with OAuth", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        oauth2: {
          tokenUrl: "http://localhost:8080/oauth/token",
          clientId: "test-client",
          clientSecret: "test-secret",
        },
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      let tokenCallCount = 0;
      mockAxios.onPost("/oauth/token").reply(() => {
        tokenCallCount++;
        return [
          200,
          {
            access_token: "test-token",
            token_type: "Bearer",
            expires_in: 3600,
          },
        ];
      });

      mockAxios.onGet(/\/test/).reply(200, {});

      // Make multiple concurrent requests
      await Promise.all([
        axiosInstance.get("/test1"),
        axiosInstance.get("/test2"),
        axiosInstance.get("/test3"),
      ]);

      // Token should only be fetched once despite concurrent requests
      expect(tokenCallCount).toBe(1);
    });
  });
});

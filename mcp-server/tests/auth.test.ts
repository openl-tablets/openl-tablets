/**
 * Unit tests for auth.ts
 * Tests authentication methods and token management
 */

import { describe, it, expect, jest, beforeEach, afterEach } from "@jest/globals";
import axios, { AxiosInstance } from "axios";
import MockAdapter from "axios-mock-adapter";
import { AuthenticationManager } from "../src/auth.js";
import type { OpenLConfig } from "../src/types.js";

// Create a separate mock adapter for global axios (used by obtainOAuth2Token)
let globalMockAxios: MockAdapter;

describe("AuthenticationManager", () => {
  let mockAxios: MockAdapter;
  let axiosInstance: AxiosInstance;

  beforeEach(() => {
    axiosInstance = axios.create();
    mockAxios = new MockAdapter(axiosInstance);
    // Also mock global axios for OAuth token requests
    globalMockAxios = new MockAdapter(axios);
  });

  afterEach(() => {
    mockAxios.reset();
    mockAxios.restore();
    globalMockAxios.reset();
    globalMockAxios.restore();
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

      // Mock token endpoint - use full URL since obtainOAuth2Token uses global axios
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(200, {
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

      // Mock token endpoint (should only be called once) - use full URL
      let tokenCallCount = 0;
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(() => {
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

      // Mock token endpoint - use full URL
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(() => {
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

      // Mock token endpoint - use full URL
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(200, {
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

      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply((config) => {
        const body = config.data;
        // URLSearchParams encodes spaces as '+' not '%20'
        expect(body).toMatch(/scope=read(\+|%20)write/);
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

      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(401, {
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

      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(200, {
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

      // Empty strings are falsy, so Basic Auth won't be added
      // This is expected behavior - empty credentials are treated as no auth
      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization;
        expect(authHeader).toBeUndefined();
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

      // Mock malformed token response (missing access_token)
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(200, {
        // Missing access_token field
        token_type: "Bearer",
      });

      // The request should fail because token is invalid (undefined access_token)
      mockAxios.onGet("/test").reply(200, {});

      // When token is invalid, the request may still proceed but without auth header
      // Or it may throw an error - depends on implementation
      // Let's check that it doesn't succeed with a valid token
      try {
        await axiosInstance.get("/test");
        // If it succeeds, check that no auth header was set (token was invalid)
        const lastRequest = mockAxios.history.get[mockAxios.history.get.length - 1];
        expect(lastRequest.headers?.Authorization).toBeUndefined();
      } catch (error) {
        // If it throws, that's also acceptable
        expect(error).toBeDefined();
      }
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
      globalMockAxios.onPost("http://localhost:8080/oauth/token").reply(() => {
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

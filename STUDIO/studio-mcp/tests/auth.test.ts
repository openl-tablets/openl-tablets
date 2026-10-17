/**
 * Unit tests for auth.ts
 * Tests authentication methods and token management
 */

import { describe, it, expect, beforeEach, afterEach } from "@jest/globals";
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

  describe("Personal Access Token", () => {
    it("should send the PAT verbatim in the `Token` scheme (not Bearer, not Base64)", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        personalAccessToken: "openl_pat_public.secret",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization as string;
        expect(authHeader).toBe("Token openl_pat_public.secret");
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });
  });


  describe("Error Handling", () => {
    it("should handle network errors", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        personalAccessToken: "openl_pat_public.secret",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      mockAxios.onGet("/test").networkError();

      await expect(axiosInstance.get("/test")).rejects.toThrow();
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
    it("should treat an empty PAT as no auth (falsy → no Authorization header)", async () => {
      const config: OpenLConfig = {
        baseUrl: "http://localhost:8080",
        personalAccessToken: "",
      };

      const auth = new AuthenticationManager(config);
      auth.setupInterceptors(axiosInstance);

      // An empty string is falsy, so no Authorization header is added.
      mockAxios.onGet("/test").reply((config) => {
        const authHeader = config.headers?.Authorization;
        expect(authHeader).toBeUndefined();
        return [200, {}];
      });

      await axiosInstance.get("/test");
    });

  });
});

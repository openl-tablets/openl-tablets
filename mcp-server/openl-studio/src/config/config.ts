/**
 * Configuration loader for OpenL Studio MCP Server
 * Loads configuration from environment variables or config file
 */

import { readFileSync, existsSync } from 'fs';
import { homedir } from 'os';
import { join } from 'path';
import type { ServerConfig, OpenLStudioConfig } from './types.js';
import { DEFAULT_CONFIG, DEFAULT_OPENL_CONFIG } from './types.js';

/**
 * Load configuration from environment variables
 */
function loadFromEnv(): Partial<ServerConfig> {
  const openl: Partial<OpenLStudioConfig> = {};

  if (process.env.OPENL_BASE_URL) {
    openl.baseUrl = process.env.OPENL_BASE_URL;
  }

  if (process.env.OPENL_USERNAME) {
    openl.username = process.env.OPENL_USERNAME;
  }

  if (process.env.OPENL_PASSWORD) {
    openl.password = process.env.OPENL_PASSWORD;
  }

  if (process.env.OPENL_TIMEOUT) {
    openl.timeout = parseInt(process.env.OPENL_TIMEOUT, 10);
  }

  if (process.env.OPENL_RETRIES) {
    openl.retries = parseInt(process.env.OPENL_RETRIES, 10);
  }

  const config: Partial<ServerConfig> = {};

  if (Object.keys(openl).length > 0) {
    config.openl = openl as OpenLStudioConfig;
  }

  if (process.env.LOG_LEVEL) {
    const level = process.env.LOG_LEVEL.toUpperCase();
    if (['ERROR', 'WARN', 'INFO', 'DEBUG'].includes(level)) {
      config.logLevel = level as 'ERROR' | 'WARN' | 'INFO' | 'DEBUG';
    }
  }

  return config;
}

/**
 * Load configuration from file
 */
function loadFromFile(configPath?: string): Partial<ServerConfig> {
  const defaultPath = join(homedir(), '.openl-mcp', 'config.json');
  const path = configPath || defaultPath;

  if (!existsSync(path)) {
    return {};
  }

  try {
    const content = readFileSync(path, 'utf-8');
    const parsed = JSON.parse(content);
    return parsed;
  } catch (error) {
    console.error(`Failed to load config from ${path}:`, error);
    return {};
  }
}


/**
 * Validate configuration
 */
function validateConfig(config: ServerConfig): void {
  if (!config.openl) {
    throw new Error('OpenL Studio configuration is required');
  }

  if (!config.openl.baseUrl) {
    throw new Error('OpenL Studio base URL is required (OPENL_BASE_URL)');
  }

  if (!config.openl.username) {
    throw new Error('OpenL Studio username is required (OPENL_USERNAME)');
  }

  if (!config.openl.password) {
    throw new Error('OpenL Studio password is required (OPENL_PASSWORD)');
  }

  // Validate URL format
  try {
    new URL(config.openl.baseUrl);
  } catch {
    throw new Error(`Invalid OpenL Studio base URL: ${config.openl.baseUrl}`);
  }

  // Validate timeout
  if (config.openl.timeout && (config.openl.timeout < 1000 || config.openl.timeout > 300000)) {
    throw new Error('Timeout must be between 1000 and 300000 milliseconds');
  }

  // Validate retries
  if (config.openl.retries && (config.openl.retries < 0 || config.openl.retries > 10)) {
    throw new Error('Retries must be between 0 and 10');
  }
}

/**
 * Load and validate configuration from all sources
 * Priority: Environment variables > Config file > Defaults
 */
export function loadConfig(configPath?: string): ServerConfig {
  const fileConfig = loadFromFile(configPath);
  const envConfig = loadFromEnv();

  const merged: any = {};

  // Merge all configs
  const configs: any[] = [DEFAULT_CONFIG, { openl: DEFAULT_OPENL_CONFIG }, fileConfig, envConfig];
  for (const config of configs) {
    if (config?.openl) {
      merged.openl = { ...merged.openl, ...config.openl };
    }
    if (config?.logLevel) {
      merged.logLevel = config.logLevel;
    }
  }

  // Type assertion after validation
  const config = merged as ServerConfig;
  validateConfig(config);

  return config;
}

/**
 * Input validators for OpenL Studio MCP Server
 */

import { ValidationError } from './errors.js';

/**
 * Validate project name
 */
export function validateProjectName(name: string): void {
  if (!name || typeof name !== 'string') {
    throw new ValidationError('Project name is required and must be a string');
  }

  if (name.trim().length === 0) {
    throw new ValidationError('Project name cannot be empty');
  }

  // Check for invalid characters (basic validation)
  if (/[<>:"|?*\\/]/.test(name)) {
    throw new ValidationError(
      `Project name contains invalid characters: ${name}`,
      { invalidChars: '<>:"|?*\\/' }
    );
  }
}

/**
 * Validate repository name
 */
export function validateRepositoryName(name: string): void {
  if (!name || typeof name !== 'string') {
    throw new ValidationError('Repository name is required and must be a string');
  }

  if (name.trim().length === 0) {
    throw new ValidationError('Repository name cannot be empty');
  }
}

/**
 * Validate file path
 */
export function validateFilePath(path: string): void {
  if (!path || typeof path !== 'string') {
    throw new ValidationError('File path is required and must be a string');
  }

  if (path.trim().length === 0) {
    throw new ValidationError('File path cannot be empty');
  }

  // Check for path traversal attempts
  if (path.includes('..')) {
    throw new ValidationError(
      'File path cannot contain parent directory references (..)',
      { path }
    );
  }
}

/**
 * Validate version string
 */
export function validateVersion(version: string): void {
  if (!version || typeof version !== 'string') {
    throw new ValidationError('Version is required and must be a string');
  }

  if (version.trim().length === 0) {
    throw new ValidationError('Version cannot be empty');
  }
}

/**
 * Sanitize string input
 */
export function sanitizeString(input: string): string {
  return input.trim().replace(/[\x00-\x1F\x7F]/g, '');
}

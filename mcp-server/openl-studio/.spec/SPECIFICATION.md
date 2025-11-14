# OpenL Studio MCP Server Specification

## Overview

This MCP (Model Context Protocol) server enables AI assistants like Claude to interact with OpenL Tablets Studio programmatically, allowing users to control OpenL Studio through natural language commands instead of manual UI interactions.

## Problem Statement

Currently, users must manually navigate the OpenL Studio web interface to perform common project operations such as:
- Opening projects
- Closing projects
- Copying projects
- Exporting projects
- Managing project dependencies

This manual process is time-consuming and prevents automation of workflows.

## Solution

Create an MCP server that exposes OpenL Studio project operations as callable tools, enabling users to issue natural language commands like:
- "Open STD Rating project"
- "Close all projects"
- "Export STD Rating project to ZIP"
- "Copy STD Rating project to new-rating-project"

## Target Users

- OpenL Studio developers who want to automate project management
- AI assistants (Claude, Cursor, etc.) that need to interact with OpenL Studio
- DevOps engineers automating OpenL deployment workflows

## Core Requirements

### Functional Requirements

1. **Project Discovery**
   - List all available projects across all repositories
   - Query project status (open/closed)
   - Get project metadata and dependencies

2. **Project Operations**
   - Open project (with option to open dependencies)
   - Close project
   - Copy project (to new name or branch)
   - Export project (as ZIP file)
   - Delete/erase project

3. **Repository Operations**
   - List all repositories
   - Get repository information

4. **Authentication & Authorization**
   - Support OpenL Studio authentication mechanisms
   - Respect user permissions (ACL-based)

### Non-Functional Requirements

1. **Performance**
   - Response time < 5 seconds for project operations
   - Handle concurrent requests

2. **Security**
   - Secure credential storage
   - HTTPS communication with OpenL Studio
   - No credential exposure in logs

3. **Reliability**
   - Graceful error handling
   - Clear error messages
   - Retry logic for transient failures

4. **Usability**
   - Natural language tool descriptions
   - Helpful parameter validation
   - Progress feedback for long operations

## Architecture

### Components

1. **MCP Server** (TypeScript)
   - Implements Model Context Protocol
   - Exposes tools for OpenL Studio operations
   - Uses STDIO transport for Claude integration

2. **OpenL Studio REST API Client**
   - Communicates with OpenL Studio backend
   - Handles authentication
   - Manages HTTP requests/responses

3. **Configuration**
   - OpenL Studio base URL
   - Authentication credentials
   - Repository settings

### Technology Stack

- **Runtime**: Node.js 16+
- **Language**: TypeScript
- **Framework**: @modelcontextprotocol/sdk
- **HTTP Client**: fetch API or axios
- **Build**: tsx for TypeScript execution

## API Integration

### OpenL Studio REST Endpoints

Based on codebase analysis, the following endpoints will be used:

#### Project Management API (`/user-workspace`)

1. **Open Project**
   - `POST /user-workspace/{repo-name}/projects/{proj-name}/open?open-dependencies={true|false}`
   - Opens a project and optionally its dependencies

2. **Close Project**
   - `POST /user-workspace/{repo-name}/projects/{proj-name}/close`
   - Closes a project and releases resources

3. **Get Project Info**
   - `GET /user-workspace/{repo-name}/projects/{proj-name}/info`
   - Returns project metadata and dependencies

4. **Delete Project**
   - `DELETE /user-workspace/{repo-name}/projects/{proj-name}/delete`
   - Soft delete with comment

5. **Erase Project**
   - `DELETE /user-workspace/{repo-name}/projects/{proj-name}/erase`
   - Permanent deletion

#### Projects API (`/projects`) - BETA

6. **Update Project Status**
   - `PATCH /projects/{projectId}`
   - Updates project status (CLOSED/VIEWING)

### Authentication

OpenL Studio uses session-based authentication. The MCP server will:
1. Authenticate using username/password
2. Maintain session cookies
3. Refresh session as needed

## MCP Tools Design

### Tool 1: list_projects

**Description**: List all available OpenL Studio projects

**Parameters**: None (or optional repository filter)

**Returns**: Array of projects with:
- Project name
- Repository name
- Status (open/closed)
- Last modified date

### Tool 2: open_project

**Description**: Open an OpenL Studio project

**Parameters**:
- `project_name` (required): Name of the project
- `repository_name` (optional): Repository containing the project
- `open_dependencies` (optional, default: true): Whether to open dependencies

**Returns**: Success/failure with project details

### Tool 3: close_project

**Description**: Close an OpenL Studio project

**Parameters**:
- `project_name` (required): Name of the project
- `repository_name` (optional): Repository containing the project

**Returns**: Success/failure message

### Tool 4: export_project

**Description**: Export an OpenL Studio project as ZIP file

**Parameters**:
- `project_name` (required): Name of the project
- `repository_name` (optional): Repository containing the project
- `version` (optional): Specific version to export
- `output_path` (optional): Where to save the ZIP file

**Returns**: Path to exported ZIP file

### Tool 5: copy_project

**Description**: Copy an OpenL Studio project

**Parameters**:
- `source_project` (required): Name of source project
- `source_repository` (optional): Source repository
- `destination_project` (required): Name of new project
- `destination_repository` (optional): Destination repository
- `copy_history` (optional, default: false): Whether to copy version history

**Returns**: Success/failure with new project details

### Tool 6: get_project_info

**Description**: Get detailed information about a project

**Parameters**:
- `project_name` (required): Name of the project
- `repository_name` (optional): Repository containing the project

**Returns**: Project metadata including:
- Dependencies
- Status
- Version information
- Last modified

### Tool 7: list_repositories

**Description**: List all configured OpenL Studio repositories

**Parameters**: None

**Returns**: Array of repositories with names and types

## Configuration

The MCP server will be configured via environment variables or config file:

```json
{
  "openl_studio": {
    "base_url": "http://localhost:8080",
    "username": "user",
    "password": "password"
  }
}
```

## Success Criteria

1. ✅ User can list all projects via natural language
2. ✅ User can open a project by name via natural language
3. ✅ User can close a project via natural language
4. ✅ User can export a project via natural language
5. ✅ User can copy a project via natural language
6. ✅ All operations respect OpenL Studio permissions
7. ✅ Error messages are clear and actionable
8. ✅ MCP server integrates with Claude Desktop

## Future Enhancements

- Support for rule module operations
- Project creation from templates
- Batch operations (open multiple projects)
- Webhook notifications for project changes
- Support for deployment operations
- Integration with CI/CD pipelines

## References

- [Model Context Protocol Specification](https://modelcontextprotocol.io)
- OpenL Tablets REST API (discovered in codebase)
- OpenL Studio Documentation

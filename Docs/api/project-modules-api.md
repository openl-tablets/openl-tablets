# Project Modules API (BETA)

The Project Modules API provides comprehensive REST endpoints for managing modules within an OpenL Tablets project. Modules define the Excel files and method filters that make up a project's rule definitions.

**Base Path:** `/projects/{projectId}/modules`
**Content Type:** `application/json`
**Tag:** `Projects: Modules (BETA)`

---

## Overview

A **module** in OpenL Tablets maps a logical name to a physical Excel file path within a project, optionally with method filtering and compilation settings. Modules are defined in the project's `rules.xml` descriptor.

There are two kinds of modules:
- **Regular modules** &mdash; a fixed `name` + `path` pair (e.g., `Main` &rarr; `Main.xlsx`)
- **Wildcard modules** &mdash; a path pattern with wildcards (e.g., `rules/*.xlsx`) that auto-discovers matching files at compile time

The API supports the full module lifecycle: listing, adding, editing, copying, and removing modules.

---

## Endpoints

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| **GET** | `/projects/{projectId}/modules` | List all modules | 200 |
| **POST** | `/projects/{projectId}/modules` | Add a new module | 201 |
| **PUT** | `/projects/{projectId}/modules/{moduleName}` | Edit an existing module | 200 |
| **POST** | `/projects/{projectId}/modules/{moduleName}/copy` | Copy a module | 201 |
| **DELETE** | `/projects/{projectId}/modules/{moduleName}` | Remove a module | 204 |

---

## Data Models

### ModuleView

Returned for regular (non-wildcard) modules.

```json
{
  "name": "Main",
  "path": "Main.xlsx",
  "methodFilter": {
    "includes": ["MyCalcTest"],
    "excludes": ["*Helper*"]
  }
}
```

| Field | Type | Description |
|-------|------|-------------|
| `name` | `string` | Module name |
| `path` | `string` | File path relative to project root |
| `methodFilter` | `MethodFilterView` | Method filter configuration (omitted if empty) |

### WildcardModuleView

Returned for wildcard modules. Extends `ModuleView` with matched concrete modules.

```json
{
  "name": "Rules",
  "path": "rules/*.xlsx",
  "matchedModules": [
    {"name": "Pricing", "path": "rules/Pricing.xlsx"},
    {"name": "Scoring", "path": "rules/Scoring.xlsx"}
  ]
}
```

| Field | Type | Description |
|-------|------|-------------|
| `name` | `string` | Module name |
| `path` | `string` | Wildcard path pattern |
| `methodFilter` | `MethodFilterView` | Method filter configuration (omitted if empty) |
| `matchedModules` | `BaseModuleView[]` | Concrete modules matching the wildcard pattern |

### MethodFilterView

Optional method filter configuration. Fields are omitted when null.

| Field | Type | Description |
|-------|------|-------------|
| `includes` | `string[]` | Include patterns for method filtering |
| `excludes` | `string[]` | Exclude patterns for method filtering |

### EditModuleRequest

Request body for **add** and **edit** operations.

```json
{
  "name": "MainTests",
  "path": "MainTests.xlsx",
  "includes": ["MyCalcTest"],
  "excludes": null,
  "compileThisModuleOnly": false,
  "createFile": true
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `string` | Yes (non-wildcard) | Module name |
| `path` | `string` | **Yes** | File path relative to project root |
| `includes` | `string[]` | No | Include patterns for method filtering |
| `excludes` | `string[]` | No | Exclude patterns for method filtering |
| `compileThisModuleOnly` | `boolean` | No | If `true`, compile only this module |
| `createFile` | `boolean` | No | If `true`, create an empty Excel file at the path. Only for **add** operation; ignored for edit and wildcard paths. Path must have `.xlsx` or `.xls` extension. |

### CopyModuleRequest

Request body for the **copy** operation.

```json
{
  "newModuleName": "Main-NY",
  "newPath": "Main-NY.xlsx"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `newModuleName` | `string` | Conditional | Required if the target path is **not** covered by a wildcard pattern. Derived from the file name otherwise. |
| `newPath` | `string` | No | Target file path. If omitted, derived from the source path and new module name. |

### CopyModuleResponse

```json
{
  "moduleName": "Main-NY",
  "path": "Main-NY.xlsx",
  "wildcardCovered": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `moduleName` | `string` | Name of the newly created module |
| `path` | `string` | File path of the new module |
| `wildcardCovered` | `boolean` | `true` if the new file is auto-discovered by an existing wildcard pattern (module is not explicitly added to the descriptor) |

---

## Endpoint Details

### GET &mdash; List Modules

Returns all module definitions from the project descriptor. Wildcard modules are expanded to include their matched concrete modules.

```
GET /rest/projects/{projectId}/modules
```

**Response:** `200 OK`

The response is a JSON array where each element is either a `ModuleView` or a `WildcardModuleView`.

**Example Response:**
```json
[
  {
    "name": "Main-CW",
    "path": "Main.xlsx"
  },
  {
    "name": "MainTests",
    "path": "MainTests.xlsx",
    "methodFilter": {
      "includes": ["MyCalcTest"]
    }
  }
]
```

---

### POST &mdash; Add Module

Adds a new module to the project descriptor.

```
POST /rest/projects/{projectId}/modules
Content-Type: application/json
```

**Request Body:** `EditModuleRequest`

**Response:** `201 Created` &mdash; Returns `ModuleView` of the created module.

**Behavior:**
- The module file must already exist at the specified path, **unless** `createFile` is set to `true`.
- When `createFile` is `true`, an empty Excel file (`.xlsx` or `.xls`) is created at the specified path.
- The module name must be unique within the project.
- The path must not conflict with existing modules.

**Example &mdash; Add module with auto-created file:**
```json
{
  "name": "MainTests",
  "path": "MainTests.xlsx",
  "createFile": true
}
```

**Response:**
```json
{
  "name": "MainTests",
  "path": "MainTests.xlsx"
}
```

---

### PUT &mdash; Edit Module

Edits an existing module in the project descriptor. Supports renaming, changing path, updating method filters, and compilation settings.

```
PUT /rest/projects/{projectId}/modules/{moduleName}
Content-Type: application/json
```

| Parameter | Location | Description |
|-----------|----------|-------------|
| `moduleName` | path | Name of the module to edit |

**Request Body:** `EditModuleRequest`

**Response:** `200 OK` &mdash; Returns updated `ModuleView`.

**Example &mdash; Rename module:**
```json
{
  "name": "Main-CW",
  "path": "Main.xlsx"
}
```

**Example &mdash; Add method filter:**
```json
{
  "name": "MainTests",
  "path": "MainTests.xlsx",
  "includes": ["MyCalcTest"]
}
```

---

### POST &mdash; Copy Module

Copies an existing module to a new file path within the same project.

```
POST /rest/projects/{projectId}/modules/{moduleName}/copy
Content-Type: application/json
```

| Parameter | Location | Description |
|-----------|----------|-------------|
| `moduleName` | path | Name of the source module to copy |
| `force` | query | If `true`, skip properties file name pattern validation. Default: `false` |

**Request Body:** `CopyModuleRequest`

**Response:** `201 Created` &mdash; Returns `CopyModuleResponse`.

**Behavior:**
- Creates a physical copy of the source module's Excel file.
- If the target path is covered by an existing wildcard pattern, the module is auto-discovered and `wildcardCovered` is `true`.
- If the target path is **not** wildcard-covered, the module is explicitly added to the descriptor.
- Grants CONTRIBUTOR ACL permission on the new file.

**Example:**
```json
{
  "newModuleName": "Main-NY",
  "newPath": "Main-NY.xlsx"
}
```

**Response:**
```json
{
  "moduleName": "Main-NY",
  "path": "Main-NY.xlsx",
  "wildcardCovered": false
}
```

---

### DELETE &mdash; Remove Module

Removes a module from the project descriptor.

```
DELETE /rest/projects/{projectId}/modules/{moduleName}
```

| Parameter | Location | Description |
|-----------|----------|-------------|
| `moduleName` | path | Name of the module to remove |
| `keepFile` | query | If `true`, the module file is kept on disk. Default: `false` (file is deleted). |

**Response:** `204 No Content`

**Behavior:**
- Removes the module entry from the project descriptor.
- By default (`keepFile=false`), also deletes the associated Excel file from the project.
- For wildcard modules, all matched files are deleted (unless `keepFile=true`).
- Clears associated OpenAPI references if applicable.

---

## Error Handling

All errors are returned as JSON with a `message` field (and optionally a `code` field for i18n):

```json
{
  "message": "Module with such name already exists."
}
```

### Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| **400** | `openl.error.400.cannot.be.empty.message` | Required field is empty (e.g., blank module name) |
| **400** | `openl.error.400.module.path.wildcard.message` | Path contains wildcard symbols (not allowed for add) |
| **400** | `openl.error.400.module.path.invalid.message` | Invalid file path format |
| **400** | `openl.error.400.module.path.pattern.mismatch.message` | File name doesn't match the project's properties file name pattern |
| **400** | `openl.error.400.module.path.not.excel.message` | `createFile=true` but path doesn't have `.xlsx`/`.xls` extension |
| **403** | `openl.error.403.default.message` | Insufficient permissions |
| **403** | `openl.error.403.module.delete.permission.message` | No permission to delete the module file |
| **404** | `openl.error.404.module.not.found.message` | Module with the specified name does not exist |
| **404** | `openl.error.404.module.file.not.found.message` | Module file does not exist at the specified path |
| **409** | `openl.error.409.module.name.exists.message` | A module with the same name already exists |
| **409** | `openl.error.409.module.file.exists.message` | A file already exists at the target path (when `createFile=true`) |
| **409** | `openl.error.409.module.path.conflict.message` | Path conflicts with an existing module |
| **409** | `openl.error.409.module.delete.failed.message` | Failed to delete module file |
| **409** | `openl.error.409.module.create.file.failed.message` | Failed to create the empty Excel file |

---

## Security

All module operations enforce ACL permissions:

| Operation | Required Permission |
|-----------|-------------------|
| List modules | Project READ |
| Add module | Project WRITE + descriptor CREATE (if new) |
| Edit module | Project WRITE |
| Copy module | Project WRITE + CREATE on target path |
| Remove module | Project WRITE + DELETE on module file |
| Create file (`createFile=true`) | CREATE on target path |

---

**Status:** BETA (Experimental)
**Last Updated:** 2026-02-27
**Version:** 6.0.0-SNAPSHOT

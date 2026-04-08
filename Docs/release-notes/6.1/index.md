## Release Notes

[v6.1.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.1.0) on the GitHub

OpenL Tablets 6.1.0 introduces a redesigned rule Trace UI and a new REST-based tracing and rule execution API, making rule debugging available both interactively in the browser and programmatically from external tools. This release also extends the REST API for project module management, improves reliability of concurrent permission assignments, adds Java 26 support, and resolves five bugs including two critical issues.

## **New Features**

### **Rule Trace UI and Trace REST API**

OpenL Tablets 6.1.0 replaces the legacy RichFaces-based trace with a modern React UI and a fully REST-based trace engine. Users can now start a trace on any rule table, navigate the execution tree, and inspect input and output values at every node — all without a page reload. Traces run asynchronously so the Studio UI stays responsive even for large decision tables or spreadsheets.

The new UI shows execution time at each node, highlights slow steps, and keeps the table view and execution tree in sync as you navigate. Parameters can be explored as an interactive tree or raw JSON, and complex values are loaded on demand to avoid loading large objects upfront. Traces can also be exported as a plain-text file for offline analysis.

The REST API (`POST /projects/{projectId}/trace`) supports the same capabilities programmatically, enabling integration with MCP tools, external debuggers, and automated analysis pipelines.

![Trace UI](images/trace-ui.png)

---

### **REST API: Execute a Rule with Test Data**

A new endpoint (`POST /projects/{projectId}/tables/{tableId}/execute`) executes a specified rule using a JSON input payload and returns the result along with execution metadata and any errors. This is intended for MCP integrations, external testing tools, and automation pipelines that need to invoke individual rules without deploying a full Rule Services instance.

## **Improvements**

### **OpenL Studio**

  * Assigning access rights to the same user or group across multiple projects at the same time no longer fails intermittently

---

### **OpenL Core**

  * Added support for Java 25 and Java 26; minimum supported runtime remains Java 21

---

### **OpenL API**

  * Added a module management REST API at `/rest/projects/{projectId}/modules` supporting list, add, edit, copy, and remove operations on project modules, with support for both regular and wildcard module types
  * Added rate factor update operations for lookup tables, supporting the new lookup versioning model introduced in 6.0.0

## **Bug Fixes**

  * Fixed users being able to sync changes into protected branches — this action is now correctly blocked in OpenL Studio
  * Fixed rule tracing failing when a generated data type used a fully qualified class name as its type identifier
  * Fixed the "Save Templates" and "Fill Tags for Project" buttons on the Tags administration page having no visible effect
  * Fixed AI-assisted edits producing a broken table structure when modifying smart rules that use the `collect` option
  * Fixed AI-assisted edits producing a broken table structure when modifying smart lookup tables

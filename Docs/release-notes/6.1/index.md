## Release Notes

[openl-tablets-6.1.0](https://github.com/openl-tablets/openl-tablets/releases/tag/openl-tablets-6.1.0) on the GitHub

OpenL Tablets 6.1.0 delivers significant enhancements to rule debugging and execution analysis through a redesigned Trace UI and a comprehensive Trace REST API, expands the REST API surface for rule execution and project resource management, and introduces AI-assisted formula editing for business analysts. This release also brings Java 26 support, major memory consumption improvements for large rule indices, and a series of critical fixes for performance, stability, and security issues across OpenL Studio and Rule Services.

## **New Features**

### **Trace UI and REST API for Rule Debugging**

OpenL Tablets 6.1.0 introduces a modern, React-based Trace UI that replaces the legacy RichFaces trace implementation, alongside a comprehensive REST API for programmatic trace access. Business analysts and developers can now debug and analyze rule execution interactively in the browser, with full visibility into decision paths, execution time, and input/output parameter values at every node.

The new Trace UI provides rich visual feedback:

  * Execution time visualization with configurable scale — slow nodes highlighted in orange/red with time annotations, fast nodes displayed lightly
  * Input and output parameters shown as both interactive JSON trees and raw JSON
  * Synchronized navigation between the table view and the execution tree
  * Nodes not contributing to the final result rendered in a lighter style for quick identification

The REST API enables external tools and CI/CD pipelines to trigger traces, retrieve node details, and export trace data programmatically, making it straightforward to integrate rule debugging into automated workflows.

![Trace UI](images/trace-ui.png)

---

### **Rule Execution Logging for Tracing and Replay**

Input parameters and output results for OpenL rule executions can now be logged in JSON format, enabling production requests to be replayed in OpenL Studio for tracing, testing, and debugging. This bridges the gap between Rule Services — including Kafka-based deployments — and OpenL Studio, where previously format differences made it difficult or impossible to reproduce production scenarios locally.

  * Structured JSON logging of rule inputs and outputs
  * Logged requests can be directly loaded into OpenL Studio Run/Trace/Test workflows
  * Works with all Rule Services transports: REST, Kafka, and embedded engine

---

### **REST API for Rule Execution with Test Data**

A new REST endpoint allows clients to execute a specified rule from a project module using JSON input data and retrieve the structured result along with any errors. This is especially useful for MCP integrations, automation pipelines, and external testing tools that need to invoke individual rules without a full Rule Services deployment.

  * Execute any rule by project ID and rule table ID
  * Accept JSON input representing rule parameters
  * Return execution output and metadata including error details

---

### **Table Formula Smart Edit**

A new AI-assisted editing mode lets users describe formula changes in natural language. The system interprets the request and proposes a solution expressed in valid OpenL syntax, which the user can review and apply. This reduces the barrier for business analysts working with complex spreadsheet logic who need to modify formulas without knowing the exact syntax.

![Table Formula Smart Edit](images/formula-smart-edit.png)

## **Improvements**

### **OpenL Studio**

  * Improved ACL permission grant concurrency — concurrent grants for the same user or group across different projects no longer fail with a `DuplicateKeyException`; the underlying `acl_sid` creation is now atomic
  * Extended Projects Resources REST API with full module management: list, add, edit, rename, copy, and remove modules from a project descriptor, supporting both regular and wildcard module types
  * Added OpenTelemetry metrics integration to the Studio UI, providing real-time frontend observability alongside existing backend metrics
  * Simplified method filter syntax for included and excluded rules — rule names can now be specified directly without internal notation; `*` wildcard is supported

---

### **OpenL Core**

  * Added Java 25 and Java 26 support; minimum supported runtime remains Java 21
  * Significantly reduced memory consumption of Rules indices for large decision tables by replacing standard Java collections with high-performance alternatives, cutting usage from over 2.3 GB to a fraction of that for large projects

---

### **OpenL API**

  * Extended the REST API with rate factor update operations for lookup tables, supporting the new lookup versioning model introduced in 6.0.0

## **Bug Fixes**

  * Fixed users being able to sync changes into protected branches — this action is now correctly restricted in OpenL Studio
  * Fixed Trace failing with a "Could not resolve type id" Jackson deserialization error when a generated bean type used a fully qualified class name as the `@type` identifier
  * Fixed the Tags administration page "Save Templates" and "Fill Tags for Project" buttons being non-functional with no visible feedback
  * Fixed broken table structure produced by the AI API when updating smart rules using the `collect` option
  * Fixed broken table structure produced by the AI API when updating smart lookup tables
  * Fixed critical performance degradation for projects containing decision tables with many `contains` conditions
  * Fixed REST API GET requests (`/rest/projects`, `/rest/projects/:projectId/tables`) taking up to 14 seconds to respond
  * Fixed inherited data type fields not appearing in the generated OpenAPI schema in Rule Services
  * Fixed selected branch disappearing from the branch dropdown after an OpenL Studio restart or repository configuration change
  * Fixed Spring Boot application failing to start when using the `repo-jar` repository type
  * Fixed project deployment failing with "Unexpected end of ZLIB input stream" when the deployment ZIP contained `.yaml` files
  * Fixed `ClassCastException` in logs when deleting a project from one design repository while a second Azure-type repository was configured
  * Fixed memory leaks in the web application when using Git or Azure repository types
  * Fixed slow project open/close operations on the Repository tab, which could take up to 20 seconds with multiple concurrent users
  * Fixed properties from a secondary repository being displayed in the Admin tab but having no effect
  * Fixed missing credential validation when connecting to a public Git repository configured with an invalid password

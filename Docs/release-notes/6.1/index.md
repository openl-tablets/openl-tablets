## Release Notes

[v6.1.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.1.0) on the GitHub

OpenL Tablets 6.1.0 introduces a redesigned Trace UI and a new REST-based trace and rule execution API, making rule debugging accessible both interactively in the browser and programmatically from external tools. This release also adds AI-assisted formula editing for business analysts, resolves a series of critical stability and performance issues, and extends the REST API surface for project and module management.

## **New Features**

### **Rule Trace UI**

Business analysts and developers can now trace rule execution directly in OpenL Studio using a modern, browser-native trace window. The new interface shows the full execution tree — every rule invoked, the inputs and outputs at each step, and how long each step took — making it straightforward to understand why a rule returned a particular result.

  * Execution time shown at every node, with slow steps highlighted so bottlenecks are easy to spot
  * Inputs and outputs displayed as an interactive tree or raw JSON
  * Nodes that did not contribute to the final result appear in a lighter style for quick filtering
  * Table view and execution tree stay in sync as you navigate

![Trace UI](images/trace-ui.png)

#### **For Developers**

A new Trace REST API allows traces to be triggered, queried, and exported programmatically. This enables integration with CI/CD pipelines, external debugging tools, and automated test workflows without requiring an interactive browser session.

---

### **Rule Execution Logging**

Rule inputs and outputs are now optionally logged in structured JSON, so production requests can be replayed in OpenL Studio for tracing, testing, and debugging. This is especially useful when reproducing issues from Kafka-based deployments or embedded engine setups, where capturing the original request is otherwise difficult.

  * Works with all Rule Services transports: REST, Kafka, and embedded engine
  * Logged payloads load directly into OpenL Studio Run, Trace, and Test workflows

---

### **REST API: Execute a Rule with Test Data**

A new endpoint allows a specific rule in a project module to be executed with a JSON input payload, returning the result and any errors. This is intended for MCP integrations, external testing tools, and automation pipelines that need to call individual rules without deploying a full Rule Services instance.

#### **For Developers**

Execute rules by project ID and rule table ID. The endpoint accepts standard JSON input and returns structured output including execution metadata and error details.

---

### **Table Formula Smart Edit**

Business analysts can now describe a formula change in plain language — for example, "increase the base rate by 15% for platinum tier" — and OpenL Studio will propose a valid OpenL expression to review and apply. This removes the need to know the exact syntax when modifying spreadsheet or decision table formulas.

  * Works with decision tables, spreadsheets, and lookup tables
  * Proposed change is shown as a preview before applying
  * Original formula is preserved until the user confirms

![Table Formula Smart Edit](images/formula-smart-edit.png)

## **Improvements**

### **OpenL Studio**

  * Assigning access rights to the same user or group across multiple projects at the same time no longer fails intermittently
  * Simplified method filter syntax for included and excluded rules — rule names can now be entered directly without special notation; `*` wildcard is supported

---

### **OpenL Core**

  * Added support for Java 25 and Java 26; minimum supported runtime remains Java 21
  * Significantly reduced memory usage for large decision table rule sets — projects that previously required over 2 GB now use a fraction of that

---

### **OpenL API**

  * Added module management operations to the Projects REST API: list, add, edit, rename, copy, and remove modules from a project descriptor, with support for both regular and wildcard module types
  * Added rate factor update operations for lookup tables to the REST API, supporting the new lookup versioning model introduced in 6.0.0
  * Integrated OpenTelemetry metrics into the Studio UI, enabling frontend observability alongside existing backend metrics

## **Bug Fixes**

  * Fixed users being able to sync changes into protected branches — this action is now correctly blocked in OpenL Studio
  * Fixed the "Save Templates" and "Fill Tags for Project" buttons on the Tags administration page having no visible effect
  * Fixed rule tracing failing for projects that use generated data types after the type resolution changes introduced in 6.0.0
  * Fixed AI-assisted edits producing a broken table structure when modifying smart rules that use the `collect` option
  * Fixed AI-assisted edits producing a broken table structure when modifying smart lookup tables
  * Fixed severe performance degradation in Rule Services for projects with decision tables containing many `contains` conditions
  * Fixed REST API requests taking up to 14 seconds to respond under normal load
  * Fixed inherited data type fields not appearing in the OpenAPI schema generated by Rule Services
  * Fixed the selected branch disappearing from the branch selector after restarting or reconfiguring OpenL Studio
  * Fixed Spring Boot applications with a JAR-based repository failing to start
  * Fixed deployment failing when a ZIP deployment package contained YAML configuration files
  * Fixed an error appearing in logs when deleting a project while an Azure-type repository was also configured
  * Fixed memory leaks that occurred when using Git or Azure repository types
  * Fixed project open and close operations on the Repository tab taking up to 20 seconds with multiple concurrent users
  * Fixed settings from one design repository incorrectly appearing in the Admin panel for a different repository
  * Fixed no error being shown when invalid credentials were entered for a public Git repository

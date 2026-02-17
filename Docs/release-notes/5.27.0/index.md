## Release Notes

[v5.27.0](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.0) on the GitHub

OpenL Tablets 5.27.0 introduces Access Control Lists for fine-grained permission management, intelligent Excel merge conflict resolution, and expression referencing in decision tables. This release focuses on strengthening security and authorization capabilities in OpenL Studio while improving the rule authoring experience.

Additional highlights include migration to OpenTelemetry for observability in Rule Services, integration of RapiDoc as the new API documentation viewer, and several usability improvements across the platform.

### New Features

Access Control Lists (ACL)

OpenL Studio now provides a robust, granular mechanism for controlling user access to assets. The new ACL system implements comprehensive subject-on-object permission control, enhancing security, authorization, and user identification across the platform. ACL management is available through the built-in REST API tool, with full documentation accessible at `webstudio/rest/api-docs`.

Merge Conflicts in Excel Files

Branch synchronization now handles conflicting Excel files intelligently. When conflicts are detected, the system retrieves revisions from the current branch, target branch, and base revision, then compares each conflicting file on a sheet-by-sheet basis. Non-conflicting sheets are merged automatically, reducing manual resolution effort.

Expression Referencing for Decision Tables

Decision tables now support referencing expressions directly using the syntax `$Expr.$C1` or `$Expr.$C1.param1`. This enables more flexible rule composition and reuse of expressions across table columns.

### Improvements

OpenL Studio:

  * Added "Copy full path" link for streamlined ACL permission granting via API
  * Added property to define default ordering mode for table organization
  * Added ability to view technical revisions on the Revisions tab
  * Table list now matches source Excel sequence in 'by File' ordering mode
  * Updated button order for Deploy Configurations repository

Rule Services:

  * Integrated RapiDoc as the API documentation viewer, replacing Swagger UI
  * Completed migration to OpenTelemetry for observability and tracing

### Fixed Bugs

OpenL Studio:

  * Resolved "Something went wrong" error in the Revision section for local projects
  * Fixed blank white screen appearing upon login

### Updated Libraries

  * prototype.js 1.7.3

### Known Issues

  * Operations by users with many groups take approximately 40% longer than for users with 1-2 groups
  * Users in the "ADMIN" group automatically receive the Administrate privilege

### Migration Notes

The user database schema has been updated. Back up the database before upgrading.

The following deprecated classes and methods have been removed:

  * `org.openl.rules.helpers.InOrNotIn`
  * `org.openl.rules.helpers.IDoubleHolder`
  * `org.openl.rules.helpers.DoubleHolder`
  * `org.openl.rules.helpers.DoubleRange` methods: `intersect`, `compareLowerBound`, `compareUpperBound`
  * `org.openl.rules.helpers.DateRange` methods: `getUpperBoundType`, `getLowerBoundType`

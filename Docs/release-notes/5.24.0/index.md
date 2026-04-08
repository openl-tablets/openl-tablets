---
title: OpenL Tablets 5.24.0 Release Notes
date: 2021-03-11
description: Introduced multiple design repositories, OpenAPI project creation and import, cluster mode, notifications,
    Apache Groovy support, and a Maven verify goal, with numerous improvements and bug fixes across WebStudio and Rule Services.
---

OpenL Tablets **5.24.0** is a major feature release that introduces support for multiple design repositories,
OpenAPI-based project creation and import, cluster mode for WebStudio, a notification system, Apache Groovy support, and
a Maven verify goal. This release includes significant improvements to WebStudio and Rule Services workflows and
requires migration action before upgrading.

## New Features

### Multiple Design Repositories in WebStudio

WebStudio now supports working with multiple design repositories simultaneously. This includes:

* A redesigned Administration tab UI for managing multiple repositories.
* Updated Create Project and Copy Project dialogs, as well as project properties, to support repository selection.
* A repository filter in the advanced filter.
* Updated UI for adding projects to deployment configurations.
* Projects with identical names can exist across different repositories; only one can have open status at a time.
* Ability to copy projects between repositories.
* For Git repositories with non-flat folder structures: folder configuration file removed; an "Explorer" style path
  window introduced; project import via "Create project from Repository"; erase options for complete removal or
  retention in repository; and project renaming in the Rules Editor.

---

### Creating a Project from an OpenAPI File

Users can now create WebStudio projects directly from OpenAPI files in `.json`, `.yaml`, or `.yml` format.

---

### Importing an OpenAPI File into an Existing Project

An **OpenAPI Import** feature has been added to the project overview in the Rules Editor. Two import methods are
available:

* **Scaffolding**: Generates new modules from the defined OpenAPI file, or re-generates existing modules.
* **Reconciliation**: Validates existing modules against the defined OpenAPI file and displays OpenAPI Reconciliation
  errors if validation fails.

---

### OpenAPI Reconciliation

Projects containing OpenAPI files are now validated against the OpenAPI schema, with errors displayed upon validation
failure.

---

### Cluster Mode Support in WebStudio

Administrators can configure cluster mode, allowing the same content to be accessed from multiple WebStudio instances
using a shared FileStorage, project index, project history, and `webstudio.properties` file.

---

### Notification System

A user notification system has been implemented for announcements such as settings updates. Notifications are manageable
via the Admin tab.

---

### Configurable SpreadsheetResult Output Model Attribute Names

Properties can be added to the rules deploy configuration to apply Spring Jackson property naming strategies for
`SpreadsheetResult` output in Rule Services.

---

### Apache Groovy Support

Apache Groovy scripts are now supported in OpenL Tablets projects, providing an easier alternative to Java classes for
custom logic stored in the repository.

---

### 'verify' Goal Added to openl-maven-plugin

A new `verify` goal ensures that an OpenL Tablets packed project contains all libraries necessary for Rule Services
deployment.

---

### Deployment of Zipped Projects from File System Repository to Rule Services

Zipped projects can now be deployed from a file system repository to Rule Services using `application.properties` file
properties.

---

## Improvements

### WebStudio

* Removed the default `admin/admin` user. A new **Configure initial users** section allows manual configuration of
  initial users.
* Administrator and view-access users are now configurable via `security.administrators` and `security.default-group`
  properties.
* Added project and file export from workspace via **Export from Repository**; specific revision export is available via
  **Export from Editor**.
* The "Changes" page has been renamed to **Local Changes** with an improved UI showing history since the last saved
  revision. The "Current" label replaces "Restore" in the Action column; restored old versions are marked "Current"
  without creating new rows.
* Added a warning message on the "Compare Excel files" page when the compared files are identical.
* Project revision history is now viewable in the Rules Editor.
* Default local repository paths have changed; the deploy configuration repository is now stored outside the design
  repository by default.
* Local project behavior changed: creating or copying a project with an existing name creates a "Closed" status project
  while the "Local" project remains unchanged.
* The "Run test cases of the test in parallel" checkbox has been removed; administrators must set **Thread number for
  tests** to `1` to disable parallel execution.
* The **Delete** button on non-default branches has been renamed to **Delete branch**; deleted non-default branches
  cannot be restored.
* Removed the 100 MB file upload size limitation for the Create Project dialog.

### Rule Services

* Publishing SOAP services is now switched off by default.
* The `ruleservice.datasource.filesystem.supportDeployments` property has been deleted.
* Deployed service uniqueness is now determined by **Deploy path** instead of **Service name**. Naming follows the
  format `deployment_configuration_name/project_name`.
* Lazy instantiation is now disabled by default.

## Bug Fixes

### Core

* Fixed same-named data tables being randomly selected without displaying an error.
* Fixed condition heights in smart lookup tables not being identified correctly.
* Fixed 2D arrays displaying the "Method is ambiguous" error when one option is obviously more suitable.

### WebStudio

* Fixed no error message being presented for duplicated data tables.
* Fixed log warning messages appearing when deleting all Excel files from `rules.xml` projects.
* Fixed an informative error message not being displayed on table column deletion.
* Fixed the "Edit Project" pop-up being corrupted by lengthy file pattern validation messages.
* Fixed projects randomly disappearing when adding and returning to them after deletion in the Deploy Configuration
  project list.
* Fixed "Compare Excel files" not displaying the current result after the window is reopened.
* Fixed users being able to upload multiple files with identical names.
* Fixed the "Sorry! Something went wrong" error appearing when running JSON.
* Fixed module lists in projects not being sorted.
* Fixed 2D arrays in smart rules displaying the "Method is ambiguous" error when one option is obviously more suitable.
* Fixed incorrect compilation messages being displayed for static fields.
* Fixed condition heights in smart rules not being identified correctly.
* Fixed the RollBack action for deleted `RET` columns in decision tables.
* Fixed the **Compare** button becoming disabled after clicking **Clear all**.

### Rule Services

* Fixed performance degradation.
* Fixed the `master` branch name not being displayed correctly in `MANIFEST.MF`.
* Fixed a second deployment with equal deployment names failing.

### Repository

* Fixed a "Merge" revision unrelated to the project appearing in the revision history.
* Fixed the upload merged file window not being functional after applying changes without uploading.
* Fixed a project being deleted after a page refresh following file deletion and project save.
* Fixed the **Clear** and **Clear all** buttons not being functional in the Files section after adding and uploading
  files.

## Library Updates

| Library                | Version          |
|:-----------------------|:-----------------|
| JSF                    | 2.3              |
| RichFaces              | 4.6.8            |
| Jetty                  | 9.4.38.v20210224 |
| Jakarta Activation API | 1.2.2            |
| Mojarra                | 2.3.9            |

## Known Issues

* On demo startup on a Linux server, the following error may appear: "Cannot build repository tree. Repository 'Deploy
  Configuration': Repository configuration is incorrect. Please change configuration."
  **Workaround**: In **Admin > Repository > Deploy_Configuration**, set **Use Design Repository** to `true`, or set
  `repository.deploy-config.use-repository=design` in `webstudio.properties`.

* Modules and files cannot be exported from WebStudio if they are not in the root project folder.
  **Workaround**: Export the entire project, or relocate the file to the root folder before exporting.

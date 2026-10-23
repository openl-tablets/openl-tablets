---
title: "OpenL Tablets 6.4.0 Migration Notes"
---

Upgrading to OpenL Tablets 6.4.0 requires no database changes and no changes to rules content. Two behavior changes
affect everyone who works with the Design repository — project deletion and Git commit messages — and several
administrator-facing settings and screens are removed. Workspace metadata is migrated automatically on first start.

## Rules Authors

* **Deleting a project is permanent.** The Archive, Undelete and Erase actions are gone, and so is the
  **Hide deleted projects** filter. Deleting a project removes it from your workspace and from the current state of
  the Design repository. For Git repositories the deletion is stored as an ordinary delete commit, so the project can
  still be recovered from repository history by an administrator, but there is no Undelete action in OpenL Studio.
  Confirm any project you were keeping in the archived state before upgrading. A new project may now reuse the name
  of a deleted one, which the archived state previously blocked.
* **Trace opens in the business view.** Existing traces behave the same, but the screen now leads with a view aimed
  at rule authors; the previous debugger chrome is behind the **Advanced** switch. No configuration is involved.
* **The Repository screens are now the React Projects and Deployments tabs.** The legacy JSF pages are removed;
  replace any bookmarks or deep links into them. The actions you used are all present, with project actions collected
  into an overflow menu.
* **Manual project import is removed.** Projects in the Design repository are detected automatically, so there is no
  import step to perform.
* Existing projects, tables and tests are otherwise unaffected. No re-save or re-compile is required.

## Administrators

* **Remove the Git commit message template.** The `comment-template` property is no longer read, and its
  `{user-message}`, `{commit-type}`, `{project-name}` and `{revision}` placeholders no longer exist. Delete the
  following lines from your properties file if present:

  ```properties
  repo-git.comment-template = {user-message} Type: {commit-type}.
  repo-default.design.comment-template = {user-message} Type: {commit-type}.
  ```

  Git repositories now store the comment the user entered as the commit message, unchanged. If your tooling parses
  commit messages for the `Type:` envelope, update it before upgrading — commits created by 6.4.0 will not carry it.
  The **User message pattern** and **Invalid user message hint** settings are unchanged and still validate what a
  user may enter.

* **Review the per-operation comment templates.** The **Archive project**, **Restore project** and **Erase project**
  templates are removed and replaced by a single **Delete project** template. **Save project**, **Create project**,
  **Copy project** and **Restore from old version** are unchanged.

* **Switch Kubernetes probes to the health check endpoints.** OpenL Studio serves `GET /healthcheck/startup` and
  `GET /healthcheck/readiness` without authentication. Using them removes the fixed `initialDelaySeconds` guesswork
  from the manifests:

  ```yaml
  startupProbe:
    httpGet:
      path: /healthcheck/startup
      port: 8080
    periodSeconds: 10
    failureThreshold: 30
  livenessProbe:
    tcpSocket:
      port: 8080
    periodSeconds: 15
    failureThreshold: 4
  readinessProbe:
    httpGet:
      path: /healthcheck/readiness
      port: 8080
    periodSeconds: 10
    failureThreshold: 3
  ```

  Any other path under `/healthcheck` remains behind the security filter and redirects to the login page.

* **Check project descriptors against the 1 MB limit.** A `rules.xml` larger than 1 MB is refused, where the previous
  limit was 16 MB. A descriptor of that size is almost always generated content; split or trim it before upgrading.

* **The username in Single-User mode comes from the operating system.** In Single-User mode OpenL Studio signs in
  the account that runs the server, where it signed in `DEFAULT` before. To keep the previous username, set it
  explicitly:

  ```properties
  security.single.username = DEFAULT
  ```

* **Workspace metadata migrates on first start.** The `.studioProps` migration always runs so local workspaces
  survive the upgrade, and the Single-User workspace is moved to the resolved username. No manual step is required;
  allow for the migration on the first start after the upgrade.

* **Project permissions are evaluated per branch.** Authorization is applied separately to each branch membership of
  a project, and against the path a project is mapped to. Review access control lists for repositories where a
  project exists on several branches with different paths.
* **Revoking access now takes effect immediately.** A revoked project is no longer converted to a local copy that the
  user keeps working in; the first project listing after the revocation evicts the opened copy — files, metainfo
  record and lock — from that user's workspace. Any unsaved work the user had in a revoked project is discarded, so
  communicate revocations that matter.

## Developers

* **The AI and gRPC search integration is removed.** The obsolete Studio AI search integration is gone, gRPC is no
  longer a dependency of OpenL Tablets, and `OpenL2TextUtils` — used only for text-based search indexing — is
  removed. If your build inherited gRPC transitively from OpenL, declare it yourself. If your code called
  `OpenL2TextUtils`, inline the conversion you need.
* **The Trace REST API is new.** The interactive debugger is driven by `/rest/projects/{projectId}/trace`, with
  `POST /step`, `POST /resume`, `POST /pause`, `PUT /breakpoints`, `GET /watches` and frame-scoped reads. The
  endpoints are additive; existing run and test endpoints are unchanged.
* **Subscribe to the change topics for live updates.** The STOMP endpoint is unchanged (`/web/ws` for session auth,
  `/rest/ws` for Basic, PAT or Bearer). Three topics now carry change notifications:
  `/topic/projects/changed` (broadcast ping), `/user/topic/workspace/changed` (per-user draft ping) and
  `/user/topic/workspace/projects/status` (per-project compilation status). Broadcast topics carry no project data,
  so clients must re-read through REST, where access control applies.
* **Uploaded content is verified for completeness.** Workbooks and archives that lost their tail are refused by the
  upload APIs with a `400` response rather than being stored. Clients that streamed partial content and relied on it
  being accepted will now see the failure at upload time.
* **The table creation API is unchanged**, and the rebuilt Create Table modal submits through it. Table copying now
  runs on the server by table id instead of re-posting the table content.

## Environment and Dependency Changes

* No Java version change. No SQL migration is required.
* Micrometer (`1.17.0`) and lz4-java (`1.11.2`) are now pinned explicitly to address CVE-2026-40984 and
  CVE-2026-59949. If your build overrides these transitive dependencies, align your overrides with these versions.
* Jetty moves to `12.1.12` and Netty to `4.2.17.Final`. Deployments that embed OpenL Studio in their own servlet
  container should confirm compatibility with these versions.
* The frontend toolchain moves to Node.js `v24.18.0` and npm `12.0.1`. This affects only builds from source.

## Testing Recommendations

After upgrading, verify in a non-production environment:

1. A project deletion, and that the deletion appears in the Git history of the Design repository.
2. A commit made from OpenL Studio, confirming the stored Git message is exactly what was entered.
3. A project that exists on more than one branch, confirming it is discovered and that permissions apply per branch.
4. An upload of a workbook and of an archive, confirming both are accepted and that a truncated file is refused.
5. The startup and readiness endpoints, if your deployment is orchestrated.

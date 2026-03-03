---
title: OpenL Tablets 6.0.0 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Rules Author** → pay special attention to sections **3, 4**
* **If you are a Developer** → pay special attention to sections **2, 3, 5**
* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2, 6**

---

### 1. Access Control & Permissions

* The legacy permission model has been replaced with role-based ACL.
  Permissions are migrated automatically, but effective access may differ after upgrade.
* `security.default-group` no longer grants implicit READ access to repositories.
  Administrators must explicitly configure permissions for the default group after upgrade.
* Project creation and deletion are additionally controlled by the system property:

```properties
security.allow-project-create-delete=true  # Default (allows creation)
security.allow-project-create-delete=false # Hides create/delete buttons
```

* Role assignments and default group permissions should be reviewed after upgrade.

#### User Access & Permission Mapping

| Legacy Permission | New Behavior | Notes / Action |
| :---- | :---- | :---- |
| **View Projects** | Included in all roles | Deprecated as a standalone permission; project viewing is now available to all roles by default |
| **Run and Trace Tables** | System action | No longer permission-restricted; available to all users regardless of role |
| **Edit Projects** | Included in Contributor role | Covered by the Edit permission |
| **Create Projects** | Separate Create permission | Included in Contributor and Manager roles; can be globally restricted |
| **Delete Projects** | Separate Delete permission | Included in Contributor and Manager roles |
| **Erase Projects** | Merged into Delete | Delete and erase are now the same action |
| **Lock / Unlock Projects** | Removed | Project locking functionality was deprecated |
| **Deploy Configuration (Create/Edit/Delete/Erase)** | System action | Available to users with Viewer access to Design repo and Edit access to Deploy repo |

---

### 2. Runtime Environment

* Java **21** is required. Earlier Java versions are no longer supported.
* All `javax.*` packages have been replaced with `jakarta.*`.
  Custom Java code must be updated accordingly and all dependencies must be Jakarta EE 10 compatible.

---

### 3. Rules & Rule Execution

* The **variations** feature has been completely removed.
    * Variation-based endpoints are no longer created.
    * Rules and services relying on variations must be rewritten using array-based or explicit rule logic.
* The default value of `isProvideRuntimeContext` is now **false**.
    * Rules that relied on implicit runtime context (for example, `currentDate`, `requestId`) may receive `null` unless enabled explicitly.

---

### 4. Metadata & OpenAPI

* Project tags are now stored inside the project structure.
    * Tags are migrated automatically.
    * Tag changes are version-controlled and visible in Git history.
* Rule and datatype descriptions are automatically exposed in generated OpenAPI documentation.

---

### 5. Integration & Serialization

* JSON polymorphic serialization now uses **NAME-based** type identification instead of **CLASS-based**.
    * Client applications relying on CLASS-based typing may require updates.
* OAuth2 authentication now uses the `sub` claim as the default username attribute.
    * Existing OAuth2 integrations should be verified after upgrade.
* Embedded JDBC drivers are no longer included.
    * Required database drivers must be provided explicitly.

---

### 6. Administration, Deployment & Removed Features

* The following components and protocols are no longer supported:
    * RMI
    * CAS SSO
    * Legacy APIs
    * Install Wizard
* The Administration UI has been fully redesigned.
    * No functional loss; administrators should familiarize themselves with the new layout.
* Demo distribution and deployment packaging have changed.
    * No breaking impact for production systems; primarily affects evaluation and onboarding scenarios.

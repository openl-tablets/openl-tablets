---
title: "OpenL Tablets 6.3.0 Migration Notes"
---

Upgrading to OpenL Tablets 6.3.0 requires no database changes and no code changes for rules projects. One behavior
change affects SAML deployments, and a few new options are available for administrators and developers.

## Rules Authors

No action required. Existing projects, tables, and tests are unaffected.

## Administrators

* **SAML Single Logout** — In `saml` mode, logout now sends a SAML `LogoutRequest` to the identity provider's Single
  Logout Service and ends the IdP session, instead of clearing only the local OpenL Studio session. No configuration
  change is needed in OpenL Studio. Confirm that your identity provider's metadata advertises a Single Logout Service
  endpoint, and verify the end-to-end logout flow after upgrading.
* **JAR repository location (optional)** — Deployment archives for the JAR local repository can now live outside the
  application classpath. The search location is controlled by the `repo-jar.location` property, which accepts a Spring
  resource pattern and defaults to `/openl/*.zip`. Leave it unset to keep the previous behavior, or point it at an
  external location such as a mounted Docker volume:

  ```properties
  repo-jar.location = file:./openl/*.zip
  ```

## Developers

* **Personal Access Tokens in AD and multi-user modes** — PAT authentication is now available in `ad` and `multi`
  modes in addition to `oauth2` and `saml` (every mode except `single`). The Personal Access Tokens screen appears
  automatically, and tokens can be used for service-to-service REST access as an alternative to interactive login.
  Existing tokens are unaffected.
* **Table source editing API** — The new `POST /rest/projects/{projectId}/tables/{tableId}/actions` and
  `DELETE /rest/projects/{projectId}/tables/{tableId}` endpoints are additive. Existing table read and update endpoints
  are unchanged.

## Environment and Dependency Changes

* Apache HttpClient5 (`5.6.2`), Apache HttpCore5 (`5.4.3`), and Spring LDAP (`3.3.8`) are now pinned explicitly. If your
  build overrides these transitive dependencies, align your overrides with these versions.
* No Java version change. No SQL migration is required.

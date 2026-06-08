---
title: OpenL Tablets 6.1.1 Release Notes
date: 2026-06-08
description: Patch release fixing a database upgrade failure caused by Flyway checksum changes, a Jakarta Activation
    dependency conflict for BOM consumers, and stale project compilation status after a table edit.
---

OpenL Tablets **6.1.1** is a patch release that resolves three issues found in 6.1.0: a database upgrade failure, a
dependency conflict for projects using the OpenL Tablets BOM, and a stale project status after editing a table. No
configuration or migration changes are required.

## Bug Fixes

* Fixed a database migration failure when upgrading from a previous OpenL Studio version. Reformatted Flyway migration
  scripts changed their checksums, so Flyway detected a mismatch against the already-applied migrations and aborted
  startup.
* Fixed a `NoClassDefFoundError: jakarta/activation/DataSource` for projects that import the OpenL Tablets BOM together
  with `jackson-module-jaxb-annotations`. The BOM now manages `jakarta.activation:jakarta.activation-api` at `2.1.4`,
  overriding the legacy `javax`-namespace `1.2.2` that Jackson pulled in transitively and that lacked the class.
* Fixed the project status API returning a stale compilation result after a table edit. Editing a table through the REST
  API now invalidates the cached compilation status, so `GET /projects/{projectId}/status` no longer reports outdated
  `ok` or `errors` results computed against the previous version of the sources.

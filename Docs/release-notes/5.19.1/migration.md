---
title: OpenL Tablets 5.19.1 Migration Notes
---

## Rule Services

Add `PropertySourcesLoader` initialization to `web.xml` and merge properties files into a single
`application.properties` file.

## Maven Plugin

* `JavaWrapperAntTask` has been replaced with `openl-maven-plugin`.
* Repository factory class names have been updated — update any references in your build configuration.

## Core

* JCL (Jakarta Commons Logging) framework dependencies have been removed.
* `RulesEngineFactory(File)` is deprecated — use the `URL` or `String` constructor alternatives instead.

## OpenL Rules

* Add `rules.xml` to any deprecated Eclipse-based projects.
* Review `autoType` property settings — Auto Type Discovery is now enabled by default.
* Remove deprecated imports from Environment tables.

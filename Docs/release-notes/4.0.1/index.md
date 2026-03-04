---
title: OpenL Tablets 4.0.1 Release Notes
date: 2007-06-11
description: Introduces Unit Tests, Run, Trace, Explanation, and Advanced Search in WebStudio.
    Improves Excel-format rendering for numbers and dates, configures session timeout,
    and changes the == operator for Strings to use equals() semantics.
---

OpenL Tablets **4.0.1** is a major release introducing key WebStudio features and language semantics changes.

## Contents

* [New Features](#new-features)
* [Improvements](#improvements)

## New Features

### WebStudio: Unit Tests, Run, Trace, Explanation, and Advanced Search

WebStudio now includes:

* **Unit Tests** — run test tables and view results interactively.
* **Run** — execute rules directly from the browser.
* **Trace** — step through rule execution to inspect intermediate values.
* **Explanation** — view explanations of how results were calculated.
* **Advanced Search** — search across rules projects with extended criteria.

## Improvements

* WebStudio now displays Excel tables using Excel formats for numbers, dates, and borders. Some compatibility
  differences between Excel and HTML rendering may remain.
* Tomcat default session timeout is configured for 30 minutes in `conf/web.xml`. WebStudio displays a message and
  restarts the web application after a session timeout.
* The semantics of the `==` operator for `String` values are changed: when both operands are non-null, `equals()` is
  used for comparison instead of reference equality. Null values are still handled correctly.
* Business View and Developer View display modes are available. WebStudio defaults to Business Mode; switch via the "Web
  Studio" text in the top menu.

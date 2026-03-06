---
title: OpenL Tablets 5.18.2 Release Notes
date: 2016-09-27
description: Adds retry logic for failed production repository connections and fixes unstable deployment of large projects.
---

OpenL Tablets **5.18.2** is a patch release with an improvement and a bug fix.

## Improvements

* Added additional retry attempts to redeploy a project to a production repository (stored in a database or JCR
  repository) in case of a failed connection.

## Bug Fixes

* Fixed: Unstable deployment of large projects to the production repository caused by ModeShape library behavior.

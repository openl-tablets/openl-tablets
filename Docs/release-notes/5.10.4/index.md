---
title: OpenL Tablets 5.10.4 Release Notes
date: 2014-03-21
description: Patch release fixing 2 web services bugs related to out-of-memory errors during parallel requests at startup and a race condition.
---

OpenL Tablets **5.10.4** is a patch release containing bug fixes.

## Bug Fixes

**Web Services:**

* Fixed: `OutOfMemoryError` exception when multiple parallel rule requests arrive simultaneously at service startup.
* Fixed: Race condition affecting the service.

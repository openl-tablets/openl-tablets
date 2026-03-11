---
title: OpenL Tablets 5.25.4 Release Notes
date: 2021-12-13
description: Fixed empty response when using @StoreLogDataToDB with sync=true, and updated Log4j.
---

OpenL Tablets **5.25.4** is a maintenance release that fixes an empty response issue with synchronous log data storage
and updates Log4j.

## Bug Fixes

### Rule Services

* Fixed an empty response being returned when `@StoreLogDataToDB` is used with `sync=true`.

## Library Updates

| Library | Version |
|:--------|:--------|
| Log4j   | 2.15.0  |

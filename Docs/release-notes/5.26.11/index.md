---
title: OpenL Tablets 5.26.11 Release Notes
date: 2023-08-25
description: Removed Google Universal Analytics integration, fixed trace and sensitive data logging issues,
    and library updates.
---

OpenL Tablets **5.26.11** is a maintenance release that removes Google Universal Analytics integration, fixes trace and sensitive data logging issues, and includes library updates.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)
* [Library Updates](#library-updates)

## Improvements

### WebStudio

* Google Universal Analytics integration has been removed because of the end of support.

## Bug Fixes

### WebStudio

* Fixed internal server error appearing if a user entered a JSON request in "Trace into File."

### Maven Plugin

* Fixed Maven plugin logging sensitive data.

## Library Updates

| Library          | Version      |
|:-----------------|:-------------|
| Spring Security  | 5.8.6        |
| Kafka Clients    | 3.5.1        |
| Snappy Java      | 1.1.10.3     |
| Cassandra Driver | 4.17.0       |
| ZooKeeper        | 3.9.0        |
| Netty            | 4.1.96.Final |
| Reactor Netty    | 1.1.10       |
| Swagger UI       | 5.4.2        |
| Amazon AWS SDK   | 1.12.533     |
| MSSQL Driver     | 12.4.0.jre11 |
| Commons Lang3    | 3.13.0       |
| BouncyCastle     | 1.76         |
| Guava            | 32.1.2-jre   |
| AspectJ          | 1.9.20       |
| Testcontainers   | 1.19.0       |
| Okio             | 3.5.0        |

---
title: OpenL Tablets 5.26.6 Release Notes
date: 2023-03-08
description: DEDUCTION mode for JSON serialization, AWS Security Token Service support,
    multiple bug fixes across Core, Rule Services, WebStudio, and Docker.
---

OpenL Tablets **5.26.6** is a maintenance release introducing DEDUCTION mode for JSON serialization, AWS Security Token Service support, multiple bug fixes, and library updates.

## Improvements

### Rule Services

* `DEDUCTION` mode is supported for serialization/deserialization of JSON response/request.

### Repository

* Support for AWS Security Token Service.

## Bug Fixes

### Core

* Fixed no error being displayed during the project compilation if the constructor accepts an invalid expression as input data.
* Fixed a duplicated error message being displayed if there is a reference to a non-existing condition field.

### Rule Services

* Fixed an incorrect field name displayed in the Swagger schema for Spreadsheet if there is a datatype with the same name.
* Fixed Rule Services not being initialized in Spring Boot due to a circular dependency.

### WebStudio

* Fixed the project being erased in the "Projects to Deploy" tab after clicking the "Save" button in deploy configuration (displaying when Deploy Config type = JDBC, JNDI, AWS).
* Fixed dependency projects from another repository not being suggested for opening.

### Demo

* Fixed changes that are not stored in the repository being lost after the DEMO restart.

### Maven Plugin

* Fixed the `verify` goal failing due to two-way dependencies between the Maven plugin classloaders.

### Docker

* Fixed Docker containers failing to start on Docker Desktop 4.17 and Amazon EKS.

## Library Updates

| Library                | Version      |
|:-----------------------|:-------------|
| Spring Security        | 5.8.2        |
| Spring Boot            | 2.7.9        |
| Nimbus JOSE + JWT      | 9.31         |
| ASM                    | 9.4          |
| Kafka Clients          | 3.4.0        |
| ZooKeeper              | 3.8.1        |
| Netty                  | 4.1.89.Final |
| Reactor Netty          | 1.1.3        |
| Log4j                  | 2.20.0       |
| Jetty                  | 10.0.14      |
| Amazon AWS SDK         | 1.12.416     |
| Azure Blob Storage SDK | 12.21.0      |
| MSSQL Driver           | 12.2.0.jre11 |
| Hibernate              | 5.6.15.Final |
| Groovy                 | 3.0.15       |

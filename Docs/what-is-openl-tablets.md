---
title: What Is OpenL Tablets?
description: Overview of OpenL Tablets — an open source business rules engine for managing complex business logic in Excel.
---

# What Is OpenL Tablets?

**OpenL Tablets** is an open source business rules engine (BRE/BRMS) and decision management system. It consists of the
following major components:

- **_Business Rules Engine_** — powerful and business-friendly
- **_OpenL Studio_** — web-based rules editing and management environment
- **_Rule Services_** — framework and application to execute rules as services, such as RESTful services, Kafka
  consumer/producer, and implementation provider for Java interfaces
- **_Rules Repository_** — an enterprise-class rules repository implementation using Git, databases, and blob storage

## Approach

**OpenL Tablets** provides an unorthodox approach to writing and managing **business rules** that allows users to keep
executable business rules as close as possible to the original source. At the same time, it features a highly scalable,
executable rules engine, integration with modern software systems, and a choice of web-based or Excel-based authoring
environments.

---

Business rules and policies most frequently exist as an unstructured set of Excel and Word documents. There are two very
important characteristics of these documents:

1. They reflect as closely as possible the intent of business users. We see numerous examples of high-quality documents
   created by business people for business people. This is their "business speak" — the world where they are comfortable
   and proficient.

2. Unfortunately, these documents are not suitable for automatic or semi-automatic generation of executable business
   logic. The common approach is to invite a group of business analysts to transform these documents so that the
   business logic becomes understandable to software developers.

The result is usually a "scrambled eggs syndrome" — the produced software bears no resemblance to the initial business
user's intent. Furthermore, beyond a certain level of complexity, such software becomes an unmanaged mess with a high
cost of ownership and support.

Does OpenL Tablets provide a silver bullet against all of these problems? Probably not. But we hope to get closer to it.
Each release aims to introduce more features to make rules easier to use by business people. Our goal is to allow all
rules maintenance without technical involvement.

---

Turning your documents into executable business rules is now simplified. You need to instruct OpenL how to process your
documents by adding headers to all your tables that describe business rules, and converting your text rule descriptions
into structured rule tables. Once done, all those rules are ready to be used in the OpenL Tablets environment. See the
OpenL Tablets [Documentation](user-guides) to find out how it's done.

## Summary

OpenL Tablets is a good approach for real-world business applications for several reasons:

- 90–95 percent of business logic is table-based
- Most of it is already maintained in Excel files
- Business logic is primarily expressed in decision, lookup, or spreadsheet tables, providing a simplified way to apply
  business algorithms

OpenL Tablets is based on the belief that structured, graphically presented documents are easier to understand and
change than business rules presented as plain text.

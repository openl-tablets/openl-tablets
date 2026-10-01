---
title: Tutorials
description: Step-by-step tutorials for learning OpenL Tablets, from decision tables to advanced rule types.
---

Series of tutorials designed to familiarize you with OpenL Tablets step-by-step, taking you from simple features and
concepts to more complex ones. These tutorials are somewhat interactive to help you become familiar with OpenL Tablets
and basic editing actions. Each tutorial is located in a separate project named Tutorial[N], where N is the tutorial
number. It is highly recommended to follow these tutorials in their natural order. Each tutorial contains a set of
working examples. Good luck and have fun!

**All these projects already exist in the OpenL DEMO.**

## Tutorial 1 - Introduction to Decision Tables

This tutorial will familiarize you with Decision Tables as the main and widely used type of OpenL Tablets components. In
addition to tutorial steps, read cell comments. Ensure that running the rules in OpenL Studio produces the desired
results; otherwise, you might have missed something.

## Tutorial 2 - Introduction to Data Tables

This tutorial demonstrates how to enter relational data into OpenL Tablets. Data can be used everywhere within OpenL
Tablets and your Java application as familiar Java Arrays. Data Tables can consist of Java classes or OpenL Datatypes.
An important feature of OpenL Data Tables is that data becomes available to OpenL Tablets or Java applications without
writing a single line of code.

## Tutorial 3 - More Advanced Decision and Data Tables

This tutorial will teach you more advanced features of decision and data tables. You will learn how to enter array data,
use merged cells to structure tables with array elements, link multiple data tables into an Object Graph using a foreign
keys-like approach, enter aggregate data objects as a single table, and enter formulas into decision table cells.

## Tutorial 4 - Introduction to Column Match Tables

This tutorial provides a short introduction and examples of how to use Column Match Table types. You will learn three
algorithms of Column Match tables: MATCH, SCORE, and WEIGHTED. Depending on the algorithm, you can map different groups
of conditions to a single return value, calculate weighted ratings, or total scores for a set of conditions.

## Tutorial 5 - Introduction to TBasic Tables

This tutorial introduces a completely new table type - TBasic - which allows you to implement different algorithms with
rather complex logic such as loops, checks, switching execution flow control/steps order, and breaking loops. The usage
of TBasic tables is demonstrated with examples.

## Tutorial 6 - Introduction to Spreadsheet Tables

This tutorial introduces another new type of OpenL Tablets - Spreadsheet Tables - which allow you to perform various
calculations and return a matrix of results. Two ways of processing Spreadsheet tables are demonstrated with examples.

## Tutorial 7 - Introduction to Table Properties

This tutorial demonstrates the advanced usage of OpenL Tablets rules when several rule sets (Rules Tables) are used
simultaneously, i.e., when rules are versioned. To achieve this, table properties are introduced.

## Tutorial 8 - Introduction to Smart Rules and Smart Lookup Tables

This tutorial explains the main features of smart rules and how they differ from simple rules. You will learn about the
structure of smart rules, data that can be used as input, supported types of ranges, True/False conditions in smart
rules, types of data that can be returned, and multiple values return.

## Example 1 - Bank Rating

This example calculates the final value of the Limit for transactions subject to credit risk from bank-counterparties.
The Limit is calculated based on the country of a bank-counterparty, total assets of the bank, its rating group, credit
ratings assigned by The Big Three credit rating agencies, and the maximum limit allowed on a bank-counterparty.

## Example 2 - Corporate Rating

This example assigns a Corporate Rating to a company based on its financial statements (Financial Rating) and the
estimated risk of collaboration with the company (Risk of Work with Corporate). The main aspect of the Rating is to
identify the creditworthiness of the company.

## Example 3 - Auto Policy Calculation

This example represents a business rules module defined in the UServ Business Rules Model 2005 (see this specification
in the **doc** file). This is a simple example of the application of OpenL Tablets rules. In this example, you will
encounter most table types: Decision tables, Data tables, Datatypes, Spreadsheets, Method, and Properties tables.
Additionally, you can see an example of tests for rules located in a separate module.

## Example-4-Reference-Guide

This example is a runnable companion of the [Reference Guide](../reference-guide/index.md). It shows every table type,
the syntax of the BEX language, and every built-in function, each next to a test table, so running the tests in OpenL
Studio shows the result of every example. Every workbook covers one topic, and a note above each table explains it.

The project is a template to look things up and to copy from:

- Open a workbook to see the syntax of a topic, for example, `SmartRules.xlsx` for the matching of conditions to
  inputs.
- Run its tests to see the results; trace a test to follow a calculation step by step.
- Change a value in a test to experiment, and run the test again.

The workbooks are grouped into folders in the order of the Reference Guide:

- **`1-Data-Model`** — `Datatypes`, `DataTables`, `Constants`: custom and vocabulary data types, default values,
  inheritance, data tables with primary and foreign keys, nested objects, lists and maps, constants.
- **`2-Decision-Tables`** — `RulesTables`, `SmartRules`, `SimpleRules`, `LookupTables`, `ExternalElements`: conditions,
  actions and returns, collected results, rule names and referents, matching of inputs, ranges and arrays, lookups,
  transposed tables, external conditions, returns and actions.
- **`3-Calculations`** — `Spreadsheets`, `MethodTables`, `TBasicTables`, `ColumnMatchTables`: spreadsheet steps,
  columns, ranges and nested spreadsheets, code with conditions and loops in Method and TBasic tables, the MATCH, SCORE
  and WEIGHTED algorithms of column match tables.
- **`4-Testing`** — `TestTables`: inputs and expected values, precision, expected errors, Run tables.
- **`5-Project`** — `Environment`, `Properties`, `Versioning`, `RuntimeContext`, `Localization`, `TableParts`: Java
  imports and module dependencies, table properties, rule versions, the runtime context, localized messages, table
  parts.
- **`6-Language`** — `Operators`, `Numbers`, `BusinessExpressions`, `Expressions`, `ArrayOperators`,
  `ValuesAndRanges`: arithmetic, comparison, logical and assignment operators, the null literal in arithmetic, the
  limits of every number type, types of results, overflow, casts and special values, remainder and power edge cases,
  numbers in cells, business keywords and attribute paths, literals, statements, casts and calls of rules, array index
  operators, values in cells, ranges.
- **`7-Functions`** — `MathFunctions`, `RoundingAndDivision`, `AggregateFunctions`, `StatisticalFunctions`,
  `StringFunctions`, `DateFunctions`, `ArrayFunctions`, `BooleanFunctions`, `SpecialFunctions`: every built-in
  function with examples of its arguments and results.

The localized messages of the `Localization` workbook are in the `i18n` folder of the project.

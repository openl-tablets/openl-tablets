# OpenL Tablets — Table Types

An OpenL Rules Table or just a Table is a tabular representation of business logic.

## How a Table Is Built

A table is a rectangular block of cells on a worksheet. Rows are read top to bottom, in a fixed order.

| Row              | Presence                        | Contains                                                    |
|------------------|---------------------------------|-------------------------------------------------------------|
| Header           | always, first row               | Keyword, return type, name, parameters — one merged cell     |
| Properties       | optional, right after header    | `<propertyName>` \| `<value>`, one property per row           |
| Structure        | per table type                  | Column roles: `C1`, `RET1`, field names, argument paths      |
| Titles           | per table type                  | Business names of the columns, shown to the author           |
| Body             | always                          | The data: one rule, field, step, test case or value per row  |

- **A blank row or column ends the table.** OpenL reads the block until the first empty line, so a blank line inside
  a table splits it and drops everything below.
- **The header cell is merged across the full width** of the table. The header is what identifies the table: its
  first word must be a keyword from [Table Type Keywords](#table-type-keywords), or the block is not a table at all.
- **A block whose first cell is not a keyword is kept as-is** and shown under a shortened form of that cell's text.
  It compiles to nothing.
- **Merged cells carry meaning**, not just formatting. Merged down rows, a cell groups the rows it spans; merged
  across columns, it declares one condition over several columns — a range, or a group of columns beneath one
  title. In a lookup the two directions say different things: the top-left cell's **height** is the number of
  arguments written across the top, its **width** the number written down the left.
- **A cell starting with `=` is an expression**, evaluated against the table's parameters and the module's rules.

### Reading the Examples

Each line below is one worksheet row and `|` separates cells. A comment after `<-` explains the row. A cell that
belongs to a merge is marked with the direction it continues from:

- `←` — merged with the cell to its **left** (the merge runs across columns)
- `↑` — merged with the cell **above** (the merge runs down rows)

```text
| Datatype Policy | ←   | ←  |  <- the header, one cell merged across the whole table
| String          | id  |    |  <- body: one field per row
| Integer         | age | 18 |
```

The header is always one cell merged across the full width, so later examples write it as a single wide cell
without repeating the `←`.

## Header Syntax

```text
TBasic <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
Method <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
Spreadsheet <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
Rules [Collect] <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
SimpleRules [Collect] <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
SmartRules [Collect] <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
SimpleLookup [Collect] <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
SmartLookup [Collect] <returnType> <tableName>(<param1Type> <param1>, <param2Type> <param2>, ...)
Environment [<tableName>]
Properties [<tableName>]
Run <executableTableName> [<tableName>]
Test <executableTableName> [<tableName>]
Data <dataType> <tableName>
Datatype <tableName> [extends <parentType>]
Datatype <tableName><<enumerationType>>
Constants [<tableName>]
```

> [!Note]
> All names in the header (table name, parameter names, datatype names) must follow Java identifier rules: start with
> a letter or underscore, followed by letters, digits, or underscores. No spaces or special characters allowed.

### Table Type Keywords

* `Datatype` - defines a custom data type (generates a Java Bean class)
* `Constants` - defines named constants of non-custom types (primitive or standard Java types)
* `Environment` - defines module-level imports and inter-module dependencies
* `Properties` - defines inherited properties for a scope (worksheet, workbook, or module)
* `Rules` - decision table with standard condition and return column structure
* `SimpleLookup` - decision table with vertical and horizontal conditions (lookup-style)
* `SimpleRules` - decision table with auto-inferred equality conditions (simplified Rules)
* `SmartLookup` - decision table with smart condition matching (ranges, collections, patterns) and vertical/horizontal
  conditions
* `SmartRules` - decision table with smart condition matching (ranges, collections, patterns) and auto-inferred
  conditions (simplified Rules)
* `Spreadsheet` - calculation table with named rows and columns, supporting cell references and expressions
* `Method` - executes a block of OpenL/Java code and returns a result; DEPRECATED, use Spreadsheet or Groovy scripts
  instead
* `TBasic` - structured programming table with explicit control flow (loops, conditionals) in cells; DEPRECATED, use
  Spreadsheet or Groovy scripts instead
* `Data` - relational data table defining a named collection of typed records; supports nested structures and
  foreign-key-style references to other Data tables
* `Test` - unit test table for any rule table, with special columns for expected results, descriptions, and error
  expectations
* `Run` - table for executing a rule table multiple times with different inputs, like Test table but without result
  validation; useful for batch execution and benchmarking
* `TablePart` - DEPRECATED, use xlsx file format which supports more rows and columns instead
* `ColumnMatch` - DEPRECATED, use Combination of Rules and Spreadsheet instead

## Properties and Metadata

All tables can have properties defined in the header or in subsequent property rows. Properties are inherited by
all tables in the same scope (worksheet, workbook, or module) unless overridden at the table level. Not all properties
are applicable to all table types; see the Properties Reference below for details.



## Table Type Details

### Decision Table (`DT` / `Rules`)

Evaluates multiple conditions and returns a result. The most common table type.

- **Header:** `Rules Double getPremium(Policy p, String region)`
- **Condition columns:** `C1`, `C2`, ... — each maps to an input parameter expression
- **Return column:** `RET` — the return value
- **Action columns:** `A1`, `A2`, ... — side-effect actions (no return)
- **Horizontal conditions:** `HC1`, `HC2`, ... — conditions along the column axis (lookup-style)
- Supports merged cells for multi-row grouping
- Supports `Collect` keyword to collect all matching results into an array

```text
| Rules String Greeting1(Integer hour)                                |
| C1                         | ←           | RET1                    |  <- roles; C1 covers two columns
| min <= hour and hour < max | ←           | greeting + ", World!"   |  <- expressions
| Integer min                | Integer max | String greeting         |  <- declared parameters
| From                       | To          | Greeting                |  <- titles
| 0                          | 12          | Good Morning            |
| 12                         | 18          | Good Afternoon          |
```

> [!Note] `SimpleRules` and `SmartRules` are DT variants with simplified column declarations — they share `XLS_DT` as
> the node type.

### Simple Rules (`SimpleRules`)

Decision table with auto-inferred equality conditions. No explicit `C1`/`RET` column rows needed.

- **Header:** `SimpleRules String getCategory(String productType, Integer age)`
- Condition columns are inferred from parameter names
- Only equality matching (`==`) is supported
- Ideal for flat lookup-style tables with simple inputs

```text
| SimpleRules Integer AmPmTo24(Integer ampmHr, String ampm) |
| AM/PM hour | AM or PM | 24 hour                           |  <- titles only, roles are inferred
| 12         | AM       | 0                                 |
| 1-11       | AM       | =ampmHr                           |
| 12         | PM       | 12                                |
```

### Smart Rules (`SmartRules`)

Decision table with smart condition matching — supports ranges, collections, and patterns.

- **Header:** `SmartRules Double getDiscount(Integer age, String region)`
- Conditions can be ranges (`18-65`), comma-separated values (`US, CA`), or `*` (wildcard/any)
- No explicit condition row declarations needed
- Best for human-authored tables with natural value expressions

```text
| SmartRules String Greeting4(Integer hour) |
| Hour | ←  | Greeting                      |  <- one argument merged over two columns: a range
| 0    | 12 | Good Morning                  |
| 18   | 22 | Good Evening                  |
```

### Simple Lookup (`SimpleLookup`)

Decision table with both vertical (left) and horizontal (top) conditions.

- **Header:** `SimpleLookup Double getRate(String country, String currency)`
- Left columns: vertical conditions (like `C1`, `C2`)
- Top row(s): horizontal conditions (like `HC1`)
- Intersection cell: the return value

```text
| SimpleLookup Double IsAdequate(CountryCode country, Boolean adequate) |
| Country Code/Adequate | True | False |  <- corner caption; a Simple table binds by position, not by title
| DE                    | 1    | 1     |
| IT                    | 1    | 0.7   |
```

### Smart Lookup (`SmartLookup`)

Same as Simple Lookup but with smart condition matching (ranges, wildcards, collections).

- **Header:** `SmartLookup Double getRate(String country, String currency)`
- Leading arguments run down the left, trailing ones across the top
- The top-left cell spans as many rows as there are arguments across the top, and as many columns as there are
  down the left — its height is how OpenL counts them
- A corner title of the form `<vertical>/<horizontal>` names both axes in one cell

```text
| SmartLookup Double CoverageRate(CoverageForm form, Integer staff, String area) |
| Coverage Type | Limit    | <= 14 | ←        | >= 15 | ←        |  <- staff, each value over its two areas
| ↑             | ↑        | Low   | High     | Low   | High     |  <- area; the two titles reach down to here
| Basic         | 100      | 20    | 30       | 30    | 66       |
| Basic         | >= 500.0 | 35    | 60       | 60    | 99       |
```

### Spreadsheet (`Spreadsheet` / `Calc`)

Calculation table with named rows and columns; cells reference each other via `$name` syntax.

- **Header:** `Spreadsheet SpreadsheetResult calcLoanPayment(Double principal, Double rate, Integer term)`
- Rows and columns have names; cells may contain expressions or literals
- Cell reference syntax: `$ColumnName$RowName`, `$ColumnName` (current row), `$RowName` (current column)
- Return type can be `SpreadsheetResult` (full result object) or a specific cell type
- `autoType` property (default `true`) infers cell types from values

```text
| Spreadsheet SpreadsheetResult Premium(Double base) |
| Steps    | Formula                                 |  <- column names, written by the author
| BaseRate | = base                                  |
| Discount | = $BaseRate * 0.1                       |
| Total    | = $BaseRate - $Discount                 |  <- last row is the result of a typed return
```

> [!Note] A Spreadsheet can return a `SpreadsheetResult` object, which exposes all named cells as fields — useful for
> complex multi-value outputs.

### Data (`Data`)

Relational data table — defines a named collection of typed records.

- **Header:** `Data Address addressBook`
- Row 1: field names (matching `Address` datatype fields)
- Row 2: descriptive titles (optional)
- Row 3+: data rows
- Use `this` as a field name to reference the data type itself (for scalar arrays)
- Supports nested structures and foreign-key-style references to other Data tables
- `Collect` keyword chains multiple Data tables into one result

```text
| Data Address addressBook          |
| city   | zip   | country.code     |  <- field names, nested by path
| City   | ZIP   | Country          |  <- titles
| Prague | 11000 | CZ               |
| Vienna | 1010  | AT               |
```

### Datatype (`Datatype`)

Defines a custom Java-like data type (generates a Java class at compile time).

- **Header:** `Datatype Policy`
- **Columns:** `Type`, `Name` (required); `Default`, `Description`, `Mandatory`, `Example` (optional; need column header
  row)
- Supports inheritance: `Datatype AutoPolicy extends Policy`
- Types can be primitive, Java types, or other Datatypes
- Default column header row is omitted when only Type and Name are used

```text
| Datatype USState |
| String | name    |  <- Type and Name only: no column header row needed
| String | code    |
```

With more than two columns the roles are named in a header row:

```text
| Datatype Policy                                       |
| Type    | Name  | Default | Mandatory | Description   |
| String  | id    |         | true      | Policy number |
| Integer | age   | 18      |           |               |
```

> [!Note] Generated classes are placed in the package defined by the `datatypePackage` property, by default
> `org.openl.generated.beans`.

### Vocabulary (`Datatype` with a base type)

Names the values one scalar type may take — an alias datatype. Written with the `Datatype` keyword and the base
type in angle brackets; OpenL Studio calls this table type `Vocabulary`.

- **Header:** `Datatype CountryCode <String>`
- **Body:** one accepted value per row, no columns and no titles
- The base type must be a simple type (`String`, `Integer`, `Double`, `Date`, ...), never a datatype
- Used as a type anywhere a simple type is: a field of a datatype, a rule argument, a return type
- A value outside the list is a compilation error wherever it is written

```text
| Datatype CountryCode <String> |
| DE                            |
| BE                            |
| NL                            |
```

### Configuration (`Environment`)

Declares module-level imports and inter-module dependencies.

- **Header:** `Environment` (no signature)
- **Commands (row 1 = command, row 2 = value):**
    - `dependency <moduleName>` — include another module's rules
    - `import <fully.qualified.ClassName>` — Java class import
    - `import <package.*>` — wildcard package import
    - `language <ruleSetName>` — parsed and ignored; support was dropped

```text
| Environment                          |
| dependency | AutoPolicyCalculation   |
| import     | org.openl.rules.helpers |
```

> [!Note] For project-level dependencies (cross-project), use `<dependencies>` in `rules.xml` instead. `Environment`
> handles within-project module-to-module dependencies.

### Method (`Method` / `Code`)

Executes a block of OpenL/Java code and returns a result.

- **Header:** `Method String formatAddress(Address a)`
- Body: one or more OpenL expressions; the **last** expression is the return value (no `return` keyword)
- Can call any other rule table or Java method
- Useful for helper logic too complex for a decision table

```text
| Method String formatAddress(Address a)  |
| a.city + ", " + a.country               |
```

### Test (`Test` / `Testmethod`)

Unit test table for any rule table. Executed by the Maven plugin's `test` goal and in OpenL Studio.

- **Header:** `Test calculatePremium testPremiumScenarios`
- Input columns: one per parameter of the tested rule
- **Special columns:**
    - `_res_` — expected result
    - `_description_` — human-readable test case description
    - `_error_` — expected error message (for negative tests)
    - `_context_` — runtime context values (date, locale, etc.)
- Rows without `_res_` are skipped in validation but still execute

```text
| Test IsAdequate IsAdequateTest       |
| countryCode | isAdequate | _res_    |  <- argument paths, then the expectation
| Country     | Adequate   | Score    |  <- titles
| CH          | True       | 1        |
| IT          | False      | 0.7      |
```

An argument of a datatype takes one column per field, addressed by path: `policy.driver.age`.

### Run (`Run` / `Runmethod`)

Executes a rule table multiple times with different inputs; no result validation.

- **Header:** `Run calculatePremium runPremiumBatch`
- Same column structure as Test but without `_res_`
- Used for batch execution, benchmarking, and integration scenarios

```text
| Run IsAdequate IsAdequateBatch |
| countryCode | isAdequate      |
| Country     | Adequate        |
| CH          | True            |
```

### Column Match (`ColumnMatch`)

Decision tree that scores or matches inputs against weighted criteria.

- **Header:** `ColumnMatch SCORE Double assessRisk(RiskInput input)`
- **Algorithms:** `MATCH`, `SCORE`, `WEIGHTED`
- **Mandatory columns:** `Names` (argument references), `Operations` (`match`, `min`, `max`), `Values`
- `SCORE` and `WEIGHTED` also require a `Weight` column
- `MATCH` maps conditions to a single return; `SCORE` computes a weighted sum

```text
| ColumnMatch SCORE Double assessRisk(RiskInput input) |
| Names        | Operations | Values | Weight         |
| input.age    | match      | 18-25  | 10             |
| input.claims | match      | >= 3   | 25             |
```

### TBasic (`TBasic` / `Algorithm`)

Structured programming table with explicit control flow (loops, conditionals) in cells.

- **Header:** `TBasic Double computeAmortization(Double principal, Double rate, Integer n)`
- Table rows correspond to algorithm steps; control flow via cell values: `IF`, `FOR`, `WHILE`, `RETURN`
- Supports user-defined subroutines and precondition/postcondition sections
- Best for complex iterative algorithms that cannot be expressed as a decision table

```text
| TBasic Double Amortize(Double principal, Integer n) |
| Operation | Condition | Action    | Return          |
| WHILE     | n > 0     | n = n - 1 |                 |
| RETURN    |           |           | principal       |
```

### Properties (`Properties`)

Sets inherited properties for a scope: worksheet, workbook, or module.

- **Header:** `Properties` (no signature)
- Row 1: `scope` value — `Worksheet`, `Workbook`, or `Module`
- Subsequent rows: `<propertyName>` | `<value>` pairs
- At most **one** Module-scoped Properties table per module
- Properties cascade: Module → Workbook → Worksheet → Table

```text
| Properties                  |
| scope         | Module      |  <- required: the scope the properties apply to
| effectiveDate | 2024-01-01  |
| lob           | Auto, Home  |
```

> [!Note] Properties defined here are inherited by all tables in the scope. A table-level property always overrides
> inherited values. See the [Properties Reference](#properties-reference) below.

### Constants (`Constants`)

Defines named constants of non-custom types accessible across the module.

- **Header:** `Constants` (no signature, optional name)
- **Columns:** `Type`, `Name`, `Value` (or expression)
- Types must be primitive or standard Java types (not user-defined Datatypes)
- Use `_DEFAULT_` as the value to represent an empty string constant

```text
| Constants                     |
| Integer | MAX_AGE     | 65    |
| String  | DEFAULT_LOB | Auto  |
```

### Table Part (`TablePart`)

Splits a large table across multiple worksheets in the **same workbook**.

- **Header:** `TablePart myLargeTable row 1 of 3`
- Split types: `row` (vertical split) or `column` (horizontal split)
- All parts must be in the same Excel file
- Applicable to: Decision Table, Data, Test, Run tables
- For `column` splits, the header row is repeated in each part

```text
| TablePart myLargeTable row 1 of 3 |
| ... the first slice of rows ...   |
```

---

## Properties Reference

All 41 properties from `DefaultPropertyDefinitions.java`, ordered by group.

| Property                   | Display Name               | Group    | Type                        | Applicable Tables        | Notes                                                          |
|----------------------------|----------------------------|----------|-----------------------------|--------------------------|----------------------------------------------------------------|
| `name`                     | Name                       | Info     | String                      | ALL                      | Deprecated — removed                                           |
| `category`                 | Category                   | Info     | String                      | ALL                      | Format: `Category-Subcategory`                                 |
| `createdBy`                | Created By                 | Info     | String                      | ALL                      | sys: set on first save                                         |
| `createdOn`                | Created On                 | Info     | Date                        | ALL                      | sys: set on first save                                         |
| `modifiedBy`               | Modified By                | Info     | String                      | ALL                      | sys: updated on each edit                                      |
| `modifiedOn`               | Modified On                | Info     | Date                        | ALL                      | sys: updated on each edit                                      |
| `description`              | Description                | Info     | String                      | ALL                      | Free-text documentation                                        |
| `tags`                     | Tags                       | Info     | String[]                    | ALL                      | Comma-separated search tags                                    |
| `version`                  | Version                    | Version  | String                      | DT, SPR, TBASIC, CM, MTH | Format: `NN.NN[.NN]`                                           |
| `active`                   | Active                     | Version  | Boolean                     | DT, SPR, TBASIC, CM, MTH | Default: `true`; one active per group                          |
| `effectiveDate`            | Effective Date             | Business | Date                        | DT, SPR, TBASIC, CM, MTH | Rule active from this date                                     |
| `expirationDate`           | Expiration Date            | Business | Date                        | DT, SPR, TBASIC, CM, MTH | Rule inactive after this date                                  |
| `startRequestDate`         | Start Request Date         | Business | Date                        | DT, SPR, TBASIC, CM, MTH | Production usage start date                                    |
| `endRequestDate`           | End Request Date           | Business | Date                        | DT, SPR, TBASIC, CM, MTH | Production usage end date                                      |
| `caRegions`                | Canada Region              | Business | CaRegionsEnum[]             | DT, SPR, TBASIC, CM, MTH | CA region filter                                               |
| `caProvinces`              | Canada Province            | Business | CaProvincesEnum[]           | DT, SPR, TBASIC, CM, MTH | CA province filter                                             |
| `country`                  | Countries                  | Business | CountriesEnum[]             | DT, SPR, TBASIC, CM, MTH | Country filter                                                 |
| `region`                   | Region                     | Business | RegionsEnum[]               | DT, SPR, TBASIC, CM, MTH | Economic region filter                                         |
| `currency`                 | Currency                   | Business | CurrenciesEnum[]            | DT, SPR, TBASIC, CM, MTH | Currency filter                                                |
| `lang`                     | Language                   | Business | LanguagesEnum[]             | DT, SPR, TBASIC, CM, MTH | Language filter                                                |
| `lob`                      | LOB                        | Business | String[]                    | DT, SPR, TBASIC, CM, MTH | Line of business filter                                        |
| `origin`                   | Origin                     | Business | OriginsEnum                 | DT, SPR, TBASIC, CM, MTH | Rule origin hierarchy                                          |
| `usregion`                 | US Region                  | Business | UsRegionsEnum[]             | DT, SPR, TBASIC, CM, MTH | US region filter                                               |
| `state`                    | US States                  | Business | UsStatesEnum[]              | DT, SPR, TBASIC, CM, MTH | US state filter                                                |
| `nature`                   | Nature                     | Business | String[]                    | DT, SPR, TBASIC, CM, MTH | Nature/type filter                                             |
| `id`                       | ID                         | Dev      | String                      | DT, SPR, TBASIC, CM, MTH | Unique ID; pattern `[a-zA-Z_][a-zA-Z0-9_]*`                    |
| `buildPhase`               | Build Phase                | Dev      | String                      | Removed in 6.0.0         | Values: `common`, `vocabulary[N]`, `main[N]`                   |
| `validateDT`               | Validate DT                | Dev      | ValidateDTEnum              | DT                       | On/Off DT validation mode                                      |
| `failOnMiss`               | Fail On Miss               | Dev      | Boolean                     | DT                       | Default: `false`; error if no rows match                       |
| `scope`                    | Scope                      | Dev      | String                      |                          | `Worksheet`, `Workbook`, or `Module`                           |
| `priority`                 | Priority                   | Dev      | Integer                     |                          | Default: `0`; for global properties ordering                   |
| `datatypePackage`          | Datatype Package           | Dev      | String                      | DTA                      | Default: `org.openl.generated.beans`                           |
| `spreadsheetResultPackage` | Spreadsheet Result Package | Dev      | String                      | SPR                      | Default: `org.openl.generated.spreadsheetresults`              |
| `cacheable`                | Cacheable                  | Dev      | Boolean                     | Removed in 6.0.0         | Cache results for variations                                   |
| `recalculate`              | Recalculate                | Dev      | RecalculateEnum             | Removed in 6.0.0         | Recalculation strategy for variations                          |
| `emptyResultProcessing`    | Empty Result Processing    | Dev      | DTEmptyResultProcessingEnum | DT                       | Default: `SKIP`                                                |
| `precision`                | Precision                  | Dev      | String                      | TEST                     | Decimal precision for result comparison                        |
| `tableStructureDetails`    | Table Structure Details    | Dev      | Boolean                     | SPR                      | Default: `false`; adds structure metadata to output            |
| `autoType`                 | Auto Type Discovery        | Dev      | Boolean                     | SPR                      | Default: `true`; infers Spreadsheet cell types                 |
| `calculateAllCells`        | Calculate All Cells        | Dev      | Boolean                     | SPR                      | Default: `true`; calculate all cells regardless of return type |
| `parallel`                 | Concurrent Execution       | Dev      | Boolean                     | DT, SPR, TBASIC, CM, MTH | Default: `false`; enable parallel row evaluation               |

**Table abbreviations:** DT = Decision Table, SPR = Spreadsheet, TBASIC = TBasic, CM = Column Match, MTH = Method,
DTA = Datatype, TEST = Test, ALL = all table types.

## Property Applicability Matrix

Rows = key business dimension and dev properties. Columns = applicable table types. ✓ = applicable.

| Property                | DT | SPR | TBASIC | CM | MTH | PROP | DTA | TEST |
|-------------------------|----|-----|--------|----|-----|------|-----|------|
| `effectiveDate`         | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `expirationDate`        | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `startRequestDate`      | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `endRequestDate`        | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `caRegions`             | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `caProvinces`           | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `country`               | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `region`                | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `currency`              | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `lang`                  | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `lob`                   | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `origin`                | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `usregion`              | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `state`                 | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `nature`                | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `version`               | ✓  | ✓   | ✓      | ✓  | ✓   |      |     |      |
| `active`                | ✓  | ✓   | ✓      | ✓  | ✓   |      |     |      |
| `id`                    | ✓  | ✓   | ✓      | ✓  | ✓   |      |     |      |
| `validateDT`            | ✓  |     |        |    |     | ✓    |     |      |
| `failOnMiss`            | ✓  |     |        |    |     | ✓    |     |      |
| `scope`                 |    |     |        |    |     | ✓    |     |      |
| `datatypePackage`       |    |     |        |    |     | ✓    | ✓   |      |
| `cacheable`             | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `recalculate`           | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `emptyResultProcessing` | ✓  |     |        |    |     | ✓    |     |      |
| `tableStructureDetails` |    | ✓   |        |    |     | ✓    |     |      |
| `autoType`              |    | ✓   |        |    |     | ✓    |     |      |
| `calculateAllCells`     |    | ✓   |        |    |     | ✓    |     |      |
| `parallel`              | ✓  | ✓   | ✓      | ✓  | ✓   | ✓    |     |      |
| `precision`             |    |     |        |    |     | ✓    |     | ✓    |

> [!Note] Info group properties (`category`, `description`, `tags`, system properties) apply to **all** table types and
> are omitted from this matrix for brevity. `buildPhase` also applies to all table types.

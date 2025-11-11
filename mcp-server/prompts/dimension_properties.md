# Dimension Properties in OpenL Tablets

OpenL Tablets has a **second versioning system** (independent from Git) called **Dimension Properties**.

## Two Independent Versioning Systems

It's critical to understand that OpenL Tablets has TWO COMPLETELY INDEPENDENT versioning systems:

1. **Git-Based Versioning**: Tracks file changes over time (commit hashes)
2. **Dimension Properties Versioning**: Multiple rule versions within the same Git commit

These work together but serve different purposes.

## Purpose of Dimension Properties

Allow multiple versions of the SAME rule (same signature) to coexist within a single Git commit, differentiated by business context:
- **Geographic**: state, country, region, caProvince
- **Business**: lob (Line of Business), currency
- **Temporal**: effectiveDate, expirationDate, startRequestDate, endRequestDate
- **Scenario**: version, active flag, origin, nature

## File Name Patterns

Properties can be encoded in file names using patterns in `rules.xml`:

```xml
<properties-file-name-pattern>.*-%state%-%effectiveDate:MMddyyyy%-%lob%</properties-file-name-pattern>
```

**Examples**:
- `Insurance-CA-01012025-Auto.xlsx` (California, effective Jan 1 2025, Auto insurance)
- `Insurance-TX-06152025-Home.xlsx` (Texas, effective Jun 15 2025, Home insurance)
- `Insurance-CW-01012025-Life.xlsx` (Country-wide, effective Jan 1 2025, Life insurance)

**Pattern Syntax**:
- `%propertyName%` - Property value (e.g., `%state%` → `CA`)
- `%propertyName:format%` - Property with format (e.g., `%effectiveDate:MMddyyyy%` → `01012025`)
- `.*` - Wildcard matching any characters

**Special Values**:
- `CW` = Country-Wide (applies to all US states)
- `Any` = Matches any value

## Table-Level Properties

In Excel, properties appear in a "properties" section after the table header:

```
Rules int calculatePremium(String state, int age)
properties
effectiveDate    01/01/2025
expirationDate   12/31/2025
state            CA
lob              Auto
```

**Scope Hierarchy** (most specific wins):
1. **Table-level properties**: Defined directly on the table
2. **Category-level properties**: Inherited from Properties table in same category
3. **Module-level properties**: Inherited from module configuration

## Available Properties

| Property | Description | Example Values | Format Options |
|----------|-------------|----------------|----------------|
| **effectiveDate** | When rule becomes legally active | 01/01/2025 | MMddyyyy, yyyy-MM-dd |
| **expirationDate** | When rule expires | 12/31/2025 | MMddyyyy, yyyy-MM-dd |
| **startRequestDate** | When rule is operationally introduced | 01/15/2025 | MMddyyyy, yyyy-MM-dd |
| **endRequestDate** | When rule is operationally retired | 12/15/2025 | MMddyyyy, yyyy-MM-dd |
| **state** | US State | CA, NY, TX, CW (country-wide) | 2-letter code |
| **lob** | Line of Business | Auto, Home, Life, Commercial | Custom values |
| **country** | Country code | US, CA, UK, FR | ISO 2-letter |
| **currency** | Currency code | USD, EUR, GBP, CAD | ISO 3-letter |
| **region** | US Region | West, East, South, North | Custom values |
| **language** | Language code | en, es, fr, de | ISO 2-letter |
| **caProvince** | Canadian Province | ON, BC, QC, AB | 2-letter code |
| **origin** | Base vs Deviation | Base, Deviation | Predefined |
| **nature** | User-defined category | Standard, Premium, Budget | Custom values |
| **version** | Scenario version | v1, v2, draft, prod | Custom values |
| **active** | Active flag | true, false | Boolean |

## Runtime Selection

When a rule is called, OpenL:
1. **Filters** rules matching input context (date, state, lob, etc.)
2. **Ranks** by specificity (table > category > module properties)
3. **Selects** most specific match
4. **Returns** result from selected rule version

**Example**:
```
Request: calculatePremium(state="CA", requestDate=2025-06-15)

Available rules (all in same Git commit):
1. calculatePremium - state: CA, effectiveDate: 01/01/2025, lob: Auto
2. calculatePremium - state: TX, effectiveDate: 01/01/2025, lob: Auto
3. calculatePremium - state: CW, effectiveDate: 01/01/2025, lob: Home

Selection: Rule #1 (CA matches, date is valid)
```

## Two Versioning Systems Working Together

**Git Commits** track file changes over time:
- Commit `a1b2c3d` (Jan 1) → Commit `e4f5g6h` (Feb 1) → Commit `i7j8k9l` (Mar 1)
- Each commit is a snapshot of all files

**Within each commit**, dimension properties create multiple rule versions:
- `calculatePremium` for CA with effective date 01/01/2025
- `calculatePremium` for CA with effective date 06/01/2025
- `calculatePremium` for TX with effective date 01/01/2025

**Key Points**:
- Git commits = file history over time
- Dimension properties = multiple versions within same file
- Same file can contain many rule versions differentiated by properties
- File name NEVER changes between Git commits
- File name pattern determines how properties are encoded in name

## Tools Available

### get_file_name_pattern
Get the current file naming pattern from rules.xml:
```
get_file_name_pattern(projectId="design_Insurance")
→ pattern: ".*-%state%-%lob%"
→ properties: ["state", "lob"]
```

### set_file_name_pattern
Set a new file naming pattern:
```
set_file_name_pattern(
  projectId="design_Insurance",
  pattern=".*-%state%-%effectiveDate:MMddyyyy%-%lob%"
)
```

### get_table_properties
Get dimension properties for a specific table:
```
get_table_properties(
  projectId="design_Insurance",
  tableId="calculatePremium-CA-Auto"
)
→ properties: {
    state: "CA",
    lob: "Auto",
    effectiveDate: "01/01/2025",
    expirationDate: "12/31/2025"
  }
```

### set_table_properties
Set dimension properties for a table:
```
set_table_properties(
  projectId="design_Insurance",
  tableId="calculatePremium-CA-Auto",
  properties: {
    state: "CA",
    lob: "Auto",
    effectiveDate: "01/01/2025",
    expirationDate: "12/31/2025"
  },
  comment: "Update effective dates for 2025"
)
```

## When to Use Dimension Properties

**Use dimension properties when**:
- Rules vary by state, region, or country
- Rules change over time (effective/expiration dates)
- Need to maintain multiple business scenarios
- Same rule has different implementations for different contexts
- Want multiple versions in production simultaneously

**Use Git commits when**:
- Tracking file changes over time
- Need audit trail of who changed what
- Comparing historical versions
- Reverting to previous state
- Managing development workflow

## Typical Workflow

### 1. Set Up File Naming Pattern
```
# Decide which properties to use in file names
set_file_name_pattern(
  projectId="design_Insurance",
  pattern=".*-%state%-%lob%"
)
```

### 2. Create Files with Encoded Properties
```
# Upload files following the pattern
upload_file(
  projectId="design_Insurance",
  fileName="Insurance-CA-Auto.xlsx",
  fileContent=<base64>
)

upload_file(
  projectId="design_Insurance",
  fileName="Insurance-TX-Home.xlsx",
  fileContent=<base64>
)
```

### 3. Set Table Properties
```
# For each table, set its dimension properties
set_table_properties(
  projectId="design_Insurance",
  tableId="calculatePremium-CA",
  properties: {
    state: "CA",
    lob: "Auto",
    effectiveDate: "01/01/2025",
    expirationDate: "12/31/2025"
  }
)
```

### 4. Test Rule Selection
```
# Execute rule with context
execute_rule(
  projectId="design_Insurance",
  ruleName="calculatePremium",
  inputData: {
    state: "CA",
    age: 30,
    requestDate: "2025-06-15"
  }
)
# OpenL automatically selects CA version
```

## Common Patterns

### Geographic Versioning
```
File pattern: ".*-%state%"
Files: Rules-CA.xlsx, Rules-TX.xlsx, Rules-CW.xlsx

Properties:
- state: CA, TX, or CW (country-wide fallback)
```

### Temporal Versioning
```
File pattern: ".*-%effectiveDate:MMddyyyy%"
Files: Rules-01012025.xlsx, Rules-06012025.xlsx

Properties:
- effectiveDate: 01/01/2025, 06/01/2025
- expirationDate: 05/31/2025, 12/31/2025
```

### Combined Versioning
```
File pattern: ".*-%state%-%lob%-%effectiveDate:MMddyyyy%"
Files: Rules-CA-Auto-01012025.xlsx, Rules-TX-Home-06012025.xlsx

Properties:
- state: CA, TX
- lob: Auto, Home
- effectiveDate: 01/01/2025, 06/01/2025
```

## Important Notes

- File names **never change** between Git commits
- Pattern is set once per project (rarely changed)
- Properties are metadata, not part of rule logic
- Properties can be set at file level (in file name) or table level (in Excel)
- Most specific property value wins at runtime
- Missing properties use default values or wildcards
- Date format in pattern must match date format in properties

## Debugging

If rule selection isn't working:
1. Check file naming pattern matches file names
2. Verify table properties are set correctly
3. Confirm request context includes necessary properties
4. Review property hierarchy (table > category > module)
5. Check date formats match pattern format
6. Look for conflicting property values

## Best Practices

- **Plan ahead**: Design file naming pattern before creating files
- **Be consistent**: Use same property names across all files
- **Document**: Add comments explaining which properties apply where
- **Test**: Execute rules with different contexts to verify selection
- **Use CW/Any**: Provide country-wide or default fallbacks
- **Date ranges**: Always set both effective and expiration dates
- **Version control**: Use Git for file changes, properties for business versions

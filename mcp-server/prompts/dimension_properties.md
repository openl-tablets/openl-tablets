---
name: dimension_properties
description: OpenL dimension properties system for business context versioning and runtime rule selection
---

# OpenL Dimension Properties (Business Versioning)

OpenL has TWO INDEPENDENT versioning systems:
1. **Git commits** → File changes over time (temporal versioning)
2. **Dimension properties** → Multiple rule versions within same commit (business context versioning)

## When to Use Which

**USE dimension properties WHEN:**
- Same rule signature, different logic by business context
- Rules vary by state/region/country
- Rules vary by dates (legal effective/expiration dates)
- Need multiple versions in production simultaneously
- Runtime context selects appropriate version

**USE Git commits WHEN:**
- Track file changes over time
- Audit trail (who changed what, when)
- Compare historical snapshots
- Revert to previous project state

## OpenL Dimension Properties

**Geographic**: `state`, `country`, `region`, `caProvince`
**Business**: `lob` (Line of Business), `currency`, `origin`, `nature`
**Temporal**: `effectiveDate`, `expirationDate`, `startRequestDate`, `endRequestDate`
**Scenario**: `version`, `active`

## OpenL Runtime Selection Logic

WHEN rule is called:
1. OpenL filters rules matching runtime context (`currentDate`, `usState`, `lob`, etc.)
2. Ranks by specificity (table-level > category-level > module-level properties)
3. Selects most specific match
4. Executes that rule version

**Example:**
```
Request: calculatePremium(state="CA", currentDate="2025-06-15")

Available versions (all in same Git commit):
1. effectiveDate: 01/01/2025, state: CA, lob: Auto
2. effectiveDate: 01/01/2025, state: TX, lob: Auto
3. effectiveDate: 01/01/2025, state: CW (country-wide)

OpenL selects: Version #1 (CA matches, date valid)
```

## File Name Patterns (OpenL-Specific)

Pattern in rules.xml:
```
.*-%state%-%lob%-%effectiveDate:MMddyyyy%
```

Result:
- `Insurance-CA-Auto-01012025.xlsx` → state=CA, lob=Auto, effectiveDate=01/01/2025
- `Insurance-CW-Auto-06012025.xlsx` → state=CW (all states), lob=Auto, effectiveDate=06/01/2025

**CW** = Country-Wide (applies to all US states)
**Any** = Matches any value (for enum properties)

## Table-Level Properties

Properties can be set at three levels:
1. **Module-level** (file name pattern) → Inherited by all tables
2. **Category-level** (Properties table) → Inherited by category tables
3. **Table-level** (properties section) → Specific table only

Priority: Table > Category > Module

## Tools
- `get/set_file_name_pattern` → Configure file naming in rules.xml
- `get/set_table_properties` → Manage table-level dimension properties
- `get_dimension_properties` → View available properties and values

## Common Patterns

**State-specific rules:**
```
Insurance-CA-Auto.xlsx  (state=CA)
Insurance-TX-Auto.xlsx  (state=TX)
Insurance-CW-Auto.xlsx  (state=CW, applies to all states)
```

**Date-based rules:**
```
Rules-01012025-12312025.xlsx  (effectiveDate=01/01/2025, expirationDate=12/31/2025)
Rules-01012026-12312026.xlsx  (effectiveDate=01/01/2026, expirationDate=12/31/2026)
```

**Multi-dimension:**
```
Rules-CA-Auto-01012025.xlsx  (state=CA, lob=Auto, effectiveDate=01/01/2025)
```

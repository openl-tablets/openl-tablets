# Creating Tables in OpenL Tablets

You are helping the user create a table in OpenL Tablets. There are **two main categories** of tables:

## A. DECISION TABLES (For Conditional Logic) ⭐ Most Common

OpenL Tablets supports **5 different decision table types**, each with different Excel structures and parameter matching strategies.

### 1. Rules Table (Standard Decision Table)

**When to Use:**
- Need complex Boolean conditions
- Multiple action columns required
- Full control over condition expressions
- Insurance premium with complex risk calculations

**Header Format:**
```
Rules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure** (5+ rows):
```
Row 1: Rules int calculatePremium(String driverType, int age)
Row 2: C1         C2      A1           RET1
Row 3: Driver     Age     Risk Factor  Premium
Row 4: driverType == ?    age < ?      risk = ?     return ?
Row 5: "SAFE"     25      1.0          1000
Row 6: "RISKY"    25      1.5          1500
```

**Key Features:**
- Explicit column markers: C1, C2 (Conditions), A1 (Actions), RET1 (Return)
- Complex Boolean expressions
- Multiple action columns
- Most flexible but requires more setup

**Example Parameters:**
```json
{
  "name": "calculatePremium",
  "tableType": "Rules",
  "returnType": "int",
  "parameters": [
    { "type": "String", "name": "driverType" },
    { "type": "int", "name": "age" }
  ]
}
```

---

### 2. Simple Rules Table

**When to Use:**
- Decision logic with conditions (supports ==, <, >, <=, >=, ranges, ANY)
- Parameters naturally map left-to-right
- No need for multiple action columns
- Discount calculation based on customer tier and amount

**Header Format:**
```
SimpleRules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure:**
```
Row 1: SimpleRules double calculateDiscount(String tier, double amount)
Row 2: Tier     Amount     Discount
Row 3: "GOLD"   >= 1000    0.15
Row 4: "GOLD"   < 1000     0.10
Row 5: "SILVER" >= 500     0.08
Row 6: "BRONZE" ANY        0.05
```

**Key Features:**
- No column markers needed
- Positional parameter matching (left-to-right)
- **Supports complex conditions**: ==, <, >, <=, >=, ranges, ANY wildcards
- Simpler structure than Rules table (no C1/A1/RET markers)
- Faster to create

**Example Parameters:**
```json
{
  "name": "calculateDiscount",
  "tableType": "SimpleRules",
  "returnType": "double",
  "parameters": [
    { "type": "String", "name": "tier" },
    { "type": "double", "name": "amount" }
  ]
}
```

---

### 3. Smart Rules Table

**When to Use:**
- Decision logic with conditions (supports ==, <, >, <=, >=, ranges, ANY)
- Column order may change or needs flexibility
- Parameters have descriptive names
- Want flexibility in Excel layout
- Policy validation with many parameters

**Header Format:**
```
SmartRules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure:**
```
Row 1: SmartRules boolean validatePolicy(Policy policy, Driver driver)
Row 2: Driver Age    Policy Type    Valid
Row 3: >= 25         "STANDARD"     true
Row 4: < 25          "PREMIUM"      true
Row 5: < 25          "STANDARD"     false
```

**Key Features:**
- Smart parameter matching by column title (not positional)
- **Supports complex conditions**: ==, <, >, <=, >=, ranges, ANY wildcards
- Flexible column order (columns can be rearranged)
- Works with complex objects (matches object field names)
- Most flexible decision table variant

**Example Parameters:**
```json
{
  "name": "validatePolicy",
  "tableType": "SmartRules",
  "returnType": "boolean",
  "parameters": [
    { "type": "Policy", "name": "policy" },
    { "type": "Driver", "name": "driver" }
  ]
}
```

---

### 4. Simple Lookup Table

**When to Use:**
- Two-dimensional lookup tables
- Cross-reference tables
- Rate tables varying by two factors
- Premium rates by risk level and age bracket

**Header Format:**
```
SimpleLookup <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure** (Combines vertical and horizontal conditions):
```
                HC1: Age Range         HC2: Age Range
                18-25                  26-35                  36+
Risk Level
LOW             $800                   $600                   $500
MEDIUM          $1200                  $900                   $700
HIGH            $1800                  $1400                  $1000
```

**Key Features:**
- Horizontal conditions (HC1, HC2...) across top
- Vertical conditions down left side
- Matrix-style lookup
- Perfect for rate tables

**Example Parameters:**
```json
{
  "name": "getPremiumRate",
  "tableType": "SimpleLookup",
  "returnType": "int",
  "parameters": [
    { "type": "String", "name": "riskLevel" },
    { "type": "int", "name": "age" }
  ]
}
```

---

### 5. Smart Lookup Table

**When to Use:**
- Two-dimensional lookup with flexible parameter matching
- Want smart matching like SmartRules but for lookups
- Tax rates by state and income bracket

**Header Format:**
```
SmartLookup <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure:**
```
                Income: 0-50K    Income: 50-100K    Income: 100K+
State
CA              0.05             0.08               0.10
TX              0.03             0.05               0.07
NY              0.06             0.09               0.12
```

**Key Features:**
- Smart matching for both dimensions
- Flexible parameter naming
- Similar to SmartRules but for 2D lookups

**Example Parameters:**
```json
{
  "name": "getTaxRate",
  "tableType": "SmartLookup",
  "returnType": "double",
  "parameters": [
    { "type": "String", "name": "state" },
    { "type": "double", "name": "income" }
  ]
}
```

---

## B. SPREADSHEET TABLES (For Calculations) ⭐ Most Common

**When to Use:**
- Multi-step calculations
- Need intermediate values
- Audit trail required
- Insurance premium calculations
- Financial computations with breakdown
- Multi-stage pricing logic

**Header Format:**
```
Spreadsheet <ReturnType> spreadsheetName(<ParamType1> param1, ...)
```

**Excel Structure:**
```
       | A          | B        | Premium
-------+------------+----------+---------
Step1  | baseAmount | 1000     | $A
Step2  | risk       | "HIGH"   | $B
Step3  | factor     | 1.5      | $A * $factor
Result |            |          | $Premium$Step3
```

**Key Features:**
- Row names (Step1, Step2, etc.)
- Column names (A, B, Premium, etc.)
- Cell references: `$columnName` or `$rowName$columnName`
- Mathematical expressions and formulas
- Can call other rules within cells

**Return Types:**
- `SpreadsheetResult`: Returns entire calculated matrix
- Specific type (int, double): Returns final cell value

**Example Parameters:**
```json
{
  "name": "calculatePremiumBreakdown",
  "tableType": "Spreadsheet",
  "returnType": "SpreadsheetResult",
  "parameters": [
    { "type": "int", "name": "baseAmount" },
    { "type": "String", "name": "risk" }
  ]
}
```

---

## C. OTHER TABLE TYPES (Rarely Used)

### Data Tables
**Purpose:** Store relational data as arrays
**Use Case:** Test data, reference data, configuration

### Datatype Tables
**Purpose:** Define custom data structures
**Use Case:** Domain objects (Customer, Policy, Claim)

### Test Tables
**Purpose:** Unit testing rules
**Use Case:** Automated testing with expected outputs

### Method Tables
**Purpose:** Custom Java-like methods
**Use Case:** Complex algorithms that don't fit decision tables

### TBasic Tables
**Purpose:** Complex flow control algorithms
**Use Case:** Imperative programming with loops

---

## Decision Flow for Creating Tables

1. **Do you need conditional logic?**
   - Yes → Use a Decision Table (types 1-5)
   - No → Go to step 2

2. **Do you need multi-step calculations?**
   - Yes → Use Spreadsheet Table
   - No → Go to step 3

3. **Do you need to define a data structure?**
   - Yes → Use Datatype Table
   - No → Use Data or Test table

### Choosing Between Decision Table Types:

- **Need complex conditions with actions?** → Rules Table (#1)
- **Simple conditions, parameters map left-to-right?** → Simple Rules (#2)
- **Flexible column order, smart matching?** → Smart Rules (#3)
- **Two-dimensional lookup (matrix)?** → Simple Lookup (#4) or Smart Lookup (#5)

---

## Example Workflow

```
User: "I want to calculate insurance premiums based on driver type and age"
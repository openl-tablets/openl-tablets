# Create Rule - Prompt Template

## Purpose
Help AI assistant select the correct rule type based on business requirements.

## Prompt

What type of rule do you want to create?

### 1. Decision Table (simplerules) ⭐ Most Common
**Use for:** IF-THEN rules, condition-action logic
**Example:** Calculate insurance premium based on age, risk, coverage
**Structure:** Conditions in rows/columns, actions in result cells
**Best for:** Business logic with multiple conditions

```
| Age | Risk Level | Coverage | → Premium |
|-----|------------|----------|-----------|
| <25 | High       | Full     | → $2000   |
| <25 | Low        | Basic    | → $800    |
| 25+ | High       | Full     | → $1500   |
```

### 2. Spreadsheet (spreadsheet)
**Use for:** Complex calculations, formulas, mathematical models
**Example:** Financial calculations, scoring models, rate computations
**Structure:** Excel-like with formulas and cell references
**Best for:** Mathematical computations with formulas

```
A1: Principal = 100000
B1: Rate = 0.05
C1: Years = 30
D1: Monthly Payment = PMT(B1/12, C1*12, -A1)
```

### 3. Datatype (datatype)
**Use for:** Define custom data structures used in rules
**Example:** Customer, Policy, Claim, Vehicle structures
**Structure:** Fields with types
**Best for:** Type definitions referenced by other rules

```
Datatype: Customer
  - customerId: String
  - age: Integer
  - riskLevel: String
  - creditScore: Integer
```

### 4. Test Table (test)
**Use for:** Unit tests for your rules with sample data
**Example:** Test premium calculation with various inputs
**Structure:** Input columns + expected output column
**Best for:** Automated testing of rule correctness

```
| Age | Risk | Coverage | → Expected Premium |
|-----|------|----------|-------------------|
| 24  | High | Full     | → 2000            |
| 30  | Low  | Basic    | → 600             |
```

### 5. Data Table (data)
**Use for:** Reference data, lookup tables, configuration
**Example:** State codes, tax rates, fee schedules
**Structure:** Static data rows
**Best for:** Business data that changes infrequently

```
| State Code | State Name | Tax Rate |
|------------|-----------|----------|
| CA         | California| 0.0725   |
| NY         | New York  | 0.0800   |
```

### 6. Method Table (method)
**Use for:** Reusable logic, helper functions, utilities
**Example:** Date calculations, validations, formatters
**Structure:** Function with parameters and return type
**Best for:** Shared logic used by multiple rules

```
Method: calculateAge(birthDate: Date) → Integer
  Returns: Years between birthDate and today
```

---

## Based on Your Description

**Your requirement:** {user_description}

**Recommended type:** {recommended_type}

**Reason:** {reason}

---

## Next Steps

1. Confirm rule type
2. Provide rule name (e.g., "CalculatePremium")
3. I'll create the rule structure for you
4. You can then populate it with your business logic

Which type would you like? (1-6)

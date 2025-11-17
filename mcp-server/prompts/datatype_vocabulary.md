---
name: datatype_vocabulary
description: Creating custom data structures (Datatypes) and enumerations (Vocabularies) in OpenL
---

## Summary

**Define reusable data structures**: Datatype tables create custom types with typed fields (like classes); Vocabulary tables define allowed values (like enums). Use Datatypes for domain objects (Policy, Customer), Vocabularies for fixed sets (RiskLevel, StateCode).

# Datatypes and Vocabularies in OpenL Tablets

Two table types for defining data structures:
- **Datatype** - Custom data structures (like Java classes)
- **Vocabulary** - Enumerations/allowed values (like Java enums)

## Part 1: Datatype Tables

### When to Use Datatypes

- Define domain objects → Policy, Customer, Vehicle, Claim
- Group related fields → Person (name, ssn, dateOfBirth, address)
- Reuse data structures → Use same Policy type across multiple rules
- Type safety → Validate fields at compile time

### Datatype Structure (2-column minimum)

```text
Row 1: Datatype <TypeName> [extends <ParentType>]
Row 2+: <Type>  <fieldName>  [defaultValue]
```

**Columns:**
- Column 1: Field type (String, Integer, Double, Date, custom types)
- Column 2: Field name (camelCase recommended)
- Column 3: Default value (optional)

### Creating Datatype - Basic Example

```text
Datatype Person

String     firstName
String     lastName
Date       dateOfBirth
Gender     gender       Male
Address    address
```

**create_rule parameters:**
```json
{
  "tableType": "Datatype",
  "name": "Person",
  "parameters": [
    { "type": "String", "name": "firstName" },
    { "type": "String", "name": "lastName" },
    { "type": "Date", "name": "dateOfBirth" },
    { "type": "Gender", "name": "gender" },
    { "type": "Address", "name": "address" }
  ]
}
```

### Creating Datatype with Inheritance

```text
Datatype InsuredPerson extends Person

Integer    age
String     licenseNumber    ""
Double     creditScore
```

**create_rule parameters:**
```json
{
  "tableType": "Datatype",
  "name": "InsuredPerson",
  "returnType": "Person",  // Parent type in returnType field
  "parameters": [
    { "type": "Integer", "name": "age" },
    { "type": "String", "name": "licenseNumber" },
    { "type": "Double", "name": "creditScore" }
  ]
}
```

**Inheritance rules:**
- Child contains ALL parent fields + child fields
- Constructor includes parent fields first, then child fields
- Use `returnType` parameter for parent type

## EASY UPDATE: Adding Fields to Datatype ⭐

**Scenario:** Add new field to existing datatype

### Step 1: Get Current Structure
```text
openl_get_table(projectId, tableId="Person_1234")
→ Returns current fields
```

### Step 2: Add New Field
```text
openl_update_table(
  projectId,
  tableId="Person_1234",
  view: {
    rows: [
      ...existingFields,
      { type: "String", name: "email", defaultValue: "" },
      { type: "String", name: "phoneNumber" }
    ]
  }
)
```

**That's it!** No schema migration, no data conversion - just add the row.

### Update Patterns for Datatypes

#### Pattern 1: Add Field (Most Common)
```json
{
  "view": {
    "rows": [
      { "type": "String", "name": "firstName" },
      { "type": "String", "name": "lastName" },
      { "type": "String", "name": "email" }  // NEW FIELD
    ]
  }
}
```

#### Pattern 2: Add Field with Default Value
```json
{
  "rows": [
    ...existingRows,
    { "type": "Boolean", "name": "isActive", "defaultValue": "true" }
  ]
}
```

#### Pattern 3: Change Default Value
```json
{
  "rows": [
    { "type": "Gender", "name": "gender", "defaultValue": "Unknown" }  // Was "Male"
  ]
}
```

#### Pattern 4: Rename Field (Caution)
⚠️ **Warning:** Renaming breaks existing rules that reference old field name

```json
{
  "rows": [
    { "type": "String", "name": "ssn" }  // Was "socialSecNumber"
  ]
}
```

After rename: Search and update all rules using old field name.

#### Pattern 5: Change Field Type (Caution)
⚠️ **Warning:** Type changes may break rules

```json
{
  "rows": [
    { "type": "Double", "name": "age" }  // Was Integer
  ]
}
```

After type change: Validate project for type errors.

### Nested Datatype Example

```text
Datatype Policy

String          policyNumber
Date            effectiveDate
Date            expirationDate
PolicyHolder    primaryInsured
Vehicle[]       vehicles
Coverage[]      coverages
```

**Array fields:** Use `Type[]` notation

### Field Types Reference

**Simple Types:**
- String, Integer, Double, Boolean, Date, Long, Float, Byte, Short, Character

**Custom Types:**
- Any Datatype defined in project (Person, Address, Policy)

**Array Types:**
- `Type[]` (e.g., `String[]`, `Vehicle[]`)

**Range Types:**
- IntRange, DoubleRange, DateRange, StringRange

## Part 2: Vocabulary Tables (Enums)

### When to Use Vocabularies

- Limited set of values → Gender (Male, Female, Other)
- Status codes → PolicyStatus (Active, Expired, Cancelled)
- Categories → RiskLevel (Low, Medium, High)
- Type safety → Prevent invalid values ("Mal" instead of "Male")

### Vocabulary Structure

```text
Row 1: Vocabulary <VocabName> <BaseType>
Row 2+: value
```

**BaseType in angle brackets:** `<String>`, `<Integer>`, `<Double>`

### Creating Vocabulary - String-Based

```text
Datatype Gender <String>

Male
Female
Other
Unknown
```

**create_rule parameters:**
```json
{
  "tableType": "Datatype",
  "name": "Gender",
  "returnType": "String",  // Base type
  "parameters": [
    { "type": "String", "name": "Male" },
    { "type": "String", "name": "Female" },
    { "type": "String", "name": "Other" },
    { "type": "String", "name": "Unknown" }
  ]
}
```

### Creating Vocabulary - Integer-Based

```text
Datatype Priority <Integer>

1
2
3
5
10
```

**create_rule parameters:**
```json
{
  "tableType": "Datatype",
  "name": "Priority",
  "returnType": "Integer",  // Base type
  "parameters": [
    { "type": "Integer", "name": "1" },
    { "type": "Integer", "name": "2" },
    { "type": "Integer", "name": "3" },
    { "type": "Integer", "name": "5" },
    { "type": "Integer", "name": "10" }
  ]
}
```

## EASY UPDATE: Adding Values to Vocabulary ⭐

**Scenario:** Add new allowed value to vocabulary

### Step 1: Get Current Values
```text
openl_get_table(projectId, tableId="Gender_1234")
→ Returns current values
```

### Step 2: Add New Value
```text
openl_update_table(
  projectId,
  tableId="Gender_1234",
  view: {
    rows: [
      { "value": "Male" },
      { "value": "Female" },
      { "value": "Other" },
      { "value": "Unknown" },
      { "value": "Unspecified" }  // NEW VALUE
    ]
  }
)
```

**That's it!** New value immediately available in all rules.

### Update Patterns for Vocabularies

#### Pattern 1: Add Value (Most Common)
```json
{
  "view": {
    "rows": [
      { "value": "Male" },
      { "value": "Female" },
      { "value": "NonBinary" },      // NEW
      { "value": "PreferNotToSay" }  // NEW
    ]
  }
}
```

#### Pattern 2: Remove Value (Caution)
⚠️ **Warning:** Removing values breaks rules using that value

```json
{
  "rows": [
    { "value": "Male" },
    { "value": "Female" }
    // "Unknown" removed
  ]
}
```

After removal: Search rules for removed value, replace with valid value.

#### Pattern 3: Rename Value (Caution)
⚠️ **Actually a delete + add** - Breaks existing rules

```json
{
  "rows": [
    { "value": "M" },      // Was "Male"
    { "value": "F" },      // Was "Female"
    { "value": "Other" }
  ]
}
```

Better approach: Add new values, update rules, then remove old values.

### Using Vocabularies in Datatypes

```text
Datatype Person

String     name
Gender     gender       Male
Priority   priority     1
```

OpenL validates all Gender/Priority values against vocabulary.

### Vocabulary Examples

#### PolicyStatus
```text
Datatype PolicyStatus <String>

Active
Pending
Expired
Cancelled
Suspended
```

#### RiskLevel
```text
Datatype RiskLevel <String>

Low
Medium
High
Critical
```

#### USStates (Abbreviated)
```text
Datatype USState <String>

AL
AK
AZ
AR
CA
...
```

## Workflow Integration

### Creating Datatype/Vocabulary

```text
1. openl_create_rule(tableType="Datatype", name=..., parameters=...)
2. openl_validate_project() → Check for compilation errors
3. openl_update_project_status() → Persist changes
```

### Updating Datatype/Vocabulary

```text
1. openl_get_table(tableId=...) → Get current structure
2. openl_update_table(tableId=..., view={...}) → Add fields/values
3. openl_validate_project() → Check for errors
4. IF errors → Fix references in rules
5. openl_update_project_status() → Persist changes
```

## Common Mistakes

❌ **Wrong:** Vocabulary without angle brackets
```text
Datatype Gender String  // WRONG - Missing < >
```

✅ **Correct:** Vocabulary with angle brackets
```text
Datatype Gender <String>  // CORRECT
```

❌ **Wrong:** Datatype with angle brackets
```text
Datatype Person <String>  // WRONG - This is vocabulary syntax
```

✅ **Correct:** Datatype without angle brackets
```text
Datatype Person  // CORRECT
```

❌ **Wrong:** Changing field order (breaks constructor)
```text
Original: name, age, gender
Changed:  age, name, gender  // Constructor order changed
```

✅ **Correct:** Add new fields at end
```text
Original: name, age, gender
Changed:  name, age, gender, email  // Added at end
```

## Field Type Validation

OpenL validates:
- ✅ Field types exist (String, Integer, custom types)
- ✅ Vocabulary values match defined list
- ✅ Default values match field type
- ✅ Array notation correct (`Type[]`)
- ✅ Inheritance chain valid (no circular inheritance)

## Integration Context

- **Simple datatype creation** (2-3 fields) → Tool description sufficient
- **Complex datatypes** (inheritance, nested types, arrays) → This prompt guides
- **Adding fields** → This prompt shows easy update pattern
- **Adding vocabulary values** → This prompt shows easy update pattern
- **User overrides** → Always allowed

## Quick Reference

| Operation | Tool | Key Points |
|-----------|------|------------|
| Create Datatype | `openl_create_rule(tableType="Datatype")` | Use parameters for fields |
| Create Vocabulary | `openl_create_rule(tableType="Datatype", returnType=baseType)` | Use angle brackets `<Type>` |
| Add Field | `openl_update_table(view={rows: [...existing, newField]})` | Just add row! |
| Add Value | `openl_update_table(view={rows: [...existing, newValue]})` | Just add row! |
| Inherit | `openl_create_rule(returnType=parentType)` | Parent in returnType |
| Validate | `openl_validate_project()` | After any change |
| Save | `openl_update_project_status()` | Persist changes |

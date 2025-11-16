---
name: append_table
description: Guide for appending new rows/fields to existing tables efficiently
arguments:
  - name: tableId
    description: ID of the table to append data to
    required: false
  - name: tableType
    description: Type of table being appended to
    required: false
---

# Appending Data to Tables

{if tableId}
## Appending to Table: **{tableId}**
{end if}
{if tableType}

**Table Type**: {tableType}
{end if}

## When to Use openl_append_table

- **Add fields to Datatype** → Extend custom data structures
- **Add rows to Data tables** → Append reference data
- **Simple additions** → More efficient than openl_update_table for appending
- **Incremental updates** → Add without fetching full structure

## openl_append_table vs openl_update_table

### Use openl_append_table When:
✅ Adding new fields/rows to end of table
✅ Don't need to modify existing data
✅ Want efficient incremental updates
✅ Working with Datatype or Data tables

### Use openl_update_table When:
✅ Modifying existing fields/rows
✅ Reordering fields
✅ Removing fields/rows
✅ Complex structural changes

## Table Types Supporting Append

### Primary Use Cases

**Datatype Tables**
- Add new fields to custom data structures
- Extend type definitions
- Example: Add `email` field to `Customer` datatype

**Data Tables**
- Append reference data rows
- Add lookup values
- Example: Add new states to `StateList` data table

## Input Structure

```json
{
  "name": "openl_append_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Customer_1234",
    "appendData": {
      "tableType": "Datatype",
      "fields": [
        {
          "name": "email",
          "type": "String",
          "required": false,
          "defaultValue": null
        },
        {
          "name": "phoneNumber",
          "type": "String",
          "required": true
        }
      ]
    },
    "comment": "Added contact information fields"
  }
}
```

## Field Definition Structure

Each field in the `fields` array must include:

### Required Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `name` | string | Field name (must be valid identifier) | `"email"`, `"phoneNumber"` |
| `type` | string | Field type (OpenL or Java type) | `"String"`, `"int"`, `"double"`, `"Date"` |

### Optional Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `required` | boolean | Whether field is required | `true`, `false` |
| `defaultValue` | any | Default value for field | `null`, `""`, `0`, `true` |

## Common Field Types

### Primitive Types
- `String` → Text values
- `int` → Integers
- `double` → Decimal numbers
- `boolean` → True/false values
- `Date` → Date values

### Complex Types
- Custom datatypes (e.g., `Address`, `Policy`)
- Arrays (e.g., `String[]`, `int[]`)
- Collections (e.g., `List<String>`)

## Usage Patterns

### Pattern 1: Add Single Field to Datatype

```json
{
  "appendData": {
    "tableType": "Datatype",
    "fields": [
      {
        "name": "middleName",
        "type": "String",
        "required": false,
        "defaultValue": ""
      }
    ]
  }
}
```

### Pattern 2: Add Multiple Fields

```json
{
  "appendData": {
    "tableType": "Datatype",
    "fields": [
      {
        "name": "email",
        "type": "String",
        "required": true
      },
      {
        "name": "phone",
        "type": "String",
        "required": false
      },
      {
        "name": "preferredContact",
        "type": "String",
        "defaultValue": "email"
      }
    ]
  }
}
```

### Pattern 3: Add Complex Type Field

```json
{
  "appendData": {
    "tableType": "Datatype",
    "fields": [
      {
        "name": "address",
        "type": "Address",
        "required": true
      }
    ]
  }
}
```

### Pattern 4: Add Array Field

```json
{
  "appendData": {
    "tableType": "Datatype",
    "fields": [
      {
        "name": "previousAddresses",
        "type": "Address[]",
        "required": false,
        "defaultValue": null
      }
    ]
  }
}
```

### Pattern 5: Add Numeric Field with Default

```json
{
  "appendData": {
    "tableType": "Datatype",
    "fields": [
      {
        "name": "creditScore",
        "type": "int",
        "required": false,
        "defaultValue": 0
      }
    ]
  }
}
```

## Workflow

### Step-by-Step Process

```
1. Identify table → Use openl_list_tables() to find tableId
2. Determine fields → Define new fields with types
3. Call openl_append_table → Add fields to table
4. Verify changes → Use openl_get_table() to confirm
5. Save project → Persist changes with openl_update_project_status()
```

### Complete Example

```bash
# Step 1: List tables to find the one to append to
openl_list_tables(projectId="design-insurance-rules", tableType="Datatype")

# Step 2: Append new fields
openl_append_table(
  projectId="design-insurance-rules",
  tableId="Customer_1234",
  appendData={
    "tableType": "Datatype",
    "fields": [
      {
        "name": "loyaltyTier",
        "type": "String",
        "required": false,
        "defaultValue": "Bronze"
      }
    ]
  },
  comment="Added customer loyalty tier field"
)

# Step 3: Verify the append
openl_get_table(projectId="design-insurance-rules", tableId="Customer_1234")

# Step 4: Save changes
openl_update_project_status(
  projectId="design-insurance-rules",
  comment="Added loyalty tier to Customer datatype"
)
```

## Best Practices

### Naming Conventions
✅ Use camelCase for field names (`firstName`, `dateOfBirth`)
✅ Use descriptive names (`email` not `e`, `phoneNumber` not `ph`)
✅ Avoid special characters except underscore
✅ Start with lowercase letter

### Type Selection
✅ Use primitive types when possible (`String`, `int`, `double`)
✅ Use custom datatypes for complex structures
✅ Consider arrays for repeating data
✅ Match types to business requirements

### Default Values
✅ Provide sensible defaults for optional fields
✅ Use `null` for object types when no default exists
✅ Use `""` for optional strings if appropriate
✅ Use `0` for numeric types if meaningful

### Required Flags
✅ Mark fields as required only when business rules demand it
✅ Consider backward compatibility (existing data may not have values)
✅ Document required fields in comments

## Common Scenarios

### Scenario 1: Extend Customer Datatype

```
GOAL: Add email and phone to Customer

SOLUTION:
openl_append_table(
  tableId="Customer_1234",
  appendData={
    "tableType": "Datatype",
    "fields": [
      {"name": "email", "type": "String", "required": true},
      {"name": "phone", "type": "String", "required": false}
    ]
  }
)
```

### Scenario 2: Add Calculated Field

```
GOAL: Add field that stores calculated value

SOLUTION:
openl_append_table(
  tableId="Policy_5678",
  appendData={
    "tableType": "Datatype",
    "fields": [
      {"name": "totalPremium", "type": "double", "defaultValue": 0.0}
    ]
  }
)
```

### Scenario 3: Add Metadata Field

```
GOAL: Add tracking/audit fields

SOLUTION:
openl_append_table(
  tableId="Transaction_9012",
  appendData={
    "tableType": "Datatype",
    "fields": [
      {"name": "createdAt", "type": "Date", "required": true},
      {"name": "createdBy", "type": "String", "required": true},
      {"name": "modifiedAt", "type": "Date"},
      {"name": "modifiedBy", "type": "String"}
    ]
  }
)
```

## Error Handling

### Common Errors

**Invalid Field Name**
```
Error: Field name must be valid Java identifier
Fix: Use camelCase, start with letter, no spaces/special chars
```

**Type Not Found**
```
Error: Type 'CustomType' not found
Fix: Ensure custom datatype exists in project first
```

**Duplicate Field Name**
```
Error: Field 'email' already exists
Fix: Use openl_update_table to modify existing field, or choose different name
```

**Invalid Table Type**
```
Error: Cannot append to table type 'Rules'
Fix: Use openl_append_table only for Datatype and Data tables
```

## Validation After Append

```
# After appending, always verify:
1. openl_get_table(tableId) → Confirm fields added
2. Manually validate in OpenL WebStudio UI → Check for type errors
   (openl_validate_project is temporarily disabled)
3. IF validation passes → openl_update_project_status(comment="...")
4. IF validation fails → Review field types and references
```

## Integration with Other Tools

### Before openl_append_table
- `openl_list_tables()` → Find table to append to
- `openl_get_table()` → Review current structure (optional)

### After openl_append_table
- `openl_get_table()` → Verify append succeeded
- Validate in OpenL WebStudio UI → Check for errors
- `openl_update_project_status()` → Save changes

### Alternatives
- `openl_update_table()` → For complex modifications
- `openl_create_rule()` → For creating new tables
- `openl_upload_file()` → For bulk Excel updates

## Quick Reference

| Task | Command |
|------|---------|
| Add single field | `openl_append_table(fields=[{name, type}])` |
| Add multiple fields | `openl_append_table(fields=[{...}, {...}, ...])` |
| Add required field | `openl_append_table(fields=[{name, type, required: true}])` |
| Add with default | `openl_append_table(fields=[{name, type, defaultValue}])` |
| Verify append | `openl_get_table(tableId)` |
| Save changes | `openl_update_project_status(comment="...")` |

## Performance Notes

- **Efficiency**: openl_append_table is more efficient than openl_update_table for simple additions
- **In-memory**: Changes are in-memory until openl_update_project_status()
- **Bulk append**: Can append multiple fields in single call
- **No fetch**: Don't need to fetch full table first (unlike openl_update_table)

# Lookup View API Documentation

## Overview

Lookup tables are used to retrieve values based on dimension parameters, returning a result from a multi-dimensional array. The Lookup API supports both **SmartLookup** and **SimpleLookup** table types and allows reading, writing, and appending rows to these tables in a structured format.

## Table Types

- **SmartLookup**: Lookup table with multi-dimensional hierarchy support for complex column grouping structures
- **SimpleLookup**: Lookup table with simpler hierarchical structure for basic column grouping

## Data Models

### LookupView

The unified view model for both SmartLookup and SimpleLookup tables.

**Example SmartLookup:**
```json
{
  "tableType": "SmartLookup",
  "name": "PrimaryBreedFactor",
  "returnType": "Double",
  "args": [
    {
      "name": "species",
      "type": "Species"
    },
    {
      "name": "breed",
      "type": "Breed"
    },
    {
      "name": "coverageType",
      "type": "CoverageType"
    },
    {
      "name": "breedPurity",
      "type": "BreedPurity"
    }
  ],
  "headers": [
    {
      "title": "Species",
      "children": []
    },
    {
      "title": "Breed",
      "children": []
    },
    {
      "title": "AccidentOnly",
      "children": [
        {
          "title": "Purebred",
          "children": []
        },
        {
          "title": "MixedBreed",
          "children": []
        }
      ]
    },
    {
      "title": "AccidentIllness",
      "children": [
        {
          "title": "Purebred",
          "children": []
        },
        {
          "title": "MixedBreed",
          "children": []
        }
      ]
    }
  ],
  "rows": [
    {
      "Species": "Dog",
      "Breed": "Affenpinscher",
      "AccidentOnly": {
        "Purebred": "1.04",
        "MixedBreed": "1.04"
      },
      "AccidentIllness": {
        "Purebred": "0.80",
        "MixedBreed": "0.80"
      }
    },
    {
      "Species": "Dog",
      "Breed": "AfghanHound",
      "AccidentOnly": {
        "Purebred": "1.34",
        "MixedBreed": "1.34"
      },
      "AccidentIllness": {
        "Purebred": "1.53",
        "MixedBreed": "1.53"
      }
    }
  ]
}
```

**Example SimpleLookup:**
```json
{
  "tableType": "SimpleLookup",
  "name": "SimpleDiscount",
  "returnType": "Double",
  "args": [
    {
      "name": "category",
      "type": "String"
    },
    {
      "name": "amount",
      "type": "Double"
    }
  ],
  "headers": [
    {
      "title": "Category",
      "children": []
    },
    {
      "title": "Amount",
      "children": []
    },
    {
      "title": "Discount",
      "children": []
    }
  ],
  "rows": [
    {
      "Category": "Premium",
      "Amount": "1000",
      "Discount": "0.15"
    },
    {
      "Category": "Standard",
      "Amount": "500",
      "Discount": "0.10"
    }
  ]
}
```

### LookupHeaderView

Header model supporting hierarchical header structures for both SmartLookup and SimpleLookup tables.

**Fields:**
- `title` (string): Header title
- `children` (array): Child headers for multi-level column grouping (empty array for leaf headers)

**Characteristics:**
- **Leaf headers**: Have empty `children` array, directly map to data values
- **Parent headers**: Have non-empty `children` array, map to nested objects in row data

**Example with children (parent header):**
```json
{
  "title": "AccidentOnly",
  "children": [
    {
      "title": "Purebred",
      "children": []
    },
    {
      "title": "MixedBreed",
      "children": []
    }
  ]
}
```

**Example without children (leaf header):**
```json
{
  "title": "Species",
  "children": []
}
```

### LookupAppend

Request model for appending rows to Lookup tables (both SmartLookup and SimpleLookup).

**Fields:**
- `tableType` (string): Either "SmartLookup" or "SimpleLookup"
- `rows` (array): List of row objects with hierarchical structure matching headers

**Example for SmartLookup:**
```json
{
  "tableType": "SmartLookup",
  "rows": [
    {
      "Species": "Cat",
      "Breed": "Siamese",
      "AccidentOnly": {
        "Purebred": "1.10",
        "MixedBreed": "1.10"
      },
      "AccidentIllness": {
        "Purebred": "0.95",
        "MixedBreed": "0.95"
      }
    }
  ]
}
```

**Example for SimpleLookup:**
```json
{
  "tableType": "SimpleLookup",
  "rows": [
    {
      "Category": "Gold",
      "Amount": "2000",
      "Discount": "0.20"
    }
  ]
}
```

### Row Structure

Rows are LinkedHashMap objects with hierarchical structure matching header hierarchy.

**Fields:**
- Leaf headers map to their actual values (string, number, etc.)
- Parent headers map to nested objects containing child values

**Example:**
```json
{
  "Species": "Dog",
  "Breed": "Affenpinscher",
  "AccidentOnly": {
    "Purebred": "1.04",
    "MixedBreed": "1.04"
  },
  "AccidentIllness": {
    "Purebred": "0.80",
    "MixedBreed": "0.80"
  }
}
```

In this example:
- `Species` and `Breed` are leaf headers with direct values
- `AccidentOnly` and `AccidentIllness` are parent headers with nested objects
- Each nested object contains values for its children (`Purebred`, `MixedBreed`)

## Table Structure

### SmartLookup Structure

SmartLookup tables support multi-dimensional hierarchies and consist of:

1. **Dimension Parameters** (first columns): Non-grouped headers representing the lookup parameters
   - Example: Species, Breed

2. **Result Columns** (remaining columns): May be heavily grouped using complex hierarchical headers
   - Example: AccidentOnly [Purebred, MixedBreed], AccidentIllness [Purebred, MixedBreed]
   - Supports multiple levels of nesting for complex structures

3. **Body Rows**: Data rows with hierarchical structure matching header hierarchy
   - Row structure mirrors header structure exactly
   - Leaf headers map to values
   - Parent headers map to nested objects with their children's values
   - No key duplication - "Purebred" under AccidentOnly is separate from "Purebred" under AccidentIllness

### SimpleLookup Structure

SimpleLookup tables support simpler hierarchical structures:

1. **Lookup Parameters** (first columns): Non-grouped headers representing the lookup parameters
   - Example: Category, Amount

2. **Result Columns** (remaining columns): May have simple hierarchical grouping
   - Example: Discount (single-level grouping)
   - Simpler structure compared to SmartLookup

3. **Body Rows**: Data rows with hierarchical structure matching header hierarchy
   - Structure aligned with simpler header hierarchy
   - Leaf headers map to values
   - Parent headers (if present) map to nested objects

## HTML Table Examples

### SmartLookup Example

```html
<table border="1">
  <caption>SmartLookup Double PrimaryBreedFactor (Species, Breed, CoverageType, BreedPurity)</caption>
  <thead>
    <tr>
      <th rowspan="2">Species</th>
      <th rowspan="2">Breed</th>
      <th colspan="2">AccidentOnly</th>
      <th colspan="2">AccidentIllness</th>
    </tr>
    <tr>
      <th>Purebred</th>
      <th>MixedBreed</th>
      <th>Purebred</th>
      <th>MixedBreed</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Dog</td>
      <td>Affenpinscher</td>
      <td>1.04</td>
      <td>1.04</td>
      <td>0.80</td>
      <td>0.80</td>
    </tr>
    <tr>
      <td>Dog</td>
      <td>AfghanHound</td>
      <td>1.34</td>
      <td>1.34</td>
      <td>1.53</td>
      <td>1.53</td>
    </tr>
  </tbody>
</table>
```

### SimpleLookup Example

```html
<table border="1">
  <caption>SimpleLookup Double SimpleDiscount (Category, Amount)</caption>
  <thead>
    <tr>
      <th>Category</th>
      <th>Amount</th>
      <th>Discount</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>Premium</td>
      <td>1000</td>
      <td>0.15</td>
    </tr>
    <tr>
      <td>Standard</td>
      <td>500</td>
      <td>0.10</td>
    </tr>
  </tbody>
</table>
```

## Key Features

### Common to Both SmartLookup and SimpleLookup

- **Hierarchical Headers**: Support for column grouping (simple in SimpleLookup, complex in SmartLookup)
- **Hierarchical Rows**: Row structure mirrors header structure exactly
- **No Key Duplication**: Same header title under different parents maps to different nested locations
- **Flexible Lookup**: Works with any number of dimension parameters
- **Type-safe**: Return type and parameter types defined in the table
- **Unified API**: Single LookupView, LookupHeaderView, and LookupAppend models support both types

### SmartLookup-Specific

- **Multi-dimensional Hierarchy**: Supports deep nesting for complex structures
- **Complex Column Grouping**: Multiple levels of grouped columns
- **Advanced Scenarios**: Suitable for sophisticated lookup tables with multiple grouped dimensions

### SimpleLookup-Specific

- **Simpler Structure**: Basic column grouping with minimal nesting
- **Easy to Use**: Straightforward lookup patterns
- **Common Scenarios**: Suitable for simple lookup tables with basic requirements

## Notes

### API Usage

- **Discriminator Field**: The `tableType` field is required and determines which Lookup variant:
  - Set to `"SmartLookup"` for SmartLookup tables
  - Set to `"SimpleLookup"` for SimpleLookup tables

- **Structure Alignment**: Row structure must align with header hierarchy
  - Leaf headers in the header tree correspond to leaf values in the row tree
  - Parent headers in the header tree correspond to nested objects in the row tree

- **No Duplication**: The hierarchical structure eliminates key duplication problems
  - "Purebred" under "AccidentOnly" → `AccidentOnly.Purebred`
  - "Purebred" under "AccidentIllness" → `AccidentIllness.Purebred`
  - These are distinct nested paths, not conflicting keys

- **Header Hierarchy**: Preserved in LookupHeaderView for proper UI rendering
- **Optional Grouping**: Child headers are optional (empty array for leaf headers without grouping)

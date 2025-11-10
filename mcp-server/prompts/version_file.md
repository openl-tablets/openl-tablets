# Version File - Prompt Template

## Purpose
Guide AI assistant in creating properly versioned rule files following OpenL naming conventions.

## Prompt

## File Versioning Strategy

**Current file:** `{current_filename}`
**Detected pattern:** `{detected_pattern}`

---

### OpenL Best Practice: File-Level Versioning

In OpenL Tablets, **file-level versioning** is recommended over table-level versioning because:
- ✅ Keeps related rules synchronized
- ✅ Maintains consistency across dependent rules
- ✅ Easier to deploy as a unit
- ✅ Follows OpenL naming conventions
- ✅ Better audit trail

---

### Recommended Naming Convention

```
{RuleName}_{VersionDimension}_{Version}.xlsx
```

**Examples:**
- `InsurancePolicy_2024_v1.xlsx` - Year-based with version
- `LoanRules_Q1_2024.xlsx` - Quarter-based
- `PricingRules_Jan2024_v2.xlsx` - Month-based with version
- `UnderwritingRules_v3.xlsx` - Simple version numbering

---

### Suggested Next Versions

Based on `{current_filename}`, here are your options:

#### Option 1: Increment Version (Minor Changes) ✅ Recommended
**New filename:** `{incremented_version}`
**Use when:** Bug fixes, minor rule adjustments, small updates
**Example:** `Policy_2024_v1.xlsx` → `Policy_2024_v2.xlsx`

#### Option 2: New Time Period (Major Changes)
**New filename:** `{new_period_version}`
**Use when:** New business period, major rule overhaul, regulatory changes
**Example:** `Policy_2024_v3.xlsx` → `Policy_2025_v1.xlsx`

#### Option 3: Custom Naming
**Your filename:** `_______________________.xlsx`
**Use when:** Special versioning needs
**Pattern:** Must follow `{Name}_{Dimension}_{Version}.xlsx`

---

### Version Properties in Rules

OpenL Tablets supports **version properties** that can be read from filename:

```
Pattern: LoanRules_{state}_{lob}_{year}_v{version}.xlsx

Extracted properties:
- state: CA, NY, TX (geographic dimension)
- lob: Auto, Home, Life (line of business)
- year: 2024, 2025 (effective period)
- version: 1, 2, 3 (incremental version)
```

At runtime, OpenL selects the correct rule file based on these properties.

**Do you want to use version properties?** (yes/no)

If yes, please specify:
- Property names (e.g., state, year, version)
- Property values for this file

---

### What Changes Are You Making?

Understanding your changes helps determine versioning:

**Change Type:**
- [ ] Bug fix (increment patch: v1 → v2)
- [ ] New rules added (increment minor: v1 → v2 or new period)
- [ ] Rules modified (increment version)
- [ ] Major regulatory change (new period)
- [ ] New business period (new year/quarter/month)

**Description of changes:**
_____________________________________________________

---

### Recommendation

**Recommended filename:** `{recommended_filename}`

**Reason:** {reason_based_on_changes}

**Proceed with this version?** (yes/no)

---

## Next Steps

1. Confirm filename
2. I'll create the new versioned file
3. Copy all rules from current version
4. Update version properties in rules.xml
5. You can then modify the rules in the new version

The original file `{current_filename}` will remain unchanged (history preserved).

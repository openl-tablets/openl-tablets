/** Sensitive fields not returned by API - empty string in form equals undefined in saved data */
export const SENSITIVE_EMPTY_FIELDS = ['password', 'secretKey', 'accountKey']

/**
 * Checks if a field path (e.g., 'settings.password') represents a sensitive field.
 * Handles both top-level fields ('password') and nested fields ('settings.password').
 */
const isSensitiveField = (fieldPath: string): boolean => {
    // Check if any sensitive field name appears in the path
    return SENSITIVE_EMPTY_FIELDS.some(sensitiveField => fieldPath.endsWith(`.${sensitiveField}`) || fieldPath === sensitiveField)
}

/**
 * Deep comparison of arrays element by element.
 * Handles nested objects and arrays recursively.
 */
const areArraysEqual = (arr1: unknown[], arr2: unknown[]): boolean => {
    if (arr1.length !== arr2.length) return false
    for (let i = 0; i < arr1.length; i++) {
        const item1 = arr1[i]
        const item2 = arr2[i]
        if (Array.isArray(item1) && Array.isArray(item2)) {
            if (!areArraysEqual(item1, item2)) return false
        } else if (
            typeof item1 === 'object' &&
            item1 !== null &&
            typeof item2 === 'object' &&
            item2 !== null &&
            !Array.isArray(item1) &&
            !Array.isArray(item2)
        ) {
            if (!isFormValuesEqual(item1 as Record<string, unknown>, item2 as Record<string, unknown>)) {
                return false
            }
        } else if (item1 !== item2) {
            return false
        }
    }
    return true
}

/**
 * Deep comparison of form values with saved repository data.
 * Treats empty/undefined sensitive fields (password, secretKey, accountKey) as equal,
 * including nested fields like settings.password.
 * Recursively compares arrays and nested objects.
 */
export const isFormValuesEqual = (
    current: Record<string, unknown>,
    saved: Record<string, unknown> | null,
    fieldPath: string = ''
): boolean => {
    if (!saved) return true
    const allKeys = new Set([...Object.keys(current), ...Object.keys(saved)])
    for (const key of allKeys) {
        const cur = current[key]
        const sav = saved[key]
        const currentFieldPath = fieldPath ? `${fieldPath}.${key}` : key
        
        // Handle sensitive fields: treat empty/undefined as equal
        // This works for both top-level fields (password) and nested fields (settings.password)
        // InputPassword component automatically sets field to undefined when value.secret is true,
        // so we need to treat undefined/empty as equal for sensitive fields
        if (isSensitiveField(currentFieldPath)) {
            // For sensitive fields, treat empty string, undefined, and null as equal
            // This prevents false positives when InputPassword automatically sets field to undefined
            const curIsEmpty = cur === '' || cur == null || cur === undefined
            const savIsEmpty = sav === '' || sav == null || sav === undefined
            
            // If both are empty, they are equal
            if (curIsEmpty && savIsEmpty) {
                continue
            }
            
            // If form has a value (non-empty) and saved is empty, user entered a new password
            if (!curIsEmpty && savIsEmpty) {
                return false
            }
            
            // If form is empty and saved has a value, this means:
            // - InputPassword set field to undefined because value.secret is true (password exists but is encrypted)
            // - This is NOT a change - user didn't modify the password
            // So we treat them as equal
            if (curIsEmpty && !savIsEmpty) {
                continue
            }
            
            // Both have values (non-empty) - this means user entered a new password
            // We can't compare encrypted values, but if user entered something, it's a change
            // Note: In practice, InputPassword sets field to undefined for secret values,
            // so this case should rarely occur. But if it does, treat as change.
            return false
        }
        
        // Handle boolean values FIRST: treat false and undefined as equal (for fields like mainBranchOnly)
        // This prevents false positives when switching between repositories where one has mainBranchOnly: false
        // and another doesn't have this field (undefined)
        // This check must come before object/array checks to handle boolean values correctly
        if (typeof cur === 'boolean' || typeof sav === 'boolean') {
            // Normalize: undefined/null becomes false, true stays true, false stays false
            const curBool = cur === true ? true : false
            const savBool = sav === true ? true : false
            if (curBool !== savBool) {
                return false
            }
            continue // Skip other checks for boolean values
        }
        
        // Handle arrays
        if (Array.isArray(cur) && Array.isArray(sav)) {
            if (!areArraysEqual(cur, sav)) {
                return false
            }
        }
        // Handle nested objects (non-arrays)
        else if (
            typeof cur === 'object' &&
            cur !== null &&
            typeof sav === 'object' &&
            sav !== null &&
            !Array.isArray(cur) &&
            !Array.isArray(sav)
        ) {
            if (!isFormValuesEqual(cur as Record<string, unknown>, sav as Record<string, unknown>, currentFieldPath)) {
                return false
            }
        }
        // Handle primitive values and mismatched types
        else if (cur !== sav) {
            return false
        }
    }
    return true
}

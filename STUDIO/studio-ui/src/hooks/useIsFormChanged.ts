import { useEffect, useState } from 'react'
import { Form, type FormInstance } from 'antd'

interface UseIsFormChangedProps<T extends object> {
    form: FormInstance<T>
    initialValues?: T | undefined
}

const hasValue = (value: unknown): boolean => {
    if (value == null) return false
    if (typeof value === 'string' && value.trim() === '') return false
    if (typeof value === 'object' && !Array.isArray(value)) {
        return Object.values(value).some(v => hasValue(v))
    }
    return true
}

/** Whether a field the initial values do not hold has been given a value. */
const isFilled = (value: unknown): boolean => {
    if (typeof value === 'object' && value !== null && !Array.isArray(value)) {
        return hasValue(value)
    }
    return value != null && value !== ''
}

/** Compares two arrays element by element. */
const areArraysEqual = (value1: unknown[], value2: unknown[]): boolean => {
    if (value1.length !== value2.length) return false
    for (let i = 0; i < value1.length; i++) {
        if (!isEqual(value1[i], value2[i])) {
            return false
        }
    }
    return true
}

/** Compares two objects key by key, over the keys of both. */
const areObjectsEqual = (value1: object, value2: object): boolean => {
    const fields1 = new Map<string, unknown>(Object.entries(value1))
    const fields2 = new Map<string, unknown>(Object.entries(value2))

    // Get all unique keys from both objects
    const allKeys = new Set([...fields1.keys(), ...fields2.keys()])

    // Compare each key
    for (const key of allKeys) {
        if (!isEqual(fields1.get(key), fields2.get(key))) {
            return false
        }
    }

    return true
}

/**
 * Deep comparison function that handles arrays, objects, and primitives recursively.
 * Exported for use in form comparison logic.
 */
const isEqual = (value1: unknown, value2: unknown): boolean => {
    // Handle null/undefined cases
    if (value1 === value2) return true
    if (value1 == null && value2 == null) return true
    if (value1 == null || value2 == null) return false

    // Handle arrays
    if (Array.isArray(value1) && Array.isArray(value2)) {
        return areArraysEqual(value1, value2)
    }

    // Handle objects (non-arrays)
    if (typeof value1 === 'object' && typeof value2 === 'object') {
        return areObjectsEqual(value1, value2)
    }

    return false
}

export const useIsFormChanged = <T extends object>({ form, initialValues }: UseIsFormChangedProps<T>) => {
    const [isFormChanged, setIsFormChanged] = useState(false)
    const watchedFields = Form.useWatch([], form)

    useEffect(() => {
        if (!form || !watchedFields || !initialValues) {
            setIsFormChanged(false)
            return
        }

        // Get current values
        const currentValues = new Map<string, unknown>(Object.entries(form.getFieldsValue()))
        const initial = new Map<string, unknown>(Object.entries(initialValues))

        // Check each field that exists in initial values
        const hasChanges = [...initial].some(([fieldKey, initialValue]) => (
            !isEqual(currentValues.get(fieldKey), initialValue)
        ))

        // Also check if any new fields (like password) have actual values
        const newFieldChanges = [...currentValues].some(([fieldKey, currentValue]) => (
            !initial.has(fieldKey) && isFilled(currentValue)
        ))

        setIsFormChanged(hasChanges || newFieldChanges)
    }, [watchedFields, form, initialValues])

    return isFormChanged
}

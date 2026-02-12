import { useEffect, useState } from 'react'
import { Form } from 'antd'

interface UseIsFormChangedProps {
    form: any
    initialValues?: any
}

const hasValue = (value: any): boolean => {
    if (value == null) return false
    if (typeof value === 'string' && value.trim() === '') return false
    if (typeof value === 'object' && !Array.isArray(value)) {
        return Object.values(value).some(v => hasValue(v))
    }
    return true
}

/**
 * Deep comparison function that handles arrays, objects, and primitives recursively.
 * Exported for use in form comparison logic.
 */
export const isEqual = (value1: any, value2: any): boolean => {
    // Handle null/undefined cases
    if (value1 === value2) return true
    if (value1 == null && value2 == null) return true
    if (value1 == null || value2 == null) return false
    
    // Handle arrays
    if (Array.isArray(value1) && Array.isArray(value2)) {
        if (value1.length !== value2.length) return false
        for (let i = 0; i < value1.length; i++) {
            if (!isEqual(value1[i], value2[i])) {
                return false
            }
        }
        return true
    }
    
    // Handle objects (non-arrays)
    if (typeof value1 === 'object' && typeof value2 === 'object') {
        const keys1 = Object.keys(value1)
        const keys2 = Object.keys(value2)
        
        // Get all unique keys from both objects
        const allKeys = new Set([...keys1, ...keys2])
        
        // Compare each key
        for (const key of allKeys) {
            if (!isEqual(value1[key], value2[key])) {
                return false
            }
        }
        
        return true
    }
    
    return false
}

export const useIsFormChanged = ({ form, initialValues }: UseIsFormChangedProps) => {
    const [isFormChanged, setIsFormChanged] = useState(false)
    const watchedFields = Form.useWatch([], form)

    useEffect(() => {
        if (!form || !watchedFields || !initialValues) {
            setIsFormChanged(false)
            return
        }

        // Get current values
        const currentValues = form.getFieldsValue()
        
        // Check each field that exists in initial values
        const hasChanges = Object.keys(initialValues).some(fieldKey => {
            const currentValue = currentValues[fieldKey]
            const initialValue = initialValues[fieldKey]
            
            return !isEqual(currentValue, initialValue)
        })
        
        // Also check if any new fields (like password) have actual values
        const newFieldChanges = Object.keys(currentValues).some(fieldKey => {
            if (!(fieldKey in initialValues)) {
                const currentValue = currentValues[fieldKey]
                // Only consider it a change if it has actual values
                if (typeof currentValue === 'object' && currentValue !== null && !Array.isArray(currentValue)) {
                    return hasValue(currentValue)
                }
                return currentValue != null && currentValue !== ''
            }
            return false
        })

        setIsFormChanged(hasChanges || newFieldChanges)
    }, [watchedFields, form, initialValues])

    return isFormChanged
}

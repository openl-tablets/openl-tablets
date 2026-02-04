import { useState, useEffect } from 'react'
import { apiCall } from '../services'
import { SystemSettings } from '../types/system'

interface AuthenticationSettingsResponse {
    defaultGroup?: string
}

/**
 * Fetches the default group name from authentication settings.
 * Only fetches when not in Single-User mode (systemSettings?.userMode is truthy).
 * Used on Users and Groups tabs to display the current default group.
 */
export function useDefaultGroup(systemSettings: SystemSettings | undefined): {
    defaultGroupName: string | undefined
    loading: boolean
} {
    const [defaultGroupName, setDefaultGroupName] = useState<string | undefined>(undefined)
    const [loading, setLoading] = useState(false)

    const shouldFetch = Boolean(systemSettings?.userMode)

    useEffect(() => {
        if (!shouldFetch) {
            setDefaultGroupName(undefined)
            setLoading(false)
            return
        }

        let cancelled = false
        setLoading(true)

        apiCall('/admin/settings/authentication')
            .then((response: AuthenticationSettingsResponse) => {
                if (!cancelled) {
                    const name = response?.defaultGroup?.trim()
                    setDefaultGroupName(name && name.length > 0 ? name : undefined)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setDefaultGroupName(undefined)
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false)
                }
            })

        return () => {
            cancelled = true
        }
    }, [shouldFetch])

    return { defaultGroupName, loading }
}

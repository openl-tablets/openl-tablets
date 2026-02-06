import React, { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { GroupItem, GroupList } from '../types/group'
import { apiCall } from '../services'
import { UserGroupType } from '../constants'
import { PermissionContext } from './PermissionContext'

type GroupsContextType = {
    groups: GroupItem[]
    loading: boolean
    error: Error | null
    reloadGroups: () => Promise<void>
}

const defaultValue: GroupsContextType = {
    groups: [],
    loading: false,
    error: null,
    reloadGroups: async () => {},
}

export const GroupsContext = createContext<GroupsContextType>(defaultValue)

/**
 * Provides the list of groups. The API /admin/management/groups is available only for admins;
 * the request is skipped when the user has no admin permission.
 */
export const GroupsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const { hasAdminPermission } = useContext(PermissionContext)
    const [groups, setGroups] = useState<GroupItem[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchGroups = useCallback(async () => {
        if (!hasAdminPermission()) {
            setGroups([])
            setError(null)
            setLoading(false)
            return
        }
        setError(null)
        setLoading(true)
        try {
            const response: GroupList = await apiCall('/admin/management/groups')
            const mapped = Object.entries(response).map(([name, group]) => ({
                name,
                oldName: name,
                admin: group.privileges?.includes(UserGroupType.Admin),
                id: group.id,
                description: group.description,
                numberOfMembers: group.numberOfMembers.total,
            }))
            setGroups(mapped)
        } catch (err) {
            setError(err instanceof Error ? err : new Error(String(err)))
            setGroups([])
        } finally {
            setLoading(false)
        }
    }, [hasAdminPermission])

    const isAdmin = hasAdminPermission()

    useEffect(() => {
        fetchGroups()
    }, [fetchGroups, isAdmin])

    const reloadGroups = useCallback(async () => {
        await fetchGroups()
    }, [fetchGroups])

    return (
        <GroupsContext.Provider value={{ groups, loading, error, reloadGroups }}>
            {children}
        </GroupsContext.Provider>
    )
}

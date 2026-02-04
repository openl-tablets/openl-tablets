import React, { createContext, useCallback, useEffect, useState } from 'react'
import { GroupItem, GroupList } from '../types/group'
import { apiCall } from '../services'
import { UserGroupType } from '../constants'

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

export const GroupsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [groups, setGroups] = useState<GroupItem[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchGroups = useCallback(async () => {
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
    }, [])

    useEffect(() => {
        fetchGroups()
    }, [fetchGroups])

    const reloadGroups = useCallback(async () => {
        await fetchGroups()
    }, [fetchGroups])

    return (
        <GroupsContext.Provider value={{ groups, loading, error, reloadGroups }}>
            {children}
        </GroupsContext.Provider>
    )
}

import { useEffect, useState } from 'react'
import { GroupItem, GroupList } from '../../types/group'
import { apiCall } from '../../services'
import { UserGroupType } from '../../constants'

export const useGroups = () => {
    const [groups, setGroups] = useState<GroupItem[]>([])

    const fetchGroups = async () => {
        const response: GroupList = await apiCall('/admin/management/groups')
        const groups = Object.entries(response).map(([name, group]) => ({
            name,
            oldName: name,
            admin: group.privileges?.includes(UserGroupType.Admin),
            id: group.id,
            description: group.description,
            numberOfMembers: group.numberOfMembers.total,
        }))
        setGroups(groups)
    }

    useEffect(() => {
        fetchGroups()
    }, [])

    const reloadGroups = async () => {
        await fetchGroups()
    }

    return {
        groups,
        reloadGroups
    }
}

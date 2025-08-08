import React, { FC, useMemo } from 'react'
import { UserDetailsGroup } from '../../types/user'
import { UserGroupType } from '../../constants'
import { Tag } from 'antd'
import { apiCall } from '../../services'
import { GroupItem } from '../../types/group'
import { EditUserGroupDetailsWithAccessRights } from '../EditUserGroupDetailsWithAccessRights'

interface RenderGroupCellProps {
    userGroups?: UserDetailsGroup[]
    notMatchedExternalGroupsCount?: number
    username: string
    groups: GroupItem[]
    reloadGroups: () => Promise<void>
    reloadUsers: () => Promise<void>
    onCloseEditDrawer: () => void
}

interface GroupWithColor extends Omit<UserDetailsGroup, 'type'> {
    color?: string
    type?: UserGroupType
}

export interface SelectedGroup extends Omit<GroupItem, 'id' | 'numberOfMembers'> {
    id?: number
    numberOfMembers?: number
}

const addColorAttribute = (group?: string): string => {
    switch (group) {
        case UserGroupType.Admin:
            return 'red'
        case UserGroupType.External:
            return 'green'
        case UserGroupType.Default:
            return 'blue'
        default:
            return 'gray'
    }
}

export const RenderGroupCell: FC<RenderGroupCellProps> = ({
    userGroups,
    groups: allGroups,
    username,
    notMatchedExternalGroupsCount,
    reloadUsers,
    reloadGroups,
    onCloseEditDrawer,
}) => {
    const [selectedGroup, setSelectedGroup] = React.useState<SelectedGroup>()
    const [notMatchedExternalGroups, setNotMatchedExternalGroups] = React.useState<string[]>([])

    const fetchExternalUserGroupsNotMatched = async () => {
        const response: string[] = await apiCall(`/users/${username}/groups/external?matched=false`)
        setNotMatchedExternalGroups(response)
    }

    const onClickGroup = (group: GroupWithColor, onOpenEditDrawer: Function) => {
        if (group.name.startsWith('+') && !group.type) {
            fetchExternalUserGroupsNotMatched()
        } else {
            const matchedGroup = allGroups.find((g) => g.name === group.name)
            if (matchedGroup) {
                setSelectedGroup(matchedGroup)
            } else {
                setSelectedGroup({ name: group.name })
            }
            // Handle click on a regular group
            onOpenEditDrawer()
        }

    }

    const groups = useMemo(() => {
        let allGroups: GroupWithColor[] = []
        // Map user groups to include color attribute
        if (userGroups && userGroups.length > 0) {
            allGroups = userGroups.map((group) => ({
                ...group,
                color: addColorAttribute(group.type),
            }))
        }

        if (notMatchedExternalGroupsCount && notMatchedExternalGroups.length === 0) {
            // Add a special group for not matched external groups
            allGroups.push({
                name: `+${notMatchedExternalGroupsCount}`
            })
        }

        if (notMatchedExternalGroups && notMatchedExternalGroups.length > 0) {
            // Map not matched external groups to include color attribute
            const notMatchedGroupsWithColor = notMatchedExternalGroups
                .filter(group => !userGroups?.some((g) => g.name === group))
                .map((group) => ({
                    name: group,
                }))
            allGroups = [...allGroups, ...notMatchedGroupsWithColor]
        }

        return allGroups
    }, [userGroups, notMatchedExternalGroups])

    return (
        <div>
            {groups
                && groups.length > 0
                && groups.map((group) => (
                    <EditUserGroupDetailsWithAccessRights
                        group={selectedGroup}
                        onClose={onCloseEditDrawer}
                        reloadGroups={reloadGroups}
                        reloadUsers={reloadUsers}
                        sid={selectedGroup?.name}
                        renderButton={(onOpenEditDrawer) => (
                            <Tag
                                key={group.name}
                                color={group.color}
                                onClick={() => onClickGroup(group, onOpenEditDrawer)}
                                style={{ cursor: 'pointer', margin: '2px' }}
                            >
                                {group.name}
                            </Tag>
                        )}
                    />
                ))}
        </div>
    )
}

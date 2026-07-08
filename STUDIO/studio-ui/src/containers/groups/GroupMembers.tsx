import React, { useEffect, useState } from 'react'
import { Flex, Spin, Tag, Tooltip, Typography } from 'antd'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { GroupMember } from '../../types/group'

interface GroupMembersProps {
    groupId: number
}

export const GroupMembers: React.FC<GroupMembersProps> = ({ groupId }) => {
    const { t } = useTranslation()
    const [members, setMembers] = useState<GroupMember[]>()
    const [failed, setFailed] = useState(false)

    useEffect(() => {
        let cancelled = false
        setMembers(undefined)
        setFailed(false)
        // throwError: the default apiCall contract swallows failures and resolves undefined;
        // suppressErrorPages: a group deleted meanwhile must not switch the whole app to the 404 page
        apiCall(`/admin/management/groups/${groupId}/users`, undefined, { suppressErrorPages: true, throwError: true })
            .then((response: GroupMember[]) => {
                if (!cancelled) {
                    setMembers(response)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setFailed(true)
                }
            })
        return () => {
            cancelled = true
        }
    }, [groupId])

    if (failed) {
        return <Typography.Text type="danger">{t('groups:failed_to_load_members')}</Typography.Text>
    }
    if (!members) {
        return <Spin data-testid="group-members-loading" size="small" />
    }
    if (!members.length) {
        return <Typography.Text type="secondary">{t('groups:no_members')}</Typography.Text>
    }
    return (
        <Flex wrap data-testid="group-members" gap={4}>
            {members.map(({ username, displayName }) => (
                <Tooltip key={username} title={displayName}>
                    <Tag>{username}</Tag>
                </Tooltip>
            ))}
        </Flex>
    )
}

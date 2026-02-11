import React, { useContext } from 'react'
import { Flex, Tag, Tooltip, Typography } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import { Trans, useTranslation } from 'react-i18next'
import { SystemContext } from '../contexts'
import { useDefaultGroup } from '../hooks/useDefaultGroup'
import { useGroups } from '../containers/groups/useGroups'

/**
 * Displays the current Default Group value with a tooltip explaining what it is and how to change it.
 * Renders nothing in Single-User mode or when the default group cannot be loaded.
 * Tag is red when the default group is an administrators group.
 * Tag is orange (warning) when the default group name is set but the group no longer exists in the list.
 * Tag is neutral (grey) while groups are still loading to avoid showing wrong color before groups are fetched.
 */
export const DefaultGroupInfo: React.FC = () => {
    const { t } = useTranslation()
    const { systemSettings } = useContext(SystemContext)
    const { defaultGroupName, loading } = useDefaultGroup(systemSettings)
    const { groups, loading: groupsLoading } = useGroups()

    if (!systemSettings?.userMode) {
        return null
    }

    if (loading) {
        return null
    }

    const hasGroup = Boolean(defaultGroupName?.trim())
    const defaultGroup = hasGroup ? groups.find((g) => g.name === defaultGroupName) : undefined
    const groupNotFound = hasGroup && !groupsLoading && defaultGroup === undefined
    const isAdminGroup = defaultGroup?.admin === true
    const tagColor =
        hasGroup && groupsLoading
            ? 'default'
            : groupNotFound
                ? 'orange'
                : isAdminGroup
                    ? 'red'
                    : 'blue'

    return (
        <Flex
            align="center"
            gap="middle"
            style={{
                marginBottom: 16,
                padding: '10px 14px',
                background: 'var(--ant-color-fill-quaternary)',
                borderRadius: 8,
            }}
        >
            <Typography.Text type="secondary">
                {t('security:default_group')}:
            </Typography.Text>
            {hasGroup ? (
                groupNotFound ? (
                    <Tooltip title={t('security:default_group_not_found')}>
                        <Tag color={tagColor}>{defaultGroupName}</Tag>
                    </Tooltip>
                ) : (
                    <Tag color={tagColor}>{defaultGroupName}</Tag>
                )
            ) : (
                <Typography.Text italic type="secondary">
                    {t('security:default_group_none')}
                </Typography.Text>
            )}
            <Tooltip title={<Trans components={{ strong: <strong /> }} i18nKey="security:default_group_tooltip" />}>
                <InfoCircleOutlined
                    aria-label={t('security:default_group_info_aria')}
                    style={{ color: 'var(--ant-color-text-tertiary)', cursor: 'help' }}
                />
            </Tooltip>
        </Flex>
    )
}

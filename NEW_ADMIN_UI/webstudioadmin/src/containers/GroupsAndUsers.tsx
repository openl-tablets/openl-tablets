import React, { useContext, useMemo } from 'react'
import { Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import { Users } from './Users'
import { Groups } from './Groups'
import { UserContext } from '../contexts/User'

export const GroupsAndUsers: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(UserContext)

    const tabs = useMemo(() => {
        const tabs = []
        if (isExternalAuthSystem) {
            tabs.push({
                key: 'groups',
                label: t('common:groups'),
                children: <Groups />,
            })
        }
        tabs.push(
            {
                key: 'users',
                label: t('common:users'),
                children: <Users />,
            }
        )
        return tabs
    }, [t, isExternalAuthSystem])

    return (
        <>
            <Tabs destroyInactiveTabPane items={tabs} />
        </>
    )
}

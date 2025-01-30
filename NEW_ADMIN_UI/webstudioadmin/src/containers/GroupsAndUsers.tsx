import React, { useContext, useEffect, useMemo, useState } from 'react'
import { Tabs } from 'antd'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { Users } from './Users'
import { Groups } from './Groups'
import { UserContext } from '../contexts/User'

interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
}

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
    }, [isExternalAuthSystem])

    return (
        <>
            <Tabs items={tabs} destroyInactiveTabPane />
        </>
    )
}

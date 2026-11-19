import React, { useContext, useEffect, useState } from 'react'
import { Menu } from 'antd'
import type { MenuProps } from 'antd'
import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import {
    AuditOutlined,
    CloudDownloadOutlined,
    DatabaseOutlined,
    KeyOutlined,
    MailOutlined,
    NotificationOutlined,
    NumberOutlined,
    SafetyOutlined,
    TeamOutlined,
    ToolOutlined,
    UserOutlined,
} from '@ant-design/icons'
import { PermissionContext, SystemContext } from '../contexts'
import { useStyles } from './MainMenu.styles'

type MenuItem = Required<MenuProps>['items'][number]

const MenuItems = {
    userProfile: '/administration/user/profile',
    userTokens: '/administration/user/tokens',
    repositories: '/administration/repositories/design',
    system: '/administration/system',
    security: '/administration/security',
    users: '/administration/admin/management/users',
    groups: '/administration/admin/management/groups',
    notification: '/administration/notification',
    tags: '/administration/tags',
    mail: '/administration/mail',
    maven: '/administration/maven',
    audit: '/administration/audit',
}

const MainMenu: React.FC = () => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const { hasAdminPermission } = useContext(PermissionContext)
    const {
        isUserManagementEnabled,
        isGroupsManagementEnabled,
        isPersonalAccessTokenEnabled,
        isAuditLogEnabled,
    } = useContext(SystemContext)
    const navigate = useNavigate()
    const location = useLocation()
    const [selectedKeys, setSelectedKeys] = useState<string[]>([])

    useEffect(() => {
        // Special handling for repositories to highlight both design and deployment tabs
        if (location.pathname.startsWith('/administration/repositories/')) {
            setSelectedKeys(['/administration/repositories/design'])
        } else {
            setSelectedKeys([location.pathname])
        }
    }, [location.pathname])

    const item = (key: string, icon: React.ReactNode, label: string): MenuItem => ({
        key, icon, label: t(`common:menu.${label}`),
    })
    const items: MenuItem[] = [
        item(MenuItems.userProfile, <UserOutlined />, 'my_profile'),
        ...(isPersonalAccessTokenEnabled
            ? [item(MenuItems.userTokens, <KeyOutlined />, 'personal_access_tokens')]
            : []),
        { type: 'divider' },
        ...(hasAdminPermission() ? [
            item(MenuItems.repositories, <DatabaseOutlined />, 'repositories'),
            item(MenuItems.system, <ToolOutlined />, 'system'),
            item(MenuItems.security, <SafetyOutlined />, 'security'),
            ...(isUserManagementEnabled ? [item(MenuItems.users, <UserOutlined />, 'users')] : []),
            ...(isGroupsManagementEnabled
                ? [item(MenuItems.groups, <TeamOutlined />, 'groups')]
                : []),
            ...(isAuditLogEnabled ? [item(MenuItems.audit, <AuditOutlined />, 'audit_log')] : []),
            item(MenuItems.notification, <NotificationOutlined />, 'notification'),
            item(MenuItems.tags, <NumberOutlined />, 'tags'),
            item(MenuItems.mail, <MailOutlined />, 'mail'),
            item(MenuItems.maven, <CloudDownloadOutlined />, 'maven'),
        ] : []),
    ]

    return (
        <div className={styles.root} id="main-menu">
            <Menu
                items={items}
                selectedKeys={selectedKeys}
                onClick={({ key }) => {
                    setSelectedKeys([key])
                    void navigate(key)
                }}
            />
        </div>
    )
}

export default MainMenu

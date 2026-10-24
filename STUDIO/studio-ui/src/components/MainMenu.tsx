import React, { useContext, useEffect, useState } from 'react'
import { Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router'
import { useTranslation } from 'react-i18next'
import {
    AuditOutlined,
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

    return (
        <div className={styles.root} id="main-menu">
            <Menu
                selectedKeys={selectedKeys}
                onClick={({ key }) => {
                    setSelectedKeys([key])
                    void navigate(key)
                }}
            >
                <Menu.Item key={MenuItems.userProfile} icon={<UserOutlined />} id="menuitem-profile">{t('common:menu.my_profile')}</Menu.Item>
                {isPersonalAccessTokenEnabled && (
                    <Menu.Item key={MenuItems.userTokens} icon={<KeyOutlined />} id="menuitem-tokens">{t('common:menu.personal_access_tokens')}</Menu.Item>
                )}
                <Menu.Divider />
                {hasAdminPermission() && (
                    <>
                        <Menu.Item key={MenuItems.repositories} icon={<DatabaseOutlined />} id="menuitem-repositories">{t('common:menu.repositories')}</Menu.Item>
                        <Menu.Item key={MenuItems.system} icon={<ToolOutlined />} id="menuitem-system">{t('common:menu.system')}</Menu.Item>
                        <Menu.Item key={MenuItems.security} icon={<SafetyOutlined />} id="menuitem-security">{t('common:menu.security')}</Menu.Item>
                        { isUserManagementEnabled &&
                            <Menu.Item key={MenuItems.users} icon={<UserOutlined />} id="menuitem-users">{t('common:menu.users')}</Menu.Item>}
                        { isGroupsManagementEnabled &&
                            <Menu.Item key={MenuItems.groups} icon={<TeamOutlined />} id="menuitem-groups">{t('common:menu.groups')}</Menu.Item>}
                        { isAuditLogEnabled &&
                            <Menu.Item key={MenuItems.audit} icon={<AuditOutlined />} id="menuitem-audit">{t('common:menu.audit_log')}</Menu.Item>}
                        <Menu.Item key={MenuItems.notification} icon={<NotificationOutlined />} id="menuitem-notification">{t('common:menu.notification')}</Menu.Item>
                        <Menu.Item key={MenuItems.tags} icon={<NumberOutlined />} id="menuitem-tags">{t('common:menu.tags')}</Menu.Item>
                        <Menu.Item key={MenuItems.mail} icon={<MailOutlined />} id="menuitem-mail">{t('common:menu.mail')}</Menu.Item>
                    </>
                )}

            </Menu>
        </div>
    )
}

export default MainMenu

import React, { useContext, useEffect, useState } from 'react'
import { Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
    DatabaseOutlined,
    MailOutlined,
    NotificationOutlined,
    NumberOutlined,
    SafetyOutlined,
    SettingOutlined,
    TeamOutlined,
    ToolOutlined,
    UserOutlined,
} from '@ant-design/icons'
import './MainMenu.scss'
import { PermissionContext, SystemContext } from '../contexts'

const MenuItems = {
    userProfile: '/administration/user/profile',
    userSettings: '/administration/user/settings',
    repositories: '/administration/repositories/design',
    system: '/administration/system',
    security: '/administration/security',
    users: '/administration/admin/management/users',
    groups: '/administration/admin/management/groups',
    notification: '/administration/notification',
    tags: '/administration/tags',
    mail: '/administration/mail',
}

const MainMenu: React.FC = () => {
    const { t } = useTranslation()
    const { hasAdminPermission } = useContext(PermissionContext)
    const { isUserManagementEnabled, isGroupsManagementEnabled } = useContext(SystemContext)
    const navigate = useNavigate()
    const location = useLocation()
    const [selectedKeys, setSelectedKeys] = useState<string[]>([])

    useEffect(() => {
        setSelectedKeys([location.pathname])
    }, [location.pathname])

    return (
        <div id="main-menu">
            <Menu
                selectedKeys={selectedKeys}
                onClick={({ key }) => {
                    setSelectedKeys([key])
                    navigate(key)
                }}
            >
                <Menu.Item key={MenuItems.userProfile} icon={<UserOutlined />} id="menuitem-profile">{t('common:menu.my_profile')}</Menu.Item>
                <Menu.Item key={MenuItems.userSettings} icon={<SettingOutlined />} id="menuitem-settings">{t('common:menu.my_settings')}</Menu.Item>
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

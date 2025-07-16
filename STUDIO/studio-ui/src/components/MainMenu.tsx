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
                <Menu.Item key={MenuItems.userProfile} id="menuitem-profile" icon={<UserOutlined />}>{t('common:menu.my_profile')}</Menu.Item>
                <Menu.Item key={MenuItems.userSettings} id="menuitem-settings" icon={<SettingOutlined />}>{t('common:menu.my_settings')}</Menu.Item>
                <Menu.Divider />
                {hasAdminPermission() && (
                    <>
                        <Menu.Item key={MenuItems.repositories} id="menuitem-repositories" icon={<DatabaseOutlined />}>{t('common:menu.repositories')}</Menu.Item>
                        <Menu.Item key={MenuItems.system} id="menuitem-system" icon={<ToolOutlined />}>{t('common:menu.system')}</Menu.Item>
                        <Menu.Item key={MenuItems.security} id="menuitem-security" icon={<SafetyOutlined />}>{t('common:menu.security')}</Menu.Item>
                        { isUserManagementEnabled &&
                            <Menu.Item key={MenuItems.users} id="menuitem-users" icon={<UserOutlined />}>{t('common:menu.users')}</Menu.Item>}
                        { isGroupsManagementEnabled &&
                            <Menu.Item key={MenuItems.groups} id="menuitem-groups" icon={<TeamOutlined />}>{t('common:menu.groups')}</Menu.Item>}
                        <Menu.Item key={MenuItems.notification} id="menuitem-notification" icon={<NotificationOutlined />}>{t('common:menu.notification')}</Menu.Item>
                        <Menu.Item key={MenuItems.tags} id="menuitem-tags" icon={<NumberOutlined />}>{t('common:menu.tags')}</Menu.Item>
                        <Menu.Item key={MenuItems.mail} id="menuitem-mail" icon={<MailOutlined />}>{t('common:menu.mail')}</Menu.Item>
                    </>
                )}

            </Menu>
        </div>
    )
}

export default MainMenu

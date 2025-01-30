import React, { useContext, useEffect, useState } from 'react'
import { Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
    DatabaseOutlined,
    MailOutlined,
    NotificationOutlined,
    NumberOutlined,
    SettingOutlined,
    TeamOutlined,
    ToolOutlined,
    UserOutlined,
} from '@ant-design/icons'
import './MainMenu.scss'
import { PermissionContext } from '../contexts/Permission'

const MainMenu: React.FC = () => {
    const { t } = useTranslation()
    const { hasAdminPermission } = useContext(PermissionContext)
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
                <Menu.Item key="/administration/user/profile" icon={<UserOutlined />}>{t('common:menu.my_profile')}</Menu.Item>
                <Menu.Item key="/administration/user/settings" icon={<SettingOutlined />}>{t('common:menu.my_settings')}</Menu.Item>
                <Menu.Divider />
                {hasAdminPermission() && (
                    <>
                        <Menu.Item key="/administration/repository/design" icon={<DatabaseOutlined />}>{t('common:menu.repositories')}</Menu.Item>
                        <Menu.Item key="/administration/system" icon={<ToolOutlined />}>{t('common:menu.system')}</Menu.Item>
                        <Menu.Item key="/administration/admin/management/groups" icon={<TeamOutlined />}>{t('common:menu.groups_and_users')}</Menu.Item>
                        <Menu.Item key="/administration/notification" icon={<NotificationOutlined />}>{t('common:menu.notification')}</Menu.Item>
                        <Menu.Item key="/administration/tags" icon={<NumberOutlined />}>{t('common:menu.tags')}</Menu.Item>
                        <Menu.Item key="/administration/mail" icon={<MailOutlined />}>{t('common:menu.mail')}</Menu.Item>
                    </>
                )}

            </Menu>
        </div>
    )
}

export default MainMenu
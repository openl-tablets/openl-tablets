import React, { useEffect, useState } from 'react'
import { Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
    DatabaseOutlined,
    FormOutlined,
    MailOutlined,
    NotificationOutlined,
    NumberOutlined,
    SettingOutlined,
    TeamOutlined,
    ToolOutlined,
    UserOutlined,
} from '@ant-design/icons'
import './MainMenu.scss'

const MainMenu: React.FC = () => {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const location = useLocation()
    const [ selectedKeys, setSelectedKeys ] = useState<string[]>([])

    useEffect(() => {
        setSelectedKeys([ location.pathname ])
    }, [ location.pathname ])

    return (
        <div id="main-menu">
            <Menu
                selectedKeys={selectedKeys}
                onClick={({ key }) => {
                    setSelectedKeys([ key ])
                    navigate(key)
                }}
            >
                <Menu.Item key="/user/profile" icon={<UserOutlined />}>{t('common:menu.my_profile')}</Menu.Item>
                <Menu.Item key="/user/settings" icon={<SettingOutlined />}>{t('common:menu.my_settings')}</Menu.Item>
                <Menu.Divider />
                <Menu.Item key="/repository/design" icon={<DatabaseOutlined />}>{t('common:menu.repositories')}</Menu.Item>
                <Menu.Item key="/system" icon={<ToolOutlined />}>{t('common:menu.system')}</Menu.Item>
                <Menu.Item key="/users" icon={<TeamOutlined />}>{t('common:menu.users')}</Menu.Item>
                <Menu.Item key="/admin/management/groups" icon={<FormOutlined />}>{t('common:menu.groups_and_privileges')}</Menu.Item>
                <Menu.Item key="/notification" icon={<NotificationOutlined />}>{t('common:menu.notification')}</Menu.Item>
                <Menu.Item key="/tags" icon={<NumberOutlined />}>{t('common:menu.tags')}</Menu.Item>
                <Menu.Item key="/mail" icon={<MailOutlined />}>{t('common:menu.mail')}</Menu.Item>
            </Menu>
        </div>
    )
}

export default MainMenu
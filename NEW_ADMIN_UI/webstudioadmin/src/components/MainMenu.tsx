import React, { useEffect, useState } from 'react'
import { Menu } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom'
import {
    FolderOutlined,
    MailOutlined,
    NotificationOutlined,
    NumberOutlined,
    SettingOutlined,
    UserOutlined,
} from '@ant-design/icons'

const MainMenu: React.FC = () => {
    const navigate = useNavigate()
    const location = useLocation()
    const [ selectedKeys, setSelectedKeys ] = useState<string[]>([])

    useEffect(() => {
        setSelectedKeys([ location.pathname ])
    }, [ location.pathname ])

    return (
        <div>
            <Menu
                selectedKeys={selectedKeys}
                onClick={({ key }) => {
                    setSelectedKeys([ key ])
                    navigate(key)
                }}
            >
                <Menu.Item key="/repository/design" icon={<FolderOutlined />}>Repositories</Menu.Item>
                <Menu.Item key="/system" icon={<SettingOutlined />}>System</Menu.Item>
                <Menu.Item key="/users" icon={<UserOutlined />}>Users</Menu.Item>
                <Menu.Item key="/admin/management/groups" icon={<UserOutlined />}>Groups & Privileges</Menu.Item>
                <Menu.Item key="/notification" icon={<NotificationOutlined />}>Notification</Menu.Item>
                <Menu.Item key="/tags" icon={<NumberOutlined />}>Tags</Menu.Item>
                <Menu.Item key="/mail" icon={<MailOutlined />}>Mail</Menu.Item>
            </Menu>
        </div>
    )
}

export default MainMenu
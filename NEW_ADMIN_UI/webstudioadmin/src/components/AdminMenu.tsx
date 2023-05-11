import React, { useEffect, useState } from 'react';
import { Menu } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { FolderOutlined, MailOutlined, NotificationOutlined, NumberOutlined, SettingOutlined, SolutionOutlined, UserOutlined } from '@ant-design/icons';

export const AdminMenu: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

    useEffect(() => {
        setSelectedKeys([location.pathname]);
    }, [location.pathname]);

    return (
        <div>
            <Menu
                onClick={({ key }) => {
                    setSelectedKeys([key]);
                    navigate(key);
                }}
                selectedKeys={selectedKeys}
            >
                <Menu.Item key="/common" icon={<SolutionOutlined />} >Common</Menu.Item>
                <Menu.Item key="/repository/design" icon={<FolderOutlined />} >Repository</Menu.Item>
                <Menu.Item key="/system" icon={<SettingOutlined />} >System</Menu.Item>
                <Menu.Item key="/users" icon={<UserOutlined />} >Users</Menu.Item>
                <Menu.Item key="/groups" icon={<UserOutlined />} >Groups & Privileges</Menu.Item>
                <Menu.Item key="/notification" icon={<NotificationOutlined />} >Notification</Menu.Item>
                <Menu.Item key="/tags" icon={<NumberOutlined />} >Tags</Menu.Item>
                <Menu.Item key="/mail" icon={<MailOutlined />} >Mail</Menu.Item>
            </Menu>
        </div>)
};

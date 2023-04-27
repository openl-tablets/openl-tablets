import React from 'react';
import { Menu } from 'antd';
import { useNavigate } from "react-router-dom";
import { FolderOutlined, MailOutlined, NotificationOutlined, NumberOutlined, SettingOutlined, SolutionOutlined, UserOutlined } from '@ant-design/icons';


export const AdminMenu: React.FC = () => {
    const navigate = useNavigate();

    return (
        <div>
            <Menu
                onClick={({ key }) => {
                    navigate(key);
                }}

                items={[
                    { label: "Common", key: "/common", icon: <SolutionOutlined /> },
                    { label: "Repository", key: "/repository/design", icon: <FolderOutlined /> },
                    { label: "System", key: "/system", icon: <SettingOutlined /> },
                    { label: "Users", key: "/users", icon: <UserOutlined /> },
                    { label: "Groups & Privileges", key: "/groups", icon: <UserOutlined /> },
                    { label: "Notification", key: "/notification", icon: <NotificationOutlined /> },
                    { label: "Tags", key: "/tags", icon: <NumberOutlined /> },
                    { label: "Mail", key: "/mail", icon: <MailOutlined /> },
                ]} >
            </Menu>
        </div>)
};

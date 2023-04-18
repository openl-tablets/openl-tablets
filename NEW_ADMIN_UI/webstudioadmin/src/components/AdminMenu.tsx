import React, { useState } from 'react';
import { Menu, MenuProps } from 'antd';
import { Link, useNavigate, Route, Routes } from "react-router-dom";
import { FolderOutlined, MailOutlined, NotificationOutlined, NumberOutlined, SettingOutlined, SolutionOutlined, UserOutlined } from '@ant-design/icons';
import { HeaderMenu } from './HeaderMenu';


export function AdminMenu() {
    const navigate = useNavigate();

    return (
        <div>
            {/* <div>
            <HheaderMenu />
            </div> */}
            <Menu 
                onClick={({ key }) => {
                    navigate(key);
                }}

                items={[
                    { label: "Common", key: "/admin/common", icon: <SolutionOutlined /> },
                    { label: "Repository", key: "/admin/repository", icon: <FolderOutlined /> },
                    { label: "System", key: "/admin/system", icon: <SettingOutlined /> },
                    { label: "Users", key: "/admin/users", icon: <UserOutlined /> },
                    { label: "Groups & Privileges", key: "/admin/groups", icon: <UserOutlined /> },
                    { label: "Notification", key: "/admin/notification", icon: <NotificationOutlined /> },
                    { label: "Tags", key: "/admin/tags", icon: <NumberOutlined /> },
                    { label: "Mail", key: "/admin/mail", icon: <MailOutlined /> },
                ]} ></Menu></div>)
};
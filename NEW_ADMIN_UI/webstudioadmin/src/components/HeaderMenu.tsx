import React, { useState } from 'react';
import { Col, Divider, Menu, MenuProps, Row } from 'antd';
import { Link, useNavigate } from "react-router-dom";

export function HeaderMenu() {
    const navigate = useNavigate();

    return (
        <div >
            <Row justify="start">
                <Col span={3}>
                    <b>
                        <Menu mode='horizontal'
                            onClick={({ key }) => {
                                navigate(key);
                            }}
                            items={[
                                { label: "WebStudio", key: "/webstudio" },
                            ]}
                        ></Menu>
                    </b>
                </Col>
                <Col span={6}>
                    <Menu mode='horizontal'
                        onClick={({ key }) => {
                            navigate(key);
                        }}
                        items={[
                            { label: "EDITOR", key: "/editor" },
                            { label: "REPOSITORY", key: "/repository" },
                            { label: "ADMIN", key: "/admin" },
                            {
                                label: "DEFAULT", key: "/default", children: [
                                    { label: "User Details", key: "/userdetails" },
                                    { label: "User Settings", key: "/usersettings" },
                                    { type: 'divider' },
                                    { label: "Help", key: "/help" },
                                ]
                            },
                        ]} ></Menu>
                </Col>
            </Row></div>)
};
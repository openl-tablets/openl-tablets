import React, { useState, useContext } from "react";
import { AdminMenu } from "../components/AdminMenu";
import { Button, Card, Table, Tag } from 'antd';
import { useNavigate, Link } from "react-router-dom";
import { NewUser } from "../components/NewUser";
import { CloseOutlined, DeleteOutlined } from "@ant-design/icons";

const userInfo =
    [
        {
            key: "1",
            userName: "a1",
            firstName: "",
            lastName: "",
            email: "a1@example.com",
            displayName: "A1",
            groups: ["Administrators"],
            action: "",
        },
        {
            key: "2",
            userName: "admin",
            firstName: "",
            lastName: "",
            email: "admin@example.com",
            displayName: "Admin",
            groups: ["Administrators"],
            action: "",
        },
        {
            key: "3",
            userName: "u0",
            firstName: "",
            lastName: "",
            email: "u0@example.com",
            displayName: "U0",
            groups: ["Testers"],
            action: "",
        },
        {
            key: "4",
            userName: "u1",
            firstName: "",
            lastName: "",
            email: "u1@example.com",
            displayName: "U1",
            groups: ["Analyst", "Developers"],
            action: "",
        },
        {
            key: "5",
            userName: "u2",
            firstName: "",
            lastName: "",
            email: "u2@example.com",
            displayName: "U2",
            groups: ["Viewers"],
            action: "",
        },
        {
            key: "6",
            userName: "u3",
            firstName: "",
            lastName: "",
            email: "u3@example.com",
            displayName: "U3",
            groups: ["Viewers"],
            action: "",
        },
        {
            key: "7",
            userName: "u4",
            firstName: "",
            lastName: "",
            email: "u4@example.com",
            displayName: "U4",
            groups: ["Deployers"],
            action: "",
        },
        {
            key: "8",
            userName: "user",
            firstName: "",
            lastName: "",
            email: "user@example.com",
            displayName: "User",
            groups: ["Viewers"],
            action: "",
        }

    ];

export const UserPage: React.FC = () => {

    const navigate = useNavigate();
    const navigateCreateuser = () => {
        let path = `/users/create`;
        navigate(path);
    }

    const [userData, setUserData] = useState(userInfo);

    const columns = [
        {
            title: 'Username',
            dataIndex: 'userName',
            key: 'userName',
        },
        {
            title: 'First name',
            dataIndex: 'firstName',
            key: 'firstName',
        },
        {
            title: 'Last name',
            dataIndex: 'lastName',
            key: 'lastName',
        },
        {
            title: 'Email',
            dataIndex: 'email',
            key: 'email',
        },
        {
            title: 'Display Name',
            dataIndex: 'displayName',
            key: 'displayName',
        },
        {
            title: 'Groups',
            dataIndex: 'groups',
            key: 'groups',
            render: (groups: string[]) => (
                <>
                    {groups.map(group => {
                        let color = "grey";
                        group === "Administrators" ? color = "red" : color = "blue";

                        return (
                            <Tag color={color} key={group} style={{ margin: 2 }}>
                                {group}
                            </Tag>
                        );
                    })}
                </>
            ),
        },
        {
            title: 'Action',
            dataIndex: 'Action',
            key: 'Action',
            render: (key: string) => (
                <Button
                    type="text"
                    icon={<CloseOutlined />}
                    onClick={() => setUserData(userData.filter(item => item.key !== key))}
                >
                </Button>
            ),
        },
    ]
    // const [data, setData] = useState(userInfo);

    const addNewUser = (newUser: { key: string; userName: string; firstName: string; lastName: string; email: string; displayName: string; groups: string[]; action: "" }) => {
        setUserData((data) => [...data, newUser]);
    }

    return (
        <div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20 }}>
                    <Table columns={columns} dataSource={userData} />
                    <Button onClick={navigateCreateuser}>Add new user</Button>
                    <NewUser addNewUser={addNewUser} />
                </Card>
            </div>
        </div>
    )
};



import React, { useState } from "react";
import { AdminMenu } from "../components/AdminMenu";
import { Button, Card, Table, Tag } from 'antd';
import { useNavigate, Link } from "react-router-dom";
import { NewUser } from "../components/NewUser";


const userInfo =
    [
        {
            key: "1",
            username: "a1",
            email: "a1@example.com",
            displayName: "A1",
            groups: ["Administrators"],
        },
        {
            key: "2",
            username: "admin",
            email: "admin@example.com",
            displayName: "Admin",
            groups: ["Administrators"],
        },
        {
            key: "3",
            username: "u0",
            email: "u0@example.com",
            displayName: "U0",
            groups: ["Testers"],
        },
        {
            key: "4",
            username: "u1",
            email: "u1@example.com",
            displayName: "U1",
            groups: ["Analyst", "Developers"],
        },
        {
            key: "5",
            username: "u2",
            email: "u2@example.com",
            displayName: "U2",
            groups: ["Viewers"],
        },
        {
            key: "6",
            username: "u3",
            email: "u3@example.com",
            displayName: "U3",
            groups: ["Viewers"],
        },
        {
            key: "7",
            username: "u4",
            email: "u4@example.com",
            displayName: "U4",
            groups: ["Deployers"],
        },
        {
            key: "8",
            username: "user",
            email: "user@example.com",
            displayName: "User",
            groups: ["Viewers"],
        }

    ];

export const UserPage:React.FC = () => {

    const navigate = useNavigate();
    const navigateCreateuser = () => {
        let path = `/users/create`;
        navigate(path);
    }

    
    const columns = [
        {
            title: 'Username',
            dataIndex: 'username',
            key: 'username',
        },
        {
            title: 'First name',
            dataIndex: 'first name',
            key: 'first name',
        },
        {
            title: 'Last name',
            dataIndex: 'last name',
            key: 'last name',
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
                            <Tag color={color} key={group} >
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
            // render: <span>
            //     <a>Delete</a>
            // </span>
        },

    ]
    const [data, setData] = useState(userInfo);

    const addNewUser = (newUser) => {
        setData((data) => [...data,newUser]);
    }

    return (
        <div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20 }}>
                    <Table columns={columns} dataSource={data} />
                    <Button onClick={navigateCreateuser}>Add new user</Button>
                    <NewUser addNewUser={addNewUser} />
                </Card>
            </div>
        </div>
    )
};



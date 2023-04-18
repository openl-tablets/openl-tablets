import React from "react";
import { HeaderMenu } from "../components/HeaderMenu";
import { AdminMenu } from "../components/AdminMenu";
import { Button, Card, Table, Tag } from 'antd';


export function UserPage() {

    const userInfo = [
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
            action: "",
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

    ]

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
            // render: groups => (
            //     <span>
            //         {groups.map(group => {
            //             let color = group.length > 10 ? 'red' : 'blue';
            //             if (group === 'Administrators') {
            //                 color = 'red';
            //             }
            //             return (
            //                 <Tag color={color} key={group} >
            //                     {group.toUppercase()}
            //                 </Tag>
            //             );
            //         })}
            //     </span>
            // ),
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

    return (
        <div>
            <div>
                <HeaderMenu />
            </div>
            <div style={{ display: "flex", flexDirection: "row" }}>
                <AdminMenu />
                <Card style={{ margin: 20 }}>
                    <Table columns={columns} dataSource={userInfo} />
                    <Button>Add new user</Button>
                </Card>
            </div>
        </div>
    )
}
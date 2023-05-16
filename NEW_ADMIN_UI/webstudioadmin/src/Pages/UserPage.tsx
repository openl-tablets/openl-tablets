import React, { useState, useContext } from 'react';
import { Button, Card, Table, Tag } from 'antd';
import { CloseOutlined } from '@ant-design/icons';
import { DataContext } from '../components/DataContext';
import DefaultLayout from '../components/DefaultLayout';
import TableUserInfo from 'views/users/TableUserInfo';
import { ModalNewUser } from 'views/users/NewUserModal';

export const UserPage: React.FC = () => {

    const { users } = useContext(DataContext);

    const [userData, setUserData] = useState(TableUserInfo);

    const columns = [
        {
            title: "Username",
            dataIndex: "userName",
            key: "userName",
        },
        {
            title: "First name",
            dataIndex: "firstName",
            key: "firstName",
        },
        {
            title: "Last name",
            dataIndex: "lastName",
            key: "lastName",
        },
        {
            title: "Email",
            dataIndex: "email",
            key: "email",
        },
        {
            title: "Display Name",
            dataIndex: "displayName",
            key: "displayName",
        },
        {
            title: "Groups",
            dataIndex: "groups",
            key: "groups",
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
            title: "Action",
            dataIndex: "Action",
            key: "Action",
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
        <DefaultLayout>
            <Card style={{ margin: 20 }}>
                <Table columns={columns} dataSource={userData} pagination={{ hideOnSinglePage: true }} />
                <ModalNewUser addNewUser={addNewUser} />
            </Card>
        </DefaultLayout>
    )
};



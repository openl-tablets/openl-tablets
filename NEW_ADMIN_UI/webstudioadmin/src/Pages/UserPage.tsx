import React, { useState, useContext } from 'react';
import { Button, Card, Modal, Table, Tag } from 'antd';
import { CloseCircleOutlined, CloseOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
import TableUserInfo from 'views/users/TableUserInfo';
import { ModalNewUser } from 'views/users/NewUserModal';
import { ModalEditUser } from 'views/users/EditUserModal';

export const UserPage: React.FC = () => {

    const [editModalVisible, setEditModalVisible] = useState(false);
    const showModal = () => {
        setEditModalVisible(true);
    };

    const handleOk = () => {
        setEditModalVisible(false);
    };

    const handleCancel = () => {
        setEditModalVisible(false);
    };
   
    const [selectedUser, setSelectedUser] = useState<any>({});

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
                    icon={<CloseCircleOutlined />}
                    onClick={() => setUserData(userData.filter(item => item.key !== key))}
                >
                </Button>
            ),
        },
    ];

    const addNewUser = (newUser: { key: string; userName: string; firstName: string; lastName: string; email: string; displayName: string; groups: string[]; action: "" }) => {
        setUserData((data) => [...data, newUser]);
    };

    const updateUser = (updatedUser: any) => {
        setUserData((userData) =>
            userData.map((user) => (user.key === updatedUser.key ? updatedUser : user)
            ));
    };


    const handleDoubleRowClick = (record: any) => {
        setSelectedUser(record);
        setEditModalVisible(true);
        console.log('Clicked row:', record);
    };

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Table
                    columns={columns}
                    dataSource={userData}
                    pagination={{ hideOnSinglePage: true }}
                    onRow={(record) => ({
                        onDoubleClick: () => handleDoubleRowClick(record),
                    })} />
                <ModalNewUser addNewUser={addNewUser} />
                <Modal
                    open={editModalVisible}
                    onCancel={handleCancel}
                    footer={[
                        <Button key="back" onClick={handleCancel}>
                            Back
                        </Button>,
                        <Button key="submit" onClick={handleOk} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                            Save
                        </Button>
                        ]}
                >
                    {editModalVisible && (
                        <ModalEditUser
                            user={selectedUser}
                            updateUser={updateUser}
                            // onClick={() => setEditModalVisible(false)}
                            onUpdateUserData={setUserData}
                        />
                    )}
                </Modal>

            </Card>
        </DefaultLayout>
    )
};



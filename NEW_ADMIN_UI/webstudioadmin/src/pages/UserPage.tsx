import React, { useState, useMemo, useEffect } from 'react'
import { Button, Card, Modal, Table, Tag } from 'antd';
import { CloseCircleOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
import { NewUserModal } from 'views/users/NewUserModal';
import { EditUserModal } from 'views/users/EditUserModal';
import './userPage.css';
import { NewUserModal1 } from 'views/users/NewUserModal1';
import { EditUserModal1 } from 'views/users/EditUserModal1';


export const UserPage: React.FC = () => {

    const apiURL = "http://localhost:8080/webstudio/rest";
    // const authorization = process.env.REACT_APP_AUTHORIZATION;

    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<any>({});
    const [userData, setUserData] = useState<any[]>([]);

    const showEditUserModal = () => {
        setIsModalOpen(true);
    };

    const hideEditUserModal = () => {
        setIsModalOpen(false);
        fetchUsers();
    };

    const fetchUsers = async () => {
        try {
            const response = await fetch(`${apiURL}/users`, {
                headers: {
                    Authorization: "Basic YWRtaW46YWRtaW4="
                },
            }
            );
            if (response.ok) {
                const jsonResponse = await response.json();
                setUserData(jsonResponse.map((user: any, index: any) => ({ ...user, key: index })));
                console.log("users: ",jsonResponse)
            } else {
                console.error("Failed to fetch users:", response.statusText);
            }
        } catch (error) {
            console.error("Error fetching users:", error);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const removeUser = (username: string) => {
        Modal.confirm({
            className: "confirm-user-modal",
            title: "Confirm Deletion",
            content: "Are you sure you want to delete this user?",

            onOk: () => {
                fetch(`${apiURL}/users/${username}`, {
                    method: "DELETE",
                    headers: {
                        Authorization: "Basic YWRtaW46YWRtaW4="
                    },
                })
                    .then(fetchUsers);
            },
            onCancel: () => {
            }
        });
    };

    const columns = [
        {
            title: "Username",
            dataIndex: "username",
            key: "username",
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
            dataIndex: "userGroups",
            key: "userGroups",
            render: (userGroups: any[]) => (
                <div>
                    {userGroups &&
                        userGroups.length > 0 &&
                        userGroups.map((userGroup) => {
                            const { name } = userGroup;
                            let color = "grey";
                            name === "Administrators" ? color = "red" : color = "blue";
                            return (
                                <Tag color={color} key={name} style={{ margin: 2 }}>
                                    {name}
                                </Tag>
                            );
                        })}
                </div>
            ),
        },
        {
            title: "Action",
            dataIndex: "Action",
            key: "Action",
            render: (text: string, record: any) => (
                <Button
                    type="text"
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeUser(record.username)}
                />
            ),
        },
    ];

    const updateUser = (updatedUser: any) => {
        setUserData((userData) =>
            userData.map((user: any) => (user.username === updatedUser.username ? updatedUser : user))
        );
    };

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record });
        showEditUserModal();
        console.log("Clicked row:", record);
    };

    // const userModalKey = useMemo(() => {
    //     return selectedUser.key + isModalOpen
    // }, [selectedUser, isModalOpen])


    // const handleEditUserSave = () => {
    //     hideEditUserModal();
    //     fetchUsers(); //
    // };

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Table
                    rowKey={(record) => record.username}
                    columns={columns}
                    dataSource={userData}
                    pagination={{ hideOnSinglePage: true }}
                    onRow={(record) => ({
                        onDoubleClick: () => handleDoubleRowClick(record),
                    })} />
                {/* <NewUserModal fetchUsers={fetchUsers} /> */}
                <NewUserModal1 fetchUsers={fetchUsers} />
                <Modal
                    className='edit-user-modal'
                    open={isModalOpen}
                    onCancel={hideEditUserModal}
                    footer={null}
                >
                    {isModalOpen && (
                        <EditUserModal1
                            user={selectedUser}
                            updateUser={updateUser}
                            onSave={hideEditUserModal}
                        />
                    )}
                </Modal>
            </Card>
        </DefaultLayout>
    )
};

import React, { useState, useMemo, useEffect } from 'react'
import { Button, Card, Modal, Table, Tag } from 'antd';
import { CloseCircleOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
// import TableUserInfo from 'views/users/TableUserInfo';
import { ModalNewUser } from 'views/users/NewUserModal';
import { EditUserModal } from 'views/users/EditUserModal';

export const UserPage: React.FC = () => {

    const apiURL = "http://localhost:8080/webstudio/rest";
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<any>({});
    const [userData, setUserData] = useState([]);

    const showNewUserModal = () => {
        setIsModalOpen(true);
    };

    const hideNewUserModal = () => {
        setIsModalOpen(false);
    };


    const fetchUsers = async () => {
        try {
            const response = await fetch(`${apiURL}/users`, {
                headers: {
                    Authorization: "Basic YWRtaW46YWRtaW4="
                }
            });
            if (response.ok) {
                const jsonResponse = await response.json();
                setUserData(jsonResponse.map((user: any, index: any) => ({ ...user, key: index })));
                console.log(jsonResponse)
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
            title: 'Confirm Deletion',
            content: 'Are you sure you want to delete this user?',

            onOk: () => {
                fetch(`${apiURL}/users/${username}`, {
                    method: "DELETE",
                    headers: {
                        Authorization: "Basic YWRtaW46YWRtaW4="
                    }
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
            dataIndex: "groups",
            key: "groups",
            // render: (groups: string[]) => (
            //     <div>
            //         {groups.map(group => {
            //             let color = "grey";
            //             group === "Administrators" ? color = "red" : color = "blue";

            //             return (
            //                 <Tag color={color} key={group} style={{ margin: 2 }}>
            //                     {group}
            //                 </Tag>
            //             );
            //         })}
            //     </div>
            // ),
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

    // const updateUser = (updatedUser: any) => {
    //     setUserData((userData) =>
    //         userData.map((user) => (user.key === updatedUser.key ? updatedUser : user))
    //     );
    //     setSelectedUser(updatedUser);
    // };

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record });
        showNewUserModal();
        console.log('Clicked row:', record);
    };

    const userModalKey = useMemo(() => {
        return selectedUser.key + isModalOpen
    }, [selectedUser, isModalOpen])


    // const handleEditUserSave = () => {
    //     updateUser(selectedUser);
    //     setEditModalVisible(false);
    //     console.log(selectedUser);
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
                <ModalNewUser
                    fetchUsers={fetchUsers}
                />
                <Modal
                    key={userModalKey}
                    open={isModalOpen}
                    onCancel={hideNewUserModal}
                    footer={[
                        // <Button key="back" onClick={handleCancel}>
                        //     Back
                        // </Button>,
                        // <Button key="submit" onClick={handleEditUserSave} style={{ marginTop: 15, color: "green", borderColor: "green" }}>
                        //     Save
                        // </Button>
                    ]}
                >
                    {/* <EditUserModal
                        user={selectedUser}
                        updateUser={updateUser}
                        onUpdateUserData={setUserData}
                        onSave={handleEditUserSave}
                    /> */}

                </Modal>

            </Card>

        </DefaultLayout>

    )
};

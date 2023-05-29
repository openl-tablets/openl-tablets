import React, { useState, useMemo, useEffect } from 'react'
import { Button, Card, Modal, Table, Tag } from 'antd';
import { CloseCircleOutlined } from '@ant-design/icons';
import DefaultLayout from '../components/DefaultLayout';
// import TableUserInfo from 'views/users/TableUserInfo';
import { ModalNewUser } from 'views/users/NewUserModal';
import { EditUserModal } from 'views/users/EditUserModal';

const JSON_HEADERS = {
    "Content-Type": "application/json",
};

export const UserPage: React.FC = () => {

    // const apiURL = "https://demo.openl-tablets.org/nightly/webstudio/rest";
    const apiURL = "http://localhost:8080/webstudio/rest"
    const [editModalVisible, setEditModalVisible] = useState(false);
    const [selectedUser, setSelectedUser] = useState<any>({});
    // const [userData, setUserData] = useState(TableUserInfo);
    const [userData, setUserData] = useState([]);

    const showModal = () => {
        setEditModalVisible(true);
    };

    const hideModal = () => {
        setEditModalVisible(false);
    };

    const fetchUsers = async () => {
        fetch(`${apiURL}/users`)
            .then((response) => response.json())
            .then((jsonResponse) => setUserData(jsonResponse));
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const removeUser = (username: any) => {
        fetch(`${apiURL}/users/` + username, {
            method: "DELETE",
        })
            .then(fetchUsers);
    };

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
            // render: (groups: string[]) => (
            //     <>
            //         {groups.map(group => {
            //             let color = "grey";
            //             group === "Administrators" ? color = "red" : color = "blue";

            //             return (
            //                 <Tag color={color} key={group} style={{ margin: 2 }}>
            //                     {group}
            //                 </Tag>
            //             );
            //         })}
            //     </>
            // ),
        },
        {
            title: "Action",
            dataIndex: "Action",
            key: "Action",
            render: (key: string) => (
                <Button
                    type="text"
                    icon={<CloseCircleOutlined />}
                // onClick={() => setUserData(userData.filter(item => item.key !== key))}

                // IDĖTI IŠTRYNIMO FUNKCIJĄ
                >
                </Button>
            ),
        },
    ];

    // const addNewUser = (newUser: { key: string; userName: string; firstName: string; lastName: string; email: string; displayName: string; groups: string[]; action: "" }) => {
    //     setUserData((data) => [...data, newUser]);
    // };

    // const updateUser = (updatedUser: any) => {
    //     setUserData((userData) =>
    //         userData.map((user) => (user.key === updatedUser.key ? updatedUser : user))
    //     );
    //     setSelectedUser(updatedUser);
    // };

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record });
        showModal();
        console.log('Clicked row:', record);
    };

    const userModalKey = useMemo(() => {
        return selectedUser.key + editModalVisible
    }, [selectedUser, editModalVisible])


    // const handleEditUserSave = () => {
    //     updateUser(selectedUser);
    //     setEditModalVisible(false);
    //     console.log(selectedUser);
    // };

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Table
                    // rowKey={(record) => record.username}
                    columns={columns}
                    dataSource={userData}
                    pagination={{ hideOnSinglePage: true }}
                    onRow={(record) => ({
                        onDoubleClick: () => handleDoubleRowClick(record),
                    })} />
                <ModalNewUser
                // addNewUser={addNewUser}
                />
                <Modal
                    key={userModalKey}
                    open={editModalVisible}
                    onCancel={hideModal}
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

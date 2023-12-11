import React, { useState, useEffect } from 'react'
import { Button, Card, Modal, Table, Tag } from 'antd'
import { CloseCircleOutlined } from '@ant-design/icons'
import { NewUserModal } from 'containers/users/NewUserModal'
import { EditUserModal } from 'containers/users/EditUserModal'
import DefaultLayout from 'layouts/DefaultLayout'
import './UserPage.scss'
import { apiCall } from 'services'

export const UserPage: React.FC = () => {
    const [ isModalOpen, setIsModalOpen ] = useState(false)
    const [ selectedUser, setSelectedUser ] = useState<any>({})
    const [ userData, setUserData ] = useState<any[]>([])

    const showEditUserModal = () => {
        setIsModalOpen(true)
    }

    const hideEditUserModal = () => {
        setIsModalOpen(false)
        fetchUsers()
    }

    const fetchUsers = async () => {
        try {
            const response = await apiCall('/users')
            if (response.ok) {
                const jsonResponse = await response.json()
                setUserData(jsonResponse.map((user: any, index: any) => ({ ...user, key: index })))
            } else {
                console.error('Failed to fetch users:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching users:', error)
        }
    }

    useEffect(() => {
        fetchUsers()
    }, [])

    const removeUser = (username: string) => {
        Modal.confirm({
            className: 'confirm-user-modal',
            title: 'Confirm Deletion',
            content: 'Are you sure you want to delete this user?',
            onOk: () => {
                apiCall(`/users/${username}`, {
                    method: 'DELETE',
                })
                    .then(fetchUsers)
            },
            onCancel: () => {},
        })
    }

    const columns = [
        {
            title: 'Username',
            dataIndex: 'username',
            key: 'username',
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
            dataIndex: 'userGroups',
            key: 'userGroups',
            render: (userGroups: any[]) => (
                <div>
                    {userGroups
                        && userGroups.length > 0
                        && userGroups.map((userGroup) => {
                            const { name } = userGroup
                            let color = name === 'Administrators' ? 'red' : 'blue'
                            return (
                                <Tag key={name} color={color} style={{ margin: 2 }}>
                                    {name}
                                </Tag>
                            )
                        })}
                </div>
            ),
        },
        {
            title: 'Action',
            dataIndex: 'Action',
            key: 'Action',
            render: (text: string, record: any) => (
                <Button
                    icon={<CloseCircleOutlined />}
                    type="text"
                    onClick={() => removeUser(record.username)}
                />
            ),
        },
    ]

    const updateUser = (updatedUser: any) => {
        setUserData((userData) => userData.map((user: any) => (user.username === updatedUser.username ? updatedUser : user)))
    }

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record })
        showEditUserModal()
    }

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Table
                    columns={columns}
                    dataSource={userData}
                    pagination={{ hideOnSinglePage: true }}
                    rowKey={(record) => record.username}
                    onRow={(record) => ({
                        onDoubleClick: () => handleDoubleRowClick(record),
                    })}
                />
                <NewUserModal fetchUsers={fetchUsers} />
                <Modal
                    className="edit-user-modal"
                    footer={null}
                    open={isModalOpen}
                    onCancel={hideEditUserModal}
                >
                    {isModalOpen && (
                        <EditUserModal
                            updateUser={updateUser}
                            user={selectedUser}
                            onSave={hideEditUserModal}
                        />
                    )}
                </Modal>
            </Card>
        </DefaultLayout>
    )
}

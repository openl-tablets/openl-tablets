import React, { useState, useEffect, useMemo } from 'react'
import { Button, Modal, Row, Table, Tag } from 'antd'
import { CloseCircleOutlined } from '@ant-design/icons'
import { AddAndEditUserModal } from './users/components/AddAndEditUserModal'
import { apiCall } from 'services'
import './Users.scss'
import { useTranslation } from 'react-i18next'

interface EditUserRequest {
    displayName: string
    email: string
    firstName: string
    groups: string[]
    lastName: string
    password: string
    username?: string
    internalPassword?: {
        password: string
    }
}

export const Users: React.FC = () => {
    const { t } = useTranslation()
    const [ isModalOpen, setIsModalOpen ] = useState(false)
    const [ selectedUser, setSelectedUser ] = useState<any>({})
    const [ userData, setUserData ] = useState<any[]>([])
    const [ isNewUser, setIsNewUser ] = useState(false)

    const showAddAndEditUserModal = () => {
        setIsModalOpen(true)
    }

    const hideAddAndEditUserModal = () => {
        setIsModalOpen(false)
        setSelectedUser({})
        setIsNewUser(false)
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
            title: t('users:confirm_deletion'),
            content: t('users:confirm_delete_user'),
            onOk: () => {
                apiCall(`/users/${username}`, {
                    method: 'DELETE',
                })
                    .then(fetchUsers)
            },
            onCancel: () => {},
        })
    }

    const onAddNewUser = () => {
        setIsNewUser(true)
        showAddAndEditUserModal()
    }

    const onSubmitUserModal = async (userData: any) => {
        const userDataMapped: EditUserRequest = {
            email: userData.email,
            displayName: userData.displayName,
            firstName: userData.firstName,
            lastName: userData.lastName,
            password: userData.password,
            groups: userData.groups,
        }

        if (isNewUser) {
            userDataMapped.username = userData.username
            userDataMapped.internalPassword = {
                password: userData.password
            }
        }

        try {
            const url = isNewUser ? '/users' : `/users/${userData.username}`

            await apiCall(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(userDataMapped),
            })
        } catch (error) {
            console.error('Error updating user:', error)
        } finally {
            hideAddAndEditUserModal()
        }
    }

    const columns = [
        {
            title: t('users:users_table.username'),
            dataIndex: 'username',
            key: 'username',
        },
        {
            title: t('users:users_table.first_name'),
            dataIndex: 'firstName',
            key: 'firstName',
        },
        {
            title: t('users:users_table.last_name'),
            dataIndex: 'lastName',
            key: 'lastName',
        },
        {
            title: t('users:users_table.email'),
            dataIndex: 'email',
            key: 'email',
        },
        {
            title: t('users:users_table.display_name'),
            dataIndex: 'displayName',
            key: 'displayName',
        },
        {
            title: t('users:users_table.groups'),
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
            title: t('users:users_table.actions'),
            render: (_: string, record: any) => (
                <Button
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeUser(record.username)}
                    type="text"
                />
            ),
        },
    ]

    // const updateUser = (updatedUser: any) => {
    //     setUserData((userData) => userData.map((user: any) => (user.username === updatedUser.username ? updatedUser : user)))
    // }

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record })
        showAddAndEditUserModal()
    }

    const modalTitle = useMemo(() => {
        return isNewUser ? t('users:add_user') : t('users:edit_user')
    }, [ isNewUser, t ])

    return (
        <>
            <Table
                columns={columns}
                dataSource={userData}
                pagination={{ hideOnSinglePage: true }}
                rowKey={(record) => record.username}
                onRow={(record) => ({
                    onDoubleClick: () => handleDoubleRowClick(record),
                })}
            />
            <Row justify="end">
                <Button
                    onClick={onAddNewUser}
                    style={{ marginTop: 20 }}
                    type="primary"
                >
                    {t('users:add_user')}
                </Button>
            </Row>
            <Modal
                className="edit-user-modal"
                footer={null}
                onCancel={hideAddAndEditUserModal}
                open={isModalOpen}
                title={modalTitle}
            >
                <AddAndEditUserModal
                    isNewUser={isNewUser}
                    onCancel={hideAddAndEditUserModal}
                    onSubmit={onSubmitUserModal}
                    user={selectedUser}
                />
            </Modal>
        </>
    )
}

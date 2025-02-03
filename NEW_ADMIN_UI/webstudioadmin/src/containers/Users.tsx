import React, { useState, useEffect, useMemo, useContext } from 'react'
import { Badge, Button, Modal, Row, Table } from 'antd'
import { CloseCircleOutlined, EditOutlined } from '@ant-design/icons'
import { EditUserModal } from './users/EditUserModal'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { UserContext } from '../contexts/User'
import { UserGroupType } from '../constants'
import { UserDetails, UserProfile } from '../types/user'
import { ColumnsType } from 'antd/es/table/interface'

export const Users: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(UserContext)
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [selectedUser, setSelectedUser] = useState<any>({})
    const [usersData, setUsersData] = useState<any[]>([])
    const [isLoading, setIsLoading] = useState(false)

    const showEditUserModal = () => {
        setIsModalOpen(true)
    }

    const hideEditUserModal = () => {
        setIsModalOpen(false)
        setSelectedUser({})
    }

    const fetchUsers = async () => {
        setIsLoading(true)
        const response: UserDetails[] = await apiCall('/users')
        setUsersData(response.map((user: any, index: any) => ({ ...user, key: index })))
        setIsLoading(false)
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

    const handleDoubleRowClick = (record: any) => {
        setSelectedUser({ ...record })
        showEditUserModal()
    }

    const updateUser = (updatedUser: UserProfile) => {
        if (updatedUser.username) {
            setUsersData((userData) => userData.map((user) => (user.username === updatedUser.username ? { ...user, ...updatedUser } : user)))
        }
    }

    const columns = useMemo(() => {
        const columns: ColumnsType<UserProfile> = [
            {
                title: t('users:users_table.username'),
                key: 'username',
                render: ({ username, userGroups }) => {
                    if (Array.isArray(userGroups) && userGroups.some(({ type }) => type === UserGroupType.Admin)) {
                        return <Badge dot color="blue">{username}</Badge>
                    }

                    return username
                },
            },
            {
                title: t('users:users_table.full_name'),
                dataIndex: 'displayName',
                key: 'displayName',
            },
            {
                title: t('users:users_table.email'),
                dataIndex: 'email',
                key: 'email',
            },
            {
                title: t('users:users_table.groups'),
                dataIndex: 'userGroups',
                key: 'userGroups',
                render: (userGroups: any[]) => (
                    <div>
                        {userGroups
                            && userGroups.length > 0
                            && userGroups.map(({ name }) =>  name).join(', ')}
                    </div>
                ),
            },
            {
                title: t('users:users_table.actions'),
                width: 100,
                render: (_: string, record: any) => (
                    <>
                        <Button
                            icon={<EditOutlined />}
                            onClick={() => handleDoubleRowClick(record)}
                            type="text"
                        />
                        <Button
                            icon={<CloseCircleOutlined />}
                            onClick={() => removeUser(record.username)}
                            type="text"
                        />
                    </>
                ),
            },
        ]

        if (!isExternalAuthSystem) {
            return columns.filter((column) => column.key !== 'userGroups')
        }

        return columns
    }, [t, isExternalAuthSystem, handleDoubleRowClick, removeUser])

    return (
        <>
            <Table
                columns={columns}
                dataSource={usersData}
                loading={isLoading}
                pagination={{ hideOnSinglePage: true }}
                rowKey={(record) => record.username}
                onRow={(record) => ({
                    onDoubleClick: () => handleDoubleRowClick(record),
                })}
            />
            <Row justify="end">
                <Button
                    onClick={showEditUserModal}
                    style={{ marginTop: 20 }}
                    type="primary"
                >
                    {t('users:add_user')}
                </Button>
            </Row>
            <Modal
                destroyOnClose
                footer={null}
                onCancel={hideEditUserModal}
                open={isModalOpen}
                width={600}
            >
                {isModalOpen && (
                    <EditUserModal
                        closeModal={hideEditUserModal}
                        onAddUser={fetchUsers}
                        updateUser={updateUser}
                        user={selectedUser}
                    />
                )}
            </Modal>
        </>
    )
}

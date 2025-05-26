import React, { useState, useEffect, useMemo, useContext } from 'react'
import { Badge, Button, Dropdown, Modal, Row, Table, Typography } from 'antd'
import { CloseCircleOutlined, EditOutlined, EllipsisOutlined, FolderViewOutlined } from '@ant-design/icons'
import { EditUserModal } from './users/EditUserModal'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../contexts'
import { UserGroupType } from '../constants'
import { UserDetails, UserProfile } from '../types/user'
import { ColumnsType } from 'antd/es/table/interface'
import { AccessManagementModal } from '../components/accessManagement'

export const Users: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(SystemContext)
    const [isEditDetailsModalOpen, setIsEditDetailsModalOpen] = useState(false)
    const [isEditAccessRightsModalOpen, setIsEditAccessRightsModalOpen] = useState(false)
    const [selectedUser, setSelectedUser] = useState<any>({})
    const [usersData, setUsersData] = useState<any[]>([])
    const [isLoading, setIsLoading] = useState(false)

    const showEditUserModal = () => {
        setIsEditDetailsModalOpen(true)
    }

    const hideEditUserModal = () => {
        setIsEditDetailsModalOpen(false)
        setSelectedUser({})
    }

    const showEditAccessRightsModal = () => {
        setIsEditAccessRightsModalOpen(true)
    }

    const hideEditAccessRightsModal = () => {
        setIsEditAccessRightsModalOpen(false)
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

    const onEditUser = (record: any) => {
        setSelectedUser({ ...record })
        showEditUserModal()
    }

    const onEditAccessRights = (record: any) => {
        setSelectedUser({ ...record })
        showEditAccessRightsModal()
    }

    const updateUser = (updatedUser: UserProfile) => {
        if (updatedUser.username) {
            setUsersData((userData) => userData.map((user) => (user.username === updatedUser.username ? { ...user, ...updatedUser } : user)))
        }
    }

    const actionItems = (record: any) => [
        {
            key: 'edit',
            label: (
                <Button
                    icon={<EditOutlined />}
                    onClick={() => onEditUser(record)}
                    type="text"
                >
                    {t('users:action.edit_details')}
                </Button>
            ),
        },
        {
            key : 'access',
            label : (
                <Button
                    icon={<FolderViewOutlined />}
                    onClick={() => onEditAccessRights(record)}
                    type="text"
                >
                    {t('users:action.edit_access_rights')}
                </Button>
            )
        },
        {
            key: 'delete',
            label: (
                <Button
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeUser(record.username)}
                    type="text"
                >
                    {t('users:action.delete_user')}
                </Button>
            ),
        }
    ]

    const columns = useMemo(() => {
        const columns: ColumnsType<UserProfile> = [
            {
                title: t('users:users_table.username'),
                key: 'username',
                render: ({ username, online, userGroups }) => {
                    const userNameComponent = Array.isArray(userGroups) && userGroups.some(({ type }) => type === UserGroupType.Admin)
                        ? <Typography.Text strong>{username}</Typography.Text>
                        : username

                    if (online) {
                        return <Badge dot color="green">{userNameComponent}</Badge>
                    }

                    return userNameComponent
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
                    <Dropdown menu={{ items: actionItems(record) }} overlayClassName="table-actions-dropdown">
                        <Button type="text">
                            <EllipsisOutlined />
                        </Button>
                    </Dropdown>
                ),
            },
        ]

        if (!isExternalAuthSystem) {
            return columns.filter((column) => column.key !== 'userGroups')
        }

        return columns
    }, [t, isExternalAuthSystem, onEditUser, removeUser])

    return (
        <>
            <Table
                columns={columns}
                dataSource={usersData}
                loading={isLoading}
                pagination={{ hideOnSinglePage: true }}
                rowKey={(record) => record.username}
                onRow={(record) => ({
                    onDoubleClick: () => onEditUser(record),
                })}
            />
            {!isExternalAuthSystem && (
                <Row justify="end">
                    <Button
                        onClick={showEditUserModal}
                        style={{ marginTop: 20 }}
                        type="primary"
                    >
                        {t('users:add_user')}
                    </Button>
                </Row>
            )}
            <Modal
                destroyOnClose
                footer={null}
                onCancel={hideEditUserModal}
                open={isEditDetailsModalOpen}
                width={600}
            >
                {isEditDetailsModalOpen && (
                    <EditUserModal
                        closeModal={hideEditUserModal}
                        onAddUser={fetchUsers}
                        updateUser={updateUser}
                        user={selectedUser}
                    />
                )}
            </Modal>
            {isEditAccessRightsModalOpen && (
                <AccessManagementModal
                    isOpen={isEditAccessRightsModalOpen}
                    isPrincipal={true}
                    onCloseModal={hideEditAccessRightsModal}
                    sid={selectedUser.username}
                />
            )}
        </>
    )
}

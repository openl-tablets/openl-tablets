import React, { useState, useEffect, useMemo, useContext } from 'react'
import { Badge, Button, Modal, Row, Table, Typography, Tooltip } from 'antd'
import { DeleteOutlined, EditOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { SystemContext } from '../contexts'
import { UserGroupType } from '../constants'
import { UserDetails } from '../types/user'
import { ColumnsType } from 'antd/es/table/interface'
import { RenderGroupCell } from './users/RenderGroupCell'
import { useGroups } from './groups/useGroups'
import { EditUserGroupDetailsWithAccessRights } from './EditUserGroupDetailsWithAccessRights'
import { ExclamationCircleTwoTone } from '@ant-design/icons'

export const Users: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem, systemSettings } = useContext(SystemContext)
    const [selectedUser, setSelectedUser] = useState<UserDetails>()
    const [users, setUsers] = useState<UserDetails[]>([])
    const { groups, reloadGroups } = useGroups()
    const [isLoading, setIsLoading] = useState(false)
    const [isEditDrawerOpen, setIsEditDrawerOpen] = useState(false)

    const showEditUserDrawer = () => {
        setIsEditDrawerOpen(true)
    }

    const hideEditUserDrawer = () => {
        setIsEditDrawerOpen(false)
        setSelectedUser(undefined)
    }

    const fetchUsers = async () => {
        setIsLoading(true)
        const response: UserDetails[] = await apiCall('/users')
        setUsers(response.map((user: any, index: any) => ({ ...user, key: index })))
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

    const onEditUser = (record: any, event?: any) => {
        if (event && !event?.target?.className.includes('ant-table-cell')) {
            return
        }
        setSelectedUser({ ...record })
        showEditUserDrawer()
    }

    const columns = useMemo(() => {
        const columns: ColumnsType<UserDetails> = [
            {
                title: t('users:users_table.username'),
                key: 'username',
                render: ({ username, online, userGroups, unsafePassword }) => {
                    const userNameComponent = Array.isArray(userGroups) && userGroups.some(({ type }) => type === UserGroupType.Admin)
                        ? [<Typography.Text strong>{username}</Typography.Text>]
                        : [username]

                    if (unsafePassword) {
                        userNameComponent.push(
                            <Tooltip title={t('users:unsafe_default_password')}>
                                <ExclamationCircleOutlined style={{ color: 'red', marginLeft: 6 }} />
                            </Tooltip>
                        )
                    }

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
                render: (email, record) => {
                    if (record.externalFlags && !record.externalFlags.emailVerified && systemSettings?.supportedFeatures?.emailVerification) {
                        return (
                            <span>
                                {email}{' '}
                                <Tooltip title={t('users:email_not_verified')}>
                                    <ExclamationCircleTwoTone twoToneColor="#faad14" />
                                </Tooltip>
                            </span>
                        )
                    }
                    return email
                },
            },
            {
                title: t('users:users_table.groups'),
                dataIndex: 'userGroups',
                key: 'userGroups',
                render: (_, { userGroups, notMatchedExternalGroupsCount, username }) => (
                    <RenderGroupCell
                        groups={groups}
                        notMatchedExternalGroupsCount={notMatchedExternalGroupsCount}
                        onCloseEditDrawer={hideEditUserDrawer}
                        reloadGroups={reloadGroups}
                        reloadUsers={fetchUsers}
                        userGroups={userGroups}
                        username={username}
                    />
                ),
            },
            {
                title: t('users:users_table.actions'),
                width: 150,
                render: (_: string, record: any) => {
                    const canDelete = !record.superUser && !record.currentUser
                    const deleteTooltip = record.superUser
                        ? t('users:cannot_delete_superuser')
                        : record.currentUser
                            ? t('users:cannot_delete_yourself')
                            : ''
                    return (
                        <>
                            <Button
                                icon={<EditOutlined />}
                                onClick={() => onEditUser(record)}
                                type="text"
                            />
                            <Tooltip title={deleteTooltip}>
                                <Button
                                    disabled={!canDelete}
                                    icon={<DeleteOutlined />}
                                    onClick={canDelete ? () => removeUser(record.username) : undefined}
                                    type="text"
                                />
                            </Tooltip>
                        </>
                    )
                },
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
                dataSource={users}
                loading={isLoading}
                pagination={{ hideOnSinglePage: true }}
                rowKey={(record) => record.username}
                onRow={(record) => ({
                    onDoubleClick: (event) => onEditUser(record, event),
                })}
            />
            {!isExternalAuthSystem && (
                <Row justify="end">
                    <Button
                        onClick={showEditUserDrawer}
                        style={{ marginTop: 20 }}
                        type="primary"
                    >
                        {t('users:add_user')}
                    </Button>
                </Row>
            )}
            <EditUserGroupDetailsWithAccessRights
                isOpenFromParent={isEditDrawerOpen}
                isPrincipal={true}
                newUser={!selectedUser}
                onClose={hideEditUserDrawer}
                reloadUsers={fetchUsers}
                sid={selectedUser?.username}
                user={selectedUser}
            />
        </>
    )
}

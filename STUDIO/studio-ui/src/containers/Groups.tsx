import React, { useState } from 'react'
import { Button, Table, Modal, Row } from 'antd'
import { DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { GroupItem } from '../types/group'
import { useGroups } from './groups/useGroups'
import { EditUserGroupDetailsWithAccessRights } from './EditUserGroupDetailsWithAccessRights'

export const Groups: React.FC = () => {
    const { t } = useTranslation()
    const [selectedGroup, setSelectedGroup] = useState<GroupItem | undefined>()
    const { groups, reloadGroups } = useGroups()
    const [isEditDrawerOpen, setIsEditDrawerOpen] = useState(false)

    const showEditGroupDrawer = () => {
        setIsEditDrawerOpen(true)
    }

    const hideEditGroupDrawer = () => {
        setIsEditDrawerOpen(false)
        setSelectedGroup(undefined)
    }

    const removeGroup = (id: number) => {
        Modal.confirm({
            className: 'confirm-group-modal',
            title: t('groups:confirm_deletion_title'),
            content: t('groups:confirm_deletion'),
            onOk: () => {
                apiCall(`/admin/management/groups/${id}`, {
                    method: 'DELETE'
                })
                    .then(reloadGroups)
            },
            onCancel: () => {},
        })
    }

    const onEditGroup = (record: GroupItem) => {
        showEditGroupDrawer()
        setSelectedGroup({ ...record })
    }

    const columns = [
        {
            title: t('groups:table.name'),
            dataIndex: 'name',
            key: 'name',
        },
        {
            title: t('groups:table.description'),
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: t('groups:table.members'),
            dataIndex: 'numberOfMembers',
            key: 'numberOfMembers',
        },
        {
            title: t('groups:table.actions'),
            key: 'Action',
            width: 150,
            render: (_: string, record: any) => (
                <>
                    <Button
                        icon={<EditOutlined />}
                        onClick={() => onEditGroup(record)}
                        type="text"
                    />
                    <Button
                        icon={<DeleteOutlined />}
                        onClick={() => removeGroup(record.id)}
                        type="text"
                    />
                </>
            ),
        },
    ]

    return (
        <>
            <Table
                columns={columns}
                dataSource={groups}
                pagination={{ hideOnSinglePage: true }}
                onRow={(record) => ({
                    onDoubleClick: () => onEditGroup(record),
                })}
            />
            <Row justify="end">
                <Button onClick={showEditGroupDrawer} style={{ marginTop: 20 }} type="primary">
                    {t('groups:invite_group')}
                </Button>
            </Row>
            <EditUserGroupDetailsWithAccessRights
                group={selectedGroup}
                isOpenFromParent={isEditDrawerOpen}
                onClose={hideEditGroupDrawer}
                reloadGroups={reloadGroups}
                sid={selectedGroup?.name}
            />
        </>
    )
}

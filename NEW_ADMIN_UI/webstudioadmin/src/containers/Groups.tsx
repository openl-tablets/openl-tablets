import React, { useContext, useEffect, useState } from 'react'
import { Button, Table, Modal } from 'antd'
import { CloseCircleOutlined, EditOutlined } from '@ant-design/icons'
import { NewGroupModal } from 'containers/groups/NewGroupModal'
import { EditGroupModal } from 'containers/groups/EditGroupModal'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { UserContext } from '../contexts/User'
import { Role, UserGroupType } from '../constants'

export interface Group {
    oldName?: string;
    name: string;
    id: number;
    description?: string;
    designRole?: Role
    prodRole?: Role
    admin?: boolean;
}

export const Groups: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(UserContext)
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [groupData, setGroupData] = useState<Group[]>([])
    const [selectedGroup, setSelectedGroup] = useState<any>({})

    const showEditGroupModal = () => {
        setIsModalOpen(true)
    }

    const hideEditGroupModal = () => {
        setIsModalOpen(false)
    }

    const fetchGroups = async () => {
        const response = await apiCall('/admin/management/groups')
        const groups = Object.entries(response).map(([name, group]: [string, unknown]) => ({
            name,
            oldName: name,
            admin: group.privileges?.includes(UserGroupType.ADMIN),
            id: group.id,
            description: group.description,
        }))
        setGroupData(groups)
    }

    useEffect(() => {
        fetchGroups()
    }, [])

    const removeGroup = (id: number) => {
        Modal.confirm({
            className: 'confirm-group-modal',
            title: t('groups:confirm_deletion_title'),
            content: t('groups:confirm_deletion'),
            onOk: () => {
                apiCall(`/admin/management/groups/${id}`, {
                    method: 'DELETE'
                })
                    .then(fetchGroups)
            },
            onCancel: () => {},
        })
    }

    const updateGroup = (updatedGroup: any) => {
        console.log('updatedGroup', updatedGroup)
        setGroupData((groupData) => groupData.map((group: any) => (group.id === updatedGroup.id ? updatedGroup : group)))
    }

    const handleDoubleRowClick = (record: any) => {
        setSelectedGroup({ ...record })
        showEditGroupModal()
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
            dataIndex: 'members',
            key: 'members',
        },
        {
            title: t('groups:table.actions'),
            key: 'Action',
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
                dataSource={groupData}
                pagination={{ hideOnSinglePage: true }}
                onRow={(record) => ({
                    onDoubleClick: () => handleDoubleRowClick(record),
                })}
            />
            <NewGroupModal fetchGroups={fetchGroups} />
            <Modal
                destroyOnClose
                className="edit-group-modal"
                footer={null}
                onCancel={hideEditGroupModal}
                open={isModalOpen}
                width={600}
            >
                {isModalOpen && (
                    <EditGroupModal
                        group={selectedGroup}
                        onSave={hideEditGroupModal}
                        updateGroup={updateGroup}
                    />
                )}
            </Modal>
        </>
    )
}

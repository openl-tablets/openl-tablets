import React, { useEffect, useState } from 'react'
import { Button, Table, Modal, Row, Dropdown } from 'antd'
import { CloseCircleOutlined, EditOutlined, EllipsisOutlined, FolderViewOutlined } from '@ant-design/icons'
import { EditGroupModal } from 'containers/groups/EditGroupModal'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'
import { UserGroupType } from '../constants'
import { GroupList, GroupTableItem } from '../types/group'
import { AccessManagementModal } from '../components/accessManagement'

export const Groups: React.FC = () => {
    const { t } = useTranslation()
    const [isEditDetailsModalOpen, setIsEditDetailsModalOpen] = useState(false)
    const [isEditAccessRightsModalOpen, setIsEditAccessRightsModalOpen] = useState(false)
    const [groupData, setGroupData] = useState<GroupTableItem[]>([])
    const [selectedGroup, setSelectedGroup] = useState<GroupTableItem | undefined>()

    const showEditGroupModal = () => {
        setIsEditDetailsModalOpen(true)
    }

    const hideEditGroupModal = () => {
        setSelectedGroup(undefined)
        setIsEditDetailsModalOpen(false)
    }

    const showEditAccessRightsModal = () => {
        setIsEditAccessRightsModalOpen(true)
    }

    const hideEditAccessRightsModal = () => {
        setIsEditAccessRightsModalOpen(false)
        setSelectedGroup(undefined)
    }

    const onEditAccessRights = (record: GroupTableItem) => {
        setSelectedGroup({ ...record })
        showEditAccessRightsModal()
    }

    const fetchGroups = async () => {
        const response: GroupList = await apiCall('/admin/management/groups')
        const groups = Object.entries(response).map(([name, group]) => ({
            name,
            oldName: name,
            admin: group.privileges?.includes(UserGroupType.Admin),
            id: group.id,
            description: group.description,
            numberOfMembers: group.numberOfMembers.total,
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
        if (updatedGroup.id) {
            setGroupData((groupData) => groupData.map((group: any) => (group.id === updatedGroup.id ? updatedGroup : group)))
        }
    }

    const handleDoubleRowClick = (record: GroupTableItem) => {
        setSelectedGroup({ ...record })
        showEditGroupModal()
    }

    const actionItems = (record: GroupTableItem) => [
        {
            key: 'edit',
            label: (
                <Button
                    icon={<EditOutlined />}
                    onClick={() => handleDoubleRowClick(record)}
                    type="text"
                >
                    {t('groups:action.edit_details')}
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
                    {t('groups:action.edit_access_rights')}
                </Button>
            )
        },
        {
            key: 'delete',
            label: (
                <Button
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeGroup(record!.id)}
                    type="text"
                >
                    {t('groups:action.delete_group')}
                </Button>
            ),
        },
    ]

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
                <Dropdown menu={{ items: actionItems(record) }} overlayClassName="table-actions-dropdown">
                    <Button type="text">
                        <EllipsisOutlined />
                    </Button>
                </Dropdown>
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
            <Row justify="end">
                <Button onClick={showEditGroupModal} style={{ marginTop: 20 }} type="primary">
                    {t('groups:invite_group')}
                </Button>
            </Row>
            <Modal
                destroyOnClose
                footer={null}
                onCancel={hideEditGroupModal}
                open={isEditDetailsModalOpen}
                width={600}
            >
                {isEditDetailsModalOpen && (
                    <EditGroupModal
                        closeModal={hideEditGroupModal}
                        group={selectedGroup}
                        onAddGroup={fetchGroups}
                        updateGroup={updateGroup}
                    />
                )}
            </Modal>
            {isEditAccessRightsModalOpen && selectedGroup && (
                <AccessManagementModal
                    isOpen={isEditAccessRightsModalOpen}
                    isPrincipal={false}
                    onCloseModal={hideEditAccessRightsModal}
                    sid={selectedGroup.name}
                />
            )}
        </>
    )
}

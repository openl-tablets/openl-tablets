import React, { useEffect, useState } from 'react'
import { Button, Table, Tag, Form, Input, Divider, Row, Col, Modal } from 'antd'
import { CloseCircleOutlined } from '@ant-design/icons'
import { NewGroupModal } from 'containers/groups/NewGroupModal'
import { EditGroupModal } from 'containers/groups/EditGroupModal'
import { apiCall } from 'services'
import { useTranslation } from 'react-i18next'

interface Group {
    name: string;
    id: number;
    description: string;
    roles: string[];
    privileges: string[];
}

export const Groups: React.FC = () => {
    const { t } = useTranslation()
    const [ isModalOpen, setIsModalOpen ] = useState(false)
    const [ groupData, setGroupData ] = useState<Group[]>([])
    const [ selectedGroup, setSelectedGroup ] = useState<any>({})

    const showEditGroupModal = () => {
        setIsModalOpen(true)
    }

    const hideEditGroupModal = () => {
        setIsModalOpen(false)
    }

    const fetchGroups = async () => {
        const response = await apiCall('/admin/management/groups')
        const groups = Object.entries(response).map(([ groupName, group ]: [string, unknown]) => ({
            groupName,
            ...(group as Group),
            privileges: (group as Group).privileges || [],
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
        setGroupData((groupData) => groupData.map((group: any) => (group.key === updatedGroup.key ? updatedGroup : group)))
    }

    const handleDoubleRowClick = (record: any) => {
        setSelectedGroup({ ...record })
        showEditGroupModal()
    }

    const columns = [
        {
            title: t('groups:table.name'),
            dataIndex: 'groupName',
            key: 'name',
            render: (text: string) => (
                <span>
                    {text}
                </span>
            ),
        },
        {
            title: t('groups:table.description'),
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: t('groups:table.roles'),
            key: 'roles',
            render: (data: { roles: string[], privileges: string[] }) => (
                <div>
                    {data.roles
                        && data.roles.length > 0
                        && data.roles.map((role: string) => {
                            let color = 'default'
                            if ([ 'Administrators' ].includes(role)) {
                                color = 'red'
                            } else if ([ 'Developers', 'Testers', 'Viewers', 'Deployers', 'Analysts' ].includes(role)) {
                                color = 'blue'
                            }
                            return (
                                <Tag key={role} color={color} style={{ margin: 2 }}>
                                    {role}
                                </Tag>
                            )
                        })}
                    {data.privileges
                        && data.privileges.length > 0
                        && data.privileges.map((privilege: string) => {
                            let color = ''
                            if ([ 'ADMIN', 'Administrate' ].includes(privilege)) {
                                color = 'red'
                            } else {
                                color = 'default'
                            }
                            return (
                                <Tag key={privilege} color={color} style={{ margin: 2 }}>
                                    {privilege}
                                </Tag>
                            )
                        })}
                </div>
            ),
        },
        {
            title: t('groups:table.actions'),
            dataIndex: 'Action',
            key: 'Action',
            render: (text: string, record: any) => (
                <Button
                    icon={<CloseCircleOutlined />}
                    onClick={() => removeGroup(record.id)}
                    type="text"
                />
            ),
        },
    ]

    return (
        <>
            <Form>
                <Form.Item label={t('groups:default_group_for_all_users')}>
                    <Row>
                        <Col flex="auto">
                            <Input />
                        </Col>
                        <Col offset={1}>
                            <Button style={{ color: 'green', borderColor: 'green' }}>{t('groups:apply')}</Button>
                        </Col>
                    </Row>
                </Form.Item>
            </Form>
            <Divider />
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
                width={1000}
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

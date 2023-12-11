import React, { useEffect, useState } from 'react'
import { Button, Card, Table, Tag, Form, Input, Divider, Row, Col, Modal } from 'antd'
import { CloseCircleOutlined } from '@ant-design/icons'
import { NewGroupModal } from 'containers/groups/NewGroupModal'
import { EditGroupModal } from 'containers/groups/EditGroupModal'
import DefaultLayout from 'layouts/DefaultLayout'
import { apiCall } from 'services'
import './GroupPage.scss'

interface Group {
  name: string;
  id: number;
  description: string;
  roles: string[];
  privileges: string[];
}

export const GroupPage: React.FC = () => {
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
        try {
            const response = await apiCall('/admin/management/groups')
            if (response.ok) {
                const responseObject = await response.json()
                const groups = Object.entries(responseObject).map(([ groupName, group ]: [string, unknown]) => ({
                    groupName,
                    ...(group as Group),
                    privileges: (group as Group).privileges || [],
                }))
                setGroupData(groups)
            } else {
                console.error('Failed to fetch groups:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching groups:', error)
        }
    }

    useEffect(() => {
        fetchGroups()
    }, [])

    const removeGroup = (id: number) => {
        Modal.confirm({
            className: 'confirm-group-modal',
            title: 'Confirm Deletion',
            content: 'Are you sure you want to delete this group?',
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
            title: 'Name',
            dataIndex: 'groupName',
            key: 'name',
            render: (text: string) => (
                <span>
                    {text}
                </span>
            ),
        },
        {
            title: 'Description',
            dataIndex: 'description',
            key: 'description',
        },
        {
            title: 'Roles',
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
            title: 'Action',
            dataIndex: 'Action',
            key: 'Action',
            render: (text: string, record: any) => (
                <Button
                    icon={<CloseCircleOutlined />}
                    type="text"
                    onClick={() => removeGroup(record.id)}
                />
            ),
        },
    ]

    return (
        <DefaultLayout>
            <Card style={{ margin: 20, width: 900 }}>
                <Form>
                    <Form.Item label={(<span>Default group for all users * &nbsp;</span>)}>
                        <Row>
                            <Col flex="auto">
                                <Input />
                            </Col>
                            <Col offset={1}>
                                <Button style={{ color: 'green', borderColor: 'green' }}>Apply and Restart</Button>
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
                    className="edit-group-modal"
                    footer={null}
                    open={isModalOpen}
                    onCancel={hideEditGroupModal}
                >
                    {isModalOpen && (
                        <EditGroupModal
                            group={selectedGroup}
                            updateGroup={updateGroup}
                            onSave={hideEditGroupModal}
                        />
                    )}
                </Modal>
            </Card>
        </DefaultLayout>
    )
}

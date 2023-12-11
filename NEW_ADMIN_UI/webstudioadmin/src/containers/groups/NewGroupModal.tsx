import React, { useEffect, useState, useMemo } from 'react'
import { Button, Form, Input, Modal, Table } from 'antd'
import type { CheckboxValueType } from 'antd/es/checkbox/Group'
import { stringify } from 'querystring'
import { CheckOutlined } from '@ant-design/icons'
import { apiCall } from '../../services'

type Group = {
  name: string;
  id: number;
  description: string;
  roles: string[];
  privileges: string[];
};

interface Privilege {
  [key: string]: string;
}

export const NewGroupModal: React.FC<{ fetchGroups: () => void }> = ({ fetchGroups }) => {
    const [ isModalOpen, setIsModalOpen ] = useState(false)
    const [ name, setName ] = useState('')
    const [ description, setDescription ] = useState('')
    const [ privileges, setPrivileges ] = useState<CheckboxValueType[]>([])
    const [ selectedColumns, setSelectedColumns ] = useState<string[]>([])
    const [ allPrivileges, setAllPrivileges ] = useState<Privilege>({})
    const [ groupData, setGroupData ] = useState<Group[]>([])

    const showModal = () => {
        setIsModalOpen(true)
        setPrivileges([])
    }

    const hideModal = () => {
        setIsModalOpen(false)
    }

    const fetchGroupData = async () => {
        try {
            const response = await apiCall('/admin/management/groups')
            if (response.ok) {
                const responseObject = await response.json()
                setGroupData(responseObject)
            } else {
                console.error('Failed to fetch groups:', response.statusText)
            }
        } catch (error) {
            console.error('Error fetching groups:', error)
        }
    }

    useEffect(() => {
        fetchGroupData()
    }, [])

    useEffect(() => {
        const fetchPrivileges = async () => {
            try {
                const response = await apiCall('/admin/management/privileges', {
                    headers: {
                        'Content-Type': 'application/json'
                    },
                })
                if (response.ok) {
                    const jsonResponse = await response.json()
                    setAllPrivileges(jsonResponse)
                } else {
                    console.error('Failed to fetch privileges:', response.statusText)
                }
            } catch (error) {
                console.error('Error fetching privileges:', error)
            }
        }

        fetchPrivileges()
    }, [])

    const createGroup = async () => {
        try {
            const requestBody = {
                name,
                description,
                privilege: privileges.map((privilege) => privilege.toString()),
                selectedColumns,
            }

            const encodedBody = stringify(requestBody)

            await apiCall('/admin/management/groups', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: encodedBody,

            }).then(applyResult)
            setName('')
            setDescription('')
            setPrivileges([])
            setSelectedColumns([])
            setIsModalOpen(false)
            fetchGroups()
        } catch (error) {
            console.error('Error creating group:', error)
        }
    }

    const applyResult = (result: any) => {
        hideModal()
    }

    const handleSubmit = (e: React.SyntheticEvent) => {
        e.preventDefault()
        createGroup()
    }

    const dataSource = useMemo(() => {
        if (!allPrivileges || !groupData) {
            return []
        }

        return Object.keys(allPrivileges).map((key) => ({
            key,
            title: allPrivileges[key],
            ...groupData,
        }))
    }, [ allPrivileges, groupData ])

    const columns = useMemo(() => {
        if (!allPrivileges || !groupData) {
            return []
        }

        return [
            {
                title: 'Privilege',
                dataIndex: 'title',
                key: 'key',
            },
            ...Object.keys(groupData || {}).map((groupKey) => ({
                title: (
                    <Button type="link">
                        {groupKey}
                    </Button>
                ),
                key: groupKey,
                render: (value: any) => {
                    const privilege = value.key
                    const group = value[groupKey]
                    const hasPrivilege = group.privileges?.includes(privilege)
                    return hasPrivilege ? <CheckOutlined /> : null
                },
            })),
        ]
    }, [ allPrivileges, groupData ])

    return (
        <div>
            <Button onClick={showModal} style={{ marginTop: 15, color: 'green', borderColor: 'green' }}>
                Add new group
            </Button>
            <Modal
                className="new-group-modal"
                onCancel={hideModal}
                open={isModalOpen}
                title="Create new group"
                width={850}
                footer={[
                    <Button key="back" onClick={hideModal}>
                        Cancel
                    </Button>,
                    <Button key="submit" onClick={handleSubmit} style={{ marginTop: 15, color: 'green', borderColor: 'green' }}>
                        Create
                    </Button> ]}
            >
                <div>
                    <Form layout="vertical" style={{ width: 800 }}>
                        <Form.Item>
                            <b>Account</b>
                        </Form.Item>
                        <Form.Item label="Name">
                            <Input id="name" onChange={(e) => setName(e.target.value)} value={name} />
                        </Form.Item>
                        <Form.Item label="Description">
                            <Input id="description" onChange={(e) => setDescription(e.target.value)} value={description} />
                        </Form.Item>
                        <Form.Item>
                            <b>Group</b>
                        </Form.Item>
                        <Form.Item className="group-create-form_last-form-item">
                            <Form.Item>
                                <Table
                                    // rowSelection={rowSelection}
                                    columns={columns}
                                    dataSource={dataSource}
                                    pagination={false}
                                    scroll={{ x: 'max-content' }}
                                />
                            </Form.Item>
                        </Form.Item>
                    </Form>
                </div>
            </Modal>
        </div>
    )
}

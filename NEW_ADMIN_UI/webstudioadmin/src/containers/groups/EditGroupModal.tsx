import React, { useEffect, useState } from 'react'
import { Button, Checkbox, Divider, Form, Input, Row, Tabs, Typography } from 'antd'
import { stringify } from 'querystring'
import { apiCall } from '../../services'
import { Group } from '../Groups'
import { DesignRepositoriesTab } from './DesignRepositoriesTab'
import { DeployRepositoriesTab } from './DeployRepositoriesTab'
import { Role } from '../../constants'

interface EditGroupProps {
    group: Group;
    updateGroup: (updatedGroup: any) => void;
    onSave: () => void;
}


export const EditGroupModal: React.FC<EditGroupProps> = ({ group, updateGroup, onSave }) => {
    const [name, setName] = useState(group.name)
    const [description, setDescription] = useState(group.description)
    const [admin, setAdmin] = useState(group.admin)

    console.log(group)

    const handleNameInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setName(e.target.value)
    }

    const handleDescriptionInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setDescription(e.target.value)
    }

    const handleSave = async () => {
        const updatedGroup = {
            ...group,
            name,
            oldName: name,
            description,
            admin
        }
        const encodedBody = stringify(updatedGroup)

        try {
            const headers = new Headers()
            headers.append('Content-Type', 'application/x-www-form-urlencoded')
            headers.append('Accept', 'application/json')

            const response = await apiCall('/admin/management/groups', {
                method: 'POST',
                headers,
                body: encodedBody,
            })
            console.log('response', response)
            if (response) {
                updateGroup(updatedGroup)
                onSave()
            } else {
                throw new Error('Error updating group')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const formItemLayout = {
        labelCol: {
            sm: { span: 6 },
        },
    };

    const tabs = [
        {
            label: 'Design Repositories',
            key: 'design_repositories',
            children: <DesignRepositoriesTab />
        },
        {
            label: 'Deploy Repositories',
            key: 'deploy_repositories',
            children: <DeployRepositoriesTab />
        }
    ]

    return (
        <div>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: '1.5em'}}>Invite Group</Typography.Title>
            <Divider orientation="left">Details</Divider>
            <Form {...formItemLayout}>
                <Form.Item label="Name" required>
                    <Input id="name" onChange={handleNameInputChange} value={name}/>
                </Form.Item>
                <Form.Item label="Description">
                    <Input id="description" onChange={handleDescriptionInputChange} value={description}/>
                </Form.Item>
                <Form.Item label={'Admin'}>
                    <Checkbox name={'admin'} onChange={(e) => setAdmin(e.target.checked)} checked={admin}/>
                </Form.Item>
                <Form.Item>
                    <Row style={{ float: 'right' }}>
                        <Button
                            type="primary"
                            onClick={handleSave}
                        >
                            Save
                        </Button>
                    </Row>
                </Form.Item>
            </Form>
            <Divider orientation="left">Access Management</Divider>
            <Tabs items={tabs} />
        </div>
    )
}

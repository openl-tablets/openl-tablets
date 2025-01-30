import React, { useEffect } from 'react'
import { apiCall } from '../../services'
import { Button, Form, Input, Select, Space } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { Role } from '../../constants'

interface DesignRepository {
    id: number
    name: string
}

interface SelectOption {
    label: string,
    value: string
}

const roleOptions = Object.keys(Role).map(role => ({ label: role, value: role }))

export const DesignRepositoriesTab: React.FC = () => {
    const [designRepositories, setDesignRepositories] = React.useState<SelectOption[]>([])
    const fetchDesignRepositories = async () => {
        const response: DesignRepository[] = await apiCall('/repos')
        setDesignRepositories(response.map(repo => ({ label: repo.name, value: repo.id.toString() })))
    }
    useEffect(() => {
        fetchDesignRepositories()
    }, [])

    const onFinish = (values: any) => {
        console.log('Received values of form:', values);
    };
    return (
        <Form
            name="dynamic_form_nest_item"
            onFinish={onFinish}
            style={{ maxWidth: 600 }}
            autoComplete="off"
        >
            <Form.List name="users">
                {(fields, { add, remove }) => (
                    <>
                        {fields.map(({ key, name, ...restField }) => (
                            <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                                <Form.Item
                                    {...restField}
                                    name={[name, 'first']}
                                    rules={[{ required: true, message: 'Missing first name' }]}
                                >
                                    <Select placeholder="Design Repository" options={designRepositories} style={{ width: 200 }} />
                                </Form.Item>
                                <Form.Item
                                    {...restField}
                                    name={[name, 'last']}
                                    rules={[{ required: true, message: 'Missing last name' }]}
                                >
                                    <Select placeholder="Role" options={roleOptions} style={{ width: 200 }} />
                                </Form.Item>
                                <MinusCircleOutlined onClick={() => remove(name)} />
                            </Space>
                        ))}
                        <Form.Item>
                            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                                Add field
                            </Button>
                        </Form.Item>
                    </>
                )}
            </Form.List>
            <Form.Item>
                <Button type="primary" htmlType="submit">
                    Submit
                </Button>
            </Form.Item>
        </Form>
    )
}
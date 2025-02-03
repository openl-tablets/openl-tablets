import React, { useEffect, useMemo } from 'react'
import { apiCall } from '../../services'
import { Button, Form, Select, Space } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { roleOptions } from './utils'
import { Option } from '../../components/form/Select'

export const DeployRepositoriesTab: React.FC<{selectedRepositories: string[]}> = ({ selectedRepositories }) => {
    const [deployRepositories, setDeployRepositories] = React.useState<Option[]>([])

    const fetchDeployRepositories = async () => {
        const response: DesignRepository[] = await apiCall('/production-repos')
        setDeployRepositories(response.map(repo => ({ label: repo.name, value: repo.aclId })))
    }
    useEffect(() => {
        fetchDeployRepositories()
    }, [])

    const repositoryOptions = useMemo(() => {
        return deployRepositories.map(repository => ({
            label: repository.label,
            value: repository.value,
            disabled: selectedRepositories.includes(repository.value)
        }))
    }, [deployRepositories, selectedRepositories])

    return (
        <Form.List name="deployRepos">
            {(fields, { add, remove }) => (
                <>
                    {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                            <Form.Item
                                {...restField}
                                name={[name, 'id']}
                                rules={[{ required: true, message: 'Select Repository' }]}
                            >
                                <Select
                                    showSearch
                                    options={repositoryOptions}
                                    placeholder="Deploy Repository"
                                    style={{ width: 250 }}
                                    filterOption={(input, option) => {
                                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                                    }}
                                    filterSort={(optionA, optionB) => {
                                        return optionA.disabled === optionB.disabled ? 0 : optionA.disabled ? 1 : -1
                                    }}
                                />
                            </Form.Item>
                            <Form.Item
                                {...restField}
                                name={[name, 'role']}
                                rules={[{ required: true, message: 'Select Role' }]}
                            >
                                <Select options={roleOptions} placeholder="Role" style={{ width: 250 }} />
                            </Form.Item>
                            <MinusCircleOutlined onClick={() => remove(name)} />
                        </Space>
                    ))}
                    <Form.Item>
                        <Button block icon={<PlusOutlined />} onClick={() => add()} type="dashed">
                            Add Role
                        </Button>
                    </Form.Item>
                </>
            )}
        </Form.List>
    )
}
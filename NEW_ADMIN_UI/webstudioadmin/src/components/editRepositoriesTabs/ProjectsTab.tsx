import React, { useEffect, useMemo } from 'react'
import { apiCall } from '../../services'
import { Button, Form, Select, Space } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { roleOptions } from './utils'
import {Project} from "../../types/projects";
import { SelectOption } from '../form/Select'

export const ProjectsTab: React.FC<{selectedProjects: string[]}> = ({ selectedProjects }) => {
    const [projects, setProjects] = React.useState<SelectOption[]>([])

    const fetchProjects = async () => {
        const response: Project[] = await apiCall('/projects')
        setProjects(response.map(project => ({ label: project.name, value: project.id })))
    }
    useEffect(() => {
        fetchProjects()
    }, [])

    const projectsOptions = useMemo(() => {
        return projects.map(project => ({
            label: project.label,
            value: project.value,
            disabled: selectedProjects.includes(project.value as string)
        }))
    }, [projects, selectedProjects])

    return (
        <Form.List name="projects">
            {(fields, { add, remove }) => (
                <>
                    {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                            <Form.Item
                                {...restField}
                                name={[name, 'id']}
                                rules={[{ required: true, message: 'Select Project' }]}
                            >
                                <Select
                                    showSearch
                                    options={projectsOptions}
                                    placeholder="Project"
                                    style={{ width: 250 }}
                                    filterOption={(input, option) => {
                                        if (!option || !option.label || !(typeof option.label === 'string')) {
                                            return false
                                        }
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
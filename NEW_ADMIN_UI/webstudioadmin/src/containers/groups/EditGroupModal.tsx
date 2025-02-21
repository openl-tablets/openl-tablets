import React, { useEffect, useMemo, useState } from 'react'
import { Button, Form, Row, Tabs, Typography } from 'antd'
import { stringify } from 'querystring'
import { apiCall } from '../../services'
import { GroupTableItem } from '../../types/group'
import { DeployRepositoriesTab, DesignRepositoriesTab, ProjectsTab } from '../../components/editRepositoriesTabs'
import { RepositoryType, Role } from '../../constants'
import { RepositoryRole } from '../../types/repositories'
import { ProjectRole } from '../../types/projects'
import { Checkbox, Input, Select } from '../../components'
import {useTranslation} from "react-i18next";

interface EditGroupProps {
    group: GroupTableItem | undefined
    updateGroup: (updatedGroup: any) => void
    onAddGroup: () => void
    closeModal: () => void
}

interface Project {
    id: string
    name: string
    role: Role
}

interface FormValues {
    name: string
    description: string
    admin: boolean
    designRepos: RepositoryRole[]
    deployRepos: RepositoryRole[]
    projects: ProjectRole[]
}

export const EditGroupModal: React.FC<EditGroupProps> = ({ group, updateGroup, onAddGroup, closeModal }) => {
    const {t} = useTranslation()
    const [isNewGroup, setIsNewGroup] = useState(!group?.id)
    const [designRepos, setDesignRepos] = useState<RepositoryRole[]>([])
    const [deployRepos, setDeployRepos] = useState<RepositoryRole[]>([])
    const [projects, setProjects] = useState<Project[]>([])
    const [isReposLoaded, setIsReposLoaded] = useState(false)
    const [isProjectsLoaded, setIsProjectsLoaded] = useState(false)
    const [selectedRepositories, setSelectedRepositories] = useState<string[]>([])
    const [selectedProjects, setSelectedProjects] = useState<string[]>([])
    const [searchString, setSearchString] = useState('')
    const [externalGroups, setExternalGroups] = useState<string[]>([])

    const [form] = Form.useForm()

    const initialValues = useMemo(() => ({
        name: group?.name,
        description: group?.description,
        admin: group?.admin,
        designRepos,
        deployRepos,
        projects
    }), [group, designRepos, deployRepos, projects])

    const fetchReposRoles = async () => {
        if (!isNewGroup && group?.name) {
            const response: RepositoryRole[] = await apiCall(`/acls/repositories?sid=${group.name}`)
            setDesignRepos(response.filter(repo => repo.type === RepositoryType.DESIGN))
            setDeployRepos(response.filter(repo => repo.type === RepositoryType.PROD))
            setSelectedRepositories(response.map(repo => repo.id))
        }
        setIsReposLoaded(true)
    }

    const fetchProjectRoles = async () => {
        if (!isNewGroup && group?.name) {
            const response: Project[] = await apiCall(`/acls/projects?sid=${group.name}`)
            setProjects(response)
            setSelectedProjects(response.map(project => project.id))
        }
        setIsProjectsLoaded(true)
    }

    const fetchExternalGroups = async () => {
        const response: string[] = await apiCall(`/admin/management/groups/external?search=${searchString}`)
        setExternalGroups(response)
    }

    useEffect(() => {
        if (searchString) {
            fetchExternalGroups()
        }
    }, [searchString])

    useEffect(() => {
        fetchReposRoles()
        fetchProjectRoles()
    }, [group?.name])

    const saveGroup = async (values: FormValues) => {
        const updatedGroup = {
            ...group,
            name: values.name,
            description: values.description,
            admin: !!values.admin
        }

        if (!isNewGroup) {
            updatedGroup.oldName = group?.oldName || updatedGroup.name
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
            if (response) {
                updatedGroup.oldName = updatedGroup.name
                updateGroup(updatedGroup)
                if (isNewGroup) {
                    setIsNewGroup(false)
                    onAddGroup()
                } else {
                    closeModal()
                }
            } else {
                throw new Error('Error updating group')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const saveReposRoles = async (groupName: string, repositories: RepositoryRole[], repoType: RepositoryType) => {
        const initialRoles = repoType === RepositoryType.DESIGN ? designRepos : deployRepos

        const addedRoles = repositories.filter((role: any) => !initialRoles.find((r: any) => r.id === role.id))
        const deletedRoles = initialRoles.filter((role: any) => !repositories.find((r: any) => r.id === role.id))
        const updatedRoles = repositories.filter((role: any) => initialRoles.find((r: any) => r.id === role.id && r.role !== role.role))

        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        deletedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}`, {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: role.role })
            })
        })

        updatedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: role.role })
            })
        })
    }

    const saveDesignRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.name, values.designRepos, RepositoryType.DESIGN)
    }

    const saveDeployRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.name, values.deployRepos, RepositoryType.PROD)
    }

    const saveProjectRoles = async (values: FormValues) => {
        const projectRoles = values.projects.map((item: any) => ({
            id: item.id,
            name: item.name,
            role: item.role
        })
        )

        const addedRoles = projectRoles.filter((role: any) => !projects.find((r: any) => r.id === role.id))
        const deletedRoles = projects.filter((role: any) => !projectRoles.find((r: any) => r.id === role.id))
        const updatedRoles = projectRoles.filter((role: any) => projects.find((r: any) => r.id === role.id && r.role !== role.role))

        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        deletedRoles.forEach((project: any) => {
            apiCall(`/acls/projects/${project.id}?sid=${values.name}`, {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((project: any) => {
            apiCall(`/acls/projects/${project.id}?sid=${values.name}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })

        updatedRoles.forEach((project: any) => {
            apiCall(`/acls/projects/${project.id}?sid=${values.name}`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })
    }

    const accessTabs = [
        {
            label: t('groups:design_repositories'),
            key: 'design_repositories',
            forceRender: true,
            children: <DesignRepositoriesTab selectedRepositories={selectedRepositories} />
        },
        {
            label: t('groups:deploy_repositories'),
            key: 'deploy_repositories',
            forceRender: true,
            children: <DeployRepositoriesTab selectedRepositories={selectedRepositories} />
        },
        {
            label: t('groups:projects'),
            key: 'projects',
            forceRender: true,
            children: <ProjectsTab selectedProjects={selectedProjects} />
        }
    ]

    const handleSearch = (newValue: string) => {
        setSearchString(newValue)
    }

    const handleChange = (newValue: string) => {
        if (newValue) {
            setSearchString('')
            form.setFieldsValue({ name: newValue })
        }
    }

    const onBlurNameField = () => {
        if (searchString) {
            form.setFieldsValue({ name: searchString })
        }
    }

    const groupOptions = useMemo(() => {
        return externalGroups.map(group => ({
            value: group,
            label: group,
        }))
    }, [externalGroups])

    const groupTabs = [
        {
            label: t('groups:details'),
            key: 'details',
            forceRender: true,
            children: (
                <>
                    <Select
                        showSearch
                        defaultActiveFirstOption={false}
                        filterOption={false}
                        label={t('groups:name')}
                        name="name"
                        notFoundContent={null}
                        onBlur={onBlurNameField}
                        onChange={handleChange}
                        onSearch={handleSearch}
                        options={groupOptions}
                        style={{ width: '100%' }}
                        suffixIcon={null}
                        rules={[
                            { required: true, message: t('groups:name_required') },
                            { max: 65, message: t('groups:group_name_max_length') }
                        ]}
                    />
                    <Input
                        label={t('groups:description')}
                        name="description"
                        rules={[{ max: 200, message: t('groups:description_max_length') }]}
                    />
                    <Checkbox label={t('groups:admin')} name="admin" valuePropName="checked" />
                </>
            )
        },
        {
            label: t('groups:access_management'),
            key: 'access_management',
            disabled: isNewGroup,
            forceRender: true,
            children: (
                <Tabs items={accessTabs} />
            )
        }
    ]

    const onFinish = async (values: FormValues) => {
        await saveGroup(values)
        if (!isNewGroup) {
            saveDesignRepositoriesRoles(values)
            saveDeployRepositoriesRoles(values)
            saveProjectRoles(values)
        }
    }

    const title = useMemo(() => {
        return isNewGroup ? t('groups:invite_group') : t('groups:edit_group')
    }, [isNewGroup])

    if (!isReposLoaded || !isProjectsLoaded) {
        return <Typography.Text>{t('common:loading')}</Typography.Text>
    }

    const onValuesChange = (_: any, allValues: FormValues) => {
        setSelectedRepositories(allValues.designRepos.map(repo => repo.id).concat(allValues.deployRepos.map(repo => repo.id)))
        setSelectedProjects(allValues.projects.map(project => project ? project.id || '' : ''))
    }

    return (
        <Form
            form={form}
            initialValues={initialValues}
            labelCol={{ sm: { span: 6 } }}
            onFinish={onFinish}
            onValuesChange={onValuesChange}

        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>{title}</Typography.Title>
            <Tabs items={groupTabs} />
            <Row justify="end">
                <Button key="back" onClick={closeModal} style={{ marginRight: 20 }}>
                    {t('groups:cancel')}
                </Button>
                <Button
                    htmlType="submit"
                    type="primary"
                >
                    {isNewGroup ? t('groups:invite') : t('groups:invite')}
                </Button>
            </Row>
        </Form>
    )
}

import React, { useMemo, useEffect, useState } from 'react'
import { Button, Row, Form, Typography, Tabs } from 'antd'
import { useTranslation } from 'react-i18next'
import { DisplayUserName, RepositoryType } from 'constants/'
import { UserDetailsTab } from './UserDatailsTab'
import { DeployRepositoriesTab, DesignRepositoriesTab, ProjectsTab } from '../../components/editRepositoriesTabs'
import { apiCall } from '../../services'
import { UserProfile } from '../../types/user'

interface EditUserProps {
    user: UserProfile
    updateUser: any // TODO add type
    closeModal: () => void;
    onAddUser: () => void;
}

export const EditUserModal: React.FC<EditUserProps> = ({ updateUser, user, onAddUser, closeModal }) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [isNewUser, setIsNewUser] = useState(!user.username)
    const [userGroups, setUserGroups] = React.useState<string[]>([])
    const [designRepos, setDesignRepos] = useState<Repository[]>([])
    const [deployRepos, setDeployRepos] = useState<Repository[]>([])
    const [projects, setProjects] = useState<Project[]>([])
    const [selectedRepositories, setSelectedRepositories] = useState<string[]>([])
    const [selectedProjects, setSelectedProjects] = useState<string[]>([])
    const [isReposLoaded, setIsReposLoaded] = useState(false)
    const [isProjectsLoaded, setIsProjectsLoaded] = useState(false)

    const fetchReposRoles = async () => {
        if (!isNewUser) {
            const response: Repository[] = await apiCall(`/acls/repositories?sid=${user.username}&principal=true`)
            setDesignRepos(response.filter(repo => repo.type === RepositoryType.DESIGN))
            setDeployRepos(response.filter(repo => repo.type === RepositoryType.PROD))
            setSelectedRepositories(response.map(repo => repo.id))
        }
        setIsReposLoaded(true)
    }

    const fetchProjectRoles = async () => {
        if (!isNewUser) {
            const response: Project[] = await apiCall(`/acls/projects?sid=${user.username}&principal=true`)
            setProjects(response)
            setSelectedProjects(response.map(project => project.id))
        }
        setIsProjectsLoaded(true)
    }

    useEffect(() => {
        fetchReposRoles()
        fetchProjectRoles()
    }, [user.username])

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            const firstName = user.firstName || ''
            const lastName = user.lastName || ''
            if (user.displayName === `${firstName} ${lastName}`.trim()) {
                return DisplayUserName.FirstLast
            }
            if (user.displayName === `${lastName} ${firstName}`.trim()) {
                return DisplayUserName.LastFirst
            }
            return DisplayUserName.Other
        }

        setUserGroups(user.userGroups?.map((group) => group.name) || [])

        return {
            username: user.username,
            email: user.email,
            password: null,
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            displayName: user.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
            designRepos,
            deployRepos,
            projects
        }
    }, [user, designRepos, deployRepos, projects])

    const onSubmitUserModal = async (userData: any) => {
        const updatedUser: EditUserRequest = {
            email: userData.email,
            displayName: userData.displayName,
            firstName: userData.firstName,
            lastName: userData.lastName,
            password: userData.password,
            groups: userGroups,
        }

        if (isNewUser) {
            updatedUser.username = userData.username
            updatedUser.internalPassword = {
                password: userData.password
            }
        }

        try {
            const url = isNewUser ? '/users' : `/users/${userData.username}`

            const response = await apiCall(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updatedUser),
            })

            if (response) {
                updateUser({ username: userData.username, ...updatedUser })
                if (isNewUser) {
                    setIsNewUser(false)
                    onAddUser()
                } else {
                    closeModal()
                }
            } else {
                throw new Error('Error updating user')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const updateButtonText = useMemo(() => {
        return isNewUser ? t('common:btn.create') : t('common:btn.save')
    }, [isNewUser, t])

    const title = useMemo(() => {
        return isNewUser ? 'Add User' : 'Edit User'
    }, [isNewUser])

    const saveReposRoles = async (groupName: string, repositories, repoType: RepositoryType) => {
        const initialRoles = repoType === RepositoryType.DESIGN ? designRepos : deployRepos

        const addedRoles = repositories.filter((role: any) => !initialRoles.find((r: any) => r.id === role.id))
        const deletedRoles = initialRoles.filter((role: any) => !repositories.find((r: any) => r.id === role.id))
        const updatedRoles = repositories.filter((role: any) => initialRoles.find((r: any) => r.id === role.id && r.role !== role.role))

        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        deletedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}&principal=true`, {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}&principal=true`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: role.role })
            })
        })

        updatedRoles.forEach((role: any) => {
            apiCall(`/acls/repositories/${role.id}?sid=${groupName}&principal=true`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: role.role })
            })
        })
    }

    const saveDesignRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.username, values.designRepos, RepositoryType.DESIGN)
    }

    const saveDeployRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.username, values.deployRepos, RepositoryType.PROD)
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
            apiCall(`/acls/projects/${project.id}?sid=${values.username}&principal=true`, {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((project: any) => {
            apiCall(`/acls/projects/${project.id}?sid=${values.username}&principal=true`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })

        updatedRoles.forEach((project: any) => {
            apiCall(`/acls/projects/${project.id}?sid=${values.name}&principal=true`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })
    }

    const accessTabs = [
        {
            label: 'Design Repositories',
            key: 'design_repositories',
            forceRender: true,
            children: <DesignRepositoriesTab selectedRepositories={selectedRepositories} />
        },
        {
            label: 'Deploy Repositories',
            key: 'deploy_repositories',
            forceRender: true,
            children: <DeployRepositoriesTab selectedRepositories={selectedRepositories} />
        },
        {
            label: 'Projects',
            key: 'projects',
            forceRender: true,
            children: <ProjectsTab selectedProjects={selectedProjects} />
        }
    ]

    const userTabs = [
        {
            label: 'Details',
            key: 'details',
            forceRender: true,
            children: <UserDetailsTab isNewUser={isNewUser} />
        },
        {
            label: 'Access Management',
            key: 'access_management',
            forceRender: true,
            children: <Tabs items={accessTabs} />
        }
    ]

    const onFinish = async (values: FormValues) => {
        await onSubmitUserModal(values)
        if (!isNewUser) {
            saveDesignRepositoriesRoles(values)
            saveDeployRepositoriesRoles(values)
            saveProjectRoles(values)
        }
    }

    const onValuesChange = (_: any, allValues: FormValues) => {
        setSelectedRepositories(allValues.designRepos.map(repo => repo.id).concat(allValues.deployRepos.map(repo => repo.id)))
        setSelectedProjects(allValues.projects.map(project => project ? project.id || '' : ''))
    }

    if (!isReposLoaded || !isProjectsLoaded) {
        return <Typography.Text>Loading...</Typography.Text>
    }

    return (
        <Form
            form={form}
            initialValues={initialValues}
            labelCol={{ sm: { span: 8 } }}
            onFinish={onFinish}
            onValuesChange={onValuesChange}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>{title}</Typography.Title>
            <Tabs items={userTabs} />
            <Row justify="end">
                <Button key="back" onClick={closeModal} style={{ marginRight: 20 }}>
                    {t('users:edit_modal.cancel')}
                </Button>
                <Button
                    key="submit"
                    htmlType="submit"
                    type="primary"
                >
                    {updateButtonText}
                </Button>
            </Row>

        </Form>
    )
}

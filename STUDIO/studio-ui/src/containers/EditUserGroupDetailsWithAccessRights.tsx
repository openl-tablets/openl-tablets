import React, { useMemo, useEffect, useState, ReactNode } from 'react'
import { Form, Tabs, TabsProps, notification, Divider, Button, Drawer } from 'antd'
import { useTranslation } from 'react-i18next'
import { DisplayUserName, RepositoryType } from 'constants/'
import { DeployRepositoriesTab, DesignRepositoriesTab, ProjectsTab } from '../components/accessManagement'
import { apiCall } from '../services'
import { Repository, RepositoryRole } from '../types/repositories'
import { ProjectRole } from '../types/projects'
import { WarningOutlined } from '@ant-design/icons'
import { EditGroupDetails } from './groups/EditGroupDetails'
import { UserDetailsTab } from './users/UserDatailsTab'
import { UpdatedUserRequest } from './users/EditUserModal'
import { GroupItem } from '../types/group'
import { UserDetails } from '../types/user'
import { SelectedGroup } from './users/RenderGroupCell'

interface EditUserGroupDetailsWithAccessRightsProps {
    isOpenFromParent?: boolean
    sid?: string
    isPrincipal?: boolean
    onClose: () => void
    group?: GroupItem | SelectedGroup
    user?: UserDetails
    renderButton?: (f: () => void) => ReactNode
    reloadGroups?: () => Promise<void>
    reloadUsers?: () => Promise<void>
    newUser?: boolean
}

interface ReposFormValues {
    designRepos: RepositoryRole[]
    deployRepos: RepositoryRole[]
    projects: ProjectRole[]
}

interface GroupFormValues extends ReposFormValues {
    name: string
    description?: string
    admin?: boolean
    oldName?: string
}

interface UserFormValues extends ReposFormValues {
    username: string
    email?: string
    password?: string | null
    firstName?: string
    lastName?: string
    displayName?: string
    displayNameSelect?: DisplayUserName
}

type FormValues = GroupFormValues | UserFormValues

enum TabKeys {
    DESIGN_REPOSITORIES = 'designRepos',
    DEPLOY_REPOSITORIES = 'deployRepos',
    PROJECTS = 'projects'
}

export const EditUserGroupDetailsWithAccessRights: React.FC<EditUserGroupDetailsWithAccessRightsProps> = ({
    sid,
    isPrincipal,
    isOpenFromParent,
    group,
    user,
    renderButton,
    reloadGroups,
    reloadUsers,
    onClose,
    newUser
}) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [designRepos, setDesignRepos] = useState<RepositoryRole[]>([])
    const [deployRepos, setDeployRepos] = useState<RepositoryRole[]>([])
    const [projects, setProjects] = useState<ProjectRole[]>([])
    const [selectedRepositories, setSelectedRepositories] = useState<string[]>([])
    const [selectedProjects, setSelectedProjects] = useState<string[]>([])
    const [designRepositories, setDesignRepositories] = React.useState<Repository[]>([])
    const [loadingDesignRepositories, setLoadingDesignRepositories] = React.useState<boolean>(false)
    const [hasErrorOnTab, setHasErrorOnTab] = useState<Record<any, boolean>>({})

    const [isOpen, setIsOpen] = React.useState(false)

    const [userGroups, setUserGroups] = React.useState<string[]>([])

    const isUser = useMemo(() => {
        return newUser || (user && user.username)
    }, [user, newUser])

    const fetchReposRoles = async () => {
        const response: RepositoryRole[] = await apiCall(`/acls/repositories?sid=${sid}${isPrincipal ? '&principal=true' : ''}`)
        setDesignRepos(response.filter(repo => repo.type === RepositoryType.DESIGN))
        setDeployRepos(response.filter(repo => repo.type === RepositoryType.PROD))
        setSelectedRepositories(response.map(repo => repo.id))
    }

    const fetchProjectRoles = async () => {
        const response: ProjectRole[] = await apiCall(`/acls/projects?sid=${sid}${isPrincipal ? '&principal=true' : ''}`)
        setProjects(response)
        setSelectedProjects(response.map(project => project.id))
    }

    const fetchDesignRepositories = async () => {
        setLoadingDesignRepositories(true)
        const response: Repository[] = await apiCall('/repos').finally(() => setLoadingDesignRepositories(false))
        setDesignRepositories(response)
    }

    const isNewGroup = useMemo(() => {
        return !newUser && (!group || !group.id)
    }, [group])

    const isNewUser = useMemo(() => {
        return !user || !user.username
    }, [user])

    const handleOpenDrawer = () => {
        setIsOpen(true)
    }

    const handleCloseDrawer = () => {
        setIsOpen(false)
        onClose()
    }

    useEffect(() => {
        if ((isOpenFromParent || isOpen) && !designRepositories.length && !loadingDesignRepositories) {
            fetchDesignRepositories()
        }
    }, [isOpenFromParent, isOpen, designRepositories, loadingDesignRepositories])

    useEffect(() => {
        if (isOpen && (((user || newUser) && sid) || (group && group.id))) {
            fetchReposRoles()
            fetchProjectRoles()
        }
    }, [isOpen, group, sid])

    const getUserInitialValues = () => {
        const displayNameSelectInitialValue = () => {
            const firstName = user?.firstName || ''
            const lastName = user?.lastName || ''
            if (user?.displayName === `${firstName} ${lastName}`.trim()) {
                return DisplayUserName.FirstLast
            }
            if (user?.displayName === `${lastName} ${firstName}`.trim()) {
                return DisplayUserName.LastFirst
            }
            return DisplayUserName.Other
        }

        setUserGroups(user?.userGroups?.map((group) => group.name) || [])

        return {
            username: user?.username || '',
            email: user?.email,
            password: null,
            firstName: user?.firstName || '',
            lastName: user?.lastName || '',
            displayName: user?.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
        }
    }

    const initialValues = useMemo(() => {
        let initialValues: UserFormValues | GroupFormValues
        if (isUser) {
            initialValues = {
                ...getUserInitialValues(),
                designRepos,
                deployRepos,
                projects
            }
        } else {
            initialValues = {
                name: group?.name || '',
                description: group?.description,
                admin: group?.admin,
                designRepos,
                deployRepos,
                projects
            }
        }
        form.setFieldsValue(initialValues)
        return initialValues
    }, [isUser, newUser, user, group, designRepos, deployRepos, projects])

    const saveReposRoles = async (repositories: RepositoryRole[], repoType: RepositoryType, sid: string) => {
        const initialRoles = repoType === RepositoryType.DESIGN ? designRepos : deployRepos

        const addedRoles = repositories.filter((role: any) => !initialRoles.find((r: any) => r.id === role.id))
        const deletedRoles = initialRoles.filter((role: any) => !repositories.find((r: any) => r.id === role.id))
        const updatedRoles = repositories.filter((role: any) => initialRoles.find((r: any) => r.id === role.id && r.role !== role.role))

        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        const generateURL = (id: string) => {
            let url = `/acls/repositories/${id}?sid=${sid}`
            if (isPrincipal) {
                url += '&principal=true'
            }
            return url
        }

        deletedRoles.forEach((repo: any) => {
            apiCall(generateURL(repo.id), {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((repo: any) => {
            apiCall(generateURL(repo.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: repo.role })
            })
        })

        updatedRoles.forEach((repo: any) => {
            apiCall(generateURL(repo.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: repo.role })
            })
        })
    }

    const saveDesignRepositoriesRoles = async (values: FormValues, sid: string) => {
        return saveReposRoles(values.designRepos, RepositoryType.DESIGN, sid)
    }

    const saveDeployRepositoriesRoles = async (values: FormValues, sid: string) => {
        return saveReposRoles(values.deployRepos, RepositoryType.PROD, sid)
    }

    const saveProjectRoles = async (values: FormValues, sid: string) => {
        const projectRoles = values.projects.map((item: any) => ({
            id: item.id,
            name: item.name,
            role: item.role
        }))

        const addedRoles = projectRoles.filter((role: any) => !projects.find((r: any) => r.id === role.id))
        const deletedRoles = projects.filter((role: any) => !projectRoles.find((r: any) => r.id === role.id))
        const updatedRoles = projectRoles.filter((role: any) => projects.find((r: any) => r.id === role.id && r.role !== role.role))

        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        const generateURL = (id: string) => {
            let url = `/acls/projects/${id}?sid=${sid}`
            if (isPrincipal) {
                url += '&principal=true'
            }
            return url
        }

        deletedRoles.forEach((project: any) => {
            apiCall(generateURL(project.id), {
                method: 'DELETE',
                headers
            })
        })

        addedRoles.forEach((project: any) => {
            apiCall(generateURL(project.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })

        updatedRoles.forEach((project: any) => {
            apiCall(generateURL(project.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        })
    }

    const accessTabs = useMemo(() => [
        {
            label: t('users:design_repositories'),
            key: TabKeys.DESIGN_REPOSITORIES,
            forceRender: true,
            icon: hasErrorOnTab[TabKeys.DESIGN_REPOSITORIES] ? <WarningOutlined style={{ color: 'red' }} /> : null,
            children: <DesignRepositoriesTab designRepositories={designRepositories} selectedRepositories={selectedRepositories} />
        },
        {
            label: t('users:deploy_repositories'),
            key: TabKeys.DEPLOY_REPOSITORIES,
            forceRender: true,
            icon: hasErrorOnTab[TabKeys.DEPLOY_REPOSITORIES] ? <WarningOutlined style={{ color: 'red' }} /> : null,
            children: <DeployRepositoriesTab selectedRepositories={selectedRepositories} />
        },
        {
            label: t('users:projects'),
            key: TabKeys.PROJECTS,
            forceRender: true,
            icon: hasErrorOnTab[TabKeys.PROJECTS] ? <WarningOutlined style={{ color: 'red' }} /> : null,
            children: <ProjectsTab designRepositories={designRepositories} selectedProjects={selectedProjects} />
        }
    ], [hasErrorOnTab, selectedRepositories, selectedProjects, designRepositories, t])

    const onFinish = async (values: FormValues) => {
        let sid
        if (isUser) {
            sid = await saveUser(values)
        } else {
            // @ts-ignore
            sid = await saveGroup(values)
        }
        // Save roles for design and deploy repositories and projects
        saveDesignRepositoriesRoles(values, sid)
        saveDeployRepositoriesRoles(values, sid)
        saveProjectRoles(values, sid)
        // Reload users and groups if necessary
        if (reloadUsers) {
            reloadUsers()
        }
        if (reloadGroups) {
            reloadGroups()
        }

        handleCloseDrawer()
    }

    const onFinishFailed = (errorInfo: any) => {
        const tabsWithErrors = errorInfo.errorFields.reduce((acc: any, field: any) => {
            const tabKey = field.name[0] as TabKeys
            if (!acc[tabKey]) {
                acc[tabKey] = true
            }
            return acc
        }, {} as TabKeys[])
        setHasErrorOnTab(tabsWithErrors)
        notification.error({ message: t('common:please_fix_errors_on_highlighted_tabs_before_saving') })
    }

    const onValuesChange = (changedValues: any, allValues: FormValues) => {
        const selectedDesignRepos = allValues.designRepos.filter(repo => repo).map(repo => repo.id)
        const selectedDeployRepos = allValues.deployRepos.filter(repo => repo).map(repo => repo.id)

        setSelectedRepositories([...selectedDesignRepos, ...selectedDeployRepos])
        setSelectedProjects(allValues.projects.filter(project => project).map(project => project.id))

        if (Object.keys(hasErrorOnTab).length) {
            const changedTab = Object.keys(changedValues)[0] as TabKeys
            if (hasErrorOnTab[changedTab]) {
                setHasErrorOnTab(prev => ({ ...prev, [changedTab]: false }))
            }
        }
    }

    const renderTabBar: TabsProps['renderTabBar'] = (props, DefaultTabBar) => {
        return <DefaultTabBar {...props} />
    }

    const title = useMemo(() => {
        if (isUser) {
            return newUser ? t('users:add_user') : t('users:edit_user')
        }
        return isNewGroup ? t('groups:invite_group') : t('groups:edit_group')
    }, [isUser, newUser, isNewGroup, t])

    const saveUser = async (userData: any) => {
        const updatedUser: UpdatedUserRequest = {
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
                return userData.username
            } else {
                throw new Error('Error updating user')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const saveGroup = async (values: GroupFormValues) => {
        const updatedGroup = {
            ...group,
            name: values.name,
            description: values.description || '',
            admin: !!values.admin
        } as GroupFormValues

        if (!isNewGroup) {
            updatedGroup.oldName = group?.oldName || updatedGroup.name
        }

        // use URLSearchParams to encode the body
        // @ts-ignore
        const encodedBody = new URLSearchParams(updatedGroup).toString()

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
                return updatedGroup.name
            } else {
                throw new Error('Error updating group')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    return (
        <>
            {renderButton && renderButton(handleOpenDrawer)}
            <Drawer
                destroyOnHidden
                onClose={handleCloseDrawer}
                open={isOpenFromParent || isOpen}
                title={title}
                width={800}
                extra={
                    <>
                        <Button key="back" onClick={handleCloseDrawer} style={{ marginRight: 20 }}>
                            {t('groups:cancel')}
                        </Button>
                        <Button
                            onClick={form.submit}
                            type="primary"
                        >
                            {isNewGroup ? t('common:btn.invite') : t('common:btn.save')}
                        </Button>
                    </>
                }
            >
                <Form
                    form={form}
                    initialValues={initialValues}
                    labelCol={{ sm: { span: 8 } }}
                    onFinish={onFinish}
                    onFinishFailed={onFinishFailed}
                    onValuesChange={onValuesChange}
                >
                    {isUser ? (
                        <UserDetailsTab
                            externalFlags={user?.externalFlags}
                            isNewUser={!!newUser}
                            showResendVerification={true}
                            userProfile={user}
                        />
                    ) : (
                        <EditGroupDetails />
                    )}
                    <Divider orientation="left">{t('common:access_rights')}</Divider>
                    <Tabs items={accessTabs} renderTabBar={renderTabBar} />
                </Form>
            </Drawer>
        </>
    )
}

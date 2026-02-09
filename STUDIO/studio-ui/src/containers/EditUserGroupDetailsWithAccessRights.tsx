import React, { useMemo, useEffect, useState, ReactNode } from 'react'
import { Form, Tabs, TabsProps, notification, Divider, Button, Drawer } from 'antd'
import { useTranslation } from 'react-i18next'
import { DisplayUserName, RepositoryType, Role, ROOT_REPOSITORY_ID_MAP } from 'constants/'
import { DeployRepositoriesTab, DesignRepositoriesTab, ProjectsTab } from '../components/accessManagement'
import { NONE_ROLE_VALUE } from '../components/accessManagement/utils'
import { apiCall } from '../services'
import { Repository, RepositoryRole, RepositoryRootRole } from '../types/repositories'
import { ProjectRole } from '../types/projects'
import { WarningOutlined } from '@ant-design/icons'
import { EditGroupDetails } from './groups/EditGroupDetails'
import { UserDetailsTab } from './users/UserDatailsTab'
import { UpdatedUserRequest } from './users/EditUserModal'
import { GroupItem } from '../types/group'
import { UserDetails } from '../types/user'
import { SelectedGroup } from './users/RenderGroupCell'
import { runSequentialCollectErrors } from '../utils/async'
import { useUserStore } from '../store'

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
    designRootRole?: RootRoleFormValue
    deployRootRole?: RootRoleFormValue
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

type RootRoleFormValue = Role | typeof NONE_ROLE_VALUE

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
    const { userProfile, fetchUserProfile } = useUserStore()
    const [form] = Form.useForm()
    const [designRepos, setDesignRepos] = useState<RepositoryRole[]>([])
    const [deployRepos, setDeployRepos] = useState<RepositoryRole[]>([])
    const [projects, setProjects] = useState<ProjectRole[]>([])
    const [rootRepositoryRoles, setRootRepositoryRoles] = useState<Partial<Record<RepositoryType, RepositoryRootRole>>>({})
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

    const fetchRootRepositoryRoles = async () => {
        try {
            const response: RepositoryRootRole[] = await apiCall(`/acls/repositories/roots?sid=${sid}${isPrincipal ? '&principal=true' : ''}`)
            if (response) {
                const mappedRoles = response.reduce((acc, rootRole) => {
                    acc[rootRole.type as RepositoryType] = rootRole
                    return acc
                }, {} as Partial<Record<RepositoryType, RepositoryRootRole>>)
                setRootRepositoryRoles(mappedRoles)
            } else {
                setRootRepositoryRoles({})
            }
        } catch (error) {
            console.error('Failed to fetch root repository roles:', error)
            notification.error({
                message: t('common:error'),
                description: t('users:failed_to_load_root_repository_roles')
            })
            setRootRepositoryRoles({})
        }
    }

    const fetchDesignRepositories = async () => {
        setLoadingDesignRepositories(true)
        const response: Repository[] = await apiCall('/repos').finally(() => setLoadingDesignRepositories(false))
        setDesignRepositories(response)
    }

    const isNewGroup = useMemo(() => {
        return !user && !newUser && (!group || !group.id)
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
        if ((isOpenFromParent || isOpen) && (((user || newUser) && sid) || (group && group.id))) {
            fetchReposRoles()
            fetchProjectRoles()
            fetchRootRepositoryRoles()
        }
    }, [isOpenFromParent, isOpen, group, sid])

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
        const designRootRole = (rootRepositoryRoles[RepositoryType.DESIGN]?.role ?? NONE_ROLE_VALUE) as RootRoleFormValue
        const deployRootRole = (rootRepositoryRoles[RepositoryType.PROD]?.role ?? NONE_ROLE_VALUE) as RootRoleFormValue
        let initialValues: UserFormValues | GroupFormValues
        if (isUser) {
            initialValues = {
                ...getUserInitialValues(),
                designRepos,
                deployRepos,
                projects,
                designRootRole,
                deployRootRole
            }
        } else {
            initialValues = {
                name: group?.name || '',
                description: group?.description,
                admin: group?.admin,
                designRepos,
                deployRepos,
                projects,
                designRootRole,
                deployRootRole
            }
        }
        form.setFieldsValue(initialValues)
        return initialValues
    }, [isUser, newUser, user, group, designRepos, deployRepos, projects, rootRepositoryRoles])

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

        // Sequential execution with error accumulation: continue on failure so remaining operations
        // still run; then report all failures at once. See saveProjectRoles for the same pattern.
        const deleteFailures = await runSequentialCollectErrors(deletedRoles, (repo) =>
            apiCall(generateURL(repo.id), { method: 'DELETE', headers })
        )
        const addFailures = await runSequentialCollectErrors(addedRoles, (repo) =>
            apiCall(generateURL(repo.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: repo.role })
            })
        )
        const updateFailures = await runSequentialCollectErrors(updatedRoles, (repo) =>
            apiCall(generateURL(repo.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: repo.role })
            })
        )

        const totalFailures = deleteFailures.length + addFailures.length + updateFailures.length
        if (totalFailures > 0) {
            notification.error({
                message: t('common:error'),
                description: t('users:some_role_operations_failed', { count: totalFailures })
            })
            throw new Error(`Repository roles: ${totalFailures} operation(s) failed`)
        }
    }

    const saveDesignRepositoriesRoles = async (values: FormValues, sid: string) => {
        return saveReposRoles(values.designRepos, RepositoryType.DESIGN, sid)
    }

    const saveDeployRepositoriesRoles = async (values: FormValues, sid: string) => {
        return saveReposRoles(values.deployRepos, RepositoryType.PROD, sid)
    }

    const saveRootRepositoriesRoles = async (values: FormValues, sid: string) => {
        const headers = new Headers()
        headers.append('Content-Type', 'application/json')
        headers.append('Accept', 'application/json')

        const generateRootURL = (type: RepositoryType) => {
            const rootId = rootRepositoryRoles[type]?.id || ROOT_REPOSITORY_ID_MAP[type]
            let url = `/acls/repositories/roots/${rootId}?sid=${sid}`
            if (isPrincipal) {
                url += '&principal=true'
            }
            return url
        }

        const syncRootRole = async (type: RepositoryType, value?: RootRoleFormValue): Promise<void> => {
            const currentRole = rootRepositoryRoles[type]?.role
            const nextRole = value && value !== NONE_ROLE_VALUE ? value as Role : undefined

            if (!nextRole && currentRole) {
                await apiCall(generateRootURL(type), {
                    method: 'DELETE',
                    headers
                })
                return
            }

            if (nextRole && nextRole !== currentRole) {
                await apiCall(generateRootURL(type), {
                    method: 'PUT',
                    headers,
                    body: JSON.stringify({ role: nextRole })
                })
            }
        }

        // Sequential execution with error accumulation: run both, then report if any failed.
        const ops: Array<{ type: RepositoryType; value?: RootRoleFormValue }> = [
            { type: RepositoryType.DESIGN, value: values.designRootRole },
            { type: RepositoryType.PROD, value: values.deployRootRole }
        ]
        const failures: Array<{ type: RepositoryType; error: unknown }> = []
        for (const { type, value } of ops) {
            try {
                await syncRootRole(type, value)
            } catch (e) {
                failures.push({ type, error: e })
            }
        }
        if (failures.length > 0) {
            console.error('Failed to save root repository roles:', failures)
            notification.error({
                message: t('common:error'),
                description: t('users:failed_to_save_root_repository_roles')
            })
            throw new Error(`Root repository roles: ${failures.length} operation(s) failed`)
        }
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

        // Sequential execution with error accumulation; see saveReposRoles.
        const deleteFailures = await runSequentialCollectErrors(deletedRoles, (project) =>
            apiCall(generateURL(project.id), { method: 'DELETE', headers })
        )
        const addFailures = await runSequentialCollectErrors(addedRoles, (project) =>
            apiCall(generateURL(project.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        )
        const updateFailures = await runSequentialCollectErrors(updatedRoles, (project) =>
            apiCall(generateURL(project.id), {
                method: 'PUT',
                headers,
                body: JSON.stringify({ role: project.role })
            })
        )

        const totalFailures = deleteFailures.length + addFailures.length + updateFailures.length
        if (totalFailures > 0) {
            notification.error({
                message: t('common:error'),
                description: t('users:some_role_operations_failed', { count: totalFailures })
            })
            throw new Error(`Project roles: ${totalFailures} operation(s) failed`)
        }
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
        try {
            let sid
            if (isUser) {
                sid = await saveUser(values)
            } else {
                // @ts-ignore
                sid = await saveGroup(values)
            }

            if (!sid) {
                // Specific error (e.g. username already exists) was already shown by apiCall; avoid duplicate generic message
                return
            }

            // Save roles for design and deploy repositories and projects
            // It is mandatory to await each function to ensure that BE updates are done in order
            await saveDesignRepositoriesRoles(values, sid)
            await saveDeployRepositoriesRoles(values, sid)
            await saveRootRepositoriesRoles(values, sid)
            await saveProjectRoles(values, sid)

            // Reload users and groups if necessary
            if (reloadUsers) {
                await reloadUsers()
            }
            if (reloadGroups) {
                await reloadGroups()
            }

            // If the edited user is the current user, refresh profile so "My Profile" and header show updated data
            if (isUser && sid && sid === userProfile?.username) {
                await fetchUserProfile()
            }

            handleCloseDrawer()
        } catch (error) {
            console.error('Failed to save user/group details:', error)
            // Error notifications are handled in individual save functions
        }
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
            console.error('Error updating user:', error)
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
                            {(isNewGroup || newUser) ? t('common:btn.invite') : t('common:btn.save')}
                        </Button>
                    </>
                }
            >
                <Form
                    form={form}
                    initialValues={initialValues}
                    labelCol={{ sm: { span: 9 } }}
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
                    <Divider titlePlacement="start">{t('common:access_rights')}</Divider>
                    <Tabs items={accessTabs} renderTabBar={renderTabBar} />
                </Form>
            </Drawer>
        </>
    )
}

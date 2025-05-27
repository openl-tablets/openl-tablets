import React, { useMemo, useEffect, useState } from 'react'
import { Button, Row, Form, Typography, Tabs, Modal, TabsProps, notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { RepositoryType } from 'constants/'
import { DeployRepositoriesTab, DesignRepositoriesTab, ProjectsTab } from './'
import { apiCall } from '../../services'
import { Repository, RepositoryRole } from '../../types/repositories'
import { ProjectRole } from '../../types/projects'
import { WarningOutlined } from '@ant-design/icons'

interface AccessManagementModalProps {
    isOpen: boolean
    sid: string
    isPrincipal?: boolean
    onCloseModal: () => void;
}

interface FormValues {
    designRepos: RepositoryRole[]
    deployRepos: RepositoryRole[]
    projects: ProjectRole[]
}

enum TabKeys {
    DESIGN_REPOSITORIES = 'designRepos',
    DEPLOY_REPOSITORIES = 'deployRepos',
    PROJECTS = 'projects'
}

export const AccessManagementModal: React.FC<AccessManagementModalProps> = ({ isOpen, sid, isPrincipal, onCloseModal }) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [designRepos, setDesignRepos] = useState<RepositoryRole[]>([])
    const [deployRepos, setDeployRepos] = useState<RepositoryRole[]>([])
    const [projects, setProjects] = useState<ProjectRole[]>([])
    const [selectedRepositories, setSelectedRepositories] = useState<string[]>([])
    const [selectedProjects, setSelectedProjects] = useState<string[]>([])
    const [isReposLoaded, setIsReposLoaded] = useState(false)
    const [isProjectsLoaded, setIsProjectsLoaded] = useState(false)
    const [designRepositories, setDesignRepositories] = React.useState<Repository[]>([])
    const [hasErrorOnTab, setHasErrorOnTab] = useState<Record<any, boolean>>({})

    const fetchReposRoles = async () => {
        const response: RepositoryRole[] = await apiCall(`/acls/repositories?sid=${sid}${isPrincipal ? '&principal=true' : ''}`)
        setDesignRepos(response.filter(repo => repo.type === RepositoryType.DESIGN))
        setDeployRepos(response.filter(repo => repo.type === RepositoryType.PROD))
        setSelectedRepositories(response.map(repo => repo.id))
        setIsReposLoaded(true)
    }

    const fetchProjectRoles = async () => {
        const response: ProjectRole[] = await apiCall(`/acls/projects?sid=${sid}${isPrincipal ? '&principal=true' : ''}`)
        setProjects(response)
        setSelectedProjects(response.map(project => project.id))
        setIsProjectsLoaded(true)
    }

    const fetchDesignRepositories = async () => {
        const response: Repository[] = await apiCall('/repos')
        setDesignRepositories(response)
    }

    useEffect(() => {
        fetchDesignRepositories()
        fetchReposRoles()
        fetchProjectRoles()
    }, [sid])

    const initialValues = useMemo(() => {
        return {
            designRepos,
            deployRepos,
            projects
        }
    }, [designRepos, deployRepos, projects])

    const saveReposRoles = async (repositories: RepositoryRole[], repoType: RepositoryType) => {
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

    const saveDesignRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.designRepos, RepositoryType.DESIGN)
    }

    const saveDeployRepositoriesRoles = async (values: FormValues) => {
        saveReposRoles(values.deployRepos, RepositoryType.PROD)
    }

    const saveProjectRoles = async (values: FormValues) => {
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
        saveDesignRepositoriesRoles(values)
        saveDeployRepositoriesRoles(values)
        saveProjectRoles(values)
        onCloseModal()
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

    if (!isReposLoaded || !isProjectsLoaded) {
        return <Typography.Text>{t('common:loading')}</Typography.Text>
    }

    return (
        <Modal
            destroyOnClose
            footer={null}
            onCancel={onCloseModal}
            open={isOpen}
            width={600}
        >
            <Form
                form={form}
                initialValues={initialValues}
                labelCol={{ sm: { span: 8 } }}
                onFinish={onFinish}
                onFinishFailed={onFinishFailed}
                onValuesChange={onValuesChange}
            >
                <Typography.Title level={4} style={{ marginTop: 0 }}>{t('common:edit_access_rights')}</Typography.Title>
                <Tabs items={accessTabs} renderTabBar={renderTabBar} />
                <Row justify="end">
                    <Button key="back" onClick={onCloseModal} style={{ marginRight: 20 }}>
                        {t('common:btn.cancel')}
                    </Button>
                    <Button
                        key="submit"
                        htmlType="submit"
                        type="primary"
                    >
                        {t('common:btn.save')}
                    </Button>
                </Row>
            </Form>
        </Modal>
    )
}

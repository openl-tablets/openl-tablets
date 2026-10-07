import React, { useState, useEffect, useMemo } from 'react'
import { App, Alert, Modal, Form, Button, Space, Spin } from 'antd'
import { RocketOutlined, BranchesOutlined, LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useCommitInfoGuard, useGlobalEvents } from 'hooks'
import { Select, TextArea } from 'components/form'
import { apiCall, fieldErrorsOf, ForbiddenError, isApiHttpError } from 'services'
import { errorHandler } from 'utils/errorHandling'
import { Repository } from 'types/repositories'
import { WIDTH_OF_FORM_LABEL_MODAL } from 'constants/ui'

interface DeployModalDetail {
    branch: string
    /** Whether the project sits on the main branch of its design repository. */
    branchDefault?: boolean
    comment: string
    id: string
    modifiedAt: string
    modifiedBy: string
    name: string
    repository: string
    revision: string
    status: string
}

interface ProjectDeployedDetail {
    projectId?: string
}

interface DeployFormValues {
    repository: string
    deploymentName: string
    comment: string
}

/** The field of the form each field of the deploy request is typed in. */
const FORM_FIELD_OF = new Map<string, keyof DeployFormValues>([
    ['comment', 'comment'],
    ['deploymentName', 'deploymentName'],
    ['productionRepositoryId', 'repository'],
])

/** Whether the error is Ant Design's validation result (ValidateErrorEntity), which the form shows inline. */
const isValidationError = (error: unknown): boolean =>
    !!error && typeof error === 'object' && Array.isArray((error as { errorFields?: unknown }).errorFields)

/**
 * DeployModal component
 * @example to call this modal, dispatch a custom event 'openDeployModal' with details:
 * window.dispatchEvent(new CustomEvent('openDeployModal', {detail: {test:'test'}}))
 */
export const DeployModal: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const selectedRepository = Form.useWatch('repository', form)
    const { detail } = useGlobalEvents<DeployModalDetail>('openDeployModal')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [visible, setVisible] = useState(false)
    const [searchString, setSearchString] = useState('')
    const [deploymentRepositories, setDeploymentRepositories] = useState<Repository[]>([])
    const [deploymentNames, setDeploymentNames] = useState<Array<{ id: string, name: string }>>([])
    const [isNewDeployment, setIsNewDeployment] = useState<boolean>(false)
    const [isDeploying, setIsDeploying] = useState<boolean>(false)

    const fetchDeploymentRepositories = async () => {
        const response: Repository[] = await apiCall('/production-repos')
        setDeploymentRepositories(response)
    }

    const fetchDeploymentNames = async () => {
        try {
            const response: Array<{ id: string, name: string }> = await apiCall(
                `/deployments?repository=${encodeURIComponent(selectedRepository)}`,
                undefined,
                { throwError: true, suppressErrorPages: true }
            )
            setDeploymentNames(response)
            form.setFields([{ name: 'repository', errors: []}])
        } catch (error) {
            if (error instanceof ForbiddenError) {
                setDeploymentNames([])
                form.setFields([{
                    name: 'repository',
                    errors: [t('deploy:notifications.no_deploy_rights_short')],
                }])
            } else {
                setDeploymentNames([])
                notification.error({
                    title: t('deploy:notifications.deploy_failed'),
                    description: error instanceof Error ? error.message : t('deploy:notifications.deploy_failed_description'),
                    placement: 'topRight',
                })
            }
        }
    }

    useEffect(() => {
        if (visible && !deploymentRepositories.length) {
            void fetchDeploymentRepositories()
        }
    }, [visible, deploymentRepositories])

    useEffect(() => {
        if (selectedRepository) {
            void fetchDeploymentNames()
        } else {
            setDeploymentNames([])
        }
        // Clean deploy name on repository is changed
        form.setFieldsValue({ deploymentName: undefined })
        setIsNewDeployment(false)
        setSearchString('')
    }, [selectedRepository, form])

    useEffect(() => {
        const hasDetails = !!(detail && Object.keys(detail).length > 0)
        setVisible(hasDetails)
        if (hasDetails) {
            // Clear form when modal opens
            form.resetFields()
            // Reset states
            setSearchString('')
            setIsNewDeployment(false)
            setDeploymentNames([])
        }
    }, [detail, form])

    const handleClose = () => {
        setVisible(false)
        window.dispatchEvent(new CustomEvent('openDeployModal', { detail: null }))
    }

    /** Sends the deployment the form describes, and tells whether it was sent. */
    const sendDeployment = async (
        { repository, deploymentName, comment }: DeployFormValues,
        projectId: string | undefined
    ): Promise<boolean> => {
        const deployOptions = { throwError: true, suppressErrorPages: true }
        if (isNewDeployment) {
            // Create new deployment
            await apiCall('/deployments', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    comment,
                    deploymentName,
                    productionRepositoryId: repository,
                    projectId,
                }),
            }, deployOptions)
            return true
        }
        // Deploy to existing deployment
        const selectedDeployment = deploymentNames.find(dep => dep.name === deploymentName)
        if (!selectedDeployment) {
            notification.error({
                title: t('deploy:notifications.deploy_failed'),
                description: t('deploy:notifications.deploy_failed_description'),
                placement: 'topRight',
            })
            return false
        }
        await apiCall(`/deployments/${selectedDeployment.id}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                comment,
                projectId,
            }),
        }, deployOptions)
        return true
    }

    /**
     * Shows under the fields of the form what the server refused in them, such as a deployment name it does
     * not accept, and answers what it refused that no field of the form stands for.
     */
    const showRefusedFields = (refusals: ReturnType<typeof fieldErrorsOf>): string[] => {
        const refused = new Map<keyof DeployFormValues, string[]>()
        const elsewhere: string[] = []
        for (const { field, message } of refusals) {
            const name = field && FORM_FIELD_OF.get(field)
            if (name) {
                refused.set(name, [...(refused.get(name) ?? []), message])
            } else {
                elsewhere.push(message)
            }
        }
        form.setFields([...refused].map(([name, errors]) => ({ name, errors })))
        return elsewhere
    }

    /**
     * Tells why a deployment failed: under the fields the server refused, and in a toast what no field stands
     * for; on the repository field when it is not granted; in a toast otherwise.
     */
    const reportDeployFailure = (error: unknown) => {
        errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
        const refusals = fieldErrorsOf(error)
        if (refusals.length > 0) {
            const elsewhere = showRefusedFields(refusals)
            if (elsewhere.length > 0) {
                notification.error({
                    title: t('deploy:notifications.deploy_failed'),
                    description: elsewhere.join('\n'),
                    placement: 'topRight',
                })
            }
            return
        }
        if (error instanceof ForbiddenError) {
            form.setFields([{
                name: 'repository',
                errors: [t('deploy:notifications.no_deploy_rights_short')],
            }])
            notification.warning({
                title: t('deploy:notifications.deploy_failed'),
                description: t('deploy:notifications.no_deploy_rights'),
                placement: 'topRight',
            })
        } else {
            notification.error({
                title: t('deploy:notifications.deploy_failed'),
                // The server explains what it refused; only a failure without an explanation of its
                // own falls back to the generic sentence.
                description: isApiHttpError(error) && error.message
                    ? error.message
                    : t('deploy:notifications.deploy_failed_description'),
                placement: 'topRight',
            })
        }
    }

    const doDeploy = async (values: DeployFormValues) => {
        if (mainBranchOnlyBlocked) {
            notification.warning({
                title: t('deploy:notifications.deploy_failed'),
                description: t('deploy:notifications.main_branch_only', { branch: detail?.branch }),
                placement: 'topRight',
            })
            return
        }
        try {
            const projectId = detail?.id

            setIsDeploying(true)

            const didDeploy = await sendDeployment(values, projectId)
            if (didDeploy) {
                notification.success({
                    title: t('deploy:notifications.deploy_success'),
                    description: t('deploy:notifications.deploy_success_description', { projectName: detail?.name }),
                    placement: 'topRight',
                })
                if (projectId) {
                    window.dispatchEvent(new CustomEvent<ProjectDeployedDetail>('projectDeployed', {
                        detail: { projectId },
                    }))
                }
                handleClose()
            }
        } catch (error) {
            // Ant Design validation (ValidateErrorEntity) — form shows errors inline, no toast
            if (isValidationError(error)) {
                return
            }
            reportDeployFailure(error)
        } finally {
            setIsDeploying(false)
        }
    }

    const handleDeploy = async () => {
        try {
            const values = await form.validateFields()
            await runWithCommitInfo(() => doDeploy(values))
        } catch (error) {
            // Ant Design validation (ValidateErrorEntity) — form shows errors inline, no toast
            if (isValidationError(error)) {
                return
            }
            errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
        }
    }

    // What the server said about a name is about that name: editing it takes the refusal away.
    const forgetRefusedName = () => {
        if (form.getFieldError('deploymentName').length > 0) {
            form.setFields([{ name: 'deploymentName', errors: []}])
        }
    }

    const handleSearchDeploymentName = (newValue: string) => {
        forgetRefusedName()
        setSearchString(newValue)
        // If a user types something new, mark as new deployment
        if (newValue && !deploymentNames.some(dep => dep.name === newValue)) {
            setIsNewDeployment(true)
        }
    }

    const handleChangeDeploymentName = (newValue: string) => {
        forgetRefusedName()
        if (newValue) {
            setSearchString('')
            // Check if this is an existing deployment or a new one
            const existingDeployment = deploymentNames.find(dep => dep.id === newValue)
            if (existingDeployment) {
                setIsNewDeployment(false)
                form.setFieldsValue({ deploymentName: existingDeployment.name })
            } else {
                setIsNewDeployment(true)
                form.setFieldsValue({ deploymentName: newValue })
            }
        }
    }

    const onBlurDeploymentName = () => {
        if (searchString) {
            setIsNewDeployment(true)
            form.setFieldsValue({ deploymentName: searchString })
        }
    }

    const deploymentRepositoriesOptions = useMemo(() => {
        return deploymentRepositories.map(group => ({
            value: group.id,
            label: group.name,
        }))
    }, [deploymentRepositories])

    // A repository that takes the main branch only refuses a project that sits anywhere else. The dialog
    // says so on the repository itself instead of letting the deploy fail.
    const mainBranchOnlyBlocked = useMemo(() => {
        const repository = deploymentRepositories.find(repo => repo.id === selectedRepository)
        return !!repository?.mainBranchOnly && !!detail?.branch && detail.branchDefault === false
    }, [deploymentRepositories, detail, selectedRepository])


    return (
        <>
            <Modal
                onCancel={handleClose}
                open={visible}
                width={800}
                footer={[
                    <Button key="cancel" disabled={isDeploying} onClick={handleClose}>
                        {t('deploy:buttons.cancel')}
                    </Button>,
                    <Button
                        key="deploy"
                        data-testid="deploy-submit"
                        disabled={mainBranchOnlyBlocked}
                        icon={isDeploying ? <LoadingOutlined /> : <RocketOutlined />}
                        loading={isDeploying}
                        onClick={handleDeploy}
                        type="primary"
                    >
                        {isDeploying ? t('deploy:messages.deploying') : t('deploy:buttons.deploy')}
                    </Button>,
                ]}
                title={
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <RocketOutlined style={{ marginRight: 8 }} />
                        {t('deploy:title', { projectName: detail?.name })}
                    </div>
                }
            >
                <Spin description={t('deploy:messages.deploying_project')} spinning={isDeploying}>
                    <Space orientation="vertical" size="large" style={{ width: '100%', minWidth: 0, paddingTop: 16 }}>
                        {mainBranchOnlyBlocked && (
                            <Alert
                                showIcon
                                data-testid="deploy-main-branch-only"
                                title={t('deploy:notifications.main_branch_only', { branch: detail?.branch })}
                                type="warning"
                            />
                        )}
                        <Form
                            labelWrap
                            form={form}
                            labelAlign="right"
                            labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                            name="deploy_form"
                            style={{ minWidth: 0 }}
                            // Sized from nothing rather than from what it holds: the reason a name is refused is
                            // a long sentence, which wraps under the field instead of pushing the field below
                            // its label.
                            wrapperCol={{ flex: '1 1 0' }}
                        >
                            <Select
                                required
                                formItemStyle={{ minWidth: 0 }}
                                label={t('deploy:repository.label')}
                                name="repository"
                                options={deploymentRepositoriesOptions}
                                placeholder={t('deploy:repository.placeholder')}
                                style={{ width: '100%' }}
                                suffixIcon={<BranchesOutlined />}
                            />
                            <Select
                                required
                                defaultActiveFirstOption={false}
                                disabled={!selectedRepository}
                                label={t('deploy:deployment_name.label')}
                                name="deploymentName"
                                notFoundContent={null}
                                onBlur={onBlurDeploymentName}
                                onChange={handleChangeDeploymentName}
                                options={deploymentNames.map(dep => ({ value: dep.id, label: dep.name }))}
                                placeholder={t('deploy:deployment_name.placeholder')}
                                showSearch={{ filterOption: false, onSearch: handleSearchDeploymentName }}
                                style={{ width: '100%' }}
                                suffixIcon={null}
                            />
                            <TextArea
                                required
                                label={t('deploy:comment.label')}
                                name="comment"
                                placeholder={t('deploy:comment.placeholder')}
                            />
                        </Form>
                    </Space>
                </Spin>
            </Modal>
            {commitInfoModal}
        </>
    )
}

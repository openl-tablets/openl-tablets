import React, { useState, useEffect, useMemo } from 'react'
import { Modal, Form, Button, Space } from 'antd'
import { RocketOutlined, BranchesOutlined } from '@ant-design/icons'
import { useGlobalEvents } from '../hooks'
import { Select, TextArea } from '../components/form'
import { apiCall } from '../services'
import { Repository } from '../types/repositories'
import { useTranslation } from 'react-i18next'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../constants'

interface DeployModalDetail {
    branch: string
    comment: string
    id: string
    modifiedAt: string
    modifiedBy: string
    name: string
    repository: string
    revision: string
    selectedBranches: string[]
    status: string
}

/**
 * DeployModal component
 * @example to call this modal, dispatch a custom event 'openDeployModal' with details:
 * window.dispatchEvent(new CustomEvent('openDeployModal', {detail: {test:'test'}}))
 */
export const DeployModal: React.FC = () => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const selectedRepository = Form.useWatch('repository', form)
    const { detail } = useGlobalEvents<DeployModalDetail>('openDeployModal')
    const [visible, setVisible] = useState(false)
    const [searchString, setSearchString] = useState('')
    const [deploymentRepositories, setDeploymentRepositories] = useState<Repository[]>([])
    const [deploymentNames, setDeploymentNames] = useState<Array<{id: string, name: string}>>([])
    const [isNewDeployment, setIsNewDeployment] = useState<boolean>(false)

    const fetchDeploymentRepositories = async () => {
        const response: Repository[] = await apiCall('/production-repos')
        setDeploymentRepositories(response)
    }

    const fetchDeploymentNames = async () => {
        const response: Array<{id: string, name: string}> = await apiCall(`/deployments?repository=${selectedRepository}`)
        setDeploymentNames(response)
    }

    useEffect(() => {
        if (visible && !deploymentRepositories.length) {
            fetchDeploymentRepositories()
        }
    }, [visible, deploymentRepositories])

    useEffect(() => {
        if (selectedRepository) {
            fetchDeploymentNames()
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

    const handleDeploy = async () => {
        try {
            const values = await form.validateFields()
            
            const { repository, deploymentName, comment } = values
            const projectId = detail?.id
            
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
                })
            } else {
                // Deploy to existing deployment
                const selectedDeployment = deploymentNames.find(dep => dep.name === deploymentName)
                if (selectedDeployment) {
                    await apiCall(`/deployments/${selectedDeployment.id}`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            comment,
                            projectId,
                        }),
                    })
                }
            }
            
            handleClose()
        } catch (info) {
            console.error('Validate Failed:', info)
        }
    }

    const handleSearchDeploymentName = (newValue: string) => {
        setSearchString(newValue)
        // If a user types something new, mark as new deployment
        if (newValue && !deploymentNames.find(dep => dep.name === newValue)) {
            setIsNewDeployment(true)
        }
    }

    const handleChangeDeploymentName = (newValue: string) => {
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

    return (
        <Modal
            onCancel={handleClose}
            open={visible}
            width={800}
            footer={[
                <Button key="cancel" onClick={handleClose}>
                    {t('deploy:buttons.cancel')}
                </Button>,
                <Button key="deploy" onClick={handleDeploy} type="primary">
                    {t('deploy:buttons.deploy')}
                </Button>,
            ]}
            title={
                <div style={{ display: 'flex', alignItems: 'center' }}>
                    <RocketOutlined style={{ marginRight: 8 }} />
                    {t('deploy:title', { projectName: detail?.name })}
                </div>
            }
        >
            <Space direction="vertical" size="large" style={{ width: '100%', paddingTop: 16 }}>
                <Form 
                    labelWrap
                    form={form}
                    labelAlign="right"
                    labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                    name="deploy_form"
                    wrapperCol={{ flex: 1 }}
                >
                    <Select
                        required
                        label={t('deploy:repository.label')}
                        name="repository"
                        options={deploymentRepositoriesOptions}
                        placeholder={t('deploy:repository.placeholder')}
                        suffixIcon={<BranchesOutlined />}
                    />
                    <Select
                        required
                        showSearch
                        defaultActiveFirstOption={false}
                        disabled={!selectedRepository}
                        filterOption={false}
                        label={t('deploy:deployment_name.label')}
                        name="deploymentName"
                        notFoundContent={null}
                        onBlur={onBlurDeploymentName}
                        onChange={handleChangeDeploymentName}
                        onSearch={handleSearchDeploymentName}
                        options={deploymentNames.map(dep => ({ value: dep.id, label: dep.name }))}
                        placeholder={t('deploy:deployment_name.placeholder')}
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
        </Modal>
    )
}

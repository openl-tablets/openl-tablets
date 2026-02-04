import React, { forwardRef, useEffect, useImperativeHandle, useMemo } from 'react'
import { Button, Divider, Form, Modal, Row, Tabs, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { RepositoryDataType, RepositoryType } from './constants'
import { WIDTH_OF_FORM_LABEL } from '../../constants'
import { Input, Select } from '../../components'
import { DesignRepositoryCommentsConfiguration } from './DesignRepositoryCommentsConfiguration'
import { RepositoryConfigurationComponent } from './RepositoryConfigurationComponent'
import { useRepositoryConfiguration } from './hooks'
import { FormRefProps, RepositoryResponse } from './index'

interface DesignRepositoriesConfigurationProps {
    repositoryDataType: RepositoryDataType
    onEditingStateChange?: (isEditing: boolean) => void
}

export const DesignRepositoriesConfiguration = forwardRef<FormRefProps, DesignRepositoriesConfigurationProps>(({ repositoryDataType, onEditingStateChange }, ref) => {
    const { t } = useTranslation()
    const { isLoading: isConfigurationLoading,
        configuration: initialConfiguration = [],
        fetchRepositoryConfigurationTemplate,
        deleteRepositoryConfiguration,
        updateRepositoryConfiguration,
        setURLSearchParam } = useRepositoryConfiguration(repositoryDataType)
    const [configuration, setConfiguration] = React.useState<RepositoryResponse[]>(initialConfiguration as RepositoryResponse[])
    const [activeRepository, setActiveRepository] = React.useState<RepositoryResponse | null>(null)
    const [activeKey, setActiveKey] = React.useState(() => {
        const queryParams = new URLSearchParams(window.location.search)
        return queryParams.get('r') || (Array.isArray(initialConfiguration) && initialConfiguration.length && initialConfiguration[0]?.id)
    })
    const [defaultConfiguration, setDefaultConfiguration] = React.useState(null)
    const [isEditingNewRepository, setIsEditingNewRepository] = React.useState(false)
    const [isLoading, setIsLoading] = React.useState(false)
    const [isLoadingForm, setIsLoadingForm] = React.useState(false)
    const [form] = Form.useForm()
    const repositoryType = Form.useWatch('type', form)

    const fetchDefaultConfiguration = async (type: string) => {
        const { id, name, ...defaultConfig } = await fetchRepositoryConfigurationTemplate(type)
        setDefaultConfiguration(defaultConfig)
        form.setFieldsValue(defaultConfig)
    }

    useImperativeHandle(ref, () => ({
        getForm: () => form,
        addRepository: async () => {
            setIsLoading(true)
            try {
                const initialConfig = await fetchRepositoryConfigurationTemplate(RepositoryType.GIT)
                // If there are no repositories, set the configuration to an array with the new repository
                if (!configuration || !Array.isArray(configuration) || configuration.length === 0) {
                    setConfiguration([initialConfig])
                } else {
                    setConfiguration([...configuration, initialConfig])
                }
                setActiveKey(initialConfig.id)
                form.setFieldsValue(initialConfig)
                setIsEditingNewRepository(true)
                onEditingStateChange?.(true)
            } finally {
                setIsLoading(false)
            }
        },
        isEditingNewRepository: () => isEditingNewRepository
    }))

    useEffect(() => {
        if (initialConfiguration && Array.isArray(initialConfiguration) && initialConfiguration.length > 0) {
            let selectedRepository
            if (activeKey) {
                selectedRepository = initialConfiguration.find(repo => repo.id === activeKey)
            }
            if (!selectedRepository) {
                selectedRepository = initialConfiguration[0]
                setActiveKey(selectedRepository.id)
                setURLSearchParam(selectedRepository.id)
            }
            setConfiguration(initialConfiguration)
            setActiveRepository(selectedRepository)
            form.setFieldsValue(selectedRepository)
        }
    }, [initialConfiguration])

    const onDeleteRepository = async (id: string) => {
        setIsLoading(true)
        try {
            await deleteRepositoryConfiguration(id)
        } finally {
            setIsLoading(false)
        }
    }

    const handleDeleteRepository = async (id: string) => {
        Modal.confirm({
            title: t('repository:confirm_delete_repository'),
            content: t('repository:confirm_delete_repository_message'),
            onOk: () => {
                onDeleteRepository(id)
            }
        })
    }

    const onApplyConfiguration = async (values: any) => {
        setIsLoading(true)
        try {
            await updateRepositoryConfiguration(values)
            setIsEditingNewRepository(false)
            onEditingStateChange?.(false)
        } finally {
            setIsLoading(false)
        }
    }

    const handleApplyConfiguration = async (values: any) => {
        Modal.confirm({
            title: t('repository:confirm_apply_configuration'),
            content: t('repository:confirm_apply_configuration_message'),
            onOk: () => {
                onApplyConfiguration(values)
            },
        })
    }

    const onEdit = async (targetKey: any, action: string) => {
        if (action === 'remove') {
            handleDeleteRepository(targetKey)
        }
    }

    const onFinish = (values: any) => {
        handleApplyConfiguration(values)
    }

    const onChangeType = (value: any) => {
        if (activeRepository
            && repositoryType
            && activeRepository.type !== value)
        {
            fetchDefaultConfiguration(value)
        } else {
            form.setFieldsValue(activeRepository)
        }
    }

    const navigateTo = (key: string) => {
        setActiveKey(key)
        const selectedRepository = configuration.find(repo => repo.id === key)
        if (!selectedRepository) {
            return
        }
        setActiveRepository(selectedRepository)
        form.setFieldsValue(selectedRepository)
        setURLSearchParam(key)

        if (tabType === 'card') {
            setConfiguration(prev => prev.slice(0, -1))
        }

        // Reset editing state when navigating to a different repository
        setIsEditingNewRepository(false)
        onEditingStateChange?.(false)
    }

    const navigateToWithDelay = (key: string ) => {
        setIsLoadingForm(true)
        // Allow React to render the spinner
        requestAnimationFrame(() => {
            navigateTo(key)
            // Ensure minimum visibility for better UX
            setTimeout(() => {
                setIsLoadingForm(false)
            }, 200)
        })
    }

    const onChangeTab = (key: string) => {
        if (form?.isFieldsTouched() || tabType === 'card') {
            Modal.confirm({
                title: t('repository:confirm_leave_without_saving'),
                content: t('repository:confirm_leave_without_saving_message'),
                onOk: () => {
                    navigateToWithDelay(key)
                },
            })
        } else {
            navigateToWithDelay(key)
        }
    }

    const deploymentBranchOptions = [
        { label: t('repository:any_branch'), value: false },
        { label: t('repository:main_branch_only'), value: true },
    ]

    const configurationData = useMemo(() => {
        if (repositoryType === activeRepository?.type) {
            return activeRepository
        }
        return defaultConfiguration
    }, [repositoryType, defaultConfiguration, activeRepository])

    const tabType = useMemo(() => {
        if (Array.isArray(initialConfiguration) && configuration && initialConfiguration.length === configuration.length) {
            return 'editable-card'
        }
        return 'card'
    }, [initialConfiguration, configuration])

    // Check if we have repositories
    const hasRepositories = initialConfiguration && Array.isArray(initialConfiguration) && initialConfiguration.length > 0

    if (isConfigurationLoading) {
        return <Spin spinning={true} style={{ margin: 'auto', width: '100%' }} />
    }

    // If there are no repositories and no configuration in state, show a message
    if (!hasRepositories && (!configuration || configuration.length === 0)) {
        const repositoryType = repositoryDataType === RepositoryDataType.DESIGN ? t('repository:design') : t('repository:deployment')
        return (
            <div style={{ padding: '20px', textAlign: 'center' }}>
                <p>{t('repository:no_repositories_available')}</p>
                <p>{t('repository:click_add_repository_to_create_first', { type: repositoryType })}</p>
            </div>
        )
    }

    // If there are repositories but no active key, select the first one
    if (!activeKey && configuration && configuration.length > 0) {
        const firstRepo = configuration[0]
        setActiveKey(firstRepo.id)
        setActiveRepository(firstRepo)
        form.setFieldsValue(firstRepo)
        setURLSearchParam(firstRepo.id)
        return null // Return null to trigger a re-render
    }

    // If still no active key
    if (!activeKey) {
        return null
    }

    // Render the tabs with repositories
    return (
        <Spin spinning={isLoading} tip={t('repository:messages.waiting_for_repository_operation')}>
            <Tabs
                destroyOnHidden
                hideAdd
                activeKey={activeKey}
                className="repositories-tabs"
                onChange={onChangeTab}
                onEdit={onEdit}
                tabPosition="left"
                type={tabType}
                items={configuration?.map((repository) => ({
                    label: typeof repository.name === 'string' ? repository.name : repository?.name?.value,
                    key: repository.id,
                    children: (
                        <Spin spinning={isLoadingForm}>
                            <Form
                                key={activeKey}
                                labelWrap
                                form={form}
                                initialValues={defaultConfiguration || activeRepository || undefined}
                                labelAlign="right"
                                labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
                                onFinish={onFinish}
                                wrapperCol={{ flex: 1 }}
                            >
                                <Divider titlePlacement="start">{t('repository:common')}</Divider>
                                <RepositoryConfigurationComponent configuration={configurationData} onChangeType={onChangeType} repositoryDataType={repositoryDataType} repositoryType={repositoryType} />
                                {repositoryDataType === RepositoryDataType.DESIGN && (
                                    <DesignRepositoryCommentsConfiguration />
                                )}
                                {repositoryDataType === RepositoryDataType.DEPLOYMENT && (
                                    <Select label={t('repository:deployment_branch')} name={['settings', 'mainBranchOnly']} options={deploymentBranchOptions} />
                                )}
                                <Row justify="end">
                                    <Button htmlType="submit" loading={isLoading} type="primary">{t('repository:buttons.apply_changes')}</Button>
                                </Row>
                                <Input hidden name="id" />
                            </Form>
                        </Spin>
                    )
                }))}
            />
        </Spin>
    )
})

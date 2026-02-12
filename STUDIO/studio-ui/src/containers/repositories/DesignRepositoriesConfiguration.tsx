import React, { forwardRef, useEffect, useImperativeHandle, useMemo, useRef } from 'react'
import { Button, Divider, Form, Modal, Row, Tabs, Spin } from 'antd'
import { useTranslation } from 'react-i18next'
import { isFormValuesEqual } from './formComparison'
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
    const [isFormInitialized, setIsFormInitialized] = React.useState(false)
    const [form] = Form.useForm()
    const repositoryType = Form.useWatch('type', form)
    // Use ref to track the last synced repository to prevent false positives when switching
    const lastSyncedRepositoryRef = useRef<RepositoryResponse | null>(null)
    // Store RAF IDs to cancel them on unmount or before scheduling new ones
    const rafIdsRef = useRef<number[]>([])

    const fetchDefaultConfiguration = async (type: string) => {
        const { id, name, ...defaultConfig } = await fetchRepositoryConfigurationTemplate(type)
        setDefaultConfiguration(defaultConfig)
        form.setFieldsValue(defaultConfig)
    }

    /**
     * Checks if there are unsaved changes in the form.
     * Returns true if editing a new repository or if form values differ from active repository.
     * Uses isFormValuesEqual which properly handles:
     * - Recursive comparison of nested objects (including settings.password, settings.secretKey, etc.)
     * - Array comparison (allAllowedRegions, allSseAlgorithms)
     * - Sensitive fields normalization (empty/undefined treated as equal for nested fields like settings.password)
     * - Boolean values normalization (false/undefined treated as equal for fields like mainBranchOnly)
     */
    const checkHasUnsavedChanges = (): boolean => {
        // If editing a new repository, always consider it as having unsaved changes
        if (isEditingNewRepository) return true
        
        // If no active repository or still loading configuration, no changes to track
        if (!activeRepository || isConfigurationLoading) return false
        
        // Get current form values
        const current = form.getFieldsValue(true) as Record<string, unknown>
        
        // If form is empty (not yet initialized), don't consider it as having changes
        if (!current || Object.keys(current).length === 0) {
            return false
        }
        
        // Critical check: verify that form ID matches activeRepository ID
        // This prevents false positives when switching between repositories where form
        // may still have values from previous repository before syncing with new one
        // This is especially important for Deployment repositories when switching tabs
        const formId = current.id
        if (formId && activeRepository.id && formId !== activeRepository.id) {
            // Form is showing a different repository, don't consider it as having changes
            // This means we're in the middle of switching repositories
            return false
        }
        
        // If form is not initialized yet, don't consider it as having changes
        // This prevents false positives when switching between repositories or tabs
        // The form may still be syncing with the new repository data
        if (!isFormInitialized) {
            return false
        }
        
        // Use the last synced repository ref for comparison if available
        // This prevents false positives when switching between repositories
        const repositoryToCompare = lastSyncedRepositoryRef.current || activeRepository
        
        // Verify that we're comparing the same repository
        if (repositoryToCompare.id !== activeRepository.id) {
            // We're comparing different repositories, don't consider it as having changes
            return false
        }
        
        // Critical check: verify form is actually synced with repository
        // This is the key check to prevent false positives when switching tabs
        // For Deployment repositories, this is especially important due to mainBranchOnly field
        // For sensitive fields like settings.password, isFormValuesEqual handles automatic
        // changes from InputPassword component (undefined vs saved encrypted value)
        const isFormSynced = isFormValuesEqual(current, repositoryToCompare as unknown as Record<string, unknown>)
        
        if (isFormSynced) {
            // Form is synced with repository, no changes
            // Update last synced repository ref if needed
            if (lastSyncedRepositoryRef.current?.id !== activeRepository.id) {
                lastSyncedRepositoryRef.current = activeRepository
            }
            return false
        }
        
        // Form values differ from repository AND form is initialized AND IDs match
        // This means user has actually made changes
        return true
    }

    /**
     * Synchronizes the form with a repository by resetting, clearing, setting values,
     * and verifying sync using double requestAnimationFrame with retry logic.
     * Updates lastSyncedRepositoryRef and setIsFormInitialized on success.
     */
    const syncFormWithRepository = (selectedRepository: RepositoryResponse) => {
        // Completely reset and clear form to ensure clean state
        form.resetFields()
        // Clear all form values first to ensure clean state
        const allFields = form.getFieldsValue()
        Object.keys(allFields).forEach(key => {
            form.setFieldValue(key, undefined)
        })
        // Set the repository values
        form.setFieldsValue(selectedRepository)
        
        // Mark form as initialized after verifying form is synced with repository
        // Use double requestAnimationFrame to ensure form values are fully set
        const rafId1 = requestAnimationFrame(() => {
            const rafId2 = requestAnimationFrame(() => {
                // Verify form is actually synced before marking as initialized
                const currentValues = form.getFieldsValue(true) as Record<string, unknown>
                if (currentValues && Object.keys(currentValues).length > 0 && selectedRepository) {
                    const isSynced = isFormValuesEqual(currentValues, selectedRepository as unknown as Record<string, unknown>)
                    if (isSynced) {
                        // Form is synced, update last synced repository ref
                        lastSyncedRepositoryRef.current = selectedRepository
                        setIsFormInitialized(true)
                    } else {
                        // If not synced, try once more and then mark as initialized
                        form.setFieldsValue(selectedRepository)
                        // Wait a bit more and verify again
                        const rafId3 = requestAnimationFrame(() => {
                            const retryValues = form.getFieldsValue(true) as Record<string, unknown>
                            const retrySynced = isFormValuesEqual(retryValues, selectedRepository as unknown as Record<string, unknown>)
                            if (retrySynced) {
                                // Form is synced after retry, update last synced repository ref
                                lastSyncedRepositoryRef.current = selectedRepository
                                setIsFormInitialized(true)
                            } else {
                                // If still not synced after retry, mark as initialized anyway
                                // The form should be synced by now, and this prevents infinite waiting
                                // The checkHasUnsavedChanges will verify actual sync before reporting changes
                                lastSyncedRepositoryRef.current = selectedRepository
                                setIsFormInitialized(true)
                            }
                        })
                        rafIdsRef.current.push(rafId3)
                    }
                } else {
                    // If form is empty, mark as initialized anyway (will be set on next render)
                    lastSyncedRepositoryRef.current = selectedRepository
                    setIsFormInitialized(true)
                }
            })
            rafIdsRef.current.push(rafId2)
        })
        rafIdsRef.current.push(rafId1)
    }

    useImperativeHandle(ref, () => ({
        getForm: () => form,
        hasUnsavedChanges: checkHasUnsavedChanges,
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
        // Cancel any pending RAFs before scheduling new ones
        rafIdsRef.current.forEach(id => cancelAnimationFrame(id))
        rafIdsRef.current = []
        
        // Reset initialization flag when configuration changes (component remounts or config updates)
        setIsFormInitialized(false)
        
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
            // Clear last synced repository ref when configuration changes
            lastSyncedRepositoryRef.current = null
            // Sync form with the selected repository
            syncFormWithRepository(selectedRepository)
            // Reset editing state when configuration is loaded/changed
            setIsEditingNewRepository(false)
            onEditingStateChange?.(false)
        }
        
        // Cleanup: cancel all pending RAFs on unmount or when dependencies change
        return () => {
            rafIdsRef.current.forEach(id => cancelAnimationFrame(id))
            rafIdsRef.current = []
        }
    }, [initialConfiguration])

    // Cleanup effect: cancel all pending RAFs on component unmount
    useEffect(() => {
        return () => {
            rafIdsRef.current.forEach(id => cancelAnimationFrame(id))
            rafIdsRef.current = []
        }
    }, [])

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
        // Cancel any pending RAFs before scheduling new ones
        rafIdsRef.current.forEach(id => cancelAnimationFrame(id))
        rafIdsRef.current = []
        
        setActiveKey(key)
        const selectedRepository = configuration.find(repo => repo.id === key)
        if (!selectedRepository) {
            return
        }
        // Reset initialization flag before setting new values
        // This is critical to prevent false positives when switching between repositories
        setIsFormInitialized(false)
        // Clear last synced repository ref when switching to a new repository
        lastSyncedRepositoryRef.current = null
        setActiveRepository(selectedRepository)
        setURLSearchParam(key)

        if (tabType === 'card') {
            setConfiguration(prev => prev.slice(0, -1))
        }

        // Sync form with the selected repository
        syncFormWithRepository(selectedRepository)

        // Reset editing state when navigating to a different repository
        setIsEditingNewRepository(false)
        onEditingStateChange?.(false)
    }

    const navigateToWithDelay = (key: string ) => {
        // Cancel any pending RAFs before scheduling new ones
        rafIdsRef.current.forEach(id => cancelAnimationFrame(id))
        rafIdsRef.current = []
        
        setIsLoadingForm(true)
        // Allow React to render the spinner
        const rafId = requestAnimationFrame(() => {
            navigateTo(key)
            // Ensure minimum visibility for better UX
            setTimeout(() => {
                setIsLoadingForm(false)
            }, 200)
        })
        rafIdsRef.current.push(rafId)
    }

    const onChangeTab = (key: string) => {
        // Only check for changes if we're actually switching to a different repository
        const isSwitchingRepository = key !== activeKey
        
        if (!isSwitchingRepository) {
            // Same repository, no need to check
            return
        }
        
        // Check for unsaved changes using shared logic
        // This will return false if:
        // - Form is not initialized yet
        // - Form ID doesn't match activeRepository ID
        // - Form is synced with repository (including handling of sensitive fields like settings.password)
        const hasChanges = checkHasUnsavedChanges()
        
        if (hasChanges || tabType === 'card') {
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
        // Mark form as initialized after values are set (will be set in useEffect, but ensure it here too)
        setTimeout(() => {
            setIsFormInitialized(true)
        }, 100)
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
                                initialValues={isFormInitialized && activeRepository ? activeRepository : undefined}
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

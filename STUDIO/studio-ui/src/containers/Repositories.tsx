import { useEffect, useRef, useState } from 'react'
import { App, Tabs, Button, Tooltip } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router'
import { DesignRepositoriesConfiguration } from './repositories/DesignRepositoriesConfiguration'
import { RepositoryDataType } from './repositories/constants'
import { useTranslation } from 'react-i18next'
import { FormRefProps } from './repositories/index'
import { apiCall } from '../services'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from '../services/apiCall'

export const Repositories = () => {
    const { modal } = App.useApp()
    const { t } = useTranslation()
    const { repositoryTab } = useParams()
    const navigate = useNavigate()
    const designFormRef = useRef<FormRefProps>(null)
    const deploymentFormRef = useRef<FormRefProps>(null)
    const [isEditingNewRepository, setIsEditingNewRepository] = useState(false)
    // Whether each list of repositories is set outside OpenL Studio, which takes no repository added or removed
    const [readOnlyLists, setReadOnlyLists] = useState<Record<RepositoryDataType, boolean>>()

    useEffect(() => {
        const repositoryDataTypes = Object.values(RepositoryDataType)
        const listsOf = (readOnly: (i: number) => boolean) => Object.fromEntries(
            repositoryDataTypes.map((repositoryDataType, i) => [repositoryDataType, readOnly(i)])
        ) as Record<RepositoryDataType, boolean>
        Promise.all(repositoryDataTypes.map(repositoryDataType =>
            apiCall(`/admin/settings/repos/${repositoryDataType}/configs`, undefined, LOCAL_LOAD_API_OPTIONS) as Promise<{ readOnly: boolean }>
        )).then(lists => {
            setReadOnlyLists(listsOf(i => lists[i]!.readOnly))
        }).catch((error: unknown) => {
            // The server refuses to add or remove a repository of a list set outside, so the buttons stay offered.
            notifyLoadFailure(t('repository:list_load_failed'), error)
            setReadOnlyLists(listsOf(() => false))
        })
    }, [t])

    // Get the current repository data type based on the active tab
    const currentRepositoryDataType = repositoryTab === RepositoryDataType.DEPLOYMENT
        ? RepositoryDataType.DEPLOYMENT
        : RepositoryDataType.DESIGN

    // Get the ref for the currently active tab
    const currentFormRef = currentRepositoryDataType === RepositoryDataType.DEPLOYMENT
        ? deploymentFormRef
        : designFormRef

    const navigateTo = (key: string) => {
        if (key !== repositoryTab) {
            void navigate(`/administration/repositories/${key}`)
        }
    }

    const onChangeTab = (key: string) => {
        // Only check for unsaved changes if we're actually switching to a different tab
        // and the form is initialized (to avoid false positives during form initialization)
        if (key !== repositoryTab) {
            const hasUnsavedChanges = currentFormRef.current?.hasUnsavedChanges?.() ?? false

            if (hasUnsavedChanges) {
                modal.confirm({
                    title: t('repository:confirm_leave_without_saving'),
                    content: t('repository:confirm_leave_without_saving_message'),
                    onOk: () => {
                        navigateTo(key)
                        // Reset editing state when switching between Design and Deployment tabs
                        setIsEditingNewRepository(false)
                    },
                })
            } else {
                navigateTo(key)
                // Reset editing state when switching between Design and Deployment tabs
                setIsEditingNewRepository(false)
            }
        }
    }

    const handleAddRepository = async () => {
        if (currentFormRef.current) {
            await currentFormRef.current.addRepository()
            setIsEditingNewRepository(true)
        }
    }

    const isListReadOnly = readOnlyLists?.[currentRepositoryDataType] ?? false

    const addRepositoryButtonLabel = currentRepositoryDataType === RepositoryDataType.DESIGN
        ? t('repository:add_design_repository')
        : t('repository:add_deployment_repository')

    const tabItems = [
        {
            key: RepositoryDataType.DESIGN,
            label: t('repository:tabs.design_repositories'),
            children: (
                <div style={{ minHeight: '400px' }}>
                    <DesignRepositoriesConfiguration
                        ref={designFormRef}
                        isListReadOnly={readOnlyLists?.[RepositoryDataType.DESIGN]}
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DESIGN}
                    />
                </div>
            ),
        },
        {
            key: RepositoryDataType.DEPLOYMENT,
            label: t('repository:tabs.deployment_repositories'),
            children: (
                <div style={{ minHeight: '400px' }}>
                    <DesignRepositoriesConfiguration
                        ref={deploymentFormRef}
                        isListReadOnly={readOnlyLists?.[RepositoryDataType.DEPLOYMENT]}
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DEPLOYMENT}
                    />
                </div>
            ),
        },
    ]

    return (
        <Tabs
            destroyOnHidden
            {...(repositoryTab !== undefined && { activeKey: repositoryTab })}
            items={tabItems}
            onChange={onChangeTab}
            tabBarExtraContent={
                <Tooltip title={isListReadOnly ? t('repository:list_set_outside') : undefined}>
                    <Button
                        // Until the lists are read, it is not known whether a repository can be added.
                        disabled={isEditingNewRepository || readOnlyLists === undefined || isListReadOnly}
                        icon={<PlusOutlined />}
                        onClick={handleAddRepository}
                        type="text"
                    >
                        {addRepositoryButtonLabel}
                    </Button>
                </Tooltip>
            }
        />
    )
}

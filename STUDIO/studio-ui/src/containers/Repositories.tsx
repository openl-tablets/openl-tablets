import React, { useRef, useState } from 'react'
import { Modal, Tabs, Button } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { DesignRepositoriesConfiguration } from './repositories/DesignRepositoriesConfiguration'
import './Repositories.scss'
import { RepositoryDataType } from './repositories/constants'
import { useTranslation } from 'react-i18next'
import { FormRefProps } from './repositories/index'

export const Repositories = () => {
    const { t } = useTranslation()
    const { repositoryTab } = useParams()
    const navigate = useNavigate()
    const designFormRef = useRef<FormRefProps>(null)
    const deploymentFormRef = useRef<FormRefProps>(null)
    const [isEditingNewRepository, setIsEditingNewRepository] = useState(false)

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
            navigate(`/administration/repositories/${key}`)
        }
    }

    const onChangeTab = (key: string) => {
        // Only check for unsaved changes if we're actually switching to a different tab
        // and the form is initialized (to avoid false positives during form initialization)
        if (key !== repositoryTab) {
            const hasUnsavedChanges = currentFormRef.current?.hasUnsavedChanges?.() ?? false

            if (hasUnsavedChanges) {
                Modal.confirm({
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

    const addRepositoryButtonLabel = currentRepositoryDataType === RepositoryDataType.DESIGN
        ? t('repository:add_design_repository')
        : t('repository:add_deployment_repository')

    return (
        <Tabs
            destroyOnHidden
            activeKey={repositoryTab}
            onChange={onChangeTab}
            tabBarExtraContent={
                <Button
                    disabled={isEditingNewRepository}
                    icon={<PlusOutlined />}
                    onClick={handleAddRepository}
                    type="text"
                >
                    {addRepositoryButtonLabel}
                </Button>
            }
        >
            <Tabs.TabPane key={RepositoryDataType.DESIGN} tab={t('repository:tabs.design_repositories')}>
                <div style={{ minHeight: '400px' }}>
                    <DesignRepositoriesConfiguration
                        ref={designFormRef}
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DESIGN}
                    />
                </div>
            </Tabs.TabPane>
            <Tabs.TabPane key={RepositoryDataType.DEPLOYMENT} tab={t('repository:tabs.deployment_repositories')}>
                <div style={{ minHeight: '400px' }}>
                    <DesignRepositoriesConfiguration
                        ref={deploymentFormRef}
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DEPLOYMENT}
                    />
                </div>
            </Tabs.TabPane>
        </Tabs>
    )
}

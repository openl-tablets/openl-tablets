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
    const formRef = useRef<FormRefProps>(null)
    const [isEditingNewRepository, setIsEditingNewRepository] = useState(false)
    
    // Get the current repository data type based on the active tab
    const currentRepositoryDataType = repositoryTab === RepositoryDataType.DEPLOYMENT 
        ? RepositoryDataType.DEPLOYMENT 
        : RepositoryDataType.DESIGN

    const navigateTo = (key: string) => {
        if (key !== repositoryTab) {
            navigate(`/administration/repositories/${key}`)
        }
    }

    const onChangeTab = (key: string) => {
        const form = formRef.current?.getForm()

        if (form?.isFieldsTouched()) {
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

    const handleAddRepository = async () => {
        if (formRef.current) {
            await formRef.current.addRepository()
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
                        ref={formRef} 
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DESIGN}
                    />
                </div>
            </Tabs.TabPane>
            <Tabs.TabPane key={RepositoryDataType.DEPLOYMENT} tab={t('repository:tabs.deployment_repositories')}>
                <div style={{ minHeight: '400px' }}>
                    <DesignRepositoriesConfiguration 
                        ref={formRef} 
                        onEditingStateChange={setIsEditingNewRepository}
                        repositoryDataType={RepositoryDataType.DEPLOYMENT}
                    />
                </div>
            </Tabs.TabPane>
        </Tabs>
    )
}

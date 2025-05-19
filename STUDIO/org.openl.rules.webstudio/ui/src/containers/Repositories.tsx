import React, { useRef } from 'react'
import { Modal, Tabs } from 'antd'
import { useNavigate, useParams } from 'react-router-dom'
import { DeployConfiguration } from './repositories/DeployConfiguration'
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

    const navigateTo = (key: string) => {
        if (key !== repositoryTab) {
            navigate(`/web/administration/repositories/${key}`)
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
                },
            })
        } else {
            navigateTo(key)
        }
    }

    return (
        <Tabs activeKey={repositoryTab} onChange={onChangeTab} destroyInactiveTabPane >
            <Tabs.TabPane key={RepositoryDataType.DESIGN} tab={t('repository:tabs.design_repositories')}>
                <DesignRepositoriesConfiguration repositoryDataType={RepositoryDataType.DESIGN} ref={formRef} />
            </Tabs.TabPane>
            <Tabs.TabPane key={RepositoryDataType.DEPLOY_CONFIGURATION} tab={t('repository:tabs.deploy_configuration_repository')}>
                <DeployConfiguration ref={formRef} />
            </Tabs.TabPane>
            <Tabs.TabPane key={RepositoryDataType.DEPLOYMENT} tab={t('repository:tabs.deployment_repositories')}>
                <DesignRepositoriesConfiguration repositoryDataType={RepositoryDataType.DEPLOYMENT} ref={formRef} />
            </Tabs.TabPane>
        </Tabs>
    )
}
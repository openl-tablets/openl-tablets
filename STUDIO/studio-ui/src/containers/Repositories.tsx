import React, { useRef } from 'react'
import { Modal, Tabs } from 'antd'
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
                },
            })
        } else {
            navigateTo(key)
        }
    }

    return (
        <Tabs destroyInactiveTabPane activeKey={repositoryTab} onChange={onChangeTab} >
            <Tabs.TabPane key={RepositoryDataType.DESIGN} tab={t('repository:tabs.design_repositories')}>
                <DesignRepositoriesConfiguration ref={formRef} repositoryDataType={RepositoryDataType.DESIGN} />
            </Tabs.TabPane>
            <Tabs.TabPane key={RepositoryDataType.DEPLOYMENT} tab={t('repository:tabs.deployment_repositories')}>
                <DesignRepositoriesConfiguration ref={formRef} repositoryDataType={RepositoryDataType.DEPLOYMENT} />
            </Tabs.TabPane>
        </Tabs>
    )
}

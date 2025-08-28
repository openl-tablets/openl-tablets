import React, { forwardRef, useEffect, useImperativeHandle, useMemo } from 'react'
import { Button, Divider, Form, Modal, Row, Tabs } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { RepositoryDataType, RepositoryType } from './constants'
import { WIDTH_OF_FORM_LABEL } from '../../constants'
import { Input, Select } from '../../components'
import { useTranslation } from 'react-i18next'
import { DesignRepositoryCommentsConfiguration } from './DesignRepositoryCommentsConfiguration'
import { RepositoryConfigurationComponent } from './RepositoryConfigurationComponent'
import { useRepositoryConfiguration } from './hooks'
import { FormRefProps, RepositoryResponse } from './index'

interface DesignRepositoriesConfigurationProps {
    repositoryDataType: RepositoryDataType
}

export const DesignRepositoriesConfiguration = forwardRef<FormRefProps, DesignRepositoriesConfigurationProps>(({ repositoryDataType }, ref) => {
    const { t } = useTranslation()
    const { configuration: initialConfiguration = [],
        fetchRepositoryConfigurationTemplate,
        handleApplyConfiguration,
        handleDeleteRepository,
        setURLSearchParam } = useRepositoryConfiguration(repositoryDataType)
    const [configuration, setConfiguration] = React.useState<RepositoryResponse[]>(initialConfiguration as RepositoryResponse[])
    const [activeRepository, setActiveRepository] = React.useState<RepositoryResponse | null>(null)
    const [activeKey, setActiveKey] = React.useState(() => {
        const queryParams = new URLSearchParams(window.location.search)
        return queryParams.get('r') || (Array.isArray(initialConfiguration) && initialConfiguration.length && initialConfiguration[0]?.id)
    })
    const [defaultConfiguration, setDefaultConfiguration] = React.useState(null)
    const [form] = Form.useForm()
    const repositoryType = Form.useWatch('type', form)

    const fetchDefaultConfiguration = async (type: string) => {
        const { id, name, ...defaultConfig } = await fetchRepositoryConfigurationTemplate(type)
        setDefaultConfiguration(defaultConfig)
        form.setFieldsValue(defaultConfig)
    }

    useImperativeHandle(ref, () => ({
        getForm: () => form,
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

    const onEdit = async (targetKey: any, action: string) => {
        if (action === 'add') {
            const initialConfig = await fetchRepositoryConfigurationTemplate(RepositoryType.GIT)
            setConfiguration([...configuration, initialConfig])
            setActiveKey(initialConfig.id)
            form.setFieldsValue(initialConfig)
        } else if (action === 'remove') {
            handleDeleteRepository(targetKey)
        }
    }

    const onFinish = ((values: any) => {
        handleApplyConfiguration(values)
    })

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

    const onChangeTab = (key: string) => {
        const navigateTo = () => {
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
        }

        if (form?.isFieldsTouched() || tabType === 'card') {
            Modal.confirm({
                title: t('repository:confirm_leave_without_saving'),
                content: t('repository:confirm_leave_without_saving_message'),
                onOk: () => {
                    navigateTo()
                },
            })
        } else {
            navigateTo()
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

    if (!activeKey) {
        return <div>Please select a repository</div>
    }

    if (!initialConfiguration || !Array.isArray(initialConfiguration) || initialConfiguration.length === 0) {
        return <div>No repositories available</div>
    }

    return (
        <Tabs
            destroyInactiveTabPane
            activeKey={activeKey}
            className="repositories-tabs"
            onChange={onChangeTab}
            onEdit={onEdit}
            tabPosition="left"
            type={tabType}
            addIcon={(
                <>
                    <PlusOutlined />
                    {t('repository:add_repository')}
                </>
            )}
            items={configuration?.map((repository) => ({
                label: repository.name,
                key: repository.id,
                children: (
                    <Form
                        key={repositoryType}
                        labelWrap
                        form={form}
                        initialValues={defaultConfiguration || activeRepository || undefined}
                        labelAlign="right"
                        labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
                        onFinish={onFinish}
                        wrapperCol={{ flex: 1 }}
                    >
                        <Divider orientation="left">{t('repository:common')}</Divider>
                        <RepositoryConfigurationComponent configuration={configurationData} onChangeType={onChangeType} repositoryDataType={repositoryDataType} repositoryType={repositoryType} />
                        {repositoryDataType === RepositoryDataType.DESIGN && (
                            <DesignRepositoryCommentsConfiguration />
                        )}
                        {repositoryDataType === RepositoryDataType.DEPLOYMENT && (
                            <Select label={t('repository:deployment_branch')} name={['settings', 'mainBranchOnly']} options={deploymentBranchOptions} />
                        )}
                        <Row justify="end">
                            <Button htmlType="submit" type="primary">Apply Changes</Button>
                        </Row>
                        <Input hidden name="id" />
                    </Form>
                )
            }))}
        />
    )
})

import React, { forwardRef, useEffect, useImperativeHandle, useMemo } from 'react'
import { Checkbox, Select } from '../../components'
import { useTranslation } from 'react-i18next'
import { Button, Form, Row } from 'antd'
import { WIDTH_OF_FORM_LABEL } from '../../constants'
import { apiCall } from '../../services'
import { RepositoryConfigurationComponent } from './RepositoryConfigurationComponent'
import { RepositoryDataType } from './constants'
import { useRepositoryConfiguration } from './hooks'
import { FormRefProps } from './index'

export const DeployConfiguration = forwardRef<FormRefProps>((_, ref) => {
    const { t } = useTranslation()
    const { configuration,
        handleApplyConfiguration,
        fetchRepositoryConfigurationTemplate } = useRepositoryConfiguration(RepositoryDataType.DEPLOY_CONFIGURATION)
    const [defaultConfiguration, setDefaultConfiguration] = React.useState(null)
    const [designRepositoryOptions, setDesignRepositoryOptions] = React.useState([])
    const [form] = Form.useForm()
    const useDesignRepository = Form.useWatch(['useDesignRepository'], { form, preserve: true })
    const repositoryType = Form.useWatch('type', form)

    useImperativeHandle(ref, () => ({
        getForm: () => form,
    }))

    const fetchDefaultConfiguration = async (type: string) => {
        const { id, name, ...defaultConfig } = await fetchRepositoryConfigurationTemplate(type)
        setDefaultConfiguration(defaultConfig)
        form.setFieldsValue(defaultConfig)
    }

    const fetchDesignRepositories = async () => {
        const options = await apiCall('/repos').then(response => {
            return response.map((repo: Record<string, string>) => ({
                label: repo.name,
                value: repo.id,
            }))
        })
        setDesignRepositoryOptions(options)
        // @ts-ignore
        form.setFieldValue('useDesignRepositoryForDeployConfig', configuration?.useDesignRepositoryForDeployConfig || options[0]?.value)
    }

    const onFinish = async (values: any) => {
        const { useDesignRepository, ...rest } = values
        // @ts-ignore
        const { name, settings } = configuration || {}

        handleApplyConfiguration({ name, settings, ...rest })
    }

    const configurationData = useMemo(() => {
        // @ts-ignore
        if (repositoryType === configuration?.type) {
            return configuration
        }
        return defaultConfiguration
    }, [repositoryType, defaultConfiguration, configuration])

    const onChangeType = (value: any) => {
        if (configuration
            && repositoryType
            // @ts-ignore
            && configuration.type !== value)
        {
            fetchDefaultConfiguration(value)
        } else {
            form.setFieldsValue(configuration)
        }
    }

    useEffect(() => {
        if (configuration
            && Object.values(configuration).length > 0
            // @ts-ignore
            && configuration.useDesignRepositoryForDeployConfig !== ''
        ) {
            form.setFieldsValue(configuration)
            form.setFieldValue('useDesignRepository', true)
        }
    }, [configuration])

    useEffect(() => {
        if (useDesignRepository && designRepositoryOptions.length === 0) {
            fetchDesignRepositories()
        }
    }, [useDesignRepository])

    if (!configuration) {
        return null
    }

    return (
        <Form
            labelWrap
            form={form}
            initialValues={configuration || undefined}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Checkbox label={t('repository:use_design_repository')} name="useDesignRepository" />
            {useDesignRepository ? (
                <Select label={t('repository:repository')} name="useDesignRepositoryForDeployConfig" options={designRepositoryOptions} />
            ) : (
                <RepositoryConfigurationComponent configuration={configurationData} onChangeType={onChangeType} repositoryDataType={RepositoryDataType.DEPLOY_CONFIGURATION} repositoryType={repositoryType} />
            )}
            <Row justify="end">
                <Button htmlType="submit" type="primary">Apply Changes</Button>
            </Row>
        </Form>
    )
})

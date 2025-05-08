import { Button, Divider, Form, Typography, Row, Modal, notification } from 'antd'
import React, { useEffect } from 'react'
import { WarningFilled } from '@ant-design/icons'
import { Trans, useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { Input, InputNumber, Checkbox } from '../components'
import { WIDTH_OF_FROM_LABEL } from '../constants'

type ValueWrapper<T> = {
    value: T
    readonly: boolean
} | T

interface SystemSettingsResponse {
    autoCompile: ValueWrapper<boolean>
    datePattern: ValueWrapper<string>
    dispatchingValidationEnabled: ValueWrapper<boolean>
    projectHistoryCount: ValueWrapper<number>
    testRunThreadCount: ValueWrapper<number>
    timeFormat: ValueWrapper<string>
    updateSystemProperties: ValueWrapper<boolean>
    userWorkspaceHome: ValueWrapper<string>
}

interface SystemSettings {
    autoCompile: boolean
    datePattern: string
    dispatchingValidationEnabled: boolean
    projectHistoryCount: number
    testRunThreadCount: number
    timeFormat: string
    updateSystemProperties: boolean
    userWorkspaceHome: string
}

const unwrapValue = <T, >(value: ValueWrapper<T>): T => {
    if (value !== null && typeof value === 'object' && 'value' in value) {
        return value.value
    }
    return value
}

const getValues = (settings: SystemSettingsResponse): SystemSettings => {
    const values = {} as SystemSettings

    values['autoCompile'] = unwrapValue(settings.autoCompile)
    values['datePattern'] = unwrapValue(settings.datePattern)
    values['dispatchingValidationEnabled'] = unwrapValue(settings.dispatchingValidationEnabled)
    values['projectHistoryCount'] = unwrapValue(settings.projectHistoryCount)
    values['testRunThreadCount'] = unwrapValue(settings.testRunThreadCount)
    values['timeFormat'] = unwrapValue(settings.timeFormat)
    values['updateSystemProperties'] = unwrapValue(settings.updateSystemProperties)
    values['userWorkspaceHome'] = unwrapValue(settings.userWorkspaceHome)

    return values
}

const unwrapReadOnly = (value: ValueWrapper<any>): boolean => {
    if (value !== null && typeof value === 'object' && 'readonly' in value) {
        return value.readonly
    }
    return false
}

const getReadOnlyFields = (settings: SystemSettingsResponse): Record<string, boolean> => {
    const readOnlyFields: Record<string, boolean> = {}

    readOnlyFields['autoCompile'] = unwrapReadOnly(settings.autoCompile)
    readOnlyFields['datePattern'] = unwrapReadOnly(settings.datePattern)
    readOnlyFields['dispatchingValidationEnabled'] = unwrapReadOnly(settings.dispatchingValidationEnabled)
    readOnlyFields['projectHistoryCount'] = unwrapReadOnly(settings.projectHistoryCount)
    readOnlyFields['testRunThreadCount'] = unwrapReadOnly(settings.testRunThreadCount)
    readOnlyFields['timeFormat'] = unwrapReadOnly(settings.timeFormat)
    readOnlyFields['updateSystemProperties'] = unwrapReadOnly(settings.updateSystemProperties)
    readOnlyFields['userWorkspaceHome'] = unwrapReadOnly(settings.userWorkspaceHome)

    return readOnlyFields
}

export const System: React.FC = () => {
    const { t } = useTranslation()
    const [systemSettings, setSystemSettings] = React.useState<SystemSettings | undefined>()
    const [readOnlyFields, setReadOnlyFields] = React.useState<Record<string, boolean>>({})
    const [form] = Form.useForm()

    const fetchSystemSettings = async () => {
        // Fetch system settings from the server
        const response: SystemSettingsResponse = await apiCall('/admin/settings/system')
        setSystemSettings(getValues(response))
        setReadOnlyFields(getReadOnlyFields(response))
    }

    const deleteAllHistory = async () => {
        // Delete all history from the server
        await apiCall('/history', { method: 'DELETE' })
            .then(() => {
                notification.success({
                    message: t('system:delete_all_history_success'),
                })
            })
    }

    const showDeleteAllHistoryConfirm = () => {
        // Show confirmation dialog before deleting all history
        Modal.confirm({
            title: t('system:confirm_delete_all_history'),
            content: t('system:confirm_delete_all_history_message'),
            onOk: deleteAllHistory,
        })
    }

    const showApplyConfirm = () => {
        // Show confirmation dialog before applying changes
        Modal.confirm({
            title: t('system:confirm_apply_settings'),
            content: t('system:confirm_apply_settings_message'),
            onOk: () => {
                form.submit()
            },
        })
    }

    const showRestoreDefaultsConfirm = () => {
        // Show confirmation dialog before restoring defaults
        Modal.confirm({
            title: t('system:confirm_restore_defaults'),
            content: t('system:confirm_restore_defaults_message'),
            onOk: () => {
                // Logic to restore default settings
                apiCall('/admin/settings/system', { method: 'DELETE' })
                    .then(() => {
                        window.location.reload()
                    })
            },
        })
    }

    const onFinish = async (values: SystemSettings) => {
        // Apply the changes to the system settings
        await apiCall('/admin/settings/system', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(values),
        }).then(() => {
            window.location.reload()
        })
    }

    useEffect(() => {
        fetchSystemSettings()
    }, [])

    if (!systemSettings) {
        return null
    }

    return (
        <Form
            labelWrap
            form={form}
            initialValues={systemSettings}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FROM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('system:core')}
            </Typography.Title>
            <Checkbox
                disabled={readOnlyFields['dispatchingValidationEnabled']}
                label={t('system:dispatching_validation')}
                name="dispatchingValidationEnabled"
            />
            <Checkbox
                disabled={readOnlyFields['autoCompile']}
                label={t('system:verify_on_edit')}
                name="autoCompile"
            />
            <Divider />
            <Typography.Title level={4}>
                {t('system:testing')}
            </Typography.Title>
            <InputNumber
                disabled={readOnlyFields['testRunThreadCount']}
                label={t('system:thread_number_for_tests')}
                name="testRunThreadCount"
            />
            <Divider />
            <Typography.Title level={4}>
                {t('system:user_workspace')}
            </Typography.Title>
            <Input
                disabled={readOnlyFields['userWorkspaceHome']}
                label={t('system:workspace_directory')}
                name="userWorkspaceHome"
            />
            <Divider />
            <Typography.Title level={4}>
                {t('system:history')}
            </Typography.Title>
            <InputNumber
                disabled={readOnlyFields['projectHistoryCount']}
                label={t('system:maximum_count_of_changes')}
                name="projectHistoryCount"
            />
            <Row justify="end">
                <Button onClick={showDeleteAllHistoryConfirm}>{t('system:clear_all_history')}</Button>
            </Row>
            <Divider />
            <Typography.Title level={4}>
                {t('system:other')}
            </Typography.Title>
            <Checkbox
                disabled={readOnlyFields['updateSystemProperties']}
                label={t('system:update_table_properties')}
                name="updateSystemProperties"
            />
            <Input
                disabled={readOnlyFields['datePattern']}
                label={t('system:date_format')}
                name="datePattern"
            />
            <Input
                disabled={readOnlyFields['timeFormat']}
                label={t('system:time_format')}
                name="timeFormat"
            />
            <Row justify="end">
                <Button onClick={showApplyConfirm} type="primary">
                    {t('common:btn.apply')}
                </Button>
            </Row>
            <Divider />
            <Typography.Title level={4} style={{ color: 'red' }}>
                {t('system:reset_settings')}
            </Typography.Title>
            <p>
                <WarningFilled style={{ color: 'red' }} />
                <Trans components={[<b style={{ color: 'red' }} />]} i18nKey="system:restore_defaults_warning" />
            </p>
            <Row justify="end">
                <Button danger onClick={showRestoreDefaultsConfirm}>
                    {t('system:restore_defaults_and_restart')}
                </Button>
            </Row>
        </Form>
    )
}

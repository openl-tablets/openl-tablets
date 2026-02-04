import { Button, Divider, Form, Typography, Row, Modal, notification } from 'antd'
import React, { useEffect } from 'react'
import { WarningFilled } from '@ant-design/icons'
import { Trans, useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { Input, InputNumber, Checkbox, InputPassword } from '../components'
import { WIDTH_OF_FORM_LABEL } from '../constants'

interface SystemSettings {
    autoCompile: boolean
    datePattern: string
    dispatchingValidationEnabled: boolean
    projectHistoryCount: number
    testRunThreadCount: number
    timeFormat: string
    updateSystemProperties: boolean
    db: {
        maximumPoolSize: number
        password: string
        url: string
        user: string
    }
}

export const System: React.FC = () => {
    const { t } = useTranslation()
    const [systemSettings, setSystemSettings] = React.useState<SystemSettings | undefined>()
    const [form] = Form.useForm()

    const fetchSystemSettings = async () => {
        // Fetch system settings from the server
        const response = await apiCall('/admin/settings/system')
        setSystemSettings(response)
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
            method: 'PATCH',
            // method: 'POST',
            headers: {
                'Content-Type': 'application/merge-patch+json',
                // 'Content-Type': 'application/json',
            },
            body: JSON.stringify(values),
        }, true).then(() => {
            window.location.reload()
        }).catch(e => {
            notification.error({ message: e.toString() })
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
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Divider titlePlacement="start">{t('system:core')}</Divider>
            <Checkbox
                label={t('system:dispatching_validation')}
                name="dispatchingValidationEnabled"
            />
            <Checkbox
                label={t('system:verify_on_edit')}
                name="autoCompile"
            />
            <Divider titlePlacement="start">{t('system:testing')}</Divider>
            <InputNumber
                label={t('system:thread_number_for_tests')}
                name="testRunThreadCount"
            />
            <Divider titlePlacement="start">{t('system:history')}</Divider>
            <InputNumber
                label={t('system:maximum_count_of_changes')}
                name="projectHistoryCount"
            />
            <Row justify="end">
                <Button onClick={showDeleteAllHistoryConfirm}>{t('system:clear_all_history')}</Button>
            </Row>
            <Divider titlePlacement="start">{t('system:other')}</Divider>
            <Checkbox
                label={t('system:update_table_properties')}
                name="updateSystemProperties"
            />
            <Input
                label={t('system:date_format')}
                name="datePattern"
            />
            <Input
                label={t('system:time_format')}
                name="timeFormat"
            />
            <Divider titlePlacement="start">{t('system:database_configuration')}</Divider>
            <Typography.Paragraph>
                {t('system:database_configuration_info')}
            </Typography.Paragraph>
            <Input label={t('system:db_url')} name={['db', 'url']} />
            <Input label={t('system:login')} name={['db', 'user']} />
            <InputPassword label={t('system:password')} name={['db', 'password']} />
            <InputNumber label={t('system:maximum_pool_size')} name={['db', 'maximumPoolSize']} />
            <Row justify="end">
                <Button onClick={showApplyConfirm} type="primary">
                    {t('common:btn.apply')}
                </Button>
            </Row>
            <Divider style={{ color: 'red' }} titlePlacement="start">{t('system:reset_settings')}</Divider>
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

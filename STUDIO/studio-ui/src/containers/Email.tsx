import React, { useEffect, useState } from 'react'
import { Button, Form, Typography, Row, notification, Modal } from 'antd'
import { Input, InputPassword, Checkbox } from '../components'
import { useTranslation } from 'react-i18next'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { apiCall } from '../services'

interface EmailSettings {
    password: string // empty string
    url: string
    username: string
}

export const Email: React.FC = () => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const isActive = Form.useWatch('isActive', form)
    const [loading, setLoading] = useState(false)

    const fetchEmailSettings = async () => {
        const response: EmailSettings = await apiCall('/admin/settings/mail')
        form.setFieldsValue(response)
        if (response.url || response.username) {
            form.setFieldValue('isActive', true)
        }
    }

    const onFinish = async (values: EmailSettings & { isActive: boolean }) => {
        setLoading(true)
        const { isActive, ...restValues } = values
        if (!isActive) {
            await apiCall('/admin/settings/mail', {
                method: 'DELETE'
            }).then(() => {
                notification.success({
                    message: t('email:email_server_configuration'),
                    description: t('email:email_server_configuration_deleted'),
                })
            })
        } else {
            await apiCall('/admin/settings/mail', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(restValues)
            }).then(() => {
                notification.success({
                    message: t('email:email_server_configuration'),
                    description: t('email:email_server_configuration_saved'),
                })
            })
        }
        setTimeout(() => {
            setLoading(false)
            window.location.reload()
        }, 1000)
    }

    const showChangeEmailConfirm = () => {
        Modal.confirm({
            title: t('email:confirm_change_email_settings'),
            content: t('email:confirm_change_email_settings_message'),
            onOk:  () => {
                form.submit()
            }
        })
    }

    useEffect(() => {
        fetchEmailSettings()
    }, [])

    return (
        <Form
            labelWrap
            form={form}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('email:email_server_configuration')}
            </Typography.Title>
            <Checkbox
                label={t('email:enable_verification')}
                name="isActive"
            />
            {isActive && (
                <>
                    <Input
                        label={t('email:url')}
                        name="url"
                        rules={[{
                            required: true,
                            message: t('common:validation.required')
                        }]}
                    />
                    <Input
                        label={t('email:username')}
                        name="username"
                        rules={[{
                            required: true,
                            message: t('common:validation.required')
                        }]}
                    />
                    <InputPassword
                        label={t('email:password')}
                        name="password"
                        rules={[{
                            required: true,
                            message: t('common:validation.required')
                        }]}
                    />
                </>
            )}
            <Row justify="end">
                <Button
                    loading={loading}
                    onClick={showChangeEmailConfirm}
                    type="primary"
                >
                    {t('common:btn.apply')}
                </Button>
            </Row>
        </Form>
    )
}

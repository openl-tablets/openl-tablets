import React, { useEffect, useState } from 'react'
import { App, Button, Form, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { Input, InputPassword } from '../components'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { apiCall } from '../services'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from '../services/apiCall'

const SETTINGS_URL = '/admin/settings/maven'

/** The settings as the server answers them: a value alone, or wrapped with its flags when it is read-only or secret. */
interface MavenRepositorySettings {
    url?: unknown
    username?: unknown
    password?: unknown
}

/** Whether Maven Resolver reads the address: an http, https or file URL, or nothing at all. */
const isRepositoryUrl = (value: unknown): boolean => {
    if (typeof value !== 'string' || value.trim() === '') {
        return true
    }
    try {
        return ['http:', 'https:', 'file:'].includes(new URL(value.trim()).protocol)
    } catch {
        return false
    }
}

/**
 * The Maven repository OpenL Studio downloads the dependencies a rules.xml names by Maven coordinates from.
 * An empty URL downloads nothing.
 */
export const Maven: React.FC = () => {
    const { modal, notification } = App.useApp()
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        (apiCall(SETTINGS_URL, undefined, LOCAL_LOAD_API_OPTIONS) as Promise<MavenRepositorySettings>)
            .then(settings => form.setFieldsValue(settings))
            .catch((error: unknown) => notifyLoadFailure(t('maven:load_failed'), error))
    }, [form, t])

    const onFinish = async (values: MavenRepositorySettings) => {
        setLoading(true)
        try {
            await apiCall(SETTINGS_URL, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/merge-patch+json' },
                body: JSON.stringify(values),
            }, LOCAL_LOAD_API_OPTIONS)
            notification.success({ title: t('maven:title'), description: t('maven:saved') })
            // Saving the settings restarts the application
            setTimeout(() => window.location.reload(), 1000)
        } catch (error) {
            notifyLoadFailure(t('maven:save_failed'), error)
            setLoading(false)
        }
    }

    const confirmApply = () => {
        modal.confirm({
            title: t('maven:confirm_apply'),
            content: t('maven:confirm_apply_message'),
            onOk: () => form.submit(),
        })
    }

    return (
        <Form
            labelWrap
            form={form}
            labelAlign="end"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={onFinish}
            wrapperCol={{ flex: 1 }}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>
                {t('maven:title')}
            </Typography.Title>
            <Typography.Paragraph type="secondary">
                {t('maven:description')}
            </Typography.Paragraph>
            <Input
                label={t('maven:url')}
                name="url"
                placeholder="https://repo.maven.apache.org/maven2"
                rules={[{
                    validator: (_, value: unknown) => isRepositoryUrl(value)
                        ? Promise.resolve()
                        : Promise.reject(new Error(t('maven:url_invalid'))),
                }]}
            />
            <Input autoComplete="off" label={t('maven:username')} name="username" />
            <InputPassword autoComplete="new-password" label={t('maven:password')} name="password" />
            <Row justify="end">
                <Button loading={loading} onClick={confirmApply} type="primary">
                    {t('common:btn.apply')}
                </Button>
            </Row>
        </Form>
    )
}

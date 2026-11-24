import React, { useCallback, useEffect, useState } from 'react'
import { App, Button, Divider, Flex, Form, Row, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { Input, InputNumber, InputPassword } from '../components'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { apiCall } from '../services'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from '../services/apiCall'
import { formatMegabytes } from '../utils/fileSize'

const SETTINGS_URL = '/admin/settings/maven'
const CACHE_URL = `${SETTINGS_URL}/cache`

/** The settings as the server answers them: a value alone, or wrapped with its flags when it is read-only or secret. */
interface MavenRepositorySettings {
    url?: unknown
    username?: unknown
    password?: unknown
    cleanupPeriod?: unknown
    cleanupThreshold?: unknown
}

/** The local Maven repository, as the server answers it. */
interface MavenCache {
    size: number
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
 * The Maven repository OpenL Studio downloads the dependencies a rules.xml names by Maven coordinates from, how the
 * downloaded files are cleaned up, and the local repository they are kept in, which can be emptied.
 * An empty URL downloads nothing.
 */
export const Maven: React.FC = () => {
    const { modal, notification } = App.useApp()
    const { t, i18n } = useTranslation()
    const locale = i18n.resolvedLanguage ?? i18n.language
    const [form] = Form.useForm()
    const [loading, setLoading] = useState(false)
    const [cacheSize, setCacheSize] = useState<number>()
    const [deleting, setDeleting] = useState(false)

    useEffect(() => {
        (apiCall(SETTINGS_URL, undefined, LOCAL_LOAD_API_OPTIONS) as Promise<MavenRepositorySettings>)
            .then(settings => form.setFieldsValue(settings))
            .catch((error: unknown) => notifyLoadFailure(t('maven:load_failed'), error))
    }, [form, t])

    const readCacheSize = useCallback(() => {
        (apiCall(CACHE_URL, undefined, LOCAL_LOAD_API_OPTIONS) as Promise<MavenCache>)
            .then(cache => setCacheSize(cache.size))
            .catch((error: unknown) => notifyLoadFailure(t('maven:cache_load_failed'), error))
    }, [t])

    useEffect(readCacheSize, [readCacheSize])

    const deleteCache = async () => {
        setDeleting(true)
        try {
            await apiCall(CACHE_URL, { method: 'DELETE' }, LOCAL_LOAD_API_OPTIONS)
            notification.success({ title: t('maven:cache_title'), description: t('maven:cache_deleted') })
            readCacheSize()
        } catch (error) {
            notifyLoadFailure(t('maven:cache_delete_failed'), error)
        } finally {
            setDeleting(false)
        }
    }

    const confirmDeleteCache = () => {
        modal.confirm({
            title: t('maven:confirm_delete_cache'),
            content: t('maven:confirm_delete_cache_message'),
            okButtonProps: { danger: true },
            okText: t('maven:delete_cache'),
            onOk: deleteCache,
        })
    }

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
            <InputNumber
                label={t('maven:cleanup_period')}
                name="cleanupPeriod"
                rules={[{ type: 'integer', min: 0, message: t('maven:not_negative') }]}
                tooltip={t('maven:cleanup_period_tooltip')}
            />
            <InputNumber
                label={t('maven:cleanup_threshold')}
                name="cleanupThreshold"
                rules={[{ type: 'integer', min: 0, message: t('maven:not_negative') }]}
                tooltip={t('maven:cleanup_threshold_tooltip')}
            />
            <Row justify="end">
                <Button loading={loading} onClick={confirmApply} type="primary">
                    {t('common:btn.apply')}
                </Button>
            </Row>
            <Divider />
            <Typography.Title level={5} style={{ marginTop: 0 }}>
                {t('maven:cache_title')}
            </Typography.Title>
            <Flex align="center" justify="space-between">
                <Typography.Text data-testid="maven-cache-size">
                    {cacheSize === undefined
                        ? t('maven:cache_size_unknown')
                        : t('maven:cache_size', { size: formatMegabytes(cacheSize, locale) })}
                </Typography.Text>
                <Button danger loading={deleting} onClick={confirmDeleteCache}>
                    {t('maven:delete_cache')}
                </Button>
            </Flex>
        </Form>
    )
}

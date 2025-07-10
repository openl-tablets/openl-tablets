import React, { useMemo, useState } from 'react'
import { Button, Divider, Form, notification, Row } from 'antd'
import { Checkbox, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { useUserStore } from 'store'

export const UserSettings: React.FC = () => {
    const { t } = useTranslation()
    const { userProfile, fetchUserProfile } = useUserStore()

    const treeViewOptions = useMemo(() =>
        userProfile?.profiles?.map((profile) => ({
            value: profile.name,
            label: profile.displayName,
        })) || [],
    [userProfile])

    const testsPerPageOptions = [
        {
            value: 1,
            label: '1',
        },
        {
            value: 5,
            label: '5',
        },
        {
            value: 20,
            label: '20',
        },
        {
            value: -1,
            label: 'All',
        },
    ]

    const [saving, setSaving] = useState(false)

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { administrator, profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }

        try {
            setSaving(true)
            const body = {
                ...restUserProfile,
                ...values,
                changePassword: {
                    newPassword: '',
                    currentPassword: '',
                    confirmPassword: '',
                }
            }
            await apiCall('/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body)
            })
            await fetchUserProfile()
            notification.success({ message: t('user:user_settings_updated_successfully') })
        } catch (error) {
            console.error('error', error)
        } finally {
            setSaving(false)
        }
    }

    return (
        <Form
            labelWrap
            initialValues={userProfile}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={handleSubmit}
            wrapperCol={{ flex: 1 }}
        >
            <Divider orientation="left">{t('user:settings.table_settings')}</Divider>
            <Checkbox label={t('user:settings.show_header')} name="showHeader" />
            <Checkbox label={t('user:settings.show_formulas')} name="showFormulas" />
            <Select label={t('user:settings.default_order')} name="treeView" options={treeViewOptions} />
            <Divider orientation="left">{t('user:settings.testing_settings')}</Divider>
            <Select label={t('user:settings.tests_per_page')} name="testsPerPage" options={testsPerPageOptions} />
            <Checkbox label={t('user:settings.failures_only')} name="testsFailuresOnly" />
            <Checkbox label={t('user:settings.compound_result')} name="showComplexResult" />
            <Divider orientation="left">{t('user:settings.trace_settings')}</Divider>
            <Checkbox label={t('user:settings.show_numbers_without_formatting')} name="showRealNumbers" />
            <Row justify="end">
                <Button
                    key="submit"
                    htmlType="submit"
                    loading={saving}
                    style={{ marginTop: 20 }}
                    type="primary"
                >
                    {t('common:btn.save')}
                </Button>
            </Row>
        </Form>
    )
}

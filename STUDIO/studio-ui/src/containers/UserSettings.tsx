import React, { useEffect, useState } from 'react'
import { App, Button, Divider, Form, Row } from 'antd'
import { Checkbox, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { LOCAL_LOAD_API_OPTIONS, notifyLoadFailure } from '../services/apiCall'
import { UserProfileFormFields } from '../types/user'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { useUserStore } from 'store'
import { changedValues } from 'utils/userProfile'

export const UserSettings: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { userProfile: profile, fetchUserProfile } = useUserStore()
    const [form] = Form.useForm()

    // The fields follow every read of the profile, so they show what a save compares them with.
    useEffect(() => {
        if (profile) {
            form.setFieldsValue(profile)
        }
    }, [form, profile])

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
        try {
            setSaving(true)
            // Only what was changed here: the rest of the profile keeps what is stored, whatever was saved meanwhile.
            const body = changedValues(values, profile)
            // A rejected save is reported below, never as saved.
            await apiCall('/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body)
            }, LOCAL_LOAD_API_OPTIONS)
            await fetchUserProfile()
            notification.success({ title: t('users:user_settings_updated_successfully') })
        } catch (error) {
            notifyLoadFailure(t('users:user_settings_save_failed'), error)
        } finally {
            setSaving(false)
        }
    }

    return (
        <Form
            labelWrap
            form={form}
            {...(profile && { initialValues: profile })}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={handleSubmit}
            wrapperCol={{ flex: 1 }}
        >
            <Divider titlePlacement="start">{t('users:settings.table_settings')}</Divider>
            <Checkbox label={t('users:settings.show_header')} name="showHeader" />
            <Checkbox label={t('users:settings.show_formulas')} name="showFormulas" />
            <Checkbox
                label={t('users:settings.show_excel_formatting')}
                name="showExcelFormatting"
                tooltip={t('users:settings.show_excel_formatting_info')}
            />
            <Divider titlePlacement="start">{t('users:settings.testing_settings')}</Divider>
            <Select label={t('users:settings.tests_per_page')} name="testsPerPage" options={testsPerPageOptions} />
            <Checkbox label={t('users:settings.failures_only')} name="testsFailuresOnly" />
            <Checkbox label={t('users:settings.compound_result')} name="showComplexResult" />
            <Divider titlePlacement="start">{t('users:settings.trace_settings')}</Divider>
            <Checkbox label={t('users:settings.show_numbers_without_formatting')} name="showRealNumbers" />
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

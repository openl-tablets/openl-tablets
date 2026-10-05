import React, { useEffect, useMemo, useState } from 'react'
import { App, Button, Divider, Form, Row } from 'antd'
import { Checkbox, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'
import { WIDTH_OF_FORM_LABEL } from '../constants'
import { useUserStore } from 'store'
import { changedValues } from 'utils/userProfile'
import { toThemeOptions, useTableThemes } from '../hooks/useTableThemes'
import { useAppTheme } from '../providers/AppThemeProvider'

/** The table theme a profile names when its tables are drawn with the formatting of the Excel file. */
const EXCEL_FORMATTING = ''

export const UserSettings: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { userProfile, fetchUserProfile } = useUserStore()
    const { tablesFollowTheme, setTablesFollowTheme } = useAppTheme()
    const [form] = Form.useForm()
    const tableThemes = useTableThemes()
    // A profile that names no theme draws its tables with the formatting of the Excel file. Built once per read of
    // the profile: the fields are set from it again whenever it is read again.
    const profile = useMemo(() => userProfile && {
        ...userProfile,
        tableTheme: userProfile.tableTheme ?? EXCEL_FORMATTING,
    }, [userProfile])

    // The fields follow every read of the profile, so they show what a save compares them with.
    useEffect(() => {
        if (profile) {
            form.setFieldsValue(profile)
        }
    }, [form, profile])

    // The choice the browser remembers is no part of the profile, so its field follows the choice alone: a save
    // that changes it sets no other field back to the profile read before the save.
    useEffect(() => {
        form.setFieldsValue({ overrideWithStudioTheme: tablesFollowTheme })
    }, [form, tablesFollowTheme])

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

    const handleSubmit = async ({ overrideWithStudioTheme, ...values }: UserProfileFormFields) => {
        try {
            setSaving(true)
            // Only what was changed here: the rest of the profile keeps what is stored, whatever was saved meanwhile.
            const body = changedValues(values, profile)
            await apiCall('/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body)
            })
            // Kept by the browser, as the Studio theme is, once the profile is saved with it.
            setTablesFollowTheme(overrideWithStudioTheme === true)
            await fetchUserProfile()
            notification.success({ title: t('users:user_settings_updated_successfully') })
        } catch (error) {
            notification.error({ title: error instanceof Error ? error.message : t('common:error') })
        } finally {
            setSaving(false)
        }
    }

    return (
        <Form
            labelWrap
            form={form}
            {...(profile && { initialValues: { ...profile, overrideWithStudioTheme: tablesFollowTheme } })}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={handleSubmit}
            wrapperCol={{ flex: 1 }}
        >
            <Divider titlePlacement="start">{t('users:settings.table_settings')}</Divider>
            <Checkbox label={t('users:settings.show_header')} name="showHeader" />
            <Checkbox label={t('users:settings.show_formulas')} name="showFormulas" />
            <Select
                label={t('users:settings.table_theme')}
                name="tableTheme"
                tooltip={t('users:settings.table_theme_info')}
                options={[
                    { value: EXCEL_FORMATTING, label: t('users:settings.excel_formatting') },
                    ...toThemeOptions(tableThemes),
                ]}
            />
            <Checkbox
                label={t('users:settings.override_with_studio_theme')}
                name="overrideWithStudioTheme"
                tooltip={t('users:settings.override_with_studio_theme_info')}
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

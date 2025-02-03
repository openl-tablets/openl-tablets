import React, { useContext, useMemo } from 'react'
import { Button, Divider, Form, Row } from 'antd'
import { Checkbox, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { UserContext } from '../contexts/User'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'

export const UserSettings: React.FC = () => {
    const { t } = useTranslation()
    const { userProfile, loadUserProfile } = useContext(UserContext)

    const treeViewOptions = useMemo(() =>
        userProfile.profiles?.map((profile) => ({
            value: profile.name,
            label: profile.displayName,
        })) || [],
    [userProfile.profiles])

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

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }
        // const { profiles, externalFlags, username, ...requestBody } = { ...values }

        try {
            const body = {
                ...restUserProfile,
                ...values,
                changePassword: {
                    newPassword: '',
                    currentPassword: '',
                    confirmPassword: '',
                }
            }
            const response = await apiCall('/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body)
            })
            loadUserProfile()
            console.log('response', response)
        } catch (error) {
            console.error('error', error)
        }
    }

    return (
        <Form
            initialValues={userProfile}
            onFinish={handleSubmit}
        >
            <Divider plain orientation="left">{t('user:settings.table_settings')}</Divider>
            <Checkbox label={t('user:settings.show_header')} name="showHeader" />
            <Checkbox label={t('user:settings.show_formulas')} name="showFormulas" />
            <Select label={t('user:settings.default_order')} name="treeView" options={treeViewOptions} />
            <Divider plain orientation="left">{t('user:settings.testing_settings')}</Divider>
            <Select label={t('user:settings.tests_per_page')} name="testsPerPage" options={testsPerPageOptions} />
            <Checkbox label={t('user:settings.failures_only')} name="testsFailuresOnly" />
            <Checkbox label={t('user:settings.compound_result')} name="showComplexResult" />
            <Divider plain orientation="left">{t('user:settings.trace_settings')}</Divider>
            <Checkbox label={t('user:settings.show_numbers_without_formatting')} name="showRealNumbers" />
            <Row justify="end">
                <Button
                    key="submit"
                    htmlType="submit"
                    style={{ marginTop: 20 }}
                    type="primary"
                >
                    {t('common:btn.save')}
                </Button>
            </Row>
        </Form>
    )
}

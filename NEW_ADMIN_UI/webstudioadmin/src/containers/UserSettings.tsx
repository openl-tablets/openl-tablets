import React from 'react'
import { Form } from 'react-final-form'
import { Button, Form as AForm, Row, Typography } from 'antd'
import { Checkbox, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { RootState, useAppDispatch, useAppSelector } from 'store'
import { updateUserProfile } from './user/userSlice'

export const UserSettings: React.FC = () => {
    const { t } = useTranslation()
    const dispatch = useAppDispatch()

    const profile = useAppSelector((state: RootState ) => state.user.profile)

    const treeViewOptions = useAppSelector((state: RootState ) => state.user.profile.profiles?.map((profile) => ({
        value: profile.name,
        label: profile.displayName,
    })) || [])

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

    const handleSubmit = async (values: typeof profile) => {
        const { profiles, externalFlags, username, ...requestBody } = { ...values }
        dispatch(updateUserProfile({
            ...requestBody,
            changePassword: {
                newPassword: '',
                currentPassword: '',
                confirmPassword: '',
            }
        }))
    }

    return (
        <div>
            <Form
                initialValues={profile}
                onSubmit={handleSubmit}
            >
                {({ handleSubmit, form }) => {
                    return (
                        <AForm onFinish={handleSubmit}>
                            <Typography.Title level={5} style={{ marginTop: 0 }}>{t('user:settings.table_settings')}</Typography.Title>
                            <Checkbox label={t('user:settings.show_header')} name="showHeader" />
                            <Checkbox label={t('user:settings.show_formulas')} name="showFormulas" />
                            <Select label={t('user:settings.default_order')} name="treeView" options={treeViewOptions} />
                            <Typography.Title level={5}>{t('user:settings.testing_settings')}</Typography.Title>
                            <Select label={t('user:settings.tests_per_page')} name="testsPerPage" options={testsPerPageOptions} />
                            <Checkbox label={t('user:settings.failures_only')} name="testsFailuresOnly" />
                            <Checkbox label={t('user:settings.compound_result')} name="showComplexResult" />
                            <Typography.Title level={5}>{t('user:settings.trace_settings')}</Typography.Title>
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
                        </AForm>
                    )
                }}
            </Form>
        </div>
    )
}

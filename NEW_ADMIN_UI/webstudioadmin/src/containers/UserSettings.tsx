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
                            <br />
                            <Typography.Title level={5}>Table Setting</Typography.Title>
                            <Checkbox label="Show Header" name="showHeader" />
                            <Checkbox label="Show Formulas" name="showFormulas" />
                            <Select label="Default Order" name="treeView" options={treeViewOptions} />
                            <Typography.Title level={5}>Testing Settings</Typography.Title>
                            <Select label="Tests per page" name="testsPerPage" options={testsPerPageOptions} />
                            <Checkbox label="Failures Only" name="testsFailuresOnly" />
                            <Checkbox label="Compound Result" name="showComplexResult" />
                            <Typography.Title level={5}>Trace Settings</Typography.Title>
                            <Checkbox label="Show numbers without formatting" name="showRealNumbers" />
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

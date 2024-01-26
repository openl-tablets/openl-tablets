import React, { useMemo, useState } from 'react'
import { Form, FormSpy } from 'react-final-form'
import { Button, Col, Form as AForm, Row, Typography } from 'antd'
import { Input, Select } from '../components'
import { useTranslation } from 'react-i18next'
import { DISPLAY_USER_NAME_FIRST_LAST, DISPLAY_USER_NAME_LAST_FIRST, DISPLAY_USER_NAME_OTHER } from 'constants/'
import { FormApi } from 'final-form'
import { useAppDispatch, RootState, useAppSelector } from 'store'
import { updateUserProfile } from './user/userSlice'

export const UserProfile: React.FC = () => {
    const { t } = useTranslation()
    const dispatch = useAppDispatch()
    const [ isDisplayNameFieldDisabled, setIsDisplayNameFieldDisabled ] = useState<boolean>(true)

    const profile = useAppSelector((state: RootState ) => state.user.profile)

    const handleSubmit = async (values: typeof profile) => {
        const { profiles, externalFlags, displayNameSelect, username, changePassword, ...requestBody } = { ...values }
        const { newPassword = '', currentPassword = '', confirmPassword = '' }  = changePassword || {}
        dispatch(updateUserProfile({
            ...requestBody,
            changePassword: {
                newPassword,
                currentPassword,
                confirmPassword,
            }
        }))
    }

    const displayNameOptions = useMemo(() => (
        [
            {
                value: DISPLAY_USER_NAME_FIRST_LAST,
                label: t('users:first_last'),
            },
            {
                value: DISPLAY_USER_NAME_LAST_FIRST,
                label: t('users:last_first'),
            },
            {
                value: DISPLAY_USER_NAME_OTHER,
                label: t('users:other'),
            },
        ]
    ), [ t ])

    const displayNameSetter = (values: any, form: FormApi) => {
        if (values.displayNameSelect === DISPLAY_USER_NAME_OTHER) {
            setIsDisplayNameFieldDisabled(false)
        } else {
            if (values.displayNameSelect === DISPLAY_USER_NAME_FIRST_LAST) {
                form.change('displayName', `${values.firstName} ${values.lastName}`)
            } else {
                form.change('displayName', `${values.lastName} ${values.firstName}`)
            }
            setIsDisplayNameFieldDisabled(true)
        }

        return null
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
                            <Typography.Title level={5}>Account</Typography.Title>
                            <Input disabled label={t('users:edit_modal.username')} name="username" />
                            <Input label={t('users:edit_modal.email')} name="email" />
                            <Typography.Title level={5}>Name</Typography.Title>
                            <Input label={t('users:edit_modal.first_name')} name="firstName" />
                            <Input label={t('users:edit_modal.last_name')} name="lastName" />
                            <Row gutter={16} justify="space-between">
                                <Col span={8}>
                                    <Select
                                        formItemStyle={{ margin: 0 }}
                                        label={t('users:edit_modal.display_name')}
                                        name="displayNameSelect"
                                        options={displayNameOptions}
                                    />
                                </Col>
                                <Col span={16}>
                                    <Input
                                        disabled={isDisplayNameFieldDisabled}
                                        formItemStyle={{ margin: 0 }}
                                        label=""
                                        name="displayName"
                                    />
                                </Col>
                            </Row>
                            <Typography.Title level={5}>Change Password</Typography.Title>
                            <Input label={t('user:profile.current_password')} name="changePassword.currentPassword" type="password" />
                            <Input label={t('user:profile.new_password')} name="changePassword.newPassword" type="password" />
                            <Input label={t('user:profile.confirm_password')} name="changePassword.confirmPassword" type="password" />
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
                            <FormSpy subscription={{ values: true }}>
                                {({ values }) => displayNameSetter(values, form)}
                            </FormSpy>
                        </AForm>
                    )
                }}
            </Form>
        </div>
    )
}

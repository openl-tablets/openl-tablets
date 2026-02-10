import React, { useContext, useEffect, useMemo, useState } from 'react'
import { Button, Divider, Form, notification, Row } from 'antd'
import { InputPassword } from '../components'
import { useTranslation } from 'react-i18next'
import { DisplayUserName, WIDTH_OF_FORM_LABEL } from 'constants/'
import { SystemContext } from '../contexts'
import { UserDetailsTab } from './users/UserDatailsTab'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'
import { useUserStore } from 'store'
import { useIsFormChanged } from '../hooks/useIsFormChanged'

export const UserProfile: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(SystemContext)
    const { userProfile, fetchUserProfile } = useUserStore()
    const [saving, setSaving] = useState(false)
    const [form] = Form.useForm()

    useEffect(() => {
        fetchUserProfile()
    }, [])

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { administrator, profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }
        const { username: _, displayNameSelect, changePassword, ...restFormValues } = values
        const { newPassword = '', currentPassword = '', confirmPassword = '' }  = changePassword || {}

        // Show verification warning only when user changed an existing email to another (not when adding email to empty field)
        const emailChanged = userProfile?.email !== values.email
        const newEmailNonEmpty = Boolean(values.email?.trim())
        const hadEmailBefore = Boolean(userProfile?.email?.trim())

        try {
            setSaving(true)
            const body = {
                ...restUserProfile,
                ...restFormValues,
                changePassword: {
                    newPassword,
                    currentPassword,
                    confirmPassword,
                }
            }

            await apiCall(
                '/users/profile',
                {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(body)
                },
                { throwError: true }
            )
            await fetchUserProfile()
            notification.success({ message: t('users:user_profile_updated_successfully') })
            if (emailChanged && newEmailNonEmpty && hadEmailBefore) {
                notification.warning({
                    message: t('users:email_verification_warning'),
                    duration: 0,
                    key: 'email-verification-warning',
                })
            }
        } catch (error) {
            console.error('error', error)
            if (error instanceof Error) {
                notification.error({ message: error.message })
            }
        } finally {
            setSaving(false)
        }
    }

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            const firstName = userProfile?.firstName || ''
            const lastName = userProfile?.lastName || ''
            if (userProfile?.displayName === `${firstName} ${lastName}`.trim()) {
                return DisplayUserName.FirstLast
            }
            if (userProfile?.displayName === `${lastName} ${firstName}`.trim()) {
                return DisplayUserName.LastFirst
            }
            return DisplayUserName.Other
        }

        return {
            username: userProfile?.username,
            email: userProfile?.email,
            firstName: userProfile?.firstName || '',
            lastName: userProfile?.lastName || '',
            displayName: userProfile?.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
        }
    }, [userProfile])

    const isFormChanged = useIsFormChanged({ form, initialValues })

    return (
        <Form
            labelWrap
            form={form}
            initialValues={initialValues}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={handleSubmit}
            wrapperCol={{ flex: 1 }}
        >
            <UserDetailsTab displayPasswordField={false} externalFlags={userProfile?.externalFlags} isNewUser={false} showResendVerification={true} userProfile={userProfile} />
            {!isExternalAuthSystem && (
                <>
                    <Divider titlePlacement="start">{t('users:edit_modal.change_password')}</Divider>
                    <InputPassword label={t('users:edit_modal.current_password')} name={['changePassword', 'currentPassword']} />
                    <InputPassword label={t('users:edit_modal.new_password')} name={['changePassword', 'newPassword']} />
                    <InputPassword label={t('users:edit_modal.confirm_password')} name={['changePassword', 'confirmPassword']} />
                </>
            )}
            <Row justify="end">
                <Button
                    key="submit"
                    disabled={!isFormChanged}
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

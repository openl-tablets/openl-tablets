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

export const UserProfile: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(SystemContext)
    const { userProfile, fetchUserProfile } = useUserStore()
    const [saving, setSaving] = useState(false)

    useEffect(() => {
        fetchUserProfile()
    }, [])

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { administrator, profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }
        const { username: _, displayNameSelect, changePassword, ...restFormValues } = values
        const { newPassword = '', currentPassword = '', confirmPassword = '' }  = changePassword || {}

        // Check if email was changed
        const emailChanged = userProfile?.email !== values.email

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

            await apiCall('/users/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(body)
            })
            await fetchUserProfile()
            notification.success({ message: t('user:user_profile_updated_successfully') })
            if (emailChanged) {
                notification.warning({
                    message: t('users:email_verification_warning'),
                    duration: 0,
                    key: 'email-verification-warning',
                })
            }
        } catch (error) {
            console.error('error', error)
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

    return (
        <Form
            labelWrap
            initialValues={initialValues}
            labelAlign="right"
            labelCol={{ flex: WIDTH_OF_FORM_LABEL }}
            onFinish={handleSubmit}
            wrapperCol={{ flex: 1 }}
        >
            <UserDetailsTab displayPasswordField={false} externalFlags={userProfile?.externalFlags} isNewUser={false} showResendVerification={true} userProfile={userProfile} />
            {!isExternalAuthSystem && (
                <>
                    <Divider orientation="left">{t('user:profile.change_password')}</Divider>
                    <InputPassword label={t('user:profile.current_password')} name={['changePassword', 'currentPassword']} />
                    <InputPassword label={t('user:profile.new_password')} name={['changePassword', 'newPassword']} />
                    <InputPassword label={t('user:profile.confirm_password')} name={['changePassword', 'confirmPassword']} />
                </>
            )}
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

import React, { useContext, useEffect, useMemo, useRef, useState } from 'react'
import { App, Button, Divider, Form, Row } from 'antd'
import { InputPassword } from '../components'
import { useTranslation } from 'react-i18next'
import { WIDTH_OF_FORM_LABEL } from 'constants/'
import { deriveDisplayNameMode } from 'utils/displayName'
import { changedValues } from 'utils/userProfile'
import { SystemUserMode } from '../constants/system'
import { SystemContext } from '../contexts'
import { UserDetailsTab } from './users/UserDatailsTab'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'
import { useUserStore } from 'store'
import { useIsFormChanged } from '../hooks/useIsFormChanged'

export const UserProfile: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { systemSettings } = useContext(SystemContext)
    const { userProfile, fetchUserProfile } = useUserStore()
    // Passwords exist only for internal user management (multi mode): single mode has no login,
    // external modes manage credentials on the identity provider side.
    const canChangePassword = systemSettings?.userMode === SystemUserMode.INTERNAL
    const [saving, setSaving] = useState(false)
    const [form] = Form.useForm()

    useEffect(() => {
        fetchUserProfile()
    }, [])

    const initialValues = useMemo(() => {
        return {
            username: userProfile?.username,
            email: userProfile?.email,
            firstName: userProfile?.firstName || '',
            lastName: userProfile?.lastName || '',
            displayName: userProfile?.displayName,
            displayNameSelect: deriveDisplayNameMode(userProfile ?? {}),
        }
    }, [userProfile])

    // Set before a save reads the profile back, when every field has to follow the profile read.
    const followAllFields = useRef(false)

    // The fields follow every read of the profile, so they show what a save compares them with. The read made on
    // opening the page reaches only the fields not edited yet, so it cannot undo what the user typed meanwhile.
    useEffect(() => {
        const followAll = followAllFields.current
        followAllFields.current = false
        form.setFields(Object.entries(initialValues)
            .filter(([name]) => followAll || !form.isFieldTouched(name))
            .map(([name, value]) => ({ name, value, touched: false })))
    }, [form, initialValues])

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { username: _, displayNameSelect, changePassword, ...restFormValues } = values
        const { newPassword = '', currentPassword = '', confirmPassword = '' } = changePassword || {}

        // Show verification warning only when user changed an existing email to another (not when adding email to empty field)
        const emailChanged = userProfile?.email !== values.email
        const newEmailNonEmpty = Boolean(values.email?.trim())
        const hadEmailBefore = Boolean(userProfile?.email?.trim())

        try {
            setSaving(true)
            // Only the details changed here: the rest of the profile, the settings included, keeps what is stored,
            // whatever was saved meanwhile.
            const body = {
                ...changedValues(restFormValues, initialValues),
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
            followAllFields.current = true
            await fetchUserProfile()
            notification.success({ title: t('users:user_profile_updated_successfully') })
            if (emailChanged && newEmailNonEmpty && hadEmailBefore && systemSettings?.supportedFeatures?.emailVerification) {
                notification.warning({
                    title: t('users:email_verification_warning'),
                    duration: 0,
                    key: 'email-verification-warning',
                })
            }
        } catch (error) {
            notification.error({ title: error instanceof Error ? error.message : t('common:error') })
        } finally {
            setSaving(false)
        }
    }

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
            <UserDetailsTab
                requireEmailAndDisplayName
                displayPasswordField={false}
                externalFlags={userProfile?.externalFlags}
                isNewUser={false}
                showResendVerification={true}
                userProfile={userProfile}
            />
            {canChangePassword && (
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

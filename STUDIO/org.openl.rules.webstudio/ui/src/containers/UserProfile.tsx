import React, { useContext, useMemo } from 'react'
import { Button, Divider, Form, Row } from 'antd'
import { InputPassword } from '../components'
import { useTranslation } from 'react-i18next'
import { DisplayUserName, WIDTH_OF_FORM_LABEL } from 'constants/'
import { SystemContext, UserContext } from '../contexts'
import { UserDetailsTab } from './users/UserDatailsTab'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'

export const UserProfile: React.FC = () => {
    const { t } = useTranslation()
    const { isExternalAuthSystem } = useContext(SystemContext)
    const { userProfile, loadUserProfile } = useContext(UserContext)

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { administrator, profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }
        const { username: _, displayNameSelect, changePassword, ...restFormValues } = values
        const { newPassword = '', currentPassword = '', confirmPassword = '' }  = changePassword || {}

        try {
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
            loadUserProfile()
        } catch (error) {
            console.error('error', error)
        }
    }

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            const firstName = userProfile.firstName || ''
            const lastName = userProfile.lastName || ''
            if (userProfile.displayName === `${firstName} ${lastName}`.trim()) {
                return DisplayUserName.FirstLast
            }
            if (userProfile.displayName === `${lastName} ${firstName}`.trim()) {
                return DisplayUserName.LastFirst
            }
            return DisplayUserName.Other
        }

        return {
            username: userProfile.username,
            email: userProfile.email,
            firstName: userProfile.firstName || '',
            lastName: userProfile.lastName || '',
            displayName: userProfile.displayName,
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
            <UserDetailsTab displayPasswordField={false} externalFlags={userProfile.externalFlags} isNewUser={false} />
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
                    style={{ marginTop: 20 }}
                    type="primary"
                >
                    {t('common:btn.save')}
                </Button>
            </Row>
        </Form>
    )
}

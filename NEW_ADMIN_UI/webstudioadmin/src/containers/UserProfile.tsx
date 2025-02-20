import React, { useContext, useMemo } from 'react'
import { Button, Divider, Form, Row } from 'antd'
import { Input } from '../components'
import { useTranslation } from 'react-i18next'
import { DisplayUserName } from 'constants/'
import { UserContext } from '../contexts/User'
import { UserDetailsTab } from './users/UserDatailsTab'
import { apiCall } from '../services'
import { UserProfileFormFields } from '../types/user'

export const UserProfile: React.FC = () => {
    const { t } = useTranslation()
    const { userProfile, loadUserProfile } = useContext(UserContext)

    const handleSubmit = async (values: UserProfileFormFields) => {
        const { profiles, externalFlags, username, ...restUserProfile } = { ...userProfile }
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

            const response = await apiCall('/users/profile', {
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
            initialValues={initialValues}
            onFinish={handleSubmit}
        >
            <UserDetailsTab displayPasswordField={false} isNewUser={false} />
            <Divider plain orientation="left">{t('user:profile.change_password')}</Divider>
            <Input label={t('user:profile.current_password')} name={['changePassword', 'currentPassword']} type="password" />
            <Input label={t('user:profile.new_password')} name={['changePassword', 'newPassword']} type="password" />
            <Input label={t('user:profile.confirm_password')} name={['changePassword', 'confirmPassword']} type="password" />
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

import React, { useMemo, useState } from 'react'
import { Button, Row, Form, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { DisplayUserName } from 'constants/'
import { UserDetailsTab } from './UserDatailsTab'
import { apiCall } from '../../services'
import { UserDetails } from '../../types/user'

interface EditUserProps {
    user: UserDetails
    updateUser: any // TODO add type
    closeModal: () => void;
    onAddUser: () => void;
}

interface FormValues {
    username: string
    email: string
    password: string
    firstName: string
    lastName: string
    displayName: string
    displayNameSelect: DisplayUserName
}

export interface UpdatedUserRequest {
    email: string
    displayName: string
    firstName: string
    lastName: string
    password: string
    groups: string[]
    // Attributes for new user
    username?: string
    internalPassword?: {
        password: string
    }
}

export const EditUserModal: React.FC<EditUserProps> = ({ updateUser, user, onAddUser, closeModal }) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [isNewUser, setIsNewUser] = useState(!user.username)
    const [userGroups, setUserGroups] = React.useState<string[]>([])

    const initialValues = useMemo(() => {
        const displayNameSelectInitialValue = () => {
            const firstName = user.firstName || ''
            const lastName = user.lastName || ''
            if (user.displayName === `${firstName} ${lastName}`.trim()) {
                return DisplayUserName.FirstLast
            }
            if (user.displayName === `${lastName} ${firstName}`.trim()) {
                return DisplayUserName.LastFirst
            }
            return DisplayUserName.Other
        }

        setUserGroups(user.userGroups?.map((group) => group.name) || [])

        return {
            username: user.username,
            email: user.email,
            password: null,
            firstName: user.firstName || '',
            lastName: user.lastName || '',
            displayName: user.displayName,
            displayNameSelect: displayNameSelectInitialValue(),
        }
    }, [user])

    const onSubmitUserModal = async (userData: any) => {
        const updatedUser: UpdatedUserRequest = {
            email: userData.email,
            displayName: userData.displayName,
            firstName: userData.firstName,
            lastName: userData.lastName,
            password: userData.password,
            groups: userGroups,
        }

        if (isNewUser) {
            updatedUser.username = userData.username
            updatedUser.internalPassword = {
                password: userData.password
            }
        }

        try {
            const url = isNewUser ? '/users' : `/users/${userData.username}`

            const response = await apiCall(url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(updatedUser),
            })

            if (response) {
                updateUser({ username: userData.username, ...updatedUser })
                if (isNewUser) {
                    setIsNewUser(false)
                    onAddUser()
                    closeModal()
                } else {
                    closeModal()
                }
            } else {
                throw new Error('Error updating user')
            }
        } catch (error) {
            console.error('Error updating group:', error)
        }
    }

    const submitButtonText = useMemo(() => {
        return isNewUser ? t('common:btn.create') : t('common:btn.save')
    }, [isNewUser, t])

    const title = useMemo(() => {
        return isNewUser ? t('users:add_user') : t('users:edit_user_details')
    }, [isNewUser, t])

    const onFinish = async (values: FormValues) => {
        await onSubmitUserModal(values)
    }

    return (
        <Form
            form={form}
            initialValues={initialValues}
            labelCol={{ sm: { span: 8 } }}
            onFinish={onFinish}
        >
            <Typography.Title level={4} style={{ marginTop: 0 }}>{title}</Typography.Title>
            <UserDetailsTab externalFlags={user.externalFlags} isNewUser={isNewUser} />
            <Row justify="end">
                <Button key="back" onClick={closeModal} style={{ marginRight: 20 }}>
                    {t('users:edit_modal.cancel')}
                </Button>
                <Button
                    key="submit"
                    htmlType="submit"
                    type="primary"
                >
                    {submitButtonText}
                </Button>
            </Row>
        </Form>
    )
}

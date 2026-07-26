import React, { useEffect, useMemo, useState } from 'react'
import { Alert, Form, Input, Modal, notification, Select, Space } from 'antd'
import { MailOutlined, UserOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../../services'
import { DisplayUserName, WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { deriveDisplayNameMode, formatDisplayName } from '../../utils/displayName'
import type { UserProfile } from '../../types/user'

interface UserProfileCompletionModalProps {
    open: boolean
    profile: UserProfile
    required?: boolean
    onSave: () => void | Promise<void>
    onCancel?: () => void
}

interface UserProfileFormValues {
    firstName: string
    lastName: string
    displayName: string
    email: string
    displayNameSelect: DisplayUserName
}

const getInitialDisplayNameMode = (profile: UserProfile): DisplayUserName => {
    if (profile.displayName?.trim()) {
        return deriveDisplayNameMode(profile)
    }
    if (profile.firstName?.trim() || profile.lastName?.trim()) {
        return DisplayUserName.FirstLast
    }
    return DisplayUserName.Other
}

const getInitialValues = (profile: UserProfile): UserProfileFormValues => ({
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    displayName: profile.displayName || '',
    email: profile.email || '',
    displayNameSelect: getInitialDisplayNameMode(profile),
})

export const UserProfileCompletionModal: React.FC<UserProfileCompletionModalProps> = ({
    open,
    profile,
    required = false,
    onSave,
    onCancel,
}) => {
    const { t } = useTranslation()
    const [form] = Form.useForm<UserProfileFormValues>()
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const initialValues = useMemo(() => getInitialValues(profile), [profile])

    const firstName = Form.useWatch('firstName', form) ?? initialValues.firstName
    const lastName = Form.useWatch('lastName', form) ?? initialValues.lastName
    const displayNameSelect = Form.useWatch('displayNameSelect', form) ?? initialValues.displayNameSelect

    useEffect(() => {
        const formatted = formatDisplayName(displayNameSelect, firstName, lastName)
        if (formatted !== null) {
            form.setFieldsValue({ displayName: formatted })
        }
    }, [firstName, lastName, displayNameSelect, form])

    const displayNameOptions = useMemo(() => [
        { value: DisplayUserName.FirstLast, label: t('users:profile_completion.display_name_first_last') },
        { value: DisplayUserName.LastFirst, label: t('users:profile_completion.display_name_last_first') },
        { value: DisplayUserName.Other, label: t('users:profile_completion.display_name_other') },
    ], [t])

    const handleSave = async () => {
        try {
            const values = await form.validateFields()
            setIsSaving(true)
            setError(null)

            await apiCall(
                '/users/info',
                {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        firstName: values.firstName.trim(),
                        lastName: values.lastName.trim(),
                        displayName: values.displayName.trim(),
                        email: values.email.trim(),
                    }),
                },
                { throwError: true }
            )

            notification.success({
                title: t('users:profile_completion.saved'),
                description: t('users:profile_completion.saved_description'),
            })
            await onSave()
        } catch (error) {
            setError(error instanceof Error && error.message
                ? error.message
                : t('users:profile_completion.save_failed'))
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <Modal
            {...(required ? { cancelButtonProps: { style: { display: 'none' } } } : {})}
            {...(!required && isSaving ? { cancelButtonProps: { disabled: true } } : {})}
            {...(onCancel ? { onCancel } : {})}
            closable={!required && !isSaving}
            keyboard={!required && !isSaving}
            mask={{ closable: !required && !isSaving }}
            okButtonProps={{ loading: isSaving }}
            okText={t('users:profile_completion.save')}
            onOk={handleSave}
            open={open}
            title={t('users:profile_completion.title')}
        >
            <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                <Alert
                    showIcon
                    title={t('users:profile_completion.description')}
                    type="info"
                />
                {error && (
                    <Alert
                        showIcon
                        title={error}
                        type="error"
                    />
                )}
                <Form
                    form={form}
                    initialValues={initialValues}
                    labelAlign="right"
                    labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                    layout="horizontal"
                    wrapperCol={{ flex: 1 }}
                >
                    <Form.Item
                        label={t('users:profile_completion.email')}
                        name="email"
                        rules={[
                            { required: true, whitespace: true, message: t('users:profile_completion.email_required') },
                            { type: 'email', message: t('users:profile_completion.email_invalid') },
                            { max: 254, message: t('users:profile_completion.email_max_length') },
                        ]}
                    >
                        <Input
                            disabled={!!profile.externalFlags?.emailExternal}
                            placeholder={t('users:profile_completion.email_placeholder')}
                            prefix={<MailOutlined />}
                        />
                    </Form.Item>
                    <Form.Item
                        label={t('users:profile_completion.first_name')}
                        name="firstName"
                        rules={[
                            { max: 25, message: t('users:profile_completion.first_name_max_length') },
                        ]}
                    >
                        <Input
                            placeholder={t('users:profile_completion.first_name_placeholder')}
                            prefix={<UserOutlined />}
                        />
                    </Form.Item>
                    <Form.Item
                        label={t('users:profile_completion.last_name')}
                        name="lastName"
                        rules={[
                            { max: 25, message: t('users:profile_completion.last_name_max_length') },
                        ]}
                    >
                        <Input
                            placeholder={t('users:profile_completion.last_name_placeholder')}
                            prefix={<UserOutlined />}
                        />
                    </Form.Item>
                    <Form.Item
                        required
                        label={t('users:profile_completion.display_name')}
                    >
                        {/* The flexed, shrinkable pair keeps the row beside its label and as wide as
                            the other inputs; a value-sized input would push the pair onto its own line. */}
                        <Space.Compact style={{ display: 'flex', width: '100%' }}>
                            <Form.Item noStyle name="displayNameSelect">
                                <Select
                                    data-testid="profile-completion-display-name-select"
                                    options={displayNameOptions}
                                    style={{ width: 140, flex: 'none' }}
                                />
                            </Form.Item>
                            <Form.Item
                                noStyle
                                name="displayName"
                                rules={[
                                    {
                                        required: true,
                                        whitespace: true,
                                        message: t('users:profile_completion.display_name_required'),
                                    },
                                    { max: 64, message: t('users:profile_completion.display_name_max_length') },
                                ]}
                            >
                                <Input
                                    aria-label={t('users:profile_completion.display_name')}
                                    disabled={displayNameSelect !== DisplayUserName.Other}
                                    placeholder={t('users:profile_completion.display_name_placeholder')}
                                    style={{ flex: '1 1 auto', minWidth: 0 }}
                                />
                            </Form.Item>
                        </Space.Compact>
                    </Form.Item>
                </Form>
            </Space>
        </Modal>
    )
}

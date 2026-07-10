import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Form, Input, Modal, notification, Select, Space, Spin } from 'antd'
import { MailOutlined, UserOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../../services'
import { DisplayUserName, WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { CommitInfoModalProps, UserCommitInfo } from './types'

interface CommitInfoFormValues {
    firstName: string
    lastName: string
    displayName: string
    email: string
    displayNameSelect: DisplayUserName
}

const EMPTY_VALUES: CommitInfoFormValues = {
    firstName: '',
    lastName: '',
    displayName: '',
    email: '',
    displayNameSelect: DisplayUserName.Other,
}

/**
 * Chooses the display-name mode from the stored value: "First Last", "Last First", or a custom
 * value. Mirrors the My Profile screen so the same identity edits the same way everywhere.
 */
const deriveDisplayNameSelect = (info: UserCommitInfo): DisplayUserName => {
    const firstName = info.firstName || ''
    const lastName = info.lastName || ''
    if (info.displayName && info.displayName === `${firstName} ${lastName}`.trim()) {
        return DisplayUserName.FirstLast
    }
    if (info.displayName && info.displayName === `${lastName} ${firstName}`.trim()) {
        return DisplayUserName.LastFirst
    }
    return DisplayUserName.Other
}

export const CommitInfoModal: React.FC<CommitInfoModalProps> = ({
    visible,
    username,
    onSave,
    onCancel,
}) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [isLoading, setIsLoading] = useState(false)
    const [isSaving, setIsSaving] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [initialValues, setInitialValues] = useState<CommitInfoFormValues>(EMPTY_VALUES)

    const firstName = Form.useWatch('firstName', form)
    const lastName = Form.useWatch('lastName', form)
    const displayNameSelect = Form.useWatch('displayNameSelect', form)

    const loadUserInfo = useCallback(async () => {
        setIsLoading(true)
        setError(null)

        try {
            const userInfo: UserCommitInfo = await apiCall(
                `/users/${encodeURIComponent(username)}`,
                { method: 'GET' },
                true
            )

            setInitialValues({
                firstName: userInfo.firstName || '',
                lastName: userInfo.lastName || '',
                displayName: userInfo.displayName || '',
                email: userInfo.email || '',
                displayNameSelect: deriveDisplayNameSelect(userInfo),
            })
        } catch (_err: any) {
            // User info might not exist yet, that's okay
            setInitialValues(EMPTY_VALUES)
        } finally {
            setIsLoading(false)
        }
    }, [username])

    // Load user info when modal opens
    useEffect(() => {
        if (visible && username) {
            loadUserInfo()
        }
    }, [visible, username, loadUserInfo])

    // Keep the display name in sync with the first/last name unless a custom value was chosen — the same
    // behaviour as the My Profile screen.
    useEffect(() => {
        if (displayNameSelect === DisplayUserName.FirstLast) {
            form.setFieldsValue({ displayName: `${firstName || ''} ${lastName || ''}`.trim() })
        } else if (displayNameSelect === DisplayUserName.LastFirst) {
            form.setFieldsValue({ displayName: `${lastName || ''} ${firstName || ''}`.trim() })
        }
    }, [firstName, lastName, displayNameSelect, form])

    const displayNameOptions = useMemo(() => [
        { value: DisplayUserName.FirstLast, label: t('merge:commit_info.display_name_first_last') },
        { value: DisplayUserName.LastFirst, label: t('merge:commit_info.display_name_last_first') },
        { value: DisplayUserName.Other, label: t('merge:commit_info.display_name_other') },
    ], [t])

    const handleSave = async () => {
        try {
            const values = await form.validateFields()
            setIsSaving(true)
            setError(null)

            await apiCall(
                `/users/${encodeURIComponent(username)}`,
                {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        firstName: values.firstName,
                        lastName: values.lastName,
                        displayName: values.displayName,
                        email: values.email,
                    }),
                },
                true
            )

            notification.success({
                title: t('merge:notifications.commit_info_saved'),
                description: t('merge:notifications.commit_info_saved_description'),
            })

            onSave()
        } catch (err: any) {
            setError(err?.message || t('merge:errors.commit_info_failed'))
        } finally {
            setIsSaving(false)
        }
    }

    return (
        <Modal
            closable={!isSaving}
            mask={{ closable: !isSaving }}
            okButtonProps={{ loading: isSaving, disabled: isLoading }}
            okText={t('merge:buttons.save')}
            onCancel={onCancel}
            onOk={handleSave}
            open={visible}
            title={t('merge:commit_info.title')}
        >
            {isLoading ? (
                <div style={{ textAlign: 'center', padding: 24 }}>
                    <Spin />
                </div>
            ) : (
                <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                    <Alert
                        showIcon
                        title={t('merge:commit_info.description')}
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
                            label={t('merge:commit_info.email')}
                            name="email"
                            rules={[
                                { required: true, message: t('merge:commit_info.email_required') },
                                { type: 'email', message: t('merge:commit_info.email_invalid') },
                            ]}
                        >
                            <Input
                                placeholder={t('merge:commit_info.email_placeholder')}
                                prefix={<MailOutlined />}
                            />
                        </Form.Item>
                        <Form.Item
                            label={t('merge:commit_info.first_name')}
                            name="firstName"
                        >
                            <Input
                                placeholder={t('merge:commit_info.first_name_placeholder')}
                                prefix={<UserOutlined />}
                            />
                        </Form.Item>
                        <Form.Item
                            label={t('merge:commit_info.last_name')}
                            name="lastName"
                        >
                            <Input
                                placeholder={t('merge:commit_info.last_name_placeholder')}
                                prefix={<UserOutlined />}
                            />
                        </Form.Item>
                        <Form.Item
                            required
                            label={t('merge:commit_info.display_name')}
                        >
                            <Space.Compact style={{ width: '100%' }}>
                                <Form.Item noStyle name="displayNameSelect">
                                    <Select
                                        data-testid="commit-info-display-name-select"
                                        options={displayNameOptions}
                                        style={{ width: 140 }}
                                    />
                                </Form.Item>
                                <Form.Item
                                    noStyle
                                    name="displayName"
                                    rules={[{ required: true, message: t('merge:commit_info.display_name_required') }]}
                                >
                                    <Input
                                        aria-label={t('merge:commit_info.display_name')}
                                        disabled={displayNameSelect !== DisplayUserName.Other}
                                        placeholder={t('merge:commit_info.display_name_placeholder')}
                                    />
                                </Form.Item>
                            </Space.Compact>
                        </Form.Item>
                    </Form>
                </Space>
            )}
        </Modal>
    )
}

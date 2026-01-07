import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Form, Input, Modal, notification, Space, Spin } from 'antd'
import { MailOutlined, UserOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { apiCall } from '../../services'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'

interface CommitInfoModalProps {
    visible: boolean
    username: string
    onSave: () => void
    onCancel: () => void
}

interface UserInfo {
    username: string
    displayName?: string
    email?: string
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

    const loadUserInfo = useCallback(async () => {
        setIsLoading(true)
        setError(null)

        try {
            const userInfo: UserInfo = await apiCall(
                `/users/${encodeURIComponent(username)}`,
                { method: 'GET' },
                true
            )

            form.setFieldsValue({
                displayName: userInfo.displayName || '',
                email: userInfo.email || '',
            })
        } catch (_err: any) {
            // User info might not exist yet, that's okay
            form.setFieldsValue({
                displayName: '',
                email: '',
            })
        } finally {
            setIsLoading(false)
        }
    }, [username, form])

    // Load user info when modal opens
    useEffect(() => {
        if (visible && username) {
            loadUserInfo()
        }
    }, [visible, username, loadUserInfo])

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
                        displayName: values.displayName,
                        email: values.email,
                    }),
                },
                true
            )

            notification.success({
                message: 'Commit info saved',
                description: 'Your Git commit information has been saved.',
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
            maskClosable={!isSaving}
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
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                    <Alert
                        showIcon
                        message={t('merge:commit_info.description')}
                        type="info"
                    />
                    {error && (
                        <Alert
                            showIcon
                            message={error}
                            type="error"
                        />
                    )}
                    <Form
                        form={form}
                        labelAlign="right"
                        labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                        layout="horizontal"
                        wrapperCol={{ flex: 1 }}
                    >
                        <Form.Item
                            label={t('merge:commit_info.display_name')}
                            name="displayName"
                            rules={[{ required: true, message: 'Display name is required' }]}
                        >
                            <Input
                                placeholder={t('merge:commit_info.display_name_placeholder')}
                                prefix={<UserOutlined />}
                            />
                        </Form.Item>
                        <Form.Item
                            label={t('merge:commit_info.email')}
                            name="email"
                            rules={[
                                { required: true, message: 'Email is required' },
                                { type: 'email', message: 'Please enter a valid email' },
                            ]}
                        >
                            <Input
                                placeholder={t('merge:commit_info.email_placeholder')}
                                prefix={<MailOutlined />}
                            />
                        </Form.Item>
                    </Form>
                </Space>
            )}
        </Modal>
    )
}

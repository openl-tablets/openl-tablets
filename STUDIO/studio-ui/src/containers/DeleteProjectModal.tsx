import React, { useCallback, useEffect, useState } from 'react'
import { Button, Checkbox, Form, Modal, Space, Typography } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { TextArea } from 'components/form'
import { useCommitInfoGuard, useGlobalEvents } from 'hooks'
import { deleteProject } from 'services/projects'

interface DeleteProjectModalDetail {
    projectId: string
    projectName: string
    onSuccess?: () => void
}

interface DeleteProjectForm {
    comment: string
    confirmed: boolean
}

export const DeleteProjectModal: React.FC = () => {
    const { t } = useTranslation()
    const [form] = Form.useForm<DeleteProjectForm>()
    const { detail } = useGlobalEvents<DeleteProjectModalDetail>('openDeleteProjectModal')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [visible, setVisible] = useState(false)
    const [deleting, setDeleting] = useState(false)
    const confirmed = Form.useWatch('confirmed', form)
    const comment = Form.useWatch('comment', form)

    useEffect(() => {
        const hasDetails = !!(detail && Object.keys(detail).length > 0)
        setVisible(hasDetails)
        setDeleting(false)
        if (hasDetails) {
            form.resetFields()
        }
    }, [detail, form])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openDeleteProjectModal', { detail: null }))
    }, [])

    const handleDelete = useCallback(async () => {
        if (!detail) {
            return
        }
        const values = await form.validateFields()
        await runWithCommitInfo(async () => {
            setDeleting(true)
            try {
                const deleted = await deleteProject(detail.projectId, detail.projectName, values.comment.trim())
                if (deleted) {
                    handleClose()
                    detail.onSuccess?.()
                }
            } finally {
                setDeleting(false)
            }
        })
    }, [detail, form, handleClose, runWithCommitInfo])

    const canDelete = Boolean(confirmed && comment?.trim())

    return (
        <>
            <Modal
                destroyOnHidden
                onCancel={handleClose}
                open={visible}
                footer={[
                    <Button key="cancel" disabled={deleting} onClick={handleClose}>
                        {t('common:btn.cancel')}
                    </Button>,
                    <Button
                        key="delete"
                        danger
                        disabled={!canDelete}
                        loading={deleting}
                        onClick={handleDelete}
                        type="primary"
                    >
                        {t('repository:delete_project_modal.confirm_button')}
                    </Button>,
                ]}
                title={(
                    <Space>
                        <DeleteOutlined />
                        {t('repository:delete_project_modal.title')}
                    </Space>
                )}
            >
                <Typography.Paragraph>
                    {t('repository:delete_project_modal.confirm', { project: detail?.projectName })}
                </Typography.Paragraph>
                <Form form={form} layout="vertical">
                    <TextArea
                        required
                        label={t('repository:delete_project_modal.comment_label', { project: detail?.projectName })}
                        name="comment"
                        rows={4}
                    />
                    <Form.Item name="confirmed" valuePropName="checked">
                        <Checkbox>{t('repository:delete_project_modal.confirmation')}</Checkbox>
                    </Form.Item>
                </Form>
            </Modal>
            {commitInfoModal}
        </>
    )
}

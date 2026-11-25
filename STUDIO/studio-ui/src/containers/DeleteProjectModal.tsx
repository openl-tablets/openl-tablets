import React, { useCallback, useEffect, useState } from 'react'
import { Button, Checkbox, Form, Modal, Space, Typography } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { TextArea } from 'components/form'
import { useCommitInfoGuard, useGlobalEvents, useRepositoryConfig } from 'hooks'
import { useCommentValidator } from 'containers/projects/CommentField'
import { deleteProject } from 'services/projects'
import { supportsComments } from 'utils/repositoryFeatures'
import type { RepositoryInfo } from 'types/repositories'

interface DeleteProjectModalDetail {
    projectId: string
    projectName: string
    repositoryInfo?: RepositoryInfo
    localOnly?: boolean
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
    const repositorySupportsComments = !detail?.localOnly && supportsComments(detail?.repositoryInfo)
    // Where the repository supports comments, deletion follows its validation rule. No deletion template is offered.
    const config = useRepositoryConfig(detail?.projectId && repositorySupportsComments
        ? { projectId: detail.projectId }
        : null)
    const checkComment = useCommentValidator(config)

    // Settings that arrive after the user typed check the comment again, so the field agrees with Delete.
    useEffect(() => {
        if (form.isFieldTouched('comment')) {
            form.validateFields(['comment']).catch(() => {
                // The field shows the message itself.
            })
        }
    }, [config, form])

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
                const deleted = await deleteProject(detail.projectId, detail.projectName,
                    repositorySupportsComments ? values.comment?.trim() : undefined)
                if (deleted) {
                    handleClose()
                    detail.onSuccess?.()
                }
            } finally {
                setDeleting(false)
            }
        })
    }, [detail, form, handleClose, repositorySupportsComments, runWithCommitInfo])

    const canDelete = Boolean(confirmed && (!repositorySupportsComments || comment?.trim() && !checkComment(comment)))

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
                    {repositorySupportsComments && <TextArea
                        // A blank comment gets only the message of the first rule.
                        validateFirst
                        label={t('repository:delete_project_modal.comment_label', { project: detail?.projectName })}
                        name="comment"
                        rows={4}
                        rules={[
                            { required: true, whitespace: true, message: t('common:validation.required') },
                            {
                                // The rule runs before the next render, so it checks the value it is given.
                                validator: async (_, value: string) => {
                                    const error = checkComment(value)
                                    if (error) {
                                        throw new Error(error)
                                    }
                                },
                            },
                        ]}
                    />}
                    <Form.Item name="confirmed" valuePropName="checked">
                        <Checkbox>{t('repository:delete_project_modal.confirmation')}</Checkbox>
                    </Form.Item>
                </Form>
            </Modal>
            {commitInfoModal}
        </>
    )
}

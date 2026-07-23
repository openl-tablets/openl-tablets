import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Modal, notification, Typography } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { apiCall, isApiHttpError } from '../../services'
import { saveProject } from '../../services/repositories'
import { useCommitInfoGuard, useRepositoryConfig } from '../../hooks'
import { suggestComment } from '../../utils/repositoryConfig'
import { CommentField, useCommentError } from './CommentField'
import { openMergeDialog } from './branchDialogs'
import type { Project } from '../../types/projects'
import { ProjectStatus } from '../../constants/project'
import { formatDateTime } from '../../utils/dateFormat'

interface SaveProjectModalProps {
    open: boolean
    project: Project | null
    onClose: () => void
    onSaved: () => void
}

/** Returns true when the project currently has unresolved merge conflicts on the server. */
const hasMergeConflicts = async (projectId: string): Promise<boolean> => {
    try {
        const details = await apiCall(
            `/projects/${encodeURIComponent(projectId)}/merge/conflicts`,
            { method: 'GET' },
            { throwError: true, suppressErrorPages: true }
        )
        return Array.isArray(details?.conflictGroups) && details.conflictGroups.length > 0
    } catch {
        return false
    }
}

/**
 * Commit-comment dialog for saving a project, mirroring the legacy repository save flow.
 *
 * Before committing it ensures the user's Git identity is configured (prompting via
 * {@link CommitInfoModal} when it is missing). If the commit races another change and the server
 * reports a merge conflict, the shared merge resolver is opened on its conflicts step.
 */
export const SaveProjectModal = ({ open, project, onClose, onSaved }: SaveProjectModalProps) => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [comment, setComment] = useState('')
    const [commentTouched, setCommentTouched] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const config = useRepositoryConfig(open && project ? { projectId: project.id } : null)
    const commentError = useCommentError(comment, config)

    // A fresh dialog starts from the repository's suggestion again.
    useEffect(() => {
        setCommentTouched(false)
    }, [open, project])

    // The repository suggests the comment; it stays editable. Saving a project opened on an older revision
    // restores that revision, and the repository has its own template for it.
    useEffect(() => {
        if (!open || commentTouched) {
            return
        }
        setComment(project?.status === ProjectStatus.ViewingVersion
            ? suggestComment(config, 'restoreFrom', {
                revision: project.revision,
                author: project.modifiedBy,
                datetime: formatDateTime(project.modifiedAt) ?? '',
            })
            : suggestComment(config, 'save', project?.name))
    }, [commentTouched, config, open, project])

    const openConflictResolution = async (target: Project) => {
        notification.warning({ title: t('browser.save_conflict') })
        // Only Git repositories can produce a save-time merge conflict.
        await openMergeDialog(target, onSaved, 'conflicts')
    }

    const doSave = async (target: Project) => {
        setSubmitting(true)
        try {
            await saveProject(target.id, comment.trim() || undefined)
            notification.success({ title: t('browser.saved') })
            onSaved()
            onClose()
        } catch (e) {
            if (isApiHttpError(e) && e.status === 409 && await hasMergeConflicts(target.id)) {
                await openConflictResolution(target)
                onClose()
                return
            }
            notification.error({
                title: t('browser.save_failed'),
                description: errorMessage(e),
            })
        } finally {
            setSubmitting(false)
        }
    }

    const submit = async () => {
        if (!project || commentError) {
            return
        }
        await runWithCommitInfo(() => doSave(project))
    }

    return (
        <>
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okButtonProps={{ 'data-testid': 'save-project-submit', disabled: !!commentError, icon: <SaveOutlined /> }}
                okText={t('browser.save')}
                onCancel={onClose}
                onOk={submit}
                open={open}
                title={<><SaveOutlined /> {t('browser.save_title')}</>}
            >
                <Typography.Paragraph type="secondary">
                    {t('browser.save_desc', { repository: project?.repositoryInfo?.name ?? project?.repository ?? '' })}
                </Typography.Paragraph>
                <CommentField
                    config={config}
                    testId="save-project-comment"
                    value={comment}
                    onChange={value => {
                        setCommentTouched(true)
                        setComment(value)
                    }}
                />
            </Modal>
            {commitInfoModal}
        </>
    )
}

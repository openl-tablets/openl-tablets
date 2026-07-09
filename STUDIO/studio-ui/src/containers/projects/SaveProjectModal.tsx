import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Input, Modal, notification, Typography } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { apiCall, isApiHttpError } from '../../services'
import { getProjectBranches, saveProject } from '../../services/repositories'
import { useCommitInfoGuard } from '../../hooks'
import type { Project } from '../../types/projects'

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
    const [submitting, setSubmitting] = useState(false)

    useEffect(() => {
        if (open) {
            setComment('')
        }
    }, [open])

    const openConflictResolution = async (target: Project) => {
        let branches: unknown[] = []
        try {
            branches = await getProjectBranches(target.id)
        } catch {
            // The resolver only needs the conflicts endpoint; branch list is best-effort.
        }
        notification.warning({ title: t('browser.save_conflict') })
        window.dispatchEvent(new CustomEvent('openMergeModal', {
            detail: {
                projectId: target.id,
                projectName: target.name,
                repositoryId: target.repository,
                // Only Git repositories can produce a save-time merge conflict.
                repositoryType: 'repo-git',
                currentBranch: target.branch || '',
                branches,
                initialStep: 'conflicts',
                onSuccess: onSaved,
            },
        }))
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
        if (!project) {
            return
        }
        await runWithCommitInfo(() => doSave(project))
    }

    return (
        <>
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okButtonProps={{ 'data-testid': 'save-project-submit', icon: <SaveOutlined /> }}
                okText={t('browser.save')}
                onCancel={onClose}
                onOk={submit}
                open={open}
                title={<><SaveOutlined /> {t('browser.save_title')}</>}
            >
                <Typography.Paragraph type="secondary">
                    {t('browser.save_desc', { repository: project?.repository ?? '' })}
                </Typography.Paragraph>
                <Typography.Text strong>{t('browser.save_comment')}</Typography.Text>
                <Input.TextArea
                    autoSize={{ maxRows: 6, minRows: 3 }}
                    data-testid="save-project-comment"
                    onChange={event => setComment(event.target.value)}
                    style={{ marginTop: 6 }}
                    value={comment}
                />
            </Modal>
            {commitInfoModal}
        </>
    )
}

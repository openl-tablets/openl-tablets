import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { App, Collapse, Flex, Modal, Typography } from 'antd'
import { SaveOutlined } from '@ant-design/icons'
import { apiCall, isApiHttpError } from '../../services'
import { LOCAL_LOAD_API_OPTIONS } from '../../services/apiCall'
import { getProject, saveProject } from '../../services/repositories'
import { useCommitInfoGuard, useRepositoryConfig } from '../../hooks'
import { errorHandler } from '../../utils/errorHandling'
import { suggestComment } from '../../utils/repositoryConfig'
import { ChangedFileList, ChangesHeader } from './ChangedFileList'
import { CommentField, useCommentError } from './CommentField'
import { openMergeDialog } from './branchDialogs'
import { openFileRevisionsCompareWindow } from './compare'
import { normalizeProjectFileChanges, type NormalizedFileChange } from './fileChanges'
import type { Project } from '../../types/projects'
import { ProjectStatus } from '../../constants/project'
import { supportsComments } from '../../utils/repositoryFeatures'

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

/** The files a save commits, and where the copy they were changed in was taken from. */
interface PendingSave {
    /** The files changed since the project was opened or last saved, by their paths in the project. */
    files: NormalizedFileChange[]
    /** The revision the working copy was opened or last saved at. */
    revision: string | undefined
    /** The branch of that revision; absent in a repository without branches. */
    branch: string | undefined
}

/**
 * Reads what a save of the project would commit, once the dialog opens for it. The project is read once for the
 * dialog, whatever new copies of it the screen behind hands in meanwhile. A read that fails is only logged: the
 * dialog saves the project all the same, without listing its files.
 */
const usePendingSave = (open: boolean, projectId: string | undefined): PendingSave | null => {
    const [pending, setPending] = useState<PendingSave | null>(null)
    useEffect(() => {
        setPending(null)
        if (!open || !projectId) {
            return
        }
        let cancelled = false
        getProject(projectId, { includes: ['status']}, LOCAL_LOAD_API_OPTIONS)
            .then(loaded => {
                if (!cancelled) {
                    const status = loaded.compileStatus
                    setPending({
                        files: normalizeProjectFileChanges(status?.pendingChanges?.files ?? [], loaded.path, loaded.name),
                        revision: status?.revision ?? undefined,
                        branch: status?.branch ?? undefined,
                    })
                }
            })
            .catch((error: unknown) => {
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
            })
        return () => {
            cancelled = true
        }
    }, [open, projectId])
    return pending
}

/**
 * Dialog for saving a project. It asks for a comment when the repository supports one.
 *
 * It lists the files the save commits, each comparable with the revision the working copy was taken from. Before
 * committing it verifies that the profile completed during login is still valid. If the commit races another change
 * and the server reports a merge conflict, the shared merge resolver is opened on its conflicts step.
 */
export const SaveProjectModal = ({ open, project, onClose, onSaved }: SaveProjectModalProps) => {
    const { notification } = App.useApp()
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const [comment, setComment] = useState('')
    const [commentTouched, setCommentTouched] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const repositorySupportsComments = supportsComments(project?.repositoryInfo)
    const config = useRepositoryConfig(open && project && repositorySupportsComments ? { projectId: project.id } : null)
    const commentError = useCommentError(comment, config)
    const pending = usePendingSave(open, project?.id)

    // A fresh dialog starts from the repository's suggestion again.
    useEffect(() => {
        setCommentTouched(false)
    }, [open, project])

    // The repository suggests the comment; it stays editable. Saving a project opened on an older revision
    // restores that revision, and the repository has its own template for it.
    useEffect(() => {
        if (!open || !repositorySupportsComments || commentTouched) {
            return
        }
        setComment(project?.status === ProjectStatus.ViewingVersion
            ? suggestComment(config, 'restoreFrom', {
                revision: project.revision,
                author: project.modifiedBy,
                datetime: project.modifiedAt,
            })
            : suggestComment(config, 'save', project?.name))
    }, [commentTouched, config, open, project, repositorySupportsComments])

    const openConflictResolution = async (target: Project) => {
        notification.warning({ title: t('browser.save_conflict') })
        // Only Git repositories can produce a save-time merge conflict.
        await openMergeDialog(target, onSaved, 'conflicts')
    }

    const doSave = async (target: Project) => {
        setSubmitting(true)
        try {
            await saveProject(target.id, repositorySupportsComments ? comment.trim() || undefined : undefined)
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
        if (!project || repositorySupportsComments && commentError) {
            return
        }
        await runWithCommitInfo(() => doSave(project))
    }

    // A modified file is compared at the revision the working copy was taken from against the working copy.
    const revision = pending?.revision
    const branch = pending?.branch
    const comparison = project && revision
        ? {
            hint: t('browser.save_compare_hint'),
            open: (file: NormalizedFileChange) => openFileRevisionsCompareWindow(project.id, file.path, branch,
                revision),
        }
        : undefined

    return (
        <>
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okButtonProps={{ 'data-testid': 'save-project-submit', disabled: repositorySupportsComments && !!commentError, icon: <SaveOutlined /> }}
                okText={t('browser.save')}
                onCancel={onClose}
                onOk={submit}
                open={open}
                title={<><SaveOutlined /> {t('browser.save_title')}</>}
                // A modified project lists its files by their paths, which take the room of a wider dialog.
                {...(project?.status === ProjectStatus.Editing ? { width: 800 } : {})}
            >
                <Typography.Paragraph type="secondary">
                    {t('browser.save_desc', { repository: project?.repositoryInfo?.name ?? project?.repository ?? '' })}
                </Typography.Paragraph>
                <Flex vertical gap="middle">
                    {project && pending && pending.files.length > 0 && (
                        // The files are folded away until asked for, as a merge folds those of other projects.
                        <Collapse
                            data-testid="save-project-changes"
                            size="small"
                            items={[{
                                key: 'changes',
                                label: (
                                    <ChangesHeader
                                        count={t('browser.save_changes', { count: pending.files.length })}
                                        name={project.name}
                                    />
                                ),
                                children: (
                                    <ChangedFileList
                                        compare={comparison}
                                        files={pending.files}
                                        testId="save-project-changes"
                                    />
                                ),
                            }]}
                        />
                    )}
                    {repositorySupportsComments && <CommentField
                        config={config}
                        testId="save-project-comment"
                        value={comment}
                        onChange={value => {
                            setCommentTouched(true)
                            setComment(value)
                        }}
                    />}
                </Flex>
            </Modal>
            {commitInfoModal}
        </>
    )
}

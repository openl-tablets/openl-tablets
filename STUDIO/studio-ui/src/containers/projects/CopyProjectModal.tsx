import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Checkbox, Input, Modal, notification, Select } from 'antd'
import {
    copyProject,
    createProjectBranch,
    getDesignRepositoryBranches,
    isProjectModifiedConflict,
    switchProjectBranch,
} from '../../services/repositories'
import { FieldError } from '../../components/FieldError'
import { FieldRow } from '../../components/FieldRow'
import { BranchSelect } from './BranchSelect'
import { branchMarksFromConfig } from './configBranchMarks'
import { RepoFolderInput } from './RepoFolderInput'
import { useProjectRevisions } from './revisions'
import type { Project } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { suggestBranchName, suggestComment, validateBranchName } from '../../utils/repositoryConfig'
import { CommentField, useCommentError } from './CommentField'
import { useUserStore } from '../../store'
import { supportsBranches, supportsMappedFolders } from '../../utils/repositoryFeatures'
import { useCommitInfoGuard, useRepositoryConfig } from '../../hooks'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { ProjectStatus } from '../../constants/project'

const LABEL_WIDTH = 160

interface CopyProjectModalProps {
    open: boolean
    project: Project | null
    repositories: Repository[]
    onClose: () => void
    onCopied: () => void
}

/**
 * Copies a project, either into a branch of its own repository or into a brand-new project.
 *
 * Both halves are guarded by different rights: branching needs write access to the project, while a new
 * project needs the right to create one in some repository. Whichever the user has decides what the dialog
 * offers; when both are available, a checkbox switches between them.
 *
 * The new branch name and the commit comment start from the repository configuration. A copy can be taken
 * from an older revision of the project instead of its latest state.
 */
export const CopyProjectModal = ({ open, project, repositories, onClose, onCopied }: CopyProjectModalProps) => {
    const { t } = useTranslation('repository')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const username = useUserStore(state => state.userProfile?.username)
    const [asNewProject, setAsNewProject] = useState(false)
    const [branch, setBranch] = useState('')
    const [targetRepositoryId, setTargetRepositoryId] = useState('')
    const [targetBranch, setTargetBranch] = useState('')
    const [targetBranchOptions, setTargetBranchOptions] = useState<string[]>([])
    const [targetBranchesLoading, setTargetBranchesLoading] = useState(false)
    const [targetBranchTouched, setTargetBranchTouched] = useState(false)
    const [name, setName] = useState('')
    const [comment, setComment] = useState('')
    const [path, setPath] = useState('')
    const [fromOldRevision, setFromOldRevision] = useState(false)
    const [chosen, setChosen] = useState<string | undefined>(undefined)
    const [touched, setTouched] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [discardBranch, setDiscardBranch] = useState<string | null>(null)

    const sourceConfig = useRepositoryConfig(open && project ? { projectId: project.id } : null)
    // The repository configures how a branch may be named, so a wrong name never reaches the server.
    const branchError = validateBranchName(branch, sourceConfig, t('browser.copy_dialog.branch_required'),
        t('browser.copy_dialog.branch_invalid'))

    const canBranch = !!project?.capabilities?.canManageBranches && !!project.branch
    // The branch is cut from the last commit, so whatever is only in the workspace does not come along.
    const modified = project?.status === ProjectStatus.Editing
    const canCopyProject = !!project?.capabilities?.canCopy && repositories.length > 0
    // Without the right to create a project the dialog can only branch, and the other way round.
    const newProjectMode = canCopyProject && (asNewProject || !canBranch)
    const targetRepository = repositories.find(repo => repo.id === targetRepositoryId)
    const targetSupportsBranches = supportsBranches(targetRepository)
    const targetConfig = useRepositoryConfig(
        open && newProjectMode && targetRepositoryId ? { repositoryId: targetRepositoryId } : null
    )
    const writeConfig = newProjectMode ? targetConfig : sourceConfig
    const commentError = useCommentError(comment, writeConfig)
    const availableTargetBranches = targetConfig?.branch && !targetBranchOptions.includes(targetConfig.branch)
        ? [targetConfig.branch, ...targetBranchOptions]
        : targetBranchOptions
    const targetBranchKnown = availableTargetBranches.includes(targetBranch.trim())
    const targetBranchError = !targetSupportsBranches || targetBranchKnown
        ? null
        : validateBranchName(targetBranch, targetConfig, t('browser.create.branch_required'),
            t('browser.create.branch_invalid'))

    useEffect(() => {
        if (!open || !project) {
            return
        }
        setTouched(false)
        setAsNewProject(false)
        setTargetRepositoryId(repositories.some(repo => repo.id === project.repository)
            ? project.repository
            : repositories[0]?.id ?? '')
        setTargetBranch('')
        setTargetBranchOptions([])
        setTargetBranchTouched(false)
        setName('')
        setPath('')
        setFromOldRevision(false)
        setChosen(undefined)
        setError(null)
        setDiscardBranch(null)
    }, [open, project, repositories])

    useEffect(() => {
        let active = true
        setTargetBranch('')
        setTargetBranchOptions([])
        setTargetBranchesLoading(false)
        setTargetBranchTouched(false)
        if (!open || !newProjectMode || !targetSupportsBranches || !targetRepositoryId) {
            return
        }
        setTargetBranchesLoading(true)
        getDesignRepositoryBranches(targetRepositoryId)
            .then(options => {
                if (active) {
                    setTargetBranchOptions(options)
                }
            })
            .catch(() => {
                if (active) {
                    setTargetBranchOptions([])
                }
            })
            .finally(() => {
                if (active) {
                    setTargetBranchesLoading(false)
                }
            })
        return () => {
            active = false
        }
    }, [newProjectMode, open, targetRepositoryId, targetSupportsBranches])

    useEffect(() => {
        if (open && newProjectMode && targetSupportsBranches && !targetBranchTouched && targetConfig?.branch) {
            setTargetBranch(targetConfig.branch)
        }
    }, [newProjectMode, open, targetBranchTouched, targetConfig, targetSupportsBranches])

    // The repository suggests the branch name and the comment; both stay editable, and a suggestion that
    // arrives after the user has typed never replaces what they wrote.
    useEffect(() => {
        if (!open || !project || touched) {
            return
        }
        setBranch(suggestBranchName(sourceConfig, { projectName: project.name, username }))
        setComment(suggestComment(writeConfig, 'copy', project.name))
    }, [open, project, sourceConfig, touched, username, writeConfig])

    // The revisions to copy from are only listed once the user asks for an older one.
    const { revisions, options: revisionOptions } = useProjectRevisions(project, open && fromOldRevision)
    const revision = chosen ?? revisions?.[0]?.revisionNo

    // A path inside the repository only applies to non-flat (mapped-folder) repositories.
    const targetSupportsFolders = supportsMappedFolders(repositories.find(repo => repo.id === targetRepositoryId))

    const submit = async () => {
        if (!project) {
            return
        }
        await runWithCommitInfo(async () => {
            setSubmitting(true)
            setError(null)
            try {
                // Branching stops half-way when it has to ask about the unsaved changes; the dialog then
                // stays open behind the confirmation.
                const finished = newProjectMode ? await copyToNewProject(project) : await branchProject(project)
                if (!finished) {
                    return
                }
                onCopied()
                onClose()
            } catch (e) {
                setError(errorMessage(e))
            } finally {
                setSubmitting(false)
            }
        })
    }

    /** @returns whether the project reached the new branch, `false` while the discard is being confirmed. */
    const branchProject = async (source: Project): Promise<boolean> => {
        if (branchError) {
            throw new Error(branchError)
        }
        const trimmed = branch.trim()
        await createProjectBranch(source.id, trimmed)
        // The point of branching here is to keep working on the copy, so the project moves onto it. A
        // modified project refuses the move: the branch is already there, so ask whether the unsaved
        // changes may go instead of reporting the raw conflict.
        try {
            await switchProjectBranch(source.id, trimmed)
        } catch (e) {
            if (isProjectModifiedConflict(e)) {
                setDiscardBranch(trimmed)
                return false
            }
            throw e
        }
        notification.success({ title: t('browser.copy_dialog.branch_success', { name: trimmed }) })
        return true
    }

    /**
     * The user kept the unsaved changes rather than move onto the new branch. The branch was already
     * created, so say so — and close the dialog, so a second attempt does not try to create it again and
     * hit a raw "branch already exists" error.
     */
    const keepChangesOnCurrentBranch = () => {
        const created = discardBranch
        setDiscardBranch(null)
        if (created) {
            notification.info({ title: t('browser.copy_dialog.branch_created_not_switched', { name: created }) })
            onCopied()
            onClose()
        }
    }

    /** Moves the project onto the branch that is already created, dropping what was never committed. */
    const moveDiscardingChanges = async (target: string) => {
        if (!project) {
            return
        }
        setDiscardBranch(null)
        setSubmitting(true)
        setError(null)
        try {
            await switchProjectBranch(project.id, target, { discardChanges: true })
            notification.success({ title: t('browser.copy_dialog.branch_success', { name: target }) })
            onCopied()
            onClose()
        } catch (e) {
            setError(errorMessage(e))
        } finally {
            setSubmitting(false)
        }
    }

    const copyToNewProject = async (source: Project): Promise<boolean> => {
        const trimmed = name.trim()
        if (!trimmed) {
            throw new Error(t('browser.copy_dialog.name_required'))
        }
        if (commentError) {
            throw new Error(commentError)
        }
        if (targetBranchError) {
            throw new Error(targetBranchError)
        }
        await copyProject(
            source.repository,
            source.id,
            targetRepositoryId,
            trimmed,
            comment.trim() || undefined,
            targetSupportsFolders ? path : undefined,
            fromOldRevision ? revision : undefined,
            targetSupportsBranches ? targetBranch.trim() : undefined
        )
        notification.success({ title: t('browser.copy_dialog.success', { name: trimmed }) })
        return true
    }

    return (
        <>
            <Modal
                destroyOnHidden
                confirmLoading={submitting}
                okText={t('browser.copy_dialog.submit')}
                onCancel={onClose}
                onOk={submit}
                open={open}
                title={t('browser.copy_dialog.title')}
                okButtonProps={{
                    'data-testid': 'copy-project-submit',
                    // An old-revision copy waits for the revision: an empty one would copy the latest state.
                    disabled: (!canBranch && !canCopyProject) || (fromOldRevision && !revision),
                }}
            >
                {error && (
                    <Alert showIcon data-testid="copy-project-error" style={{ marginBottom: 12 }} title={error} type="error" />
                )}
                {modified && !newProjectMode && (
                    <Alert
                        showIcon
                        data-testid="copy-project-modified-warning"
                        style={{ marginBottom: 12 }}
                        title={t('browser.copy_dialog.modified_warning')}
                        type="warning"
                    />
                )}
                <FieldRow label={t('browser.copy_dialog.project_name')} labelWidth={LABEL_WIDTH}>
                    <span data-testid="copy-project-source">{project?.name}</span>
                </FieldRow>
                {canBranch && canCopyProject && (
                    <FieldRow label={t('browser.copy_dialog.as_new_project')} labelWidth={LABEL_WIDTH}>
                        <Checkbox
                            checked={asNewProject}
                            data-testid="copy-project-as-new"
                            onChange={event => setAsNewProject(event.target.checked)}
                        />
                    </FieldRow>
                )}
                {!!project?.branch && (
                    <FieldRow label={t('browser.copy_dialog.current_branch')} labelWidth={LABEL_WIDTH}>
                        <span data-testid="copy-project-current-branch">{project.branch}</span>
                    </FieldRow>
                )}
                {!newProjectMode ? (
                    <FieldRow required label={t('browser.copy_dialog.new_branch')} labelWidth={LABEL_WIDTH}>
                        <Input
                            data-testid="copy-project-branch"
                            status={branch.trim() && branchError ? 'error' : ''}
                            value={branch}
                            onChange={event => {
                                setTouched(true)
                                setBranch(event.target.value)
                            }}
                        />
                        <FieldError message={branch.trim() ? branchError : null} testId="copy-project-branch-error" />
                    </FieldRow>
                ) : (
                    <>
                        <FieldRow required label={t('browser.copy_dialog.new_name')} labelWidth={LABEL_WIDTH}>
                            <Input data-testid="copy-project-name" onChange={event => setName(event.target.value)} value={name} />
                        </FieldRow>
                        <FieldRow required label={t('browser.copy_dialog.target_repository')} labelWidth={LABEL_WIDTH}>
                            <Select
                                data-testid="copy-project-repository"
                                onChange={value => setTargetRepositoryId(value as string)}
                                options={repositories.map(repo => ({ value: repo.id, label: repo.name }))}
                                style={{ width: '100%' }}
                                value={targetRepositoryId}
                            />
                        </FieldRow>
                        {targetSupportsBranches && (
                            <FieldRow required label={t('browser.create.branch')} labelWidth={LABEL_WIDTH}>
                                <BranchSelect
                                    allowNew
                                    branchNames={availableTargetBranches}
                                    data-testid="copy-project-target-branch"
                                    loading={targetBranchesLoading}
                                    marksOf={branchMarksFromConfig(targetConfig)}
                                    placeholder={t('browser.create.branch')}
                                    value={targetBranch}
                                    onChange={value => {
                                        setTargetBranchTouched(true)
                                        setTargetBranch(value)
                                    }}
                                />
                                <FieldError
                                    message={targetBranchTouched ? targetBranchError : null}
                                    testId="copy-project-target-branch-error"
                                />
                            </FieldRow>
                        )}
                        {targetSupportsFolders && (
                            <FieldRow label={t('browser.copy_dialog.path')} labelWidth={LABEL_WIDTH}>
                                <RepoFolderInput
                                    data-testid="copy-project-path"
                                    onChange={setPath}
                                    placeholder={t('browser.copy_dialog.path_placeholder')}
                                    repositoryId={targetRepositoryId}
                                    value={path}
                                />
                            </FieldRow>
                        )}
                        <CommentField
                            config={writeConfig}
                            labelWidth={LABEL_WIDTH}
                            testId="copy-project-comment"
                            value={comment}
                            onChange={value => {
                                setTouched(true)
                                setComment(value)
                            }}
                        />
                        <FieldRow label={t('browser.copy_dialog.old_revision')} labelWidth={LABEL_WIDTH}>
                            <Checkbox
                                checked={fromOldRevision}
                                data-testid="copy-project-old-revision"
                                onChange={event => setFromOldRevision(event.target.checked)}
                            />
                        </FieldRow>
                        {fromOldRevision && (
                            <FieldRow required label={t('browser.copy_dialog.revision')} labelWidth={LABEL_WIDTH}>
                                <Select
                                    data-testid="copy-project-revision"
                                    loading={revisions === null}
                                    onChange={value => setChosen(value as string)}
                                    options={revisionOptions}
                                    style={{ width: '100%' }}
                                    value={revision}
                                />
                            </FieldRow>
                        )}
                    </>
                )}
            </Modal>
            <DiscardChangesModal
                cancelButtonTestId="copy-project-discard-cancel"
                confirmButtonTestId="copy-project-discard-confirm"
                confirmText={t('browser.copy_dialog.branch_discard_confirm_unsafe')}
                onCancel={keepChangesOnCurrentBranch}
                onConfirm={() => void moveDiscardingChanges(discardBranch ?? '')}
                open={discardBranch !== null}
                warning={t('browser.switch_branch_discard_warning')}
            />
            {commitInfoModal}
        </>
    )
}

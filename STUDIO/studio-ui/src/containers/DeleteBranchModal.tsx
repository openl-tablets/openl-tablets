import React, { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Modal, Space, Spin, Typography } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useCommitInfoGuard, useGlobalEvents } from '../hooks'
import { apiCall, type ApiCallOptions } from '../services'
import { deleteBranch } from '../services/branches'
import { toUrlSafeId } from '../services/projectId'
import { ProjectStatus } from '../constants/project'

// Only reads here — a project fetch and a merge check — so the merge-check POST must not drop the snapshot.
const CHECK_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true, skipWorkspaceEvent: true }

/**
 * Detail passed with the {@code openDeleteBranchModal} event by whoever opens the dialog.
 *
 * {@code modified} and {@code mergedIntoMain} are resolved by the modal itself through the projects and merge
 * REST APIs. Everything the opener already read — the identifiers, the main branch, the branch marks — is
 * supplied here rather than fetched a second time.
 */
export interface DeleteBranchModalDetail {
    /** The project's own id, as the server issued it. */
    projectId?: string
    repositoryId: string
    projectName: string
    branch: string
    mainBranch?: string
    /** Whether the branch is protected; deleting one asks the server for the protection bypass. */
    branchProtected?: boolean
    /** Whether this is the only branch holding the project, so deleting it deletes the project. */
    lastBranch?: boolean
    onSuccess?: () => void
}

/**
 * The project id every request of this dialog puts in its URL path, in the URL-safe Base64 alphabet.
 *
 * The server issues ids in the standard alphabet, whose `/` a servlet container rejects once percent-encoded,
 * so the id is normalized here as it is everywhere else. An opener that knows only the repository and the
 * project name gets the id rebuilt from them: that encoding covers Latin-1 names, which is all the legacy
 * screens address.
 */
const resolveProjectId = (detail: DeleteBranchModalDetail): string =>
    toUrlSafeId(detail.projectId ?? btoa(`${detail.repositoryId}:${detail.projectName}`))

/**
 * DeleteBranchModal component.
 *
 * @example to open this modal, dispatch a custom event:
 * globalThis.dispatchEvent(new CustomEvent('openDeleteBranchModal', {detail: {...}}))
 */
export const DeleteBranchModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<DeleteBranchModalDetail>('openDeleteBranchModal')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()

    const [visible, setVisible] = useState(false)
    const [loading, setLoading] = useState(false)
    const [deleting, setDeleting] = useState(false)
    const [modified, setModified] = useState(false)
    const [mergedIntoMain, setMergedIntoMain] = useState(true)

    const projectId = detail ? resolveProjectId(detail) : ''

    // Resolve the deletion warnings when the modal opens: `modified` from the project status,
    // `mergedIntoMain` from a merge check (send current branch into main). The check answers whether the
    // branches differ even when the main branch is protected and the user may not merge into it — the
    // warning is about losing changes, not about permission. A modified project is already destructive, so
    // the merge check is skipped; any preflight failure falls back to the cautious "not merged" state so
    // the user must confirm.
    useEffect(() => {
        const hasDetails = !!(detail && Object.keys(detail).length > 0)
        setVisible(hasDetails)
        if (!hasDetails || !detail) {
            return
        }
        let cancelled = false
        setModified(false)
        setMergedIntoMain(true)
        setLoading(true)
        const id = resolveProjectId(detail)
        const resolve = async () => {
            const project = (await apiCall(`/projects/${id}`, { method: 'GET' }, CHECK_API_OPTIONS)) as { status?: ProjectStatus }
            if (cancelled) {
                return
            }
            const isModified = project?.status === ProjectStatus.Editing
            setModified(isModified)
            if (isModified || !detail.mainBranch || detail.mainBranch === detail.branch) {
                return
            }
            try {
                const result = (await apiCall(`/projects/${id}/merge/check`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ mode: 'send', otherBranch: detail.mainBranch }),
                }, CHECK_API_OPTIONS)) as { status?: string }
                if (!cancelled) {
                    setMergedIntoMain(result?.status === 'up-to-date')
                }
            } catch {
                if (!cancelled) {
                    setMergedIntoMain(false)
                }
            }
        }
        resolve()
            .catch(() => {
                // Could not determine the branch state; fall back to the cautious (unsafe)
                // posture so the user must confirm a potentially destructive delete.
                if (!cancelled) {
                    setMergedIntoMain(false)
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false)
                }
            })
        return () => {
            cancelled = true
        }
    }, [detail])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openDeleteBranchModal', { detail: null }))
    }, [])

    const handleDelete = useCallback(async () => {
        if (!detail) {
            return
        }
        await runWithCommitInfo(async () => {
            setDeleting(true)
            try {
                // Deleting a protected branch needs the repository bypass, and this dialog is the explicit
                // confirmation for it. The server still decides: a user without the bypass gets a 403.
                const deleted = await deleteBranch(projectId, detail.branch, detail.branchProtected ?? false)
                if (deleted) {
                    handleClose()
                    detail.onSuccess?.()
                }
            } finally {
                setDeleting(false)
            }
        })
    }, [detail, projectId, handleClose, runWithCommitInfo])

    const unsafe = modified || !mergedIntoMain || !!detail?.lastBranch

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
                    <Button key="delete" danger disabled={loading} loading={deleting} onClick={handleDelete} type="primary">
                        {unsafe
                            ? t('repository:delete_branch.confirm_button_unsafe')
                            : t('repository:delete_branch.confirm_button')}
                    </Button>,
                ]}
                title={
                    <Space>
                        <BranchesOutlined />
                        {t('repository:delete_branch.title')}
                    </Space>
                }
            >
                <Spin spinning={loading}>
                    <Typography.Paragraph>
                        {t('repository:delete_branch.confirm', { branch: detail?.branch })}
                    </Typography.Paragraph>
                    {detail?.branchProtected && (
                        <Alert
                            showIcon
                            data-testid="delete-branch-protected-warning"
                            style={{ marginBottom: 8 }}
                            title={t('repository:delete_branch.protected_warning')}
                            type="warning"
                        />
                    )}
                    {detail?.lastBranch && (
                        <Alert
                            showIcon
                            data-testid="delete-branch-last-branch-warning"
                            style={{ marginBottom: 8 }}
                            title={t('repository:delete_branch.last_branch_warning')}
                            type="warning"
                        />
                    )}
                    {modified && (
                        <Alert
                            showIcon
                            style={{ marginBottom: 8 }}
                            title={t('repository:delete_branch.modified_warning')}
                            type="warning"
                        />
                    )}
                    {!mergedIntoMain && (
                        <Alert
                            showIcon
                            type="warning"
                            title={t('repository:delete_branch.not_merged_warning', {
                                branch: detail?.branch,
                                mainBranch: detail?.mainBranch,
                            })}
                        />
                    )}
                </Spin>
            </Modal>
            {commitInfoModal}
        </>
    )
}

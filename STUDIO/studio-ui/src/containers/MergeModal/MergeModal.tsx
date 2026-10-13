import React, { useCallback, useEffect, useState } from 'react'
import { App, Modal, Typography } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useCommitInfoGuard, useGlobalEvents } from '../../hooks'
import { apiCall, NotFoundError } from '../../services'
import { MergeBranchesStep } from './MergeBranchesStep'
import { ConflictResolutionStep } from './ConflictResolutionStep'
import { MergeSummaryStep } from './MergeSummaryStep'
import {
    BranchInfo,
    ConflictDetails,
    ConflictGroup,
    MergeModalDetail,
    MergeResultResponse,
    MergeStep,
    MergeSummary,
} from './types'
import { openConflictCompareWindow } from '../projects/compare'

/**
 * MergeModal component
 * @example to call this modal, dispatch a custom event 'openMergeModal' with details:
 * window.dispatchEvent(new CustomEvent('openMergeModal', {detail: {...}}))
 */
export const MergeModal: React.FC = () => {
    const { notification } = App.useApp()
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<MergeModalDetail>('openMergeModal')
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()

    // Modal state
    const [visible, setVisible] = useState(false)
    const [currentStep, setCurrentStep] = useState<MergeStep>('branches')
    // A running merge keeps the dialog open: it is left through its Cancel button, which stops the merge.
    const [merging, setMerging] = useState(false)

    // Conflict state
    const [conflictGroups, setConflictGroups] = useState<ConflictGroup[]>([])
    // The branch list the opener seeded, kept in state so a list widened inside the branches step also
    // carries its marks into the conflict step.
    const [branches, setBranches] = useState<BranchInfo[]>([])
    // What the merge changed, shown once it is committed; resolved tells a merge completed by resolving conflicts.
    const [summary, setSummary] = useState<{ value: MergeSummary, resolved: boolean } | null>(null)

    // Check for existing conflicts when modal opens
    // 404 means no conflicts - this is expected, so we suppress error pages
    const checkExistingConflicts = useCallback(async (projectId: string) => {
        try {
            const conflictDetails: ConflictDetails = await apiCall(
                `/projects/${projectId}/merge/conflicts`,
                { method: 'GET' },
                { throwError: true, suppressErrorPages: true }
            )
            // If there are existing conflicts, go directly to conflict resolution
            if (conflictDetails?.conflictGroups && conflictDetails.conflictGroups.length > 0) {
                setConflictGroups(conflictDetails.conflictGroups)
                setCurrentStep('conflicts')
                return true
            }
        } catch (err) {
            // 404 (NotFoundError) means no conflicts - this is expected
            if (err instanceof NotFoundError) {
                return false
            }
            // Other errors - stay on branches step
        }
        return false
    }, [])

    // Open modal when detail is received
    useEffect(() => {
        const hasDetails = !!(detail && Object.keys(detail).length > 0)
        setVisible(hasDetails)
        // A summary is let go with the dialog: it may list thousands of files.
        setSummary(null)
        if (hasDetails) {
            // Reset state when modal opens
            setCurrentStep('branches')
            setConflictGroups([])
            setBranches(detail.branches)
            // Check for existing unresolved conflicts
            void checkExistingConflicts(detail.projectId).then((hasConflicts) => {
                if (!hasConflicts && detail.initialStep !== 'conflicts') {
                    // No conflicts found — stay on branches step (already set)
                } else if (!hasConflicts && detail.initialStep === 'conflicts') {
                    // Was opened for conflict resolution but conflicts not found — close
                    setVisible(false)
                }
            })
        }
    }, [detail, checkExistingConflicts])

    const handleClose = useCallback(() => {
        // Dispatch event to notify RichFaces. The same event is consumed by
        // `useGlobalEvents` above, which resets `detail` to null; that drives
        // the close-effect (sets `visible` to false). Resetting `detail` before
        // `visible` avoids rendering stale step content inside Ant Design's
        // leave animation (visible to users in unit tests where CSS animations
        // don't tick and the motion never "ends").
        window.dispatchEvent(new CustomEvent('openMergeModal', { detail: null }))
    }, [])

    /**
     * Ends a merge that is committed: the dialog sums up what it changed. When the server tells nothing, as for the
     * conflicts of a save, a notification confirms it and the dialog closes. The opener is told at once, so the
     * screens behind the dialog read the merged project meanwhile.
     */
    const completeMerge = useCallback((mergeSummary: MergeSummary | undefined, resolved: boolean) => {
        if (mergeSummary) {
            setSummary({ value: mergeSummary, resolved })
        } else {
            notification.success(resolved
                ? {
                    title: t('merge:notifications.resolve_success'),
                    description: t('merge:notifications.resolve_success_description'),
                }
                : {
                    title: t('merge:notifications.merge_success'),
                    description: t('merge:notifications.merge_success_description'),
                })
            handleClose()
        }

        // Call the success callback passed from RichFaces
        detail?.onSuccess?.()
    }, [t, notification, handleClose, detail])

    const handleMergeSuccess = useCallback((mergeSummary?: MergeSummary) => {
        completeMerge(mergeSummary, false)
    }, [completeMerge])

    const handleMergeConflicts = useCallback((result: MergeResultResponse) => {
        setConflictGroups(result.conflictGroups)
        setCurrentStep('conflicts')
    }, [])

    const handleResolveSuccess = useCallback((mergeSummary?: MergeSummary) => {
        completeMerge(mergeSummary, true)
    }, [completeMerge])

    const handleCancelMerge = useCallback(() => {
        handleClose()
    }, [handleClose])

    // The comparison opens in a window of its own, which reads the two versions itself - the same
    // window wherever the merge was started from.
    const handleCompare = useCallback((filePath: string) => {
        if (detail) {
            openConflictCompareWindow(detail.projectId, filePath)
        }
    }, [detail])

    const handleCheckCommitInfo = useCallback((callback: () => void) => {
        void runWithCommitInfo(callback)
    }, [runWithCommitInfo])

    // A committed merge is summed up, whichever step it was committed from.
    const step = summary ? 'summary' : currentStep

    // Determine modal title based on step
    const modalTitle = {
        branches: t('merge:title', { projectName: detail?.projectName }),
        conflicts: t('merge:conflicts.title'),
        summary: t('merge:summary.title'),
    }[step]

    // The conflicts and the summary give each file a row of its own with its actions, so they are wide. The branches
    // step keeps the width of its form: a long path there is shortened in the middle, keeping the name of the file.
    const modalWidth = step === 'branches' ? 600 : 1000

    return (
        <>
            <Modal
                destroyOnHidden
                closable={!merging}
                footer={null}
                keyboard={!merging}
                mask={{ closable: step === 'branches' && !merging }}
                onCancel={handleClose}
                open={visible}
                width={modalWidth}
                title={
                    <div style={{ display: 'flex', alignItems: 'center', minWidth: 0 }}>
                        <BranchesOutlined style={{ marginRight: 8, flexShrink: 0 }} />
                        <Typography.Text ellipsis={{ tooltip: modalTitle }}>
                            {modalTitle}
                        </Typography.Text>
                    </div>
                }
            >
                {detail && step === 'branches' && (
                    <MergeBranchesStep
                        branches={detail.branches}
                        currentBranch={detail.currentBranch}
                        onBranchesWidened={setBranches}
                        onCheckCommitInfo={handleCheckCommitInfo}
                        onMergeConflicts={handleMergeConflicts}
                        onMergeSuccess={handleMergeSuccess}
                        onMergingChange={setMerging}
                        projectId={detail.projectId}
                        projectName={detail.projectName}
                        repositoryType={detail.repositoryType}
                        {...(detail.targetBranch ? { targetBranch: detail.targetBranch } : {})}
                    />
                )}
                {detail && summary && (
                    <MergeSummaryStep onClose={handleClose} resolved={summary.resolved} summary={summary.value} />
                )}
                {detail && step === 'conflicts' && (
                    <ConflictResolutionStep
                        branches={branches}
                        conflictGroups={conflictGroups}
                        onCancel={handleCancelMerge}
                        onCompare={handleCompare}
                        onResolveSuccess={handleResolveSuccess}
                        projectId={detail.projectId}
                    />
                )}
            </Modal>
            {commitInfoModal}
        </>
    )
}

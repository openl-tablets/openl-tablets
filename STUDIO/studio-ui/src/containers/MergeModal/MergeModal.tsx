import React, { useCallback, useEffect, useState } from 'react'
import { Modal, notification } from 'antd'
import { BranchesOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useGlobalEvents } from '../../hooks'
import { apiCall, NotFoundError } from '../../services'
import { useUserStore } from 'store'
import { MergeBranchesStep } from './MergeBranchesStep'
import { ConflictResolutionStep } from './ConflictResolutionStep'
import { CommitInfoModal } from './CommitInfoModal'
import { ConflictDetails, ConflictGroup, MergeModalDetail, MergeResultResponse, MergeStep } from './types'

/**
 * MergeModal component
 * @example to call this modal, dispatch a custom event 'openMergeModal' with details:
 * window.dispatchEvent(new CustomEvent('openMergeModal', {detail: {...}}))
 */
export const MergeModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<MergeModalDetail>('openMergeModal')
    const { userProfile } = useUserStore()

    // Modal state
    const [visible, setVisible] = useState(false)
    const [currentStep, setCurrentStep] = useState<MergeStep>('branches')

    // Conflict state
    const [conflictGroups, setConflictGroups] = useState<ConflictGroup[]>([])

    // Sub-modal states
    const [commitInfoModalVisible, setCommitInfoModalVisible] = useState(false)
    const [pendingMergeCallback, setPendingMergeCallback] = useState<(() => void) | null>(null)

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
        if (hasDetails) {
            // Reset state when modal opens
            setCurrentStep('branches')
            setConflictGroups([])
            // Check for existing unresolved conflicts
            checkExistingConflicts(detail.projectId)
        }
    }, [detail, checkExistingConflicts])

    const handleClose = useCallback(() => {
        setVisible(false)
        // Dispatch event to notify RichFaces
        window.dispatchEvent(new CustomEvent('openMergeModal', { detail: null }))
    }, [])

    const handleMergeSuccess = useCallback(() => {
        notification.success({
            message: t('merge:notifications.merge_success'),
            description: t('merge:notifications.merge_success_description'),
        })

        handleClose()

        // Call the success callback passed from RichFaces
        detail?.onSuccess?.()
    }, [t, handleClose, detail])

    const handleMergeConflicts = useCallback((result: MergeResultResponse) => {
        setConflictGroups(result.conflictGroups)
        setCurrentStep('conflicts')
    }, [])

    const handleResolveSuccess = useCallback(() => {
        handleClose()

        // Call the success callback passed from RichFaces
        detail?.onSuccess?.()
    }, [handleClose, detail])

    const handleCancelMerge = useCallback(() => {
        handleClose()
    }, [handleClose])

    const handleCompare = useCallback((filePath: string) => {
        // Use external compare dialog (RichFaces integration)
        detail?.onCompare?.(filePath)
    }, [detail])

    // Check commit info before merge (for Git repositories)
    const handleCheckCommitInfo = useCallback(async (callback: () => void) => {
        const username = userProfile?.username
        if (!username) {
            callback()
            return
        }

        try {
            // Check if user has commit info configured
            const userInfo = await apiCall(
                `/users/${encodeURIComponent(username)}`,
                { method: 'GET' },
                true
            )

            // If user has both displayName and email, proceed with merge
            if (userInfo?.displayName && userInfo?.email) {
                callback()
            } else {
                // Show commit info modal
                setPendingMergeCallback(() => callback)
                setCommitInfoModalVisible(true)
            }
        } catch (_err) {
            // User info doesn't exist, show modal
            setPendingMergeCallback(() => callback)
            setCommitInfoModalVisible(true)
        }
    }, [userProfile?.username])

    const handleCommitInfoSave = useCallback(() => {
        setCommitInfoModalVisible(false)
        // Execute pending merge callback
        if (pendingMergeCallback) {
            pendingMergeCallback()
            setPendingMergeCallback(null)
        }
    }, [pendingMergeCallback])

    const handleCommitInfoCancel = useCallback(() => {
        setCommitInfoModalVisible(false)
        setPendingMergeCallback(null)
    }, [])

    // Determine modal title based on step
    const modalTitle = currentStep === 'branches'
        ? t('merge:title', { projectName: detail?.projectName })
        : t('merge:conflicts.title')

    // Determine modal width based on step
    const modalWidth = currentStep === 'branches' ? 600 : 1000

    return (
        <>
            <Modal
                destroyOnClose
                footer={null}
                maskClosable={currentStep === 'branches'}
                onCancel={handleClose}
                open={visible}
                width={modalWidth}
                title={
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <BranchesOutlined style={{ marginRight: 8 }} />
                        {modalTitle}
                    </div>
                }
            >
                {detail && currentStep === 'branches' && (
                    <MergeBranchesStep
                        branches={detail.branches}
                        currentBranch={detail.currentBranch}
                        onCheckCommitInfo={handleCheckCommitInfo}
                        onMergeConflicts={handleMergeConflicts}
                        onMergeSuccess={handleMergeSuccess}
                        projectId={detail.projectId}
                        projectName={detail.projectName}
                        repositoryType={detail.repositoryType}
                    />
                )}
                {detail && currentStep === 'conflicts' && (
                    <ConflictResolutionStep
                        conflictGroups={conflictGroups}
                        onCancel={handleCancelMerge}
                        onCompare={handleCompare}
                        onResolveSuccess={handleResolveSuccess}
                        projectId={detail.projectId}
                    />
                )}
            </Modal>
            {/* Commit Info Modal */}
            <CommitInfoModal
                onCancel={handleCommitInfoCancel}
                onSave={handleCommitInfoSave}
                username={userProfile?.username || ''}
                visible={commitInfoModalVisible}
            />
        </>
    )
}

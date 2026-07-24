import { useCallback, useRef, useState } from 'react'
import { apiCall } from '../services'
import { useUserStore } from 'store'
import { CommitInfoModal } from '../containers/MergeModal/CommitInfoModal'

type RepositoryAction = () => void | Promise<void>

interface UserCommitInfo {
    displayName?: string
    email?: string
}

const isCommitInfoFilled = (userInfo?: UserCommitInfo): boolean =>
    Boolean(userInfo?.displayName?.trim() && userInfo?.email?.trim())

/**
 * Guards repository-changing actions with the same Git author profile requirement as legacy Studio.
 */
export const useCommitInfoGuard = () => {
    const { userProfile, fetchUserProfile } = useUserStore()
    const [visible, setVisible] = useState(false)
    const [pendingAction, setPendingAction] = useState<RepositoryAction | null>(null)
    const [busy, setBusy] = useState(false)
    const runningRef = useRef(false)

    const username = userProfile?.username

    const runGuarded = useCallback(async (action: RepositoryAction) => {
        if (!username || isCommitInfoFilled(userProfile)) {
            await action()
            return
        }

        try {
            const userInfo = await apiCall(
                `/users/${encodeURIComponent(username)}`,
                { method: 'GET' },
                { throwError: true }
            ) as UserCommitInfo
            if (isCommitInfoFilled(userInfo)) {
                await action()
                return
            }
        } catch {
            // Missing user info is resolved by the modal below.
        }

        setPendingAction(() => action)
        setVisible(true)
    }, [userProfile, username])

    /**
     * Runs the action once the commit identity is known, and refuses to start a second one while the
     * first is still running: checking the identity is a round trip of its own, and the action behind it
     * writes to the repository, so a second click would commit twice.
     */
    const runWithCommitInfo = useCallback(async (action: RepositoryAction) => {
        if (runningRef.current) {
            return
        }
        runningRef.current = true
        setBusy(true)
        try {
            await runGuarded(action)
        } finally {
            runningRef.current = false
            setBusy(false)
        }
    }, [runGuarded])

    const close = useCallback(() => {
        setVisible(false)
        setPendingAction(null)
    }, [])

    const save = useCallback(() => {
        setVisible(false)
        const action = pendingAction
        setPendingAction(null)
        void fetchUserProfile?.()
        if (action) {
            void action()
        }
    }, [fetchUserProfile, pendingAction])

    return {
        runWithCommitInfo,
        /** Whether a guarded action is running: the caller keeps its button busy for the whole of it. */
        busy,
        commitInfoModal: visible ? (
            <CommitInfoModal
                onCancel={close}
                onSave={save}
                username={username ?? ''}
                visible={visible}
            />
        ) : null,
    }
}

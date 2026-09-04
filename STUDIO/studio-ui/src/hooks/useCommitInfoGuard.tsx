import { useCallback, useRef, useState } from 'react'
import { apiCall } from '../services'
import { useUserStore } from 'store'
import { UserProfileCompletionModal } from 'containers/users/UserProfileCompletionModal'
import { isUserProfileComplete, type UserIdentity } from 'utils/userProfile'

type RepositoryAction = () => void | Promise<void>

/**
 * Keeps repository-changing actions safe if the user profile becomes incomplete after login.
 */
export const useCommitInfoGuard = () => {
    const { userProfile, fetchUserProfile } = useUserStore()
    const [visible, setVisible] = useState(false)
    const [pendingAction, setPendingAction] = useState<RepositoryAction | null>(null)
    const [busy, setBusy] = useState(false)
    const runningRef = useRef(false)

    const username = userProfile?.username

    const runGuarded = useCallback(async (action: RepositoryAction) => {
        if (!username || isUserProfileComplete(userProfile)) {
            await action()
            return
        }

        try {
            const userInfo = await apiCall(
                `/users/${encodeURIComponent(username)}`,
                { method: 'GET' },
                { throwError: true }
            ) as UserIdentity
            if (isUserProfileComplete(userInfo)) {
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

    const save = useCallback(async () => {
        // The profile is now complete, so refresh it regardless of how the resumed action turns out.
        void fetchUserProfile?.()
        const action = pendingAction
        if (action) {
            // Await so a repository failure surfaces through the modal; keep the pending action for a retry.
            await action()
        }
        setPendingAction(null)
        setVisible(false)
    }, [fetchUserProfile, pendingAction])

    return {
        runWithCommitInfo,
        /** Whether a guarded action is running: the caller keeps its button busy for the whole of it. */
        busy,
        commitInfoModal: visible && userProfile ? (
            <UserProfileCompletionModal
                onCancel={close}
                onSave={save}
                open={visible}
                profile={userProfile}
            />
        ) : null,
    }
}

import { useCallback, useState } from 'react'
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

    const username = userProfile?.username

    const runWithCommitInfo = useCallback(async (action: RepositoryAction) => {
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

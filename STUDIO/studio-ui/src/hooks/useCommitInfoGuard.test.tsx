import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCommitInfoGuard } from './useCommitInfoGuard'

const { apiCallMock, fetchUserProfileMock, userProfileMock } = vi.hoisted(() => ({
    apiCallMock: vi.fn(),
    fetchUserProfileMock: vi.fn(),
    userProfileMock: {
        current: {
            username: 'jane',
            firstName: '',
            lastName: '',
            displayName: '',
            email: '',
        } as {
            username?: string
            firstName?: string
            lastName?: string
            displayName?: string
            email?: string
        },
    },
}))

vi.mock('../services', () => ({
    apiCall: (...args: unknown[]) => apiCallMock(...args),
}))

vi.mock('store', () => ({
    useUserStore: () => ({
        userProfile: userProfileMock.current,
        fetchUserProfile: fetchUserProfileMock,
    }),
}))

vi.mock('../containers/users/UserProfileCompletionModal', () => ({
    UserProfileCompletionModal: ({
        onCancel,
        onSave,
        open,
    }: {
        onCancel: () => void
        onSave: () => void | Promise<void>
        open: boolean
    }) => open ? (
        <div data-testid="commit-info-modal">
            <button data-testid="commit-info-cancel" onClick={onCancel} type="button">Cancel</button>
            {/* Mirror the real modal: await onSave and swallow its rejection so the modal can show the error. */}
            <button data-testid="commit-info-save" onClick={() => { void Promise.resolve(onSave()).catch(() => {}) }} type="button">Save</button>
        </div>
    ) : null,
}))

const Harness = ({ onRun }: { onRun: (run: (action: () => void | Promise<void>) => Promise<void>) => void }) => {
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    onRun(runWithCommitInfo)
    return <>{commitInfoModal}</>
}

describe('useCommitInfoGuard', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        userProfileMock.current = {
            username: 'jane',
            firstName: '',
            lastName: '',
            displayName: '',
            email: '',
        }
        apiCallMock.mockResolvedValue({ firstName: '', lastName: '', displayName: '', email: '' })
    })

    it('runs the action immediately when commit info is already filled', async () => {
        userProfileMock.current = {
            username: 'jane',
            firstName: 'Jane',
            lastName: 'Doe',
            displayName: 'Jane Doe',
            email: 'jane@example.com',
        }
        const action = vi.fn()
        let run!: (action: () => void | Promise<void>) => Promise<void>

        render(<Harness onRun={value => { run = value }} />)

        await act(async () => {
            await run(action)
        })

        expect(action).toHaveBeenCalledTimes(1)
        expect(screen.queryByTestId('commit-info-modal')).toBeNull()
    })

    it('opens the modal and resumes the pending action after save', async () => {
        const action = vi.fn()
        let run!: (action: () => void | Promise<void>) => Promise<void>

        render(<Harness onRun={value => { run = value }} />)

        await act(async () => {
            await run(action)
        })

        expect(screen.getByTestId('commit-info-modal')).toBeTruthy()
        expect(action).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('commit-info-save'))

        await waitFor(() => expect(action).toHaveBeenCalledTimes(1))
        expect(fetchUserProfileMock).toHaveBeenCalled()
        expect(screen.queryByTestId('commit-info-modal')).toBeNull()
    })

    it('keeps the modal open and retains the action when the resumed action fails', async () => {
        const action = vi.fn()
            .mockRejectedValueOnce(new Error('repository failure'))
            .mockResolvedValueOnce(undefined)
        let run!: (action: () => void | Promise<void>) => Promise<void>

        render(<Harness onRun={value => { run = value }} />)

        await act(async () => {
            await run(action)
        })

        // First attempt fails: the modal must stay open so the failure is visible.
        await userEvent.click(screen.getByTestId('commit-info-save'))
        await waitFor(() => expect(action).toHaveBeenCalledTimes(1))
        expect(fetchUserProfileMock).toHaveBeenCalled()
        expect(screen.getByTestId('commit-info-modal')).toBeTruthy()

        // Retry succeeds: the pending action was retained, and the modal closes.
        await userEvent.click(screen.getByTestId('commit-info-save'))
        await waitFor(() => expect(screen.queryByTestId('commit-info-modal')).toBeNull())
        expect(action).toHaveBeenCalledTimes(2)
    })

    it('drops the pending action when the modal is cancelled', async () => {
        const action = vi.fn()
        let run!: (action: () => void | Promise<void>) => Promise<void>

        render(<Harness onRun={value => { run = value }} />)

        await act(async () => {
            await run(action)
        })

        await userEvent.click(screen.getByTestId('commit-info-cancel'))

        expect(action).not.toHaveBeenCalled()
        expect(screen.queryByTestId('commit-info-modal')).toBeNull()
    })
})

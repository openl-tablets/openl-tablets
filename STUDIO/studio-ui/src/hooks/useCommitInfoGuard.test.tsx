import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useCommitInfoGuard } from './useCommitInfoGuard'

const { apiCallMock, fetchUserProfileMock, userProfileMock } = vi.hoisted(() => ({
    apiCallMock: vi.fn(),
    fetchUserProfileMock: vi.fn(),
    userProfileMock: { current: { username: 'jane', displayName: '', email: '' } as { username?: string, displayName?: string, email?: string } },
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

vi.mock('../containers/MergeModal/CommitInfoModal', () => ({
    CommitInfoModal: ({
        onCancel,
        onSave,
        visible,
    }: {
        onCancel: () => void
        onSave: () => void
        visible: boolean
    }) => visible ? (
        <div data-testid="commit-info-modal">
            <button data-testid="commit-info-cancel" onClick={onCancel} type="button">Cancel</button>
            <button data-testid="commit-info-save" onClick={onSave} type="button">Save</button>
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
        userProfileMock.current = { username: 'jane', displayName: '', email: '' }
        apiCallMock.mockResolvedValue({ displayName: '', email: '' })
    })

    it('runs the action immediately when commit info is already filled', async () => {
        userProfileMock.current = { username: 'jane', displayName: 'Jane', email: 'jane@example.com' }
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

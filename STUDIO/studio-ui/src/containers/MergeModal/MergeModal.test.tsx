import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import { MergeModal } from 'containers/MergeModal/MergeModal'
import * as services from 'services'
import { MergeModalDetail } from 'containers/MergeModal/types'
import type { MockedFunction, Mock } from 'vitest'

// --- Mocks ---

vi.mock('services', () => {
    class MockNotFoundError extends Error {
        status = 404
        constructor(message = 'Not Found') {
            super(message)
            this.name = 'NotFoundError'
        }
    }
    return {
        apiCall: vi.fn(),
        NotFoundError: MockNotFoundError,
    }
})

vi.mock('store', () => ({
    useUserStore: () => ({ userProfile: { username: 'testuser' } }),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, params?: Record<string, string>) => {
            if (params?.['projectName']) return `${key}:${params['projectName']}`
            return key
        },
        i18n: { language: 'en' },
    }),
}))

// Capture props passed to child components
const mergeBranchesStepProps = vi.fn()
const conflictResolutionStepProps = vi.fn()

vi.mock('containers/MergeModal/MergeBranchesStep', () => ({
    MergeBranchesStep: (props: any) => {
        mergeBranchesStepProps(props)
        return <div data-testid="merge-branches-step" />
    },
}))

vi.mock('containers/MergeModal/ConflictResolutionStep', () => ({
    ConflictResolutionStep: (props: any) => {
        conflictResolutionStepProps(props)
        return <div data-testid="conflict-resolution-step" />
    },
}))

vi.mock('containers/MergeModal/CommitInfoModal', () => ({
    CommitInfoModal: (props: any) => (
        <div
            data-testid="commit-info-modal"
            data-username={props.username}
            data-visible={props.visible}
        />
    ),
}))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>
const MockNotFoundError = (services as any).NotFoundError

// --- Helpers ---

const createDetail = (overrides?: Partial<MergeModalDetail>): MergeModalDetail => ({
    projectId: 'proj-1',
    projectName: 'MyProject',
    repositoryId: 'repo-1',
    repositoryType: 'repo-git',
    currentBranch: 'main',
    branches: [
        { name: 'main', protected: false },
        { name: 'feature', protected: false },
    ],
    onSuccess: vi.fn(),
    onCompare: vi.fn(),
    ...overrides,
})

const dispatchOpenModal = async (detail: MergeModalDetail | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openMergeModal', { detail }))
    })
}

const getLatestProps = (spy: Mock) =>
    spy.mock.calls[spy.mock.calls.length - 1]![0]

// --- Tests ---

describe('MergeModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        // Default: no existing conflicts (404)
        mockApiCall.mockRejectedValue(new MockNotFoundError())
    })

    it('does not render modal content when no event dispatched', () => {
        render(<MergeModal />)
        expect(screen.queryByTestId('merge-branches-step')).not.toBeInTheDocument()
    })

    it('opens modal and shows MergeBranchesStep when event dispatched', async () => {
        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
        })
    })

    it('passes correct props to MergeBranchesStep', async () => {
        const detail = createDetail()
        render(<MergeModal />)
        await dispatchOpenModal(detail)

        await waitFor(() => {
            expect(mergeBranchesStepProps).toHaveBeenCalled()
        })

        const props = getLatestProps(mergeBranchesStepProps)
        expect(props.projectId).toBe('proj-1')
        expect(props.projectName).toBe('MyProject')
        expect(props.currentBranch).toBe('main')
        expect(props.branches).toEqual(detail.branches)
        expect(props.repositoryType).toBe('repo-git')
    })

    it('displays branch title with project name', async () => {
        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByText('merge:title:MyProject')).toBeInTheDocument()
        })
    })

    it('closes modal when event with null detail dispatched', async () => {
        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
        })

        await dispatchOpenModal(null)

        await waitFor(() => {
            expect(screen.queryByTestId('merge-branches-step')).not.toBeInTheDocument()
        })
    })

    it('checks for existing conflicts when modal opens', async () => {
        mockApiCall.mockRejectedValue(new MockNotFoundError())

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/conflicts',
                { method: 'GET' },
                { throwError: true, suppressErrorPages: true }
            )
        })
    })

    it('shows ConflictResolutionStep when existing conflicts found', async () => {
        mockApiCall.mockResolvedValue({
            conflictGroups: [
                { projectName: 'MyProject', projectPath: 'test', files: ['Main.xlsx']},
            ],
        })

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('conflict-resolution-step')).toBeInTheDocument()
        })
        expect(screen.queryByTestId('merge-branches-step')).not.toBeInTheDocument()
    })

    it('shows conflicts title when in conflict step', async () => {
        mockApiCall.mockResolvedValue({
            conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f']}],
        })

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByText('merge:conflicts.title')).toBeInTheDocument()
        })
    })

    it('stays on branches step when no existing conflicts (404)', async () => {
        mockApiCall.mockRejectedValue(new MockNotFoundError())

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
        })
        expect(screen.queryByTestId('conflict-resolution-step')).not.toBeInTheDocument()
    })

    it('stays on branches step when conflict check returns empty groups', async () => {
        mockApiCall.mockResolvedValue({ conflictGroups: []})

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
        })
    })

    it('stays on branches step when conflict check returns non-404 error', async () => {
        mockApiCall.mockRejectedValue(new Error('Server error'))

        render(<MergeModal />)
        await dispatchOpenModal(createDetail())

        await waitFor(() => {
            expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
        })
    })

    describe('onMergeConflicts callback', () => {
        it('switches to conflict resolution step', async () => {
            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(mergeBranchesStepProps).toHaveBeenCalled()
            })

            const { onMergeConflicts } = getLatestProps(mergeBranchesStepProps)
            act(() => {
                onMergeConflicts({
                    status: 'conflicts',
                    conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['a.xlsx']}],
                })
            })

            await waitFor(() => {
                expect(screen.getByTestId('conflict-resolution-step')).toBeInTheDocument()
            })
        })
    })

    describe('onMergeSuccess callback', () => {
        it('closes modal and calls onSuccess from detail', async () => {
            const detail = createDetail()
            render(<MergeModal />)
            await dispatchOpenModal(detail)

            await waitFor(() => {
                expect(mergeBranchesStepProps).toHaveBeenCalled()
            })

            const { onMergeSuccess } = getLatestProps(mergeBranchesStepProps)
            act(() => {
                onMergeSuccess()
            })

            await waitFor(() => {
                expect(detail.onSuccess).toHaveBeenCalled()
            })
        })
    })

    describe('handleCheckCommitInfo', () => {
        it('calls callback directly when user has displayName and email', async () => {
            mockApiCall
                .mockRejectedValueOnce(new MockNotFoundError()) // conflict check
                .mockResolvedValueOnce({ displayName: 'Test User', email: 'test@example.com' }) // user info

            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(mergeBranchesStepProps).toHaveBeenCalled()
            })

            const { onCheckCommitInfo } = getLatestProps(mergeBranchesStepProps)
            const callback = vi.fn()
            await act(async () => {
                await onCheckCommitInfo(callback)
            })

            expect(callback).toHaveBeenCalled()
        })

        it('shows CommitInfoModal when user has no displayName', async () => {
            mockApiCall
                .mockRejectedValueOnce(new MockNotFoundError()) // conflict check
                .mockResolvedValueOnce({ displayName: '', email: 'test@example.com' }) // user info

            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(mergeBranchesStepProps).toHaveBeenCalled()
            })

            const { onCheckCommitInfo } = getLatestProps(mergeBranchesStepProps)
            const callback = vi.fn()
            await act(async () => {
                await onCheckCommitInfo(callback)
            })

            expect(callback).not.toHaveBeenCalled()
            expect(screen.getByTestId('commit-info-modal')).toHaveAttribute('data-visible', 'true')
        })

        it('shows CommitInfoModal when user info fetch fails', async () => {
            mockApiCall
                .mockRejectedValueOnce(new MockNotFoundError()) // conflict check
                .mockRejectedValueOnce(new Error('User not found')) // user info

            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(mergeBranchesStepProps).toHaveBeenCalled()
            })

            const { onCheckCommitInfo } = getLatestProps(mergeBranchesStepProps)
            const callback = vi.fn()
            await act(async () => {
                await onCheckCommitInfo(callback)
            })

            expect(callback).not.toHaveBeenCalled()
            expect(screen.getByTestId('commit-info-modal')).toHaveAttribute('data-visible', 'true')
        })

        it('calls callback directly when no username in profile', async () => {
            const storeModule = await import('store')
            const useUserStoreSpy = vi
                .spyOn(storeModule, 'useUserStore')
                .mockReturnValue({ userProfile: { username: null } } as ReturnType<typeof storeModule.useUserStore>)
            let unmount: (() => void) | undefined
            try {
                mockApiCall.mockRejectedValue(new MockNotFoundError())
                ;({ unmount } = render(<MergeModal />))
                await dispatchOpenModal(createDetail())

                await waitFor(() => {
                    expect(mergeBranchesStepProps).toHaveBeenCalled()
                })

                const { onCheckCommitInfo } = getLatestProps(mergeBranchesStepProps)
                const callback = vi.fn()
                await act(async () => {
                    await onCheckCommitInfo(callback)
                })

                expect(callback).toHaveBeenCalled()
            } finally {
                if (unmount) {
                    unmount()
                }
                useUserStoreSpy.mockRestore()
            }
        })
    })

    describe('CommitInfoModal integration', () => {
        it('passes username to CommitInfoModal', async () => {
            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(screen.getByTestId('commit-info-modal')).toHaveAttribute('data-username', 'testuser')
            })
        })
    })

    describe('conflict resolution callbacks', () => {
        const openWithConflicts = async () => {
            mockApiCall.mockResolvedValue({
                conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f']}],
            })
            const detail = createDetail()
            render(<MergeModal />)
            await dispatchOpenModal(detail)

            await waitFor(() => {
                expect(conflictResolutionStepProps).toHaveBeenCalled()
            })
            return detail
        }

        it('onCancel closes modal', async () => {
            await openWithConflicts()

            const { onCancel } = getLatestProps(conflictResolutionStepProps)
            act(() => {
                onCancel()
            })

            await waitFor(() => {
                expect(screen.queryByTestId('conflict-resolution-step')).not.toBeInTheDocument()
            })
        })

        it('onResolveSuccess closes modal and calls onSuccess', async () => {
            const detail = await openWithConflicts()

            const { onResolveSuccess } = getLatestProps(conflictResolutionStepProps)
            act(() => {
                onResolveSuccess()
            })

            await waitFor(() => {
                expect(detail.onSuccess).toHaveBeenCalled()
            })
        })

        it('onCompare calls detail.onCompare with file path', async () => {
            const detail = await openWithConflicts()

            const { onCompare } = getLatestProps(conflictResolutionStepProps)
            act(() => {
                onCompare('Main.xlsx')
            })

            expect(detail.onCompare).toHaveBeenCalledWith('Main.xlsx')
        })
    })

    describe('modal reset on reopen', () => {
        it('resets to branches step when reopened without initialStep', async () => {
            // First open: go to conflicts
            mockApiCall.mockResolvedValueOnce({
                conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f']}],
            })

            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(screen.getByTestId('conflict-resolution-step')).toBeInTheDocument()
            })

            // Close
            await dispatchOpenModal(null)
            await waitFor(() => {
                expect(screen.queryByTestId('conflict-resolution-step')).not.toBeInTheDocument()
            })

            // Reopen: no conflicts this time
            mockApiCall.mockRejectedValueOnce(new MockNotFoundError())
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
            })
        })
    })

    describe('initialStep support (save merge conflicts)', () => {
        it('opens directly in conflicts step when initialStep is conflicts', async () => {
            mockApiCall.mockResolvedValue({
                conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f.xlsx']}],
            })

            render(<MergeModal />)
            await dispatchOpenModal(createDetail({ initialStep: 'conflicts' }))

            await waitFor(() => {
                expect(screen.getByTestId('conflict-resolution-step')).toBeInTheDocument()
            })
            expect(screen.queryByTestId('merge-branches-step')).not.toBeInTheDocument()
        })

        it('shows conflicts title when opened with initialStep conflicts', async () => {
            mockApiCall.mockResolvedValue({
                conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f.xlsx']}],
            })

            render(<MergeModal />)
            await dispatchOpenModal(createDetail({ initialStep: 'conflicts' }))

            await waitFor(() => {
                expect(screen.getByText('merge:conflicts.title')).toBeInTheDocument()
            })
        })

        it('defaults to branches step when initialStep is not provided', async () => {
            mockApiCall.mockRejectedValue(new MockNotFoundError())

            render(<MergeModal />)
            await dispatchOpenModal(createDetail())

            await waitFor(() => {
                expect(screen.getByTestId('merge-branches-step')).toBeInTheDocument()
            })
            expect(screen.queryByTestId('conflict-resolution-step')).not.toBeInTheDocument()
        })

        it('passes conflict groups to ConflictResolutionStep when opened with initialStep', async () => {
            const conflictGroups = [
                { projectName: 'MyProject', projectPath: 'test', files: ['Bank Rating.xlsx']},
            ]
            mockApiCall.mockResolvedValue({ conflictGroups })

            render(<MergeModal />)
            await dispatchOpenModal(createDetail({ initialStep: 'conflicts' }))

            await waitFor(() => {
                expect(conflictResolutionStepProps).toHaveBeenCalled()
            })

            const props = getLatestProps(conflictResolutionStepProps)
            expect(props.projectId).toBe('proj-1')
            expect(props.conflictGroups).toEqual(conflictGroups)
        })

        it('opens with empty branches array for save conflicts', async () => {
            mockApiCall.mockResolvedValue({
                conflictGroups: [{ projectName: 'P', projectPath: 'p', files: ['f.xlsx']}],
            })

            render(<MergeModal />)
            await dispatchOpenModal(createDetail({ initialStep: 'conflicts', branches: []}))

            await waitFor(() => {
                expect(screen.getByTestId('conflict-resolution-step')).toBeInTheDocument()
            })
        })
    })
})

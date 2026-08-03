import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal } from 'antd'
import { MergeBranchesStep } from 'containers/MergeModal/MergeBranchesStep'
import * as services from 'services'
import { BranchInfo, CheckMergeResult, MergeBlockedBy, MergeResultResponse } from 'containers/MergeModal/types'
import type { MockedFunction } from 'vitest'

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return {
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: vi.fn(),
        },
    }
})

vi.mock('services', () => {
    class MockApiHttpError extends Error {
        status: number
        payload?: unknown
        constructor(status: number, message: string, payload?: unknown) {
            super(message)
            this.name = 'ApiHttpError'
            this.status = status
            this.payload = payload
        }
    }
    return {
        apiCall: vi.fn(),
        isApiHttpError: vi.fn((err: unknown) => err instanceof MockApiHttpError),
        ApiHttpError: MockApiHttpError,
    }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

// Mock the shared BranchSelect as a native <select> that reports selection, and render each branch's marks
// beside it — AntD's Select dropdown does not settle in jsdom.
vi.mock('containers/projects/BranchSelect', () => ({
    BranchSelect: ({ value, onChange, branchNames, marksOf, placeholder, ...rest }: any) => (
        <div>
            <select {...rest} onChange={(e) => onChange?.(e.target.value)} value={value ?? ''}>
                <option value="">{placeholder}</option>
                {branchNames.map((name: string) => (
                    <option key={name} value={name}>{name}</option>
                ))}
            </select>
            {branchNames.map((name: string) => {
                const marks = marksOf?.(name) ?? {}
                return (
                    <span key={name}>
                        {marks.isDefault && <i data-testid={`branch-option-${name}-default`} />}
                        {marks.isProtected && <i data-testid={`branch-option-${name}-protected`} />}
                    </span>
                )
            })}
        </div>
    ),
}))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const branches: BranchInfo[] = [
    { name: 'main', protected: false },
    { name: 'feature', protected: false },
    { name: 'release-1.0', protected: true },
]

const defaultProps = () => ({
    projectId: 'proj-1',
    projectName: 'TestProject',
    repositoryType: 'repo-git',
    currentBranch: 'main',
    branches,
    onMergeSuccess: vi.fn(),
    onMergeConflicts: vi.fn(),
    onCheckCommitInfo: vi.fn((cb: () => void) => cb()),
})

const mergeableResult = (source: string, target: string, blockedBy?: MergeBlockedBy): CheckMergeResult => ({
    sourceBranch: source,
    targetBranch: target,
    status: 'mergeable',
    canMerge: !blockedBy,
    ...(blockedBy ? { blockedBy } : {}),
})

const upToDateResult = (source: string, target: string): CheckMergeResult => ({
    sourceBranch: source,
    targetBranch: target,
    status: 'up-to-date',
    canMerge: true,
})

const createApiError = (status: number, message: string, payload?: unknown) =>
    new (services as any).ApiHttpError(status, message, payload)

const BYPASS_CODE = 'openl.error.409.protected.branch.bypass.required'
const createBypassError = (message = 'bypass required') =>
    createApiError(409, message, { code: BYPASS_CODE, message })

const selectBranch = async (branchValue: string) => {
    const select = screen.getByTestId('merge-target-branch')
    await userEvent.selectOptions(select, branchValue)
}

const getButton = (name: RegExp) => screen.getByRole('button', { name })

describe('MergeBranchesStep', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('renders branch selector with current branch', () => {
        render(<MergeBranchesStep {...defaultProps()} />)
        expect(screen.getByText('main')).toBeInTheDocument()
        expect(screen.getByText('merge:branches.current')).toBeInTheDocument()
    })

    it('filters out current branch from options', () => {
        render(<MergeBranchesStep {...defaultProps()} />)
        const options = screen.getAllByRole('option')
        const values = options.map(o => (o as HTMLOptionElement).value)
        expect(values).not.toContain('main')
        expect(values).toContain('feature')
        expect(values).toContain('release-1.0')
    })

    it('marks a protected branch in the options', () => {
        render(<MergeBranchesStep {...defaultProps()} />)
        expect(screen.getByTestId('branch-option-release-1.0-protected')).toBeInTheDocument()
    })

    describe('merge check', () => {
        it('calls check API for both directions when branch selected', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => expect(mockApiCall).toHaveBeenCalledTimes(2))

            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/check',
                expect.objectContaining({
                    body: JSON.stringify({ mode: 'receive', otherBranch: 'feature' }),
                }),
                expect.objectContaining({ throwError: true, suppressErrorPages: true })
            )
            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/check',
                expect.objectContaining({
                    body: JSON.stringify({ mode: 'send', otherBranch: 'feature' }),
                }),
                expect.objectContaining({ throwError: true, suppressErrorPages: true })
            )
        })

        it('checks merge status immediately when a target branch is provided', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} targetBranch="feature" />)

            await waitFor(() => expect(mockApiCall).toHaveBeenCalledTimes(2))

            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/check',
                expect.objectContaining({
                    body: JSON.stringify({ mode: 'receive', otherBranch: 'feature' }),
                }),
                expect.objectContaining({ throwError: true, suppressErrorPages: true })
            )
            expect(mockApiCall).toHaveBeenCalledWith(
                '/projects/proj-1/merge/check',
                expect.objectContaining({
                    body: JSON.stringify({ mode: 'send', otherBranch: 'feature' }),
                }),
                expect.objectContaining({ throwError: true, suppressErrorPages: true })
            )
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())
        })

        it('enables receive button when receive is mergeable', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(upToDateResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())
            expect(getButton(/merge:actions.send/i)).toBeDisabled()
        })

        it('enables send button when send is mergeable', async () => {
            mockApiCall
                .mockResolvedValueOnce(upToDateResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => expect(getButton(/merge:actions.send/i)).not.toBeDisabled())
            expect(getButton(/merge:actions.receive/i)).toBeDisabled()
        })

        it('disables both buttons when both are up-to-date', async () => {
            mockApiCall
                .mockResolvedValueOnce(upToDateResult('feature', 'main'))
                .mockResolvedValueOnce(upToDateResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => {
                expect(getButton(/merge:actions.receive/i)).toBeDisabled()
                expect(getButton(/merge:actions.send/i)).toBeDisabled()
            })
        })

        it('does not call API when no branch selected', () => {
            render(<MergeBranchesStep {...defaultProps()} />)
            expect(mockApiCall).not.toHaveBeenCalled()
        })
    })

    describe('protected branch errors', () => {
        it('shows error and disables send when send check fails with protected branch', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('release-1.0', 'main'))
                .mockRejectedValueOnce(createApiError(409, "Cannot merge into the branch 'release-1.0' because it is protected."))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('release-1.0')

            await waitFor(() => {
                expect(screen.getByText("Cannot merge into the branch 'release-1.0' because it is protected.")).toBeInTheDocument()
            })
            expect(getButton(/merge:actions.receive/i)).not.toBeDisabled()
            expect(getButton(/merge:actions.send/i)).toBeDisabled()
        })

        it('shows error and disables receive when receive check fails with protected branch', async () => {
            mockApiCall
                .mockRejectedValueOnce(createApiError(409, "Cannot merge into the branch 'main' because it is protected."))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => {
                expect(screen.getByText("Cannot merge into the branch 'main' because it is protected.")).toBeInTheDocument()
            })
            expect(getButton(/merge:actions.receive/i)).toBeDisabled()
            expect(getButton(/merge:actions.send/i)).not.toBeDisabled()
        })

        it('deduplicates identical error messages from both directions', async () => {
            const errMsg = 'Cannot merge because the project is not in a valid state for merging.'
            mockApiCall
                .mockRejectedValueOnce(createApiError(409, errMsg))
                .mockRejectedValueOnce(createApiError(409, errMsg))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => expect(screen.getAllByRole('alert')).toHaveLength(1))
            expect(screen.getAllByRole('alert')[0]).toHaveTextContent(errMsg)
        })

        it('shows both errors when receive and send fail with different messages', async () => {
            mockApiCall
                .mockRejectedValueOnce(createApiError(409, 'Receive error'))
                .mockRejectedValueOnce(createApiError(409, 'Send error'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => expect(screen.getAllByRole('alert')).toHaveLength(2))
            expect(screen.getByText('Receive error')).toBeInTheDocument()
            expect(screen.getByText('Send error')).toBeInTheDocument()
        })

        it('shows generic error for non-API errors', async () => {
            mockApiCall
                .mockRejectedValueOnce(new Error('Network error'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            expect(await screen.findByText('merge:errors.check_failed')).toBeInTheDocument()
        })
    })

    describe('merge execution', () => {
        it('calls merge API and triggers onMergeSuccess on success', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())

            mockApiCall.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
            await userEvent.click(getButton(/merge:actions.receive/i))

            await waitFor(() => expect(props.onMergeSuccess).toHaveBeenCalled())
        })

        it('calls onMergeConflicts when merge returns conflicts', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.send/i)).not.toBeDisabled())

            const conflictResponse: MergeResultResponse = {
                status: 'conflicts',
                conflictGroups: [{ projectName: 'TestProject', projectPath: 'test', files: ['Main.xlsx']}],
            }
            mockApiCall.mockResolvedValueOnce(conflictResponse)
            await userEvent.click(getButton(/merge:actions.send/i))

            await waitFor(() => expect(props.onMergeConflicts).toHaveBeenCalledWith(conflictResponse))
        })

        it('shows merge error when merge API fails with protected branch', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.send/i)).not.toBeDisabled())

            mockApiCall.mockRejectedValueOnce(
                createApiError(409, "Cannot merge into the branch 'feature' because it is protected.")
            )
            await userEvent.click(getButton(/merge:actions.send/i))

            await waitFor(() => {
                expect(screen.getByText("Cannot merge into the branch 'feature' because it is protected.")).toBeInTheDocument()
            })
        })

        it('shows generic merge error for non-API errors', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())

            mockApiCall.mockRejectedValueOnce(new Error('Connection failed'))
            await userEvent.click(getButton(/merge:actions.receive/i))

            await screen.findByText('Connection failed')
        })

        it('uses onCheckCommitInfo for git repositories before merging', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())

            mockApiCall.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
            await userEvent.click(getButton(/merge:actions.receive/i))

            await waitFor(() => expect(props.onCheckCommitInfo).toHaveBeenCalled())
        })

        it('skips onCheckCommitInfo for non-git repositories', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            props.repositoryType = 'repo-jdbc'
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())

            mockApiCall.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
            await userEvent.click(getButton(/merge:actions.receive/i))

            await waitFor(() => expect(props.onMergeSuccess).toHaveBeenCalled())
            expect(props.onCheckCommitInfo).not.toHaveBeenCalled()
        })
    })

    describe('a protected target branch', () => {
        it('offers the merge as a bypass when the check says the user may confirm it', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main', 'bypass-required'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await waitFor(() => {
                expect(screen.getByText('merge:bypass.title')).toBeInTheDocument()
            })

            // One question, one request: the check answers it whether or not the user may merge.
            expect(mockApiCall).toHaveBeenCalledTimes(2)
            expect(mockApiCall).toHaveBeenNthCalledWith(
                1,
                '/projects/proj-1/merge/check',
                expect.anything(),
                expect.anything()
            )
            // The merge stays available: the user confirms the bypass themselves.
            const receiveBtn = getButton(/merge:actions.receive/i)
            expect(receiveBtn).not.toBeDisabled()
            // Button styled danger (red) to mirror GitHub's "Confirm bypass rules and merge" UX
            expect(receiveBtn).toHaveClass('ant-btn-dangerous')
            // Send button is unaffected because its check reported no obstacle
            expect(getButton(/merge:actions.send/i)).not.toHaveClass('ant-btn-dangerous')
        })

        it('reports the difference and refuses the merge the user may not perform', async () => {
            mockApiCall
                .mockResolvedValueOnce(upToDateResult('release-1.0', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'release-1.0', 'protected-branch'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('release-1.0')

            await screen.findByTestId('merge-blocked-send')
            expect(screen.getByTestId('merge-blocked-send')).toHaveTextContent('merge:blocked.protected')
            expect(getButton(/merge:actions.send/i)).toBeDisabled()
            // No error alert: the answer arrived, it just says the merge is not for this user.
            expect(screen.queryByText('merge:errors.check_failed')).not.toBeInTheDocument()
        })

        it('says a locked target branch is what blocks the merge', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main', 'locked'))
                .mockResolvedValueOnce(upToDateResult('main', 'feature'))

            render(<MergeBranchesStep {...defaultProps()} />)
            await selectBranch('feature')

            await screen.findByTestId('merge-blocked-receive')
            expect(screen.getByTestId('merge-blocked-receive')).toHaveTextContent('merge:blocked.locked')
            expect(getButton(/merge:actions.receive/i)).toBeDisabled()
        })

        it('opens danger confirm modal and retries merge with force=true on confirm', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.send/i)).not.toBeDisabled())

            // First merge attempt → bypass-required
            mockApiCall.mockRejectedValueOnce(createBypassError())
            await userEvent.click(getButton(/merge:actions.send/i))

            // Confirm modal is invoked with danger styling and the bypass copy
            await waitFor(() => expect(Modal.confirm).toHaveBeenCalled())
            const confirmConfig = vi.mocked(Modal.confirm).mock.calls[0]![0]
            expect(confirmConfig.title).toBe('merge:bypass.title')
            expect(confirmConfig.okText).toBe('merge:bypass.confirm')
            expect(confirmConfig.okButtonProps).toEqual({ danger: true })

            // Confirming retries the merge with force=true
            mockApiCall.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
            await act(async () => {
                await confirmConfig.onOk?.()
            })

            await waitFor(() => {
                expect(mockApiCall).toHaveBeenCalledWith(
                    '/projects/proj-1/merge?force=true',
                    expect.objectContaining({
                        body: JSON.stringify({ mode: 'send', otherBranch: 'feature' }),
                    }),
                    expect.anything()
                )
            })
            await waitFor(() => expect(props.onMergeSuccess).toHaveBeenCalled())
        })

        it('does not retry when user cancels the bypass confirm modal', async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))

            const props = defaultProps()
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.send/i)).not.toBeDisabled())

            mockApiCall.mockRejectedValueOnce(createBypassError())
            await userEvent.click(getButton(/merge:actions.send/i))

            await waitFor(() => expect(Modal.confirm).toHaveBeenCalled())
            const callsAfterFirstAttempt = mockApiCall.mock.calls.length

            // Simulate the user dismissing the modal — onOk is never invoked
            expect(mockApiCall.mock.calls.length).toBe(callsAfterFirstAttempt)
            expect(props.onMergeSuccess).not.toHaveBeenCalled()
        })
    })
})

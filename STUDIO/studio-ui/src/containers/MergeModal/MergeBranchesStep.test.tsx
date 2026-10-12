import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal, notification } from 'antd'
import { MergeBranchesStep } from 'containers/MergeModal/MergeBranchesStep'
import * as services from 'services'
import { getProjectBranches } from '../../services/repositories'
import { mergeBranches } from 'containers/MergeModal/mergeBranches'
import { BranchInfo, CheckMergeResult, MergeBlockedBy, MergeResultResponse } from 'containers/MergeModal/types'
import type { MockedFunction } from 'vitest'

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        Modal: {
            ...actual.Modal,
            confirm: vi.fn(),
        },
        notification: {
            ...actual.notification,
            info: vi.fn(),
        },
    })
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

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
}))

vi.mock('containers/MergeModal/mergeBranches', async importOriginal => ({
    ...await importOriginal<typeof import('containers/MergeModal/mergeBranches')>(),
    mergeBranches: vi.fn(),
}))

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
    BranchSelect: ({ value, onChange, branchNames, marksOf, placeholder, loading, ...rest }: any) => (
        <div>
            <select {...rest} onChange={(e) => onChange?.(e.target.value)} value={value ?? ''}>
                <option value="">{placeholder}</option>
                {branchNames.map((name: string) => (
                    <option key={name} value={name}>{name}</option>
                ))}
            </select>
            {loading && <span data-testid="merge-target-branch-loading" />}
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
const mockMergeBranches = vi.mocked(mergeBranches)

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
        const branchField = screen.getByTestId('merge-target-branch-field')
        expect(branchField).toHaveStyle({ minWidth: 0, width: '100%' })
        expect(branchField.closest('.ant-form-item-control')).toHaveStyle({ minWidth: 0 })
        const targetRow = branchField.closest('.ant-form-item-row')!
        expect(getComputedStyle(targetRow).flexWrap).toBe('nowrap')
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

    it('marks the target field as reading while the repository branches are widened', async () => {
        let resolveBranches!: (branches: BranchInfo[]) => void
        const pending = new Promise<BranchInfo[]>(resolve => {
            resolveBranches = resolve
        })
        vi.mocked(getProjectBranches).mockReturnValue(pending as never)
        // A project living only on its own branch widens the list on open, so the read starts by itself.
        render(<MergeBranchesStep {...defaultProps()} branches={[{ name: 'main', protected: false }]} />)

        await waitFor(() => expect(screen.getByTestId('merge-target-branch-loading')).toBeInTheDocument())

        resolveBranches([{ name: 'main', protected: false }, { name: 'feature', protected: false }])

        await waitFor(() => expect(screen.queryByTestId('merge-target-branch-loading')).not.toBeInTheDocument())
        expect(screen.getByRole('option', { name: 'feature' })).toBeInTheDocument()
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

    describe('cancelling a running merge', () => {
        const startReceive = async () => {
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('feature', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'feature'))
            const props = { ...defaultProps(), repositoryType: 'repo-jdbc', onMergingChange: vi.fn() }
            render(<MergeBranchesStep {...props} />)
            await selectBranch('feature')
            await waitFor(() => expect(getButton(/merge:actions.receive/i)).not.toBeDisabled())
            return props
        }

        /**
         * A merge the test ends when it likes, as a long merge ends when the server says so. The server takes it at
         * once, unless the test says it has not yet.
         *
         * The merge ends with the result the test gives, or fails with the error it gives.
         */
        const runningMerge = (accepted = true) => {
            let end: (outcome: MergeResultResponse | Error) => void = () => undefined
            mockMergeBranches.mockImplementationOnce((_projectId, _mode, _branch, _force, onAccepted) => {
                if (accepted) {
                    onAccepted?.()
                }
                return new Promise((resolve, reject) => {
                    end = outcome => (outcome instanceof Error ? reject(outcome) : resolve(outcome))
                })
            })
            // The merge ends outside React, so its end is resolved inside an act that waits for what follows it.
            return (outcome: MergeResultResponse | Error) => act(async () => {
                end(outcome)
            })
        }

        it('keeps the dialog open while the merge runs', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))

            await waitFor(() => expect(props.onMergingChange).toHaveBeenLastCalledWith(true))

            await end({ status: 'success', conflictGroups: []})

            expect(props.onMergingChange).toHaveBeenLastCalledWith(false)
        })

        it('offers no cancel until a merge runs', async () => {
            await startReceive()

            expect(screen.queryByTestId('merge-cancel')).not.toBeInTheDocument()
        })

        it('offers no cancel until the server has taken the merge', async () => {
            await startReceive()
            const end = runningMerge(false)
            await userEvent.click(getButton(/merge:actions.receive/i))

            // A cancel sent now would find no merge to stop, and the merge would go on.
            expect(getButton(/merge:actions.receive/i)).toBeDisabled()
            expect(screen.queryByTestId('merge-cancel')).not.toBeInTheDocument()

            await end({ status: 'success', conflictGroups: []})
        })

        it('asks the running merge to stop and tells that it was cancelled', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))

            mockApiCall.mockResolvedValueOnce(undefined)
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            expect(mockApiCall).toHaveBeenLastCalledWith('/projects/proj-1/merge/conflicts',
                { method: 'DELETE' },
                expect.objectContaining({ throwError: true }))
            expect(screen.getByTestId('merge-cancel')).toBeDisabled()

            await end({ status: 'cancelled', conflictGroups: []})

            await waitFor(() => expect(vi.mocked(notification.info)).toHaveBeenCalledWith(expect.objectContaining({
                title: 'merge:notifications.merge_cancelled',
            })))
            expect(props.onMergeSuccess).not.toHaveBeenCalled()
            expect(props.onMergeConflicts).not.toHaveBeenCalled()
            expect(screen.queryByTestId('merge-cancel')).not.toBeInTheDocument()
        })

        it('tells a merge cancelled elsewhere that it was cancelled', async () => {
            const props = await startReceive()
            mockMergeBranches.mockResolvedValueOnce({ status: 'cancelled', conflictGroups: []})

            await userEvent.click(getButton(/merge:actions.receive/i))

            await waitFor(() => expect(vi.mocked(notification.info)).toHaveBeenCalled())
            expect(props.onMergeConflicts).not.toHaveBeenCalled()
        })

        it('cancels the conflicts of a merge that ended before the cancel reached it', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))
            mockApiCall.mockRejectedValueOnce(createApiError(404, 'The merge result for the project is not found.'))
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            mockApiCall.mockResolvedValueOnce(undefined)
            await end({
                status: 'conflicts',
                conflictGroups: [{ projectName: 'TestProject', projectPath: 'test', files: ['Main.xlsx']}],
            })

            await waitFor(() => expect(vi.mocked(notification.info)).toHaveBeenCalled())
            expect(mockApiCall.mock.calls.filter(([url]) => url === '/projects/proj-1/merge/conflicts'))
                .toHaveLength(2)
            expect(props.onMergeConflicts).not.toHaveBeenCalled()
        })

        it('tells that the merge was cancelled when the cancel has dropped its result', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))
            mockApiCall.mockResolvedValueOnce(undefined)
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            // The merge ended with conflicts before the cancel reached it, and the cancel dropped them with the result.
            await end(createApiError(404, 'No merge of the project to report'))

            await waitFor(() => expect(vi.mocked(notification.info)).toHaveBeenCalledWith(expect.objectContaining({
                title: 'merge:notifications.merge_cancelled',
            })))
            expect(screen.queryByText('No merge of the project to report')).not.toBeInTheDocument()
            expect(props.onMergeConflicts).not.toHaveBeenCalled()
        })

        it('reports a missing result as an error when no cancel was asked for', async () => {
            await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))

            await end(createApiError(404, 'No merge of the project to report'))

            expect(await screen.findByText('No merge of the project to report')).toBeInTheDocument()
            expect(vi.mocked(notification.info)).not.toHaveBeenCalled()
        })

        it('takes a merge that committed before the cancel reached it as merged', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))
            mockApiCall.mockResolvedValueOnce(undefined)
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            await end({ status: 'success', conflictGroups: []})

            await waitFor(() => expect(props.onMergeSuccess).toHaveBeenCalled())
            expect(vi.mocked(notification.info)).not.toHaveBeenCalled()
        })

        it('lets the cancel be asked again when it fails', async () => {
            await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))

            mockApiCall.mockRejectedValueOnce(createApiError(500, 'The repository is not available'))
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            // The merge goes on, and the user learns why it was not stopped.
            expect(await screen.findByText('The repository is not available')).toBeInTheDocument()
            expect(screen.getByTestId('merge-cancel')).not.toBeDisabled()

            await end({ status: 'success', conflictGroups: []})
        })

        it('shows the conflicts the cancel could not drop for resolution', async () => {
            const props = await startReceive()
            const end = runningMerge()
            await userEvent.click(getButton(/merge:actions.receive/i))
            mockApiCall.mockResolvedValueOnce(undefined)
            await userEvent.click(await screen.findByTestId('merge-cancel'))

            // The merge ended with conflicts before the cancel reached it, and dropping them fails.
            mockApiCall.mockRejectedValueOnce(createApiError(500, 'The session is gone'))
            const conflicts: MergeResultResponse = {
                status: 'conflicts',
                conflictGroups: [{ projectName: 'TestProject', projectPath: 'test', files: ['Main.xlsx']}],
            }
            await end(conflicts)

            await waitFor(() => expect(props.onMergeConflicts).toHaveBeenCalledWith(conflicts))
            expect(vi.mocked(notification.info)).not.toHaveBeenCalled()
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

            mockMergeBranches.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
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
            mockMergeBranches.mockResolvedValueOnce(conflictResponse)
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

            mockMergeBranches.mockRejectedValueOnce(
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

            mockMergeBranches.mockRejectedValueOnce(new Error('Connection failed'))
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

            mockMergeBranches.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
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

            mockMergeBranches.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
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
            mockMergeBranches.mockRejectedValueOnce(createBypassError())
            await userEvent.click(getButton(/merge:actions.send/i))

            // Confirm modal is invoked with danger styling and the bypass copy
            await waitFor(() => expect(Modal.confirm).toHaveBeenCalled())
            const confirmConfig = vi.mocked(Modal.confirm).mock.calls[0]![0]
            expect(confirmConfig.title).toBe('merge:bypass.title')
            expect(confirmConfig.okText).toBe('merge:bypass.confirm')
            expect(confirmConfig.okButtonProps).toEqual({ danger: true })

            // Confirming retries the merge with force=true
            mockMergeBranches.mockResolvedValueOnce({ status: 'success', conflictGroups: []})
            await act(async () => {
                await confirmConfig.onOk?.()
            })

            await waitFor(() => {
                expect(mockMergeBranches).toHaveBeenLastCalledWith('proj-1', 'send', 'feature', true, expect.any(Function))
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

            mockMergeBranches.mockRejectedValueOnce(createBypassError())
            await userEvent.click(getButton(/merge:actions.send/i))

            await waitFor(() => expect(Modal.confirm).toHaveBeenCalled())

            // Simulate the user dismissing the modal — onOk is never invoked
            expect(mockMergeBranches).toHaveBeenCalledTimes(1)
            expect(mockMergeBranches).toHaveBeenCalledWith('proj-1', 'send', 'feature', false, expect.any(Function))
            expect(props.onMergeSuccess).not.toHaveBeenCalled()
        })
    })

    // EPBDS-16411: a project created in its own branch is merged into the base branch, which does not hold
    // it. The short list stays the default so a wrong target is harder to pick; the rest is one click away.
    describe('widening the target list', () => {
        it('does not read the repository branches until the option is turned on', async () => {
            render(<MergeBranchesStep {...defaultProps()} />)

            expect(getProjectBranches).not.toHaveBeenCalled()
            expect(screen.getByRole('checkbox')).not.toBeChecked()
        })

        it('offers a branch that does not hold the project once the option is on', async () => {
            vi.mocked(getProjectBranches).mockResolvedValue([
                { name: 'main' },
                { name: 'feature' },
                { name: 'untouched-by-the-project' },
            ])
            render(<MergeBranchesStep {...defaultProps()} />)

            const targets = () => within(screen.getByTestId('merge-target-branch')).queryAllByRole('option')
                .map(option => (option as HTMLOptionElement).value)
            expect(targets()).not.toContain('untouched-by-the-project')

            await userEvent.click(screen.getByRole('checkbox'))

            await waitFor(() => expect(targets()).toContain('untouched-by-the-project'))
            expect(getProjectBranches).toHaveBeenCalledWith('proj-1', 'repository', expect.any(Object))
        })

        it('reads the repository branches only once', async () => {
            vi.mocked(getProjectBranches).mockResolvedValue([{ name: 'main' }, { name: 'feature' }])
            render(<MergeBranchesStep {...defaultProps()} />)

            const option = screen.getByRole('checkbox')
            await userEvent.click(option)
            await waitFor(() => expect(getProjectBranches).toHaveBeenCalledTimes(1))

            await userEvent.click(option)
            await userEvent.click(option)
            expect(getProjectBranches).toHaveBeenCalledTimes(1)
        })

        // Unticking must not leave the merge armed against a branch the dialog stopped showing.
        it('drops a selection the narrowed list no longer offers', async () => {
            vi.mocked(getProjectBranches).mockResolvedValue([
                { name: 'main' },
                { name: 'feature' },
                { name: 'untouched-by-the-project' },
            ])
            mockApiCall
                .mockResolvedValueOnce(mergeableResult('untouched-by-the-project', 'main'))
                .mockResolvedValueOnce(mergeableResult('main', 'untouched-by-the-project'))
            render(<MergeBranchesStep {...defaultProps()} />)

            const option = screen.getByRole('checkbox')
            await userEvent.click(option)
            await waitFor(() => expect(getProjectBranches).toHaveBeenCalled())
            await selectBranch('untouched-by-the-project')

            const select = screen.getByTestId('merge-target-branch') as HTMLSelectElement
            expect(select.value).toBe('untouched-by-the-project')

            await userEvent.click(option)

            await waitFor(() => expect(select.value).toBe(''))
        })

        it('leaves the option off and says why when the repository cannot be read', async () => {
            vi.mocked(getProjectBranches).mockRejectedValue(new Error('offline'))
            render(<MergeBranchesStep {...defaultProps()} />)

            const option = screen.getByRole('checkbox')
            await userEvent.click(option)

            await waitFor(() => expect(option).not.toBeChecked())
            expect(screen.getByTestId('merge-branches-error')).toBeInTheDocument()
        })

        // A project created in its own branch is held by that branch alone, so its own scope offers nothing
        // to merge with — the dialog would open empty exactly where it is needed most.
        it('widens the list by itself when the project has no other branch', async () => {
            vi.mocked(getProjectBranches).mockResolvedValue([{ name: 'feature' }, { name: 'main' }])
            render(<MergeBranchesStep {...defaultProps()} branches={[{ name: 'feature', protected: false }]} currentBranch="feature" />)

            await waitFor(() => expect(screen.getByRole('checkbox')).toBeChecked())
            const targets = within(screen.getByTestId('merge-target-branch')).queryAllByRole('option')
                .map(option => (option as HTMLOptionElement).value)
            expect(targets).toContain('main')
        })

        it('asks once when the automatic widening fails, instead of retrying forever', async () => {
            vi.mocked(getProjectBranches).mockRejectedValue(new Error('offline'))
            render(<MergeBranchesStep {...defaultProps()} branches={[{ name: 'feature', protected: false }]} currentBranch="feature" />)

            await waitFor(() => expect(screen.getByTestId('merge-branches-error')).toBeInTheDocument())
            await new Promise(resolve => setTimeout(resolve, 50))

            expect(getProjectBranches).toHaveBeenCalledTimes(1)
            expect(screen.getByRole('checkbox')).not.toBeChecked()
        })
    })
})

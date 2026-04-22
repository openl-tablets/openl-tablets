import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MergeBranchesStep } from 'containers/MergeModal/MergeBranchesStep'
import * as services from 'services'
import { BranchInfo, CheckMergeResult, MergeResultResponse } from 'containers/MergeModal/types'
import type { MockedFunction } from 'vitest'

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

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

// Mock the custom Select to render a native <select> that properly triggers onChange.
// Ant Design's Form.useWatch + MessageChannel does not work in jsdom.
vi.mock('components/form', () => ({
    // Drop AntD-only props that React warns about if spread onto a native <select>;
    // forward everything else (className, style, disabled, value, data-*, aria-*, …)
    // so tests keep fidelity on accessibility and custom attributes.
    Select: ({
        name,
        label,
        options,
        onChange,
        placeholder,
        // AntD-only — extend this list if the component under test starts passing
        // more AntD-specific props (e.g. `mode`, `showSearch`).
         
        suffixIcon,
        ...rest
    }: any) => (
        <div>
            <label htmlFor={name}>{label}</label>
            <select
                {...rest}
                id={name}
                onChange={(e) => onChange?.(e.target.value)}
            >
                <option value="">{placeholder}</option>
                {options?.map((opt: any) => (
                    <option key={String(opt.value)} value={String(opt.value)}>
                        {opt.label}
                    </option>
                ))}
            </select>
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

const mergeableResult = (source: string, target: string): CheckMergeResult => ({
    sourceBranch: source,
    targetBranch: target,
    status: 'mergeable',
})

const upToDateResult = (source: string, target: string): CheckMergeResult => ({
    sourceBranch: source,
    targetBranch: target,
    status: 'up-to-date',
})

const createApiError = (status: number, message: string) =>
    new (services as any).ApiHttpError(status, message)

const selectBranch = async (branchValue: string) => {
    const select = screen.getByLabelText('merge:branches.target')
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

    it('shows protected label in branch options', () => {
        render(<MergeBranchesStep {...defaultProps()} />)
        expect(screen.getByText('release-1.0 (protected)')).toBeInTheDocument()
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

            await waitFor(() => expect(screen.getByText('merge:errors.check_failed')).toBeInTheDocument())
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

            await waitFor(() => expect(screen.getByText('Connection failed')).toBeInTheDocument())
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
})

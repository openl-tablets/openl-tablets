import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DeleteBranchModal, DeleteBranchModalDetail } from 'containers/DeleteBranchModal'
import * as services from 'services'
import { deleteBranch } from 'services/branches'
import type { MockedFunction } from 'vitest'
import { recentBranches, recentProjects } from 'utils/recentChoices'

vi.mock('services', () => ({
    apiCall: vi.fn(),
}))

vi.mock('services/branches', () => ({
    deleteBranch: vi.fn(),
}))

// Replace AntD's Modal with an `open`-gated wrapper; its leave animation never ends in
// jsdom. Other components (Button, Alert, Spin) stay real so the footer is clickable.
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({
        open,
        title,
        children,
        footer,
    }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        footer?: React.ReactNode
    }) =>
        open ? (
            <div role="dialog">
                {title && <div data-testid="modal-title">{title}</div>}
                {children}
                {footer && <div data-testid="modal-footer">{footer}</div>}
            </div>
        ) : null
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>
const mockDeleteBranch = deleteBranch as MockedFunction<typeof deleteBranch>

// The project id, as the server issued it.
const PROJECT_ID = '2QK5V769WQEQ8'

const createDetail = (overrides?: Partial<DeleteBranchModalDetail>): DeleteBranchModalDetail => ({
    projectId: PROJECT_ID,
    repositoryId: 'repo-1',
    projectName: 'MyProject',
    branch: 'feature',
    mainBranch: 'master',
    onSuccess: vi.fn(),
    ...overrides,
})

const dispatchOpen = async (detail: DeleteBranchModalDetail | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openDeleteBranchModal', { detail }))
    })
}

describe('DeleteBranchModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('does not render when no event is dispatched', () => {
        render(<DeleteBranchModal />)
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })

    it('confirms deletion of a merged, unmodified branch and refreshes', async () => {
        localStorage.clear()
        recentBranches.remember('master', 'repo-1')
        recentBranches.remember('feature', 'repo-1')
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' }) // getProject -> not modified
            .mockResolvedValueOnce({ status: 'up-to-date' }) // merge check -> merged
        mockDeleteBranch.mockResolvedValueOnce(true)
        const detail = createDetail()

        render(<DeleteBranchModal />)
        await dispatchOpen(detail)

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument())
        expect(screen.getByText('repository:delete_branch.confirm')).toBeInTheDocument()
        // Wait for the preflight (project status + merge check) to finish so Delete is enabled.
        await waitFor(() =>
            expect(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' })).toBeEnabled()
        )
        expect(screen.queryByText('repository:delete_branch.not_merged_warning')).not.toBeInTheDocument()
        expect(screen.queryByText('repository:delete_branch.modified_warning')).not.toBeInTheDocument()

        await userEvent.click(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' }))

        await waitFor(() => {
            expect(mockDeleteBranch).toHaveBeenCalledWith(PROJECT_ID, 'feature', false)
            expect(detail.onSuccess).toHaveBeenCalled()
        })
        // A branch removed is offered first by no switcher of its repository.
        expect(recentBranches.read('repo-1')).toEqual(['master'])
    })

    it('warns and uses the unsafe button when the project is modified', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'EDITING' }) // modified -> skip merge check

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByText('repository:delete_branch.modified_warning')).toBeInTheDocument()
        )
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('forgets the project among the recent ones once the only branch holding it is deleted', async () => {
        localStorage.clear()
        recentProjects.remember('kept')
        recentProjects.remember(PROJECT_ID)
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' })
            .mockResolvedValueOnce({ status: 'up-to-date' })
        mockDeleteBranch.mockResolvedValueOnce(true)
        const detail = createDetail({ lastBranch: true })

        render(<DeleteBranchModal />)
        await dispatchOpen(detail)
        const confirm = await screen.findByRole('button', { name: 'repository:delete_branch.confirm_button_unsafe' })
        await waitFor(() => expect(confirm).toBeEnabled())
        await userEvent.click(confirm)

        await waitFor(() => expect(detail.onSuccess).toHaveBeenCalled())
        expect(recentProjects.read()).toEqual(['kept'])
    })

    it('warns that deleting the only branch holding the project deletes the project', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' }) // not modified
            .mockResolvedValueOnce({ status: 'up-to-date' }) // merged

        render(<DeleteBranchModal />)
        await dispatchOpen({ ...createDetail(), lastBranch: true })

        expect(await screen.findByText('repository:delete_branch.last_branch_warning')).toBeInTheDocument()
        // Losing the project is destructive, so the deliberate confirmation is required.
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('warns when the branch is not merged into main', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' }) // not modified
            .mockResolvedValueOnce({ status: 'mergeable' }) // not merged

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByText('repository:delete_branch.not_merged_warning')).toBeInTheDocument()
        )
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('trusts the merge check even when the user may not merge into a protected main branch', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' }) // not modified
            // The main branch is protected: the merge is refused, the branches are still up to date.
            .mockResolvedValueOnce({ status: 'up-to-date', canMerge: false, blockedBy: 'protected-branch' })

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' })).toBeEnabled()
        )
        expect(screen.queryByText('repository:delete_branch.not_merged_warning')).not.toBeInTheDocument()
        // The check asks where the branches stand, not for permission to merge.
        expect(mockApiCall).toHaveBeenCalledWith(
            `/projects/${PROJECT_ID}/merge/check`,
            expect.objectContaining({ method: 'POST' }),
            expect.anything()
        )
    })

    it('falls back to the cautious (unsafe) state when the preflight fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('boom')) // getProject fails

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
        )
        expect(screen.getByText('repository:delete_branch.not_merged_warning')).toBeInTheDocument()
    })
})

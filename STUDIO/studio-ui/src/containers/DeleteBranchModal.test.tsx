import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { DeleteBranchModal, DeleteBranchModalDetail } from 'containers/DeleteBranchModal'
import * as services from 'services'
import { deleteBranch } from 'services/branches'
import type { MockedFunction } from 'vitest'

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

// URL-safe Base64 id, matching the id the modal builds for an opener that knows only repository and name.
const urlSafeId = (raw: string) => btoa(raw).replaceAll('+', '-').replaceAll('/', '_')

const createDetail = (overrides?: Partial<DeleteBranchModalDetail>): DeleteBranchModalDetail => ({
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
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' } as never) // getProject -> not modified
            .mockResolvedValueOnce({ status: 'up-to-date' } as never) // merge check -> merged
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
            expect(mockDeleteBranch).toHaveBeenCalledWith(urlSafeId('repo-1:MyProject'), 'feature', false)
            expect(detail.onSuccess).toHaveBeenCalled()
        })
    })

    it('normalizes a server-issued id before putting it in a URL path', async () => {
        // The server encodes ids with the standard Base64 alphabet. A project named "Тарифный план" yields a
        // '/', which a servlet container rejects in a path, so every request here must carry the URL-safe form.
        const serverIssuedId = 'cmVwby0xOtCi0LDRgNC40YTQvdGL0Lkg0L/Qu9Cw0L0='
        const urlSafe = 'cmVwby0xOtCi0LDRgNC40YTQvdGL0Lkg0L_Qu9Cw0L0='
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' } as never)
            .mockResolvedValueOnce({ status: 'up-to-date' } as never)
        mockDeleteBranch.mockResolvedValueOnce(true)

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail({ projectId: serverIssuedId }))

        await waitFor(() =>
            expect(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' })).toBeEnabled()
        )
        expect(mockApiCall).toHaveBeenCalledWith(`/projects/${urlSafe}`, expect.anything(), expect.anything())
        expect(mockApiCall).toHaveBeenCalledWith(
            `/projects/${urlSafe}/merge/check`,
            expect.anything(),
            expect.anything()
        )

        await userEvent.click(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' }))

        await waitFor(() => expect(mockDeleteBranch).toHaveBeenCalledWith(urlSafe, 'feature', false))
    })

    it('warns and uses the unsafe button when the project is modified', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'EDITING' } as never) // modified -> skip merge check

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByText('repository:delete_branch.modified_warning')).toBeInTheDocument()
        )
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('warns that deleting the only branch holding the project deletes the project', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' } as never) // not modified
            .mockResolvedValueOnce({ status: 'up-to-date' } as never) // merged

        render(<DeleteBranchModal />)
        await dispatchOpen({ ...createDetail(), lastBranch: true })

        expect(await screen.findByText('repository:delete_branch.last_branch_warning')).toBeInTheDocument()
        // Losing the project is destructive, so the deliberate confirmation is required.
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('warns when the branch is not merged into main', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' } as never) // not modified
            .mockResolvedValueOnce({ status: 'mergeable' } as never) // not merged

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByText('repository:delete_branch.not_merged_warning')).toBeInTheDocument()
        )
        expect(screen.getByText('repository:delete_branch.confirm_button_unsafe')).toBeInTheDocument()
    })

    it('trusts the merge check even when the user may not merge into a protected main branch', async () => {
        mockApiCall
            .mockResolvedValueOnce({ status: 'VIEWING' } as never) // not modified
            // The main branch is protected: the merge is refused, the branches are still up to date.
            .mockResolvedValueOnce({ status: 'up-to-date', canMerge: false, blockedBy: 'protected-branch' } as never)

        render(<DeleteBranchModal />)
        await dispatchOpen(createDetail())

        await waitFor(() =>
            expect(screen.getByRole('button', { name: 'repository:delete_branch.confirm_button' })).toBeEnabled()
        )
        expect(screen.queryByText('repository:delete_branch.not_merged_warning')).not.toBeInTheDocument()
        // The check asks where the branches stand, not for permission to merge.
        expect(mockApiCall).toHaveBeenCalledWith(
            `/projects/${urlSafeId('repo-1:MyProject')}/merge/check`,
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

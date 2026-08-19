import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OpenRevisionModal } from './OpenRevisionModal'
import { getProjectRevisions, openProjectRevision } from '../../services/repositories'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('../../services/repositories', () => ({
    getProjectRevisions: vi.fn(),
    openProjectRevision: vi.fn(),
    isProjectModifiedConflict: (e: unknown) => (e as { modifiedConflict?: boolean })?.modifiedConflict === true,
    REVISIONS_PAGE_SIZE: 50,
}))

vi.mock('../DiscardChangesModal', () => ({
    DiscardChangesModal: ({ open, onConfirm, confirmButtonTestId }: Record<string, unknown>) => (open ? (
        <button data-testid={confirmButtonTestId as string} onClick={onConfirm as never}>discard</button>
    ) : null),
}))

/** The server's answer when a project has unsaved changes. */
const modifiedConflict = () => Object.assign(new Error('modified'), { modifiedConflict: true })

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okText, title }: Record<string, unknown>) => open ? (
        <div>
            <h2>{title as never}</h2>
            {children as never}
            <button data-testid="open-revision-ok" onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    const Alert = ({ message, showIcon, ...rest }: Record<string, unknown>) => {
        void showIcon
        return <div data-testid={rest['data-testid'] as string}>{message as never}</div>
    }
    interface Opt { value: string, label: string }
    const Select = ({ options, onChange, value, ...rest }: Record<string, unknown>) => (
        <select
            data-testid={rest['data-testid'] as string}
            onChange={event => (onChange as (v: string) => void)(event.target.value)}
            value={value as string}
        >
            {(options as Opt[]).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
        </select>
    )
    const notification = { success: vi.fn(), error: vi.fn() }
    return { Alert, Modal, Select, notification }
})

const project = {
    id: 'p1',
    name: 'Alpha',
    repository: 'design',
    branch: 'main',
    status: ProjectStatus.Opened,
} as unknown as Project

const renderModal = async (overrides: Partial<Project> = {}) => {
    const onOpened = vi.fn()
    const onClose = vi.fn()
    render(
        <OpenRevisionModal
            open
            onClose={onClose}
            onOpened={onOpened}
            project={{ ...project, ...overrides }}
        />
    )
    await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())
    return { onOpened, onClose }
}

describe('OpenRevisionModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(openProjectRevision).mockResolvedValue()
        vi.mocked(getProjectRevisions).mockResolvedValue({
            content: [
                { revisionNo: 'rev-2', shortRevisionNo: 'rev2', createdAt: '2026-07-22T14:41:20Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'DEFAULT' } },
                { revisionNo: 'rev-1', shortRevisionNo: 'rev1', createdAt: '2026-07-21T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        })
    })

    it('offers the revisions the way a business user reads them', async () => {
        await renderModal()

        // A revision leads its own label, so a picker never offers two entries that read alike.
        // See EPBDS-16458.
        expect(screen.getByText(/^rev-2 · DEFAULT: /)).toBeInTheDocument()
        expect(screen.getByText(/^rev-1 · Joe Doe: /)).toBeInTheDocument()
        // The project name lives in the dialog title, not in a field of its own.
        expect(screen.getByRole('heading')).toHaveTextContent('browser.open_revision_dialog.title')
    })

    it('opens the chosen revision', async () => {
        const { onOpened, onClose } = await renderModal()

        fireEvent.change(screen.getByTestId('open-revision-select'), { target: { value: 'rev-1' } })
        await userEvent.click(screen.getByTestId('open-revision-ok'))

        await waitFor(() => expect(openProjectRevision).toHaveBeenCalledWith('p1', 'rev-1', {}))
        expect(onOpened).toHaveBeenCalled()
        expect(onClose).toHaveBeenCalled()
    })

    it('confirms before dropping unsaved changes, then opens the revision', async () => {
        vi.mocked(openProjectRevision).mockRejectedValueOnce(modifiedConflict())
        await renderModal({ status: ProjectStatus.Editing })

        expect(screen.getByTestId('open-revision-modified')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('open-revision-ok'))

        // The first attempt is refused by the server; the user confirms losing the changes explicitly.
        await waitFor(() => expect(openProjectRevision).toHaveBeenCalledWith('p1', 'rev-2', {}))
        await userEvent.click(await screen.findByTestId('open-revision-discard-confirm'))

        await waitFor(() => expect(openProjectRevision).toHaveBeenCalledWith('p1', 'rev-2', { discardChanges: true }))
    })

    it('shows no warning for a project without local changes', async () => {
        await renderModal()

        expect(screen.queryByTestId('open-revision-modified')).toBeNull()
    })

    it('reports a failure to open', async () => {
        vi.mocked(openProjectRevision).mockRejectedValue(new Error('locked'))
        const { onOpened } = await renderModal()

        await userEvent.click(screen.getByTestId('open-revision-ok'))

        await screen.findByTestId('open-revision-error')
        expect(onOpened).not.toHaveBeenCalled()
    })
})

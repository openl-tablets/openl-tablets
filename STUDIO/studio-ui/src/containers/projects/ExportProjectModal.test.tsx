import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notification } from 'antd'
import { ExportProjectModal } from './ExportProjectModal'
import { downloadProject, getProjectRevisions } from '../../services/repositories'
import { downloadFile, fileExistsAt, getFileRevisions } from '../../services/files'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('../../services/repositories', () => ({
    downloadProject: vi.fn(),
    getProjectRevisions: vi.fn(),
    REVISIONS_PAGE_SIZE: 50,
}))

vi.mock('../../services/files', () => ({
    downloadFile: vi.fn(),
    fileExistsAt: vi.fn(),
    getFileRevisions: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okText }: Record<string, unknown>) => open ? (
        <div>
            {children as never}
            <button data-testid="export-ok" onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    const notification = { error: vi.fn() }
    const Alert = ({ message, showIcon, ...rest }: Record<string, unknown>) => {
        void showIcon
        return <div data-testid={rest['data-testid'] as string}>{message as never}</div>
    }
    interface Opt { value: string, label: string }
    const Select = ({ options, onChange, value, popupRender, ...rest }: Record<string, unknown>) => (
        <>
            <select
                data-testid={rest['data-testid'] as string}
                onChange={event => (onChange as (v: string) => void)(event.target.value)}
                value={value as string}
            >
                {(options as Opt[]).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
            {popupRender ? ((popupRender as (menu: unknown) => unknown)(null) as never) : null}
        </>
    )
    const Button = ({ children, onClick, block, loading, type, ...rest }: Record<string, unknown>) => {
        void block; void loading; void type
        return <button data-testid={rest['data-testid'] as string} onClick={onClick as never}>{children as never}</button>
    }
    return { Alert, Button, Modal, notification, Select }
})

const project = {
    id: 'p1',
    name: 'Alpha',
    repository: 'design',
    branch: 'main',
    status: ProjectStatus.Opened,
} as unknown as Project

const renderModal = async ({ filePath, ...overrides }: Partial<Project> & { filePath?: string } = {}) => {
    const onClose = vi.fn()
    render(
        <ExportProjectModal
            open
            filePath={filePath}
            onClose={onClose}
            project={{ ...project, ...overrides }}
        />
    )
    await waitFor(() => expect(filePath ? getFileRevisions : getProjectRevisions).toHaveBeenCalled())
    return { onClose }
}

describe('ExportProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(fileExistsAt).mockResolvedValue(true)
        const projectPage = {
            content: [
                { revisionNo: 'rev-2', shortRevisionNo: 'rev2', createdAt: '2026-07-22T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
                { revisionNo: 'rev-1', shortRevisionNo: 'rev1', createdAt: '2026-07-21T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Jane Roe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        }
        // Naming a path asks for that file's own history; the two answer differently on purpose.
        const filePage = {
            content: [
                { revisionNo: 'file-rev-2', shortRevisionNo: 'file-2', createdAt: '2026-07-22T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
                { revisionNo: 'file-gone', shortRevisionNo: 'gone', createdAt: '2026-07-23T10:00:00Z', fullComment: '', deleted: true, technicalRevision: false, author: { displayName: 'Joe Doe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        }
        vi.mocked(getProjectRevisions).mockResolvedValue(projectPage)
        vi.mocked(getFileRevisions).mockResolvedValue(filePage)
    })

    it('downloads what the opened project shows', async () => {
        const { onClose } = await renderModal()

        expect(screen.getByText('browser.export_dialog.viewing')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadProject).toHaveBeenCalledWith('p1', undefined)
        expect(onClose).toHaveBeenCalled()
    })

    it('downloads the local changes of a project being edited', async () => {
        await renderModal({ status: ProjectStatus.Editing })

        expect(screen.getByText('browser.export_dialog.in_editing')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadProject).toHaveBeenCalledWith('p1', undefined)
    })

    it('downloads the chosen earlier revision', async () => {
        await renderModal()

        fireEvent.change(screen.getByTestId('export-project-revision'), { target: { value: 'rev-1' } })
        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadProject).toHaveBeenCalledWith('p1', 'rev-1')
    })

    // The editor exports the open module, which is one file of the project rather than the whole of it.
    it('downloads a single file when one is named', async () => {
        const { onClose } = await renderModal({ filePath: 'rules/Main.xlsx' })

        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', undefined)
        expect(downloadProject).not.toHaveBeenCalled()
        expect(onClose).toHaveBeenCalled()
    })

    it('downloads the chosen earlier revision of a single file', async () => {
        await renderModal({ filePath: 'rules/Main.xlsx' })

        // A revision of the file itself, not of the project around it.
        fireEvent.change(screen.getByTestId('export-project-revision'), { target: { value: 'file-rev-2' } })
        await userEvent.click(screen.getByTestId('export-ok'))

        await waitFor(() => expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'file-rev-2'))
    })

    // The dialog it replaced listed the whole history, so an older revision has to stay reachable.
    it('appends older revisions on demand', async () => {
        vi.mocked(getProjectRevisions).mockResolvedValueOnce({
            content: [
                { revisionNo: 'rev-2', shortRevisionNo: 'rev2', createdAt: '2026-07-22T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
            ],
            pageNumber: 0,
            pageSize: 1,
            numberOfElements: 1,
            total: 2,
        }).mockResolvedValueOnce({
            content: [
                { revisionNo: 'rev-1', shortRevisionNo: 'rev1', createdAt: '2026-07-21T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Jane Roe' } },
            ],
            pageNumber: 1,
            pageSize: 1,
            numberOfElements: 1,
            total: 2,
        })
        await renderModal()

        await userEvent.click(screen.getByTestId('export-project-load-more'))

        await waitFor(() => expect(getProjectRevisions).toHaveBeenCalledWith('p1', { size: 50, page: 1 }))
        fireEvent.change(screen.getByTestId('export-project-revision'), { target: { value: 'rev-1' } })
        await userEvent.click(screen.getByTestId('export-ok'))
        expect(downloadProject).toHaveBeenCalledWith('p1', 'rev-1')
    })

    // Nothing older to fetch: the whole history is already on screen.
    it('offers no load-more once the history is exhausted', async () => {
        await renderModal()

        expect(screen.queryByTestId('export-project-load-more')).toBeNull()
    })

    // The project's history offers revisions that never held the file; the file's own history does not.
    it('offers the file own revisions when a file is exported', async () => {
        await renderModal({ filePath: 'rules/Main.xlsx' })

        await waitFor(() => expect(getFileRevisions).toHaveBeenCalledWith(
            'p1', 'rules/Main.xlsx', { size: 50, page: 0 }
        ))
        expect(getProjectRevisions).not.toHaveBeenCalled()
        const values = [...screen.getByTestId('export-project-revision').querySelectorAll('option')]
            .map(option => option.getAttribute('value'))
        expect(values).toContain('file-rev-2')
        // The revision that removed the file cannot be exported from, so it is not offered.
        expect(values).not.toContain('file-gone')
    })

    // A revision that removed the file is not offered, so it must not be preselected either — the dialog
    // would submit a revision missing from its own list.
    it('defaults a closed project to the newest revision still offered', async () => {
        vi.mocked(getFileRevisions).mockResolvedValue({
            content: [
                { revisionNo: 'file-gone', shortRevisionNo: 'gone', createdAt: '2026-07-23T10:00:00Z', fullComment: '', deleted: true, technicalRevision: false, author: { displayName: 'Joe Doe' } },
                { revisionNo: 'file-rev-2', shortRevisionNo: 'file-2', createdAt: '2026-07-22T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        })
        await renderModal({ status: ProjectStatus.Closed, filePath: 'rules/Main.xlsx' })

        await userEvent.click(screen.getByTestId('export-ok'))

        await waitFor(() => expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'file-rev-2'))
    })

    // A refused download is silent in the browser, so the dialog says what happened instead.
    it('refuses to export a file the chosen revision does not hold', async () => {
        vi.mocked(fileExistsAt).mockResolvedValue(false)
        const { onClose } = await renderModal({ filePath: 'rules/Main.xlsx' })

        fireEvent.change(screen.getByTestId('export-project-revision'), { target: { value: 'file-rev-2' } })
        await userEvent.click(screen.getByTestId('export-ok'))

        await waitFor(() => expect(notification.error).toHaveBeenCalled())
        expect(downloadFile).not.toHaveBeenCalled()
        expect(onClose).not.toHaveBeenCalled()
    })

    // The check failing is not proof the file is missing, so the export still goes ahead.
    it('exports anyway when the existence check itself fails', async () => {
        vi.mocked(fileExistsAt).mockRejectedValue(new Error('offline'))
        await renderModal({ filePath: 'rules/Main.xlsx' })

        await userEvent.click(screen.getByTestId('export-ok'))

        await waitFor(() => expect(downloadFile).toHaveBeenCalled())
    })

    it('offers revisions only for a closed project, starting with the latest', async () => {
        await renderModal({ status: ProjectStatus.Closed })

        expect(screen.queryByText('browser.export_dialog.viewing')).toBeNull()
        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadProject).toHaveBeenCalledWith('p1', 'rev-2')
    })
})

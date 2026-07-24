import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ExportProjectModal } from './ExportProjectModal'
import { downloadProject, getProjectRevisions } from '../../services/repositories'
import { ProjectStatus } from '../../constants/project'
import type { Project } from '../../types/projects'

vi.mock('../../services/repositories', () => ({
    downloadProject: vi.fn(),
    getProjectRevisions: vi.fn(),
    REVISIONS_PAGE_SIZE: 50,
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
    return { Alert, Modal, Select }
})

const project = {
    id: 'p1',
    name: 'Alpha',
    repository: 'design',
    branch: 'main',
    status: ProjectStatus.Opened,
} as unknown as Project

const renderModal = async (overrides: Partial<Project> = {}) => {
    const onClose = vi.fn()
    render(<ExportProjectModal open onClose={onClose} project={{ ...project, ...overrides }} />)
    await waitFor(() => expect(getProjectRevisions).toHaveBeenCalled())
    return { onClose }
}

describe('ExportProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectRevisions).mockResolvedValue({
            content: [
                { revisionNo: 'rev-2', shortRevisionNo: 'rev2', createdAt: '2026-07-22T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Joe Doe' } },
                { revisionNo: 'rev-1', shortRevisionNo: 'rev1', createdAt: '2026-07-21T10:00:00Z', fullComment: '', deleted: false, technicalRevision: false, author: { displayName: 'Jane Roe' } },
            ],
            pageNumber: 0,
            pageSize: 50,
            numberOfElements: 2,
            total: 2,
        })
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

    it('offers revisions only for a closed project, starting with the latest', async () => {
        await renderModal({ status: ProjectStatus.Closed })

        expect(screen.queryByText('browser.export_dialog.viewing')).toBeNull()
        await userEvent.click(screen.getByTestId('export-ok'))

        expect(downloadProject).toHaveBeenCalledWith('p1', 'rev-2')
    })
})

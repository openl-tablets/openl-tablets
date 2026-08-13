import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import type { MockedFunction } from 'vitest'
import { ExportProjectModalHost } from './ExportProjectModalHost'
import { getProject } from 'services/repositories'
import { useAppStore } from 'store'
import type { Project } from 'types/projects'

vi.mock('services/repositories', () => ({
    getProject: vi.fn(),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return { ...actual, notification: { ...actual.notification, error: vi.fn() } }
})

// The dialog itself is covered by its own test; here only the props it is handed matter.
interface CapturedProps {
    open: boolean
    project: Project | null
    filePath?: string
    onClose: () => void
}

const { exportModalMock } = vi.hoisted(() => ({ exportModalMock: vi.fn() }))
vi.mock('containers/projects/ExportProjectModal', () => ({
    ExportProjectModal: (props: CapturedProps) => {
        exportModalMock(props)
        return props.open ? <div data-testid="export-project-modal">{props.project?.name}</div> : null
    },
}))

// The last render wins: earlier calls carry props from before the load resolved.
const latest = () => exportModalMock.mock.calls.at(-1)![0] as CapturedProps

const mockGetProject = getProject as MockedFunction<typeof getProject>

const project = { id: 'p1', name: 'Bank Rating' } as Project

const renderHost = () => act(async () => {
    render(<ExportProjectModalHost />)
})

const open = async (detail: Record<string, unknown> = { projectId: 'p1' }) => act(async () => {
    globalThis.dispatchEvent(new CustomEvent('openExportProjectModal', { detail }))
})

describe('ExportProjectModalHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useAppStore.setState({ showLoader: vi.fn(), hideLoader: vi.fn() })
        mockGetProject.mockResolvedValue(project)
    })

    it('stays closed until the editor asks for it', async () => {
        await renderHost()

        expect(screen.queryByTestId('export-project-modal')).not.toBeInTheDocument()
        expect(mockGetProject).not.toHaveBeenCalled()
    })

    it('exports the whole project when no file is named', async () => {
        await renderHost()

        await open()

        await waitFor(() => expect(screen.getByTestId('export-project-modal')).toBeInTheDocument())
        expect(latest().project).toEqual(project)
        expect(latest().filePath).toBeUndefined()
    })

    // The editor exports the open module, which is one file of the project rather than the whole of it.
    it('passes the module file on to the dialog', async () => {
        await renderHost()

        await open({ projectId: 'p1', filePath: 'rules/Main.xlsx' })

        await waitFor(() => expect(screen.getByTestId('export-project-modal')).toBeInTheDocument())
        expect(latest().filePath).toBe('rules/Main.xlsx')
    })
})

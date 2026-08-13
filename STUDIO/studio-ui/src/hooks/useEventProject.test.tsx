import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import type { MockedFunction } from 'vitest'
import { useEventProject } from './useEventProject'
import { notifyLoadFailure } from '../services/apiCall'
import { getProject } from '../services/repositories'
import { useAppStore } from '../store'
import type { Project } from '../types/projects'

vi.mock('../services/repositories', () => ({
    getProject: vi.fn(),
}))

vi.mock('../services/apiCall', async importOriginal => ({
    ...await importOriginal<typeof import('../services/apiCall')>(),
    notifyLoadFailure: vi.fn(),
}))

const EVENT = 'openTestModal'

const Harness: React.FC = () => {
    const { detail, project, close } = useEventProject<{ projectId: string }>(EVENT, 'repository:browser.load_failed')
    return (
        <div>
            {project && <span data-testid="project">{project.name}</span>}
            {detail && <span data-testid="detail">{detail.projectId}</span>}
            <button data-testid="close" onClick={close} type="button">close</button>
        </div>
    )
}

const showLoader = vi.fn()
const hideLoader = vi.fn()

const mockGetProject = getProject as MockedFunction<typeof getProject>

const project = { id: 'p1', name: 'Bank Rating' } as Project

const renderHarness = () => act(async () => {
    render(<Harness />)
})

const open = async (projectId = 'p1') => act(async () => {
    globalThis.dispatchEvent(new CustomEvent(EVENT, { detail: { projectId } }))
})

describe('useEventProject', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useAppStore.setState({ showLoader, hideLoader })
        mockGetProject.mockResolvedValue(project)
    })

    it('holds no project until the editor asks for one', async () => {
        await renderHarness()

        expect(screen.queryByTestId('project')).not.toBeInTheDocument()
        expect(mockGetProject).not.toHaveBeenCalled()
    })

    // The editor page sends the id alone, so the dialog's project is read here.
    it('reads the project named by the event', async () => {
        await renderHarness()

        await open()

        await waitFor(() => expect(screen.getByTestId('project')).toHaveTextContent('Bank Rating'))
        // Read with the errors suppressed: this failure is reported here, not by the global error page.
        expect(mockGetProject).toHaveBeenCalledWith('p1', {}, { throwError: true, suppressErrorPages: true })
    })

    // An empty dialog would act on nothing, so a project that cannot be read keeps it shut. Whether the
    // failure is worth a toast is the request layer's call, covered by its own test.
    it('reports a project that cannot be read and holds no project', async () => {
        const failure = new Error('offline')
        mockGetProject.mockRejectedValue(failure)
        await renderHarness()

        await open()

        await waitFor(() => expect(notifyLoadFailure).toHaveBeenCalled())
        expect(vi.mocked(notifyLoadFailure).mock.calls[0]?.[1]).toBe(failure)
        expect(screen.queryByTestId('project')).not.toBeInTheDocument()
    })

    // A second click while the dialog is up must not read the project again.
    it('reads the project once for repeated requests', async () => {
        await renderHarness()

        await open()
        await open()

        await waitFor(() => expect(screen.getByTestId('project')).toBeInTheDocument())
        expect(mockGetProject).toHaveBeenCalledTimes(1)
    })

    // Asked for another project, the dialog must not stay open on the one it still holds — it would act on
    // the project the user has just navigated away from.
    it('drops the loaded project while a different one is being read', async () => {
        await renderHarness()
        await open('p1')
        await waitFor(() => expect(screen.getByTestId('project')).toHaveTextContent('Bank Rating'))

        let settle: (value: Project) => void = () => {}
        mockGetProject.mockReturnValue(new Promise<Project>(resolve => {
            settle = resolve
        }))
        await open('p2')

        expect(screen.queryByTestId('project')).not.toBeInTheDocument()

        await act(async () => settle({ id: 'p2', name: 'Auto Policy' } as Project))
        await waitFor(() => expect(screen.getByTestId('project')).toHaveTextContent('Auto Policy'))
    })

    it('drops the project when the dialog is closed', async () => {
        await renderHarness()
        await open()
        await waitFor(() => expect(screen.getByTestId('project')).toBeInTheDocument())

        await act(async () => {
            screen.getByTestId('close').click()
        })

        await waitFor(() => expect(screen.queryByTestId('project')).not.toBeInTheDocument())
        expect(screen.queryByTestId('detail')).not.toBeInTheDocument()
    })

    // The read takes as long as the repository does, so the click has to show something and the next click
    // must not start it over.
    it('holds the loading overlay open for the whole read', async () => {
        let settle: (value: Project) => void = () => {}
        mockGetProject.mockReturnValue(new Promise<Project>(resolve => {
            settle = resolve
        }))
        await renderHarness()

        await open()
        expect(showLoader).toHaveBeenCalledTimes(1)
        expect(hideLoader).not.toHaveBeenCalled()

        await act(async () => settle(project))
        await waitFor(() => expect(hideLoader).toHaveBeenCalledTimes(1))
    })
})

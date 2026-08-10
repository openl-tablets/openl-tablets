import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import { notification } from 'antd'
import type { MockedFunction } from 'vitest'
import { CopyProjectModalHost } from './CopyProjectModalHost'
import { EmptyError } from 'services/apiCall'
import { getDesignRepositories, getProject } from 'services/repositories'
import { useAppStore } from 'store'
import type { Project } from 'types/projects'
import type { Repository } from 'types/repositories'

vi.mock('services/repositories', () => ({
    getProject: vi.fn(),
    getDesignRepositories: vi.fn(),
}))

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return { ...actual, notification: { ...actual.notification, error: vi.fn() } }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

// The dialog itself is covered by its own test; here only the props it is handed matter.
interface CapturedProps {
    open: boolean
    project: Project | null
    repositories: Repository[]
    onClose: () => void
    onCopied: () => void
}

const { copyModalMock } = vi.hoisted(() => ({ copyModalMock: vi.fn() }))
vi.mock('containers/projects/CopyProjectModal', () => ({
    CopyProjectModal: (props: CapturedProps) => {
        copyModalMock(props)
        return props.open ? <div data-testid="copy-project-modal">{props.project?.name}</div> : null
    },
}))

// The last render wins: earlier calls carry props from before the load resolved.
const latest = () => copyModalMock.mock.calls.at(-1)![0] as CapturedProps

const showLoader = vi.fn()
const hideLoader = vi.fn()

const mockGetProject = getProject as MockedFunction<typeof getProject>
const mockGetRepositories = getDesignRepositories as MockedFunction<typeof getDesignRepositories>

const project = { id: 'p1', name: 'Bank Rating' } as Project
const repositories = [
    { id: 'design', capabilities: { canCreateProject: true } },
    { id: 'locked', capabilities: {} },
] as Repository[]

const renderHost = () => act(async () => {
    render(<CopyProjectModalHost />)
})

const open = async (onSuccess = vi.fn()) => {
    await act(async () => {
        globalThis.dispatchEvent(new CustomEvent('openCopyProjectModal', { detail: { projectId: 'p1', onSuccess } }))
    })
    return onSuccess
}

describe('CopyProjectModalHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useAppStore.setState({ showLoader, hideLoader })
        mockGetProject.mockResolvedValue(project)
        mockGetRepositories.mockResolvedValue(repositories)
    })

    it('stays closed until the editor asks for it', async () => {
        await renderHost()

        expect(screen.queryByTestId('copy-project-modal')).not.toBeInTheDocument()
        expect(mockGetProject).not.toHaveBeenCalled()
    })

    // The editor page sends the id alone, so everything the dialog works off is read here.
    it('reads the project and opens the dialog on the event', async () => {
        await renderHost()

        await open()

        await waitFor(() => expect(screen.getByTestId('copy-project-modal')).toBeInTheDocument())
        // Read with the errors suppressed: this failure is reported here, not by the global error page.
        expect(mockGetProject).toHaveBeenCalledWith('p1', {}, { throwError: true, suppressErrorPages: true })
        expect(latest().project).toEqual(project)
    })

    // A repository the user cannot create in is not a copy target, so it is never offered as one.
    it('offers only the repositories a project may be created in', async () => {
        await renderHost()

        await open()

        await waitFor(() => expect(latest().repositories).toHaveLength(1))
        expect(latest().repositories[0]?.id).toBe('design')
    })

    it('reports the copy back to the page that opened the dialog', async () => {
        await renderHost()
        const onSuccess = await open()
        await waitFor(() => expect(latest().project).toEqual(project))

        act(() => latest().onCopied())

        expect(onSuccess).toHaveBeenCalled()
    })

    // An empty dialog would offer a copy of nothing, so a project that cannot be read keeps it shut.
    it('keeps the dialog closed when the project cannot be read', async () => {
        mockGetProject.mockRejectedValue(new Error('offline'))
        await renderHost()

        await open()

        await waitFor(() => expect(screen.queryByTestId('copy-project-modal')).not.toBeInTheDocument())
    })

    // Branching needs no target repository, so the dialog still opens for it — but a copy target list that
    // silently came back empty would read as "this project cannot be copied anywhere".
    it('opens the dialog even when the repositories cannot be read, and says so', async () => {
        mockGetRepositories.mockRejectedValue(new Error('offline'))
        await renderHost()

        await open()

        await waitFor(() => expect(screen.getByTestId('copy-project-modal')).toBeInTheDocument())
        expect(latest().repositories).toEqual([])
        expect(notification.error).toHaveBeenCalledWith(
            expect.objectContaining({ title: 'repository:browser.copy_dialog.repositories_failed' })
        )
    })

    // An expired session already draws the login prompt; a blank-bodied toast over it says nothing.
    it('stays quiet when the session has expired', async () => {
        mockGetProject.mockRejectedValue(new EmptyError())
        await renderHost()

        await open()

        await waitFor(() => expect(mockGetProject).toHaveBeenCalled())
        expect(notification.error).not.toHaveBeenCalled()
    })

    // The reads take as long as the repository does, so the click has to show something and the next click
    // must not start them over.
    it('holds the loading overlay open for the whole read', async () => {
        let settle: (value: Project) => void = () => {}
        mockGetProject.mockReturnValue(new Promise<Project>(resolve => {
            settle = resolve
        }))
        await renderHost()

        await open()
        expect(showLoader).toHaveBeenCalledTimes(1)
        expect(hideLoader).not.toHaveBeenCalled()

        await act(async () => settle(project))
        await waitFor(() => expect(hideLoader).toHaveBeenCalledTimes(1))
    })
})

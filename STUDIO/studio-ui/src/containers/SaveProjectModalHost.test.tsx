import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import type { MockedFunction } from 'vitest'
import { SaveProjectModalHost } from './SaveProjectModalHost'
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
    onClose: () => void
    onSaved: () => void
}

const { saveModalMock } = vi.hoisted(() => ({ saveModalMock: vi.fn() }))
vi.mock('containers/projects/SaveProjectModal', () => ({
    SaveProjectModal: (props: CapturedProps) => {
        saveModalMock(props)
        return props.open ? <div data-testid="save-project-modal">{props.project?.name}</div> : null
    },
}))

// The last render wins: earlier calls carry props from before the load resolved.
const latest = () => saveModalMock.mock.calls.at(-1)![0] as CapturedProps

const mockGetProject = getProject as MockedFunction<typeof getProject>

const project = { id: 'p1', name: 'Bank Rating' } as Project

const renderHost = () => render(<SaveProjectModalHost />)

const open = async (onSuccess = vi.fn()) => {
    await act(async () => {
        globalThis.dispatchEvent(new CustomEvent('openSaveProjectModal', { detail: { projectId: 'p1', onSuccess } }))
    })
    return onSuccess
}

describe('SaveProjectModalHost', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useAppStore.setState({ showLoader: vi.fn(), hideLoader: vi.fn() })
        mockGetProject.mockResolvedValue(project)
    })

    it('stays closed until the editor asks for it', async () => {
        renderHost()

        expect(screen.queryByTestId('save-project-modal')).not.toBeInTheDocument()
        expect(mockGetProject).not.toHaveBeenCalled()
    })

    it('reads the project and opens the dialog on the event', async () => {
        renderHost()

        await open()

        expect(await screen.findByTestId('save-project-modal')).toBeInTheDocument()
        expect(latest().project).toEqual(project)
    })

    // Saving may commit the project under a different name, so the editor page is told to move to it.
    it('reports the save back to the page that opened the dialog', async () => {
        renderHost()
        const onSuccess = await open()
        await waitFor(() => expect(latest().project).toEqual(project))

        act(() => latest().onSaved())

        expect(onSuccess).toHaveBeenCalled()
    })
})

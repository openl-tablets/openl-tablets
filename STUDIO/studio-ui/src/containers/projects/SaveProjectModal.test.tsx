import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SaveProjectModal } from './SaveProjectModal'
import { apiCall, ApiHttpError } from '../../services'
import { getProjectBranches, getRepositoryConfig, saveProject } from '../../services/repositories'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('../../services', () => {
    class ApiHttpError extends Error {
        status: number
        constructor(status: number, message?: string) {
            super(message)
            this.status = status
        }
    }
    return {
        apiCall: vi.fn(),
        ApiHttpError,
        isApiHttpError: (e: unknown) => e instanceof ApiHttpError,
    }
})

vi.mock('../../services/repositories', () => ({
    getDesignRepositoryConfig: vi.fn(),
    getProjectBranches: vi.fn(),
    getRepositoryConfig: vi.fn(),
    saveProject: vi.fn(),
}))

vi.mock('store', () => ({
    useUserStore: () => ({
        userProfile: { username: 'jane', firstName: '', lastName: '', displayName: '', email: '' },
    }),
}))

vi.mock('../users/UserProfileCompletionModal', () => ({
    UserProfileCompletionModal: ({ open, onSave }: { open: boolean, onSave: () => void }) =>
        open ? <button data-testid="commit-info-save" onClick={onSave}>identity</button> : null,
}))

vi.mock('antd', () => {
    const Modal = ({ open, title, children, onOk, okButtonProps }: Record<string, unknown>) => {
        const { icon, ...okRest } = (okButtonProps as Record<string, unknown>) ?? {}
        void icon
        return open ? (
            <div role="dialog">
                <span>{title as never}</span>
                {children as never}
                <button {...okRest} onClick={onOk as never}>ok</button>
            </div>
        ) : null
    }
    const TextArea = ({ onChange, ...rest }: Record<string, unknown>) => {
        const { autoSize, ...dom } = rest
        void autoSize
        return <textarea onChange={onChange as never} {...dom} />
    }
    const Input = { TextArea }
    const Typography = {
        Paragraph: ({ children }: { children?: unknown }) => <p>{children as never}</p>,
        Text: ({ children, type, ...rest }: Record<string, unknown>) => {
            void type
            return <span {...rest}>{children as never}</span>
        },
    }
    const notification = { error: vi.fn(), success: vi.fn(), warning: vi.fn() }
    return { Input, Modal, Typography, notification }
})

const project = { id: 'p1', name: 'Alpha', repository: 'design', status: 'EDITING', branch: 'main' } as never

describe('SaveProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(saveProject).mockResolvedValue()
        vi.mocked(getProjectBranches).mockResolvedValue([])
        vi.mocked(getRepositoryConfig).mockResolvedValue({ comment: { templates: {} } })
        // Default: identity configured
        vi.mocked(apiCall).mockResolvedValue({
            firstName: 'Jane',
            lastName: 'Doe',
            displayName: 'Jane Doe',
            email: 'jane@x.io',
        })
    })

    afterEach(() => {
        window.dispatchEvent(new CustomEvent('openMergeModal', { detail: null }))
    })

    it('commits with the entered comment when the identity is configured', async () => {
        const onSaved = vi.fn()
        const onClose = vi.fn()
        render(<SaveProjectModal open onClose={onClose} onSaved={onSaved} project={project} />)

        const comment = screen.getByTestId('save-project-comment')
        await userEvent.clear(comment)
        await userEvent.type(comment, 'my message')
        await userEvent.click(screen.getByTestId('save-project-submit'))

        await waitFor(() => expect(saveProject).toHaveBeenCalledWith('p1', 'my message'))
        await waitFor(() => expect(onSaved).toHaveBeenCalled())
        await waitFor(() => expect(onClose).toHaveBeenCalled())
    })

    it('commits without generating a default comment when the repository suggests none', async () => {
        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={project} />)

        const comment = screen.getByTestId('save-project-comment') as HTMLTextAreaElement
        expect(comment.value).toBe('')
        await userEvent.click(screen.getByTestId('save-project-submit'))

        await waitFor(() => expect(saveProject).toHaveBeenCalledWith('p1', undefined))
    })

    it('starts from the comment the repository suggests for a save', async () => {
        vi.mocked(getRepositoryConfig).mockResolvedValue({
            comment: { templates: { save: 'Project {project-name} was modified' } },
        })
        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={project} />)

        await waitFor(() => expect((screen.getByTestId('save-project-comment') as HTMLTextAreaElement).value)
            .toBe('Project Alpha was modified'))
    })

    it('starts from the restore template when the project is opened on an older revision', async () => {
        vi.mocked(getRepositoryConfig).mockResolvedValue({
            comment: { templates: { restoreFrom: 'Restored from {revision} by {author}' } },
        })
        const viewing = { ...project as object, status: 'VIEWING_VERSION', revision: 'abc123', modifiedBy: 'john' } as never
        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={viewing} />)

        await waitFor(() => expect((screen.getByTestId('save-project-comment') as HTMLTextAreaElement).value)
            .toBe('Restored from abc123 by john'))
    })

    it('refuses a comment the repository pattern forbids', async () => {
        vi.mocked(getRepositoryConfig).mockResolvedValue({
            comment: { userMessagePattern: 'EPBDS-\\d+.*', invalidUserMessageHint: 'Start with a ticket', templates: {} },
        })
        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={project} />)

        await userEvent.type(screen.getByTestId('save-project-comment'), 'no ticket')

        await waitFor(() => expect(screen.getByTestId('save-project-comment-error')).toHaveTextContent('Start with a ticket'))
        await userEvent.click(screen.getByTestId('save-project-submit'))
        expect(saveProject).not.toHaveBeenCalled()
    })

    it('prompts for commit identity when it is missing, then saves', async () => {
        vi.mocked(apiCall).mockResolvedValue({ firstName: '', lastName: '', displayName: '', email: '' })
        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={project} />)

        await userEvent.click(screen.getByTestId('save-project-submit'))
        await waitFor(() => expect(screen.getByTestId('commit-info-save')).toBeInTheDocument())
        expect(saveProject).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('commit-info-save'))
        await waitFor(() => expect(saveProject).toHaveBeenCalled())
    })

    it('opens the merge resolver on a save conflict', async () => {
        const listener = vi.fn()
        window.addEventListener('openMergeModal', listener as EventListener)
        vi.mocked(saveProject).mockRejectedValue(new ApiHttpError(409, 'conflict'))
        // identity check (GET), then conflicts check returns groups
        vi.mocked(apiCall)
            .mockResolvedValueOnce({
                firstName: 'Jane',
                lastName: 'Doe',
                displayName: 'Jane Doe',
                email: 'jane@x.io',
            })
            .mockResolvedValueOnce({ conflictGroups: [{ projectName: 'Alpha' }]})

        render(<SaveProjectModal open onClose={vi.fn()} onSaved={vi.fn()} project={project} />)
        await userEvent.click(screen.getByTestId('save-project-submit'))

        await waitFor(() => expect(listener).toHaveBeenCalled())
        const event = listener.mock.calls.at(-1)![0] as CustomEvent
        expect(event.detail.initialStep).toBe('conflicts')
        expect(event.detail.projectId).toBe('p1')
        window.removeEventListener('openMergeModal', listener as EventListener)
    })
})

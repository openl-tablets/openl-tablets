import React from 'react'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { OpenProjectModal, type OpenProjectModalDetail } from './OpenProjectModal'
// AntD's Modal leave animation never ends in jsdom, so gate it on `open` instead.
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const MockModal = ({ open, title, children, okText, cancelText, onOk, onCancel }: {
        open?: boolean
        title?: React.ReactNode
        children?: React.ReactNode
        okText?: React.ReactNode
        cancelText?: React.ReactNode
        onOk?: () => void
        onCancel?: () => void
    }) =>
        open ? (
            <div role="dialog">
                <div>{title}</div>
                {children}
                <button onClick={onOk}>{okText}</button>
                <button onClick={onCancel}>{cancelText}</button>
            </div>
        ) : null
    return { ...actual, Modal: MockModal }
})

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const dispatchOpen = async (detail: OpenProjectModalDetail | null) => {
    await act(async () => {
        window.dispatchEvent(new CustomEvent('openProjectModal', { detail }))
    })
}

const detail = (overrides?: Partial<OpenProjectModalDetail>): OpenProjectModalDetail => ({
    projectName: 'Consumer',
    repository: 'design',
    branch: 'side',
    dependencies: [{ name: 'Provider', id: 'dep-1', repository: 'design', branch: 'side' }],
    onConfirm: vi.fn(),
    ...overrides,
})

describe('OpenProjectModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('opens the dependencies by default and reports the answer', async () => {
        const opened = detail()
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(opened)

        expect(await screen.findByRole('dialog')).toBeInTheDocument()
        // Nothing sits outside the branch, so nothing is warned about.
        expect(screen.queryByTestId('open-project-branch-warning')).toBeNull()

        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        expect(opened.onConfirm).toHaveBeenCalledWith(true)
    })

    it('opens the project alone when the box is cleared', async () => {
        const opened = detail()
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(opened)

        await userEvent.click(screen.getByTestId('open-project-dependencies'))
        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        expect(opened.onConfirm).toHaveBeenCalledWith(false)
    })

    it('warns about the dependencies the branch of the project does not hold', async () => {
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(detail({
            dependencies: [
                { name: 'Provider', id: 'dep-1', repository: 'design', branch: 'side' },
                { name: 'Rates', id: 'dep-2', repository: 'design', branch: 'master' },
                // The server sends an unresolved dependency as its declared name alone.
                { name: 'Ghost', missing: true },
                // Two repositories keep no branches in step, so this one is not the user's to switch.
                { name: 'Shared Rates', id: 'dep-3', repository: 'shared', branch: 'trunk' },
            ],
        }))

        expect(await screen.findByTestId('open-project-branch-warning')).toBeInTheDocument()
        const warning = screen.getByTestId('open-project-branch-warning')
        expect(warning).toHaveTextContent('Rates')
        expect(warning).toHaveTextContent('Ghost')
        // The one already on this branch is not something the user has to act on, and neither is a
        // dependency of another repository, whose branches nothing keeps in step with this one.
        expect(warning).not.toHaveTextContent('Provider')
        expect(warning).not.toHaveTextContent('Shared Rates')
    })
})

import React from 'react'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { ProjectStatus } from '../../constants/project'
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
        // Nothing sits outside the branch, so nothing is warned about and there is nothing to move.
        expect(screen.queryByTestId('open-project-branch-warning')).toBeNull()
        expect(screen.queryByTestId('open-project-switch-dependencies')).toBeNull()

        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        expect(opened.onConfirm).toHaveBeenCalledWith({ openDependencies: true, switchDependencies: true })
    })

    it('opens the project alone when the box is cleared', async () => {
        const opened = detail()
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(opened)

        await userEvent.click(screen.getByTestId('open-project-dependencies'))
        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        // Nothing is opened, so nothing is moved either.
        expect(opened.onConfirm).toHaveBeenCalledWith({ openDependencies: false, switchDependencies: false })
    })

    it('moves the dependencies on another branch by default and warns about the ones that stay', async () => {
        const opened = detail({
            dependencies: [
                { name: 'Provider', id: 'dep-1', repository: 'design', branch: 'side' },
                { name: 'Rates', id: 'dep-2', repository: 'design', branch: 'master' },
                // Moving it would lose its unsaved changes, so it keeps its branch.
                { name: 'Draft', id: 'dep-4', repository: 'design', branch: 'master', status: ProjectStatus.Editing },
                // The server sends an unresolved dependency as its declared name alone.
                { name: 'Ghost', missing: true },
                // Two repositories keep no branches in step, so this one is not moved or warned about.
                { name: 'Shared Rates', id: 'dep-3', repository: 'shared', branch: 'trunk' },
            ],
        })
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(opened)

        expect(await screen.findByTestId('open-project-switch-dependencies')).toBeInTheDocument()
        const warning = screen.getByTestId('open-project-branch-warning')
        expect(warning).toHaveTextContent('Draft')
        expect(warning).toHaveTextContent('Ghost')
        expect(warning).not.toHaveTextContent('Rates')
        expect(warning).not.toHaveTextContent('Provider')

        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        expect(opened.onConfirm).toHaveBeenCalledWith({ openDependencies: true, switchDependencies: true })
    })

    it('warns about a transitive dependency the move may not reach', async () => {
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(detail({
            dependencies: [
                // The move does not go below a dependency of another repository.
                { name: 'Shared Rates', id: 'dep-3', repository: 'shared', branch: 'trunk' },
                { name: 'Base', id: 'dep-5', repository: 'design', branch: 'master', transitive: true },
            ],
        }))

        expect(await screen.findByTestId('open-project-branch-warning')).toHaveTextContent('Base')
    })

    it('does not warn about a transitive dependency the move reaches', async () => {
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(detail({
            dependencies: [
                { name: 'Rates', id: 'dep-2', repository: 'design', branch: 'master' },
                { name: 'Base', id: 'dep-5', repository: 'design', branch: 'master', transitive: true },
            ],
        }))

        expect(await screen.findByTestId('open-project-switch-dependencies')).toBeInTheDocument()
        expect(screen.queryByTestId('open-project-branch-warning')).toBeNull()
    })

    it('shows the move cleared while the dependencies are not opened', async () => {
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(detail({
            dependencies: [{ name: 'Rates', id: 'dep-2', repository: 'design', branch: 'master' }],
        }))

        const move = await screen.findByRole('checkbox', { name: 'browser.open_project.switch_dependencies' })
        expect(move).toBeChecked()

        await userEvent.click(screen.getByTestId('open-project-dependencies'))

        expect(move).not.toBeChecked()
        expect(move).toBeDisabled()
    })

    it('warns about every dependency left on another branch when they are not moved', async () => {
        const opened = detail({
            dependencies: [
                { name: 'Provider', id: 'dep-1', repository: 'design', branch: 'side' },
                { name: 'Rates', id: 'dep-2', repository: 'design', branch: 'master' },
                { name: 'Shared Rates', id: 'dep-3', repository: 'shared', branch: 'trunk' },
            ],
        })
        render(<MemoryRouter><OpenProjectModal /></MemoryRouter>)
        await dispatchOpen(opened)

        expect(screen.queryByTestId('open-project-branch-warning')).toBeNull()
        await userEvent.click(await screen.findByTestId('open-project-switch-dependencies'))

        const warning = screen.getByTestId('open-project-branch-warning')
        expect(warning).toHaveTextContent('Rates')
        expect(warning).not.toHaveTextContent('Provider')
        expect(warning).not.toHaveTextContent('Shared Rates')

        await userEvent.click(screen.getByText('browser.open_project.confirm_button'))

        expect(opened.onConfirm).toHaveBeenCalledWith({ openDependencies: true, switchDependencies: false })
    })
})

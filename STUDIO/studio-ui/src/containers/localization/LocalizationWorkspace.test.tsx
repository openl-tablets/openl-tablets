import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App as AntApp } from 'antd'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectStatus } from '../../constants/project'
import { applyMessageActions, fetchMessageKeys, fetchMessages } from '../../services/localization'
import { listModules } from '../../services/modules'
import { getProject, setProjectStatus } from '../../services/repositories'
import type { MessagesPage } from '../../types/localization'
import type { Project } from '../../types/projects'
import { LocalizationWorkspace } from './LocalizationWorkspace'

const { navigateMock, route } = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    // The address of the screen: the search the reader is on, as a link or a click left it.
    route: { search: new URLSearchParams(), shown: new URLSearchParams() },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }) }
})

vi.mock('react-router', async () => {
    const { useState } = await vi.importActual<typeof import('react')>('react')
    return {
        useNavigate: () => navigateMock,
        useParams: () => ({ projectId: 'p1' }),
        useSearchParams: () => {
            const [search, setSearch] = useState(route.search)
            return [search, (next: Record<string, string>) => {
                route.shown = new URLSearchParams(next)
                setSearch(route.shown)
            }]
        },
        useBlocker: () => ({ state: 'unblocked' }),
        Link: ({ children, to }: { children?: unknown, to?: string }) => <a href={to}>{children as never}</a>,
    }
})

vi.mock('../../services/repositories', () => ({ getProject: vi.fn(), setProjectStatus: vi.fn() }))
vi.mock('../../services/modules', () => ({ listModules: vi.fn() }))
vi.mock('../../services/localization', async importOriginal => ({
    ...await importOriginal<typeof import('../../services/localization')>(),
    applyMessageActions: vi.fn(),
    fetchMessageKeys: vi.fn(),
    fetchMessages: vi.fn(),
}))
vi.mock('../../hooks/useLiveProjectChanges', () => ({ useLiveProjectChanges: vi.fn() }))

// The header, the compilation and the problems of the project have screens and tests of their own.
vi.mock('../modules/ModuleActionBar', () => ({ ModuleActionBar: () => null }))
vi.mock('../modules/ModuleCompilationView', () => ({ CompilationMark: () => null }))
vi.mock('../modules/useModuleCompilation', () => ({
    useModuleCompilation: () => ({ state: 'idle', tests: 0, verifyNeeded: false }),
}))
vi.mock('../modules/useConfirmBefore', () => ({ useConfirmBefore: () => (go: () => void) => go() }))
vi.mock('../modules/useOverwriteConfirm', () => ({ useOverwriteConfirm: () => (go: () => void) => go() }))
vi.mock('../projects/CompileProblemsPanel', () => ({ CompileProblemsPanel: () => null }))
// A switcher of the trail or the title is named by its test id: what it lists is its own test's to tell.
vi.mock('../projects/CrumbSwitcher', () => ({ CrumbSwitcher: ({ testId }: { testId: string }) => <span data-testid={testId} /> }))
// jsdom lays nothing out: the table is given the room a screen would give it.
vi.mock('../../hooks/useElementSize', () => ({ useElementSize: () => ({ ref: vi.fn(), size: { height: 600, width: 1200 } }) }))

const project = (status = ProjectStatus.Opened): Project => ({
    id: 'p1',
    name: 'Sample',
    status,
    repository: 'design',
    capabilities: { canWrite: true },
} as unknown as Project)

const page = (content: MessagesPage['content'], languages: MessagesPage['languages'] = [{ tag: '' }, { tag: 'de' }]) => ({
    languages,
    content,
    pageNumber: 0,
    pageSize: 250,
    numberOfElements: content?.length ?? 0,
    total: content?.length ?? 0,
}) as MessagesPage

const GREETING = { key: 'greeting', values: { '': 'Hello', de: 'Hallo' } }
const TOTAL = { key: 'total', values: { '': 'Total' } }

const renderScreen = () => render(<AntApp><LocalizationWorkspace /></AntApp>)

const addFromTheRow = async (key: string, text: string) => {
    const keyInput = screen.getByTestId('localization-new-key')
    fireEvent.change(keyInput, { target: { value: key } })
    fireEvent.keyDown(keyInput, { key: 'Enter', code: 'Enter', keyCode: 13 })
    const textInput = screen.getByTestId('localization-new-key-text')
    fireEvent.change(textInput, { target: { value: text } })
    fireEvent.keyDown(textInput, { key: 'Enter', code: 'Enter', keyCode: 13 })
    await waitFor(() => expect(keyInput).toHaveValue(''))
}

describe('LocalizationWorkspace', () => {
    beforeEach(() => {
        route.search = new URLSearchParams()
        vi.mocked(getProject).mockResolvedValue(project())
        vi.mocked(listModules).mockResolvedValue([])
        vi.mocked(fetchMessageKeys).mockResolvedValue(['greeting', 'total'])
        vi.mocked(fetchMessages).mockResolvedValue(page([GREETING, TOTAL]))
        vi.mocked(applyMessageActions).mockResolvedValue(undefined)
    })

    it('wears the header of the module page: the trail picks another project, the title another module', async () => {
        renderScreen()

        const header = await screen.findByTestId('localization-header')
        // The project of the trail is the switcher of the module page, not a link of its own.
        expect(within(header).getByTestId('crumb-project')).toBeInTheDocument()
        expect(within(header).getByTestId('module-switcher')).toBeInTheDocument()
    })

    it('only shows the messages of a project the user may not write to, as the project tells it', async () => {
        vi.mocked(getProject).mockResolvedValue({ ...project(), capabilities: {} })
        renderScreen()

        const table = await screen.findByTestId('localization-table')
        expect(await within(table).findByText('greeting')).toBeInTheDocument()
        // Nothing writes: no band of actions, no row of a new key, no key taken out, no language added.
        expect(screen.queryByTestId('localization-toolbar')).not.toBeInTheDocument()
        expect(screen.queryByTestId('localization-new-key')).not.toBeInTheDocument()
        expect(screen.queryByTestId('localization-remove-key')).not.toBeInTheDocument()
        expect(screen.queryByTestId('localization-add-language')).not.toBeInTheDocument()
        // A text is read, not opened to be written.
        const cell = within(table).getAllByTestId('localization-cell')[0]!
        expect(cell).not.toHaveAttribute('role', 'button')
        fireEvent.click(cell)
        expect(screen.queryByTestId('localization-cell-input')).not.toBeInTheDocument()
    })

    it('offers a project the user may not write to no default file to create', async () => {
        vi.mocked(getProject).mockResolvedValue({ ...project(), capabilities: {} })
        vi.mocked(fetchMessageKeys).mockResolvedValue([])
        vi.mocked(fetchMessages).mockResolvedValue({ content: [], pageNumber: 0, pageSize: 250, numberOfElements: 0, total: 0 })
        renderScreen()

        expect(await screen.findByText('no_files')).toBeInTheDocument()
        expect(screen.queryByTestId('localization-create')).not.toBeInTheDocument()
    })

    it('asks for a closed project to be opened and reads none of its messages', async () => {
        vi.mocked(getProject).mockResolvedValue(project(ProjectStatus.Closed))
        renderScreen()

        expect(await screen.findByTestId('localization-closed')).toHaveTextContent('closed')
        expect(fetchMessages).not.toHaveBeenCalled()
        expect(fetchMessageKeys).not.toHaveBeenCalled()
        expect(screen.queryByTestId('localization-toolbar')).not.toBeInTheDocument()
    })

    it('opens a closed project from the screen and reads its messages then', async () => {
        vi.mocked(getProject).mockResolvedValueOnce(project(ProjectStatus.Closed))
        vi.mocked(setProjectStatus).mockResolvedValue(undefined)
        renderScreen()

        await userEvent.click(await screen.findByTestId('localization-closed-open'))

        // A project that declares no dependencies opens at once, its dependencies asked for as the Projects screen asks.
        expect(setProjectStatus).toHaveBeenCalledWith('p1', 'OPENED', { openDependencies: true })
        expect(await within(await screen.findByTestId('localization-table')).findByText('greeting')).toBeInTheDocument()
        expect(screen.queryByTestId('localization-closed')).not.toBeInTheDocument()
    })

    it('offers a project without localization files to create the default one', async () => {
        vi.mocked(fetchMessageKeys).mockResolvedValue([])
        vi.mocked(fetchMessages).mockResolvedValue({ content: [], pageNumber: 0, pageSize: 250, numberOfElements: 0, total: 0 })
        renderScreen()

        await userEvent.click(await screen.findByTestId('localization-create'))

        expect(await screen.findByText('no_keys')).toBeInTheDocument()
        expect(screen.getByTestId('localization-new-key')).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('localization-save'))
        await waitFor(() => expect(applyMessageActions).toHaveBeenCalledWith('p1', [{ operation: 'addLanguage', language: '' }]))
    })

    it('adds keys from the row under the table without asking the server, and sends them on save', async () => {
        renderScreen()
        await screen.findAllByTestId('localization-open-key')
        const readBefore = vi.mocked(fetchMessages).mock.calls.length

        await addFromTheRow('order.total', 'Order total')
        await addFromTheRow('order.tax', '')

        // The keys are added last, as the files will write them, and nothing is asked of the server for them.
        const keys = screen.getAllByTestId('localization-open-key').map(link => link.textContent)
        expect(keys).toEqual(['greeting', 'total', 'order.total', 'order.tax'])
        expect(fetchMessages).toHaveBeenCalledTimes(readBefore)

        await userEvent.click(screen.getByTestId('localization-save'))
        await waitFor(() => expect(applyMessageActions).toHaveBeenCalledWith('p1', [
            { operation: 'addKey', key: 'order.total', values: { '': 'Order total' } },
            { operation: 'addKey', key: 'order.tax', values: { '': '' } },
        ]))
        // Saved, the messages are read afresh from the server.
        await waitFor(() => expect(fetchMessages).toHaveBeenCalledTimes(readBefore + 1))
        expect(screen.getByTestId('localization-save')).toBeDisabled()
    })

    it('refuses a key the project writes already, or one just added', async () => {
        renderScreen()
        await screen.findAllByTestId('localization-open-key')
        await addFromTheRow('note', 'A note')

        for (const key of ['total', 'note']) {
            fireEvent.change(screen.getByTestId('localization-new-key'), { target: { value: key } })
            fireEvent.click(screen.getByTestId('localization-new-key-add'))
            expect(await screen.findByText('key_exists')).toBeInTheDocument()
        }
        expect(screen.getAllByTestId('localization-open-key')).toHaveLength(3)
    })

    it('narrows the keys down to those a link names untranslated, and leaves the address', async () => {
        route.search = new URLSearchParams({ untranslated: 'fr' })
        renderScreen()

        await waitFor(() => expect(fetchMessages).toHaveBeenLastCalledWith('p1',
            { search: '', onlyProblems: false, notTranslatedInto: ['fr'], order: null }, { page: 0, size: 250 }))
        // The filter is the reader's now: the plain address of the screen does not keep it.
        expect(route.shown.has('untranslated')).toBe(false)
    })

    it('refuses a key the table is narrowed past, as the rail holds every key', async () => {
        renderScreen()
        await screen.findAllByTestId('localization-open-key')

        await userEvent.click(screen.getByTestId('localization-key-filter'))
        const search = (await screen.findByTestId('localization-key-search')).querySelector('input')!
        fireEvent.change(search, { target: { value: 'gree' } })
        fireEvent.keyDown(search, { key: 'Enter', code: 'Enter', keyCode: 13 })
        await waitFor(() => expect(fetchMessages).toHaveBeenLastCalledWith('p1',
            { search: 'gree', onlyProblems: false, notTranslatedInto: [], order: null }, { page: 0, size: 250 }))

        fireEvent.change(screen.getByTestId('localization-new-key'), { target: { value: 'total' } })
        fireEvent.click(screen.getByTestId('localization-new-key-add'))

        expect(await screen.findByText('key_exists')).toBeInTheDocument()
        // The tree is read once, whatever the table is narrowed down to.
        expect(fetchMessageKeys).toHaveBeenCalledTimes(1)
    })

    it('keeps what was done when the save is refused', async () => {
        vi.mocked(applyMessageActions).mockRejectedValue(new Error('The key exists'))
        renderScreen()
        await screen.findAllByTestId('localization-open-key')
        await addFromTheRow('note', 'A note')

        await userEvent.click(screen.getByTestId('localization-save'))

        expect(await screen.findByText('save_failed')).toBeInTheDocument()
        expect(screen.getByTestId('localization-save')).toBeEnabled()
        expect(screen.getAllByTestId('localization-open-key').map(link => link.textContent)).toContain('note')
    })

    it('takes a key out once the reader confirms, and undoes it', async () => {
        renderScreen()
        await screen.findAllByTestId('localization-open-key')

        await userEvent.click(screen.getAllByTestId('localization-remove-key')[1]!)
        const dialog = await screen.findByRole('dialog')
        await userEvent.click(within(dialog).getByRole('button', { name: 'remove_key' }))

        await waitFor(() => expect(screen.getAllByTestId('localization-open-key').map(link => link.textContent))
            .toEqual(['greeting']))
        await userEvent.click(screen.getByTestId('localization-undo'))
        expect(screen.getAllByTestId('localization-open-key').map(link => link.textContent)).toEqual(['greeting', 'total'])
        expect(screen.getByTestId('localization-save')).toBeDisabled()
    })

    it('reads a key on its own page alone', async () => {
        route.search = new URLSearchParams({ key: 'greeting' })
        vi.mocked(fetchMessages).mockResolvedValue(page([GREETING]))
        renderScreen()

        const panel = await screen.findByTestId('localization-key')
        expect(within(panel).getAllByTestId('localization-key-language')).toHaveLength(2)
        expect(fetchMessages).toHaveBeenCalledWith('p1', expect.anything(), { key: 'greeting', page: 0, size: 1 })
        expect(fetchMessages).not.toHaveBeenCalledWith('p1', expect.anything(), { page: 0, size: 250 })
    })

    it('leads a key only a dependency writes to the project that writes it', async () => {
        route.search = new URLSearchParams({ key: 'shared.key' })
        vi.mocked(fetchMessages).mockResolvedValue(page([{
            key: 'shared.key',
            values: {},
            shared: { '': { value: 'Shared', project: 'Base', projectId: 'b1' } },
        }]))
        renderScreen()

        expect(await screen.findByTestId('localization-key-missing')).toHaveTextContent('key_in_dependency')
        await userEvent.click(screen.getByTestId('localization-key-owner'))
        expect(navigateMock).toHaveBeenCalledWith(expect.stringContaining('/projects/b1/'))
    })

    it('says a key no project writes is missing, and offers to add it once the default file is there', async () => {
        route.search = new URLSearchParams({ key: 'nowhere' })
        vi.mocked(fetchMessages).mockResolvedValue(page([]))
        renderScreen()

        expect(await screen.findByTestId('localization-key-missing')).toHaveTextContent('key_missing')
        expect(screen.getByTestId('localization-key-add')).toBeInTheDocument()
    })

    it('offers no key to add to a project without the default file', async () => {
        route.search = new URLSearchParams({ key: 'nowhere' })
        vi.mocked(fetchMessages).mockResolvedValue(page([], [{ tag: 'de' }]))
        renderScreen()

        expect(await screen.findByTestId('localization-key-missing')).toBeInTheDocument()
        expect(screen.queryByTestId('localization-key-add')).not.toBeInTheDocument()
    })

    it('keeps the texts as they are while they are saved', async () => {
        let written: () => void = () => undefined
        vi.mocked(applyMessageActions).mockReturnValue(new Promise(done => {
            written = () => done(undefined)
        }))
        renderScreen()
        await screen.findAllByTestId('localization-open-key')
        await addFromTheRow('note', 'A note')

        fireEvent.click(screen.getByTestId('localization-save'))

        await waitFor(() => expect(applyMessageActions).toHaveBeenCalled())
        expect(screen.getAllByTestId('localization-cell').every(cell => cell.getAttribute('role') === null)).toBe(true)
        written()
        await waitFor(() => expect(screen.getAllByTestId('localization-cell')[0]).toHaveAttribute('role', 'button'))
    })
})

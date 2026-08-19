import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from 'antd'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OverviewPanel } from './OverviewPanel'
import { ProjectStatus } from '../../constants/project'
import { getFileContent, rootFileExists, uploadFile, writeRootFile } from '../../services/files'
import { getProjectMigration, migrateProject } from '../../services/migration'
import type { Project } from '../../types/projects'

// Type without the per-keystroke delay: these forms render real antd and re-render on every character, so
// the default cadence dragged the descriptor-editing tests over the coverage build's timeout under load.
const user = userEvent.setup({ delay: null })

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, opts?: { count?: number }) => (opts?.count !== undefined ? `${key}:${opts.count}` : key),
    }),
}))

vi.mock('../../services/projectStatus', () => ({
    subscribeProjectStatus: () => ({ unsubscribe: () => {} }),
}))

// The declared text of the overview is read from (and written to) rules.xml; each test sets the file.
vi.mock('../../services/files', () => ({
    getFileContent: vi.fn(),
    rootFileExists: vi.fn().mockResolvedValue(true),
    uploadFile: vi.fn().mockResolvedValue(undefined),
    writeRootFile: vi.fn().mockResolvedValue(undefined),
}))

const setRulesXml = (xml: string) => vi.mocked(getFileContent).mockResolvedValue(xml)

/** The input behind the OpenAPI upload button — what a file is picked through. */
const fileInput = (): HTMLInputElement => {
    const input = document.querySelector('input[type="file"]')
    if (!(input instanceof HTMLInputElement)) {
        throw new Error('the OpenAPI upload input is not rendered')
    }
    return input
}

const openApiFile = () => new File(['{}'], 'spec.json', { type: 'application/json' })

vi.mock('../../services/repositories', () => ({
    getProjectFiles: vi.fn().mockResolvedValue([
        { type: 'file', path: 'api/openapi.json' },
        { type: 'file', path: 'rules/Main.xlsx' },
    ]),
    getTagTypes: vi.fn().mockResolvedValue([]),
}))

vi.mock('../../services/projectIndex', () => ({
    getProjectIndex: vi.fn().mockResolvedValue({
        projects: [{ name: 'Common Datatypes' }, { name: 'Rates' }],
        statuses: [],
        projectIndexHealth: {},
    }),
}))

vi.mock('../../services/migration', () => ({
    getProjectMigration: vi.fn().mockResolvedValue({
        rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
        rulesDeploy: { migratable: false },
    }),
    migrateProject: vi.fn().mockResolvedValue(undefined),
    EMPTY_MIGRATION: {
        rulesXml: { movableRootModules: [], migratable: false, newModules: [] },
        rulesDeploy: { migratable: false },
    },
}))

const setMigration = (rulesXml: { movableRootModules: string[], migratable: boolean, newModules?: string[] }) =>
    vi.mocked(getProjectMigration).mockResolvedValue({
        rulesXml: { newModules: [], ...rulesXml },
        rulesDeploy: { migratable: false },
    })

const base: Project = {
    id: 'p1',
    name: 'Proj',
    branch: '',
    comment: '',
    modifiedAt: '',
    modifiedBy: '',
    repository: 'design',
    revision: '',
    status: ProjectStatus.Local,
}

/** The panel under test — a reload rerenders this same element with a bumped token. */
const panel = (project: Project, { repoType, reloadToken = 0 }: { repoType?: string | undefined, reloadToken?: number } = {}) => (
    <App>
        <MemoryRouter>
            <OverviewPanel onUnlock={() => {}} project={project} reloadToken={reloadToken} repoLabel="design" repoType={repoType} />
        </MemoryRouter>
    </App>
)

const renderPanel = async (project: Project, repoType?: string) => {
    let result!: ReturnType<typeof render>
    // The overview reads rules.xml on mount; flush that async effect so nothing updates after the test.
    await act(async () => {
        result = render(panel(project, { repoType }))
        await Promise.resolve()
        await Promise.resolve()
    })
    return result
}

describe('OverviewPanel', () => {
    beforeEach(() => {
        // No rules.xml text by default; a test that needs it sets its own.
        setRulesXml('')
        // Nothing is written on mount, so what a test finds written is what the test itself caused.
        vi.mocked(uploadFile).mockClear()
        vi.mocked(writeRootFile).mockClear()
    })

    it('hides empty sections and metadata fields without placeholders', async () => {
        await renderPanel(base)
        expect(screen.getByText('browser.overview.status')).toBeInTheDocument()
        expect(screen.getByText('browser.overview.repository')).toBeInTheDocument()
        expect(screen.queryByText('browser.overview.description')).toBeNull()
        expect(screen.queryByText('browser.overview.depends_on')).toBeNull()
        expect(screen.queryByText('browser.overview.version_patterns')).toBeNull()
        expect(screen.queryByText('browser.overview.exposed_methods')).toBeNull()
        expect(screen.queryByText('browser.overview.branch')).toBeNull()
        expect(screen.queryByText('browser.overview.path')).toBeNull()
        expect(screen.queryByText('—')).toBeNull()
    })

    it('renders depends-on entries as links to the referenced projects', async () => {
        await renderPanel({
            ...base,
            dependencies: [{ name: 'Common Datatypes', id: 'dep-id-1', branch: 'main' }],
        })
        expect(screen.getByText('browser.overview.depends_on')).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Common Datatypes' })).toHaveAttribute('href', '/projects/dep-id-1')
    })

    it('shows a declared dependency the workspace does not have, and marks it as missing', async () => {
        await renderPanel({
            ...base,
            dependencies: [
                { name: 'Common Datatypes', id: 'dep-1' },
                { name: 'Ghost', missing: true },
            ],
        })

        expect(screen.getByRole('link', { name: 'Common Datatypes' })).toBeInTheDocument()
        // Nothing to open: the project it names is not there.
        expect(screen.queryByRole('link', { name: 'Ghost' })).toBeNull()
        expect(screen.getByText('Ghost')).toBeInTheDocument()
        expect(screen.getByTestId('dependency-missing-Ghost')).toBeInTheDocument()
    })

    it('tells a dependency another dependency brings in from one rules.xml declares', async () => {
        await renderPanel({
            ...base,
            dependencies: [
                { name: 'Common Datatypes', id: 'dep-1' },
                { name: 'Shared Rates', id: 'dep-2', transitive: true },
            ],
        })

        expect(screen.getByTestId('dependency-transitive-Shared Rates')).toBeInTheDocument()
        expect(screen.queryByTestId('dependency-transitive-Common Datatypes')).toBeNull()
    })

    it('marks the branch of a dependency the same way as the branch of the project', async () => {
        await renderPanel({
            ...base,
            dependencies: [{ name: 'Common Datatypes', id: 'dep-1', branch: 'main', branchDefault: true }],
            usedBy: [{ name: 'Auto Pricing', id: 'used-1', branch: 'release', branchProtected: true }],
        })

        expect(screen.getByTestId('dependency-branch-dep-1-default')).toBeInTheDocument()
        expect(screen.queryByTestId('dependency-branch-dep-1-protected')).toBeNull()
        expect(screen.getByTestId('dependency-branch-used-1-protected')).toBeInTheDocument()
        expect(screen.queryByTestId('dependency-branch-used-1-default')).toBeNull()
    })

    it('reads the descriptor text from rules.xml and the modules from the project', async () => {
        setRulesXml(`
            <project>
                <comment>A ruleset</comment>
                <properties-file-name-pattern>.*-%state%</properties-file-name-pattern>
                <exposed-methods><include>calc*</include><exclude>debug*</exclude></exposed-methods>
            </project>
        `)
        await renderPanel({
            ...base,
            tags: { Region: 'EU' },
            descriptor: { modules: [{ name: 'Pricing', path: 'rules/Pricing.xlsx' }]},
        })
        const left = within(screen.getByTestId('overview-left'))
        const right = within(screen.getByTestId('overview-right'))

        // From the file.
        expect(await left.findByText('A ruleset')).toBeInTheDocument()
        expect(left.getByText('.*-%state%')).toBeInTheDocument()
        expect(left.getByText('calc*')).toBeInTheDocument()
        expect(left.getByText('debug*')).toBeInTheDocument()
        // From the project.
        expect(left.getByText('browser.overview.modules:1')).toBeInTheDocument()
        expect(left.getByText('Pricing')).toBeInTheDocument()
        // The tags belong to the facts about the project, on the right.
        expect(right.getByText('browser.overview.tags')).toBeInTheDocument()
        expect(right.getByText('EU')).toBeInTheDocument()
    })

    it('counts the declarations of rules.xml, folding what a pattern matched under it', async () => {
        await renderPanel({
            ...base,
            descriptor: { modules: [
                { name: 'Main', path: 'rules/Main.xlsx' },
                { name: 'Rules', path: 'rules/**/*.xlsx', modules: [
                    { name: 'Auto', path: 'rules/Auto.xlsx' },
                    { name: 'Home', path: 'rules/Home.xlsx' },
                ]},
                { name: 'Tests', path: 'tests/**/*.xlsx', modules: []},
            ]},
        })

        // Three declarations, three rows — whatever the patterns matched.
        expect(screen.getByText('browser.overview.modules:3')).toBeInTheDocument()
        expect(screen.getByText('rules/**/*.xlsx')).toBeInTheDocument()
        expect(screen.queryByText('rules/Auto.xlsx')).toBeNull()
        // A pattern that matched nothing has no switcher, only the mark that it stands for nothing.
        expect(screen.getByTestId('module-unmatched-tests/**/*.xlsx')).toBeInTheDocument()

        await user.click(screen.getByTestId('module-matched-rules/**/*.xlsx'))

        expect(screen.getByText('rules/Auto.xlsx')).toBeInTheDocument()
        expect(screen.getByText('rules/Home.xlsx')).toBeInTheDocument()
    })

    it('marks defaulted modules and sources, and overlays a module method-filter from the file', async () => {
        setRulesXml(`
            <project>
                <modules>
                    <module>
                        <rules-root path="rules/**/*.xlsx"/>
                        <method-filter><includes><value>calc*</value></includes></method-filter>
                    </module>
                </modules>
            </project>
        `)
        await renderPanel({
            ...base,
            descriptor: {
                modules: [{ name: 'Rules', path: 'rules/**/*.xlsx' }],
                modulesDefault: true,
                sources: ['groovy/', 'lib/*.jar'],
                sourcesDefault: true,
            },
        })

        expect(screen.getByTestId('modules-default')).toBeInTheDocument()
        expect(screen.getByText('browser.overview.sources')).toBeInTheDocument()
        expect(screen.getByTestId('sources-default')).toBeInTheDocument()
        // The module's own filter is read from the file and shown under the module.
        expect(await screen.findByTestId('module-filter-rules/**/*.xlsx')).toBeInTheDocument()
    })

    it('reads a pattern that names no module of its own by what it stands for', async () => {
        await renderPanel({
            ...base,
            descriptor: { modules: [{ path: 'rules/**/*.xlsx', modules: [{ name: 'Auto', path: 'rules/Auto.xlsx' }]}]},
        })

        expect(screen.getByText('browser.overview.modules_pattern')).toBeInTheDocument()
    })

    it('reads the engine defaults as the rules and the tests found automatically', async () => {
        await renderPanel({
            ...base,
            descriptor: {
                modules: [
                    { path: 'rules/**/*.xlsx', modules: [{ name: 'Pricing', path: 'rules/Pricing.xlsx' }]},
                    { path: 'tests/**/*.xlsx', modules: [{ name: 'PricingTest', path: 'tests/PricingTest.xlsx' }]},
                ],
                modulesDefault: true,
                sources: ['groovy/', 'lib/*.jar'],
                sourcesDefault: true,
            },
        })

        // Defaulted modules read by what they are — never as a "pattern".
        expect(screen.getByText('browser.overview.modules_auto')).toBeInTheDocument()
        expect(screen.getByText('browser.overview.modules_auto_tests')).toBeInTheDocument()
        expect(screen.queryByText('browser.overview.modules_pattern')).toBeNull()
    })

    it('offers a migrate when the rules.xml can be rewritten, still allowing editing', async () => {
        setMigration({ movableRootModules: [], migratable: true })

        await renderPanel({ ...base, capabilities: { canWrite: true } })

        expect(await screen.findByTestId('overview-migrate')).toBeInTheDocument()
        expect(screen.getByTestId('overview-edit')).toBeInTheDocument()
        expect(screen.queryByTestId('overview-migrate-notice')).toBeNull()
    })

    it('disables the migrate when a rewrite would turn undeclared workbooks into modules', async () => {
        setMigration({ movableRootModules: [], migratable: true, newModules: ['rules/Extra.xlsx'] })

        await renderPanel({ ...base, capabilities: { canWrite: true } })

        // The server refuses this migrate, so the button is shown disabled rather than firing a doomed request.
        expect(await screen.findByTestId('overview-migrate')).toBeDisabled()
        // Only the migrate is blocked — editing the existing rules.xml is still allowed.
        expect(screen.getByTestId('overview-edit')).toBeInTheDocument()
    })

    it('withholds editing and offers a migrate when the project has no rules.xml', async () => {
        setMigration({ movableRootModules: ['Pricing.xlsx', 'Rating.xlsx'], migratable: true })

        await renderPanel({ ...base, capabilities: { canWrite: true } })

        expect(await screen.findByTestId('overview-migrate')).toBeInTheDocument()
        // Editing is withheld until the root workbooks are moved.
        expect(screen.queryByTestId('overview-edit')).toBeNull()
    })

    it('runs the migrate after confirmation', async () => {
        setMigration({ movableRootModules: [], migratable: true })
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(await screen.findByTestId('overview-migrate'))
        const dialog = await screen.findByRole('dialog')
        await user.click(within(dialog).getByRole('button', { name: 'browser.overview.migrate' }))

        await waitFor(() => expect(migrateProject).toHaveBeenCalledWith('p1', 'rulesXml'))
    })

    it('disables the edit button while a migrate is in flight', async () => {
        setMigration({ movableRootModules: [], migratable: true })
        let resolveMigrate!: () => void
        vi.mocked(migrateProject).mockReturnValueOnce(new Promise(resolve => { resolveMigrate = () => resolve(undefined) }))
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(await screen.findByTestId('overview-migrate'))
        const dialog = await screen.findByRole('dialog')
        await user.click(within(dialog).getByRole('button', { name: 'browser.overview.migrate' }))

        // While the migrate write is still in flight, editing the same rules.xml must be blocked.
        await waitFor(() => expect(screen.getByTestId('overview-edit')).toBeDisabled())

        await act(async () => {
            resolveMigrate()
            await Promise.resolve()
        })
    })

    it('keeps auto-discovered modules and sources read-only with a note while editing', async () => {
        await renderPanel({
            ...base,
            capabilities: { canWrite: true },
            descriptor: {
                modules: [{ path: 'rules/**/*.xlsx', modules: [{ name: 'Auto', path: 'rules/Auto.xlsx' }]}],
                modulesDefault: true,
                sources: ['groovy/', 'lib/*.jar'],
                sourcesDefault: true,
            },
        })

        await user.click(screen.getByTestId('overview-edit'))

        // The auto-discovered modules stay read-only with a note instead of the editable list.
        expect(screen.getByTestId('modules-readonly')).toBeInTheDocument()
        expect(screen.queryByTestId('edit-module-add')).toBeNull()
        // The engine-default classpath is read-only the same way.
        expect(screen.getByTestId('sources-readonly')).toBeInTheDocument()
        expect(screen.queryByTestId('edit-source-add')).toBeNull()
    })

    it('folds a section away by its own heading', async () => {
        setRulesXml('<project><comment>A ruleset</comment></project>')
        await renderPanel(base)

        expect(await screen.findByText('A ruleset')).toBeInTheDocument()

        await user.click(screen.getByText('browser.overview.description'))

        expect(screen.queryByText('A ruleset')).toBeNull()

        await user.click(screen.getByText('browser.overview.description'))

        expect(screen.getByText('A ruleset')).toBeInTheDocument()
    })

    it('offers no editing to a user who may not write the project', async () => {
        setRulesXml('<project><comment>A ruleset</comment></project>')
        await renderPanel(base)

        expect(screen.queryByTestId('overview-edit')).toBeNull()
    })

    it('edits the descriptor text in place and saves it back to rules.xml', async () => {
        setRulesXml('<project><name>P</name><comment>old</comment></project>')
        const onChanged = vi.fn()
        await act(async () => {
            render(
                <App>
                    <MemoryRouter>
                        <OverviewPanel
                            onChanged={onChanged}
                            onUnlock={() => {}}
                            project={{ ...base, capabilities: { canWrite: true } }}
                            repoLabel="design"
                        />
                    </MemoryRouter>
                </App>
            )
            await Promise.resolve()
            await Promise.resolve()
        })

        await user.click(screen.getByTestId('overview-edit'))
        // A version pattern is added and typed in — an empty list section shows in the editing view.
        await user.click(screen.getByTestId('edit-version-pattern-add'))
        await user.type(screen.getByTestId('edit-version-pattern-0'), '%lob%-%state%')
        // A declared module — its name and rules-root path.
        await user.click(screen.getByTestId('edit-module-add'))
        await user.type(screen.getByTestId('edit-module-0'), 'Main')
        await user.type(screen.getByTestId('edit-module-0-path'), 'rules/Main.xlsx')
        await user.clear(screen.getByTestId('edit-description'))
        await user.type(screen.getByTestId('edit-description'), 'new')
        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalledWith(
            'p1',
            'rules.xml',
            expect.stringContaining('<comment>new</comment>'),
            'overwrite'
        ))
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<properties-file-name-pattern>%lob%-%state%</properties-file-name-pattern>')
        expect(saved).toContain('<rules-root path="rules/Main.xlsx"/>')
        // The project name the user did not touch is carried over.
        expect(saved).toContain('<name>P</name>')
        expect(onChanged).toHaveBeenCalled()
    })

    it('edits the sources and the declared dependencies, and writes them to rules.xml', async () => {
        setRulesXml('<project><name>P</name></project>')
        await act(async () => {
            render(
                <App>
                    <MemoryRouter>
                        <OverviewPanel
                            onUnlock={() => {}}
                            project={{ ...base, capabilities: { canWrite: true }, descriptor: { sources: ['groovy/'], sourcesDefault: false } }}
                            repoLabel="design"
                        />
                    </MemoryRouter>
                </App>
            )
            await Promise.resolve()
            await Promise.resolve()
        })

        await user.click(screen.getByTestId('overview-edit'))
        // A source entry.
        await user.click(screen.getByTestId('edit-source-add'))
        await user.type(screen.getByTestId('edit-source-0'), 'lib/*.jar')
        // A dependency is picked from the existing projects, not typed by hand.
        await user.click(screen.getByTestId('edit-dependency-add'))
        fireEvent.mouseDown(within(screen.getByTestId('edit-dependency-0')).getByRole('combobox'))
        expect(await screen.findByRole('option', { name: 'Common Datatypes' })).toBeInTheDocument()
        expect(screen.getByRole('option', { name: 'Rates' })).toBeInTheDocument()

        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<classpath>')
        expect(saved).toContain('<entry path="lib/*.jar"/>')
    })




    it('configures the OpenAPI file and mode inline, the way the legacy editor did', async () => {
        setRulesXml('<project><name>P</name></project>')
        await act(async () => {
            render(
                <App>
                    <MemoryRouter>
                        <OverviewPanel
                            onUnlock={() => {}}
                            project={{ ...base, capabilities: { canWrite: true } }}
                            repoLabel="design"
                        />
                    </MemoryRouter>
                </App>
            )
            await Promise.resolve()
            await Promise.resolve()
        })

        await user.click(screen.getByTestId('overview-edit'))

        // The file is picked from the project, not typed: only specification files are offered.
        fireEvent.mouseDown(within(screen.getByTestId('edit-openapi-path')).getByRole('combobox'))
        expect(await screen.findByRole('option', { name: 'api/openapi.json' })).toBeInTheDocument()
        expect(screen.queryByRole('option', { name: 'rules/Main.xlsx' })).toBeNull()

        // Switching the mode to generation reveals the module names it needs.
        expect(screen.queryByTestId('edit-openapi-algorithm')).toBeNull()
        await user.click(screen.getByText('browser.overview.openapi_generation'))
        await user.type(screen.getByTestId('edit-openapi-algorithm'), 'Algorithms')

        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<mode>GENERATION</mode>')
        expect(saved).toContain('<algorithm-module-name>Algorithms</algorithm-module-name>')
    })

    it('removes the whole OpenAPI configuration when the file is cleared', async () => {
        setRulesXml(`
            <project>
                <name>P</name>
                <openapi>
                    <path>openapi.json</path>
                    <model-module-name>Models</model-module-name>
                    <algorithm-module-name>Algorithms</algorithm-module-name>
                    <mode>GENERATION</mode>
                </openapi>
            </project>
        `)
        await act(async () => {
            render(
                <App>
                    <MemoryRouter>
                        <OverviewPanel
                            onUnlock={() => {}}
                            project={{ ...base, capabilities: { canWrite: true } }}
                            repoLabel="design"
                        />
                    </MemoryRouter>
                </App>
            )
            await Promise.resolve()
            await Promise.resolve()
        })

        await user.click(screen.getByTestId('overview-edit'))
        // The clear control of the file picker is the way to drop the configuration.
        const select = screen.getByTestId('edit-openapi-path')
        fireEvent.mouseDown(select.querySelector('.ant-select-clear')!)

        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).not.toContain('<openapi>')
        expect(saved).not.toContain('<mode>')
    })

    it('picks an OpenAPI file without writing anything to the project', async () => {
        setRulesXml('<project><name>P</name></project>')
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(screen.getByTestId('overview-edit'))
        await user.upload(fileInput(), openApiFile())

        // The descriptor names the picked file at once, so the edit reads as it would be saved.
        expect(within(screen.getByTestId('edit-openapi-path')).getByTitle('spec.json')).toBeInTheDocument()
        // Nothing reached the project though, and the edit is still the user's to finish.
        expect(uploadFile).not.toHaveBeenCalled()
        expect(writeRootFile).not.toHaveBeenCalled()
        expect(screen.getByTestId('overview-save')).toBeInTheDocument()
    })

    it('writes a picked OpenAPI file into the project with the save that keeps it', async () => {
        setRulesXml('<project><name>P</name></project>')
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(screen.getByTestId('overview-edit'))
        const spec = openApiFile()
        await user.upload(fileInput(), spec)
        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(uploadFile).toHaveBeenCalledWith('p1', '', spec, 'spec.json'))
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<path>spec.json</path>')
    })

    it('keeps the edit when the picked OpenAPI file cannot be written', async () => {
        vi.mocked(uploadFile).mockRejectedValueOnce(new Error('no space left'))
        setRulesXml('<project><name>P</name></project>')
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(screen.getByTestId('overview-edit'))
        await user.upload(fileInput(), openApiFile())
        await user.click(screen.getByTestId('overview-save'))

        // A descriptor naming a file the project does not have is worse than an edit left to finish, so
        // the save stops and the picked file stays on offer for another try.
        await waitFor(() => expect(screen.getByTestId('overview-save')).toBeEnabled())
        expect(writeRootFile).not.toHaveBeenCalled()
        expect(within(screen.getByTestId('edit-openapi-path')).getByTitle('spec.json')).toBeInTheDocument()
    })

    it('forgets a picked OpenAPI file when the edit is cancelled', async () => {
        setRulesXml('<project><name>P</name></project>')
        await renderPanel({ ...base, capabilities: { canWrite: true } })

        await user.click(screen.getByTestId('overview-edit'))
        await user.upload(fileInput(), openApiFile())
        await user.click(screen.getByTestId('overview-cancel'))

        // The next edit starts from the descriptor as it is, so the dropped file is written by no save.
        await user.click(screen.getByTestId('overview-edit'))
        await user.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        expect(uploadFile).not.toHaveBeenCalled()
        expect(vi.mocked(writeRootFile).mock.calls.at(-1)![2]).not.toContain('spec.json')
    })

    it('keeps an edit under way when the project is read again', async () => {
        setRulesXml('<project><name>P</name></project>')
        const project = { ...base, capabilities: { canWrite: true } }
        const result = await renderPanel(project)

        await user.click(screen.getByTestId('overview-edit'))
        await user.type(screen.getByTestId('edit-description'), 'Rates')

        // Anything the project changes — an upload of its own, a commit by someone else — pings the page,
        // which re-reads the project underneath the edit.
        await act(async () => {
            result.rerender(panel(project, { reloadToken: 1 }))
            await Promise.resolve()
            await Promise.resolve()
        })

        // The edit ends by its own Save or Cancel, so it is still on screen with what was typed into it.
        expect(screen.getByTestId('overview-save')).toBeInTheDocument()
        expect(screen.getByTestId('edit-description')).toHaveValue('Rates')
    })

    it('lets a reload answer when the save it raced with failed', async () => {
        setRulesXml('<project><name>P</name></project>')
        const project = { ...base, capabilities: { canWrite: true } }
        const result = await renderPanel(project)

        await user.click(screen.getByTestId('overview-edit'))

        // A reload starts while the edit is open, and its read of rules.xml hangs.
        let rulesXml!: (xml: string) => void
        vi.mocked(getFileContent).mockReturnValueOnce(new Promise<string>(resolve => { rulesXml = resolve }))
        await act(async () => {
            result.rerender(panel(project, { reloadToken: 1 }))
            await Promise.resolve()
            await Promise.resolve()
        })

        // The save fails, so it wrote nothing and has no newer text of its own to keep.
        vi.mocked(writeRootFile).mockRejectedValueOnce(new Error('locked'))
        await user.click(screen.getByTestId('overview-save'))
        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())

        await act(async () => {
            rulesXml('<project><name>P</name><comment>from the reload</comment></project>')
            await Promise.resolve()
            await Promise.resolve()
        })
        await user.click(screen.getByTestId('overview-cancel'))

        // Nothing overtook the read, so what it brought is what the descriptor now reads as.
        expect(screen.getByText('from the reload')).toBeInTheDocument()
    })

    it('keeps the way out of an edit when a reload cannot read the descriptor', async () => {
        setRulesXml('<project><name>P</name></project>')
        const project = { ...base, capabilities: { canWrite: true } }
        const result = await renderPanel(project)

        await user.click(screen.getByTestId('overview-edit'))
        await user.type(screen.getByTestId('edit-description'), 'Rates')

        vi.mocked(rootFileExists).mockRejectedValueOnce(new Error('offline'))
        await act(async () => {
            result.rerender(panel(project, { reloadToken: 1 }))
            await Promise.resolve()
            await Promise.resolve()
        })

        // The read failed and says so, but the fields it failed under keep their Save and Cancel: the
        // sections show fields off the edit alone, so taking the toolbar away would strand the user.
        expect(screen.getByTestId('overview-descriptor-error')).toBeInTheDocument()
        expect(screen.getByTestId('overview-save')).toBeInTheDocument()
        expect(screen.getByTestId('overview-cancel')).toBeInTheDocument()
        expect(screen.getByTestId('edit-description')).toHaveValue('Rates')
    })

    it('keeps what a save wrote when a read from before it answers afterwards', async () => {
        setRulesXml('<project><name>P</name></project>')
        const project = { ...base, capabilities: { canWrite: true } }
        const result = await renderPanel(project)

        await user.click(screen.getByTestId('overview-edit'))
        await user.type(screen.getByTestId('edit-description'), 'Rates')

        // A reload starts while the edit is open, and its read of rules.xml hangs.
        let rulesXml!: (xml: string) => void
        vi.mocked(getFileContent).mockReturnValueOnce(new Promise<string>(resolve => { rulesXml = resolve }))
        await act(async () => {
            result.rerender(panel(project, { reloadToken: 1 }))
            // Two flushes, as the other reload tests use: the read awaits its lookup before the content.
            await Promise.resolve()
            await Promise.resolve()
        })

        await user.click(screen.getByTestId('overview-save'))
        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())

        // The read finally answers with the text from before the save.
        await act(async () => {
            rulesXml('<project><name>P</name><comment>stale</comment></project>')
            await Promise.resolve()
            await Promise.resolve()
        })

        // The save is the newer word on the file, so what it wrote is what the read view shows.
        expect(screen.getByText('Rates')).toBeInTheDocument()
        expect(screen.queryByText('stale')).toBeNull()
    })

    it('marks a protected current branch with a shield', async () => {
        const { container } = await renderPanel({ ...base, branch: 'main', branchProtected: true })
        expect(container.querySelector('.anticon-safety')).toBeTruthy()
    })

    it('badges the branch that is the repository main one', async () => {
        await renderPanel({ ...base, branch: 'main', branchDefault: true })
        expect(screen.getByTestId('overview-branch-default')).toBeInTheDocument()
    })

    it('shows no default badge for an ordinary branch', async () => {
        await renderPanel({ ...base, branch: 'feature/rates' })
        expect(screen.queryByTestId('overview-branch-default')).toBeNull()
    })

    it('shows no shield for an unprotected branch', async () => {
        const { container } = await renderPanel({ ...base, branch: 'main' })
        expect(container.querySelector('.anticon-safety')).toBeNull()
    })

    it('shows the revision shortened, keeping the full one for copying', async () => {
        await renderPanel({ ...base, revision: 'abcdef1234567890' })

        expect(screen.getByText('abcdef')).toBeInTheDocument()
        expect(screen.queryByText('abcdef1234567890')).toBeNull()
    })

    it('shows a logical repository icon', async () => {
        await renderPanel(base, 'repo-jdbc')

        expect(screen.getByTestId('repo-badge-database')).toBeInTheDocument()
    })

    it('keeps the descriptor toolbar in place while the project is read again', async () => {
        const project = { ...base, capabilities: { canWrite: true } }
        const result = await renderPanel(project)
        expect(screen.getByTestId('overview-edit')).toBeInTheDocument()

        // A reload re-reads rules.xml. Taking the toolbar away until the new text lands would read as a
        // flicker, not as progress — the reload shows itself over the whole project instead.
        let rulesXml!: (xml: string) => void
        vi.mocked(getFileContent).mockReturnValueOnce(new Promise<string>(resolve => { rulesXml = resolve }))
        result.rerender(panel(project, { reloadToken: 1 }))

        expect(screen.getByTestId('overview-edit')).toBeInTheDocument()
        // In place, but not on offer: an edit started here would take its draft from the descriptor about
        // to be replaced and save it back over the newer one.
        expect(screen.getByTestId('overview-edit')).toBeDisabled()

        await act(async () => {
            rulesXml('')
            await Promise.resolve()
        })
        expect(screen.getByTestId('overview-edit')).toBeInTheDocument()
        expect(screen.getByTestId('overview-edit')).not.toBeDisabled()
    })
})

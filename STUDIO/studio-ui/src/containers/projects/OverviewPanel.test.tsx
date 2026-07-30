import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from 'antd'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OverviewPanel } from './OverviewPanel'
import { ProjectStatus } from '../../constants/project'
import { getFileContent } from '../../services/files'
import type { Project } from '../../types/projects'

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
    writeRootFile: vi.fn().mockResolvedValue(undefined),
}))

const setRulesXml = (xml: string) => vi.mocked(getFileContent).mockResolvedValue(xml)

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

vi.mock('./ManageBranchesModal', () => ({
    ManageBranchesModal: ({ open }: { open: boolean }) => open ? <div data-testid="manage-branches-modal" /> : null,
}))

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

const renderPanel = async (project: Project, repoType?: string) => {
    let result!: ReturnType<typeof render>
    // The overview reads rules.xml on mount; flush that async effect so nothing updates after the test.
    await act(async () => {
        result = render(
            <App>
                <MemoryRouter>
                    <OverviewPanel onUnlock={() => {}} project={project} repoLabel="design" repoType={repoType} />
                </MemoryRouter>
            </App>
        )
        await Promise.resolve()
        await Promise.resolve()
    })
    return result
}

describe('OverviewPanel', () => {
    beforeEach(() => {
        // No rules.xml text by default; a test that needs it sets its own.
        setRulesXml('')
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

        await userEvent.click(screen.getByTestId('module-matched-rules/**/*.xlsx'))

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

    it('folds a section away by its own heading', async () => {
        setRulesXml('<project><comment>A ruleset</comment></project>')
        await renderPanel(base)

        expect(await screen.findByText('A ruleset')).toBeInTheDocument()

        await userEvent.click(screen.getByText('browser.overview.description'))

        expect(screen.queryByText('A ruleset')).toBeNull()

        await userEvent.click(screen.getByText('browser.overview.description'))

        expect(screen.getByText('A ruleset')).toBeInTheDocument()
    })

    it('offers no editing to a user who may not write the project', async () => {
        setRulesXml('<project><comment>A ruleset</comment></project>')
        await renderPanel(base)

        expect(screen.queryByTestId('overview-edit')).toBeNull()
    })

    it('edits the descriptor text in place and saves it back to rules.xml', async () => {
        const { writeRootFile } = await import('../../services/files')
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

        await userEvent.click(screen.getByTestId('overview-edit'))
        // A version pattern is added and typed in — an empty list section shows in the editing view.
        await userEvent.click(screen.getByTestId('edit-version-pattern-add'))
        await userEvent.type(screen.getByTestId('edit-version-pattern-0'), '%lob%-%state%')
        // A declared module — its name and rules-root path.
        await userEvent.click(screen.getByTestId('edit-module-add'))
        await userEvent.type(screen.getByTestId('edit-module-0'), 'Main')
        await userEvent.type(screen.getByTestId('edit-module-0-path'), 'rules/Main.xlsx')
        await userEvent.clear(screen.getByTestId('edit-description'))
        await userEvent.type(screen.getByTestId('edit-description'), 'new')
        await userEvent.click(screen.getByTestId('overview-save'))

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
        const { writeRootFile } = await import('../../services/files')
        setRulesXml('<project><name>P</name></project>')
        await act(async () => {
            render(
                <App>
                    <MemoryRouter>
                        <OverviewPanel
                            onUnlock={() => {}}
                            project={{ ...base, capabilities: { canWrite: true }, descriptor: { sources: ['groovy/'], sourcesDefault: true } }}
                            repoLabel="design"
                        />
                    </MemoryRouter>
                </App>
            )
            await Promise.resolve()
            await Promise.resolve()
        })

        await userEvent.click(screen.getByTestId('overview-edit'))
        // A source entry.
        await userEvent.click(screen.getByTestId('edit-source-add'))
        await userEvent.type(screen.getByTestId('edit-source-0'), 'lib/*.jar')
        // A dependency is picked from the existing projects, not typed by hand.
        await userEvent.click(screen.getByTestId('edit-dependency-add'))
        fireEvent.mouseDown(within(screen.getByTestId('edit-dependency-0')).getByRole('combobox'))
        expect(await screen.findByRole('option', { name: 'Common Datatypes' })).toBeInTheDocument()
        expect(screen.getByRole('option', { name: 'Rates' })).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<classpath>')
        expect(saved).toContain('<entry path="lib/*.jar"/>')
    })




    it('configures the OpenAPI file and mode inline, the way the legacy editor did', async () => {
        const { writeRootFile } = await import('../../services/files')
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

        await userEvent.click(screen.getByTestId('overview-edit'))

        // The file is picked from the project, not typed: only specification files are offered.
        fireEvent.mouseDown(within(screen.getByTestId('edit-openapi-path')).getByRole('combobox'))
        expect(await screen.findByRole('option', { name: 'api/openapi.json' })).toBeInTheDocument()
        expect(screen.queryByRole('option', { name: 'rules/Main.xlsx' })).toBeNull()

        // Switching the mode to generation reveals the module names it needs.
        expect(screen.queryByTestId('edit-openapi-algorithm')).toBeNull()
        await userEvent.click(screen.getByText('browser.overview.openapi_generation'))
        await userEvent.type(screen.getByTestId('edit-openapi-algorithm'), 'Algorithms')

        await userEvent.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).toContain('<mode>GENERATION</mode>')
        expect(saved).toContain('<algorithm-module-name>Algorithms</algorithm-module-name>')
    })

    it('removes the whole OpenAPI configuration when the file is cleared', async () => {
        const { writeRootFile } = await import('../../services/files')
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

        await userEvent.click(screen.getByTestId('overview-edit'))
        // The clear control of the file picker is the way to drop the configuration.
        const select = screen.getByTestId('edit-openapi-path')
        fireEvent.mouseDown(select.querySelector('.ant-select-clear')!)

        await userEvent.click(screen.getByTestId('overview-save'))

        await waitFor(() => expect(writeRootFile).toHaveBeenCalled())
        const saved = vi.mocked(writeRootFile).mock.calls.at(-1)![2]
        expect(saved).not.toContain('<openapi>')
        expect(saved).not.toContain('<mode>')
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

    it('opens branch management for a project whose branches the user may manage', async () => {
        await renderPanel({ ...base, branch: 'main', capabilities: { canManageBranches: true } })

        await userEvent.click(screen.getByTestId('manage-branches'))

        expect(screen.getByTestId('manage-branches-modal')).toBeInTheDocument()
    })

    it('offers no branch management without write access', async () => {
        await renderPanel({ ...base, branch: 'main' })

        expect(screen.queryByTestId('manage-branches')).toBeNull()
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
})

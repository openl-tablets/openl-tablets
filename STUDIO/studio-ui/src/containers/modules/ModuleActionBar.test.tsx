import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Project } from '../../types/projects'
import { ModuleActionBar } from './ModuleActionBar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const navigate = vi.fn()
vi.mock('react-router-dom', async importOriginal => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigate,
}))

vi.mock('../projects/RevisionsPanel', () => ({
    RevisionsPanel: ({ projectId }: { projectId: string }) => <div data-testid="revisions-panel">{projectId}</div>,
}))

vi.mock('../projects/LocalChangesView', () => ({
    LocalChangesView: ({ onRestored }: { onRestored?: () => void }) => (
        <div data-testid="local-changes-view">
            <button data-testid="local-changes-restored" onClick={() => onRestored?.()} type="button" />
        </div>
    ),
}))

vi.mock('../execution/TestsResultModal', () => ({ TestsResultModal: () => <div data-testid="tests-result" /> }))

vi.mock('../projects/SaveProjectModal', () => ({
    SaveProjectModal: ({ open }: { open: boolean }) => open ? <div data-testid="save-project-modal" /> : null,
}))

vi.mock('../projects/CopyProjectModal', () => ({
    CopyProjectModal: ({ open }: { open: boolean }) => open ? <div data-testid="copy-project-modal" /> : null,
}))

const openMergeDialog = vi.fn()
vi.mock('../projects/branchDialogs', () => ({ openMergeDialog: (...args: unknown[]) => openMergeDialog(...args) }))

vi.mock('../../services/repositories', () => ({ getDesignRepositories: () => Promise.resolve([]) }))

const project = (capabilities: Project['capabilities']): Project => ({
    id: 'p1',
    name: 'Rating',
    revision: 'abc123',
    capabilities,
} as Project)

const bar = async (capabilities: Project['capabilities'] = { canViewHistory: true } as Project['capabilities']) => {
    render(<ModuleActionBar moduleName="Claims" project={project(capabilities)} />)
    await act(async () => {
        await Promise.resolve()
    })
}

describe('ModuleActionBar', () => {
    it('offers the project\'s tests through the launch panel, module-only until the project is compiled', async () => {
        const opened = vi.fn()
        window.addEventListener('openTestsLaunch', opened)
        render(<ModuleActionBar moduleName="Claims" project={project({} as Project['capabilities'])} testCount={3} />)
        await act(async () => {
            await Promise.resolve()
        })

        await userEvent.click(screen.getByTestId('module-test'))

        window.removeEventListener('openTestsLaunch', opened)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        // Without a table the panel runs every test of the project; the module is only what it can be narrowed to.
        expect(detail).toMatchObject({ projectId: 'p1', moduleName: 'Claims', moduleOnlyLocked: true })
        expect(detail).not.toHaveProperty('tableId')
    })

    it('offers the project\'s own actions by the rules the project screen offers them', async () => {
        await bar({ canSave: true, canMerge: true, canDeploy: true, canCopy: true } as Project['capabilities'])

        expect(screen.getByTestId('module-save')).toBeInTheDocument()
        expect(screen.getByTestId('module-sync')).toBeInTheDocument()
        expect(screen.getByTestId('module-deploy')).toBeInTheDocument()
        expect(screen.getByTestId('module-copy')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('module-save'))
        expect(await screen.findByTestId('save-project-modal')).toBeInTheDocument()
    })

    it('withholds what the project does not allow', async () => {
        await bar({ canViewHistory: true } as Project['capabilities'])

        // Saving, syncing, deploying, copying and replacing the workbook are the project's to allow.
        expect(screen.queryByTestId('module-save')).toBeNull()
        expect(screen.queryByTestId('module-sync')).toBeNull()
        expect(screen.queryByTestId('module-deploy')).toBeNull()
        expect(screen.queryByTestId('module-copy')).toBeNull()
        expect(screen.queryByTestId('module-update')).toBeNull()
    })

    it('copies the project from the editor, through the project\'s own dialog', async () => {
        await bar({ canCopy: true } as Project['capabilities'])

        await userEvent.click(screen.getByTestId('module-copy'))

        expect(await screen.findByTestId('copy-project-modal')).toBeInTheDocument()
    })

    it('syncs the project from the editor, through the dialog that reads its branches', async () => {
        await bar({ canMerge: true } as Project['capabilities'])

        await userEvent.click(screen.getByTestId('module-sync'))

        expect(openMergeDialog).toHaveBeenCalledWith(expect.objectContaining({ id: 'p1' }), expect.any(Function))
    })

    it('replaces the module workbook where the project may be written to', async () => {
        const opened = vi.fn()
        window.addEventListener('openUpdateModuleModal', opened)
        render(
            <ModuleActionBar
                moduleName="Claims"
                modulePath="rules/Claims.xlsx"
                project={project({ canWrite: true } as Project['capabilities'])}
            />
        )
        await act(async () => {
            await Promise.resolve()
        })

        await userEvent.click(screen.getByTestId('module-update'))

        window.removeEventListener('openUpdateModuleModal', opened)
        expect((opened.mock.calls[0]?.[0] as CustomEvent).detail)
            .toMatchObject({ projectId: 'p1', modulePath: 'rules/Claims.xlsx' })
    })

    it('offers Verify only while the module is waiting to be compiled', async () => {
        const onVerify = vi.fn()
        render(<ModuleActionBar verifyNeeded moduleName="Claims" onVerify={onVerify} project={project({} as Project['capabilities'])} />)
        await act(async () => {
            await Promise.resolve()
        })

        await userEvent.click(screen.getByTestId('module-verify'))

        expect(onVerify).toHaveBeenCalled()
    })

    it('carries no Verify where compilation follows every edit on its own', async () => {
        render(<ModuleActionBar moduleName="Claims" project={project({} as Project['capabilities'])} />)
        await act(async () => {
            await Promise.resolve()
        })

        expect(screen.queryByTestId('module-verify')).not.toBeInTheDocument()
    })

    it('writes a new table into the module, through the dialog that builds it', async () => {
        const opened = vi.fn()
        window.addEventListener('openCreateTableModal', opened)
        const onTableCreated = vi.fn()
        render(
            <ModuleActionBar
                moduleName="Claims"
                onTableCreated={onTableCreated}
                project={project({ canWrite: true } as Project['capabilities'])}
            />
        )
        await act(async () => {
            await Promise.resolve()
        })

        await userEvent.click(screen.getByTestId('module-createTable'))

        window.removeEventListener('openCreateTableModal', opened)
        const { detail } = opened.mock.calls[0]?.[0] as CustomEvent
        // The module being read is where the table lands, unless the dialog is told otherwise.
        expect(detail).toMatchObject({ projectId: 'p1', currentModuleName: 'Claims' })
        detail.onSuccess({ id: 'new-1' }, 'Claims')
        expect(onTableCreated).toHaveBeenCalledWith({ id: 'new-1' }, 'Claims')
    })

    it('offers no new table to a reader who may not write to the project', async () => {
        await bar()

        expect(screen.queryByTestId('module-createTable')).toBeNull()
    })

    it('reads the project history in a window rather than on the project screen', async () => {
        await bar()

        await userEvent.click(screen.getByTestId('module-more'))
        await userEvent.click(await screen.findByText('browser.module.revisions'))

        expect(await screen.findByTestId('revisions-panel')).toHaveTextContent('p1')
        // Leaving the editor for the project screen would take the reader away from the module they are reading.
        expect(navigate).not.toHaveBeenCalled()
    })

    it('reads the module again once a version has been restored from the local changes', async () => {
        const onRevisionOpened = vi.fn()
        render(
            <ModuleActionBar
                moduleName="Claims"
                onRevisionOpened={onRevisionOpened}
                project={project({ canViewHistory: true } as Project['capabilities'])}
            />
        )
        await act(async () => {
            await Promise.resolve()
        })
        await userEvent.click(screen.getByTestId('module-more'))
        await userEvent.click(await screen.findByText('browser.module.local_changes'))

        await userEvent.click(await screen.findByTestId('local-changes-restored'))

        // The workbook is another version now, so the module is read from it again — which is what the
        // editor was missing: only a reload of the page used to show the restored version.
        expect(onRevisionOpened).toHaveBeenCalledTimes(1)
    })

    it('compares two Excel files of the reader\'s own, not two revisions of the project', async () => {
        const opened = vi.spyOn(window, 'open').mockReturnValue(null)
        try {
            await bar()

            await userEvent.click(screen.getByTestId('module-more'))
            await userEvent.click(await screen.findByText('browser.module.compare'))

            // The window opens on the file picker: no project is named, so no revision of one is offered.
            expect(opened).toHaveBeenCalledTimes(1)
            expect(opened.mock.calls[0]?.[0]).toBe('/compare')
        } finally {
            opened.mockRestore()
        }
    })

    it('offers no history to a reader who may not read it', async () => {
        await bar({ canViewHistory: false } as Project['capabilities'])

        await userEvent.click(screen.getByTestId('module-more'))

        expect(await screen.findByText('browser.module.local_changes')).toBeInTheDocument()
        expect(screen.queryByText('browser.module.revisions')).toBeNull()
    })
})

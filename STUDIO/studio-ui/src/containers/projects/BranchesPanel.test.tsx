import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BranchesPanel } from './BranchesPanel'
import {
    createProjectBranch,
    getProjectBranches,
    setSelectedBranches,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => error instanceof Error && error.message === 'modified'),
    setSelectedBranches: vi.fn(),
    switchProjectBranch: vi.fn(),
    createProjectBranch: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }), cx: (...a: unknown[]) => a.filter(Boolean).join(' ') }),
}))

vi.mock('./projectsTheme', () => ({ MOCKUP: { fontMono: 'mono' } }))
vi.mock('./MonoChip', () => ({
    MonoChip: ({ children, ...rest }: Record<string, unknown>) => <span {...rest}>{children as never}</span>,
}))

vi.mock('antd', () => {
    const domProps = (props: unknown): Record<string, unknown> => {
        if (!props || typeof props !== 'object') {
            return {}
        }
        const { danger, ...dom } = props as Record<string, unknown>
        void danger
        return dom
    }
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { size, danger, type, loading, ...dom } = rest
        void size; void danger; void type; void loading
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Skeleton = () => <div>loading</div>
    const Checkbox = ({ checked, disabled, onChange, ...rest }: Record<string, unknown>) =>
        <input checked={checked as boolean} disabled={disabled as boolean} onChange={onChange as never} type="checkbox" {...rest} />
    const Switch = ({ checked, onChange, ...rest }: Record<string, unknown>) => {
        const { size, ...dom } = rest
        void size
        return <input checked={checked as boolean} onChange={e => (onChange as (v: boolean) => void)(e.target.checked)} role="switch" type="checkbox" {...dom} />
    }
    const Select = ({ options, value, onChange, ...rest }: Record<string, unknown>) => {
        const { loading, popupMatchSelectWidth, ...dom } = rest
        void loading; void popupMatchSelectWidth
        return (
            <select onChange={e => (onChange as (v: string) => void)(e.target.value)} value={value as string} {...dom}>
                {(options as Array<{ value: string; label: string }>).map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
        )
    }
    const Search = ({ value, onChange, ...rest }: Record<string, unknown>) => {
        const { allowClear, ...dom } = rest
        void allowClear
        return <input onChange={onChange as never} value={value as string} {...dom} />
    }
    const Input = Object.assign(
        ({ onChange, value, onPressEnter, ...rest }: Record<string, unknown>) => {
            void onPressEnter
            return <input onChange={onChange as never} value={value as string} {...rest} />
        },
        { Search }
    )
    const Popconfirm = ({ children, onConfirm, disabled }: Record<string, unknown>) =>
        disabled ? <>{children as never}</> : <span onClick={onConfirm as never}>{children as never}</span>
    const Modal = ({ children, open, onOk, okButtonProps, okText }: Record<string, unknown>) =>
        open ? (
            <div>
                {children as never}
                <button {...domProps(okButtonProps)} onClick={onOk as never}>{okText as never}</button>
            </div>
        ) : null
    const Alert = ({ title }: Record<string, unknown>) => <div>{title as never}</div>
    const notification = { error: vi.fn(), success: vi.fn() }
    return { Alert, Button, Checkbox, Input, Modal, Popconfirm, Select, Skeleton, Switch, Tag, Tooltip, notification }
})

const BRANCHES = [
    { name: 'main', protected: false, base: true, bypassEligible: false },
    { name: 'dev', protected: false, base: false, bypassEligible: false },
    { name: 'feature', protected: false, base: false, bypassEligible: false },
]

function branch(name: string): ProjectBranch {
    return { name, protected: false, base: false, bypassEligible: false }
}

function deferred<T>() {
    let resolve!: (value: T) => void
    let reject!: (reason?: unknown) => void
    const promise = new Promise<T>((resolvePromise, rejectPromise) => {
        resolve = resolvePromise
        reject = rejectPromise
    })
    return { promise, resolve, reject }
}

async function renderPanel(props: Partial<{ selectedBranches: string[]; currentBranch: string; canWrite: boolean }> = {}) {
    await act(async () => {
        render(
            <BranchesPanel
                canWrite={props.canWrite ?? true}
                currentBranch={props.currentBranch ?? 'main'}
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Proj"
                repositoryId="repo1"
                selectedBranches={props.selectedBranches ?? ['main', 'dev']}
            />
        )
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('BranchesPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue(BRANCHES)
        vi.mocked(setSelectedBranches).mockResolvedValue()
        vi.mocked(switchProjectBranch).mockResolvedValue()
        vi.mocked(createProjectBranch).mockResolvedValue()
    })

    it('lists every branch but offers only the selected ones in the current-branch dropdown', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        const options = within(screen.getByTestId('branches-current')).getAllByRole('option').map(o => o.textContent)
        expect(options).toEqual(['main', 'dev'])
    })

    it('filters the branch table by name', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-dev')).toBeTruthy())

        await userEvent.type(screen.getByTestId('branches-filter'), 'feat')

        expect(screen.queryByTestId('branch-merge-dev')).toBeNull()
        expect(screen.getByTestId('branch-merge-feature')).toBeTruthy()
    })

    it('opens merge modal with the clicked branch preselected', async () => {
        const events: CustomEvent[] = []
        const listener = (e: Event) => events.push(e as CustomEvent)
        window.addEventListener('openMergeModal', listener)
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        await userEvent.click(screen.getByTestId('branch-merge-feature'))

        expect(events).toHaveLength(1)
        expect(events[0]!.detail).toMatchObject({
            projectId: 'p1',
            projectName: 'Proj',
            repositoryId: 'repo1',
            currentBranch: 'main',
            targetBranch: 'feature',
        })
        window.removeEventListener('openMergeModal', listener)
    })

    it('reveals checkboxes only in configure mode and saves the new selection', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())
        expect(screen.queryByTestId('branch-select-feature')).toBeNull()

        await userEvent.click(screen.getByTestId('branches-config-toggle'))
        await userEvent.click(screen.getByTestId('branch-select-feature'))
        await userEvent.click(screen.getByTestId('branches-save-selection'))

        await waitFor(() => expect(setSelectedBranches).toHaveBeenCalledTimes(1))
        const [projectId, branches] = vi.mocked(setSelectedBranches).mock.calls[0]!
        expect(projectId).toBe('p1')
        expect([...branches].sort()).toEqual(['dev', 'feature', 'main'])
    })

    it('shows the last commit', async () => {
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: false, base: true, bypassEligible: false },
            {
                name: 'feature', protected: false, base: false, bypassEligible: false,
                lastCommit: { author: 'jane', modifiedAt: '2024-01-02T00:00:00Z', message: 'Add feature', revision: 'abc1234' },
            },
        ])
        await renderPanel({ currentBranch: 'main', selectedBranches: ['main', 'feature']})
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        expect(screen.getByTestId('branch-commit-feature').textContent).toContain('jane')
        expect(screen.getByTestId('branch-commit-message-feature').textContent).toContain('Add feature')
        expect(screen.getByTestId('branch-commit-revision-feature').textContent).toBe('abc1234')
    })

    it('refetches branches after switching the current branch', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())
        expect(getProjectBranches).toHaveBeenCalledTimes(1)

        await userEvent.selectOptions(screen.getByTestId('branches-current'), 'dev')

        await waitFor(() => expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'dev', {}))
        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledTimes(2))
    })

    it('confirms discarding changes before forcing a branch switch', async () => {
        vi.mocked(switchProjectBranch)
            .mockRejectedValueOnce(new Error('modified'))
            .mockResolvedValueOnce(undefined)
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        await userEvent.selectOptions(screen.getByTestId('branches-current'), 'dev')

        await waitFor(() => expect(screen.getByText('browser.switch_branch_discard_warning')).toBeTruthy())
        expect(screen.getByText('browser.switch_branch_discard_confirm_unsafe')).toBeTruthy()
        expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'dev', {})

        await userEvent.click(screen.getByTestId('branch-discard-switch-confirm'))

        await waitFor(() => expect(switchProjectBranch).toHaveBeenLastCalledWith('p1', 'dev', { discardChanges: true }))
    })

    it('switches to the new branch after creating it only when the toggle is on', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        await userEvent.click(screen.getByTestId('branches-create'))
        await userEvent.type(screen.getByTestId('branches-new-name'), 'hotfix')
        await userEvent.click(screen.getByTestId('branches-switch-after'))
        await userEvent.click(screen.getByTestId('branches-create-submit'))

        await waitFor(() => expect(createProjectBranch).toHaveBeenCalledWith('p1', 'hotfix'))
        await waitFor(() => expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'hotfix', {}))
    })

    it('does not switch after creating when the toggle is off', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        await userEvent.click(screen.getByTestId('branches-create'))
        await userEvent.type(screen.getByTestId('branches-new-name'), 'hotfix')
        await userEvent.click(screen.getByTestId('branches-create-submit'))

        await waitFor(() => expect(createProjectBranch).toHaveBeenCalledWith('p1', 'hotfix'))
        expect(switchProjectBranch).not.toHaveBeenCalled()
    })

    it('locks the base and current branch checkboxes in configure mode', async () => {
        await renderPanel()
        await waitFor(() => expect(screen.getByTestId('branch-merge-feature')).toBeTruthy())

        await userEvent.click(screen.getByTestId('branches-config-toggle'))

        expect((screen.getByTestId('branch-select-main') as HTMLInputElement).disabled).toBe(true)
        expect((screen.getByTestId('branch-select-feature') as HTMLInputElement).disabled).toBe(false)
    })

    it('hides write actions for read-only projects', async () => {
        await renderPanel({ canWrite: false })
        await waitFor(() => expect(screen.getByTestId('branches-filter')).toBeTruthy())

        expect(screen.queryByTestId('branches-create')).toBeNull()
        expect(screen.queryByTestId('branches-config-toggle')).toBeNull()
        expect(screen.queryByTestId('branch-merge-dev')).toBeNull()
        expect(screen.queryByTestId('branch-delete-dev')).toBeNull()
        expect(screen.getByTestId('branches-current')).toBeTruthy()
    })

    it('allows branch deletion only for the current branch and passes the base branch', async () => {
        const events: CustomEvent[] = []
        const listener = (e: Event) => events.push(e as CustomEvent)
        window.addEventListener('openDeleteBranchModal', listener)
        await renderPanel({ currentBranch: 'dev', selectedBranches: ['main', 'dev']})
        await waitFor(() => expect(screen.getByTestId('branch-delete-dev')).toBeTruthy())

        expect((screen.getByTestId('branch-delete-feature') as HTMLButtonElement).disabled).toBe(true)
        expect((screen.getByTestId('branch-delete-dev') as HTMLButtonElement).disabled).toBe(false)

        await userEvent.click(screen.getByTestId('branch-delete-dev'))

        expect(events).toHaveLength(1)
        expect(events[0]!.detail).toMatchObject({
            repositoryId: 'repo1',
            projectName: 'Proj',
            branch: 'dev',
            mainBranch: 'main',
        })
        window.removeEventListener('openDeleteBranchModal', listener)
    })

    it('ignores branch loads from a previous project', async () => {
        const p1Load = deferred<ProjectBranch[]>()
        const p2Load = deferred<ProjectBranch[]>()
        vi.mocked(getProjectBranches).mockImplementation(projectId => {
            if (projectId === 'p1') {
                return p1Load.promise
            }
            return p2Load.promise
        })

        const { rerender } = render(
            <BranchesPanel
                canWrite
                currentBranch="main"
                onChanged={vi.fn()}
                projectId="p1"
                projectName="Proj"
                repositoryId="repo1"
                selectedBranches={['main']}
            />
        )
        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))

        rerender(
            <BranchesPanel
                canWrite
                currentBranch="main"
                onChanged={vi.fn()}
                projectId="p2"
                projectName="Proj"
                repositoryId="repo1"
                selectedBranches={['main']}
            />
        )
        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p2'))

        await act(async () => {
            p2Load.resolve([branch('new')])
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        await waitFor(() => expect(screen.getByTestId('branch-merge-new')).toBeTruthy())

        await act(async () => {
            p1Load.resolve([branch('old')])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByTestId('branch-merge-old')).toBeNull()
        expect(screen.getByTestId('branch-merge-new')).toBeTruthy()
    })
})

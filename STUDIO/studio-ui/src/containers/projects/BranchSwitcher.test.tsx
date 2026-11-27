import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notification } from 'antd'
import { BranchSwitcher } from './BranchSwitcher'
import { recentBranches } from '../../utils/recentChoices'
import { getProjectBranches, switchProjectBranch, type ProjectBranch } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => error instanceof Error && error.message === 'modified'),
    switchProjectBranch: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en', resolvedLanguage: 'en' } }) }
})

vi.mock('../../components/SearchInput', () => ({
    SearchInput: (props: Record<string, unknown>) => (
        <input
            data-testid={props['data-testid'] as string}
            onChange={props['onChange'] as never}
            placeholder={props['placeholder'] as string}
            value={props['value'] as string}
        />
    ),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    BranchesOutlined: () => null,
    DownOutlined: () => null,
    LoadingOutlined: (props: Record<string, unknown>) => <i data-testid={props['data-testid'] as string} />,
    SafetyOutlined: (props: Record<string, unknown>) => <i data-testid={props['data-testid'] as string} />,
    HomeOutlined: (props: Record<string, unknown>) => (
        <i aria-label={props['aria-label'] as string} data-testid={props['data-testid'] as string} />
    ),
}))

vi.mock('antd', async () => {
    const domProps = (props: unknown): Record<string, unknown> => {
        if (!props || typeof props !== 'object') {
            return {}
        }
        const { danger, ...dom } = props as Record<string, unknown>
        void danger
        return dom
    }
    const Alert = ({ title }: Record<string, unknown>) => <div>{title as never}</div>
    const Tooltip = ({ children }: { children?: unknown }) => <>{children as never}</>
    const Tag = ({ children, ...rest }: Record<string, unknown>) => (
        <span data-testid={rest['data-testid'] as string}>{children as never}</span>
    )
    const Modal = ({ children, okButtonProps, okText, onOk, open }: Record<string, unknown>) => open ? (
        <div>
            {children as never}
            <button {...domProps(okButtonProps)} onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    interface MenuItem { key: string, label: unknown }
    interface Menu { items: MenuItem[], onClick: (info: { key: string }) => void }
    const Dropdown = ({ children, menu, popupRender, onOpenChange }: Record<string, unknown>) => {
        const list = (
            <ul>
                {(menu as Menu).items.map(item => (
                    <li key={item.key}>
                        <button
                            data-testid={`option-${item.key}`}
                            onClick={() => (menu as Menu).onClick({ key: item.key })}
                        >
                            {item.label as never}
                        </button>
                    </li>
                ))}
            </ul>
        )
        return (
            <div>
                <div onClick={() => (onOpenChange as (v: boolean) => void)?.(true)}>{children as never}</div>
                {popupRender ? (popupRender as (node: unknown) => never)(list) : list}
            </div>
        )
    }
    const notification = { error: vi.fn() }
    const Spin = () => <div data-testid="spin" />
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({ Alert, Dropdown, Modal, Spin, Tag, Tooltip, notification })
})

const props = {
    projectId: 'p1',
    repositoryId: 'design',
    currentBranch: 'main',
    onSwitched: vi.fn(),
}

describe('BranchSwitcher', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        localStorage.clear()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: true, base: true },
            { name: 'dev' },
        ])
        vi.mocked(switchProjectBranch).mockResolvedValue()
    })

    it('marks the current branch from the project without listing the branches', () => {
        render(<BranchSwitcher {...props} currentBranchDefault currentBranchProtected />)

        expect(screen.getByTestId('branch-switcher-default')).toBeInTheDocument()
        expect(screen.getByTestId('branch-switcher-protected')).toBeInTheDocument()
        expect(getProjectBranches).not.toHaveBeenCalled()
    })

    it('shows only the current branch until the dropdown is opened', () => {
        render(<BranchSwitcher {...props} />)
        expect(screen.getByTestId('branch-switcher')).toHaveTextContent('main')
        expect(screen.queryByTestId('option-main')).not.toBeInTheDocument()
        expect(getProjectBranches).not.toHaveBeenCalled()
    })

    it('loads and offers the project branches when opened', async () => {
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))
        expect(screen.getByTestId('option-dev')).toBeInTheDocument()
        expect(screen.queryByTestId('option-other')).not.toBeInTheDocument()
    })

    it('filters the offered branches by name as the user types', async () => {
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', base: true },
            { name: 'feature/rates' },
            { name: 'dev' },
        ])
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')

        await userEvent.type(screen.getByTestId('branch-switcher-search'), 'feat')

        expect(screen.getByTestId('option-feature/rates')).toBeInTheDocument()
        expect(screen.queryByTestId('option-dev')).not.toBeInTheDocument()
        expect(screen.queryByTestId('option-main')).not.toBeInTheDocument()
    })

    it('shows a no-match message when the search matches no branch', async () => {
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')

        await userEvent.type(screen.getByTestId('branch-switcher-search'), 'zzz')

        expect(screen.getByText('browser.branch.no_match')).toBeInTheDocument()
        expect(screen.queryByTestId('option-dev')).not.toBeInTheDocument()
    })

    it('resets the search each time the dropdown is reopened', async () => {
        render(<BranchSwitcher {...props} />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        await userEvent.type(screen.getByTestId('branch-switcher-search'), 'zzz')
        expect(screen.queryByTestId('option-dev')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        expect(screen.getByTestId('branch-switcher-search')).toHaveValue('')
        expect(screen.getByTestId('option-dev')).toBeInTheDocument()
    })

    it('reloads membership when the component is reused for another project', async () => {
        const { rerender } = render(<BranchSwitcher {...props} />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        vi.mocked(getProjectBranches).mockResolvedValueOnce([{ name: 'release' }])

        rerender(<BranchSwitcher {...props} currentBranch="release" projectId="p2" />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await screen.findByTestId('option-release')
        expect(screen.queryByTestId('option-dev')).not.toBeInTheDocument()
        expect(getProjectBranches).toHaveBeenLastCalledWith('p2')
    })

    it('reports a branch-list failure and retries when reopened', async () => {
        vi.mocked(getProjectBranches)
            .mockRejectedValueOnce(new Error('offline'))
            .mockResolvedValueOnce([{ name: 'main' }, { name: 'dev' }])
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({
            title: 'browser.branch.load_failed',
            description: 'offline',
        }))

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await screen.findByTestId('option-dev')
        expect(getProjectBranches).toHaveBeenCalledTimes(2)
    })

    it('marks every branch it lists once the branch list is loaded', async () => {
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        // The default branch wears the house, named for a screen reader as the Default branch.
        await waitFor(() => expect(within(screen.getByTestId('option-main')).getByLabelText('browser.branch.default_tag'))
            .toBeInTheDocument())
    })

    it('leads with the branches of the repository used most recently, then lists the others by name', async () => {
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', base: true },
            { name: 'zeta' },
            { name: 'Beta' },
            { name: 'alpha' },
            { name: 'release-2' },
        ])
        // A branch used by another project of the repository counts as well.
        recentBranches.remember('zeta', 'design')
        recentBranches.remember('main', 'design')
        // The branches of another repository count for that repository alone.
        recentBranches.remember('alpha', 'other')
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-alpha')

        const options = screen.getAllByTestId(/^option-/).map(option => option.dataset['testid'])
        expect(options).toEqual([
            'option-main',
            'option-zeta',
            'option-alpha-divider',
            'option-alpha',
            'option-Beta',
            'option-release-2',
        ])
    })

    it('shows at once a branch another switcher of the repository was just used on, in a list read before', async () => {
        render(<BranchSwitcher {...props} />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        const order = () => screen.getAllByTestId(/^option-/).map(option => option.dataset['testid'])
        expect(order()).toEqual(['option-dev', 'option-main'])

        // The switcher of another project of the repository switches it to dev.
        act(() => recentBranches.remember('dev', 'design'))

        expect(order()).toEqual(['option-dev', 'option-main-divider', 'option-main'])
        expect(getProjectBranches).toHaveBeenCalledTimes(1)
    })

    it('keeps the recent branches the project is not on, and remembers the one switched to', async () => {
        // The list of the project holds the branches holding it; another project of the repository may be elsewhere.
        recentBranches.remember('elsewhere', 'design')
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        expect(screen.queryByTestId('option-elsewhere')).toBeNull()
        await userEvent.click(screen.getByTestId('option-dev'))

        await waitFor(() => expect(recentBranches.read('design')).toEqual(['dev', 'elsewhere']))
    })

    it('switches the project to the chosen branch', async () => {
        const onSwitched = vi.fn()
        render(<BranchSwitcher {...props} onSwitched={onSwitched} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        await userEvent.click(screen.getByTestId('option-dev'))

        await waitFor(() => expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'dev', {}))
        await waitFor(() => expect(onSwitched).toHaveBeenCalled())
    })

    it('confirms discarding changes before forcing a branch switch', async () => {
        const onSwitched = vi.fn()
        vi.mocked(switchProjectBranch)
            .mockRejectedValueOnce(new Error('modified'))
            .mockResolvedValueOnce(undefined)
        render(<BranchSwitcher {...props} onSwitched={onSwitched} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')
        await userEvent.click(screen.getByTestId('option-dev'))

        await screen.findByText('browser.switch_branch_discard_warning')
        await userEvent.click(screen.getByTestId('branch-switcher-discard-switch-confirm'))

        await waitFor(() => expect(switchProjectBranch).toHaveBeenLastCalledWith('p1', 'dev', { discardChanges: true }))
        await waitFor(() => expect(onSwitched).toHaveBeenCalled())
    })

    it('stays busy from the click until the reload behind the switch has finished', async () => {
        const reload = Promise.withResolvers<void>()
        const onBusyChange = vi.fn()
        render(<BranchSwitcher {...props} onBusyChange={onBusyChange} onSwitched={() => reload.promise} />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')

        await userEvent.click(screen.getByTestId('option-dev'))

        // The request is over, but the screen still shows the branch it was switched away from.
        await waitFor(() => expect(screen.getByTestId('branch-switcher-switching')).toBeInTheDocument())
        expect(screen.getByTestId('branch-switcher-trigger')).toBeDisabled()
        expect(onBusyChange).toHaveBeenLastCalledWith(true)

        reload.resolve()

        await waitFor(() => expect(onBusyChange).toHaveBeenLastCalledWith(false))
        expect(screen.queryByTestId('branch-switcher-switching')).not.toBeInTheDocument()
    })

    it('releases the project when the switch fails', async () => {
        const onBusyChange = vi.fn()
        vi.mocked(switchProjectBranch).mockRejectedValueOnce(new Error('offline'))
        render(<BranchSwitcher {...props} onBusyChange={onBusyChange} />)
        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))
        await screen.findByTestId('option-dev')

        await userEvent.click(screen.getByTestId('option-dev'))

        await waitFor(() => expect(onBusyChange).toHaveBeenLastCalledWith(false))
        expect(screen.queryByTestId('branch-switcher-switching')).not.toBeInTheDocument()
    })

    it('says the branch list is being read instead of showing an empty popup', async () => {
        const branches = Promise.withResolvers<ProjectBranch[]>()
        vi.mocked(getProjectBranches).mockReturnValueOnce(branches.promise)
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        expect(screen.getByTestId('branch-switcher-list-loading')).toBeInTheDocument()

        branches.resolve([{ name: 'main' }, { name: 'dev' }])

        await screen.findByTestId('option-dev')
        expect(screen.queryByTestId('branch-switcher-list-loading')).not.toBeInTheDocument()
    })
})

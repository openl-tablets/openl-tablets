import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notification } from 'antd'
import { BranchSwitcher } from './BranchSwitcher'
import { getProjectBranches, switchProjectBranch } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => error instanceof Error && error.message === 'modified'),
    switchProjectBranch: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    BranchesOutlined: () => null,
    DownOutlined: () => null,
    SafetyOutlined: (props: Record<string, unknown>) => <i data-testid={props['data-testid'] as string} />,
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
    const Dropdown = ({ children, menu, onOpenChange }: Record<string, unknown>) => (
        <div>
            <div onClick={() => (onOpenChange as (v: boolean) => void)?.(true)}>{children as never}</div>
            <ul>
                {(menu as unknown as Menu).items.map(item => (
                    <li key={item.key}>
                        <button
                            data-testid={`option-${item.key}`}
                            onClick={() => (menu as unknown as Menu).onClick({ key: item.key })}
                        >
                            {item.label as never}
                        </button>
                    </li>
                ))}
            </ul>
        </div>
    )
    const notification = { error: vi.fn() }
    return { Alert, Dropdown, Modal, Tag, Tooltip, notification }
})

const props = {
    projectId: 'p1',
    currentBranch: 'main',
    onSwitched: vi.fn(),
}

describe('BranchSwitcher', () => {
    beforeEach(() => {
        vi.clearAllMocks()
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

    it('reports a branch-list failure', async () => {
        vi.mocked(getProjectBranches).mockRejectedValueOnce(new Error('offline'))
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({
            title: 'browser.branch.load_failed',
            description: 'offline',
        }))
    })

    it('marks every branch it lists once the branch list is loaded', async () => {
        render(<BranchSwitcher {...props} />)

        await userEvent.click(screen.getByTestId('branch-switcher-trigger'))

        await waitFor(() => expect(within(screen.getByTestId('option-main')).getByText('browser.branch.default_tag'))
            .toBeInTheDocument())
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
})

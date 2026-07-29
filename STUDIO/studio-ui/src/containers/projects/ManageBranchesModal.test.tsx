import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ManageBranchesModal } from './ManageBranchesModal'
import { getProjectBranches } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
}))

vi.mock('react-i18next', () => {
    // One stable `t`: a fresh identity per render would loop the effects that depend on it.
    const t = (key: string, opts?: { count?: number }) => (opts?.count === undefined ? key : `${key}:${opts.count}`)
    const useTranslation = () => ({ t })
    return { useTranslation }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    BranchesOutlined: () => null,
    SafetyOutlined: (props: Record<string, unknown>) => <i data-testid={props['data-testid'] as string} />,
    SearchOutlined: () => null,
}))

vi.mock('../../components/SearchInput', () => ({
    SearchInput: (props: Record<string, unknown>) => (
        <input
            data-testid={props['data-testid'] as string}
            onChange={props['onChange'] as never}
            value={props['value'] as string}
        />
    ),
}))

vi.mock('antd', () => {
    const Modal = ({ children, footer, open, title }: Record<string, unknown>) => open ? (
        <div>
            <h2>{title as never}</h2>
            {children as never}
            <div>{footer as never}</div>
        </div>
    ) : null
    const Button = ({ children, disabled, onClick, ...rest }: Record<string, unknown>) => (
        <button
            data-testid={rest['data-testid'] as string}
            disabled={disabled as boolean}
            onClick={onClick as never}
        >
            {children as never}
        </button>
    )
    const Skeleton = () => <div data-testid="loading" />
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const notification = { error: vi.fn(), success: vi.fn() }
    return { Button, Modal, Skeleton, Tag, Tooltip, notification }
})

const props = {
    open: true,
    projectId: 'p1',
    onClose: vi.fn(),
}

const renderModal = async (overrides: Partial<typeof props> = {}) => {
    render(<ManageBranchesModal {...props} {...overrides} />)
    await screen.findByTestId('manage-branches-item-main')
}

describe('ManageBranchesModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: true, base: true, containsProject: true },
            { name: 'dev', protected: false, base: false, containsProject: true },
            { name: 'feature/rates', protected: false, base: false, containsProject: false },
        ])
    })

    it('lists only branches that contain the project', async () => {
        await renderModal()

        expect(screen.getByTestId('manage-branches-item-main')).toBeInTheDocument()
        expect(screen.getByTestId('manage-branches-item-dev')).toBeInTheDocument()
        expect(screen.queryByTestId('manage-branches-item-feature/rates')).toBeNull()
        expect(screen.queryByText('browser.branch.contains_project')).toBeNull()
        expect(screen.queryByText('browser.branch.does_not_contain_project')).toBeNull()
    })

    it('filters the list by branch name', async () => {
        await renderModal()

        await userEvent.type(screen.getByTestId('manage-branches-filter'), 'dev')

        await waitFor(() => expect(screen.queryByTestId('manage-branches-item-main')).toBeNull())
        expect(screen.getByTestId('manage-branches-item-dev')).toBeInTheDocument()
    })

    it('closes without submitting mutable membership', async () => {
        await renderModal()

        expect(screen.getByRole('button', { name: 'browser.close' })).toBeInTheDocument()
        await userEvent.click(screen.getByTestId('manage-branches-close'))

        expect(props.onClose).toHaveBeenCalled()
    })
})

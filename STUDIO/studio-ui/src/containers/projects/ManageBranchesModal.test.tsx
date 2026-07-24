import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ManageBranchesModal } from './ManageBranchesModal'
import { getProjectBranches, setSelectedBranches } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
    setSelectedBranches: vi.fn(),
}))

vi.mock('../../hooks', () => {
    const guard = { runWithCommitInfo: (action: () => Promise<void>) => action(), commitInfoModal: null }
    return { useCommitInfoGuard: () => guard }
})

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
    const Checkbox = ({ checked, disabled, onChange, ...rest }: Record<string, unknown>) => (
        <input
            checked={checked as boolean}
            data-testid={rest['data-testid'] as string}
            disabled={disabled as boolean}
            onChange={onChange as never}
            type="checkbox"
        />
    )
    const Popconfirm = ({ children, onConfirm, title }: Record<string, unknown>) => (
        <span>
            {children as never}
            <button data-testid="confirm-removal" onClick={onConfirm as never}>{title as never}</button>
        </span>
    )
    const Skeleton = () => <div data-testid="loading" />
    const Tag = ({ children, ...rest }: Record<string, unknown>) => (
        <span data-testid={rest['data-testid'] as string}>{children as never}</span>
    )
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const notification = { error: vi.fn(), success: vi.fn() }
    return { Button, Checkbox, Modal, Popconfirm, Skeleton, Tag, Tooltip, notification }
})

const props = {
    open: true,
    projectId: 'p1',
    currentBranch: 'dev',
    selectedBranches: ['main', 'dev'],
    onClose: vi.fn(),
    onSaved: vi.fn(),
}

const renderModal = async (overrides: Partial<typeof props> = {}) => {
    render(<ManageBranchesModal {...props} {...overrides} />)
    await screen.findByTestId('manage-branches-select-main')
}

describe('ManageBranchesModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: true, base: true },
            { name: 'dev', protected: false, base: false },
            { name: 'feature/rates', protected: false, base: false },
        ])
        vi.mocked(setSelectedBranches).mockResolvedValue()
    })

    it('lists every repository branch, keeping the main branch and the one in use', async () => {
        await renderModal()

        expect(screen.getByTestId('manage-branches-select-feature/rates')).toBeInTheDocument()
        expect(screen.getByTestId('manage-branches-select-main')).toBeDisabled()
        expect(screen.getByTestId('manage-branches-select-dev')).toBeDisabled()
        expect(screen.getByTestId('manage-branches-select-feature/rates')).not.toBeDisabled()
    })

    it('filters the list by branch name', async () => {
        await renderModal()

        await userEvent.type(screen.getByTestId('manage-branches-filter'), 'feat')

        await waitFor(() => expect(screen.queryByTestId('manage-branches-select-main')).toBeNull())
        expect(screen.getByTestId('manage-branches-select-feature/rates')).toBeInTheDocument()
    })

    it('adds a branch to the selection', async () => {
        await renderModal()

        await userEvent.click(screen.getByTestId('manage-branches-select-feature/rates'))
        await userEvent.click(screen.getByTestId('manage-branches-save'))

        await waitFor(() => expect(setSelectedBranches).toHaveBeenCalledWith('p1', ['main', 'dev', 'feature/rates']))
        expect(props.onSaved).toHaveBeenCalled()
    })

    it('confirms before removing the project from a branch', async () => {
        await renderModal({ selectedBranches: ['main', 'dev', 'feature/rates']})

        await userEvent.click(screen.getByTestId('manage-branches-select-feature/rates'))

        expect(setSelectedBranches).not.toHaveBeenCalled()
        await userEvent.click(screen.getByTestId('confirm-removal'))

        await waitFor(() => expect(setSelectedBranches).toHaveBeenCalledWith('p1', ['main', 'dev']))
    })

    it('keeps the save disabled until the selection changes', async () => {
        await renderModal()

        expect(screen.getByTestId('manage-branches-save')).toBeDisabled()
    })
})

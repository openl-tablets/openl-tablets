import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { fireEvent } from '@testing-library/dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BranchSelector } from './BranchSelector'
import { getProjectBranches, switchProjectBranch } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({
    getProjectBranches: vi.fn(),
    isProjectModifiedConflict: vi.fn((error: unknown) => error instanceof Error && error.message === 'modified'),
    switchProjectBranch: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('@ant-design/icons', () => ({ BranchesOutlined: () => null }))

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
    const Modal = ({
        children,
        okButtonProps,
        okText,
        onOk,
        open,
    }: Record<string, unknown>) => open ? (
        <div>
            {children as never}
            <button {...domProps(okButtonProps)} onClick={onOk as never}>{okText as never}</button>
        </div>
    ) : null
    interface Opt { value: string, label: string }
    const Select = ({ options, onChange, onOpenChange, value }: Record<string, unknown>) => (
        <div>
            <button data-testid="branch-open" onClick={() => (onOpenChange as (v: boolean) => void)?.(true)}>open</button>
            <select
                data-testid="branch-select"
                onChange={event => (onChange as (v: string) => void)?.(event.target.value)}
                value={value as string}
            >
                {(options as Opt[])?.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
        </div>
    )
    const notification = { error: vi.fn() }
    return { Alert, Modal, Select, Tooltip, notification }
})

describe('BranchSelector', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: false, base: true, bypassEligible: false },
            { name: 'dev', protected: false, base: false, bypassEligible: false },
        ])
        vi.mocked(switchProjectBranch).mockResolvedValue()
    })

    it('lazily loads branches only when the dropdown opens', async () => {
        render(<BranchSelector currentBranch="main" onSwitched={vi.fn()} projectId="p1" />)
        expect(getProjectBranches).not.toHaveBeenCalled()

        await userEvent.click(screen.getByTestId('branch-open'))

        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))
    })

    it('switches the project to the chosen branch', async () => {
        const onSwitched = vi.fn()
        render(<BranchSelector currentBranch="main" onSwitched={onSwitched} projectId="p1" />)

        await userEvent.click(screen.getByTestId('branch-open'))
        await waitFor(() => expect(screen.getByText('dev')).toBeTruthy())

        fireEvent.change(screen.getByTestId('branch-select'), { target: { value: 'dev' } })

        await waitFor(() => expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'dev', {}))
        await waitFor(() => expect(onSwitched).toHaveBeenCalled())
    })

    it('confirms discarding changes before forcing a branch switch', async () => {
        const onSwitched = vi.fn()
        vi.mocked(switchProjectBranch)
            .mockRejectedValueOnce(new Error('modified'))
            .mockResolvedValueOnce(undefined)
        render(<BranchSelector currentBranch="main" onSwitched={onSwitched} projectId="p1" />)

        await userEvent.click(screen.getByTestId('branch-open'))
        await waitFor(() => expect(screen.getByText('dev')).toBeTruthy())

        fireEvent.change(screen.getByTestId('branch-select'), { target: { value: 'dev' } })

        await waitFor(() => expect(screen.getByText('browser.switch_branch_discard_warning')).toBeTruthy())
        expect(screen.getByText('browser.switch_branch_discard_confirm_unsafe')).toBeTruthy()
        expect(switchProjectBranch).toHaveBeenCalledWith('p1', 'dev', {})

        await userEvent.click(screen.getByTestId('branch-selector-discard-switch-confirm'))

        await waitFor(() => expect(switchProjectBranch).toHaveBeenLastCalledWith('p1', 'dev', { discardChanges: true }))
        await waitFor(() => expect(onSwitched).toHaveBeenCalled())
    })
})

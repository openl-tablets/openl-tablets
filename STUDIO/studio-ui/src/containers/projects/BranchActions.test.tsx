import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { BranchActions } from './BranchActions'
import { createProjectBranch, getProjectBranches } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({ getProjectBranches: vi.fn(), createProjectBranch: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('@ant-design/icons', () => ({ BranchesOutlined: () => null, MergeOutlined: () => null, PlusOutlined: () => null }))

vi.mock('antd', () => {
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { icon, loading, size, ...dom } = rest
        void icon; void loading; void size
        return <button onClick={onClick as never} {...dom}>{children as never}</button>
    }
    const Space = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Tooltip = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Input = ({ onChange, onPressEnter, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return <input onChange={onChange as never} {...rest} />
    }
    const Modal = ({ open, children, onOk, okButtonProps, title }: Record<string, unknown>) => {
        const okProps = (okButtonProps ?? {}) as Record<string, unknown>
        return open ? (
            <div>
                {title as never}
                {children as never}
                <button data-testid={okProps['data-testid'] as string} disabled={okProps['disabled'] as boolean} onClick={onOk as never}>ok</button>
            </div>
        ) : null
    }
    const notification = { error: vi.fn() }
    return { Button, Space, Tooltip, Input, Modal, notification }
})

const props = {
    projectId: 'p1',
    projectName: 'Alpha',
    repositoryId: 'design',
    currentBranch: 'feature',
    onChanged: vi.fn(),
}

describe('BranchActions', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectBranches).mockResolvedValue([
            { name: 'main', protected: true, base: true, bypassEligible: false },
            { name: 'feature', protected: false, base: false, bypassEligible: false },
        ])
    })

    it('loads branches and opens the merge modal with git type', async () => {
        const events: CustomEvent[] = []
        const listener = (e: Event) => events.push(e as CustomEvent)
        window.addEventListener('openMergeModal', listener)
        render(<BranchActions {...props} />)

        await userEvent.click(screen.getByTestId('branch-merge'))

        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))
        await waitFor(() => expect(events.length).toBe(1))
        expect(events[0]!.detail).toMatchObject({
            projectId: 'p1',
            repositoryId: 'design',
            repositoryType: 'repo-git',
            currentBranch: 'feature',
        })
        expect(events[0]!.detail.branches).toHaveLength(2)
        window.removeEventListener('openMergeModal', listener)
    })

    it('opens the delete-branch modal for the current branch', async () => {
        const events: CustomEvent[] = []
        const listener = (e: Event) => events.push(e as CustomEvent)
        window.addEventListener('openDeleteBranchModal', listener)
        render(<BranchActions {...props} />)

        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))
        await waitFor(() => expect((screen.getByTestId('branch-delete') as HTMLButtonElement).disabled).toBe(false))
        await userEvent.click(screen.getByTestId('branch-delete'))

        expect(events.length).toBe(1)
        expect(events[0]!.detail).toMatchObject({
            repositoryId: 'design',
            projectName: 'Alpha',
            branch: 'feature',
            mainBranch: 'main',
        })
        window.removeEventListener('openDeleteBranchModal', listener)
    })

    it('disables delete on the base branch', async () => {
        render(<BranchActions {...props} currentBranch="main" />)

        await waitFor(() => expect(getProjectBranches).toHaveBeenCalledWith('p1'))
        await waitFor(() => expect((screen.getByTestId('branch-delete') as HTMLButtonElement).disabled).toBe(true))
    })

    it('creates a new branch', async () => {
        const onChanged = vi.fn()
        vi.mocked(createProjectBranch).mockResolvedValue()
        render(<BranchActions {...props} onChanged={onChanged} />)

        await userEvent.click(screen.getByTestId('branch-new'))
        await userEvent.type(screen.getByTestId('branch-name'), 'release-1')
        await userEvent.click(screen.getByTestId('branch-create-submit'))

        await waitFor(() => expect(createProjectBranch).toHaveBeenCalledWith('p1', 'release-1'))
        await waitFor(() => expect(onChanged).toHaveBeenCalled())
    })
})

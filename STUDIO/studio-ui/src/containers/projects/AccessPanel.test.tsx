import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AccessPanel } from './AccessPanel'
import { getProjectAcl, removeProjectAcl, setProjectAcl, type AccessControlEntry } from '../../services/acl'

vi.mock('../../services/acl', () => ({
    getProjectAcl: vi.fn(),
    removeProjectAcl: vi.fn(),
    setProjectAcl: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    DeleteOutlined: () => null,
    PlusOutlined: () => null,
    TeamOutlined: () => null,
    UserOutlined: () => null,
}))

vi.mock('./AddAccessModal', () => ({ AddAccessModal: () => null }))

vi.mock('antd', () => {
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { danger, disabled, icon, size, type, ...dom } = rest
        void danger; void disabled; void icon; void size; void type
        return <button onClick={onClick as never} {...dom}>{children as never}</button>
    }
    const Select = ({ value, onChange, options, ...rest }: Record<string, unknown>) => (
        <select onChange={event => (onChange as (v: string) => void)(event.target.value)} value={value as string} {...rest}>
            {(options as { value: string, label: string }[])?.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    const Tag = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Skeleton = () => <div>loading</div>
    const Empty = ({ description, ...rest }: Record<string, unknown>) => {
        const { image, ...dom } = rest
        void image
        return <div {...dom}>{description as never}</div>
    }
    Empty.PRESENTED_IMAGE_SIMPLE = 'simple'
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const Popconfirm = ({ children, onConfirm }: Record<string, unknown>) => (
        <div>
            {children as never}
            <button data-testid="popconfirm-ok" onClick={onConfirm as never} type="button">confirm</button>
        </div>
    )
    const notification = { error: vi.fn() }
    return { Alert, Button, Empty, Popconfirm, Select, Skeleton, Tag, notification }
})

function entry(sid: string, source?: AccessControlEntry['source']): AccessControlEntry {
    return { role: 'VIEWER' as never, source, sub: { sid, principal: true } }
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

describe('AccessPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getProjectAcl).mockResolvedValue([])
        vi.mocked(removeProjectAcl).mockResolvedValue()
        vi.mocked(setProjectAcl).mockResolvedValue()
    })

    it('ignores ACL loads from a previous project', async () => {
        const p1Load = deferred<AccessControlEntry[]>()
        const p2Load = deferred<AccessControlEntry[]>()
        vi.mocked(getProjectAcl).mockImplementation(projectId => {
            if (projectId === 'p1') {
                return p1Load.promise
            }
            return p2Load.promise
        })

        const { rerender } = render(<AccessPanel canManage projectId="p1" projectName="Alpha" />)
        await waitFor(() => expect(getProjectAcl).toHaveBeenCalledWith('p1', { inherited: true }))

        rerender(<AccessPanel canManage projectId="p2" projectName="Beta" />)
        await waitFor(() => expect(getProjectAcl).toHaveBeenCalledWith('p2', { inherited: true }))

        await act(async () => {
            p2Load.resolve([entry('new-user')])
            await new Promise(resolve => setTimeout(resolve, 0))
        })
        await waitFor(() => expect(screen.getByText('new-user')).toBeTruthy())

        await act(async () => {
            p1Load.resolve([entry('old-user')])
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByText('old-user')).toBeNull()
        expect(screen.getByText('new-user')).toBeTruthy()
    })

    it('shows repository ACL entries as read-only inherited rows', async () => {
        vi.mocked(getProjectAcl).mockResolvedValue([
            entry('direct-user', 'project'),
            entry('repo-user', 'repository'),
        ])

        await act(async () => {
            render(<AccessPanel canManage projectId="p1" projectName="Alpha" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.getByText('repo-user')).toBeTruthy())
        expect(screen.getByText('browser.access.source_project')).toBeTruthy()
        expect(screen.getByText('browser.access.source_repository')).toBeTruthy()
        expect(screen.getByTestId('access-role-repository-repo-user')).toBeDisabled()
        expect(screen.queryByTestId('access-remove-repository-repo-user')).toBeNull()
        expect(screen.getByTestId('access-role-project-direct-user')).not.toBeDisabled()
        expect(screen.getByTestId('access-remove-project-direct-user')).toBeTruthy()
    })

    it('shows an error state when ACL loading fails', async () => {
        vi.mocked(getProjectAcl).mockRejectedValue(new Error('network'))

        await act(async () => {
            render(<AccessPanel canManage projectId="p1" projectName="Alpha" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.getByText('browser.access.load_failed')).toBeTruthy())
    })

    it('updates and removes direct project ACL entries', async () => {
        vi.mocked(getProjectAcl).mockResolvedValue([entry('direct-user', 'project')])

        await act(async () => {
            render(<AccessPanel canManage projectId="p1" projectName="Alpha" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        await waitFor(() => expect(screen.getByTestId('access-role-project-direct-user')).toBeTruthy())

        await userEvent.selectOptions(screen.getByTestId('access-role-project-direct-user'), 'CONTRIBUTOR')

        await waitFor(() => expect(setProjectAcl).toHaveBeenCalledWith('p1', 'direct-user', 'CONTRIBUTOR', true))

        await act(async () => {
            screen.getByTestId('popconfirm-ok').click()
        })

        await waitFor(() => expect(removeProjectAcl).toHaveBeenCalledWith('p1', 'direct-user', true))
    })

    it('hides add controls when the user cannot manage access', async () => {
        vi.mocked(getProjectAcl).mockResolvedValue([entry('viewer', 'project')])

        await act(async () => {
            render(<AccessPanel canManage={false} projectId="p1" projectName="Alpha" />)
            await new Promise(resolve => setTimeout(resolve, 0))
        })

        expect(screen.queryByTestId('access-add')).toBeNull()
    })
})

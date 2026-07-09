import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AddAccessModal } from './AddAccessModal'
import { Role } from '../../constants'
import { searchProjectAclSubjects, setProjectAcl } from '../../services/acl'
import { SystemContext } from '../../contexts'

vi.mock('../../services/acl', () => ({
    searchProjectAclSubjects: vi.fn(),
    setProjectAcl: vi.fn(),
}))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string, opts?: Record<string, unknown>) => opts?.['name'] ? `${key}:${opts['name']}` : key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    TeamOutlined: () => null,
    UserAddOutlined: () => null,
    UserOutlined: () => null,
}))

vi.mock('antd', () => {
    const Modal = ({ children, okButtonProps, okText, onCancel, onOk, open, title }: Record<string, unknown>) => {
        if (!open) {
            return null
        }
        const { disabled, icon, ...buttonProps } = (okButtonProps ?? {}) as Record<string, unknown>
        void icon
        return (
            <div>
                <h1>{title as never}</h1>
                {children as never}
                <button disabled={disabled as boolean} onClick={onOk as never} {...buttonProps}>{okText as never}</button>
                <button onClick={onCancel as never}>cancel</button>
            </div>
        )
    }
    interface AutoOption { value: string, label: string }
    const AutoComplete = ({ onChange, onInputKeyDown, options, value, ...rest }: Record<string, unknown>) => {
        const { filterOption, notFoundContent, ...dom } = rest
        void filterOption; void notFoundContent
        return (
            <div>
                <input
                    onChange={event => (onChange as (next: string) => void)(event.target.value)}
                    onKeyDown={onInputKeyDown as never}
                    value={(value as string) ?? ''}
                    {...dom}
                />
                <div>
                    {((options as AutoOption[] | undefined) ?? []).map(option => (
                        <button
                            key={option.value}
                            data-testid={`add-access-option-${option.value}`}
                            onClick={() => (onChange as (next: string) => void)(option.value)}
                            type="button"
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            </div>
        )
    }
    interface Option { value: string, label: string }
    const Select = ({ onChange, options, value, ...rest }: Record<string, unknown>) => (
        <select
            onChange={event => (onChange as (next: string) => void)(event.target.value)}
            value={value as string}
            {...rest}
        >
            {(options as Option[]).map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const Spin = () => null
    return { Alert, AutoComplete, Modal, Select, Spin }
})

const renderModal = ({ groupsEnabled = true }: { groupsEnabled?: boolean } = {}) => render(
    <SystemContext.Provider
        value={{
            isExternalAuthSystem: false,
            isGroupsManagementEnabled: groupsEnabled,
            isPersonalAccessTokenEnabled: false,
            isUserManagementEnabled: true,
        }}
    >
        <AddAccessModal open onClose={vi.fn()} onGranted={vi.fn()} projectId="p1" projectName="Alpha" />
    </SystemContext.Provider>
)

describe('AddAccessModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.useRealTimers()
        vi.mocked(searchProjectAclSubjects).mockResolvedValue([])
        vi.mocked(setProjectAcl).mockResolvedValue()
    })

    it('grants access with the supported ACL request shape', async () => {
        const onClose = vi.fn()
        const onGranted = vi.fn()
        render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    isUserManagementEnabled: true,
                }}
            >
                <AddAccessModal open onClose={onClose} onGranted={onGranted} projectId="p1" projectName="Alpha" />
            </SystemContext.Provider>
        )

        expect(screen.queryByText('browser.access.notify')).toBeNull()

        await userEvent.click(screen.getByText('browser.access.type_group'))
        fireEvent.change(screen.getByTestId('add-access-sid'), { target: { value: ' Editors ' } })
        fireEvent.change(screen.getByTestId('add-access-role'), { target: { value: Role.MANAGER } })
        await userEvent.click(screen.getByTestId('add-access-submit'))

        await waitFor(() => expect(setProjectAcl).toHaveBeenCalled())
        expect(vi.mocked(setProjectAcl).mock.calls[0]).toEqual(['p1', 'Editors', Role.MANAGER, false])
        expect(onGranted).toHaveBeenCalled()
        expect(onClose).toHaveBeenCalled()
    })

    it('loads subject suggestions after a debounce', async () => {
        vi.useFakeTimers()
        vi.mocked(searchProjectAclSubjects).mockResolvedValue(['jane'])
        renderModal()

        fireEvent.change(screen.getByTestId('add-access-sid'), { target: { value: 'j' } })
        await act(async () => {
            await vi.advanceTimersByTimeAsync(300)
        })
        expect(searchProjectAclSubjects).not.toHaveBeenCalled()

        fireEvent.change(screen.getByTestId('add-access-sid'), { target: { value: 'ja' } })
        await act(async () => {
            await vi.advanceTimersByTimeAsync(299)
        })
        expect(searchProjectAclSubjects).not.toHaveBeenCalled()
        await act(async () => {
            await vi.advanceTimersByTimeAsync(1)
        })

        expect(searchProjectAclSubjects).toHaveBeenCalledWith('p1', true, 'ja', 10)
        expect(screen.getByTestId('add-access-option-jane')).toBeTruthy()
    })

    it('searches groups when the group subject type is selected', async () => {
        vi.useFakeTimers()
        vi.mocked(searchProjectAclSubjects).mockResolvedValue(['Editors'])
        renderModal()

        fireEvent.click(screen.getByText('browser.access.type_group'))
        fireEvent.change(screen.getByTestId('add-access-sid'), { target: { value: 'ed' } })
        await act(async () => {
            await vi.advanceTimersByTimeAsync(300)
        })

        expect(searchProjectAclSubjects).toHaveBeenCalledWith('p1', false, 'ed', 10)
    })

    it('hides the group subject type when group management is disabled', () => {
        renderModal({ groupsEnabled: false })

        expect(screen.getByText('browser.access.type_user')).toBeTruthy()
        expect(screen.queryByText('browser.access.type_group')).toBeNull()
    })
})

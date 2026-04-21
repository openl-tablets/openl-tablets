import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import * as services from 'services'
import { SecurityUserMode } from 'constants/security'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
    Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
}))

// Track the current mock value for Form.useWatch
let mockUserMode: string | { value: string, readOnly: boolean } | undefined = undefined

jest.mock('antd', () => {
    const actual = jest.requireActual('antd')
    return {
        ...actual,
        Form: {
            ...actual.Form,
            useForm: actual.Form.useForm,
            useWatch: () => mockUserMode,
            Item: actual.Form.Item,
        },
        Modal: {
            ...actual.Modal,
            confirm: jest.fn(),
        },
        notification: {
            success: jest.fn(),
            error: jest.fn(),
        },
    }
})

const { Modal } = jest.requireMock('antd')

jest.mock('components/form', () => ({
    Checkbox: ({ label, name }: any) => (
        <label>
            <input data-testid={`checkbox-${name}`} name={name} type="checkbox" />
            {label}
        </label>
    ),
    RadioGroup: ({ label, name, options }: any) => (
        <div data-testid={`radiogroup-${name}`}>
            <span>{label}</span>
            {options?.map((opt: any) => (
                <label key={opt.value}>
                    <input name={name} type="radio" value={opt.value} />
                    {opt.label}
                </label>
            ))}
        </div>
    ),
}))

jest.mock('containers/security/InitialUsers', () => ({
    InitialUsers: ({ showDefaultGroup }: any) => (
        <div data-testid="initial-users" data-show-default-group={String(showDefaultGroup)} />
    ),
}))

jest.mock('containers/security/ActiveDirectoryMode', () => ({
    ActiveDirectoryMode: () => <div data-testid="ad-mode" />,
}))

jest.mock('containers/security/SAMLMode', () => ({
    SAMLMode: () => <div data-testid="saml-mode" />,
}))

jest.mock('containers/security/OAuth2Mode', () => ({
    OAuth2Mode: () => <div data-testid="oauth2-mode" />,
}))

jest.mock('containers/security/SingleMode', () => ({
    SingleMode: () => <div data-testid="single-mode" />,
}))

jest.mock('components/modal/InfoFieldModal', () => ({
    __esModule: true,
    default: () => <span data-testid="info-modal" />,
}))

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { Security } = require('containers/Security')

const defaultSettings = {
    userMode: 'multi',
    allowProjectCreateDelete: true,
    administrators: ['admin'],
}

// Mock window.location.reload for save tests
const originalLocation = window.location

beforeAll(() => {
    // @ts-ignore
    delete (window as any).location
    window.location = { reload: jest.fn() } as any
})

afterAll(() => {
    // @ts-ignore
    delete (window as any).location
    // @ts-ignore
    window.location = originalLocation
})

describe('Security', () => {
    beforeEach(() => {
        jest.clearAllMocks()
        mockUserMode = undefined
        mockApiCall.mockResolvedValue(defaultSettings)
    })

    it('fetches security settings on mount', async () => {
        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/settings/authentication')
        })
    })

    it('renders user mode radio group with all options', async () => {
        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('radiogroup-userMode')).toBeInTheDocument()
        })
        expect(screen.getByText('security:user_modes.single')).toBeInTheDocument()
        expect(screen.getByText('security:user_modes.multi')).toBeInTheDocument()
        expect(screen.getByText('security:user_modes.ad')).toBeInTheDocument()
        expect(screen.getByText('security:user_modes.saml')).toBeInTheDocument()
        expect(screen.getByText('security:user_modes.oauth2')).toBeInTheDocument()
    })

    it('renders apply button', async () => {
        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByText('common:btn.apply')).toBeInTheDocument()
        })
    })

    it('renders allowProjectCreateDelete checkbox', async () => {
        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('checkbox-allowProjectCreateDelete')).toBeInTheDocument()
        })
    })

    it('renders section titles', async () => {
        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByText('security:select_user_mode')).toBeInTheDocument()
            expect(screen.getByText('security:select_user_mode_info')).toBeInTheDocument()
        })
    })

    it('renders SingleMode component when userMode is single', async () => {
        mockUserMode = SecurityUserMode.SINGLE

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('single-mode')).toBeInTheDocument()
        })
        // Single mode should not show InitialUsers
        expect(screen.queryByTestId('initial-users')).not.toBeInTheDocument()
    })

    it('renders InitialUsers without default group for multi mode', async () => {
        mockUserMode = SecurityUserMode.MULTI

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
        expect(screen.getByTestId('initial-users')).toHaveAttribute('data-show-default-group', 'false')
    })

    it('renders ActiveDirectoryMode and InitialUsers with default group for AD mode', async () => {
        mockUserMode = SecurityUserMode.AD

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('ad-mode')).toBeInTheDocument()
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
        expect(screen.getByTestId('initial-users')).toHaveAttribute('data-show-default-group', 'true')
    })

    it('renders SAMLMode for SAML mode', async () => {
        mockUserMode = SecurityUserMode.SAML

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('saml-mode')).toBeInTheDocument()
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
    })

    it('renders OAuth2Mode for OAuth2 mode', async () => {
        mockUserMode = SecurityUserMode.OAUTH2

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('oauth2-mode')).toBeInTheDocument()
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
    })

    it('renders SingleMode and hides InitialUsers when wrapped userMode is read-only single', async () => {
        mockUserMode = { value: SecurityUserMode.SINGLE, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('single-mode')).toBeInTheDocument()
        })
        expect(screen.queryByTestId('initial-users')).not.toBeInTheDocument()
    })

    it('renders InitialUsers without default group when wrapped userMode is read-only multi', async () => {
        mockUserMode = { value: SecurityUserMode.MULTI, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
        expect(screen.getByTestId('initial-users')).toHaveAttribute('data-show-default-group', 'false')
    })

    it('renders AD mode and InitialUsers with default group when wrapped userMode is read-only ad', async () => {
        const groupsResponse = { Admins: {} }
        mockApiCall
            .mockResolvedValueOnce(defaultSettings)
            .mockResolvedValueOnce(groupsResponse)
        mockUserMode = { value: SecurityUserMode.AD, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(screen.getByTestId('ad-mode')).toBeInTheDocument()
            expect(screen.getByTestId('initial-users')).toBeInTheDocument()
        })
        expect(screen.getByTestId('initial-users')).toHaveAttribute('data-show-default-group', 'true')
    })

    it('fetches user groups for external auth modes', async () => {
        const groupsResponse = { Admins: {}, Viewers: {} }
        mockApiCall
            .mockResolvedValueOnce(defaultSettings) // fetchSecuritySettings
            .mockResolvedValueOnce(groupsResponse)  // fetchUserGroups
        mockUserMode = SecurityUserMode.AD

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups')
        })
    })

    it('does not fetch user groups for read-only multi mode (wrapped userMode)', async () => {
        mockApiCall.mockResolvedValueOnce(defaultSettings)
        mockUserMode = { value: SecurityUserMode.MULTI, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/settings/authentication')
        })
        expect(mockApiCall).not.toHaveBeenCalledWith('/admin/management/groups')
    })

    it('fetches user groups for read-only external auth mode (wrapped userMode)', async () => {
        const groupsResponse = { Admins: {}, Viewers: {} }
        mockApiCall
            .mockResolvedValueOnce(defaultSettings)
            .mockResolvedValueOnce(groupsResponse)
        mockUserMode = { value: SecurityUserMode.AD, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups')
        })
    })

    it('fetches template when userMode changes from initial', async () => {
        const templateResponse = { ...defaultSettings, userMode: 'ad' }
        mockApiCall
            .mockResolvedValueOnce(defaultSettings)  // fetchSecuritySettings
            .mockResolvedValueOnce(templateResponse)  // fetchSecuritySettingsTemplate
            .mockResolvedValueOnce({ Admins: {} })    // fetchUserGroups
        mockUserMode = SecurityUserMode.AD

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/settings/authentication/template', expect.objectContaining({
                method: 'POST',
            }))
        })
    })

    it('shows confirm modal on form submit', async () => {
        mockUserMode = SecurityUserMode.MULTI

        await act(async () => {
            render(<Security />)
        })

        const submitButton = screen.getByText('common:btn.apply')
        await userEvent.click(submitButton)

        await waitFor(() => {
            expect(Modal.confirm).toHaveBeenCalledWith(
                expect.objectContaining({
                    title: 'security:confirm_apply_configuration',
                })
            )
        })
    })

    it('calls save API on confirm and reloads page', async () => {
        mockUserMode = SecurityUserMode.MULTI

        await act(async () => {
            render(<Security />)
        })

        const submitButton = screen.getByText('common:btn.apply')
        await userEvent.click(submitButton)

        await waitFor(() => {
            expect(Modal.confirm).toHaveBeenCalled()
        })
        const onOk = Modal.confirm.mock.calls[0][0].onOk
        await act(async () => {
            await onOk()
        })

        await waitFor(() => {
            // saveSecuritySettings calls apiCall with the authentication URL and a write method
            expect(mockApiCall).toHaveBeenCalledWith(
                '/admin/settings/authentication',
                expect.objectContaining({
                    headers: { 'Content-Type': 'application/merge-patch+json' },
                }),
                true
            )
        })
    })

    it('shows error notification on save failure', async () => {
        const { notification } = jest.requireMock('antd')
        mockUserMode = SecurityUserMode.MULTI

        let saveCallCount = 0
        mockApiCall.mockImplementation((url: string, options?: any) => {
            // The save call is the one with Content-Type: application/merge-patch+json
            if (options?.headers?.['Content-Type'] === 'application/merge-patch+json') {
                return Promise.reject(new Error('Save failed'))
            }
            saveCallCount++
            return Promise.resolve(defaultSettings)
        })

        await act(async () => {
            render(<Security />)
        })

        const submitButton = screen.getByText('common:btn.apply')
        await userEvent.click(submitButton)

        await waitFor(() => {
            expect(Modal.confirm).toHaveBeenCalled()
        })
        const onOk = Modal.confirm.mock.calls[0][0].onOk
        await act(async () => {
            await onOk()
        })

        await waitFor(() => {
            expect(notification.error).toHaveBeenCalled()
        })
    })
})

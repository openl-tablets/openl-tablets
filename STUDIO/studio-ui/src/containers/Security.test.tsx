import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Modal, notification } from 'antd'
import * as services from 'services'
import { SecurityUserMode } from 'constants/security'
import type { MockedFunction } from 'vitest'

vi.mock('services', async () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', async () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
        Trans: ({ i18nKey }: { i18nKey: string }) => <span>{i18nKey}</span>,
    }
})

// Track the current mock value for Form.useWatch
let mockUserMode: string | { value: string, readOnly: boolean } | undefined = undefined

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
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
            confirm: vi.fn(),
        },
        notification: {
            success: vi.fn(),
            error: vi.fn(),
        },
    }
})

vi.mock('components/form', async () => ({
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

vi.mock('containers/security/InitialUsers', async () => ({
    InitialUsers: ({ showDefaultGroup }: any) => (
        <div data-show-default-group={String(showDefaultGroup)} data-testid="initial-users" />
    ),
}))

vi.mock('containers/security/ActiveDirectoryMode', async () => ({
    ActiveDirectoryMode: () => <div data-testid="ad-mode" />,
}))

vi.mock('containers/security/SAMLMode', async () => ({
    SAMLMode: () => <div data-testid="saml-mode" />,
}))

vi.mock('containers/security/OAuth2Mode', async () => ({
    OAuth2Mode: () => <div data-testid="oauth2-mode" />,
}))

vi.mock('containers/security/SingleMode', async () => ({
    SingleMode: () => <div data-testid="single-mode" />,
}))

vi.mock('components/modal/InfoFieldModal', async () => ({
    __esModule: true,
    default: () => <span data-testid="info-modal" />,
}))

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

import { Security } from 'containers/Security'

const defaultSettings = {
    userMode: 'multi',
    allowProjectCreateDelete: true,
    administrators: ['admin'],
}

// Note: jsdom emits "Error: Not implemented: navigation (except hash changes)"
// when Security.tsx calls `window.location.reload()` on save. Location is
// non-configurable in jsdom, so reload cannot be stubbed at the API layer.
// The error is silenced globally in src/setupTests.ts via jest-fail-on-console's
// silenceMessage option.

describe('Security', () => {
    beforeEach(() => {
        vi.clearAllMocks()
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
        const adSettings = { ...defaultSettings, userMode: 'ad' }
        const groupsResponse = { Admins: {} }
        mockApiCall
            .mockResolvedValueOnce(adSettings)
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

    it('fetches user groups when backend mode already supports the groups endpoint', async () => {
        const adSettings = { ...defaultSettings, userMode: 'ad' }
        const groupsResponse = { Admins: {}, Viewers: {} }
        mockApiCall
            .mockResolvedValueOnce(adSettings) // fetchSecuritySettings
            .mockResolvedValueOnce(groupsResponse)  // fetchUserGroups
        mockUserMode = SecurityUserMode.AD

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups', undefined, expect.objectContaining({
                throwError: true,
                suppressErrorPages: true,
            }))
        })
    })

    it('does not fetch user groups when UI selects AD but backend is still multi', async () => {
        // Endpoint is only registered when active backend user.mode is not single/multi.
        // Switching the radio before clicking Apply must not trigger the fetch (which would 404 and redirect).
        mockApiCall.mockResolvedValueOnce(defaultSettings)
        mockUserMode = SecurityUserMode.AD

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/settings/authentication')
        })
        expect(mockApiCall).not.toHaveBeenCalledWith('/admin/management/groups', expect.anything(), expect.anything())
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
        expect(mockApiCall).not.toHaveBeenCalledWith('/admin/management/groups', expect.anything(), expect.anything())
    })

    it('fetches user groups for read-only external auth mode (wrapped userMode)', async () => {
        const adSettings = { ...defaultSettings, userMode: { value: 'ad', readOnly: true } }
        const groupsResponse = { Admins: {}, Viewers: {} }
        mockApiCall
            .mockResolvedValueOnce(adSettings)
            .mockResolvedValueOnce(groupsResponse)
        mockUserMode = { value: SecurityUserMode.AD, readOnly: true }

        await act(async () => {
            render(<Security />)
        })

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith('/admin/management/groups', undefined, expect.objectContaining({
                throwError: true,
                suppressErrorPages: true,
            }))
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
        const onOk = vi.mocked(Modal.confirm).mock.calls[0]![0].onOk
        await act(async () => {
            await onOk!()
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
        const onOk = vi.mocked(Modal.confirm).mock.calls[0]![0].onOk
        await act(async () => {
            await onOk!()
        })

        await waitFor(() => {
            expect(notification.error).toHaveBeenCalled()
        })
    })
})

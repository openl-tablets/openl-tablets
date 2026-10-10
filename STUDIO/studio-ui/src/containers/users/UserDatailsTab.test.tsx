import { Button, Form } from 'antd'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserDetailsTab } from './UserDatailsTab'
import { SystemContext } from '../../contexts/SystemContext'
import { DisplayUserName } from '../../constants'
import type { UserExternalFlags, UserProfile } from '../../types/user'
import type { SystemSettings } from '../../types/system'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const renderUserDetails = (requireEmailAndDisplayName: boolean, externalFlags?: UserExternalFlags) => {
    const onFinish = vi.fn()

    render(
        <Form onFinish={onFinish}>
            <UserDetailsTab
                displayPasswordField={false}
                externalFlags={externalFlags}
                requireEmailAndDisplayName={requireEmailAndDisplayName}
            />
            <Button htmlType="submit">Save</Button>
        </Form>
    )

    return onFinish
}

describe('UserDetailsTab', () => {
    it('rejects an empty email and display name in the profile editor', async () => {
        const onFinish = renderUserDetails(true)

        await userEvent.click(screen.getByRole('button', { name: 'Save' }))

        expect(await screen.findByText('users:edit_modal.email_required')).toBeInTheDocument()
        expect(screen.getByText('users:edit_modal.display_name_required')).toBeInTheDocument()
        expect(onFinish).not.toHaveBeenCalled()
    })

    it('does not require an email or a display name that an external system manages', async () => {
        const onFinish = renderUserDetails(true, {
            emailExternal: true,
            displayNameExternal: true,
            firstNameExternal: false,
            lastNameExternal: false,
            emailVerified: false,
        })

        await userEvent.click(screen.getByRole('button', { name: 'Save' }))

        await waitFor(() => expect(onFinish).toHaveBeenCalledOnce())
    })

    it('keeps email and display name optional when the requirement is disabled', async () => {
        const onFinish = renderUserDetails(false)

        await userEvent.click(screen.getByRole('button', { name: 'Save' }))

        await waitFor(() => expect(onFinish).toHaveBeenCalledOnce())
    })

    describe('Resend Verification Email', () => {
        const profile = {
            username: 'jdoe',
            email: 'jdoe@example.com',
            firstName: 'John',
            lastName: 'Doe',
            displayName: 'John Doe',
            externalFlags: {
                displayNameExternal: false,
                emailExternal: false,
                emailVerified: false,
                firstNameExternal: false,
                lastNameExternal: false,
            },
        } as UserProfile

        const renderProfile = (onResendVerification: () => void) => render(
            <SystemContext.Provider
                value={{
                    isExternalAuthSystem: false,
                    isUserManagementEnabled: true,
                    isGroupsManagementEnabled: true,
                    isPersonalAccessTokenEnabled: false,
                    systemSettings: { supportedFeatures: { emailVerification: true } } as SystemSettings,
                }}
            >
                <Form
                    initialValues={{
                        username: profile.username,
                        email: profile.email,
                        firstName: profile.firstName,
                        lastName: profile.lastName,
                        displayName: profile.displayName,
                        displayNameSelect: DisplayUserName.FirstLast,
                    }}
                >
                    <UserDetailsTab
                        showResendVerification
                        cooldown={0}
                        displayPasswordField={false}
                        onResendVerification={onResendVerification}
                        userProfile={profile}
                    />
                </Form>
            </SystemContext.Provider>
        )

        it('resends the email from a form nobody changed', async () => {
            const onResendVerification = vi.fn()
            renderProfile(onResendVerification)

            const resend = await screen.findByRole('button', { name: 'users:resend_verification_email' })
            await waitFor(() => expect(resend).toBeEnabled())
            await userEvent.click(resend)

            expect(onResendVerification).toHaveBeenCalledOnce()
        })

        it('keeps the email of the user, not changes to other fields, for the resend', async () => {
            renderProfile(vi.fn())

            await userEvent.type(screen.getByLabelText('users:edit_modal.first_name'), 'ny')

            expect(await screen.findByRole('button', { name: 'users:resend_verification_email' })).toBeEnabled()
        })

        it('waits for a changed email to be saved before resending', async () => {
            renderProfile(vi.fn())

            const email = screen.getByDisplayValue('jdoe@example.com')
            await userEvent.clear(email)
            await userEvent.type(email, 'john@example.com')

            expect(screen.getByRole('button', { name: 'users:resend_verification_email' })).toBeDisabled()
        })
    })
})

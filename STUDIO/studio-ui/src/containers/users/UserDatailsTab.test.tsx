import { Button, Form } from 'antd'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserDetailsTab } from './UserDatailsTab'
import type { UserExternalFlags } from '../../types/user'

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
})

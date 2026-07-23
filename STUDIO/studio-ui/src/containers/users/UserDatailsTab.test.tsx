import React from 'react'
import { Button, Form } from 'antd'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserDetailsTab } from './UserDatailsTab'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const renderUserDetails = (requireEmailAndDisplayName: boolean) => {
    const onFinish = vi.fn()

    render(
        <Form onFinish={onFinish}>
            <UserDetailsTab
                displayPasswordField={false}
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

    it('keeps email and display name optional when the requirement is disabled', async () => {
        const onFinish = renderUserDetails(false)

        await userEvent.click(screen.getByRole('button', { name: 'Save' }))

        await waitFor(() => expect(onFinish).toHaveBeenCalledOnce())
    })
})

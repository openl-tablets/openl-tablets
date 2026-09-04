import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserProfileCompletionModal } from './UserProfileCompletionModal'
import * as services from 'services'
import type { UserProfile } from '../../types/user'
import type { MockedFunction } from 'vitest'

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return {
        ...actual,
        notification: {
            success: vi.fn(),
            error: vi.fn(),
        },
    }
})

vi.mock('services', () => ({
    apiCall: vi.fn(),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    const i18n = { language: 'en' }
    return {
        useTranslation: () => ({ t, i18n }),
    }
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

const createProfile = (overrides: Partial<UserProfile> = {}): UserProfile => ({
    username: 'testuser',
    firstName: '',
    lastName: '',
    displayName: '',
    email: '',
    ...overrides,
} as UserProfile)

const defaultProps = () => ({
    open: true,
    profile: createProfile(),
    required: false,
    onSave: vi.fn(),
    onCancel: vi.fn(),
})

const renderModal = async (props = defaultProps()) => {
    let result: ReturnType<typeof render>
    await act(async () => {
        result = render(<UserProfileCompletionModal {...props} />)
    })
    return { ...result!, props }
}

const getDisplayNameInput = () =>
    screen.getByRole('textbox', { name: 'users:profile_completion.display_name' })
const getEmailInput = () => screen.getByRole('textbox', { name: 'users:profile_completion.email' })
const getFirstNameInput = () => screen.getByRole('textbox', { name: 'users:profile_completion.first_name' })
const getLastNameInput = () => screen.getByRole('textbox', { name: 'users:profile_completion.last_name' })
const getSaveButton = () => screen.getByRole('button', { name: 'users:profile_completion.save' })

describe('UserProfileCompletionModal', () => {
    const user = userEvent.setup({ delay: null })

    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('renders the profile form while open', async () => {
        await renderModal()

        expect(screen.getByText('users:profile_completion.title')).toBeInTheDocument()
        expect(getEmailInput()).toBeInTheDocument()
        expect(getFirstNameInput()).toBeInTheDocument()
        expect(getLastNameInput()).toBeInTheDocument()
        expect(getDisplayNameInput()).toBeInTheDocument()
    })

    it('does not render the profile form when closed', async () => {
        const props = defaultProps()
        props.open = false
        await renderModal(props)

        expect(screen.queryByText('users:profile_completion.title')).not.toBeInTheDocument()
    })

    it('populates the form from the current profile', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            firstName: 'John',
            lastName: 'Doe',
            displayName: 'John Doe',
            email: 'john@example.com',
        })

        await renderModal(props)

        expect(getFirstNameInput()).toHaveValue('John')
        expect(getLastNameInput()).toHaveValue('Doe')
        expect(getDisplayNameInput()).toHaveValue('John Doe')
        expect(getEmailInput()).toHaveValue('john@example.com')
    })

    it('requires email and display name while allowing empty first and last names', async () => {
        mockApiCall.mockResolvedValueOnce({})
        const { props } = await renderModal()

        await user.type(getEmailInput(), 'jane@example.com')
        await user.type(getDisplayNameInput(), 'Jane Doe')
        await user.click(getSaveButton())

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/info',
                {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        firstName: '',
                        lastName: '',
                        displayName: 'Jane Doe',
                        email: 'jane@example.com',
                    }),
                },
                { throwError: true }
            )
        })
        await waitFor(() => expect(props.onSave).toHaveBeenCalled())
    })

    it('does not submit when required fields are empty', async () => {
        const { props } = await renderModal()

        await user.click(getSaveButton())

        expect(mockApiCall).not.toHaveBeenCalled()
        expect(props.onSave).not.toHaveBeenCalled()
    })

    it('does not submit a whitespace-only display name', async () => {
        const { props } = await renderModal()

        await user.type(getEmailInput(), 'jane@example.com')
        await user.type(getDisplayNameInput(), '   ')
        await user.click(getSaveButton())

        expect(mockApiCall).not.toHaveBeenCalled()
        expect(props.onSave).not.toHaveBeenCalled()
    })

    it('creates a display name from the optional first and last names', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            firstName: 'Jane',
            lastName: 'Doe',
            email: 'jane@example.com',
        })
        mockApiCall.mockResolvedValueOnce({})

        await renderModal(props)

        expect(getDisplayNameInput()).toHaveValue('Jane Doe')
        expect(getDisplayNameInput()).toBeDisabled()

        await user.click(getSaveButton())

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/info',
                expect.objectContaining({
                    body: JSON.stringify({
                        firstName: 'Jane',
                        lastName: 'Doe',
                        displayName: 'Jane Doe',
                        email: 'jane@example.com',
                    }),
                }),
                { throwError: true }
            )
        })
    })

    it('shows the server error without completing the profile', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            firstName: 'Jane',
            lastName: 'Doe',
            displayName: 'Jane Doe',
            email: 'jane@example.com',
        })
        mockApiCall.mockRejectedValueOnce(new Error('Save failed'))

        await renderModal(props)
        await user.click(getSaveButton())

        await waitFor(() => expect(screen.getByText('Save failed')).toBeInTheDocument())
        expect(props.onSave).not.toHaveBeenCalled()
    })

    it('shows a generic error when the server response has no message', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            firstName: 'Jane',
            lastName: 'Doe',
            displayName: 'Jane Doe',
            email: 'jane@example.com',
        })
        mockApiCall.mockRejectedValueOnce({})

        await renderModal(props)
        await user.click(getSaveButton())

        await waitFor(() => {
            expect(screen.getByText('users:profile_completion.save_failed')).toBeInTheDocument()
        })
    })

    it('cannot be dismissed when profile completion is required', async () => {
        const props = defaultProps()
        props.required = true

        await renderModal(props)

        expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument()
        expect(screen.queryByRole('button', { name: 'Cancel' })).not.toBeInTheDocument()
    })

    it('can be cancelled when used as a defensive action guard', async () => {
        const { props } = await renderModal()
        const cancelButton = screen
            .getAllByRole('button')
            .find(button => button !== getSaveButton() && button.getAttribute('aria-label') !== 'Close')

        expect(cancelButton).toBeDefined()
        await user.click(cancelButton as HTMLElement)

        expect(props.onCancel).toHaveBeenCalled()
    })

    it('keeps a custom display name editable', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            firstName: 'John',
            lastName: 'Doe',
            displayName: 'JD',
            email: 'john@example.com',
        })

        await renderModal(props)

        expect(getDisplayNameInput()).toHaveValue('JD')
        expect(getDisplayNameInput()).not.toBeDisabled()
    })

    it('disables the email field when the email is managed externally', async () => {
        const props = defaultProps()
        props.profile = createProfile({
            email: 'external@example.com',
            externalFlags: {
                displayNameExternal: false,
                emailExternal: true,
                emailVerified: true,
                firstNameExternal: false,
                lastNameExternal: false,
            },
        })

        await renderModal(props)

        expect(getEmailInput()).toBeDisabled()
        expect(getDisplayNameInput()).not.toBeDisabled()
    })

    it('blocks cancellation while a save is in flight', async () => {
        let resolveApiCall: (value: unknown) => void = () => {}
        mockApiCall.mockImplementationOnce(
            () => new Promise((resolve) => { resolveApiCall = resolve })
        )
        const props = defaultProps()
        props.profile = createProfile({
            displayName: 'Jane Doe',
            email: 'jane@example.com',
        })

        await renderModal(props)
        const cancelButton = screen
            .getAllByRole('button')
            .find(button => button !== getSaveButton() && button.getAttribute('aria-label') !== 'Close')
        expect(cancelButton).toBeDefined()

        await user.click(getSaveButton())
        await waitFor(() => expect(cancelButton).toBeDisabled())

        await user.keyboard('{Escape}')
        expect(props.onCancel).not.toHaveBeenCalled()

        resolveApiCall({})
        await waitFor(() => expect(props.onSave).toHaveBeenCalled())
    })
})

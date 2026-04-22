import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { CommitInfoModal } from 'containers/MergeModal/CommitInfoModal'
import * as services from 'services'

jest.mock('services', () => ({
    apiCall: jest.fn(),
}))

jest.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string) => key,
        i18n: { language: 'en' },
    }),
}))

const mockApiCall = services.apiCall as jest.MockedFunction<typeof services.apiCall>

const defaultProps = () => ({
    visible: true,
    username: 'testuser',
    onSave: jest.fn(),
    onCancel: jest.fn(),
})

const renderModal = async (props = defaultProps()) => {
    let result: ReturnType<typeof render>
    await act(async () => {
        result = render(<CommitInfoModal {...props} />)
    })
    return { ...result!, props }
}

const getDisplayNameInput = () => screen.getByRole('textbox', { name: 'merge:commit_info.display_name' })
const getEmailInput = () => screen.getByRole('textbox', { name: 'merge:commit_info.email' })
const getSaveButton = () => screen.getByRole('button', { name: 'merge:buttons.save' })

describe('CommitInfoModal', () => {
    beforeEach(() => {
        jest.clearAllMocks()
    })

    it('renders modal with title and form fields', async () => {
        mockApiCall.mockResolvedValueOnce({ displayName: '', email: '' })

        await renderModal()

        expect(screen.getByText('merge:commit_info.title')).toBeInTheDocument()
        expect(getDisplayNameInput()).toBeInTheDocument()
        expect(getEmailInput()).toBeInTheDocument()
    })

    it('does not render form fields when not visible', async () => {
        const props = defaultProps()
        props.visible = false

        await renderModal(props)

        expect(screen.queryByText('merge:commit_info.title')).not.toBeInTheDocument()
    })

    it('loads user info on open and populates form', async () => {
        mockApiCall.mockResolvedValueOnce({ displayName: 'John Doe', email: 'john@example.com' })

        await renderModal()

        expect(mockApiCall).toHaveBeenCalledWith(
            '/users/testuser',
            { method: 'GET' },
            true
        )

        expect(getDisplayNameInput()).toHaveValue('John Doe')
        expect(getEmailInput()).toHaveValue('john@example.com')
    })

    it('sets empty values when user info fetch fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('Not found'))

        await renderModal()

        expect(getDisplayNameInput()).toHaveValue('')
        expect(getEmailInput()).toHaveValue('')
    })

    it('does not load user info when username is empty', async () => {
        const props = defaultProps()
        props.username = ''

        await renderModal(props)

        expect(mockApiCall).not.toHaveBeenCalled()
    })

    it('shows spinner while loading then shows form when done', async () => {
        let resolveApi: (value: any) => void
        mockApiCall.mockImplementationOnce(() => new Promise(resolve => { resolveApi = resolve }))

        const props = defaultProps()
        await act(async () => {
            render(<CommitInfoModal {...props} />)
        })

        // Form should not be visible while loading
        expect(screen.queryByRole('textbox', { name: 'merge:commit_info.display_name' })).not.toBeInTheDocument()

        await act(async () => {
            resolveApi!({ displayName: '', email: '' })
        })

        // Form should appear after loading completes
        expect(getDisplayNameInput()).toBeInTheDocument()
    })

    it('saves user info and calls onSave on OK', async () => {
        mockApiCall
            .mockResolvedValueOnce({ displayName: '', email: '' }) // load
            .mockResolvedValueOnce({}) // save

        const { props } = await renderModal()

        await userEvent.type(getDisplayNameInput(), 'Jane Doe')
        await userEvent.type(getEmailInput(), 'jane@example.com')

        await userEvent.click(getSaveButton())

        await waitFor(() => {
            expect(mockApiCall).toHaveBeenCalledWith(
                '/users/testuser',
                {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ displayName: 'Jane Doe', email: 'jane@example.com' }),
                },
                true
            )
        })

        await waitFor(() => {
            expect(props.onSave).toHaveBeenCalled()
        })
    })

    it('shows error when save fails', async () => {
        mockApiCall
            .mockResolvedValueOnce({ displayName: '', email: '' }) // load
            .mockRejectedValueOnce(new Error('Save failed')) // save

        await renderModal()

        await userEvent.type(getDisplayNameInput(), 'Jane')
        await userEvent.type(getEmailInput(), 'jane@example.com')

        await userEvent.click(getSaveButton())

        await waitFor(() => {
            expect(screen.getByText('Save failed')).toBeInTheDocument()
        })
    })

    it('shows generic error when save fails without message', async () => {
        mockApiCall
            .mockResolvedValueOnce({ displayName: '', email: '' }) // load
            .mockRejectedValueOnce({}) // save - no message

        await renderModal()

        await userEvent.type(getDisplayNameInput(), 'Jane')
        await userEvent.type(getEmailInput(), 'jane@example.com')

        await userEvent.click(getSaveButton())

        await waitFor(() => {
            expect(screen.getByText('merge:errors.commit_info_failed')).toBeInTheDocument()
        })
    })

    it('calls onCancel when Cancel button clicked', async () => {
        mockApiCall.mockResolvedValueOnce({ displayName: '', email: '' })

        const { props } = await renderModal()

        const saveButton = getSaveButton()
        const cancelButton = screen
            .getAllByRole('button')
            .find(button => button !== saveButton && button.getAttribute('aria-label') !== 'Close')
        expect(cancelButton).toBeDefined()
        await userEvent.click(cancelButton as HTMLElement)

        expect(props.onCancel).toHaveBeenCalled()
    })

    it('encodes username in API URL', async () => {
        const props = defaultProps()
        props.username = 'user with spaces'

        mockApiCall.mockResolvedValueOnce({ displayName: '', email: '' })

        await renderModal(props)

        expect(mockApiCall).toHaveBeenCalledWith(
            '/users/user%20with%20spaces',
            { method: 'GET' },
            true
        )
    })

    it('does not submit when required fields are empty', async () => {
        mockApiCall.mockResolvedValueOnce({ displayName: '', email: '' })

        const { props } = await renderModal()

        await userEvent.click(getSaveButton())

        // Only the load call should have been made, not a save call
        expect(mockApiCall).toHaveBeenCalledTimes(1)
        expect(props.onSave).not.toHaveBeenCalled()
    })
})

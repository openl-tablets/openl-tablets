import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserSettings } from './UserSettings'
import * as services from '../services'
import { useUserStore } from 'store'
import type { MockedFunction } from 'vitest'
import type { UserProfile } from '../types/user'

vi.mock('../services', () => ({ apiCall: vi.fn() }))

vi.mock('../services/tables', () => ({
    getTableThemes: () => Promise.resolve([{ id: 'default', name: 'Default' }, { id: 'green', name: 'Green' }]),
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// A real store, so the page renders the profile it holds and renders again when a save reads it anew.
vi.mock('store', async () => {
    const { create } = await import('zustand')
    return { useUserStore: create(() => ({})) }
})

const profile = {
    username: 'jdoe',
    email: 'jdoe@example.com',
    firstName: 'John',
    lastName: 'Doe',
    displayName: 'John Doe',
    administrator: false,
    externalFlags: {},
    testsFailuresPerTest: 5,
    showHeader: true,
    showFormulas: false,
    testsPerPage: 5,
    testsFailuresOnly: false,
    showComplexResult: false,
    showRealNumbers: false,
} as UserProfile

vi.mock('antd', async () => {
    const actual = await vi.importActual('antd')
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        ...actual,
        notification: { error: vi.fn(), success: vi.fn() },
    })
})

const mockApiCall = services.apiCall as MockedFunction<typeof services.apiCall>

describe('UserSettings', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useUserStore.setState({ userProfile: profile, fetchUserProfile: vi.fn(async () => undefined) })
    })

    /** The body of the given PUT the page sent. */
    const sentBody = (call: number): unknown =>
        JSON.parse((mockApiCall.mock.calls[call]?.[1] as RequestInit).body as string)

    it('shows a success notification when settings are saved', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserSettings />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.success).toHaveBeenCalledWith({
            title: 'users:user_settings_updated_successfully',
        }))
    })

    it('sends only the settings the user changed', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserSettings />)

        await userEvent.click(screen.getAllByRole('checkbox')[1] as HTMLElement)
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ showFormulas: true })
    })

    it('keeps what another tab saved when a second save follows the first', async () => {
        // The profile the first save reads back holds a change another tab saved in the meantime.
        useUserStore.setState({
            fetchUserProfile: vi.fn(async () => {
                useUserStore.setState({ userProfile: { ...profile, showFormulas: true, showHeader: false } })
            }),
        })
        mockApiCall.mockResolvedValue(undefined)
        render(<UserSettings />)

        await userEvent.click(screen.getAllByRole('checkbox')[1] as HTMLElement)
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))
        await waitFor(() => expect(screen.getAllByRole('checkbox')[0]).not.toBeChecked())

        await userEvent.click(screen.getAllByRole('checkbox')[2] as HTMLElement)
        // jsdom never ends the leave motion of the loading icon, so the name keeps it: match the label, wait for the state.
        const save = screen.getByRole('button', { name: /common:btn\.save/ })
        await waitFor(() => expect(save).not.toHaveClass('ant-btn-loading'))
        await userEvent.click(save)

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledTimes(2))
        expect(sentBody(0)).toEqual({ showFormulas: true })
        expect(sentBody(1)).toEqual({ testsFailuresOnly: true })
    })

    it('draws the tables with the formatting of the Excel file while the profile names no theme', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserSettings />)

        expect(await screen.findByTitle('users:settings.excel_formatting')).toBeInTheDocument()
        await userEvent.click(screen.getByLabelText('users:settings.table_theme'))
        await userEvent.click(await screen.findByTitle('Green'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ tableTheme: 'green' })
    })

    it('goes back to the formatting of the Excel file by naming no theme', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        useUserStore.setState({ userProfile: { ...profile, tableTheme: 'green' } })
        render(<UserSettings />)

        await userEvent.click(screen.getByLabelText('users:settings.table_theme'))
        await userEvent.click(await screen.findByTitle('users:settings.excel_formatting'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ tableTheme: '' })
    })

    it('saves the table theme the user chooses, offered by its name', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        useUserStore.setState({ userProfile: { ...profile, tableTheme: 'default' } })
        render(<UserSettings />)

        await userEvent.click(screen.getByLabelText('users:settings.table_theme'))
        await userEvent.click(await screen.findByTitle('Green'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ tableTheme: 'green' })
    })

    it('sends nothing to change when nothing was changed', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        render(<UserSettings />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({})
    })

    it('shows an error notification when saving settings fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        render(<UserSettings />)

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })
})

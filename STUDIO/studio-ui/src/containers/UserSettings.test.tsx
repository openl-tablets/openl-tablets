import { act, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { notification } from 'antd'
import { UserSettings } from './UserSettings'
import * as services from '../services'
import { renderInTheme } from '../testing/theme'
import { THEME_TABLES_KEY } from '../utils/themeMode'
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

/** The page inside the provider that keeps what the browser remembers, as the application draws it. */
const renderSettings = (tablesFollowTheme = false) => renderInTheme(<UserSettings />, { tablesFollowTheme })

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
        renderSettings()

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.success).toHaveBeenCalledWith({
            title: 'users:user_settings_updated_successfully',
        }))
    })

    it('sends only the settings the user changed', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.show_formulas'))
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
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.show_formulas'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))
        await waitFor(() => expect(screen.getByLabelText('users:settings.show_header')).not.toBeChecked())

        await userEvent.click(screen.getByLabelText('users:settings.failures_only'))
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
        renderSettings()

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
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.table_theme'))
        await userEvent.click(await screen.findByTitle('users:settings.excel_formatting'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ tableTheme: '' })
    })

    it('saves the table theme the user chooses, offered by its name', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        useUserStore.setState({ userProfile: { ...profile, tableTheme: 'default' } })
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.table_theme'))
        await userEvent.click(await screen.findByTitle('Green'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({ tableTheme: 'green' })
    })

    it('sends nothing to change when nothing was changed', async () => {
        mockApiCall.mockResolvedValueOnce(undefined)
        renderSettings()

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        expect(sentBody(0)).toEqual({})
    })

    it('remembers in the browser, not in the profile, to draw the tables in the look of the Studio theme',
        async () => {
            mockApiCall.mockResolvedValueOnce(undefined)
            renderSettings()

            // The look of the Studio theme is asked for whatever table theme the profile names, Excel Formatting too.
            expect(await screen.findByTitle('users:settings.excel_formatting')).toBeInTheDocument()
            await userEvent.click(screen.getByLabelText('users:settings.override_with_studio_theme'))
            await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

            await waitFor(() => expect(localStorage.getItem(THEME_TABLES_KEY)).toBe('true'))
            expect(sentBody(0)).toEqual({})
        })

    it('keeps showing what was saved while the profile is read anew', async () => {
        // The save is answered once the test lets it, and the profile read after it is never answered.
        let answerSave: (value?: unknown) => void = () => undefined
        mockApiCall.mockReturnValueOnce(new Promise(resolve => {
            answerSave = resolve
        }))
        useUserStore.setState({ fetchUserProfile: vi.fn(() => new Promise<void>(() => undefined)) })
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.show_formulas'))
        await userEvent.click(screen.getByLabelText('users:settings.override_with_studio_theme'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))
        await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith('/users/profile', expect.anything()))
        await act(async () => answerSave())

        expect(localStorage.getItem(THEME_TABLES_KEY)).toBe('true')
        expect(screen.getByLabelText('users:settings.show_formulas')).toBeChecked()
        expect(screen.getByLabelText('users:settings.override_with_studio_theme')).toBeChecked()
    })

    it('sets the table theme and its override apart as experimental settings', async () => {
        renderSettings()

        const experimental = await screen.findByText('users:settings.experimental')
        const testing = screen.getByText('users:settings.testing_settings')
        for (const label of ['users:settings.table_theme', 'users:settings.override_with_studio_theme']) {
            const field = screen.getByLabelText(label)
            expect(experimental.compareDocumentPosition(field) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
            expect(field.compareDocumentPosition(testing) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
        }
        // The other table settings stay above the experimental ones.
        const formulas = screen.getByLabelText('users:settings.show_formulas')
        expect(formulas.compareDocumentPosition(experimental) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    })

    it('tells that the table theme and its override change only what the screen shows', async () => {
        renderSettings()

        const [tableTheme, override] = await screen.findAllByRole('img', { name: 'question-circle' })
        await userEvent.hover(tableTheme as HTMLElement)
        expect(await screen.findByText('users:settings.table_theme_info')).toBeInTheDocument()
        await userEvent.hover(override as HTMLElement)
        expect(await screen.findByText('users:settings.override_with_studio_theme_info')).toBeInTheDocument()
    })

    it('shows the choice the browser remembers', async () => {
        useUserStore.setState({ userProfile: { ...profile, tableTheme: 'green' } })
        renderSettings(true)

        // The table theme is named once the themes are read.
        expect(await screen.findByTitle('Green')).toBeInTheDocument()
        expect(screen.getByLabelText('users:settings.override_with_studio_theme')).toBeChecked()
    })

    it('keeps the choice the browser remembers when the save fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        useUserStore.setState({ userProfile: { ...profile, tableTheme: 'green' } })
        renderSettings()

        await userEvent.click(screen.getByLabelText('users:settings.override_with_studio_theme'))
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
        expect(localStorage.getItem(THEME_TABLES_KEY)).toBe('false')
    })

    it('shows an error notification when saving settings fails', async () => {
        mockApiCall.mockRejectedValueOnce(new Error('save failed'))
        renderSettings()

        await userEvent.click(screen.getByRole('button', { name: 'common:btn.save' }))

        await waitFor(() => expect(notification.error).toHaveBeenCalledWith({ title: 'save failed' }))
    })
})

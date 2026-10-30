import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SettingsMenu } from './SettingsMenu'
import { useSettingsStore } from 'store'
import { DEFAULT_USER_SETTINGS, readUserSettings } from 'store/settingsStore'
import dayjs from 'dayjs'
import { datePickerFormatForLocale, formatDate, formatDateTime } from 'utils/dateFormat'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** The menu while it is on the screen; Ant Design keeps a closed one in the page, hidden. */
const openMenu = (): Element | null => document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)')

const renderMenu = () => render(
    // Without motion a closed menu is hidden at once: jsdom never ends an animation.
    <ConfigProvider theme={{ token: { motion: false } }}>
        <span data-testid="elsewhere">page</span>
        <SettingsMenu />
    </ConfigProvider>
)

describe('SettingsMenu', () => {
    beforeEach(() => {
        localStorage.clear()
        useSettingsStore.setState({ settings: DEFAULT_USER_SETTINGS })
    })

    it('offers the settings in groups, each switch showing the setting in force', async () => {
        useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, showFormulas: true } })
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))

        expect(await screen.findByText('common:settings.table_settings')).toBeInTheDocument()
        expect(screen.getByText('common:settings.testing_settings')).toBeInTheDocument()
        expect(screen.getByText('common:settings.trace_settings')).toBeInTheDocument()
        expect(screen.getByTestId('settings-showHeader')).toBeChecked()
        expect(screen.getByTestId('settings-showFormulas')).toBeChecked()
        expect(screen.getByTestId('settings-testsFailuresOnly')).not.toBeChecked()
        expect(screen.getByTestId('settings-showComplexResult')).not.toBeChecked()
        expect(screen.getByTestId('settings-showRealNumbers')).not.toBeChecked()
    })

    it('applies and remembers a switch at once, staying open, and closes on a click elsewhere', async () => {
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.show_header'))
        await userEvent.click(screen.getByText('common:settings.show_numbers_without_formatting'))

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, showHeader: false, showRealNumbers: true })
        expect(screen.getByTestId('settings-showHeader')).not.toBeChecked()
        expect(openMenu()).not.toBeNull()

        await userEvent.click(screen.getByTestId('elsewhere'))

        await waitFor(() => expect(openMenu()).toBeNull())
    })

    it('picks how many tests a page holds among the sizes offered', async () => {
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.tests_per_page'))
        await userEvent.click(await screen.findByText('execution:tests.all'))

        expect(readUserSettings().testsPerPage).toBe(-1)
    })

    it('follows the regional settings of the browser at first, offering no format of its own', async () => {
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))

        expect(await screen.findByText('common:settings.date_time_settings')).toBeInTheDocument()
        expect(screen.getByTestId('settings-useSystemDateFormat')).toBeChecked()
        expect(screen.getByTestId('settings-useSystemDateFormat-hint')).toBeInTheDocument()
        expect(screen.queryByText('common:settings.date_format')).not.toBeInTheDocument()
        expect(screen.queryByTestId('settings-hours24')).not.toBeInTheDocument()
    })

    it('picks a date format and a clock once the system is not followed', async () => {
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.use_system_formats'))
        await userEvent.click(await screen.findByText('common:settings.date_format'))
        for (const name of ['us_numeric', 'us_text', 'uk', 'european', 'iso']) {
            expect(await screen.findByText(`common:settings.formats.${name}`)).toBeInTheDocument()
        }
        await userEvent.click(screen.getByText('2026-12-31'))
        await userEvent.click(screen.getByText('common:settings.hours_24'))

        expect(readUserSettings()).toEqual({
            ...DEFAULT_USER_SETTINGS, useSystemDateFormat: false, dateFormat: 'YYYY-MM-DD', hours24: true,
        })
        // a moment is written in the browser time zone, in the format and on the clock picked
        expect(formatDateTime('2026-10-29T18:20:19')).toBe('2026-10-29 18:20:19')
        expect(formatDate('2026-10-29T18:20:19')).toBe('2026-10-29')
    })

    it('goes back to the regional settings of the browser', async () => {
        useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, useSystemDateFormat: false } })
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.use_system_formats'))

        expect(readUserSettings().useSystemDateFormat).toBe(true)
        expect(formatDate('2026-10-29T18:20:19')).toBe(dayjs('2026-10-29T18:20:19').format(datePickerFormatForLocale()))
        await waitFor(() => expect(screen.queryByText('common:settings.date_format')).not.toBeInTheDocument())
    })
})

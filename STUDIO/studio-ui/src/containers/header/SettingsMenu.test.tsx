import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SettingsMenu } from './SettingsMenu'
import { useSettingsStore } from 'store'
import { DEFAULT_USER_SETTINGS, readUserSettings } from 'store/settingsStore'
import dayjs from 'dayjs'
import { datePickerFormatForLocale, formatDate, formatDateTime, timeFormatForLocale } from 'utils/dateFormat'

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

    it('writes dates and times as the regional settings of the browser do at first, offering them first', async () => {
        const system = dayjs('2026-12-31T13:45:30')
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        expect(await screen.findByText('common:settings.date_time_settings')).toBeInTheDocument()
        expect(screen.getByText('common:settings.date_format').parentElement)
            .toHaveTextContent(system.format(datePickerFormatForLocale()))
        expect(screen.getByText('common:settings.time_format').parentElement)
            .toHaveTextContent(system.format(timeFormatForLocale()))

        await userEvent.click(screen.getByText('common:settings.date_format'))

        // the system format first, marked as such, then the formats on offer in their order
        await screen.findByText('2026-12-31')
        const items = screen.getAllByRole('menuitem').map(item => item.textContent)
        const first = items.findIndex(text => text === `${system.format(datePickerFormatForLocale())}common:settings.system_format`)
        expect(items.slice(first)).toEqual([
            `${system.format(datePickerFormatForLocale())}common:settings.system_format`,
            '31/12/2026',
            '31.12.2026',
            '31-12-2026',
            '31 Dec 2026',
            '2026-12-31',
            '2026.12.31',
            '2026/12/31',
            'Dec 31, 2026',
            '12/31/2026',
        ])
    })

    it('offers the time of the regional settings first, then a 24-hour and a 12-hour clock', async () => {
        const system = `${dayjs('2026-12-31T13:45:30').format(timeFormatForLocale())}common:settings.system_format`
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.time_format'))
        await screen.findByText('13:45:30')

        const items = screen.getAllByRole('menuitem').map(item => item.textContent)
        expect(items.slice(items.indexOf(system))).toEqual([system, '13:45:30', '1:45:30 PM'])
    })

    it('picks a date format and a time format apart', async () => {
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.date_format'))
        await userEvent.click(await screen.findByText('2026-12-31'))
        await userEvent.click(screen.getByText('common:settings.time_format'))
        await userEvent.click(await screen.findByText('13:45:30'))

        expect(readUserSettings()).toEqual({ ...DEFAULT_USER_SETTINGS, dateFormat: 'YYYY-MM-DD', timeFormat: 'HH:mm:ss' })
        // a moment is written in the browser time zone, in the formats picked
        expect(formatDateTime('2026-10-29T18:20:19')).toBe('2026-10-29 18:20:19')
        expect(formatDate('2026-10-29T18:20:19')).toBe('2026-10-29')
    })

    it('goes back to the regional settings of the browser by their format, first in the list', async () => {
        useSettingsStore.setState({ settings: { ...DEFAULT_USER_SETTINGS, dateFormat: 'DD.MM.YYYY' } })
        renderMenu()

        await userEvent.click(screen.getByTestId('settings-menu'))
        await userEvent.click(await screen.findByText('common:settings.date_format'))
        await userEvent.click(await screen.findByText('common:settings.system_format'))

        expect(readUserSettings().dateFormat).toBe('')
        expect(formatDate('2026-10-29T18:20:19')).toBe(dayjs('2026-10-29T18:20:19').format(datePickerFormatForLocale()))
    })
})

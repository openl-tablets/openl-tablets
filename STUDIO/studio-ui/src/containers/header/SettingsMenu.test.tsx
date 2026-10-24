import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SettingsMenu } from './SettingsMenu'
import { useSettingsStore } from 'store'
import { DEFAULT_USER_SETTINGS, readUserSettings } from 'store/settingsStore'

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
})

import { render, screen, waitFor } from '@testing-library/react'
import { ConfigProvider } from 'antd'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ThemeSwitch } from './ThemeSwitch'
import { AppThemeProvider } from '../../providers/AppThemeProvider'
import { THEME_COMPACT_KEY, THEME_NAME_KEY } from '../../utils/themeMode'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** The menu while it is on the screen; Ant Design keeps a closed one in the page, hidden. */
const openMenu = (): Element | null => document.querySelector('.ant-dropdown:not(.ant-dropdown-hidden)')

describe('ThemeSwitch menu', () => {
    beforeEach(() => localStorage.clear())

    it('stays open after a pick, so choices can be tried in a row, and closes on a click elsewhere', async () => {
        render(
            <AppThemeProvider>
                {/* Without motion a closed menu is hidden at once: jsdom never ends an animation. */}
                <ConfigProvider theme={{ token: { motion: false } }}>
                    <span data-testid="elsewhere">page</span>
                    <ThemeSwitch />
                </ConfigProvider>
            </AppThemeProvider>
        )

        await userEvent.click(screen.getByTestId('theme-switch'))
        await userEvent.click(await screen.findByText('common:theme.names.dracula'))
        await userEvent.click(screen.getByText('common:theme.compact'))

        expect(localStorage.getItem(THEME_NAME_KEY)).toBe('dracula')
        expect(localStorage.getItem(THEME_COMPACT_KEY)).toBe('true')
        expect(openMenu()).not.toBeNull()

        await userEvent.click(screen.getByTestId('elsewhere'))

        await waitFor(() => expect(openMenu()).toBeNull())
    })
})

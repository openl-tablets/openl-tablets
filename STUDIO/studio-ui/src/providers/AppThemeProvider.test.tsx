import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useThemeMode } from 'antd-style'
import { beforeEach, describe, expect, it } from 'vitest'
import { theme as antdTheme } from 'antd'
import { AppThemeProvider, densityTheme, useAppTheme } from './AppThemeProvider'
import { THEME_COMPACT_KEY, THEME_MODE_KEY } from '../utils/themeMode'

const ThemeProbe = () => {
    const { isDarkMode, setThemeMode, themeMode } = useThemeMode()
    const { compact, setCompact } = useAppTheme()

    return (
        <>
            <span data-testid="mode">{themeMode}</span>
            <span data-testid="dark">{String(isDarkMode)}</span>
            <span data-testid="compact">{String(compact)}</span>
            <button onClick={() => setThemeMode('dark')} type="button">go dark</button>
            <button onClick={() => setCompact(!compact)} type="button">toggle density</button>
        </>
    )
}

const renderProbe = () => render(<AppThemeProvider><ThemeProbe /></AppThemeProvider>)

describe('AppThemeProvider', () => {
    beforeEach(() => localStorage.clear())

    it('follows the system when nothing was picked yet', () => {
        renderProbe()

        expect(screen.getByTestId('mode').textContent).toBe('auto')
    })

    it('starts in the appearance remembered from the previous visit', () => {
        localStorage.setItem(THEME_MODE_KEY, 'dark')

        renderProbe()

        expect(screen.getByTestId('mode').textContent).toBe('dark')
        expect(screen.getByTestId('dark').textContent).toBe('true')
    })

    it('applies a newly picked appearance and remembers it', async () => {
        renderProbe()

        await userEvent.click(screen.getByText('go dark'))

        expect(screen.getByTestId('mode').textContent).toBe('dark')
        expect(screen.getByTestId('dark').textContent).toBe('true')
        expect(localStorage.getItem(THEME_MODE_KEY)).toBe('dark')
    })

    it('starts comfortable and turns compact on request, remembering the choice', async () => {
        renderProbe()

        expect(screen.getByTestId('compact').textContent).toBe('false')

        await userEvent.click(screen.getByText('toggle density'))

        expect(screen.getByTestId('compact').textContent).toBe('true')
        expect(localStorage.getItem(THEME_COMPACT_KEY)).toBe('true')
    })

    it('starts in the density remembered from the previous visit', () => {
        localStorage.setItem(THEME_COMPACT_KEY, 'true')

        renderProbe()

        expect(screen.getByTestId('compact').textContent).toBe('true')
    })

    it('adds the compact algorithm only when compact was picked', () => {
        expect(densityTheme(true)).toEqual({ algorithm: antdTheme.compactAlgorithm })
        expect(densityTheme(false)).toEqual({})
    })
})

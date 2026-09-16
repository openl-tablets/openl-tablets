import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useThemeMode } from 'antd-style'
import { beforeEach, describe, expect, it } from 'vitest'
import { AppThemeProvider } from './AppThemeProvider'
import { THEME_MODE_KEY } from '../utils/themeMode'

const ThemeProbe = () => {
    const { isDarkMode, setThemeMode, themeMode } = useThemeMode()

    return (
        <>
            <span data-testid="mode">{themeMode}</span>
            <span data-testid="dark">{String(isDarkMode)}</span>
            <button onClick={() => setThemeMode('dark')} type="button">go dark</button>
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
})

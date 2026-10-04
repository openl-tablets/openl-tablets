import React from 'react'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { CodeBlock, HighlightedCode } from './HighlightedCode'
import { useAppTheme } from '../../providers/AppThemeProvider'
import { renderInTheme } from '../../testing/theme'

const ThemePicker = () => {
    const { setThemeName } = useAppTheme()
    return <button onClick={() => setThemeName('solarized')} type="button">go solarized</button>
}

const renderCode = () => renderInTheme(
    <>
        <ThemePicker />
        <HighlightedCode code="public class Rules {}" language="java" />
    </>,
    { theme: 'dracula', mode: 'dark' }
)

const keyword = async (): Promise<HTMLElement> => {
    let found: HTMLElement | undefined
    await waitFor(() => {
        found = [...document.querySelectorAll<HTMLElement>('pre code span')].find(span => span.textContent === 'public')
        expect(found).toBeDefined()
    })
    return found as HTMLElement
}

describe('HighlightedCode in a theme', () => {
    it('draws the code in the colours the code editor of the theme in force gives it', async () => {
        const { container } = renderCode()

        expect(await keyword()).toHaveStyle({ color: '#ff79c6' })
        expect(container.querySelector('pre')).toHaveStyle({ backgroundColor: '#282a36', color: '#f8f8f2' })
    })

    it('draws a block of plain text in the colours of the code editor too', () => {
        const { container } = renderInTheme(<CodeBlock>plain text</CodeBlock>, { theme: 'dracula', mode: 'dark' })

        expect(container.querySelector('pre')).toHaveStyle({ backgroundColor: '#282a36', color: '#f8f8f2' })
    })

    it('repaints the code when another theme is picked', async () => {
        const { container } = renderCode()
        await keyword()

        await userEvent.click(screen.getByText('go solarized'))

        await waitFor(async () => expect(await keyword()).toHaveStyle({ color: '#859900' }))
        expect(container.querySelector('pre')).toHaveStyle({ backgroundColor: '#002b36' })
    })
})

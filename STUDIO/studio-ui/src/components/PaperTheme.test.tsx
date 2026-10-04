import { screen } from '@testing-library/react'
import { theme as antdTheme } from 'antd'
import { describe, expect, it } from 'vitest'
import { PaperTheme } from './PaperTheme'
import { renderInTheme } from '../testing/theme'

/** What an Ant Design control inside would be drawn with. */
const TokenProbe = () => {
    const { token } = antdTheme.useToken()
    return (
        <>
            <span data-testid="background">{token.colorBgContainer}</span>
            <span data-testid="text">{token.colorText}</span>
            <span data-testid="primary">{token.colorPrimary}</span>
            <span data-testid="height">{token.controlHeight}</span>
        </>
    )
}

const renderOnPaper = (compact = false) =>
    renderInTheme(<PaperTheme><TokenProbe /></PaperTheme>, { theme: 'dracula', mode: 'dark', compact })

describe('PaperTheme', () => {
    it('draws the controls inside it in Ant Design\'s own light appearance, whatever the theme', () => {
        const light = antdTheme.getDesignToken()

        renderOnPaper()

        expect(screen.getByTestId('background')).toHaveTextContent('#ffffff')
        expect(screen.getByTestId('text')).toHaveTextContent(light.colorText)
        // Not Dracula's purple: nothing of the theme in force reaches the paper.
        expect(screen.getByTestId('primary')).toHaveTextContent(light.colorPrimary)
    })

    it('keeps the density in force', () => {
        renderOnPaper()
        const comfortable = Number(screen.getByTestId('height').textContent)

        renderOnPaper(true)

        expect(Number(screen.getAllByTestId('height')[1]?.textContent)).toBeLessThan(comfortable)
    })
})

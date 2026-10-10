import { screen } from '@testing-library/react'
import { theme as antdTheme } from 'antd'
import { describe, expect, it } from 'vitest'
import { PaperTheme } from './PaperTheme'
import { renderInTheme, tokenFor } from '../testing/theme'

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

const renderOnPaper = (compact = false, excelFormatting = true) => renderInTheme(<PaperTheme><TokenProbe /></PaperTheme>,
    { theme: 'dracula', mode: 'dark', compact, excelFormatting })

describe('PaperTheme', () => {
    it('draws the controls inside it in the theme of the application, as the tables are drawn', () => {
        renderOnPaper(false, false)

        expect(screen.getByTestId('background')).toHaveTextContent(tokenFor('dracula', true).colorBgContainer)
        expect(screen.getByTestId('primary')).toHaveTextContent(tokenFor('dracula', true).colorPrimary)
    })

    it('draws the controls in Ant Design\'s own light appearance on the paper of an Excel file', () => {
        const light = antdTheme.getDesignToken()

        renderOnPaper()

        expect(screen.getByTestId('background')).toHaveTextContent('#ffffff')
        expect(screen.getByTestId('text')).toHaveTextContent(light.colorText)
        // Not Dracula's purple: nothing of the theme in force reaches the paper.
        expect(screen.getByTestId('primary')).toHaveTextContent(light.colorPrimary)
    })

    it('keeps the density in force on the paper of an Excel file', () => {
        renderOnPaper()
        const comfortable = Number(screen.getByTestId('height').textContent)

        renderOnPaper(true)

        expect(Number(screen.getAllByTestId('height')[1]?.textContent)).toBeLessThan(comfortable)
    })
})

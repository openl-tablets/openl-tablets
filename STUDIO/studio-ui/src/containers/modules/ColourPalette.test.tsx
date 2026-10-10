import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import '../../i18n'
import '../../locales'
import { colourIn, OFFICE_PALETTE } from '../../styles/tableColours'
import { renderInTheme } from '../../testing/theme'
import { ColourPalette } from './ColourPalette'

/** The theme colours as Excel lays them out in the columns of its palette. */
const COLUMNS = ['Background 1', 'Text 1', 'Background 2', 'Text 2',
    'Accent 1', 'Accent 2', 'Accent 3', 'Accent 4', 'Accent 5', 'Accent 6']

const draw = (look: Parameters<typeof renderInTheme>[1] = {}) => {
    const acted = { onPick: vi.fn(), onPreview: vi.fn() }
    renderInTheme(<ColourPalette {...acted} />, look)
    return acted
}

/** The swatch of a colour, by the name the palette of Excel gives it. */
const swatch = (name: string) => screen.getByTitle(name)

describe('ColourPalette', () => {
    it('offers the sixty colours of Excel: the theme colours, each with five shades under it', () => {
        draw()

        const swatches = screen.getAllByTestId('table-edit-swatch')
        expect(swatches).toHaveLength(60)
        expect(swatches.slice(0, 10).map(each => each.title)).toEqual(COLUMNS)
        expect(swatches.filter((_, at) => at % 10 === 4).map(each => each.title)).toEqual(['Accent 1',
            'Accent 1, Lighter 80%', 'Accent 1, Lighter 60%', 'Accent 1, Lighter 40%',
            'Accent 1, Darker 25%', 'Accent 1, Darker 50%'])
    })

    it('offers the shades Excel offers for white, black and a light grey', () => {
        draw()

        // White only darkens, and black only lightens.
        expect(swatch('Background 1, Darker 5%')).toBeInTheDocument()
        expect(swatch('Background 1, Darker 50%')).toBeInTheDocument()
        expect(swatch('Text 1, Lighter 50%')).toBeInTheDocument()
        expect(swatch('Text 1, Lighter 5%')).toBeInTheDocument()
        expect(swatch('Background 2, Darker 90%')).toBeInTheDocument()
        expect(swatch('Text 2, Lighter 80%')).toBeInTheDocument()
    })

    it('draws each colour in the colours of Office, as Excel draws it in a new workbook, whatever the theme', () => {
        draw({ theme: 'dracula', mode: 'dark', excelFormatting: true })

        expect(swatch('Accent 1')).toHaveStyle({ backgroundColor: OFFICE_PALETTE.accent1 })
        expect(swatch('Accent 2, Lighter 60%'))
            .toHaveStyle({ backgroundColor: colourIn(OFFICE_PALETTE, { name: 'accent2', tint: 0.6 }) })
        expect(swatch('Background 1, Darker 50%')).toHaveStyle({ backgroundColor: '#808080' })
    })

    it('shows the colour under the pointer and takes the one the reader settles on', async () => {
        const acted = draw()

        await userEvent.hover(swatch('Accent 1, Lighter 80%'))
        expect(acted.onPreview).toHaveBeenLastCalledWith({ name: 'accent1', tint: 0.8 })
        await userEvent.unhover(swatch('Accent 1, Lighter 80%'))
        expect(acted.onPreview).toHaveBeenLastCalledWith(null)

        await userEvent.click(swatch('Text 2'))
        expect(acted.onPick).toHaveBeenCalledWith({ name: 'dk2' })
    })
})

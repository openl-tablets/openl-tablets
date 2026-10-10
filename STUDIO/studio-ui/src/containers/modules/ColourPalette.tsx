import React from 'react'
import { useTranslation } from 'react-i18next'
import { colourIn, OFFICE_PALETTE } from 'styles/tableColours'
import type { ExcelThemeColorName, RawTableThemeColor } from 'types/tables'
import { useStyles } from './ColourPalette.styles'

/** The theme colours in the columns of the palette of Excel: the backgrounds and the texts, then the accents. */
const COLUMNS: readonly ExcelThemeColorName[] = ['lt1', 'dk1', 'lt2', 'dk2',
    'accent1', 'accent2', 'accent3', 'accent4', 'accent5', 'accent6']

/** The shades Excel offers under a theme colour of middle lightness, from the top: lighter, then darker. */
const MIDDLE_SHADES = [0.8, 0.6, 0.4, -0.25, -0.5]

/**
 * The five shades Excel offers under each theme colour of Office, from the top: how much lighter, above 0, or darker,
 * below 0. White only darkens and black only lightens.
 */
const SHADES: Readonly<Record<ExcelThemeColorName, readonly number[]>> = {
    lt1: [-0.05, -0.15, -0.25, -0.35, -0.5],
    dk1: [0.5, 0.35, 0.25, 0.15, 0.05],
    lt2: [-0.1, -0.25, -0.5, -0.75, -0.9],
    dk2: MIDDLE_SHADES,
    accent1: MIDDLE_SHADES,
    accent2: MIDDLE_SHADES,
    accent3: MIDDLE_SHADES,
    accent4: MIDDLE_SHADES,
    accent5: MIDDLE_SHADES,
    accent6: MIDDLE_SHADES,
}

/** The theme colours themselves, the top row of the palette. */
const THEME_COLOURS: readonly RawTableThemeColor[] = COLUMNS.map(name => ({ name }))

/** The rows of shades under the theme colours. */
const SHADE_ROWS: readonly (readonly RawTableThemeColor[])[] = MIDDLE_SHADES.map((_, shade) =>
    COLUMNS.map(name => ({ name, tint: SHADES[name][shade] as number })))

/** What tells a colour of the palette from the others: its theme colour and its tint. */
const keyOf = ({ name, tint = 0 }: RawTableThemeColor) => `${name}:${tint}`

interface ColourPaletteProps {
    /** The colour the reader settled on. */
    onPick: (colour: RawTableThemeColor) => void
    /** The colour the pointer is over, or null once it has left the palette. */
    onPreview: (colour: RawTableThemeColor | null) => void
}

/**
 * The **Theme Colors** of the palette of Excel: the ten theme colours of a workbook, each with five shades under it,
 * sixty colours in all.
 *
 * <p>A cell is coloured where the tables are shown in the formatting of their Excel files, so each colour is shown in
 * the colours of Office, as Excel shows it in a new workbook. The pointer resting on a colour paints the picked cell
 * with it, and taking the pointer away puts the cell back as it was: the reader sees the colour where it will stand
 * rather than on a swatch beside it.
 */
export const ColourPalette: React.FC<ColourPaletteProps> = ({ onPick, onPreview }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()

    /** The colour as the palette of Excel names it, such as Accent 1, Lighter 60%. */
    const nameOf = ({ name, tint }: RawTableThemeColor) => {
        const colour = t(`browser.module.excel_colours.${name}`)
        if (tint === undefined) {
            return colour
        }
        const percent = Math.round(Math.abs(tint) * 100)
        return t(tint > 0 ? 'browser.module.edit_lighter' : 'browser.module.edit_darker', { colour, percent })
    }

    const row = (colours: readonly RawTableThemeColor[]) => (
        <div key={colours.map(keyOf).join()} className={styles.row}>
            {colours.map(colour => (
                <button
                    key={keyOf(colour)}
                    className={styles.swatch}
                    data-testid="table-edit-swatch"
                    onClick={() => onPick(colour)}
                    // Pressing must not take the focus off the cell the colour is meant for.
                    onMouseDown={event => event.preventDefault()}
                    onMouseEnter={() => onPreview(colour)}
                    style={{ background: colourIn(OFFICE_PALETTE, colour) }}
                    title={nameOf(colour)}
                    type="button"
                />
            ))}
        </div>
    )

    return (
        <div
            className={styles.palette}
            data-testid="table-edit-palette"
            // The cell goes back to its own colour whichever way the pointer leaves: off the edge of the
            // palette, or straight out of the panel without crossing a swatch.
            onMouseLeave={() => onPreview(null)}
        >
            {row(THEME_COLOURS)}
            <div className={styles.shades}>
                {SHADE_ROWS.map(row)}
            </div>
        </div>
    )
}

export default ColourPalette

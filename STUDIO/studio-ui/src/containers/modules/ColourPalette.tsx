import React from 'react'
import { useTranslation } from 'react-i18next'
import { hexOf, STANDARD_COLOURS } from 'styles/excelColours'
import type { ExcelThemeColorName, RawCellColor } from 'types/tables'
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
const THEME_COLOURS: readonly RawCellColor[] = COLUMNS.map(theme => ({ theme }))

/** The rows of shades under the theme colours. */
const SHADE_ROWS: readonly (readonly RawCellColor[])[] = MIDDLE_SHADES.map((_, shade) =>
    COLUMNS.map(theme => ({ theme, tint: SHADES[theme][shade] as number })))

/** The standard colours, the bottom row of the palette. */
const STANDARD_ROW: readonly RawCellColor[] = STANDARD_COLOURS.map(({ rgb }) => ({ rgb }))

/** What tells a colour of the palette from the others. */
const keyOf = (colour: RawCellColor) => ('rgb' in colour ? colour.rgb : `${colour.theme}:${colour.tint ?? 0}`)

interface ColourPaletteProps {
    /** The colour the reader settled on. */
    onPick: (colour: RawCellColor) => void
    /** The colour the pointer is over, or null once it has left the palette. */
    onPreview: (colour: RawCellColor | null) => void
}

/**
 * The palette of Excel, and nothing else: its **Theme Colors**, the ten theme colours of a workbook each with five
 * shades under it, and its ten **Standard Colors**.
 *
 * <p>A theme colour is written as the theme colour itself, so the cell takes the colours of the theme of its workbook.
 * Each is shown in the colours of Office, as Excel shows it in a new workbook. The pointer resting on a colour paints
 * the picked cell with it, and taking the pointer away puts the cell back as it was: the reader sees the colour where
 * it will stand rather than on a swatch beside it.
 */
export const ColourPalette: React.FC<ColourPaletteProps> = ({ onPick, onPreview }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()

    /** The colour as the palette of Excel names it, such as Accent 1, Lighter 60%. */
    const nameOf = (colour: RawCellColor) => {
        if ('rgb' in colour) {
            const standard = STANDARD_COLOURS.find(({ rgb }) => rgb === colour.rgb)
            return t(`browser.module.standard_colours.${standard?.name}`)
        }
        const name = t(`browser.module.excel_colours.${colour.theme}`)
        if (colour.tint === undefined) {
            return name
        }
        const percent = Math.round(Math.abs(colour.tint) * 100)
        return t(colour.tint > 0 ? 'browser.module.edit_lighter' : 'browser.module.edit_darker',
            { colour: name, percent })
    }

    const row = (colours: readonly RawCellColor[]) => (
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
                    style={{ background: hexOf(colour) }}
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
            <div className={styles.heading}>{t('browser.module.edit_theme_colours')}</div>
            {row(THEME_COLOURS)}
            <div className={styles.shades}>
                {SHADE_ROWS.map(row)}
            </div>
            <div className={styles.heading}>{t('browser.module.edit_standard_colours')}</div>
            {row(STANDARD_ROW)}
        </div>
    )
}

export default ColourPalette

import React from 'react'
import { useTranslation } from 'react-i18next'
import { useStyles } from './ColourPalette.styles'

/**
 * The colours the Editor offered, in the rows it laid them out in: a column of greys and nine of hues, each
 * going from pale at the top to dark at the bottom.
 */
const PALETTE = [
    ['#FFFFFF', '#FFDDDD', '#DDFFDD', '#DDDDFF', '#FFFFAA', '#FFE4BE', '#FFDDFF', '#FFD6AF', '#E5F5FF', '#DBFFEB'],
    ['#EEEEEE', '#FFAAAA', '#AAFFAA', '#AAAAFF', '#FFFF77', '#FFC29C', '#FFBBFF', '#FFBF80', '#D2EDFF', '#C2FFDD'],
    ['#CCCCCC', '#FF6666', '#66FF66', '#6666FF', '#FFFF33', '#FF9275', '#F19CEC', '#FAA857', '#AEDEFE', '#8FFFC0'],
    ['#999999', '#FF3333', '#45F745', '#3333FF', '#FFFF00', '#FF7256', '#CD69C9', '#FF7F00', '#87CEFF', '#54FF9F'],
    ['#666666', '#EA0B0B', '#25DA25', '#2222D3', '#EEEE00', '#EE6A50', '#B23AEE', '#EE7600', '#7EC0EE', '#4EEE94'],
    ['#333333', '#AA0000', '#00AA00', '#1717AB', '#CDCD00', '#CD5B45', '#9A32CD', '#CD6600', '#6CA6CD', '#43CD80'],
    ['#000000', '#660000', '#006600', '#000066', '#8B8B00', '#8B3E2F', '#68228B', '#8B4500', '#4A708B', '#2E8B57'],
]

interface ColourPaletteProps {
    /** The colour the reader settled on. */
    onPick: (colour: string) => void
    /** The colour the pointer is over, or null once it has left the palette. */
    onPreview: (colour: string | null) => void
}

/**
 * The Editor's palette, which shows a colour on the cell before it is chosen.
 *
 * <p>The pointer resting on a colour paints the picked cell with it, and taking the pointer away puts the cell
 * back as it was: the reader sees the colour where it will stand rather than on a swatch beside it.
 */
export const ColourPalette: React.FC<ColourPaletteProps> = ({ onPick, onPreview }) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()

    return (
        <div
            className={styles.palette}
            data-testid="table-edit-palette"
            // The cell goes back to its own colour whichever way the pointer leaves: off the edge of the
            // palette, or straight out of the panel without crossing a swatch.
            onMouseLeave={() => onPreview(null)}
        >
            {PALETTE.map(row => (
                <div key={row.join()} className={styles.row}>
                    {row.map(colour => (
                        <button
                            key={colour}
                            className={styles.swatch}
                            data-testid="table-edit-swatch"
                            onClick={() => onPick(colour)}
                            // Pressing must not take the focus off the cell the colour is meant for.
                            onMouseDown={event => event.preventDefault()}
                            onMouseEnter={() => onPreview(colour)}
                            style={{ background: colour }}
                            title={t('browser.module.edit_colour', { colour })}
                            type="button"
                        />
                    ))}
                </div>
            ))}
        </div>
    )
}

export default ColourPalette

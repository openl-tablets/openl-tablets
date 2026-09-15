import React, { useState } from 'react'
import { Button, ColorPicker, Tooltip } from 'antd'
import type { AggregationColor } from 'antd/es/color-picker/color'
import { useTranslation } from 'react-i18next'
import { ColourPalette } from './ColourPalette'

interface CellColourPickerProps {
    /** What the button is for, shown as its tooltip. */
    title: string
    icon: React.ReactNode
    /** The colour the cell carries now, which the picker opens on. */
    value: string
    disabled: boolean
    /** The colour the reader settled on. */
    onPick: (colour: string) => void
    /** The colour the pointer is over, or null once it has left the palette. */
    onPreview: (colour: string | null) => void
    testId: string
    className: string
}

/** The colour as the API writes it: #rrggbb, without whatever the picker says about opacity. */
const colour = (chosen: AggregationColor) => chosen.toHexString().slice(0, 7)

/**
 * A colour for the picked cell, chosen from the palette the Editor offered.
 *
 * <p>The palette is what opens, and it shows each colour on the cell itself while the pointer rests on it. A
 * colour outside it is a press away: More Colours puts the full picker in the palette's place, rather than
 * beside it, so only one of the two is ever on screen.
 */
export const CellColourPicker: React.FC<CellColourPickerProps> = ({
    title,
    icon,
    value,
    disabled,
    onPick,
    onPreview,
    testId,
    className,
}) => {
    const { t } = useTranslation('repository')
    const [open, setOpen] = useState(false)
    const [more, setMore] = useState(false)

    /**
     * Whether the picker is open, which is the picker's own to say.
     *
     * <p>It closes itself when the reader clicks away from it, and is closed here only when a colour is taken
     * from the palette. Answering its own trigger as well would fight it: a press would close it and reopen it
     * in the same click.
     */
    const opened = (isOpen: boolean) => {
        setOpen(isOpen)
        if (!isOpen) {
            setMore(false)
            onPreview(null)
        }
    }

    return (
        <Tooltip title={title}>
            <span>
                <ColorPicker
                    disabled={disabled}
                    format="hex"
                    onOpenChange={opened}
                    open={open}
                    value={value}
                    onChangeComplete={chosen => {
                        onPreview(null)
                        onPick(colour(chosen))
                    }}
                    panelRender={panel => (more ? panel : (
                        <>
                            <ColourPalette
                                onPreview={onPreview}
                                onPick={chosen => {
                                    opened(false)
                                    onPick(chosen)
                                }}
                            />
                            <Button
                                block
                                data-testid={`${testId}-more`}
                                onClick={() => setMore(true)}
                                // Pressing must not take the focus off the cell the colour is meant for.
                                onMouseDown={event => event.preventDefault()}
                                size="small"
                                type="link"
                            >
                                {t('browser.module.edit_more_colours')}
                            </Button>
                        </>
                    ))}
                >
                    <Button
                        className={className}
                        data-testid={testId}
                        disabled={disabled}
                        icon={icon}
                        size="small"
                        type="text"
                    />
                </ColorPicker>
            </span>
        </Tooltip>
    )
}

export default CellColourPicker

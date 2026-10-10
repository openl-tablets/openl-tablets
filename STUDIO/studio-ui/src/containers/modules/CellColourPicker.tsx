import React, { useState } from 'react'
import { Button, Popover, Tooltip } from 'antd'
import type { RawTableThemeColor } from 'types/tables'
import { ColourPalette } from './ColourPalette'

interface CellColourPickerProps {
    /** What the button is for, shown as its tooltip. */
    title: string
    icon: React.ReactNode
    disabled: boolean
    /** The colour the reader settled on. */
    onPick: (colour: RawTableThemeColor) => void
    /** The colour the pointer is over, or null once it has left the palette. */
    onPreview: (colour: RawTableThemeColor | null) => void
    testId: string
    className: string
}

/**
 * A colour for the picked cell, one of the sixty colours the palette of Excel offers for the theme of a workbook.
 *
 * <p>The palette opens under the button, and it shows each colour on the cell itself while the pointer rests on it.
 */
export const CellColourPicker: React.FC<CellColourPickerProps> = ({
    title,
    icon,
    disabled,
    onPick,
    onPreview,
    testId,
    className,
}) => {
    const [open, setOpen] = useState(false)

    /**
     * Whether the palette is open, which the popover says.
     *
     * <p>It closes itself when the reader clicks away from it, and is closed here only when a colour is taken from
     * the palette. Answering its own trigger as well would fight it: a press would close it and reopen it in the
     * same click.
     */
    const opened = (isOpen: boolean) => {
        setOpen(isOpen)
        if (!isOpen) {
            onPreview(null)
        }
    }

    const palette = (
        <ColourPalette
            onPreview={onPreview}
            onPick={chosen => {
                opened(false)
                onPick(chosen)
            }}
        />
    )

    return (
        <Tooltip title={title}>
            <span>
                <Popover
                    arrow={false}
                    content={palette}
                    onOpenChange={opened}
                    open={open && !disabled}
                    placement="bottomLeft"
                    trigger="click"
                >
                    <Button
                        className={className}
                        data-testid={testId}
                        disabled={disabled}
                        icon={icon}
                        size="small"
                        type="text"
                    />
                </Popover>
            </span>
        </Tooltip>
    )
}

export default CellColourPicker

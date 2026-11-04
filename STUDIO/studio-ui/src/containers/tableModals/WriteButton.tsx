import React from 'react'
import { Button } from 'antd'
import { SplitButton } from 'components/SplitButton'
import type { Destination, ModuleFormat } from './shared'

interface WriteButtonProps {
    destination: Destination
    disabled: boolean
    loading: boolean
    label: string
    /** What the button says when the table is written as a Markdown file. */
    markdownLabel: string
    /** Prefix of the test ids of the arrow and of its menu entry. */
    testId: string
    /** Writes the table; a format is given only for a new module of Markdown files. */
    onWrite: (format?: ModuleFormat) => void
}

/**
 * The button a dialog writes its table with, named after the format the table is written in.
 *
 * <p>A module the project has takes the table in its own format: a workbook on a sheet, a folder of Markdown files
 * as a file, which the button names. A new module is a workbook, or a folder of Markdown files, which the menu of the
 * button offers.
 */
export const WriteButton = ({
    destination,
    disabled,
    loading,
    label,
    markdownLabel,
    testId,
    onWrite,
}: WriteButtonProps) => {
    const button = { disabled, loading, type: 'primary' as const, onClick: () => onWrite() }
    if (destination === 'workbook') {
        return <Button {...button}>{label}</Button>
    }
    if (destination === 'markdown') {
        return <Button {...button}>{markdownLabel}</Button>
    }
    return (
        <SplitButton
            {...button}
            arrowLabel={markdownLabel}
            arrowTestId={`${testId}-more`}
            menu={{
                items: [{ key: 'markdown', label: <span data-testid={`${testId}-markdown`}>{markdownLabel}</span> }],
                onClick: () => onWrite('markdown'),
            }}
        >
            {label}
        </SplitButton>
    )
}

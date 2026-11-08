import React from 'react'
import { Descriptions, theme, Typography } from 'antd'
import type { BundleLanguage, KeyRow } from './bundle'
import { LanguageTitle } from './LanguageTitle'
import { MessageCell } from './MessageCell'
import type { MessageEdits } from './useMessageEdits'
import { useStyles as useTableEditorStyles } from '../modules/TableEditor.styles'

/** The width of the names of the languages, beside their texts. */
const LANGUAGE_WIDTH = 220

export interface KeyPanelProps {
    row: KeyRow
    languages: readonly BundleLanguage[]
    /** The language a link sent the reader to, marked among the others. */
    askedLanguage: string | null
    edits: MessageEdits
    canWrite: boolean
    nameOf: (tag: string) => string
}

/**
 * One key of the messages with its text in every language, a row per language, each written in place.
 *
 * <p>A text changed and not saved is marked as the table of a module marks a cell written and not saved, and the
 * language a link sent the reader to as an item picked in a list.
 */
export const KeyPanel = ({ row, languages, askedLanguage, edits, canWrite, nameOf }: KeyPanelProps) => {
    const { styles: table } = useTableEditorStyles(false)
    const { token } = theme.useToken()
    const asked = { background: token.controlItemBgActive }
    return (
        <Descriptions
            bordered
            column={1}
            data-testid="localization-key"
            size="small"
            styles={{ label: { width: LANGUAGE_WIDTH } }}
            title={<Typography.Text copyable strong>{row.key}</Typography.Text>}
            items={languages.map(language => ({
                key: language.tag,
                label: <LanguageTitle name={nameOf(language.tag)} tag={language.tag} testId="localization-key-language" />,
                ...(edits.changed(row.key, language.tag) ? { classNames: { content: table.touched } } : {}),
                ...(language.tag === askedLanguage ? { styles: { label: asked, content: asked } } : {}),
                children: (
                    <MessageCell
                        // A cell of another key starts afresh, its draft left behind.
                        key={row.key}
                        listWhole
                        canWrite={canWrite}
                        edits={edits}
                        language={language}
                        nameOf={nameOf}
                        row={row}
                    />
                ),
            }))}
        />
    )
}

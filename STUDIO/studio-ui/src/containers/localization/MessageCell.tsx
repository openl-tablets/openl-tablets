import React, { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Flex, Input, Tooltip, Typography } from 'antd'
import { RollbackOutlined, WarningOutlined } from '@ant-design/icons'
import { cellOf, type BundleLanguage, type KeyRow } from './bundle'
import { useStyles } from './LocalizationWorkspace.styles'
import { warningsAt } from './messageWarnings'
import { MessageText } from './MessageText'
import { ProblemLines } from './ProblemLines'
import { ProblemList } from './ProblemList'
import type { MessageEdits } from './useMessageEdits'

interface MessageCellProps {
    /** The key, as read. */
    row: KeyRow
    language: BundleLanguage
    edits: MessageEdits
    canWrite: boolean
    nameOf: (tag: string) => string
    /**
     * Lists under the text the warnings about it as a whole, where there is room for them: they have no piece of the
     * text to be told on. A cell of the table tells them by its mark alone.
     */
    listWhole?: boolean | undefined
}

/**
 * The text of a key in a language, written in place.
 *
 * <p>A click, or Enter or Space on the focused cell, opens the text for writing. Enter keeps it, Shift+Enter starts a new line, Escape gives it up, and
 * leaving the cell keeps it. What is being written lives in the cell, so a keystroke draws no other cell.
 *
 * <p>A text opened in a cell that shows an inherited one starts empty: keeping it empty changes nothing, and the
 * language goes on inheriting. A language other than the default one can give its own text up for the inherited
 * one, by the action beside the field.
 *
 * <p>What the check of the files warned about a piece of the text is told when the piece is pointed at, so the texts
 * stay readable. A warning about the text as a whole, such as a key written twice or a line without a text, has no
 * piece to be told on: it is listed under the text where there is room, and is told by the mark in a cell of the
 * table. The mark at the end of the cell tells everything. While the text is written, all its warnings are listed
 * under it the way the problems of a table are, each with the piece of the text it is about; reading them leaves the
 * text open. The warnings are about the saved text: a text changed and not saved yet shows none, until it is saved.
 *
 * <p>The place the cell is drawn in marks a text changed and not saved, as the table of a module marks a cell.
 */
export const MessageCell = ({
    row,
    language,
    edits,
    canWrite,
    nameOf,
    listWhole = false,
}: MessageCellProps) => {
    const { t } = useTranslation('localization')
    const { styles } = useStyles()
    const texts = edits.textsOf(row.key, row.base) ?? {}
    const own = texts[language.tag]
    const [draft, setDraft] = useState<string | null>(null)
    const changed = edits.changed(row.key, language.tag)
    const problems = changed ? [] : warningsAt(row.warnings, language.tag)
    // A warning about the text as a whole has no piece of it to be told on, and a line without a text has nothing to
    // point at at all: it stays in view.
    const unspotted = listWhole ? problems.filter(problem => problem.spots.length === 0) : []

    const keep = () => {
        if (draft !== null && draft !== (own ?? '')) {
            edits.setText(row.key, row.base, language.tag, draft)
        }
        setDraft(null)
    }
    // A row scrolled out of the virtual table takes its cell away, and a browser may drop the field without telling
    // that it lost the focus: the text written so far is kept then too.
    const keepOnLeave = useRef(keep)
    useEffect(() => {
        keepOnLeave.current = keep
    })
    useEffect(() => () => keepOnLeave.current(), [])

    const open = () => setDraft(own ?? '')
    const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
        // The Enter that ends the composition of a character, as of a Chinese or a Japanese one, keeps nothing.
        if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) {
            event.preventDefault()
            keep()
        } else if (event.key === 'Escape') {
            setDraft(null)
        }
    }

    if (draft === null) {
        return (
            <Flex
                vertical
                className={styles.cell}
                data-testid="localization-cell"
                gap="small"
                {...(canWrite ? {
                    role: 'button',
                    tabIndex: 0,
                    onClick: open,
                    onKeyDown: (event: React.KeyboardEvent) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault()
                            open()
                        }
                    },
                } : {})}
            >
                <Flex align="flex-start" gap="small" justify="space-between">
                    <MessageText
                        cell={cellOf({ texts, shared: row.shared }, language)}
                        nameOf={nameOf}
                        warnings={problems}
                    />
                    {problems.length > 0 && (
                        <Tooltip title={<ProblemList problems={problems} />}>
                            <Typography.Text type="warning">
                                <WarningOutlined data-testid="localization-cell-problem" />
                            </Typography.Text>
                        </Tooltip>
                    )}
                </Flex>
                {unspotted.length > 0 && <ProblemLines problems={unspotted} text={own ?? ''} />}
            </Flex>
        )
    }
    return (
        <Flex vertical gap="small">
            <Flex align="flex-start" gap="small">
                <Input.TextArea
                    autoFocus
                    autoSize
                    data-testid="localization-cell-input"
                    onBlur={keep}
                    onChange={event => setDraft(event.target.value)}
                    onKeyDown={onKeyDown}
                    size="small"
                    // The field takes the width the cell leaves beside its action.
                    style={{ flex: 1 }}
                    value={draft}
                    // The caret goes after the text, where the reader goes on writing it.
                    onFocus={event => {
                        const end = event.currentTarget.value.length
                        event.currentTarget.setSelectionRange(end, end)
                    }}
                />
                {language.tag !== '' && own !== undefined && (
                    <Tooltip title={t('use_inherited')}>
                        <Button
                            aria-label={t('use_inherited')}
                            data-testid="localization-cell-inherit"
                            icon={<RollbackOutlined />}
                            size="small"
                            type="text"
                            // Pressed before the field loses the focus, so leaving it does not keep the text first.
                            onMouseDown={event => {
                                event.preventDefault()
                                edits.removeText(row.key, row.base, language.tag)
                                setDraft(null)
                            }}
                        />
                    </Tooltip>
                )}
            </Flex>
            {problems.length > 0 && <ProblemLines problems={problems} text={own ?? ''} />}
        </Flex>
    )
}

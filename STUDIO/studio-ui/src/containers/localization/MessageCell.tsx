import React, { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input } from 'antd'
import type { Bundle, BundleLanguage } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { MessageText } from './MessageText'
import type { MessageDrafts } from './useMessageDrafts'

interface MessageCellProps {
    bundle: Bundle
    language: BundleLanguage
    messageKey: string
    drafts: MessageDrafts
    canWrite: boolean
    styles: LocalizationStyles
    nameOf: (tag: string) => string
}

/**
 * The text of a key in a language, written in place.
 *
 * <p>A click opens the text for writing. Enter keeps it, Shift+Enter starts a new line, Escape gives it up, and
 * leaving the cell keeps it. What is being written lives in the cell, so a keystroke draws no other cell.
 *
 * <p>A text opened in a cell that shows an inherited one starts empty: keeping it empty changes nothing, and the
 * language goes on inheriting. A language other than the default one can give its own text up for the inherited
 * one.
 */
export const MessageCell = ({ bundle, language, messageKey, drafts, canWrite, styles, nameOf }: MessageCellProps) => {
    const { t } = useTranslation('localization')
    const own = drafts.textOf(language, messageKey)
    const [draft, setDraft] = useState<string | null>(null)

    const keep = () => {
        if (draft !== null && draft !== (own ?? '')) {
            drafts.write(language, messageKey, draft)
        }
        setDraft(null)
    }
    const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault()
            keep()
        } else if (event.key === 'Escape') {
            setDraft(null)
        }
    }

    if (draft === null) {
        return (
            <div
                className={drafts.changed(language.tag, messageKey) ? styles.changed : styles.cell}
                data-testid="localization-cell"
                onClick={canWrite ? () => setDraft(own ?? '') : undefined}
            >
                <MessageText
                    bundle={bundle}
                    language={language}
                    messageKey={messageKey}
                    nameOf={nameOf}
                    styles={styles}
                    textOf={drafts.textOf}
                />
            </div>
        )
    }
    return (
        <div className={styles.cell}>
            <Input.TextArea
                autoFocus
                autoSize
                data-testid="localization-cell-input"
                onBlur={keep}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={onKeyDown}
                size="small"
                value={draft}
            />
            {language.tag !== '' && own !== undefined && (
                <Button
                    className={styles.inherit}
                    data-testid="localization-cell-inherit"
                    size="small"
                    type="link"
                    // Pressed before the field loses the focus, so leaving it does not keep the text first.
                    onMouseDown={event => {
                        event.preventDefault()
                        drafts.write(language, messageKey, null)
                        setDraft(null)
                    }}
                >
                    {t('use_inherited')}
                </Button>
            )}
        </div>
    )
}

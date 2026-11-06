import React from 'react'
import { Typography } from 'antd'
import type { Bundle } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { MessageText, type TextOf } from './MessageText'

interface KeyPanelProps {
    bundle: Bundle
    messageKey: string
    /** The language a link sent the reader to, marked among the others. */
    askedLanguage: string | null
    textOf: TextOf
    nameOf: (tag: string) => string
    styles: LocalizationStyles
}

/** One key of the messages with its text in every language, a row per language. */
export const KeyPanel = ({ bundle, messageKey, askedLanguage, textOf, nameOf, styles }: KeyPanelProps) => (
    <div className={styles.keyPanel} data-testid="localization-key">
        <div className={styles.keyHead}>
            <Typography.Text copyable strong className={styles.key}>{messageKey}</Typography.Text>
        </div>
        {bundle.languages.map(language => (
            <div
                key={language.tag}
                data-testid="localization-key-language"
                className={language.tag === askedLanguage
                    ? `${styles.languageRow} ${styles.languageRowAsked}`
                    : styles.languageRow}
            >
                <div className={styles.languageName}>
                    {nameOf(language.tag)}
                    <span className={styles.columnFile}>{language.file.name}</span>
                </div>
                <div className={styles.cell} data-testid="localization-cell">
                    <MessageText
                        bundle={bundle}
                        language={language}
                        messageKey={messageKey}
                        nameOf={nameOf}
                        styles={styles}
                        textOf={textOf}
                    />
                </div>
            </div>
        ))}
    </div>
)

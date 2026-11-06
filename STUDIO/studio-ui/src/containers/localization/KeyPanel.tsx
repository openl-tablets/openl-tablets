import React from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Typography } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import type { Bundle } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { MessageCell } from './MessageCell'
import type { MessageDrafts } from './useMessageDrafts'

interface KeyPanelProps {
    bundle: Bundle
    messageKey: string
    /** The language a link sent the reader to, marked among the others. */
    askedLanguage: string | null
    drafts: MessageDrafts
    canWrite: boolean
    onRemoveKey: (key: string) => void
    nameOf: (tag: string) => string
    styles: LocalizationStyles
}

/** One key of the messages with its text in every language, a row per language, each written in place. */
export const KeyPanel = ({
    bundle,
    messageKey,
    askedLanguage,
    drafts,
    canWrite,
    onRemoveKey,
    nameOf,
    styles,
}: KeyPanelProps) => {
    const { t } = useTranslation('localization')
    return (
        <div className={styles.keyPanel} data-testid="localization-key">
            <div className={styles.keyHead}>
                <Typography.Text copyable strong className={styles.key}>{messageKey}</Typography.Text>
                {canWrite && (
                    <Button
                        data-testid="localization-key-remove"
                        icon={<DeleteOutlined />}
                        onClick={() => onRemoveKey(messageKey)}
                        size="small"
                        type="text"
                    >
                        {t('remove_key')}
                    </Button>
                )}
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
                    <MessageCell
                        // A cell of another key starts afresh, its draft left behind.
                        key={messageKey}
                        bundle={bundle}
                        canWrite={canWrite}
                        drafts={drafts}
                        language={language}
                        messageKey={messageKey}
                        nameOf={nameOf}
                        styles={styles}
                    />
                </div>
            ))}
        </div>
    )
}

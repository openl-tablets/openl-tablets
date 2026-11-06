import React from 'react'
import { useTranslation } from 'react-i18next'
import { cellOf, type Bundle, type BundleLanguage, type TextOf } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'

interface MessageTextProps {
    bundle: Bundle
    language: BundleLanguage
    messageKey: string
    textOf: TextOf
    styles: LocalizationStyles
    nameOf: (tag: string) => string
}

/** Draws what a language shows for a key: its own text, an inherited one, an empty text, or nothing at all. */
export const MessageText = ({ bundle, language, messageKey, textOf, styles, nameOf }: MessageTextProps) => {
    const { t } = useTranslation('localization')
    const cell = cellOf(bundle, language, messageKey, textOf)
    switch (cell.kind) {
        case 'own':
            return <span className={styles.own}>{cell.text}</span>
        case 'empty':
            return <span className={styles.empty}>{t('empty')}</span>
        case 'inherited':
            return (
                <span className={styles.inherited}>
                    {cell.text === '' ? t('empty') : cell.text}
                    <span className={styles.origin}>
                        {cell.project === undefined
                            ? t('inherited_from', { language: nameOf(cell.from) })
                            : t('inherited_from_project', { language: nameOf(cell.from), project: cell.project.name })}
                    </span>
                </span>
            )
        case 'missing':
            return <span className={styles.missing}>{t('missing')}</span>
    }
}

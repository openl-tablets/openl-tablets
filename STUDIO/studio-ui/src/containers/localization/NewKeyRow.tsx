import React, { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Input, Table, Tooltip } from 'antd'
import type { InputRef } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { BundleLanguage } from './bundle'

interface NewKeyRowProps {
    languages: readonly BundleLanguage[]
    /** Whether the messages hold a key already, which a new one may not repeat. */
    exists: (key: string) => Promise<boolean>
    /** Adds the key with its texts by the language: the default one always, another one when it was written. */
    onAdd: (key: string, texts: Record<string, string>) => void
    /** Whether the table has a column of the actions after the languages. */
    withActions: boolean
    /** Brings the column of a language into sight once its text takes the focus, by its place among the languages. */
    onFocusLanguage: (at: number) => void
}

/**
 * The last row of the table, under the keys: a new key and its text in every language, added with Enter.
 *
 * <p>Enter in the key goes on to the default text, Tab from text to text across the languages, and Enter in any text
 * adds the key. A language left empty takes the text it falls back to; the default one is written even empty. The
 * row is then emptied and the key takes the focus again, so the reader adds one key after another without leaving the
 * keyboard.
 *
 * <p>An empty key, or one the messages have already, is not added: the reason is shown at the key.
 */
export const NewKeyRow = ({ languages, exists, onAdd, withActions, onFocusLanguage }: NewKeyRowProps) => {
    const { t } = useTranslation('localization')
    const keyInput = useRef<InputRef>(null)
    const defaultInput = useRef<InputRef>(null)
    const [key, setKey] = useState('')
    const [texts, setTexts] = useState<Record<string, string>>({})
    const [problem, setProblem] = useState<string | null>(null)
    const [checking, setChecking] = useState(false)

    const add = async () => {
        if (checking) {
            return
        }
        const trimmed = key.trim()
        if (trimmed === '') {
            setProblem(t('key_required'))
            keyInput.current?.focus()
            return
        }
        setChecking(true)
        try {
            if (await exists(trimmed)) {
                setProblem(t('key_exists'))
                keyInput.current?.focus()
                return
            }
        } finally {
            setChecking(false)
        }
        const written = Object.fromEntries(Object.entries(texts).filter(([tag, text]) => tag !== '' && text !== ''))
        onAdd(trimmed, { '': texts[''] ?? '', ...written })
        setKey('')
        setTexts({})
        keyInput.current?.focus()
    }

    return (
        <Table.Summary.Row data-testid="localization-new-key-row">
            <Table.Summary.Cell index={0}>
                <Tooltip open={problem !== null} placement="topLeft" title={problem}>
                    <Input
                        ref={keyInput}
                        data-testid="localization-new-key"
                        onBlur={() => setProblem(null)}
                        onPressEnter={() => defaultInput.current?.focus()}
                        placeholder={t('new_key')}
                        status={problem === null ? '' : 'error'}
                        value={key}
                        variant="borderless"
                        onChange={event => {
                            setKey(event.target.value)
                            setProblem(null)
                        }}
                    />
                </Tooltip>
            </Table.Summary.Cell>
            {languages.map((language, at) => (
                <Table.Summary.Cell key={language.tag} index={at + 1}>
                    <Input
                        ref={language.tag === '' ? defaultInput : undefined}
                        data-testid={language.tag === '' ? 'localization-new-key-text' : `localization-new-key-text-${language.tag}`}
                        onFocus={() => onFocusLanguage(at)}
                        onPressEnter={() => void add()}
                        placeholder={language.tag === '' ? t('default_text') : t('falls_back_when_empty')}
                        value={texts[language.tag] ?? ''}
                        variant="borderless"
                        onChange={event => {
                            const text = event.target.value
                            setTexts(current => ({ ...current, [language.tag]: text }))
                        }}
                    />
                </Table.Summary.Cell>
            ))}
            {withActions && (
                <Table.Summary.Cell index={languages.length + 1}>
                    <Tooltip title={t('add_key')}>
                        <Button
                            aria-label={t('add_key')}
                            data-testid="localization-new-key-add"
                            icon={<PlusOutlined />}
                            loading={checking}
                            onClick={() => void add()}
                            size="small"
                            type="text"
                        />
                    </Tooltip>
                </Table.Summary.Cell>
            )}
        </Table.Summary.Row>
    )
}

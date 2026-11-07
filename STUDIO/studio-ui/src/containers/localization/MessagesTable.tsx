import React, { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Table, Tooltip, Typography } from 'antd'
import type { TableProps } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import type { Bundle } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { MessageCell } from './MessageCell'
import type { ProblemIndex } from './messageProblems'
import type { MessageDrafts } from './useMessageDrafts'

/** The widths the columns are laid out at; the table scrolls sideways past them. */
const KEY_WIDTH = 260
const LANGUAGE_WIDTH = 280
const ACTIONS_WIDTH = 48
/** The height of the header row, taken off the height the rows scroll in. */
const HEADER_HEIGHT = 56

/** One row of the table: a key of the messages. */
interface Row {
    key: string
}

interface MessagesTableProps {
    bundle: Bundle
    keys: string[]
    drafts: MessageDrafts
    canWrite: boolean
    onRemoveKey: (key: string) => void
    onAddLanguage: () => void
    /** The problems of the keys, which each column counts for its language in its head. */
    problems: ProblemIndex
    /** Opens a key on its own, with its text in every language. */
    onOpenKey: (key: string) => void
    nameOf: (tag: string) => string
    styles: LocalizationStyles
    /** The height the table is given, its header included. */
    height: number
}

/**
 * All the messages as one table: a row per key, a column per language.
 *
 * <p>The rows are drawn a screenful at a time, as bundles reach thousands of keys. A text is written in its cell;
 * the name of a key opens it on its own.
 */
export const MessagesTable = ({
    bundle,
    keys,
    drafts,
    canWrite,
    onRemoveKey,
    onAddLanguage,
    problems,
    onOpenKey,
    nameOf,
    styles,
    height,
}: MessagesTableProps) => {
    const { t } = useTranslation('localization')
    const rows = useMemo<Row[]>(() => keys.map(key => ({ key })), [keys])

    const columns = useMemo<NonNullable<TableProps<Row>['columns']>>(() => [
        {
            title: t('key'),
            dataIndex: 'key',
            key: 'key',
            fixed: 'left',
            width: KEY_WIDTH,
            render: (key: string) => (
                <Typography.Link className={styles.key} data-testid="localization-open-key" onClick={() => onOpenKey(key)}>
                    {key}
                </Typography.Link>
            ),
        },
        ...bundle.languages.map(language => ({
            key: `language:${language.tag}`,
            width: LANGUAGE_WIDTH,
            title: (
                <>
                    {nameOf(language.tag)}
                    {(problems.byLanguage.get(language.tag) ?? 0) > 0 && (
                        <Tooltip title={t('problem_count', { count: problems.byLanguage.get(language.tag) })}>
                            <span className={styles.warningCount} data-testid="localization-problem-count">
                                {problems.byLanguage.get(language.tag)}
                            </span>
                        </Tooltip>
                    )}
                    <span className={styles.columnFile}>{language.file.name}</span>
                </>
            ),
            render: (_: unknown, row: Row) => (
                <MessageCell
                    bundle={bundle}
                    canWrite={canWrite}
                    drafts={drafts}
                    language={language}
                    messageKey={row.key}
                    nameOf={nameOf}
                    styles={styles}
                />
            ),
        })),
        // The column after the last language adds a language, and in every row removes the key of the row; it
        // stays in sight however far the languages are scrolled.
        ...(canWrite ? [{
            key: 'actions',
            fixed: 'right' as const,
            width: ACTIONS_WIDTH,
            title: (
                <Tooltip title={t('add_language')}>
                    <Button
                        aria-label={t('add_language')}
                        data-testid="localization-add-language"
                        icon={<PlusOutlined />}
                        onClick={onAddLanguage}
                        size="small"
                    />
                </Tooltip>
            ),
            render: (_: unknown, row: Row) => (
                <Tooltip title={t('remove_key')}>
                    <Button
                        aria-label={t('remove_key')}
                        data-testid="localization-remove-key"
                        icon={<DeleteOutlined />}
                        onClick={() => onRemoveKey(row.key)}
                        size="small"
                        type="text"
                    />
                </Tooltip>
            ),
        }] : []),
    ], [bundle, canWrite, drafts, nameOf, onAddLanguage, onOpenKey, onRemoveKey, problems, styles, t])

    return (
        <Table<Row>
            bordered
            virtual
            columns={columns}
            data-testid="localization-table"
            dataSource={rows}
            locale={{ emptyText: t('no_match') }}
            pagination={false}
            rowKey="key"
            size="small"
            scroll={{
                x: KEY_WIDTH + LANGUAGE_WIDTH * bundle.languages.length + (canWrite ? ACTIONS_WIDTH : 0),
                y: Math.max(height - HEADER_HEIGHT, 100),
            }}
        />
    )
}

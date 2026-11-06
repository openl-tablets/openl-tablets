import React, { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Table, Typography } from 'antd'
import type { TableProps } from 'antd'
import type { Bundle } from './bundle'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import { MessageText, type TextOf } from './MessageText'

/** The widths the columns are laid out at; the table scrolls sideways past them. */
const KEY_WIDTH = 260
const LANGUAGE_WIDTH = 280
/** The height of the header row, taken off the height the rows scroll in. */
const HEADER_HEIGHT = 56

/** One row of the table: a key of the messages. */
interface Row {
    key: string
}

interface MessagesTableProps {
    bundle: Bundle
    keys: string[]
    textOf: TextOf
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
 * <p>The rows are drawn a screenful at a time, as bundles reach thousands of keys. The name of a key opens it on
 * its own.
 */
export const MessagesTable = ({ bundle, keys, textOf, onOpenKey, nameOf, styles, height }: MessagesTableProps) => {
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
                    <span className={styles.columnFile}>{language.file.name}</span>
                </>
            ),
            render: (_: unknown, row: Row) => (
                <div className={styles.cell} data-testid="localization-cell">
                    <MessageText
                        bundle={bundle}
                        language={language}
                        messageKey={row.key}
                        nameOf={nameOf}
                        styles={styles}
                        textOf={textOf}
                    />
                </div>
            ),
        })),
    ], [bundle, nameOf, onOpenKey, styles, t, textOf])

    return (
        <Table<Row>
            bordered
            virtual
            columns={columns}
            data-testid="localization-table"
            dataSource={rows}
            pagination={false}
            rowKey="key"
            size="small"
            scroll={{
                x: KEY_WIDTH + LANGUAGE_WIDTH * bundle.languages.length,
                y: Math.max(height - HEADER_HEIGHT, 100),
            }}
        />
    )
}

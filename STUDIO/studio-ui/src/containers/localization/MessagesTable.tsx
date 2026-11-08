import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Table, theme, Tooltip, Typography } from 'antd'
import type { TableProps } from 'antd'
import type { TableRef } from 'antd/es/table'
import type { FilterDropdownProps, FilterValue, SorterResult } from 'antd/es/table/interface'
import { DeleteOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import type { BundleLanguage, KeyRow } from './bundle'
import { LanguageTitle } from './LanguageTitle'
import { SIDEWAYS_BAR, useStyles } from './LocalizationWorkspace.styles'
import { useStyles as useTableEditorStyles } from '../modules/TableEditor.styles'
import { fromKeyFilter, KeyColumnFilter, keyFilterOf } from './KeyColumnFilter'
import { MessageCell } from './MessageCell'
import type { MessageFilter } from './messageFilter'
import { NewKeyRow } from './NewKeyRow'
import type { MessageEdits } from './useMessageEdits'

/** The widths the columns are laid out at; the table scrolls sideways past them. */
const KEY_WIDTH = 260
const LANGUAGE_WIDTH = 280
const ACTIONS_WIDTH = 48
/**
 * The height of the header row and of the row of a new key, taken off the height the rows scroll in until the table
 * is laid out and they are measured.
 */
const HEADER_HEIGHT = 56
const NEW_KEY_HEIGHT = 56
/** The line the bordered table draws over its header. */
const BORDER = 1
/** How long a column or a row just added is lit, fading out. */
const FLASH_MS = 1600
/** How close to the last row read the reader scrolls before the next page is read. */
const MORE_AHEAD = 600
/** How long the scroll to a key just added reads no next page, which would push the key out of sight. */
const QUIET_MS = 500

export interface MessagesTableProps {
    /** The keys read so far, then those the reader added. */
    rows: KeyRow[]
    languages: readonly BundleLanguage[]
    edits: MessageEdits
    canWrite: boolean
    onRemoveKey: (row: KeyRow) => void
    /** Adds a key with its texts by the language from the row under the keys; absent, the row is not shown. */
    onAddKey?: ((key: string, texts: Record<string, string>) => void) | undefined
    /** Whether the messages hold a key already, which a new one may not repeat. */
    keyExists?: ((key: string) => Promise<boolean>) | undefined
    /** What the table says when it has no key. */
    emptyText: string
    /** What the columns narrow the keys down to, and the order of the keys. */
    filter: MessageFilter
    /** Narrows the keys down, or orders them, as the reader set the columns. */
    onFilter: (filter: MessageFilter) => void
    onAddLanguage: () => void
    /** Opens a key on its own, with its text in every language. */
    onOpenKey: (key: string) => void
    nameOf: (tag: string) => string
    /** The height the table is given, its header included. */
    height: number
    /** The width the table is given: narrower than its columns, it scrolls sideways. */
    width: number
    /** Reads the next page of the keys, once the reader scrolls near the last one read. */
    onMore: () => void
    /** The language added on this page, brought into sight and lit for a moment. */
    addedLanguage?: string | null | undefined
    /** The key added on this page, brought into sight and lit for a moment. */
    addedKey?: string | null | undefined
}

/** The filter of a column of a language that leaves the keys the language shows the default text for. */
const UNTRANSLATED = 'untranslated'

/** The order of the column of the keys as the table names it, and back. */
const SORT_ORDERS = { asc: 'ascend', desc: 'descend', none: null } as const
const ORDERS = { ascend: 'asc', descend: 'desc' } as const

const renderKeyFilter = (props: FilterDropdownProps) => <KeyColumnFilter {...props} />

/** The icon of the search in the column of the keys, in the colour of the theme while the keys are narrowed down. */
const KeyFilterIcon = ({ filtered }: { filtered: boolean }) => {
    const { token } = theme.useToken()
    return <SearchOutlined data-testid="localization-key-filter" style={filtered ? { color: token.colorPrimary } : undefined} />
}

const renderKeyFilterIcon = (filtered: boolean) => <KeyFilterIcon filtered={filtered} />

/** What the columns narrow the keys down to, and their order, as the reader set them. */
const filterOf = (
    filters: Record<string, FilterValue | null>,
    sorter: SorterResult<KeyRow> | SorterResult<KeyRow>[],
    languages: readonly BundleLanguage[]
): MessageFilter => {
    const sorted = Array.isArray(sorter) ? sorter[0] : sorter
    return {
        ...fromKeyFilter(filters['key']),
        notTranslatedInto: languages
            .filter(({ tag }) => (filters[`language:${tag}`]?.length ?? 0) > 0)
            .map(({ tag }) => tag),
        order: sorted?.order ? ORDERS[sorted.order] : null,
    }
}

/** A sideways scroll, which a virtual table takes though its types leave it out. */
type SidewaysScroll = Parameters<TableRef['scrollTo']>[0] & { left: number }

/**
 * Scrolls the table sideways. The rows of a virtual table scrolled this way leave its header and the row of a new key
 * where they were, so both follow.
 */
const scrollSideways = (table: TableRef | null, left: number) => {
    const toColumn: SidewaysScroll = { left }
    table?.scrollTo(toColumn)
    table?.nativeElement.querySelectorAll<HTMLElement>('.ant-table-header, .ant-table-summary').forEach(held => {
        held.scrollLeft = left
    })
}

/**
 * The name given, lit from the moment it is given until its light fades; null after that, so a row drawn again by a
 * scroll is not lit again.
 */
const useFlash = (name: string | null): string | null => {
    const [lit, setLit] = useState<string | null>(null)
    useEffect(() => {
        if (name === null) {
            return undefined
        }
        setLit(name)
        const timer = window.setTimeout(() => setLit(null), FLASH_MS)
        return () => window.clearTimeout(timer)
    }, [name])
    return lit
}

/**
 * The key added on the page, brought into sight once the table holds it and lit for a moment. Until the scroll settles,
 * scrolling near the last row reads no next page, which would push the key out of sight.
 *
 * @returns the key lit, and the moment until which no next page is read
 */
const useAddedKey = (table: React.RefObject<TableRef | null>, rows: readonly KeyRow[], addedKey: string | null) => {
    const addedShown = addedKey !== null && rows.some(row => row.key === addedKey)
    const quietUntil = useRef(0)
    useEffect(() => {
        if (addedShown) {
            quietUntil.current = Date.now() + QUIET_MS
            table.current?.scrollTo({ key: addedKey, align: 'end' })
        }
    }, [table, addedKey, addedShown])
    return { litKey: useFlash(addedShown ? addedKey : null), quietUntil }
}

/** The language added on the page, its column scrolled beside the keys and lit for a moment. */
const useAddedLanguage = (table: React.RefObject<TableRef | null>,
    languages: readonly BundleLanguage[],
    addedLanguage: string | null) => {
    const languageAt = languages.findIndex(language => language.tag === addedLanguage)
    useEffect(() => {
        if (languageAt < 0) {
            return undefined
        }
        // Scrolled once the table has measured the new column: before, it cannot be scrolled that far.
        const frame = requestAnimationFrame(() => scrollSideways(table.current, languageAt * LANGUAGE_WIDTH))
        return () => cancelAnimationFrame(frame)
    }, [table, addedLanguage, languageAt])
    return useFlash(languageAt >= 0 ? addedLanguage : null)
}

/**
 * The height of the header and of the row under the keys: they are not scrolled with the rows, so the rows are given
 * what the table has left. Both are measured as they are drawn, as the density and the theme change them; until then
 * they are taken at the height they usually have.
 */
const useHeldHeight = (table: React.RefObject<TableRef | null>, withNewKey: boolean): number => {
    const [held, setHeld] = useState(0)
    useEffect(() => {
        const element = table.current?.nativeElement
        const parts = [...element?.querySelectorAll<HTMLElement>(
            '.ant-table-container > .ant-table-header, .ant-table-container > .ant-table-summary') ?? []]
        const measure = () => setHeld(parts.reduce((sum, part) => sum + part.offsetHeight, 0))
        measure()
        const observer = new ResizeObserver(measure)
        parts.forEach(part => observer.observe(part))
        return () => observer.disconnect()
    }, [table, withNewKey])
    const usual = withNewKey ? HEADER_HEIGHT + NEW_KEY_HEIGHT : HEADER_HEIGHT
    return held > 0 ? held : usual
}

/**
 * All the messages as one table: a row per key, a column per language.
 *
 * <p>The keys are read a page at a time, the next page once the reader scrolls near the last key read, and the rows
 * are drawn a screenful at a time, as bundles reach thousands of keys. A text is written in its cell; the name of a key
 * opens it on its own.
 *
 * <p>A language or a key added here is scrolled to, and its column or row is lit for a moment, so the reader finds
 * it among the others. A key added comes after the keys read so far; scrolling to it reads no next page, which would
 * push it out of sight.
 *
 * <p>Under the keys a row adds a new key with its text in the default language, one after another from the keyboard.
 * It stays in sight however far the keys are scrolled.
 *
 * <p>A text changed and not saved is marked as the table of a module marks a cell written and not saved.
 */
export const MessagesTable = ({
    rows,
    languages,
    edits,
    canWrite,
    onRemoveKey,
    onAddKey,
    keyExists,
    emptyText,
    filter,
    onFilter,
    onAddLanguage,
    onOpenKey,
    nameOf,
    height,
    width,
    onMore,
    addedLanguage = null,
    addedKey = null,
}: MessagesTableProps) => {
    const { t } = useTranslation('localization')
    const { styles } = useStyles()
    const { styles: tableEditor } = useTableEditorStyles(false)
    const table = useRef<TableRef>(null)
    const { litKey, quietUntil } = useAddedKey(table, rows, addedKey)
    const litLanguage = useAddedLanguage(table, languages, addedLanguage)

    // A text of the row of a new key that takes the focus has its column brought into sight, past the key on the left
    // and the actions on the right, which stay where they are: the rows and the header go along with it.
    const revealLanguage = useCallback((at: number) => {
        requestAnimationFrame(() => {
            const header = table.current?.nativeElement.querySelector<HTMLElement>('.ant-table-header')
            if (!header) {
                return
            }
            const shown = header.scrollLeft
            const room = header.clientWidth - KEY_WIDTH - (canWrite ? ACTIONS_WIDTH : 0)
            const start = at * LANGUAGE_WIDTH
            const end = start + LANGUAGE_WIDTH
            const left = start < shown ? start : Math.max(shown, end - room)
            scrollSideways(table.current, left)
        })
    }, [canWrite])

    const columns = useMemo<NonNullable<TableProps<KeyRow>['columns']>>(() => [
        {
            title: t('key'),
            dataIndex: 'key',
            key: 'key',
            fixed: 'left',
            width: KEY_WIDTH,
            // Every page of the keys is asked of the server in the order and with the filters of the columns.
            sorter: true,
            sortOrder: SORT_ORDERS[filter.order ?? 'none'],
            filteredValue: keyFilterOf(filter),
            filterDropdown: renderKeyFilter,
            filterIcon: renderKeyFilterIcon,
            render: (key: string) => (
                <Typography.Link data-testid="localization-open-key" onClick={() => onOpenKey(key)}>
                    {key}
                </Typography.Link>
            ),
        },
        ...languages.map(language => ({
            key: `language:${language.tag}`,
            // The default language shows its own texts: it falls back to no other.
            ...(language.tag === '' ? {} : {
                filters: [{ text: t('not_translated'), value: UNTRANSLATED }],
                filterMultiple: false,
                filteredValue: filter.notTranslatedInto.includes(language.tag) ? [UNTRANSLATED] : null,
            }),
            ...(language.tag === litLanguage ? { className: styles.flashed } : {}),
            width: LANGUAGE_WIDTH,
            title: (
                <LanguageTitle
                    name={nameOf(language.tag)}
                    tag={language.tag}
                    testId={`localization-column-${language.tag || 'default'}`}
                />
            ),
            onCell: (row: KeyRow) => (edits.changed(row.key, language.tag) ? { className: tableEditor.touched } : {}),
            render: (_: unknown, row: KeyRow) => (
                <MessageCell
                    canWrite={canWrite}
                    edits={edits}
                    language={language}
                    nameOf={nameOf}
                    row={row}
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
            render: (_: unknown, row: KeyRow) => (
                <Tooltip title={t('remove_key')}>
                    <Button
                        aria-label={t('remove_key')}
                        data-testid="localization-remove-key"
                        icon={<DeleteOutlined />}
                        onClick={() => onRemoveKey(row)}
                        size="small"
                        type="text"
                    />
                </Tooltip>
            ),
        }] : []),
    ], [canWrite, edits, filter, languages, litLanguage, nameOf, onAddLanguage, onOpenKey, onRemoveKey, styles, t, tableEditor])

    const newKey = canWrite && onAddKey !== undefined && keyExists !== undefined
        && languages.some(language => language.tag === '')
        // The table holds the row under the keys only when it is given the summary itself.
        ? () => (
            <Table.Summary fixed="bottom">
                <NewKeyRow
                    exists={keyExists}
                    languages={languages}
                    onAdd={onAddKey}
                    onFocusLanguage={revealLanguage}
                    withActions={canWrite}
                />
            </Table.Summary>
        )
        : null

    const heldHeight = useHeldHeight(table, newKey !== null)
    // A table narrower than its columns scrolls sideways, and its bar is given room under the rows: a virtual table
    // lays it over the last row otherwise.
    const columnsWidth = KEY_WIDTH + LANGUAGE_WIDTH * languages.length + (canWrite ? ACTIONS_WIDTH : 0)
    const sideways = width > 0 && columnsWidth > width - 2 * BORDER

    return (
        <Table<KeyRow>
            ref={table}
            bordered
            virtual
            {...(sideways ? { className: styles.sideways } : {})}
            columns={columns}
            data-testid="localization-table"
            dataSource={rows}
            locale={{ emptyText }}
            onChange={(_, filters, sorter) => onFilter(filterOf(filters, sorter, languages))}
            pagination={false}
            rowClassName={row => (row.key === litKey ? styles.flashedRow : '')}
            rowKey="key"
            size="small"
            {...(newKey === null ? {} : { summary: newKey })}
            onScroll={event => {
                const { scrollTop, scrollHeight, clientHeight } = event.currentTarget
                if (scrollHeight - scrollTop - clientHeight < MORE_AHEAD && Date.now() > quietUntil.current) {
                    onMore()
                }
            }}
            scroll={{
                x: columnsWidth,
                y: Math.max(height - BORDER - heldHeight - (sideways ? SIDEWAYS_BAR : 0), 100),
            }}
        />
    )
}

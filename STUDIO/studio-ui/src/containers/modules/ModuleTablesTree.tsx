import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useUserStore } from '../../store'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, Empty, Input, Modal, Select, Skeleton, Space, Spin, Tooltip, Tree, Typography } from 'antd'
import { CheckCircleFilled, FilterOutlined, SlidersOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import { COMPILE_COLORS } from '../projects/projectsTheme'
import { useSharedStyles } from '../projects/sharedStyles'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { groupIcon, tableIcon } from './tableIcons'
import {
    DEFAULT_VIEW,
    loadView,
    saveView,
    TABLE_VIEWS,
    treeOf,
    widthOf,
    type TableNode,
    type TableView,
} from './tableGrouping'

/** The height of one row of the tree, which the virtual list counts in. */
const ROW_HEIGHT = 24

/** The width the rail was last dragged to, kept so a reader who made room for long names keeps it. */
const WIDTH_STORAGE_KEY = 'openl.module.rail.width'
const WIDTH = { min: 180, max: 640, fallback: 256 }

const useStyles = createStyles(({ css, token }) => ({
    /** The rail is dragged by its right edge, which the grip is laid along. */
    resizable: css`
        position: relative;
    `,
    /** The mode switch and, under it, whatever the mode needs. */
    top: css`
        flex: none;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /** The search, and under it the grouping with its filter beside it, as the Editor kept them, set apart. */
    control: css`
        margin-top: 8px;
    `,
    /**
     * The tree scrolls inside itself: it draws the rows it shows and no more, so a module of hundreds of tables
     * scrolls as fast as one of ten. Sideways it scrolls too — a table is recognised by its full name, and a
     * wrapped or clipped one is neither readable nor comparable to the row above it.
     */
    body: css`
        flex: 1;
        min-height: 0;
        overflow: hidden;
        padding: 4px 8px 12px;
    `,
    /**
     * A table switched off by its `active` property is written in the module but takes no part in the rules.
     * The Editor drew it faint, and the rail keeps that: the row is read as present but out of play.
     */
    inactive: css`
        opacity: 0.45;
    `,
    state: css`
        padding: 12px 16px;
    `,
    /** A row with errors under it, named the way the Editor named it: in the error colour. */
    broken: css`
        color: ${COMPILE_COLORS.errors};
    `,
    /** How many errors the row stands for, as the badge the Editor put at the end of the row. */
    errors: css`
        display: inline-block;
        min-width: 17px;
        margin-left: ${token.marginXS}px;
        padding: 0 ${token.paddingXXS}px;
        border-radius: ${token.borderRadiusSM}px;
        background: ${COMPILE_COLORS.errors};
        color: ${token.colorTextLightSolid};
        font-size: ${token.fontSizeSM}px;
        line-height: ${token.fontSizeSM + 5}px;
        text-align: center;
    `,
    /** Holds the mark over the table's own icon, which the tree draws in a box of its own width. */
    marked: css`
        position: relative;
    `,
    /**
     * The mark on a table some test exercises, in the corner of that table's icon, as the Editor drew it.
     * It sits over the icon rather than beside it: the tree gives a row one icon's width, and a second glyph
     * on that line would push the name out of place.
     */
    tested: css`
        position: absolute;
        right: -2px;
        bottom: 0;
        color: ${COMPILE_COLORS.ok};
        font-size: ${token.fontSizeSM - 3}px;
    `,
}))

/** One node as the tree draws it. */
interface TreeDataNode {
    key: string
    title: React.ReactNode
    icon: React.ReactNode
    selectable: boolean
    /** How many errors were raised about the tables under this row, this row's own table included. */
    errors: number
    /** Set on a row the tree draws apart — a table that takes no part in the rules. */
    className?: string
    'data-testid'?: string
    children: TreeDataNode[]
}

/** What a row is called, and how many errors stand behind it. */
const nodeTitle = (node: TableNode, styles: TreeStyles, errors: number): React.ReactNode => {
    const named = (
        <>
            <span className={errors > 0 ? styles.broken : undefined}>{node.title}</span>
            {errors > 0 && <span className={styles.errors} data-testid="module-table-errors">{errors}</span>}
        </>
    )
    const table = node.table
    if (table === undefined) {
        return node.hint ? <Tooltip title={node.hint}>{named}</Tooltip> : named
    }
    // The full signature is what the Editor showed on a node, so a name cut short still says what it is.
    return <Tooltip title={table.signature ?? table.displayName ?? table.name}>{named}</Tooltip>
}

/** The classes the tree draws a table's state in. */
interface TreeStyles {
    inactive: string
    broken: string
    errors: string
    marked: string
    tested: string
}

/**
 * One node of the tree as Ant Design draws it, with what the compilation made of the table it names.
 *
 * <p>A group carries the errors of everything under it, as the Editor's tree carried them: a branch says how
 * much is broken inside it without having to be opened. Counted on the way back up, so a tree of any size is
 * walked once.
 */
const toTreeNode = (node: TableNode, styles: TreeStyles): TreeDataNode => {
    const children = node.children.map(child => toTreeNode(child, styles))
    const errors = children.reduce((total, child) => total + child.errors, node.table?.errors ?? 0)
    return {
        key: node.table ? node.table.id : node.key,
        title: nodeTitle(node, styles, errors),
        icon: node.table
            ? (
                <span
                    className={styles.marked}
                    data-testid={node.table.hasTests === true ? 'module-table-tested' : undefined}
                >
                    {tableIcon(node.table.kind)}
                    {node.table.hasTests === true && <CheckCircleFilled className={styles.tested} />}
                </span>
            )
            : groupIcon(node.groupedBy),
        selectable: node.table !== undefined,
        errors,
        ...(node.table?.active === false ? { className: styles.inactive, 'data-testid': 'module-table-inactive' } : {}),
        children,
    }
}

/** The keys of the groups on the way down to the given table, so only that branch stands open. */
const pathTo = (nodes: TableNode[], tableId: string, trail: string[] = []): string[] | null => {
    for (const node of nodes) {
        if (node.table?.id === tableId) {
            return trail
        }
        const found = pathTo(node.children, tableId, [...trail, node.key])
        if (found) {
            return found
        }
    }
    return null
}

interface ModuleTablesTreeProps {
    /** The tables of the module, or null while they have not been read — a closed project, or a compile still running. */
    tables: ModuleTable[] | null
    /** The table shown beside the tree, so the tree marks where the reader is. */
    selectedTableId?: string | undefined
    onSelectTable: (table: ModuleTable) => void
    /** Opens the extended search, carrying what the reader has typed so far. */
    onExtendedSearch: (typed: string) => void
    /** Whether the free-form tables — the ones OpenL does not recognize — are listed with the rest. */
    showOther: boolean
    /** Asks for the free-form tables to be listed, or to be left out again. */
    onShowOther: (shown: boolean) => void
    /**
     * Set while the list on screen is being read again, with or without the free-form tables. The list stays
     * on screen, dimmed, so the reader keeps their place; the filter is held while any list is on its way, or
     * a choice made in it would start a second read of the same module beside the one in flight.
     */
    reloading: boolean
}

/**
 * The left rail of the editor: the tables of the module being read. Which module that is, is chosen in the
 * header, where the module's name stands.
 *
 * The tables are grouped here rather than by the server: the list arrives flat, carrying what every view groups
 * by, so changing the view rearranges what the browser already holds and costs no request. The chosen view is
 * remembered, and the tree opens on the one the engine itself defaults to — by Excel sheet.
 *
 * The tree stands closed except along the way down to the table being read, so a module of hundreds of tables
 * opens as a short list rather than as everything at once.
 *
 * The free-form tables are not in the list unless asked for, as the Editor's tree hid its utility tables until
 * its filter dialog said otherwise; the rail keeps that dialog behind the sliders beside the grouping, and the
 * server lists them on the choice made there.
 */
export const ModuleTablesTree = ({
    tables,
    selectedTableId,
    onSelectTable,
    onExtendedSearch,
    showOther,
    onShowOther,
    reloading,
}: ModuleTablesTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const { styles: shared } = useSharedStyles()
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'right', WIDTH)
    const [view, setView] = useState<TableView>(DEFAULT_VIEW)
    // The Default Order of the user's own settings decides what the tree opens on.
    const preferredView = useUserStore(state => state.userProfile?.treeView)
    const [expanded, setExpanded] = useState<string[]>([])
    const [search, setSearch] = useState('')
    // The filter dialog works on a copy of the choice, applied or thrown away when it closes, as the Editor's did.
    const [filterOpen, setFilterOpen] = useState(false)
    const [draftOther, setDraftOther] = useState(showOther)
    // The tree draws the rows that fit and no more, so it has to be told what fits.
    const bodyRef = useRef<HTMLDivElement>(null)
    const [body, setBody] = useState({ height: 0, width: 0 })

    useEffect(() => {
        const measured = bodyRef.current
        if (!measured) {
            return
        }
        const observer = new ResizeObserver(entries => {
            const box = entries[0]?.contentRect
            setBody({ height: Math.floor(box?.height ?? 0), width: Math.floor(box?.width ?? 0) })
        })
        observer.observe(measured)
        return () => observer.disconnect()
    }, [])

    // Read once the settings are known: this browser's last choice, or the user's Default Order.
    useEffect(() => setView(loadView(preferredView)), [preferredView])

    // The rail searches what it shows, by name: the tables of this module are already in the browser, so the
    // search costs no request. Everything wider than a name — a header, the text in the cells, another module —
    // is what the extended search asks the server for.
    const shown = useMemo(() => {
        const wanted = search.trim().toLowerCase()
        return wanted === '' ? tables ?? [] : (tables ?? [])
            .filter(table => (table.displayName ?? table.name).toLowerCase().includes(wanted))
    }, [tables, search])

    const nodes = useMemo(() => treeOf(shown, view, t), [shown, view, t])
    // A row is as wide as its own name needs, and never narrower than the rail: a scrolling width smaller than
    // what is on screen leaves the virtual list pushed to the right of an empty rail.
    const rowWidth = useMemo(() => Math.max(Math.ceil(widthOf(nodes)), body.width), [nodes, body.width])

    // Only the branch holding the open table stands open; the user opens the rest themselves.
    useEffect(() => {
        setExpanded(selectedTableId ? pathTo(nodes, selectedTableId) ?? [] : [])
    }, [nodes, selectedTableId])

    const viewOptions = useMemo(
        () => TABLE_VIEWS.map(name => ({ value: name, label: t(`browser.module.view_${name}`) })),
        [t]
    )

    // Built once per tree: the rail redraws on every status the compilation pushes and on every step of a
    // drag, and a module of hundreds of tables would be rebuilt, icons and all, each time.
    const marks = useMemo(() => ({
        inactive: styles.inactive,
        broken: styles.broken,
        errors: styles.errors,
        marked: styles.marked,
        tested: styles.tested,
    }), [styles.inactive, styles.broken, styles.errors, styles.marked, styles.tested])
    const treeData = useMemo(() => nodes.map(node => toTreeNode(node, marks)), [nodes, marks])

    // A screenful of rows at a time, drawn at once rather than slid open — the height a fold animates is
    // painted by the page, frame by frame (measured at 64 repaints over 350 ms for one folder against 9).
    const railTree = {
        blockNode: true,
        showIcon: true,
        className: shared.railTree,
        height: body.height,
        itemHeight: ROW_HEIGHT,
        motion: false as const,
    }

    /**
     * What the rail draws: the tables of the module being read — the shape of a list while they are still on
     * their way, and a word when the search matched none of them.
     */
    const railBody = () => {
        if (tables === null) {
            return (
                <Skeleton active className={styles.state} paragraph={{ rows: 8 }} title={false} />
            )
        }
        if (shown.length === 0) {
            return (
                <Empty
                    className={styles.state}
                    data-testid="module-tables-empty"
                    description={t(search.trim() === '' ? 'browser.module.no_tables' : 'browser.module.no_match')}
                />
            )
        }
        return (
            <Tree
                {...railTree}
                data-testid="module-tables-tree"
                expandedKeys={expanded}
                onExpand={keys => setExpanded(keys as string[])}
                scrollWidth={rowWidth}
                selectedKeys={selectedTableId ? [selectedTableId] : []}
                treeData={treeData as never}
                onSelect={(_keys, info) => {
                    // Only a table is selectable, and a table row is keyed by its own id.
                    const table = shown.find(candidate => candidate.id === String(info.node.key))
                    if (table) {
                        onSelectTable(table)
                    }
                }}
            />
        )
    }

    return (
        <aside className={cx(shared.rail, styles.resizable)} data-testid="module-rail" style={{ width }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId="module-rail-resizer" />
            <div className={styles.top}>
                <Input
                    allowClear
                    className={styles.control}
                    data-testid="module-tables-search"
                    onChange={event => setSearch(event.target.value)}
                    placeholder={t('browser.module.search_placeholder')}
                    size="small"
                    value={search}
                    suffix={(
                        <Tooltip title={t('browser.module.search_extended')}>
                            <Button
                                aria-label={t('browser.module.search_extended')}
                                data-testid="module-tables-search-extended"
                                icon={<FilterOutlined />}
                                onClick={() => onExtendedSearch(search.trim())}
                                size="small"
                                type="text"
                            />
                        </Tooltip>
                    )}
                />
                <Space.Compact block className={cx(shared.compactField, styles.control)}>
                    <Select
                        data-testid="module-tables-view"
                        options={viewOptions}
                        size="small"
                        value={view}
                        onChange={chosen => {
                            setView(chosen)
                            saveView(chosen)
                        }}
                    />
                    <Tooltip title={t('browser.module.filter')}>
                        <Button
                            aria-label={t('browser.module.filter')}
                            data-testid="module-tables-filter"
                            disabled={reloading || tables === null}
                            icon={<SlidersOutlined />}
                            size="small"
                            onClick={() => {
                                setDraftOther(showOther)
                                setFilterOpen(true)
                            }}
                        />
                    </Tooltip>
                </Space.Compact>
            </div>
            <div ref={bodyRef} className={styles.body}>
                <Spin data-testid="module-tables-reloading" spinning={reloading}>
                    {railBody()}
                </Spin>
            </div>
            <Modal
                cancelText={t('common:btn.cancel')}
                okText={t('common:btn.apply')}
                onCancel={() => setFilterOpen(false)}
                open={filterOpen}
                title={t('browser.module.filter')}
                width={420}
                onOk={() => {
                    setFilterOpen(false)
                    if (draftOther !== showOther) {
                        onShowOther(draftOther)
                    }
                }}
            >
                <Checkbox
                    checked={draftOther}
                    data-testid="module-tables-other"
                    onChange={event => setDraftOther(event.target.checked)}
                >
                    {t('browser.module.show_other')}
                </Checkbox>
                <Typography.Paragraph type="secondary">{t('browser.module.show_other_hint')}</Typography.Paragraph>
            </Modal>
        </aside>
    )
}

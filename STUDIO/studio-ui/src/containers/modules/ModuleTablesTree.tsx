import { useEffect, useMemo, useRef, useState } from 'react'
import { useUserStore } from '../../store'
import { useTranslation } from 'react-i18next'
import { Alert, Empty, Segmented, Select, Tree } from 'antd'
import { FileExcelOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import type { ModuleInfo } from '../../services/modules'
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

/** What the panel shows: the tables of the open module, or the modules to open instead. */
type RailMode = 'tables' | 'modules'

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
    picker: css`
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
}))

/** One node as the tree draws it. */
interface TreeDataNode {
    key: string
    title: string
    icon: React.ReactNode
    selectable: boolean
    /** Set on a row the tree draws apart — a table that takes no part in the rules. */
    className?: string
    'data-testid'?: string
    children: TreeDataNode[]
}

/** One node of the tree as Ant Design draws it, with the class a switched-off table is drawn faint in. */
const toTreeNode = (node: TableNode, inactive: string): TreeDataNode => ({
    key: node.table ? node.table.id : node.key,
    title: node.title,
    icon: node.table ? tableIcon(node.table.kind) : groupIcon(node.groupedBy),
    selectable: node.table !== undefined,
    ...(node.table?.active === false ? { className: inactive, 'data-testid': 'module-table-inactive' } : {}),
    children: node.children.map(child => toTreeNode(child, inactive)),
})

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
    /** The modules of the project, so another one can be opened from here. */
    modules: ModuleInfo[]
    /** The module the editor has open, marked in the module list. */
    currentModule: string
    /**
     * Whether that module is still being compiled.
     *
     * <p>A session compiles one module at a time, so asking for another one while this is running only queues
     * the request behind it — the reader would be left on an empty screen until the first compilation reached
     * its end. The list is closed for as long as that lasts, and the screen offers to stop the compilation.
     */
    compiling?: boolean
    /** The table shown beside the tree, so the tree marks where the reader is. */
    selectedTableId?: string | undefined
    onSelectTable: (table: ModuleTable) => void
    onSelectModule: (moduleName: string) => void
}

/**
 * The left rail of the editor: the tables of the open module, or the modules of the project.
 *
 * The tables are grouped here rather than by the server: the list arrives flat, carrying what every view groups
 * by, so changing the view rearranges what the browser already holds and costs no request. The chosen view is
 * remembered, and the tree opens on the one the engine itself defaults to — by Excel sheet.
 *
 * The tree stands closed except along the way down to the table being read, so a module of hundreds of tables
 * opens as a short list rather than as everything at once.
 */
export const ModuleTablesTree = ({
    tables,
    modules,
    currentModule,
    compiling = false,
    selectedTableId,
    onSelectTable,
    onSelectModule,
}: ModuleTablesTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const { styles: shared } = useSharedStyles()
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'right', WIDTH)
    const [mode, setMode] = useState<RailMode>('tables')
    const [view, setView] = useState<TableView>(DEFAULT_VIEW)
    // The Default Order of the user's own settings decides what the tree opens on.
    const preferredView = useUserStore(state => state.userProfile?.treeView)
    const [expanded, setExpanded] = useState<string[]>([])
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

    const nodes = useMemo(() => treeOf(tables ?? [], view), [tables, view])
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
    const treeData = useMemo(() => nodes.map(node => toTreeNode(node, styles.inactive)), [nodes, styles.inactive])

    const moduleNodes = useMemo(() => modules.map(module => ({
        key: module.name,
        title: module.name,
        icon: <FileExcelOutlined />,
        selectable: true,
        // Only the module already open can be picked while it compiles; the rest would wait behind it.
        disabled: compiling && module.name !== currentModule,
        children: [],
    })), [modules, compiling, currentModule])

    return (
        <aside className={cx(shared.rail, styles.resizable)} data-testid="module-rail" style={{ width }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId="module-rail-resizer" />
            <div className={styles.top}>
                <Segmented
                    block
                    data-testid="module-rail-mode"
                    onChange={value => setMode(value as RailMode)}
                    size="small"
                    value={mode}
                    options={[
                        { label: t('browser.module.rail_tables'), value: 'tables' },
                        { label: t('browser.module.rail_modules'), value: 'modules' },
                    ]}
                />
                {mode === 'tables' && (
                    <Select
                        className={styles.picker}
                        data-testid="module-tables-view"
                        options={viewOptions}
                        size="small"
                        style={{ width: '100%' }}
                        value={view}
                        onChange={chosen => {
                            setView(chosen)
                            saveView(chosen)
                        }}
                    />
                )}
            </div>
            <div ref={bodyRef} className={styles.body}>
                {mode === 'modules' && compiling && (
                    <Alert
                        showIcon
                        className={styles.state}
                        data-testid="module-rail-compiling"
                        title={t('browser.module.switch_blocked', { module: currentModule })}
                        type="info"
                    />
                )}
                {mode === 'modules' ? (
                    <Tree
                        blockNode
                        showIcon
                        className={shared.railTree}
                        data-testid="module-rail-modules"
                        height={body.height}
                        itemHeight={ROW_HEIGHT}
                        onSelect={(_keys, info) => onSelectModule(String(info.node.key))}
                        selectedKeys={[currentModule]}
                        treeData={moduleNodes as never}
                    />
                ) : tables === null ? null : tables.length === 0 ? (
                    <Empty
                        className={styles.state}
                        data-testid="module-tables-empty"
                        description={t('browser.module.no_tables')}
                    />
                ) : (
                    <Tree
                        blockNode
                        showIcon
                        className={shared.railTree}
                        data-testid="module-tables-tree"
                        expandedKeys={expanded}
                        height={body.height}
                        itemHeight={ROW_HEIGHT}
                        onExpand={keys => setExpanded(keys as string[])}
                        scrollWidth={rowWidth}
                        selectedKeys={selectedTableId ? [selectedTableId] : []}
                        treeData={treeData as never}
                        onSelect={(_keys, info) => {
                            // Only a table is selectable, and a table row is keyed by its own id.
                            const table = tables?.find(candidate => candidate.id === String(info.node.key))
                            if (table) {
                                onSelectTable(table)
                            }
                        }}
                    />
                )}
            </div>
        </aside>
    )
}

import { useEffect, useMemo, useRef, useState } from 'react'
import { useUserStore } from '../../store'
import { useTranslation } from 'react-i18next'
import { Empty, Segmented, Select, Tree } from 'antd'
import { FileExcelOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import type { ModuleInfo } from '../../services/modules'
import { useSharedStyles } from '../projects/sharedStyles'
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

/** What the panel shows: the tables of the open module, or the modules to open instead. */
type RailMode = 'tables' | 'modules'

const useStyles = createStyles(({ css, token }) => ({
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
    tree: css`
        background: transparent;

        .ant-tree-treenode {
            padding-bottom: 0;
            white-space: nowrap;
            align-items: center;
        }

        /* The rail is narrow: every step of the hierarchy costs width, so it stays small. */
        .ant-tree-indent-unit {
            width: 12px;
        }

        .ant-tree-switcher {
            width: 18px;
            line-height: 24px;
        }

        .ant-tree-node-content-wrapper {
            display: inline-flex;
            align-items: center;
            gap: 4px;
            min-height: 24px;
            line-height: 24px;
            padding: 0 4px;
            overflow: visible;
        }

        /* A name is read in full, on one line: the tree scrolls sideways instead of clipping it. */
        .ant-tree-title,
        .ant-tree-node-content-wrapper .ant-tree-title {
            overflow: visible;
            text-overflow: clip;
            white-space: nowrap;
        }

        .ant-tree-iconEle {
            width: auto;
            line-height: 24px;
        }
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
    children: TreeDataNode[]
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
    /** The modules of the project, so another one can be opened from here. */
    modules: ModuleInfo[]
    /** The module the editor has open, marked in the module list. */
    currentModule: string
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
    selectedTableId,
    onSelectTable,
    onSelectModule,
}: ModuleTablesTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { styles: shared } = useSharedStyles()
    const [mode, setMode] = useState<RailMode>('tables')
    const [view, setView] = useState<TableView>(DEFAULT_VIEW)
    // The Default Order of the user's own settings decides what the tree opens on.
    const preferredView = useUserStore(state => state.userProfile?.treeView)
    const [expanded, setExpanded] = useState<string[]>([])
    // The tree draws the rows that fit and no more, so it has to be told what fits.
    const bodyRef = useRef<HTMLDivElement>(null)
    const [bodyHeight, setBodyHeight] = useState(0)

    useEffect(() => {
        const body = bodyRef.current
        if (!body) {
            return
        }
        const observer = new ResizeObserver(entries => {
            const measured = entries[0]?.contentRect.height ?? 0
            setBodyHeight(Math.floor(measured))
        })
        observer.observe(body)
        return () => observer.disconnect()
    }, [])

    // Read once the settings are known: this browser's last choice, or the user's Default Order.
    useEffect(() => setView(loadView(preferredView)), [preferredView])

    const nodes = useMemo(() => treeOf(tables ?? [], view), [tables, view])
    const rowWidth = useMemo(() => Math.ceil(widthOf(nodes)), [nodes])

    // Only the branch holding the open table stands open; the user opens the rest themselves.
    useEffect(() => {
        setExpanded(selectedTableId ? pathTo(nodes, selectedTableId) ?? [] : [])
    }, [nodes, selectedTableId])

    const viewOptions = TABLE_VIEWS.map(name => ({ value: name, label: t(`browser.module.view_${name}`) }))

    const toTreeNode = (node: TableNode): TreeDataNode => ({
        key: node.table ? node.table.id : node.key,
        title: node.title,
        icon: node.table ? tableIcon(node.table.kind) : groupIcon(node.groupedBy),
        selectable: node.table !== undefined,
        children: node.children.map(toTreeNode),
    })

    const findTable = (from: TableNode[], key: string): ModuleTable | undefined => {
        for (const node of from) {
            if (node.table?.id === key) {
                return node.table
            }
            const found = findTable(node.children, key)
            if (found) {
                return found
            }
        }
        return undefined
    }

    const moduleNodes = modules.map(module => ({
        key: module.name,
        title: module.name,
        icon: <FileExcelOutlined />,
        selectable: true,
        children: [],
    }))

    return (
        <aside className={shared.rail} data-testid="module-rail">
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
                {mode === 'modules' ? (
                    <Tree
                        blockNode
                        showIcon
                        className={styles.tree}
                        data-testid="module-rail-modules"
                        height={bodyHeight}
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
                        className={styles.tree}
                        data-testid="module-tables-tree"
                        expandedKeys={expanded}
                        height={bodyHeight}
                        itemHeight={ROW_HEIGHT}
                        onExpand={keys => setExpanded(keys as string[])}
                        scrollWidth={rowWidth}
                        selectedKeys={selectedTableId ? [selectedTableId] : []}
                        treeData={nodes.map(toTreeNode) as never}
                        onSelect={(_keys, info) => {
                            const table = findTable(nodes, String(info.node.key))
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

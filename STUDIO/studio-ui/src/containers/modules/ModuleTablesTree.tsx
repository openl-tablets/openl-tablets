import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Empty, Select, Tree } from 'antd'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import { useSharedStyles } from '../projects/sharedStyles'
import { groupIcon, tableIcon } from './tableIcons'
import {
    DEFAULT_VIEW,
    loadView,
    saveView,
    TABLE_VIEWS,
    treeOf,
    type TableNode,
    type TableView,
} from './tableGrouping'

const useStyles = createStyles(({ css, token }) => ({
    /** The view picker, above the scrolling tree, in the rail's own header band. */
    head: css`
        flex: none;
        padding: 8px 12px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    /**
     * The tree scrolls sideways instead of squeezing the names: a table is recognised by its full name, and a
     * wrapped or clipped one is neither readable nor comparable to the row above it.
     */
    body: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: 4px 8px 12px;

        .ant-tree-list-holder-inner,
        .ant-tree-list {
            min-width: max-content;
        }
    `,
    tree: css`
        background: transparent;
        width: max-content;
        min-width: 100%;

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

interface ModuleTablesTreeProps {
    tables: ModuleTable[]
    /** The table shown beside the tree, so the tree marks where the reader is. */
    selectedTableId?: string | undefined
    onSelectTable: (table: ModuleTable) => void
}

/**
 * The tables of the open module, gathered into a tree.
 *
 * The grouping is done here rather than by the server: the list arrives flat, carrying what every view groups by,
 * so changing the view rearranges what the browser already holds and costs no request. The chosen view is
 * remembered, and the tree opens on the one the engine itself defaults to — by Excel sheet.
 */
export const ModuleTablesTree = ({ tables, selectedTableId, onSelectTable }: ModuleTablesTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { styles: shared } = useSharedStyles()
    const [view, setView] = useState<TableView>(DEFAULT_VIEW)
    const [expanded, setExpanded] = useState<string[]>([])

    // Read once, on the browser the page is opened in.
    useEffect(() => setView(loadView()), [])

    const nodes = useMemo(() => treeOf(tables, view), [tables, view])

    // Opened to the tables: a tree that shows only its groups hides everything the reader came for.
    useEffect(() => {
        const groups = (from: TableNode[]): string[] =>
            from.filter(node => node.children.length > 0).flatMap(node => [node.key, ...groups(node.children)])
        setExpanded(groups(nodes))
    }, [nodes])

    const options = TABLE_VIEWS.map(name => ({ value: name, label: t(`browser.module.view_${name}`) }))

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

    return (
        <aside className={shared.rail} data-testid="module-tables-panel">
            <div className={styles.head}>
                <Select
                    data-testid="module-tables-view"
                    options={options}
                    size="small"
                    style={{ width: '100%' }}
                    value={view}
                    onChange={chosen => {
                        setView(chosen)
                        saveView(chosen)
                    }}
                />
            </div>
            <div className={styles.body}>
                {tables.length === 0 ? (
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
                        onExpand={keys => setExpanded(keys as string[])}
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

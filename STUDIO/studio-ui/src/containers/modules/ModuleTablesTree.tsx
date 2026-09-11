import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Empty, Select, Tree } from 'antd'
import {
    DatabaseOutlined,
    FileExcelOutlined,
    FolderOutlined,
    ProfileOutlined,
    TableOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import type { ModuleTable } from 'types/tables'
import {
    activeLevels,
    buildTableTree,
    DEFAULT_GROUPING,
    GROUP_BY_CATEGORY,
    GROUP_BY_FILE,
    GROUP_BY_KIND,
    GROUP_BY_NONE,
    GROUP_BY_TABLE_TYPE,
    loadGrouping,
    saveGrouping,
    type TableGroupingLevels,
    type TableNode,
} from './tableGrouping'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: flex;
        flex-direction: column;
        flex: none;
        width: 280px;
        min-height: 0;
        overflow: hidden;
        border-right: 1px solid ${token.colorBorderSecondary};
    `,
    grouping: css`
        flex: none;
        padding: 8px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    body: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: 8px;
    `,
}))

/** The icon a group wears: what it gathers the tables by. */
const groupIcon = (level: string): ReactNode => {
    if (level === GROUP_BY_FILE) {
        return <FileExcelOutlined />
    }
    if (level === GROUP_BY_KIND) {
        return <DatabaseOutlined />
    }
    if (level === GROUP_BY_TABLE_TYPE) {
        return <ProfileOutlined />
    }
    return <FolderOutlined />
}

/** One node as the tree draws it. */
interface TreeDataNode {
    key: string
    title: string
    icon: ReactNode
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
 * The grouping is done here rather than by the server: the list arrives flat, carrying what every level groups by,
 * so changing the grouping rearranges what the browser already holds and costs no request. The chosen grouping is
 * remembered, as the projects tree remembers its own.
 */
export const ModuleTablesTree = ({ tables, selectedTableId, onSelectTable }: ModuleTablesTreeProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const [levels, setLevels] = useState<TableGroupingLevels>(DEFAULT_GROUPING)
    const [expanded, setExpanded] = useState<string[]>([])

    // Read once, on the browser the page is opened in.
    useEffect(() => setLevels(loadGrouping()), [])

    const nodes = useMemo(() => buildTableTree(tables, activeLevels(levels)), [tables, levels])

    // Opened to the tables: a tree that shows only its groups hides everything the reader came for.
    useEffect(() => {
        const groups = (from: TableNode[]): string[] =>
            from.filter(node => node.children.length > 0).flatMap(node => [node.key, ...groups(node.children)])
        setExpanded(groups(nodes))
    }, [nodes])

    const options = [
        { value: GROUP_BY_KIND, label: t('browser.module.group_by_kind') },
        { value: GROUP_BY_TABLE_TYPE, label: t('browser.module.group_by_type') },
        { value: GROUP_BY_FILE, label: t('browser.module.group_by_file') },
        { value: GROUP_BY_CATEGORY, label: t('browser.module.group_by_category') },
        { value: GROUP_BY_NONE, label: t('browser.module.group_by_nothing') },
    ]

    const toTreeNode = (node: TableNode): TreeDataNode => ({
        key: node.key,
        title: node.title,
        icon: node.table ? <TableOutlined /> : groupIcon(levels[0]),
        selectable: node.table !== undefined,
        children: node.children.map(toTreeNode),
    })

    const findTable = (from: TableNode[], key: string): ModuleTable | undefined => {
        for (const node of from) {
            if (node.key === key) {
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
        <div className={styles.panel} data-testid="module-tables-panel">
            <div className={styles.grouping}>
                <Select
                    data-testid="module-tables-grouping"
                    options={options}
                    size="small"
                    style={{ width: '100%' }}
                    value={levels[0]}
                    onChange={level => {
                        const chosen: TableGroupingLevels = [level, GROUP_BY_NONE]
                        setLevels(chosen)
                        saveGrouping(chosen)
                    }}
                />
            </div>
            <div className={styles.body}>
                {tables.length === 0 ? (
                    <Empty data-testid="module-tables-empty" description={t('browser.module.no_tables')} />
                ) : (
                    <Tree
                        blockNode
                        showIcon
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
        </div>
    )
}

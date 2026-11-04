import React, { useState } from 'react'
import { Empty, Tree } from 'antd'
import {
    FileAddOutlined,
    FileExcelOutlined,
    FileExclamationOutlined,
    FileSyncOutlined,
    FileTextOutlined,
    SwapRightOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { Comparison, ComparisonNode } from 'types/compare'
import { useStyles } from './ComparePage.styles'

interface ComparisonTreeProps {
    comparison: Comparison
    /** Whether the elements that read the same in both files are listed. */
    showEqualElements: boolean
    /** Whether the cells outside OpenL tables are listed, such as notes or formulas beside them. */
    showOtherTables?: boolean | undefined
    onSelect: (tableId: string | null) => void
}

/**
 * Whether an element is listed: one that differs, unless the equal ones are asked for; an OpenL table, unless the
 * other tables are asked for. A table both versions of a conflict changed differently is listed whatever it holds.
 */
const listed = (element: ComparisonNode, showEqualElements: boolean, showOtherTables: boolean): boolean =>
    (showEqualElements || element.status !== 'equal') && (showOtherTables || !element.other || !!element.conflict)

/**
 * What the two compared files hold, grouped by sheet, as a tree the user picks an element from.
 *
 * A sheet is a heading rather than a choice: it is the tables under it that have two sides to show. The cells outside
 * OpenL tables, such as notes or formulas beside them, are left out unless they are asked for or both versions of a
 * conflict changed them differently.
 */
export const ComparisonTree: React.FC<ComparisonTreeProps> = ({
    comparison,
    showEqualElements,
    showOtherTables = false,
    onSelect,
}) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    /** The nodes the reader closed; every other one is open. */
    const [closed, setClosed] = useState<string[]>([])

    /**
     * The icon an element carries, saying how the second file holds it.
     *
     * Every element is a file of the workbook, drawn as one and marked with what became of it, as the
     * old window drew them. A file is gained, lost, moved, reads differently or reads the same.
     */
    const statusIcon = (node: ComparisonNode) => {
        switch (node.status) {
            case 'added':
                return <FileAddOutlined className={styles.added} title={t('status_added')} />
            case 'removed':
                return <FileExcelOutlined className={styles.removed} title={t('status_removed')} />
            case 'changed':
                return <FileExclamationOutlined className={styles.changedIcon} title={t('status_changed')} />
            case 'moved':
                return <FileSyncOutlined className={styles.changedIcon} title={t('status_moved')} />
            default:
                return <FileTextOutlined className={styles.equalIcon} title={t('status_equal')} />
        }
    }

    const label = (node: ComparisonNode) => (
        <span className={cx(node.status === 'added' && styles.added, node.status === 'removed' && styles.removed)}>
            {statusIcon(node)} {node.name}
        </span>
    )

    /** Where a table moved on purpose stood and stands now, as it reads under the table. */
    const moved = (element: ComparisonNode) => ({
        key: `${element.id}:moved`,
        title: (
            <span>
                <SwapRightOutlined className={styles.changedIcon} title={t('status_moved')} />
                {' '}
                {t('moved_places', { first: element.firstRange, second: element.secondRange })}
            </span>
        ),
        selectable: false,
        isLeaf: true,
    })

    const sheets = comparison.sheets
        .map(sheet => ({
            key: sheet.id,
            title: label(sheet),
            selectable: false,
            children: (sheet.children ?? [])
                .filter(element => listed(element, showEqualElements, showOtherTables))
                .map(element => ({
                    key: element.id,
                    title: label(element),
                    children: element.moved && element.firstRange && element.secondRange ? [moved(element)] : [],
                })),
        }))
        .filter(sheet => sheet.children.length > 0)

    if (sheets.length === 0) {
        return <Empty description={t('no_elements')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    // Everything is open unless the reader closed it. A node that appears later - a sheet the equal
    // elements bring back - opens like the rest, which it would not if the tree remembered only what
    // was open when it was first drawn.
    const branches = sheets.flatMap(sheet => [sheet.key, ...sheet.children.map(element => element.key)])

    return (
        <Tree
            expandedKeys={branches.filter(key => !closed.includes(key))}
            onExpand={open => setClosed(branches.filter(key => !open.includes(key)))}
            onSelect={keys => onSelect(keys.length > 0 ? String(keys[0]) : null)}
            treeData={sheets}
        />
    )
}

export default ComparisonTree

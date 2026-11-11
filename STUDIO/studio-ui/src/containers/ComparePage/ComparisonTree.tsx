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
import type { Comparison, ComparisonNode, ComparisonNodeStatus } from 'types/compare'
import { useStyles } from './ComparePage.styles'

interface ComparisonTreeProps {
    comparison: Comparison
    /** Whether the elements that read the same in both files are listed. */
    showEqualElements: boolean
    /** Whether the cells outside OpenL tables are listed, such as notes or formulas beside them. */
    showOtherTables?: boolean | undefined
    /** Whether a table that differs in formatting only is listed as a changed one, as it is unless told otherwise. */
    showFormattingChanges?: boolean | undefined
    onSelect: (tableId: string | null) => void
}

/**
 * How a table is listed. A table that differs in formatting only is a changed one while the changes of formatting are
 * shown. Otherwise it is listed as the table it is without them: a moved one, or an equal one.
 */
const statusShown = (element: ComparisonNode, showFormattingChanges: boolean): ComparisonNodeStatus => {
    if (showFormattingChanges || !element.formattingOnly) {
        return element.status
    }
    return element.moved ? 'moved' : 'equal'
}

/**
 * A sheet as it is listed, given its tables as they are: one whose tables all read the same once the changes of
 * formatting are left out reads the same too.
 */
const sheetShown = (sheet: ComparisonNode, tables: ComparisonNode[]): ComparisonNode =>
    (sheet.status === 'changed' && tables.every(table => table.status === 'equal') ? { ...sheet, status: 'equal' } : sheet)

/**
 * Whether a table may be listed at all: an OpenL table, unless the other tables are asked for. A table both versions
 * of a conflict changed differently may be listed whatever it holds.
 */
const offered = (element: ComparisonNode, showOtherTables: boolean): boolean =>
    showOtherTables || !element.other || !!element.conflict

/** Whether an element is listed: one that may be, and that differs, unless the equal ones are asked for. */
const listed = (element: ComparisonNode, showEqualElements: boolean, showOtherTables: boolean): boolean =>
    (showEqualElements || element.status !== 'equal') && offered(element, showOtherTables)

/**
 * Whether the formatting is all that differs among the tables that may be listed. With the changes of formatting left
 * out, the list of elements is then empty, with nothing said about why.
 *
 * The cells outside OpenL tables count only when they are asked for: showing the changes of formatting would not list
 * them otherwise.
 */
export const onlyFormattingDiffers = (comparison: Comparison, showOtherTables: boolean): boolean => {
    const tables = comparison.sheets
        .flatMap(sheet => sheet.children ?? [])
        .filter(table => offered(table, showOtherTables))
    return comparison.sheets.every(sheet => sheet.status !== 'added' && sheet.status !== 'removed')
        && tables.some(table => table.formattingOnly)
        && tables.every(table => statusShown(table, false) === 'equal')
}

/**
 * What the two compared files hold, grouped by sheet, as a tree the user picks an element from.
 *
 * A sheet is a heading rather than a choice: it is the tables under it that have two sides to show. The cells outside
 * OpenL tables, such as notes or formulas beside them, are left out unless they are asked for or both versions of a
 * conflict changed them differently.
 *
 * A table that differs in formatting only is listed as a changed one while the changes of formatting are shown.
 * Otherwise it is listed as an equal table, or as a moved one when it was moved as well.
 */
export const ComparisonTree: React.FC<ComparisonTreeProps> = ({
    comparison,
    showEqualElements,
    showOtherTables = false,
    showFormattingChanges = true,
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
        .map(sheet => {
            // Each table as it is listed, a change of formatting alone left out unless it is asked for.
            const tables = (sheet.children ?? []).map((element): ComparisonNode => (
                { ...element, status: statusShown(element, showFormattingChanges) }))
            return {
                key: sheet.id,
                title: label(sheetShown(sheet, tables)),
                selectable: false,
                children: tables
                    .filter(element => listed(element, showEqualElements, showOtherTables))
                    .map(element => ({
                        key: element.id,
                        title: label(element),
                        children: element.moved && element.firstRange && element.secondRange ? [moved(element)] : [],
                    })),
            }
        })
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

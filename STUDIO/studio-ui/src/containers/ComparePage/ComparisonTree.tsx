import React, { useState } from 'react'
import { Empty, Tree } from 'antd'
import {
    FileAddOutlined,
    FileExcelOutlined,
    FileExclamationOutlined,
    FileTextOutlined,
    TagOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { Comparison, ComparisonNode, ComparisonPropertyChange } from 'types/compare'
import { useStyles } from './ComparePage.styles'

interface ComparisonTreeProps {
    comparison: Comparison
    /** Whether the elements that read the same in both files are listed. */
    showEqualElements: boolean
    onSelect: (tableId: string | null) => void
}

/**
 * What the two compared files hold, grouped by sheet, as a tree the user picks an element from.
 *
 * A sheet is a heading rather than a choice: it is the tables under it that have two sides to show.
 */
export const ComparisonTree: React.FC<ComparisonTreeProps> = ({ comparison, showEqualElements, onSelect }) => {
    const { t } = useTranslation('compare')
    const { styles, cx } = useStyles()
    /** The nodes the reader closed; every other one is open. */
    const [closed, setClosed] = useState<string[]>([])

    /**
     * The icon an element carries, saying how the second file holds it.
     *
     * Every element is a file of the workbook, drawn as one and marked with what became of it, as the
     * old window drew them: a file gained, a file lost, a file that reads differently, a file that
     * reads the same.
     */
    const statusIcon = (node: ComparisonNode) => {
        switch (node.status) {
            case 'added':
                return <FileAddOutlined className={styles.added} title={t('status_added')} />
            case 'removed':
                return <FileExcelOutlined className={styles.removed} title={t('status_removed')} />
            case 'changed':
                return <FileExclamationOutlined className={styles.changedIcon} title={t('status_changed')} />
            default:
                return <FileTextOutlined className={styles.equalIcon} title={t('status_equal')} />
        }
    }

    const label = (node: ComparisonNode) => (
        <span className={cx(node.status === 'added' && styles.added, node.status === 'removed' && styles.removed)}>
            {statusIcon(node)} {node.name}
        </span>
    )

    /** What a property of the table says in each file, as it reads under the table. */
    const change = (element: ComparisonNode, property: ComparisonPropertyChange, index: number) => ({
        key: `${element.id}:${index}`,
        title: (
            <span>
                <TagOutlined className={styles.changedIcon} title={t('status_changed')} />
                {' '}
                {t('change', {
                    property: property.property,
                    first: property.first ?? t('change_absent'),
                    second: property.second ?? t('change_absent'),
                })}
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
                .filter(element => showEqualElements || element.status !== 'equal')
                .map(element => ({
                    key: element.id,
                    title: label(element),
                    children: (element.changes ?? []).map((property, index) => change(element, property, index)),
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

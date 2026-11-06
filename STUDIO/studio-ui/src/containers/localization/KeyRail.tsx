import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Skeleton, Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { TranslationOutlined } from '@ant-design/icons'
import { ResizeHandle, useDragSize } from '../../components/ResizeHandle'
import { useSharedStyles } from '../projects/sharedStyles'
import type { LocalizationStyles } from './LocalizationWorkspace.styles'
import type { KeyProblems, ProblemIndex } from './messageProblems'
import { useElementSize } from './useElementSize'

/** The height of one row of the tree, which the virtual list counts in. */
const ROW_HEIGHT = 24

/** The width the rail was last dragged to, kept apart from the rail of the modules. */
const WIDTH_STORAGE_KEY = 'openl.localization.rail.width'
const WIDTH = { min: 180, max: 640, fallback: 280 }

/** The key of the root of the tree, which no message key can be: it holds a control character. */
const ROOT = '\u0000all'

interface KeyRailProps {
    /** The keys of the messages, or null while they are read. */
    keys: string[] | null
    /** The key on screen, or null for all of them. */
    selectedKey: string | null
    onSelectKey: (key: string | null) => void
    /** The problems of the keys, marked on each key. */
    problems: ProblemIndex
    styles: LocalizationStyles
}

/** The classes the counts are drawn in, read once rather than for every row. */
interface Marks {
    error: string
    warning: string
}

const NONE: KeyProblems = { count: 0, error: false }

/** A row of the list: the key, and how many problems it has in the colour of the worst of them. */
const titleOf = (key: string, problems: KeyProblems, marks: Marks) => (
    <>
        {key}
        {problems.count > 0 && (
            <span className={problems.error ? marks.error : marks.warning} data-testid="localization-key-problems">
                {problems.count}
            </span>
        )}
    </>
)

/**
 * The left rail of the messages: every key in the order it is written, under the root that stands for all of them.
 *
 * <p>The root opens the table of every key in every language, a key opens that key alone. The list draws the rows
 * in sight and no more, so a bundle of thousands of keys scrolls as fast as one of ten; the key on screen is brought
 * into sight.
 */
export const KeyRail = ({
    keys,
    selectedKey,
    onSelectKey,
    problems,
    styles,
}: KeyRailProps) => {
    const { t } = useTranslation('localization')
    const { styles: shared } = useSharedStyles()
    const { size: width, startResize } = useDragSize(WIDTH_STORAGE_KEY, 'right', WIDTH)
    const { ref: bodyRef, size: body } = useElementSize<HTMLDivElement>()
    const treeRef = useRef<React.ComponentRef<typeof Tree>>(null)
    const [expanded, setExpanded] = useState<React.Key[]>([ROOT])

    // Built once per list: the classes are named by their strings, which stay while the theme does.
    const marks = useMemo(
        () => ({ error: styles.errorCount, warning: styles.warningCount }),
        [styles.errorCount, styles.warningCount]
    )
    const treeData = useMemo<TreeDataNode[]>(() => [{
        key: ROOT,
        title: t('all_messages'),
        icon: <TranslationOutlined />,
        children: (keys ?? []).map(key => ({
            key,
            title: titleOf(key, problems.byKey.get(key) ?? NONE, marks),
            isLeaf: true,
        })),
    }], [keys, problems, marks, t])

    useEffect(() => {
        if (selectedKey !== null && keys !== null) {
            setExpanded(current => (current.includes(ROOT) ? current : [...current, ROOT]))
            treeRef.current?.scrollTo({ key: selectedKey, align: 'auto' })
        }
    }, [keys, selectedKey])

    return (
        <aside className={`${shared.rail} ${styles.rail}`} data-testid="localization-rail" style={{ width }}>
            <ResizeHandle edge="right" onPointerDown={startResize} testId="localization-rail-resizer" />
            <div ref={bodyRef} className={styles.railBody}>
                {keys === null
                    ? <Skeleton active paragraph={{ rows: 8 }} title={false} />
                    : (
                        <Tree
                            ref={treeRef}
                            blockNode
                            showIcon
                            className={shared.railTree}
                            data-testid="localization-keys"
                            expandedKeys={expanded}
                            height={body.height}
                            itemHeight={ROW_HEIGHT}
                            motion={false}
                            onExpand={setExpanded}
                            onSelect={(_, info) => onSelectKey(info.node.key === ROOT ? null : String(info.node.key))}
                            selectedKeys={[selectedKey ?? ROOT]}
                            treeData={treeData}
                        />
                    )}
            </div>
        </aside>
    )
}

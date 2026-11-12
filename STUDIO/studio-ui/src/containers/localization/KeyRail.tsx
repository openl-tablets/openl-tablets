import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Skeleton, Tree } from 'antd'
import type { TreeDataNode } from 'antd'
import { TranslationOutlined } from '@ant-design/icons'
import { ResizableRail } from '../projects/ResizableRail'
import { useSharedStyles } from '../projects/sharedStyles'
import { useSearchScope } from '../../hooks/useSearchScope'

/** The height of one row of the tree, which the virtual list counts in. */
const ROW_HEIGHT = 24

/** The width the rail was last dragged to, kept apart from the rail of the modules. */
const WIDTH_STORAGE_KEY = 'openl.localization.rail.width'
const WIDTH = { min: 180, max: 640, fallback: 280 }

/** The key of the root of the tree, which no message key can be: it holds a control character. */
const ROOT = '\u0000all'

interface KeyRailProps {
    /** Every key of the messages, or null while they are read. */
    keys: string[] | null
    /** The key on screen, or null for all of them. */
    selectedKey: string | null
    onSelectKey: (key: string | null) => void
}

/**
 * The left rail of the messages: every key in the order it is written, under the root that stands for all of them.
 *
 * <p>The root opens the table of every key in every language, a key opens that key alone. The list draws the rows
 * in sight and no more, so a bundle of thousands of keys scrolls as fast as one of ten; the key on screen is brought
 * into sight.
 *
 * <p>The search of the header narrows the keys down by a piece of their names, in any case, without asking the
 * server: the rail holds every key. The table of the keys has filters of its own, in its columns.
 */
export const KeyRail = ({ keys, selectedKey, onSelectKey }: KeyRailProps) => {
    const { t } = useTranslation('localization')
    const { styles: shared } = useSharedStyles()
    const treeRef = useRef<React.ComponentRef<typeof Tree>>(null)
    const [expanded, setExpanded] = useState<React.Key[]>([ROOT])
    const [search, setSearch] = useState('')
    useSearchScope({
        id: 'messages',
        label: t('common:search.scopes.messages'),
        onChange: setSearch,
        placeholder: t('search_keys'),
        value: search,
    })

    const shown = useMemo(() => {
        const wanted = search.trim().toLowerCase()
        return wanted === '' ? keys : keys?.filter(key => key.toLowerCase().includes(wanted)) ?? null
    }, [keys, search])

    const treeData = useMemo<TreeDataNode[]>(() => [{
        key: ROOT,
        title: t('all_messages'),
        icon: <TranslationOutlined />,
        children: (shown ?? []).map(key => ({
            key,
            title: key,
            isLeaf: true,
        })),
    }], [shown, t])

    useEffect(() => {
        if (selectedKey !== null && keys !== null) {
            setExpanded(current => (current.includes(ROOT) ? current : [...current, ROOT]))
            treeRef.current?.scrollTo({ key: selectedKey, align: 'auto' })
        }
    }, [keys, selectedKey])

    return (
        <ResizableRail
            testId="localization-rail"
            width={WIDTH}
            widthKey={WIDTH_STORAGE_KEY}
        >
            {/* The list draws the rows in sight only once it knows its height: before, it would draw every key. */}
            {room => (shown === null || room.height === 0
                ? <Skeleton active paragraph={{ rows: 8 }} title={false} />
                : (
                    <Tree
                        ref={treeRef}
                        blockNode
                        showIcon
                        className={shared.railTree}
                        data-testid="localization-keys"
                        expandedKeys={expanded}
                        height={room.height}
                        itemHeight={ROW_HEIGHT}
                        motion={false}
                        onExpand={setExpanded}
                        onSelect={(_, info) => onSelectKey(info.node.key === ROOT ? null : String(info.node.key))}
                        selectedKeys={[selectedKey ?? ROOT]}
                        treeData={treeData}
                    />
                ))}
        </ResizableRail>
    )
}

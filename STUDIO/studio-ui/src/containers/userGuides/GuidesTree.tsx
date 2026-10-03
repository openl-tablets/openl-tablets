import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Tree } from 'antd'
import type { GetRef, TreeDataNode } from 'antd'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router'
import type { GuideEntry } from 'services/userGuides'
import { GUIDES_ROUTE, routeOfFile, trailTo } from './guidePaths'
import { useStyles } from './UserGuides.styles'

/** The height of a row of the tree, which draws only the rows that fit. */
const ROW_HEIGHT = 28

/** The key of an entry: its page, or, for a folder without one, the titles leading to it. */
const keyOf = (entry: GuideEntry, parentKey: string): string => entry.file ?? `${parentKey}/${entry.title}`

interface GuidesTreeData {
    nodes: TreeDataNode[]
    /** The key of every entry, to tell the folders to open on the way to a page. */
    keys: Map<GuideEntry, string>
}

/**
 * The rows of the table of contents: the root page first, then the guides, the way the documentation site lists
 * them in its sidebar.
 */
const treeOf = (contents: GuideEntry): GuidesTreeData => {
    const keys = new Map<GuideEntry, string>()
    const nodeOf = (entry: GuideEntry, parentKey: string): TreeDataNode => {
        const key = keyOf(entry, parentKey)
        keys.set(entry, key)
        return {
            key,
            title: entry.title,
            isLeaf: !entry.children?.length,
            ...(entry.children ? { children: entry.children.map(child => nodeOf(child, key)) } : {}),
        }
    }
    const rootKey = keyOf(contents, '')
    keys.set(contents, rootKey)
    const nodes = [
        { key: rootKey, title: contents.title, isLeaf: true },
        ...(contents.children ?? []).map(child => nodeOf(child, '')),
    ]
    return { nodes, keys }
}

interface GuidesTreeProps {
    contents: GuideEntry
    /** The page shown, whose folders stand open. */
    current?: GuideEntry | undefined
}

/** The table of contents of the guides, opening a page when its row is picked. */
export const GuidesTree: React.FC<GuidesTreeProps> = ({ contents, current }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const navigate = useNavigate()
    const treeRef = useRef<GetRef<typeof Tree>>(null)
    const bodyRef = useRef<HTMLDivElement>(null)
    const [height, setHeight] = useState(0)
    const tree = useMemo(() => treeOf(contents), [contents])
    const [expanded, setExpanded] = useState<React.Key[]>([])

    // The tree draws the rows that fit and no more, so it has to be told what fits.
    useEffect(() => {
        const body = bodyRef.current
        if (!body) {
            return
        }
        const observer = new ResizeObserver(entries => setHeight(Math.floor(entries[0]?.contentRect.height ?? 0)))
        observer.observe(body)
        return () => observer.disconnect()
    }, [])

    // The folders on the way to the page shown open, and the ones the reader opened stay open.
    useEffect(() => {
        if (current) {
            const trail = trailTo(contents, current).map(entry => tree.keys.get(entry)).filter(key => key !== undefined)
            setExpanded(keys => [...new Set([...keys, ...trail])])
            if (current.file) {
                treeRef.current?.scrollTo({ key: current.file, align: 'auto' })
            }
        }
    }, [contents, current, tree])

    const entries = useMemo(() => new Map([...tree.keys].map(([entry, key]) => [key, entry])), [tree])

    const open = (key: React.Key) => {
        const entry = entries.get(String(key))
        if (entry?.file) {
            void navigate(`${GUIDES_ROUTE}/${routeOfFile(entry.file)}`)
        } else {
            setExpanded(keys => (keys.includes(key) ? keys.filter(other => other !== key) : [...keys, key]))
        }
    }

    return (
        <div ref={bodyRef} className={styles.railBody}>
            <Tree
                ref={treeRef}
                blockNode
                aria-label={t('guides:contents')}
                className={styles.tree}
                expandedKeys={expanded}
                {...(height > 0 ? { height } : {})}
                itemHeight={ROW_HEIGHT}
                motion={false}
                onExpand={setExpanded}
                onSelect={(_, { node }) => open(node.key)}
                selectedKeys={current?.file ? [current.file] : []}
                treeData={tree.nodes}
            />
        </div>
    )
}

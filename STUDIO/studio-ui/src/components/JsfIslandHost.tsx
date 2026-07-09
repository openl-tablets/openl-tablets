import React, { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { ISLAND_REGISTRY } from './islandRegistry'

interface MountedIsland {
    node: HTMLElement
    key: string
    render: (dataset: DOMStringMap) => React.ReactNode
}

const ISLAND_SELECTOR = '[data-island]'

const collectIslands = (container: Element): MountedIsland[] => {
    const found: MountedIsland[] = []
    container.querySelectorAll<HTMLElement>(ISLAND_SELECTOR).forEach((node) => {
        const name = node.dataset['island']
        const render = name ? ISLAND_REGISTRY[name] : undefined
        if (render) {
            // The placeholder's data-* attributes are its identity: recreating it with different
            // attributes (e.g. another project or branch) changes the key, so React remounts the island.
            const key = JSON.stringify({ ...node.dataset })
            found.push({ node, key, render })
        }
    })
    return found
}

const sameIslands = (a: MountedIsland[], b: MountedIsland[]): boolean =>
    a.length === b.length && a.every((island, i) => island.node === b[i]?.node && island.key === b[i]?.key)

const containsIsland = (node: Node): boolean =>
    node instanceof Element && (node.matches(ISLAND_SELECTOR) || node.querySelector(ISLAND_SELECTOR) !== null)

const touchesIsland = (nodes: NodeList): boolean => Array.from(nodes).some(containsIsland)

// True when a mutation batch adds or removes a placeholder (or a subtree containing one). Island
// content re-renders and unrelated legacy churn (RichFaces AJAX, layout resizing, the table editor)
// mutate the observed subtree constantly; rescanning only on a real placeholder change keeps the
// host off that hot path.
const hasIslandChange = (records: MutationRecord[]): boolean =>
    records.some((record) => touchesIsland(record.addedNodes) || touchesIsland(record.removedNodes))

/**
 * Bridges the persistent React app into the legacy JSF pages. JSF fragments loaded into the shell
 * carry `<div data-island="<name>" data-*>` placeholders; this host (mounted once in
 * {@link DefaultLayout}) watches the `#center` shell region and portals the registered component
 * into each placeholder, so islands share the app's Ant Design, i18n, security and store context.
 *
 * A portal is dropped as soon as its placeholder leaves the DOM (navigation or panel reload),
 * which unsubscribes listeners and avoids reconciling into a detached node. Only `#appRoot` is a
 * React root; islands render through portals, never a second `createRoot`.
 */
export const JsfIslandHost: React.FC = () => {
    const [islands, setIslands] = useState<MountedIsland[]>([])

    useEffect(() => {
        // Islands live in the #center shell region (a sibling of the React root #appRoot), or the
        // document body on the rare page without it. Rescan only when a mutation actually adds or
        // removes a placeholder, so islands' own re-renders and unrelated legacy DOM churn don't walk
        // the (large, busy) subtree for nothing.
        const root = document.getElementById('center') ?? document.body
        const rescan = () => {
            const found = collectIslands(root)
            setIslands((prev) => (sameIslands(prev, found) ? prev : found))
        }
        const observer = new MutationObserver((records) => {
            if (hasIslandChange(records)) {
                rescan()
            }
        })
        observer.observe(root, { childList: true, subtree: true })
        // A placeholder may already be present when React mounts.
        rescan()
        return () => observer.disconnect()
    }, [])

    return (
        <>
            {islands.map((island) => createPortal(island.render(island.node.dataset), island.node, island.key))}
        </>
    )
}

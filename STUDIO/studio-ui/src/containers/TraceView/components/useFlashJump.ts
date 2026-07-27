import { useRef, useState } from 'react'

/**
 * Scroll-and-flash for step-reference jumps: scrolls the row carrying the given `data-rowkey` into
 * view inside the returned container and pulses it briefly, so the eye lands on the original step.
 */
export const useFlashJump = (): {
    treeRef: React.RefObject<HTMLDivElement | null>
    flashKey: string | null
    jumpToRow: (key: string) => void
} => {
    const treeRef = useRef<HTMLDivElement>(null)
    const [flashKey, setFlashKey] = useState<string | null>(null)
    const jumpToRow = (key: string): void => {
        treeRef.current?.querySelector(`[data-rowkey="${CSS.escape(key)}"]`)
            ?.scrollIntoView({ block: 'center', behavior: 'smooth' })
        setFlashKey(key)
        window.setTimeout(() => setFlashKey(prev => (prev === key ? null : prev)), 1600)
    }
    return { treeRef, flashKey, jumpToRow }
}

export default useFlashJump

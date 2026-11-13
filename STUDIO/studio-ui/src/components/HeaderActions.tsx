import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { create } from 'zustand'

const useSlot = create<{ slot: HTMLElement | null }>(() => ({ slot: null }))

// One function for the life of the module, so the top bar does not hand its slot over again on every render.
const keepSlot = (slot: HTMLElement | null) => useSlot.setState({ slot })

/** The place in the top bar, right of its tabs, where the screen shown puts its own actions. */
export const HeaderActionsSlot = ({ className }: { className?: string }) => (
    <div ref={keepSlot} className={className} data-testid="header-actions" />
)

/**
 * Shows the actions of a screen in the top bar while the screen is shown.
 *
 * The actions stay part of the screen: they keep its state and leave the top bar with it. Nothing is shown while
 * the top bar is not on the page.
 */
export const HeaderActions = ({ children }: { children: ReactNode }) => {
    const slot = useSlot(state => state.slot)
    return slot ? createPortal(children, slot) : null
}

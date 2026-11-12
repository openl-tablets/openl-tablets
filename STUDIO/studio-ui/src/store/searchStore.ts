import type { ReactNode } from 'react'
import { create } from 'zustand'

/** One place the search can look, of the several a control lets the reader choose between. */
export interface SearchChoice {
    value: string
    label: string
}

/**
 * The control a screen adds to the search of the header, as an icon at its right end.
 *
 * It is either pressed, with the text typed so far — the extended search of a module — or opens a list to choose
 * from — the guide to search in. A control with `choices` is a list; any other is pressed.
 */
export interface SearchAction {
    /** What the control does, shown as its tooltip and read as its name. */
    label: string
    icon: ReactNode
    onClick?: (text: string) => void
    choices?: SearchChoice[]
    /** The choice in force. The first one is the widest, so any other marks the control as narrowing the search. */
    choice?: string
    onChoice?: (value: string) => void
}

/** What a screen lets the search of the header look through: one list, tree or text of its own. */
export interface SearchScope {
    /** Tells the scopes on screen apart, and names the field for a test: `projects`, `files`, `tables`. */
    id: string
    /** What is searched, as the field names it before the text: Projects, Files, Tables. */
    label: string
    placeholder: string
    value: string
    onChange: (value: string) => void
    /** A panel beside the screen, which gives the field to the main part of the screen while that has a search. */
    secondary?: boolean
    /**
     * Sends the text off on Enter, for a scope that does not filter as it is typed. The field shows the Enter key
     * while there is a text to send.
     */
    onEnter?: (text: string) => void
    action?: SearchAction
}

interface SearchStore {
    /** The scopes of the screen shown, in the order they appeared. */
    scopes: SearchScope[]
    /** The scope the reader chose in the field, kept while it stays on screen. */
    picked: string | null
    mount: (scope: SearchScope) => void
    unmount: (id: string) => void
    pick: (id: string) => void
}

/** Whether the field would draw two scopes alike. The handlers and the icon are not compared: a screen keeps them. */
const drawnAlike = (one: SearchScope, other: SearchScope): boolean =>
    one.label === other.label
    && one.placeholder === other.placeholder
    && one.value === other.value
    && Boolean(one.secondary) === Boolean(other.secondary)
    && Boolean(one.onEnter) === Boolean(other.onEnter)
    && one.action?.label === other.action?.label
    && one.action?.choice === other.action?.choice
    && Boolean(one.action?.onClick) === Boolean(other.action?.onClick)
    && Boolean(one.action?.onChoice) === Boolean(other.action?.onChoice)
    // A screen keeps its list of choices while it is the same list, so the list itself is compared.
    && one.action?.choices === other.action?.choices

/**
 * What the screen shown offers to search, published by its parts through `useSearchScope` and drawn by the one
 * search field of the header.
 */
export const useSearchStore = create<SearchStore>(set => ({
    scopes: [],
    picked: null,
    mount: scope => set((state) => {
        const known = state.scopes.find(mounted => mounted.id === scope.id)
        if (!known) {
            return { scopes: [...state.scopes, scope]}
        }
        if (drawnAlike(known, scope)) {
            return state
        }
        return { scopes: state.scopes.map(mounted => (mounted === known ? scope : mounted)) }
    }),
    unmount: id => set(state => ({
        scopes: state.scopes.filter(scope => scope.id !== id),
        picked: state.picked === id ? null : state.picked,
    })),
    pick: id => set({ picked: id }),
}))

/**
 * The scope the field searches: the one the reader chose, or else the main part of the screen, or else a panel
 * beside it. Of two alike the later one wins, being the deeper part of the screen.
 */
export const activeScope = ({ scopes, picked }: Pick<SearchStore, 'scopes' | 'picked'>): SearchScope | undefined =>
    scopes.find(scope => scope.id === picked)
    ?? scopes.findLast(scope => !scope.secondary)
    ?? scopes.at(-1)

/** The scopes as the field lists them to choose from: the main part of the screen before the panels beside it. */
export const listedScopes = (scopes: SearchScope[]): SearchScope[] =>
    [...scopes.filter(scope => !scope.secondary), ...scopes.filter(scope => scope.secondary)]

import { act } from '@testing-library/react'
import { vi } from 'vitest'
import { activeScope, type SearchScope, useSearchStore } from '../store/searchStore'

/**
 * What a screen under test handed to the search of the header: the scope of the given id, or the one the field
 * would search. A test renders the screen without the header, so it reads the scope here.
 */
export const searchScope = (id?: string): SearchScope => {
    const state = useSearchStore.getState()
    const scope = id === undefined ? activeScope(state) : state.scopes.find(known => known.id === id)
    if (!scope) {
        throw new Error(id === undefined ? 'The screen offers nothing to search' : `The screen offers no ${id} scope to the search`)
    }
    return scope
}

/** Whether the screen under test offers the scope to the search of the header. */
export const hasSearchScope = (id: string): boolean =>
    useSearchStore.getState().scopes.some(scope => scope.id === id)

/** Writes the text into the search of the header, as a reader typing it there does. */
export const searchFor = (text: string, id?: string): void => {
    act(() => searchScope(id).onChange(text))
}

/** Presses the control the screen added to the search of the header, with the text typed so far. */
export const pressSearchAction = (id?: string): void => {
    act(() => {
        const scope = searchScope(id)
        scope.action?.onClick?.(scope.value)
    })
}

/** Makes the choice in the control the screen added to the search of the header. */
export const chooseInSearch = (value: string, id?: string): void => {
    act(() => searchScope(id).action?.onChoice?.(value))
}

/** A scope as a part of a screen hands it to the search: named by its id, with a mock taking the text. */
export const searchScopeOf = (id: string, over: Partial<SearchScope> = {}): SearchScope => ({
    id,
    label: id,
    onChange: vi.fn(),
    placeholder: `Search ${id}`,
    value: '',
    ...over,
})

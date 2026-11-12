import { act, render } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { type SearchScope, useSearchStore } from '../store/searchStore'
import { useSearchScope } from './useSearchScope'
import { searchScopeOf } from '../testing/search'

const Part = ({ scope }: { scope: SearchScope | null }) => {
    useSearchScope(scope)
    return null
}

const files = (over: Partial<SearchScope> = {}): SearchScope =>
    searchScopeOf('files', { label: 'Files', placeholder: 'Search files', ...over })

const mounted = () => useSearchStore.getState().scopes

describe('useSearchScope', () => {
    beforeEach(() => useSearchStore.setState({ scopes: [], picked: null }))

    it('hands the field what the part searches, and takes it back when the part leaves the screen', () => {
        const { unmount } = render(<Part scope={files({ value: 'xml' })} />)

        expect(mounted()).toHaveLength(1)
        expect(mounted()[0]).toMatchObject({ id: 'files', label: 'Files', placeholder: 'Search files', value: 'xml' })

        unmount()
        expect(mounted()).toEqual([])
    })

    it('follows the text the part holds', () => {
        const { rerender } = render(<Part scope={files()} />)

        rerender(<Part scope={files({ value: 'rules' })} />)

        expect(mounted()[0]?.value).toBe('rules')
    })

    it('sends what is typed to the handler of the latest render', () => {
        const first = vi.fn()
        const latest = vi.fn()
        const { rerender } = render(<Part scope={files({ onChange: first })} />)
        const typed = mounted()[0]!.onChange

        rerender(<Part scope={files({ onChange: latest })} />)
        act(() => typed('x'))

        // The texts did not change, so the field was not redrawn — and still reaches the part as it is now.
        expect(mounted()[0]!.onChange).toBe(typed)
        expect(first).not.toHaveBeenCalled()
        expect(latest).toHaveBeenCalledWith('x')
    })

    it('leaves the field while the part has nothing to search, and comes back with it', () => {
        const { rerender } = render(<Part scope={files()} />)

        rerender(<Part scope={null} />)
        expect(mounted()).toEqual([])

        rerender(<Part scope={files()} />)
        expect(mounted().map(scope => scope.id)).toEqual(['files'])
    })

    it('swaps one scope for another when the part changes what it searches', () => {
        const { rerender } = render(<Part scope={files()} />)

        rerender(<Part scope={files({ id: 'revisions', label: 'Revisions' })} />)

        expect(mounted().map(scope => scope.id)).toEqual(['revisions'])
    })

    it('presses the control of the latest render with the text typed', () => {
        const first = vi.fn()
        const latest = vi.fn()
        const action = { icon: null, label: 'Extended search' }
        const { rerender } = render(<Part scope={files({ action: { ...action, onClick: first } })} />)
        const press = mounted()[0]!.action!.onClick!

        rerender(<Part scope={files({ action: { ...action, onClick: latest } })} />)
        press('greet')

        expect(first).not.toHaveBeenCalled()
        expect(latest).toHaveBeenCalledWith('greet')
    })

    it('sends the text off through the handler of the latest render, for a scope that takes Enter', () => {
        const first = vi.fn()
        const latest = vi.fn()
        const { rerender } = render(<Part scope={files({ onEnter: first })} />)
        const send = mounted()[0]!.onEnter!

        rerender(<Part scope={files({ onEnter: latest })} />)
        send('bank')

        expect(first).not.toHaveBeenCalled()
        expect(latest).toHaveBeenCalledWith('bank')
        // A scope that filters as it is typed takes no Enter.
        rerender(<Part scope={files()} />)
        expect(mounted()[0]!.onEnter).toBeUndefined()
    })

    it('hands a choice to the latest render, and offers no press for a control that only chooses', () => {
        const onChoice = vi.fn()
        const choices = [{ value: '', label: 'All Guides' }, { value: 'studio/', label: 'Studio' }]
        render(<Part scope={files({ action: { choice: '', choices, icon: null, label: 'Where', onChoice } })} />)
        const action = mounted()[0]!.action!

        expect(action.onClick).toBeUndefined()
        expect(action.choices).toEqual(choices)
        action.onChoice!('studio/')

        expect(onChoice).toHaveBeenCalledWith('studio/')
    })
})

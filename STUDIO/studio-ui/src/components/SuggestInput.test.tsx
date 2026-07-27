import { act, render } from '@testing-library/react'
import { SuggestInput } from './SuggestInput'

interface SearchConfig {
    filterOption?: boolean | ((input: string, option?: { label?: unknown }) => boolean)
    onSearch?: (value: string) => void
}

interface CapturedProps {
    showSearch?: SearchConfig
    onOpenChange?: (open: boolean) => void
}

const autoComplete = vi.fn()

vi.mock('antd', () => ({
    AutoComplete: (props: CapturedProps) => {
        autoComplete(props)
        return <input />
    },
}))

// The last render wins: earlier calls hold stale closures once state has changed.
const latest = (): CapturedProps => autoComplete.mock.calls.at(-1)![0] as CapturedProps
/** Ant Design v6 carries the filter and the keystroke callback inside `showSearch`. */
const search = (): SearchConfig => latest().showSearch ?? {}

describe('SuggestInput', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        render(<SuggestInput onChange={vi.fn()} options={[{ label: 'String', value: 'String' }]} value="String" />)
    })

    it('shows the whole list until the user types', () => {
        // Ant Design filters against the field's own value, which would open a field holding "String" on that one
        // entry. `false` disables filtering, so every option stays visible.
        expect(search().filterOption).toBe(false)
    })

    it('narrows the list from the first keystroke, case-insensitively', () => {
        act(() => search().onSearch?.('str'))

        const filter = search().filterOption
        expect(typeof filter).toBe('function')
        expect((filter as (i: string, o?: { label?: unknown }) => boolean)('str', { label: 'String' })).toBe(true)
        expect((filter as (i: string, o?: { label?: unknown }) => boolean)('str', { label: 'Integer' })).toBe(false)
    })

    it('releases the filter when the list closes, so the next open shows everything', () => {
        act(() => search().onSearch?.('str'))
        expect(search().filterOption).not.toBe(false)

        act(() => latest().onOpenChange?.(false))
        act(() => latest().onOpenChange?.(true))

        expect(search().filterOption).toBe(false)
    })

    it('keeps the filter when typing is what opened the list', () => {
        // A keystroke on a closed field both searches and opens it, in one batch — hence one `act`. Splitting them
        // would commit the search before the open reads it, which is what hides the bug this guards.
        act(() => {
            search().onSearch?.('str')
            latest().onOpenChange?.(true)
        })

        expect(search().filterOption).not.toBe(false)
    })

    it('narrows within a group instead of offering all of it', () => {
        act(() => search().onSearch?.('es'))

        const filter = search().filterOption as (input: string, option?: Record<string, unknown>) => boolean
        const group = { label: 'Simple Types', options: [{ label: 'String', value: 'String' }]}
        // A matching heading makes Ant Design take the whole group, so the heading must not match.
        expect(filter('es', group)).toBe(false)
        expect(filter('es', { label: 'Test', value: 'Test' })).toBe(true)
    })
})

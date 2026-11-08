import { act, fireEvent, render, renderHook, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { keyRowOf, languagesOf } from './bundle'
import { useStyles } from './LocalizationWorkspace.styles'
import { NO_FILTER, type MessageFilter } from './messageFilter'
import { MessagesTable } from './MessagesTable'
import { useMessageEdits } from './useMessageEdits'
import { useStyles as useTableEditorStyles } from '../modules/TableEditor.styles'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const languages = languagesOf([{ tag: '' }, { tag: 'de' }, { tag: 'fr' }], tag => tag)
const rows = [keyRowOf({ key: 'greeting', values: { '': 'Hello', de: 'Hallo' } })]

const Messages = ({ addedLanguage = null, addedKey = null, onFilter = vi.fn(), width = 2000 }: {
    addedLanguage?: string | null
    addedKey?: string | null
    onFilter?: (filter: MessageFilter) => void
    width?: number
}) => {
    const edits = useMessageEdits()
    const [filter, setFilter] = useState(NO_FILTER)
    return (
        <MessagesTable
            canWrite
            addedKey={addedKey}
            addedLanguage={addedLanguage}
            edits={edits}
            emptyText="no_keys"
            filter={filter}
            height={400}
            languages={languages}
            nameOf={tag => tag}
            onAddLanguage={vi.fn()}
            onMore={vi.fn()}
            onOpenKey={vi.fn()}
            onRemoveKey={vi.fn()}
            rows={rows}
            width={width}
            onFilter={next => {
                onFilter(next)
                setFilter(next)
            }}
        />
    )
}

describe('MessagesTable', () => {
    it.each([
        ['gives the bar room under the rows of a table narrower than its columns', 600, true],
        ['gives no room to a bar a table as wide as its columns has not', 2000, false],
    ])('%s', (_, width, sideways) => {
        const { result } = renderHook(() => useStyles())
        const { container } = render(<Messages width={width} />)

        expect(container.querySelector('.ant-table-wrapper')?.classList.contains(result.current.styles.sideways))
            .toBe(sideways)
    })

    it('lights the column of a language added for a moment, in its place among the others', () => {
        vi.useFakeTimers()
        const { result } = renderHook(() => useStyles())
        render(<Messages addedLanguage="fr" />)
        const header = (tag: string) => screen.getByTestId(`localization-column-${tag}`).closest('th')

        expect(header('fr')).toHaveClass(result.current.styles.flashed)
        expect(header('de')).not.toHaveClass(result.current.styles.flashed)
        // Nothing is held beside the keys: the languages keep their order.
        const headers = [...document.querySelectorAll('[data-testid^=localization-column-]')]
        expect(headers.map(title => title.getAttribute('data-testid'))).toEqual([
            'localization-column-default',
            'localization-column-de',
            'localization-column-fr',
        ])
        expect(header('fr')).not.toHaveClass('ant-table-cell-fix-start')

        act(() => vi.advanceTimersByTime(1600))
        expect(header('fr')).not.toHaveClass(result.current.styles.flashed)
        vi.useRealTimers()
    })

    it('lights the row of a key added for a moment', () => {
        vi.useFakeTimers()
        const { result } = renderHook(() => useStyles())
        render(<Messages addedKey="greeting" />)
        const row = () => screen.getByText('greeting').closest('.ant-table-row')

        expect(row()).toHaveClass(result.current.styles.flashedRow)

        act(() => vi.advanceTimersByTime(1600))
        expect(row()).not.toHaveClass(result.current.styles.flashedRow)
        vi.useRealTimers()
    })

    it('marks a text changed and not saved as the table of a module marks a cell written', () => {
        const { result } = renderHook(() => useTableEditorStyles(false))
        render(<Messages />)
        const cell = (text: string) => screen.getByText(text).closest('.ant-table-cell')

        fireEvent.click(screen.getByText('Hallo'))
        const field = screen.getByTestId('localization-cell-input')
        fireEvent.change(field, { target: { value: 'Guten Tag' } })
        fireEvent.blur(field)

        expect(cell('Guten Tag')).toHaveClass(result.current.styles.touched)
        expect(screen.getAllByText('Hello')[0]!.closest('.ant-table-cell')).not.toHaveClass(result.current.styles.touched)
    })

    it('orders the keys by name, up, then down, then as the files write them', async () => {
        const filtered = vi.fn()
        render(<Messages onFilter={filtered} />)
        const header = screen.getByText('key').closest('th')!

        for (const order of ['asc', 'desc', null]) {
            await userEvent.click(header)
            expect(filtered).toHaveBeenLastCalledWith({ ...NO_FILTER, order })
        }
    })

    it('narrows the keys down by a search in the column of the keys, and to those with warnings', async () => {
        const filtered = vi.fn()
        render(<Messages onFilter={filtered} />)

        await userEvent.click(screen.getByTestId('localization-key-filter'))
        // The search box wraps its input
        const search = (await screen.findByTestId('localization-key-search')).querySelector('input')!
        fireEvent.change(search, { target: { value: 'tot' } })
        fireEvent.keyDown(search, { key: 'Enter', code: 'Enter', keyCode: 13 })
        await waitFor(() => expect(filtered).toHaveBeenLastCalledWith({ ...NO_FILTER, search: 'tot' }))

        fireEvent.click(screen.getByTestId('localization-only-problems'))
        await waitFor(() => expect(filtered).toHaveBeenLastCalledWith({ ...NO_FILTER, search: 'tot', onlyProblems: true }))
    })

    it('narrows the keys down to those a language does not translate, but for the default one', async () => {
        const filtered = vi.fn()
        render(<Messages onFilter={filtered} />)
        const header = (tag: string) => screen.getByTestId(`localization-column-${tag}`).closest('th')!

        expect(header('default').querySelector('.ant-table-filter-trigger')).toBeNull()
        await userEvent.click(header('de').querySelector('.ant-table-filter-trigger')!)
        const dropdown = await screen.findByRole('menu')
        await userEvent.click(within(dropdown).getByText('not_translated'))
        await userEvent.click(document.querySelector('.ant-table-filter-dropdown-btns .ant-btn-primary')!)

        await waitFor(() => expect(filtered).toHaveBeenLastCalledWith({ ...NO_FILTER, notTranslatedInto: ['de']}))
    })
})

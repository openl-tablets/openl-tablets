import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { keyRowOf, languagesOf } from './bundle'
import { NO_FILTER } from './messageFilter'
import { MessagesTable } from './MessagesTable'
import { useMessageEdits } from './useMessageEdits'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const languages = languagesOf([{ tag: '' }, { tag: 'de' }], tag => tag)
const rows = [keyRowOf({ key: 'greeting', values: { '': 'Hello', de: 'Hallo' } })]

const Messages = ({ onAddKey, canWrite = true }: { onAddKey: (key: string, texts: Record<string, string>) => void, canWrite?: boolean }) => {
    const edits = useMessageEdits()
    return (
        <MessagesTable
            canWrite={canWrite}
            edits={edits}
            emptyText="no_keys"
            filter={NO_FILTER}
            height={400}
            keyExists={key => Promise.resolve(key === 'greeting')}
            languages={languages}
            nameOf={tag => tag}
            onAddKey={onAddKey}
            onAddLanguage={vi.fn()}
            onFilter={vi.fn()}
            onMore={vi.fn()}
            onOpenKey={vi.fn()}
            onRemoveKey={vi.fn()}
            rows={rows}
            width={2000}
        />
    )
}

const enter = (element: HTMLElement) => fireEvent.keyDown(element, { key: 'Enter', code: 'Enter', keyCode: 13 })

describe('NewKeyRow', () => {
    it('adds a key with its default text by Enter, and is emptied for the next key', async () => {
        const added = vi.fn()
        render(<Messages onAddKey={added} />)
        const key = screen.getByTestId('localization-new-key')
        const text = screen.getByTestId('localization-new-key-text')

        fireEvent.change(key, { target: { value: ' order.total ' } })
        enter(key)
        expect(text).toHaveFocus()
        fireEvent.change(text, { target: { value: 'Total' } })
        await act(async () => enter(text))

        expect(added).toHaveBeenCalledWith('order.total', { '': 'Total' })
        expect(key).toHaveValue('')
        expect(text).toHaveValue('')
        expect(key).toHaveFocus()
    })

    it('adds a key with its text in every language written, a language left empty falling back', async () => {
        const added = vi.fn()
        render(<Messages onAddKey={added} />)

        fireEvent.change(screen.getByTestId('localization-new-key'), { target: { value: 'note' } })
        const german = screen.getByTestId('localization-new-key-text-de')
        expect(german).toHaveAttribute('placeholder', 'falls_back_when_empty')
        fireEvent.change(german, { target: { value: 'Eine Notiz' } })
        await act(async () => enter(german))

        // The default text is written even empty: the key is added to the default file.
        expect(added).toHaveBeenCalledWith('note', { '': '', de: 'Eine Notiz' })
        expect(german).toHaveValue('')
    })

    it('adds no key the messages have already, nor an empty one', async () => {
        const added = vi.fn()
        render(<Messages onAddKey={added} />)
        const key = screen.getByTestId('localization-new-key')

        await act(async () => fireEvent.click(screen.getByTestId('localization-new-key-add')))
        await waitFor(() => expect(screen.getByText('key_required')).toBeInTheDocument())

        fireEvent.change(key, { target: { value: 'greeting' } })
        await act(async () => fireEvent.click(screen.getByTestId('localization-new-key-add')))
        await waitFor(() => expect(screen.getByText('key_exists')).toBeInTheDocument())
        expect(key).toHaveValue('greeting')
        expect(added).not.toHaveBeenCalled()
    })

    it('is not offered to a reader who cannot change the messages', () => {
        render(<Messages canWrite={false} onAddKey={vi.fn()} />)
        expect(screen.queryByTestId('localization-new-key')).not.toBeInTheDocument()
    })
})

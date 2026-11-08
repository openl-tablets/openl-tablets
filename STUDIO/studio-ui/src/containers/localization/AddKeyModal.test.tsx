import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { AddKeyModal } from './AddKeyModal'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

const exists = (key: string) => Promise.resolve(key === 'greeting')

describe('AddKeyModal', () => {
    it('adds the key trimmed, with its text in the default language', async () => {
        const added = vi.fn()
        render(<AddKeyModal open exists={exists} onAdd={added} onClose={vi.fn()} />)

        await userEvent.type(await screen.findByTestId('localization-add-key-name'), '  order.total ')
        await userEvent.type(screen.getByTestId('localization-add-key-text'), 'Total')
        await userEvent.click(screen.getByTestId('localization-add-key-ok'))

        await waitFor(() => expect(added).toHaveBeenCalledWith('order.total', 'Total'))
    })

    it('offers the key a rule asks for, and adds it without a text', async () => {
        const added = vi.fn()
        render(<AddKeyModal open exists={exists} initialKey="missing.key" onAdd={added} onClose={vi.fn()} />)

        expect(await screen.findByTestId('localization-add-key-name')).toHaveValue('missing.key')
        await userEvent.click(screen.getByTestId('localization-add-key-ok'))

        await waitFor(() => expect(added).toHaveBeenCalledWith('missing.key', ''))
    })

    it('opens empty again after it is cancelled or a key is added', async () => {
        const closed = vi.fn()
        const added = vi.fn()
        const { rerender } = render(<AddKeyModal open exists={exists} onAdd={added} onClose={closed} />)
        const name = await screen.findByTestId('localization-add-key-name')

        await userEvent.type(name, 'draft.key')
        await userEvent.click(screen.getByRole('button', { name: 'common:btn.cancel' }))
        expect(closed).toHaveBeenCalled()
        rerender(<AddKeyModal exists={exists} onAdd={added} onClose={closed} open={false} />)
        rerender(<AddKeyModal open exists={exists} onAdd={added} onClose={closed} />)
        expect(await screen.findByTestId('localization-add-key-name')).toHaveValue('')

        await userEvent.type(screen.getByTestId('localization-add-key-name'), 'order.total')
        await userEvent.click(screen.getByTestId('localization-add-key-ok'))
        await waitFor(() => expect(added).toHaveBeenCalledWith('order.total', ''))
        expect(screen.getByTestId('localization-add-key-name')).toHaveValue('')
    })

    it('adds no empty key, nor one the messages have already', async () => {
        const added = vi.fn()
        render(<AddKeyModal open exists={exists} onAdd={added} onClose={vi.fn()} />)

        await userEvent.click(await screen.findByTestId('localization-add-key-ok'))
        expect(await screen.findByText('key_required')).toBeInTheDocument()

        await userEvent.type(screen.getByTestId('localization-add-key-name'), 'greeting')
        await userEvent.click(screen.getByTestId('localization-add-key-ok'))
        expect(await screen.findByText('key_exists')).toBeInTheDocument()
        expect(added).not.toHaveBeenCalled()
    })
})

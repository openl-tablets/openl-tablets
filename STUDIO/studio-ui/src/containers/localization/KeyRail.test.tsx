import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { KeyRail } from './KeyRail'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// jsdom lays nothing out: the tree is given the room a screen would give it, or none before it is measured.
const room = vi.hoisted(() => ({ height: 600 }))
vi.mock('../../hooks/useElementSize', () => ({ useElementSize: () => ({ ref: vi.fn(), size: { height: room.height, width: 280 } }) }))

const Rail = ({ keys, selectedKey = null, onSelectKey = vi.fn() }: {
    keys: string[] | null
    selectedKey?: string | null
    onSelectKey?: (key: string | null) => void
}) => <KeyRail keys={keys} onSelectKey={onSelectKey} selectedKey={selectedKey} />

const titles = () => [...screen.getByTestId('localization-keys').querySelectorAll('.ant-tree-title')]
    .map(title => title.textContent)

describe('KeyRail', () => {
    it('lists the keys under the root, which opens all of them', async () => {
        const selected = vi.fn()
        render(<Rail keys={['greeting', 'total']} onSelectKey={selected} selectedKey="total" />)
        const tree = screen.getByTestId('localization-keys')

        expect(titles()).toEqual(['all_messages', 'greeting', 'total'])
        await userEvent.click(within(tree).getByText('greeting'))
        expect(selected).toHaveBeenLastCalledWith('greeting')
        await userEvent.click(within(tree).getByText('all_messages'))
        expect(selected).toHaveBeenLastCalledWith(null)
    })

    it('waits for the keys while they are read, and for its height before it draws them', () => {
        render(<Rail keys={null} />)
        expect(screen.queryByTestId('localization-keys')).not.toBeInTheDocument()

        room.height = 0
        try {
            render(<Rail keys={['greeting']} />)
            // Without a height the list would draw every key at once.
            expect(screen.queryByTestId('localization-keys')).not.toBeInTheDocument()
        } finally {
            room.height = 600
        }
    })

    it('searches the keys it holds by a piece of their names, in any case', () => {
        render(<Rail keys={['order.total', 'order.tax', 'greeting']} />)

        fireEvent.change(screen.getByTestId('localization-search'), { target: { value: ' ORDER.T ' } })
        expect(titles()).toEqual(['all_messages', 'order.total', 'order.tax'])

        fireEvent.change(screen.getByTestId('localization-search'), { target: { value: '' } })
        expect(titles()).toEqual(['all_messages', 'order.total', 'order.tax', 'greeting'])
    })
})

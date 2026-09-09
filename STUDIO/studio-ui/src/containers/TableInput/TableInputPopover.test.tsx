import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TableInputPopover } from 'containers/TableInput/TableInputPopover'

const anchor = { left: 10, top: 20, width: 40, height: 30 }

describe('TableInputPopover', () => {
    it('hangs under the button and closes on a click outside', async () => {
        const onClose = vi.fn()
        render(
            <TableInputPopover open anchor={anchor} footer={<button>Go</button>} onClose={onClose} width={300}>
                <span>content</span>
            </TableInputPopover>
        )

        expect(screen.getByTestId('table-input-anchor')).toHaveStyle({ position: 'fixed', left: '10px', top: '20px' })
        expect(await screen.findByText('content')).toBeInTheDocument()
        await userEvent.click(document.body)
        expect(onClose).toHaveBeenCalledTimes(1)
    })

    it('stays open while the action it started is running', async () => {
        const onClose = vi.fn()
        render(
            <TableInputPopover busy open anchor={anchor} footer={<button>Go</button>} onClose={onClose} width={300}>
                <span>content</span>
            </TableInputPopover>
        )

        await screen.findByText('content')
        await userEvent.click(document.body)
        expect(onClose).not.toHaveBeenCalled()
    })
})

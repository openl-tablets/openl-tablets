import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ListFooter } from './ListFooter'

const renderFooter = (total: number, onChange = vi.fn(), pageSize = 25) => render(
    <ListFooter
        current={1}
        onChange={onChange}
        pageSize={pageSize}
        summary={<span>{total} projects</span>}
        testIdPrefix="items"
        total={total}
    />
)

describe('ListFooter', () => {
    it('shows the summary alone while the list fits one page', () => {
        renderFooter(25)

        expect(screen.getByTestId('items-footer')).toHaveTextContent('25 projects')
        expect(screen.queryByTestId('items-pagination')).toBeNull()
    })

    it('adds the page switcher beside the summary when the list needs more pages', async () => {
        const onChange = vi.fn()
        renderFooter(26, onChange)

        expect(screen.getByTestId('items-footer')).toHaveTextContent('26 projects')
        // a page of the switcher is the link of its number
        await userEvent.click(within(screen.getByTestId('items-pagination')).getByText('2'))

        expect(onChange).toHaveBeenCalledWith(2, 25)
    })

    it('keeps the page switcher while a smaller page than the one picked would need more pages', () => {
        renderFooter(30, vi.fn(), 50)

        expect(screen.getByTestId('items-pagination')).toBeInTheDocument()
    })
})

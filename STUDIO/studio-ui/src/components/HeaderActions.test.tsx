import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { HeaderActions, HeaderActionsSlot } from './HeaderActions'

describe('HeaderActions', () => {
    it('shows the actions of a screen in the top bar and takes them away with the screen', () => {
        const { rerender } = render(
            <>
                <HeaderActionsSlot />
                <HeaderActions><button type="button">New</button></HeaderActions>
            </>
        )

        expect(screen.getByTestId('header-actions').textContent).toBe('New')

        rerender(<HeaderActionsSlot />)

        expect(screen.getByTestId('header-actions').textContent).toBe('')
    })

    it('shows nothing while the top bar is not on the page', () => {
        render(<HeaderActions><button type="button">New</button></HeaderActions>)

        expect(screen.queryByText('New')).toBeNull()
    })
})

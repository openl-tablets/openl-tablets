import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SplitButton } from './SplitButton'

describe('SplitButton', () => {
    it('runs the main action on the button itself', async () => {
        const onClick = vi.fn()
        const onMenuClick = vi.fn()
        render(
            <SplitButton menu={{ items: [{ key: 'revision', label: 'Open Revision' }], onClick: onMenuClick }} onClick={onClick}>
                Open
            </SplitButton>
        )

        await userEvent.click(screen.getByRole('button', { name: 'Open' }))

        expect(onClick).toHaveBeenCalled()
        expect(onMenuClick).not.toHaveBeenCalled()
    })

    it('keeps the related actions behind the arrow', async () => {
        const onClick = vi.fn()
        const onMenuClick = vi.fn()
        render(
            <SplitButton
                arrowLabel="More"
                arrowTestId="open-more"
                menu={{ items: [{ key: 'revision', label: 'Open Revision' }], onClick: onMenuClick }}
                onClick={onClick}
            >
                Open
            </SplitButton>
        )

        await userEvent.click(screen.getByTestId('open-more'))
        await userEvent.click(await screen.findByText('Open Revision'))

        expect(onMenuClick).toHaveBeenCalledWith(expect.objectContaining({ key: 'revision' }))
        expect(onClick).not.toHaveBeenCalled()
    })
})

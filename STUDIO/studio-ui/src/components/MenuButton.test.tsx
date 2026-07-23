import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MenuButton } from './MenuButton'

describe('MenuButton', () => {
    it('opens its menu and reports the chosen entry', async () => {
        const onClick = vi.fn()
        render(
            <MenuButton menu={{ items: [{ key: 'upload', label: 'Upload files' }], onClick }}>
                Add
            </MenuButton>
        )

        await userEvent.click(screen.getByRole('button', { name: /Add/ }))
        await userEvent.click(await screen.findByText('Upload files'))

        expect(onClick).toHaveBeenCalledWith(expect.objectContaining({ key: 'upload' }))
    })
})

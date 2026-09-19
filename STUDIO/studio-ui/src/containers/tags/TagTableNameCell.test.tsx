import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { TagTableNameCell } from './TagTableNameCell'

describe('TagTableNameCell', () => {

    it('opens the field from the keyboard, so a tag is renamed without a mouse', async () => {
        render(<TagTableNameCell name="Rating" onChange={vi.fn().mockResolvedValue(true)} />)

        await userEvent.tab()
        await userEvent.keyboard('{Enter}')

        expect(await screen.findByRole('textbox')).toHaveValue('Rating')
    })

    it('reports the new name once the field is left', async () => {
        const onChange = vi.fn().mockResolvedValue(true)
        render(<TagTableNameCell name="Rating" onChange={onChange} />)

        await userEvent.click(screen.getByText('Rating'))
        const field = await screen.findByRole('textbox')
        await userEvent.clear(field)
        await userEvent.type(field, 'Coverage')
        await userEvent.tab()

        await waitFor(() => expect(onChange).toHaveBeenCalledWith('Coverage'))
    })
})

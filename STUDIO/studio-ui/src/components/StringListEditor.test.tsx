import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StringListEditor } from './StringListEditor'

const renderEditor = (values: string[]) => {
    const onChange = vi.fn()
    render(<StringListEditor addLabel="Add source" onChange={onChange} removeLabel="Remove" values={values} />)
    return onChange
}

describe('StringListEditor', () => {
    it('renders one input per value', () => {
        renderEditor(['a', 'b'])
        expect(screen.getAllByRole('textbox')).toHaveLength(2)
    })

    it('appends a blank entry when adding', async () => {
        const onChange = renderEditor(['a'])
        await userEvent.click(screen.getByRole('button', { name: /Add source/ }))
        expect(onChange).toHaveBeenCalledWith(['a', ''])
    })

    it('reports the edited value of a line', async () => {
        const onChange = renderEditor([''])
        await userEvent.type(screen.getByRole('textbox'), 'x')
        expect(onChange).toHaveBeenLastCalledWith(['x'])
    })

    it('removes a line', async () => {
        const onChange = renderEditor(['a', 'b'])
        await userEvent.click(screen.getAllByRole('button', { name: 'Remove' })[0]!)
        expect(onChange).toHaveBeenCalledWith(['b'])
    })
})

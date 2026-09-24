import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { TraceParameterValue } from 'types/trace'
import * as look from './parameterValues.styles'
import { useValueStyles, ValueCell } from './ParameterValues'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** A list of values, drawn the way a list of results draws them: the look is read once for the whole list. */
const Values: React.FC<{
    values: unknown[]
    lazy?: boolean
    onLoad?: () => Promise<TraceParameterValue | undefined>
}> = ({ values, lazy, onLoad }) => {
    const valueStyles = useValueStyles()
    return (
        <>
            {values.map((value, index) => (
                <ValueCell
                    key={index}
                    lazy={lazy}
                    onLoad={onLoad}
                    path={`value-${index}`}
                    styles={valueStyles}
                    value={value}
                />
            ))}
        </>
    )
}

describe('ValueCell', () => {
    it('reads the look of a list of values once, not once a value', () => {
        const readLook = vi.spyOn(look, 'useStyles')
        try {
            render(<Values values={Array.from({ length: 50 }, (_, index) => index)} />)

            expect(readLook).toHaveBeenCalledTimes(1)
            expect(screen.getByText('49')).toBeInTheDocument()
        } finally {
            readLook.mockRestore()
        }
    })

    it('draws a large value only as far as it is opened', async () => {
        const policies = Array.from({ length: 500 }, (_, index) => ({ id: index, drivers: [{ age: 20 + index }]}))
        const { container } = render(<Values values={[{ policies, total: 500 }]} />)
        // The tree keeps a hidden node of its own to measure lines by; only the lines in view count.
        const lines = () => container.querySelectorAll('.ant-tree-treenode:not([aria-hidden="true"])')
        const drawn = () => lines().length
        const open = async (line: number) => userEvent.click(lines()[line]!.querySelector('.ant-tree-switcher')!)

        expect(drawn()).toBe(1)

        await open(0)
        expect(drawn()).toBe(3)

        await open(1)
        // The first 100 policies of the open field are drawn, with a line that lists more; not one of their drivers is.
        expect(drawn()).toBe(104)

        await userEvent.click(screen.getByTestId('more-value-0-0'))
        expect(drawn()).toBe(204)
    })

    it('reads a value the API only referred to when it is asked for', async () => {
        const onLoad = vi.fn().mockResolvedValue({ name: 'driver', description: 'String', lazy: false, value: 'Sara' })
        render(<Values lazy onLoad={onLoad} values={[null]} />)

        await userEvent.click(screen.getByTestId('load-value-0'))

        expect(await screen.findByText('"Sara"')).toBeInTheDocument()
        expect(onLoad).toHaveBeenCalledTimes(1)
    })
})

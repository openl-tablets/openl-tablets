import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ValueLevel } from 'types/execution'
import * as look from './parameterValues.styles'
import { type ReadLines, useValueStyles, ValueCell } from './ParameterValues'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** A list of values, drawn the way a list of results draws them: the look is read once for the whole list. */
const Values: React.FC<{
    values: unknown[]
    lazy?: boolean
    readLines?: ReadLines
}> = ({ values, lazy, readLines }) => {
    const valueStyles = useValueStyles()
    return (
        <>
            {values.map((value, index) => (
                <ValueCell
                    key={index}
                    lazy={lazy}
                    path={`value-${index}`}
                    readLines={readLines}
                    styles={valueStyles}
                    value={value}
                />
            ))}
        </>
    )
}

/** The lines of the tree in view: the tree keeps a hidden node of its own to measure lines by. */
const linesIn = (container: HTMLElement) =>
    [...container.querySelectorAll('.ant-tree-treenode:not([aria-hidden="true"])')] as HTMLElement[]

/** Opens the line of the tree that reads as the given text. */
const openLine = async (container: HTMLElement, text: string) => {
    const line = linesIn(container).find(node => node.textContent?.includes(text))
    await userEvent.click(line!.querySelector('.ant-tree-switcher')!)
}

/** A value read a level at a time: a driver with two licenses, and a long list of claims. */
const LEVELS: Record<string, ValueLevel> = {
    '[]': {
        type: 'Driver',
        total: 3,
        lines: [
            { name: 'name', segment: 'name', value: 'Sara' },
            { name: 'licenses', segment: 'licenses', type: 'String[]', size: 2, elements: true },
        ],
    },
    '["licenses"]': {
        total: 2,
        elements: true,
        lines: [{ name: '[0]', segment: '0', value: 'B' }, { name: '[1]', segment: '1', value: 'C' }],
    },
}

describe('ValueCell', () => {
    it('reads a value a level at a time, as far as it is opened', async () => {
        const readLines = vi.fn((path: readonly string[]) => Promise.resolve(LEVELS[JSON.stringify(path)]!))
        const { container } = render(<Values lazy readLines={readLines} values={[null]} />)

        await userEvent.click(screen.getByTestId('load-value-0'))

        // Read, the value comes closed, the way a value read whole does: it stands for the count of its fields.
        expect(await screen.findByText('{3 fields}')).toBeInTheDocument()
        expect(screen.queryByText('"Sara"')).toBeNull()
        expect(readLines).toHaveBeenCalledTimes(1)
        expect(readLines).toHaveBeenLastCalledWith([], 0)

        // Its first level is read already: opening it asks for nothing. A field with inner structure only says
        // what it holds.
        await openLine(container, '{3 fields}')
        expect(await screen.findByText('"Sara"')).toBeInTheDocument()
        expect(screen.getByText('{2 elements}')).toBeInTheDocument()
        expect(readLines).toHaveBeenCalledTimes(1)

        await openLine(container, 'licenses')

        expect(await screen.findByText('"C"')).toBeInTheDocument()
        expect(readLines).toHaveBeenLastCalledWith(['licenses'], 0)
    })

    it('reads more lines of a level when the reader asks for them', async () => {
        const readLines = vi.fn((path: readonly string[], offset: number) => Promise.resolve(offset === 0
            ? LEVELS['[]']!
            : { total: 3, lines: [{ name: 'age', segment: 'age', value: 25 }]}))
        const { container } = render(<Values lazy readLines={readLines} values={[null]} />)
        await userEvent.click(screen.getByTestId('load-value-0'))
        await screen.findByText('{3 fields}')
        await openLine(container, '{3 fields}')

        await userEvent.click(await screen.findByTestId('more-value-0 []'))

        expect(await screen.findByText('25')).toBeInTheDocument()
        expect(readLines).toHaveBeenLastCalledWith([], 2)
        // What was read stays, and every line of the level is there: nothing is left to read.
        expect(screen.getByText('"Sara"')).toBeInTheDocument()
        expect(screen.queryByTestId('more-value-0 []')).toBeNull()
    })

    it('says why a level could not be read, and reads it again when asked', async () => {
        const readLines = vi.fn((path: readonly string[]) => (path.length === 0
            ? Promise.resolve(LEVELS['[]']!)
            : Promise.reject(new Error('The project is being compiled'))))
        const { container } = render(<Values lazy readLines={readLines} values={[null]} />)
        await userEvent.click(screen.getByTestId('load-value-0'))
        await screen.findByText('{3 fields}')
        await openLine(container, '{3 fields}')
        await screen.findByText('"Sara"')

        await openLine(container, 'licenses')

        expect(await screen.findByText('The project is being compiled')).toBeInTheDocument()
        readLines.mockImplementation((path: readonly string[]) => Promise.resolve(LEVELS[JSON.stringify(path)]!))
        await userEvent.click(screen.getByTestId('retry-value-0 ["licenses"]'))

        expect(await screen.findByText('"C"')).toBeInTheDocument()
        await waitFor(() => expect(screen.queryByText('The project is being compiled')).toBeNull())
    })

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
        const drawn = () => linesIn(container).length
        const open = async (line: number) =>
            userEvent.click(linesIn(container)[line]!.querySelector('.ant-tree-switcher')!)

        expect(drawn()).toBe(1)

        await open(0)
        expect(drawn()).toBe(3)

        await open(1)
        // The first 100 policies of the open field are drawn, with a line that lists more; not one of their drivers is.
        expect(drawn()).toBe(104)

        await userEvent.click(screen.getByTestId('more-value-0-0'))
        expect(drawn()).toBe(204)
    })

    it('draws a value that opens into no lines as it came with its first read', async () => {
        const id = '00000000-0000-0000-0000-000000000001'
        const readLines = vi.fn((_path: readonly string[], _offset: number) =>
            Promise.resolve<ValueLevel>({ type: 'UUID', total: 0, lines: [], value: id }))
        const { container } = render(<Values lazy readLines={readLines} values={[null]} />)

        await userEvent.click(screen.getByTestId('load-value-0'))

        expect(await screen.findByText(`"${id}"`)).toBeInTheDocument()
        expect(linesIn(container)).toHaveLength(0)
        expect(readLines).toHaveBeenCalledTimes(1)
    })
})

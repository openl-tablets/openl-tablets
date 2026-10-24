import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FailuresOption } from './ResultOptions'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

/** Opens the list of counts and picks the one written that way. */
const pickCount = async (label: string) => {
    fireEvent.mouseDown(within(screen.getByTestId('tests-failures')).getByRole('combobox'))
    await userEvent.click(await screen.findByTitle(label))
}

describe('FailuresOption', () => {
    const onChange = vi.fn()

    beforeEach(() => {
        onChange.mockClear()
    })

    it('waits, disabled, until the failures are what is listed', async () => {
        const { rerender } = render(
            <FailuresOption failures={5} failuresOnly={false} onChange={onChange} />
        )

        expect(screen.getByTestId('tests-failures')).toHaveClass('ant-select-disabled')

        await userEvent.click(screen.getByTestId('tests-failures-only'))
        expect(onChange).toHaveBeenCalledWith({ failuresOnly: true })

        rerender(<FailuresOption failuresOnly failures={5} onChange={onChange} />)
        expect(screen.getByTestId('tests-failures')).not.toHaveClass('ant-select-disabled')
    })

    it('offers every failure of a test table, and not only the first few', async () => {
        render(<FailuresOption failuresOnly failures={5} onChange={onChange} />)

        await pickCount('tests.all')

        expect(onChange).toHaveBeenCalledWith({ failures: -1 })
    })

    it('reports the count that was picked', async () => {
        render(<FailuresOption failuresOnly failures={5} onChange={onChange} />)

        await pickCount('20')

        await waitFor(() => expect(onChange).toHaveBeenCalledWith({ failures: 20 }))
    })
})

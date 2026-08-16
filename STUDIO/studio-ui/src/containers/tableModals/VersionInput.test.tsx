import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { VersionInput } from './VersionInput'

vi.mock('react-i18next', () => ({
    useTranslation: () => ({
        t: (key: string, values?: Record<string, unknown>) =>
            values?.['version'] ? `${key}:${values['version']}` : key,
    }),
}))

const renderInput = (props: Partial<React.ComponentProps<typeof VersionInput>> = {}) => {
    const onChange = vi.fn()
    render(<VersionInput data-testid="version" onChange={onChange} value="1.2.3" {...props} />)
    return onChange
}

describe('VersionInput', () => {
    it('shows the three numbers the engine orders versions by', () => {
        renderInput()

        expect(screen.getByTestId('version-0')).toHaveValue('1')
        expect(screen.getByTestId('version-1')).toHaveValue('2')
        expect(screen.getByTestId('version-2')).toHaveValue('3')
    })

    it('reads a version of another shape as the numbers it can make out', () => {
        renderInput({ value: '1.0' })

        // A version written before the format was stated still opens in the editor rather than emptying it.
        expect(screen.getByTestId('version-0')).toHaveValue('1')
        expect(screen.getByTestId('version-1')).toHaveValue('0')
        expect(screen.getByTestId('version-2')).toHaveValue('0')
    })

    it('writes back a version the engine can read', async () => {
        const user = userEvent.setup({ delay: null })
        const onChange = renderInput()

        await user.type(screen.getByTestId('version-2'), '4')

        expect(onChange).toHaveBeenLastCalledWith('1.2.34')
    })

    it('names the version the table stands for beside the numbers', () => {
        renderInput({ current: '1.2.3' })

        expect(screen.getByTestId('version-current')).toHaveTextContent('1.2.3')
    })

    it('says nothing beside the numbers when there is no version to show', () => {
        renderInput()

        expect(screen.queryByTestId('version-current')).not.toBeInTheDocument()
    })
})

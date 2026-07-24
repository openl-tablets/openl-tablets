import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { GroupProjectsModal } from './GroupProjectsModal'
import { GROUP_BY_REPOSITORY, type GroupingLevels } from './projectGrouping'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('../../components/FieldRow', () => ({
    FieldRow: ({ label, children }: Record<string, unknown>) => <div><label>{label as never}</label>{children as never}</div>,
}))

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, onCancel, okText, okButtonProps }: Record<string, unknown>) => (open
        ? (
            <div role="dialog">
                {children as never}
                <button
                    data-testid={(okButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                    onClick={onOk as never}
                    type="button"
                >
                    {okText as never}
                </button>
                <button onClick={onCancel as never} type="button">cancel</button>
            </div>
        )
        : null)
    const Select = ({ value, onChange, options, disabled, ...rest }: Record<string, unknown>) => (
        <select
            disabled={disabled as boolean}
            onChange={event => (onChange as (value: string) => void)(event.target.value)}
            value={value as string}
            {...rest}
        >
            {(options as { value: string, label: string }[]).map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
            ))}
        </select>
    )
    return { Modal, Select }
})

const renderModal = (levels: GroupingLevels, onApply = vi.fn()) => {
    render(<GroupProjectsModal open levels={levels} onApply={onApply} onClose={vi.fn()} tagTypes={['Domain', 'LOB']} />)
    return onApply
}

describe('GroupProjectsModal', () => {
    it('shows the current levels and applies what the user picked', async () => {
        const onApply = renderModal([GROUP_BY_REPOSITORY, '', ''])

        await userEvent.selectOptions(screen.getByTestId('grouping-level-2'), 'Domain')
        await userEvent.click(screen.getByTestId('grouping-apply'))

        expect(onApply).toHaveBeenCalledWith([GROUP_BY_REPOSITORY, 'Domain', ''])
    })

    it('offers each value on one level only, so the levels never repeat each other', () => {
        renderModal([GROUP_BY_REPOSITORY, 'Domain', ''])

        // The repository is taken by the first level, Domain by the second; the third offers the rest.
        const third = screen.getByTestId('grouping-level-3')
        const offered = [...third.querySelectorAll('option')].map(option => option.getAttribute('value'))
        expect(offered).toEqual(['', 'LOB'])
    })

    it('ends the grouping below a level that was cleared', async () => {
        const onApply = renderModal([GROUP_BY_REPOSITORY, 'Domain', 'LOB'])

        await userEvent.selectOptions(screen.getByTestId('grouping-level-1'), '')
        await userEvent.click(screen.getByTestId('grouping-apply'))

        // Clearing the first level pulls the ones below it out of the grouping too.
        expect(onApply).toHaveBeenCalledWith(['', '', ''])
    })

    it('disables a level until the one above it groups by something', () => {
        renderModal(['', '', ''])

        expect(screen.getByTestId('grouping-level-1')).toBeEnabled()
        expect(screen.getByTestId('grouping-level-2')).toBeDisabled()
        expect(screen.getByTestId('grouping-level-3')).toBeDisabled()
    })
})

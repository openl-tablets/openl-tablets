import { fireEvent, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { BranchSelect } from './BranchSelect'

// A testable stand-in for AntD Select: the selected value, a search box that reports typing and blur, and
// one button per option so a click reports a selection. Search config (onSearch) rides in the `showSearch`
// object, the way the current AntD Select API takes it.
vi.mock('antd', () => ({
    Select: ({ value, onChange, showSearch, onBlur, options, suffixIcon }: {
        value?: string
        onChange?: (value: string) => void
        showSearch?: boolean | { onSearch?: (value: string) => void }
        onBlur?: () => void
        options: { value: string, label: unknown }[]
        suffixIcon?: unknown
    }) => {
        const onSearch = typeof showSearch === 'object' ? showSearch.onSearch : undefined
        return (
            <div>
                {suffixIcon as never}
                <span data-testid="selected">{value ?? ''}</span>
                <input data-testid="search" onBlur={() => onBlur?.()} onChange={event => onSearch?.(event.target.value)} />
                {options.map(option => (
                    <button key={option.value} data-testid={`opt-${option.value}`} onClick={() => onChange?.(option.value)} type="button">
                        {option.label as never}
                    </button>
                ))}
            </div>
        )
    },
}))

vi.mock('@ant-design/icons', () => ({ BranchesOutlined: () => <span data-testid="branch-icon" /> }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: (_target, name) => String(name) }) }),
}))

vi.mock('./BranchMarks', () => ({
    BranchMarks: ({ isDefault, isProtected }: { isDefault?: boolean, isProtected?: boolean }) => (
        <span data-default={String(!!isDefault)} data-protected={String(!!isProtected)} data-testid="marks" />
    ),
}))

const marksOf = (name: string) => ({ isDefault: name === 'master', isProtected: name === 'release/1.0' })

describe('BranchSelect', () => {
    it('offers every branch with its marks and a branch icon', () => {
        render(<BranchSelect branchNames={['master', 'release/1.0', 'topic']} marksOf={marksOf} onChange={vi.fn()} value="master" />)

        expect(screen.getByTestId('branch-icon')).toBeInTheDocument()
        expect(within(screen.getByTestId('opt-master')).getByTestId('marks').getAttribute('data-default')).toBe('true')
        expect(within(screen.getByTestId('opt-release/1.0')).getByTestId('marks').getAttribute('data-protected')).toBe('true')
    })

    it('reports the branch a user picks', async () => {
        const onChange = vi.fn()
        render(<BranchSelect branchNames={['master', 'dev']} onChange={onChange} value="master" />)

        await userEvent.click(screen.getByTestId('opt-dev'))

        expect(onChange).toHaveBeenCalledWith('dev')
    })

    it('offers a typed new branch and takes it as the value, only when new names are allowed', () => {
        const onChange = vi.fn()
        render(<BranchSelect allowNew branchNames={['master']} onChange={onChange} value="" />)

        fireEvent.change(screen.getByTestId('search'), { target: { value: 'feature/new' } })

        // The typed name becomes a choice of its own and the value as it is typed.
        expect(screen.getByTestId('opt-feature/new')).toBeInTheDocument()
        expect(onChange).toHaveBeenCalledWith('feature/new')
    })

    it('propagates an empty value when the field is cleared', () => {
        const onChange = vi.fn()
        render(<BranchSelect allowNew branchNames={['master']} onChange={onChange} value="feature/new" />)
        const search = screen.getByTestId('search')

        fireEvent.change(search, { target: { value: 'feature/new' } })
        onChange.mockClear()
        fireEvent.change(search, { target: { value: '' } })

        expect(onChange).toHaveBeenCalledWith('')
    })

    it('does not invent a branch when new names are not allowed', () => {
        const onChange = vi.fn()
        render(<BranchSelect branchNames={['master']} onChange={onChange} value="master" />)

        fireEvent.change(screen.getByTestId('search'), { target: { value: 'feature/new' } })

        expect(screen.queryByTestId('opt-feature/new')).toBeNull()
        expect(onChange).not.toHaveBeenCalled()
    })
})

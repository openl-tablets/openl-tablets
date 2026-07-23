import { createRef } from 'react'
import type { InputRef } from 'antd'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SearchInput } from './SearchInput'

vi.mock('./SearchInput.styles', () => ({
    useStyles: () => ({
        styles: { search: 'search-style' },
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    SearchOutlined: () => <span data-testid="search-icon" />,
}))

describe('SearchInput', () => {
    it('renders an input with the embedded search icon and placeholder', () => {
        render(<SearchInput className="extra" data-testid="search" placeholder="Find" />)
        expect(screen.getByTestId('search')).toBeInTheDocument()
        expect(screen.getByPlaceholderText('Find')).toBeInTheDocument()
        expect(screen.getByTestId('search-icon')).toBeInTheDocument()
    })

    it('forwards typing to onChange', async () => {
        const onChange = vi.fn()
        render(<SearchInput data-testid="search" onChange={onChange} />)
        await userEvent.type(screen.getByTestId('search'), 'a')
        expect(onChange).toHaveBeenCalled()
    })

    it('exposes the input through a forwarded ref', () => {
        const ref = createRef<InputRef>()
        render(<SearchInput ref={ref} data-testid="search" />)
        expect(ref.current).not.toBeNull()
        expect(screen.getByTestId('search')).toBeInTheDocument()
    })
})

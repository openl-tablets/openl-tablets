import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { TagsModal } from './TagsModal'
import { getTagTypes, updateProjectTags } from '../../services/repositories'

vi.mock('../../services/repositories', () => ({ updateProjectTags: vi.fn(), getTagTypes: vi.fn() }))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('@ant-design/icons', () => ({ DeleteOutlined: () => null, PlusOutlined: () => null }))

vi.mock('antd', () => {
    const Modal = ({ open, children, footer, title }: Record<string, unknown>) =>
        open ? <div>{title as never}{children as never}{footer as never}</div> : null
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { type, loading, block, ...dom } = rest
        void type; void loading; void block
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Flex = ({ children }: { children?: unknown }) => <div>{children as never}</div>
    const Alert = ({ title, showIcon, type, ...rest }: Record<string, unknown>) => {
        void showIcon; void type
        return <div {...rest}>{title as never}</div>
    }
    interface Opt { value: string, label?: string }
    const AutoComplete = ({ onChange, value, ...rest }: Record<string, unknown>) => {
        const { options, filterOption, style, ...dom } = rest
        void options; void filterOption; void style
        return <input onChange={event => (onChange as (v: string) => void)(event.target.value)} value={(value as string) ?? ''} {...dom} />
    }
    const Select = ({ onChange, value, options, ...rest }: Record<string, unknown>) => {
        const { allowClear, showSearch, style, placeholder, ...dom } = rest
        void allowClear; void showSearch; void style; void placeholder
        return (
            <select onChange={event => (onChange as (v: string) => void)(event.target.value)} value={(value as string) ?? ''} {...dom}>
                <option value="" />
                {(options as Opt[]).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select>
        )
    }
    return { Modal, Button, Flex, Alert, AutoComplete, Select }
})

describe('TagsModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(updateProjectTags).mockResolvedValue()
        vi.mocked(getTagTypes).mockResolvedValue([])
    })

    it('saves edited and added tags, dropping blank names', async () => {
        const onSaved = vi.fn()
        render(
            <TagsModal
                open
                initialTags={{ Region: 'EU' }}
                onClose={vi.fn()}
                onSaved={onSaved}
                projectId="p1"
            />
        )

        // Edit the existing tag's value, then add a new tag.
        await userEvent.clear(screen.getByTestId('tag-value-0'))
        await userEvent.type(screen.getByTestId('tag-value-0'), 'US')
        await userEvent.click(screen.getByTestId('tag-add'))
        await userEvent.type(screen.getByTestId('tag-name-1'), 'Team')
        await userEvent.type(screen.getByTestId('tag-value-1'), 'Payroll')
        // A blank-name row must be ignored.
        await userEvent.click(screen.getByTestId('tag-add'))
        await userEvent.type(screen.getByTestId('tag-value-2'), 'ignored')

        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(updateProjectTags).toHaveBeenCalledWith('p1', { Region: 'US', Team: 'Payroll' }))
        await waitFor(() => expect(onSaved).toHaveBeenCalled())
    })

    it('restricts a fixed tag type to its catalog values', async () => {
        vi.mocked(getTagTypes).mockResolvedValue([
            { name: 'Region', extensible: false, nullable: true, values: ['EU', 'US']},
        ])
        render(
            <TagsModal
                open
                initialTags={{ Region: 'EU' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
                projectId="p1"
            />
        )

        // The value control is a dropdown of the type's values; pick another one.
        await waitFor(() => expect(screen.getByTestId('tag-value-0').tagName).toBe('SELECT'))
        await userEvent.selectOptions(screen.getByTestId('tag-value-0'), 'US')
        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(updateProjectTags).toHaveBeenCalledWith('p1', { Region: 'US' }))
    })

    it('removes a tag row', async () => {
        render(
            <TagsModal
                open
                initialTags={{ Region: 'EU' }}
                onClose={vi.fn()}
                onSaved={vi.fn()}
                projectId="p1"
            />
        )

        await userEvent.click(screen.getByTestId('tag-remove-0'))
        await userEvent.click(screen.getByTestId('tags-save'))

        await waitFor(() => expect(updateProjectTags).toHaveBeenCalledWith('p1', {}))
    })
})

import { render, screen } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ProjectFolderInput, projectFolders } from './ProjectFolderInput'
import type { FsNode } from '../../types/files'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('@ant-design/icons', () => ({ MoreOutlined: () => null }))

vi.mock('antd', () => {
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Button = ({ onClick, icon, ...rest }: Record<string, unknown>) => (
        <button data-testid={rest['data-testid'] as string} onClick={onClick as never}>{icon as never}</button>
    )
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const AutoComplete = ({ value, onChange, options, ...rest }: Record<string, unknown>) => (
        <>
            <input
                data-testid={rest['data-testid'] as string}
                onChange={event => (onChange as (v: string) => void)(event.target.value)}
                value={value as string}
            />
            {(options as Array<{ value: string }>).map(option => (
                <button key={option.value} onClick={() => (onChange as (v: string) => void)(option.value)}>
                    {option.value}
                </button>
            ))}
        </>
    )
    return { AutoComplete, Button, Space, Tooltip }
})

vi.mock('./ProjectFolderPicker', () => ({
    ProjectFolderPicker: ({ open, onSelect }: Record<string, unknown>) => (open as boolean) ? (
        <button data-testid="picker-pick" onClick={() => (onSelect as (path: string) => void)('rules/nested')}>
            pick
        </button>
    ) : null,
}))

describe('projectFolders', () => {
    it('lists the folder paths of the project in order', () => {
        const files = [
            { path: 'rules/nested', name: 'nested', basePath: 'rules', type: 'folder' },
            { path: 'Main.xlsx', name: 'Main.xlsx', basePath: '', type: 'file' },
            { path: 'rules', name: 'rules', basePath: '', type: 'folder' },
        ] as FsNode[]

        expect(projectFolders(files)).toEqual(['rules', 'rules/nested'])
    })
})

describe('ProjectFolderInput', () => {
    const onChange = vi.fn()
    const props = { folders: ['rules', 'rules/nested'], onChange, value: '', 'data-testid': 'path' }

    beforeEach(() => vi.clearAllMocks())

    it('reports what the user types', () => {
        render(<ProjectFolderInput {...props} />)

        fireEvent.change(screen.getByTestId('path'), { target: { value: 'rules/2026' } })

        expect(onChange).toHaveBeenCalledWith('rules/2026')
    })

    it('completes from the folders that already exist', async () => {
        render(<ProjectFolderInput {...props} />)

        await userEvent.click(screen.getByRole('button', { name: 'rules/nested' }))

        expect(onChange).toHaveBeenCalledWith('rules/nested')
    })

    it('takes the folder chosen in the picker', async () => {
        render(<ProjectFolderInput {...props} />)

        await userEvent.click(screen.getByTestId('path-picker'))
        await userEvent.click(screen.getByTestId('picker-pick'))

        expect(onChange).toHaveBeenCalledWith('rules/nested')
    })
})

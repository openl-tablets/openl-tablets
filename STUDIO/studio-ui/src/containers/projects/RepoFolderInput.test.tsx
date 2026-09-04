import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { RepoFolderInput } from './RepoFolderInput'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('@ant-design/icons', () => ({ MoreOutlined: () => null }))

vi.mock('./RepoFolderPicker', () => ({
    RepoFolderPicker: ({ open, onSelect, onClose }: { open: boolean, onSelect: (path: string) => void, onClose: () => void }) => (open
        ? (
            <div data-testid="repo-folder-picker">
                <button data-testid="picker-select" onClick={() => { onSelect('rules/2026'); onClose() }} type="button">pick</button>
            </div>
        )
        : null),
}))

describe('RepoFolderInput', () => {
    it('passes what is typed to the caller', async () => {
        const onChange = vi.fn()
        render(<RepoFolderInput data-testid="folder" onChange={onChange} repositoryId="design" value="" />)

        await userEvent.type(screen.getByTestId('folder'), 'a')

        expect(onChange).toHaveBeenCalledWith('a')
    })

    it('opens the folder picker beside the field and takes the picked path', async () => {
        const onChange = vi.fn()
        render(<RepoFolderInput data-testid="folder" onChange={onChange} repositoryId="design" value="" />)

        expect(screen.queryByTestId('repo-folder-picker')).toBeNull()
        await userEvent.click(screen.getByTestId('folder-picker'))
        await userEvent.click(screen.getByTestId('picker-select'))

        expect(onChange).toHaveBeenCalledWith('rules/2026')
        expect(screen.queryByTestId('repo-folder-picker')).toBeNull()
    })
})

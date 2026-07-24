import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { ProjectFolderPicker } from './ProjectFolderPicker'

const dialogProps = vi.hoisted(() => vi.fn())

vi.mock('./FolderPickerDialog', () => ({
    FolderPickerDialog: (props: Record<string, unknown>) => {
        dialogProps(props)
        return <div data-testid="folder-picker-dialog" />
    },
    useFolderNode: () => (key: string, name: string) => ({ key, title: name }),
}))

describe('ProjectFolderPicker', () => {
    it('nests the flat folder paths into a tree', () => {
        render(<ProjectFolderPicker open folders={['rules/nested', 'rules', 'deploy']} onClose={vi.fn()} onSelect={vi.fn()} />)

        expect(screen.getByTestId('folder-picker-dialog')).toBeInTheDocument()
        expect(dialogProps.mock.calls[0]![0].treeData).toEqual([
            { key: 'deploy', title: 'deploy' },
            { key: 'rules', title: 'rules', children: [{ key: 'rules/nested', title: 'nested' }]},
        ])
    })
})

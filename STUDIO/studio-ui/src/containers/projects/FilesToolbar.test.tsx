import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FilesToolbar } from './FilesToolbar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    FileAddOutlined: () => null,
    FolderAddOutlined: () => null,
    PlusOutlined: () => null,
    UploadOutlined: () => null,
}))

vi.mock('../../components/SearchInput', () => ({
    SearchInput: ({ onChange, value, ...rest }: Record<string, unknown>) =>
        <input onChange={onChange as never} value={value as never} {...rest} />,
}))

vi.mock('../../components/MenuButton', () => {
    interface Menu { items?: Array<{ key?: string, label?: unknown }>, onClick?: (info: { key: string }) => void }
    return {
        MenuButton: ({ children, menu, ...rest }: Record<string, unknown>) => (
            <div>
                <button data-testid={rest['data-testid'] as string}>{children as never}</button>
                {(menu as Menu).items?.filter(item => item.key).map(item => (
                    <button key={item.key} onClick={() => (menu as Menu).onClick?.({ key: item.key! })}>
                        {item.label as never}
                    </button>
                ))}
            </div>
        ),
    }
})

// Each dialog is stubbed by the marker it renders when open; the toolbar only decides which one opens.
vi.mock('./NewFolderModal', () => ({
    NewFolderModal: ({ open }: { open: boolean }) => open ? <div data-testid="new-folder-modal" /> : null,
}))
vi.mock('./NewFileModal', () => ({
    NewFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="new-file-modal" /> : null,
}))
vi.mock('./UploadFileModal', () => ({
    UploadFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="upload-file-modal" /> : null,
}))

const baseProps = {
    canWrite: true,
    filter: '',
    folders: ['rules'],
    onChanged: vi.fn(),
    onCreateFolder: vi.fn(),
    onFilterChange: vi.fn(),
    projectId: 'p1',
}

describe('FilesToolbar', () => {
    beforeEach(() => vi.clearAllMocks())

    it('filters the tree as the user types', async () => {
        render(<FilesToolbar {...baseProps} />)

        await userEvent.type(screen.getByTestId('files-search'), 'x')

        expect(baseProps.onFilterChange).toHaveBeenCalledWith('x')
    })

    it.each([
        ['files-new-folder', 'new-folder-modal'],
        ['files-new-text-file', 'new-file-modal'],
        ['files-upload', 'upload-file-modal'],
    ])('opens the %s dialog from the Add menu', async (entry, dialog) => {
        render(<FilesToolbar {...baseProps} />)

        await userEvent.click(screen.getByTestId(entry))

        expect(screen.getByTestId(dialog)).toBeInTheDocument()
    })

    it('offers nothing to add without write access', () => {
        render(<FilesToolbar {...baseProps} canWrite={false} />)

        expect(screen.queryByTestId('files-add')).not.toBeInTheDocument()
        expect(screen.queryByTestId('files-new-folder')).not.toBeInTheDocument()
    })
})

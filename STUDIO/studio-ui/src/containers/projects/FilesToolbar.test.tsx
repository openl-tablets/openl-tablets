import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FilesToolbar } from './FilesToolbar'
import { createTextFile, uploadFiles } from '../../services/files'

vi.mock('../../services/files', () => ({ createTextFile: vi.fn(), uploadFiles: vi.fn() }))

vi.mock('react-i18next', () => ({
    useTranslation: () => ({ t: (key: string, options?: { path?: string }) => options?.path ? `${key}:${options.path}` : key }),
}))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }), cx: (...a: unknown[]) => a.filter(Boolean).join(' ') }),
}))

vi.mock('@ant-design/icons', () => ({
    FileAddOutlined: () => null,
    FolderAddOutlined: () => null,
    UploadOutlined: () => null,
}))

vi.mock('antd', () => {
    const Button = ({ children, ...rest }: Record<string, unknown>) => {
        const { icon, loading, size, ...dom } = rest
        void icon; void loading; void size
        return <button {...dom}>{children as never}</button>
    }
    const Search = ({ onChange, ...rest }: Record<string, unknown>) => {
        const { allowClear, ...dom } = rest
        void allowClear
        return <input {...dom} onChange={onChange as never} />
    }
    const Input = ({ onChange, onPressEnter, ...rest }: Record<string, unknown>) => {
        const { allowClear, ...dom } = rest
        void allowClear
        return (
            <input
                {...dom}
                onChange={onChange as never}
                onKeyDown={e => e.key === 'Enter' && (onPressEnter as (() => void) | undefined)?.()}
            />
        )
    }
    Input.Search = Search
    const Modal = ({ open, title, children, onOk, okButtonProps }: Record<string, unknown>) =>
        open ? (
            <div role="dialog">
                <span>{title as never}</span>
                {children as never}
                <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>ok</button>
            </div>
        ) : null
    const Alert = ({ action, message }: Record<string, unknown>) => (
        <div>
            <span>{message as never}</span>
            {action as never}
        </div>
    )
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Tooltip = ({ children }: Record<string, unknown>) => children as never
    const notification = { error: vi.fn() }
    return { Alert, Button, Input, Modal, Space, Tooltip, notification }
})

const baseProps = {
    canWrite: true,
    filter: '',
    onChanged: vi.fn(),
    onCreateFolder: vi.fn(),
    onFilterChange: vi.fn(),
    projectId: 'p1',
}

describe('FilesToolbar', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(uploadFiles).mockResolvedValue()
        vi.mocked(createTextFile).mockResolvedValue()
    })

    it('uploads the selected batch once and refreshes', async () => {
        const onChanged = vi.fn()
        render(<FilesToolbar {...baseProps} onChanged={onChanged} />)

        fireEvent.change(screen.getByTestId('files-upload-input'), {
            target: {
                files: [new File(['a'], 'a.txt'), new File(['b'], 'b.txt')],
            },
        })

        await waitFor(() => expect(uploadFiles).toHaveBeenCalledTimes(1))
        const [projectId, target, files] = vi.mocked(uploadFiles).mock.calls[0]!
        expect(projectId).toBe('p1')
        expect(target).toBe('')
        expect(files).toHaveLength(2)
        await waitFor(() => expect(onChanged).toHaveBeenCalled())
    })

    it('uploads files into the selected target folder', async () => {
        render(<FilesToolbar {...baseProps} targetFolder="rules" />)

        fireEvent.change(screen.getByTestId('files-upload-input'), {
            target: {
                files: [new File(['a'], 'a.txt')],
            },
        })

        await waitFor(() => expect(uploadFiles).toHaveBeenCalledTimes(1))
        expect(vi.mocked(uploadFiles).mock.calls[0]?.[1]).toBe('rules/')
    })

    it('reports filter changes to the caller', async () => {
        const onFilterChange = vi.fn()
        render(<FilesToolbar {...baseProps} onFilterChange={onFilterChange} />)

        await userEvent.type(screen.getByTestId('files-search'), 'x')

        expect(onFilterChange).toHaveBeenCalledWith('x')
    })

    it('adds a folder virtually without calling the server', async () => {
        const onChanged = vi.fn()
        const onCreateFolder = vi.fn()
        render(<FilesToolbar {...baseProps} onChanged={onChanged} onCreateFolder={onCreateFolder} />)

        await userEvent.click(screen.getByTestId('files-new-folder'))
        await userEvent.type(screen.getByTestId('files-folder-path'), 'sub/dir')
        await userEvent.click(screen.getByTestId('files-folder-submit'))

        await waitFor(() => expect(onCreateFolder).toHaveBeenCalledWith('sub/dir'))
        expect(createTextFile).not.toHaveBeenCalled()
        expect(onChanged).not.toHaveBeenCalled()
    })

    it('adds a folder relative to the selected target folder', async () => {
        const onCreateFolder = vi.fn()
        render(<FilesToolbar {...baseProps} onCreateFolder={onCreateFolder} targetFolder="rules" />)

        await userEvent.click(screen.getByTestId('files-new-folder'))
        expect(screen.getByText('browser.files.create_target.folder:rules')).toBeTruthy()
        await userEvent.type(screen.getByTestId('files-folder-path'), 'sub/dir')
        await userEvent.click(screen.getByTestId('files-folder-submit'))

        await waitFor(() => expect(onCreateFolder).toHaveBeenCalledWith('rules/sub/dir'))
    })

    it('can reset folder creation to the project root', async () => {
        const onCreateFolder = vi.fn()
        render(<FilesToolbar {...baseProps} onCreateFolder={onCreateFolder} targetFolder="rules" />)

        await userEvent.click(screen.getByTestId('files-new-folder'))
        await userEvent.click(screen.getByText('browser.files.create_in_root'))
        await userEvent.type(screen.getByTestId('files-folder-path'), 'sub/dir')
        await userEvent.click(screen.getByTestId('files-folder-submit'))

        await waitFor(() => expect(onCreateFolder).toHaveBeenCalledWith('sub/dir'))
    })

    it('creates an empty text file', async () => {
        const onChanged = vi.fn()
        render(<FilesToolbar {...baseProps} onChanged={onChanged} targetFolder="rules" />)

        await userEvent.click(screen.getByTestId('files-new-text-file'))
        await userEvent.type(screen.getByTestId('files-text-file-path'), 'notes.txt')
        await userEvent.click(screen.getByTestId('files-text-file-submit'))

        await waitFor(() => expect(createTextFile).toHaveBeenCalledWith('p1', 'rules/notes.txt'))
        await waitFor(() => expect(onChanged).toHaveBeenCalled())
    })

    it('hides upload and new-folder actions without write access', () => {
        render(<FilesToolbar {...baseProps} canWrite={false} />)

        expect(screen.getByTestId('files-search')).toBeInTheDocument()
        expect(screen.queryByTestId('files-upload')).not.toBeInTheDocument()
        expect(screen.queryByTestId('files-new-folder')).not.toBeInTheDocument()
        expect(screen.queryByTestId('files-new-text-file')).not.toBeInTheDocument()
    })
})

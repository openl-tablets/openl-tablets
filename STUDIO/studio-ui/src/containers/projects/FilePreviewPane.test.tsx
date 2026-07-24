import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FilePreviewPane } from './FilePreviewPane'
import {
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'

vi.mock('../../services/files', () => ({
    downloadFile: vi.fn(),
    getFileContent: vi.fn(),
    isEditableTextFile: vi.fn(),
    updateFileContent: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    CloseOutlined: () => null,
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DiffOutlined: () => null,
    DownloadOutlined: () => null,
    DragOutlined: () => null,
    EditOutlined: () => null,
    FontColorsOutlined: () => null,
    FolderOpenOutlined: () => null,
    MoreOutlined: () => null,
    SaveOutlined: () => null,
    UploadOutlined: () => null,
}))

vi.mock('./MoveFileModal', () => ({
    MoveFileModal: ({ open, mode }: { open: boolean, mode: string }) => open ? <div data-testid={`move-file-modal-${mode}`} /> : null,
}))

vi.mock('./CodeEditor', () => ({
    CodeEditor: ({ onChange, readOnly, value }: Record<string, unknown>) => (
        <textarea
            data-testid="code-editor"
            onChange={event => (onChange as (value: string) => void)(event.target.value)}
            readOnly={readOnly as boolean}
            value={value as string}
        />
    ),
}))


vi.mock('antd', () => {
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { danger, disabled, icon, loading, size, type, ...dom } = rest
        void danger; void icon; void loading; void size; void type
        return <button disabled={disabled as boolean} onClick={onClick as never} {...dom}>{children as never}</button>
    }
    const Input = ({ onChange, onPressEnter, value, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return <input onChange={onChange as never} value={value as string} {...rest} />
    }
    const Modal = ({
        cancelButtonProps,
        children,
        okButtonProps,
        okText,
        onCancel,
        onOk,
        open,
        title,
    }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <span>{title as never}</span>
            <div>{children as never}</div>
            <button
                data-testid={(cancelButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                onClick={onCancel as never}
            >
                Cancel
            </button>
            <button
                data-testid={(okButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                onClick={onOk as never}
            >
                {(okText as string | undefined) ?? 'OK'}
            </button>
        </div>
    ) : null
    const Popconfirm = ({ children, onConfirm }: Record<string, unknown>) => (
        <>
            {children as never}
            <button data-testid="popconfirm-ok" onClick={onConfirm as never} type="button">confirm</button>
        </>
    )
    interface MenuItem { key: string, label: unknown }
    const Dropdown = ({ children, menu }: Record<string, unknown>) => {
        const { items, onClick } = menu as { items?: MenuItem[], onClick?: (info: { key: string }) => void }
        return (
            <div>
                {children as never}
                {items?.map(item => (
                    <button key={item.key} data-testid={`file-${item.key}`} onClick={() => onClick?.({ key: item.key })} type="button">
                        {item.label as never}
                    </button>
                ))}
            </div>
        )
    }
    const Skeleton = () => <div>loading</div>
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const notification = { error: vi.fn() }
    return { Alert, Button, Dropdown, Input, Modal, notification, Popconfirm, Skeleton, Space, Tag, Tooltip }
})

vi.mock('./CopyFileModal', () => ({
    CopyFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="copy-file-modal" /> : null,
}))
vi.mock('./DeleteFileModal', () => ({
    DeleteFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="delete-file-modal" /> : null,
}))
vi.mock('./UpdateFileModal', () => ({
    UpdateFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="update-file-modal" /> : null,
}))

const baseProps = {
    folders: ['rules'],
    branch: 'main',
    canDelete: true,
    canWrite: true,
    onChanged: vi.fn(),
    onDeleted: vi.fn(),
    onMoved: vi.fn(),
    projectId: 'p1',
    projectName: 'Project',
    repositoryId: 'repo1',
}

describe('FilePreviewPane', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(isEditableTextFile).mockReturnValue(true)
        vi.mocked(getFileContent).mockImplementation((_projectId, path) => {
            if (path === 'second.txt') {
                return Promise.resolve('second content')
            }
            return Promise.resolve('first content')
        })
        vi.mocked(updateFileContent).mockResolvedValue()
    })

    it('keeps the dirty file open when a file switch is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="second.txt" reloadToken={0} />)

        expect(screen.getByText('browser.files.discard_changes_title')).toBeTruthy()
        expect(screen.getByText('first.txt')).toBeTruthy()
        expect(getFileContent).not.toHaveBeenCalledWith('p1', 'second.txt')

        await userEvent.click(screen.getByTestId('file-discard-cancel'))

        expect(screen.queryByText('browser.files.discard_changes_title')).toBeNull()
        expect(screen.getByText('first.txt')).toBeTruthy()
        expect(screen.getByDisplayValue('modified content')).toBeTruthy()
    })

    it('loads the selected file after dirty changes are discarded', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="second.txt" reloadToken={0} />)
        await userEvent.click(screen.getByTestId('file-discard-confirm'))

        await waitFor(() => expect(getFileContent).toHaveBeenCalledWith('p1', 'second.txt'))
        await screen.findByDisplayValue('second content')
        expect(screen.getByText('second.txt')).toBeTruthy()
    })

    it('keeps dirty content when a reload is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={1} />)

        expect(screen.getByText('browser.files.discard_changes_title')).toBeTruthy()
        expect(getFileContent).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('file-discard-cancel'))

        expect(screen.queryByText('browser.files.discard_changes_title')).toBeNull()
        expect(screen.getByDisplayValue('modified content')).toBeTruthy()
        expect(getFileContent).toHaveBeenCalledTimes(1)
    })

    it('shows the empty state when no file is selected', () => {
        render(<FilePreviewPane {...baseProps} path={null} />)
        expect(screen.getByTestId('file-preview-empty')).toBeTruthy()
    })

    it('shows a binary preview and triggers download', async () => {
        vi.mocked(isEditableTextFile).mockReturnValue(false)

        render(<FilePreviewPane {...baseProps} path="rules/Main.xlsx" />)

        expect(screen.getByTestId('file-preview-binary')).toBeTruthy()
        // The toolbar action and the placeholder both offer the download; use the toolbar one.
        await userEvent.click(screen.getByTestId('file-download'))
        expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx')
    })

    it('saves edited text content', async () => {
        render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'saved content')
        await userEvent.click(screen.getByTestId('file-save'))

        await waitFor(() => expect(updateFileContent).toHaveBeenCalledWith('p1', 'first.txt', 'saved content'))
        expect(baseProps.onChanged).toHaveBeenCalled()
    })

    // Every file action opens its own dialog; one parameterized test covers the whole menu.
    it.each([
        ['delete', 'file-delete', 'delete-file-modal'],
        ['copy', 'file-copy', 'copy-file-modal'],
        ['replace', 'file-update', 'update-file-modal'],
        ['rename', 'file-rename', 'move-file-modal-rename'],
        ['move', 'file-move', 'move-file-modal-move'],
    ])('opens the %s dialog for the active file', async (_action, trigger, modal) => {
        render(<FilePreviewPane {...baseProps} path="rules/first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId(trigger))

        expect(screen.getByTestId(modal)).toBeInTheDocument()
    })

    it('surfaces load failures in the preview pane', async () => {
        vi.mocked(getFileContent).mockRejectedValue(new Error('load failed'))

        render(<FilePreviewPane {...baseProps} path="broken.txt" reloadToken={0} />)

        expect(await screen.findByTestId('file-preview-error')).toBeInTheDocument()
    })
})

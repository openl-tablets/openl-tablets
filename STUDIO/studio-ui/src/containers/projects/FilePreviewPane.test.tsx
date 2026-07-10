import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FilePreviewPane } from './FilePreviewPane'
import {
    copyFile,
    deleteFile,
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'

vi.mock('../../services/files', () => ({
    copyFile: vi.fn(),
    deleteFile: vi.fn(),
    downloadFile: vi.fn(),
    getFileContent: vi.fn(),
    isEditableTextFile: vi.fn(),
    updateFileContent: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({ styles: new Proxy({}, { get: () => '' }) }),
}))

vi.mock('@ant-design/icons', () => ({
    CloseOutlined: () => null,
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DiffOutlined: () => null,
    DownloadOutlined: () => null,
    EditOutlined: () => null,
    FolderOpenOutlined: () => null,
    SaveOutlined: () => null,
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
    const Skeleton = () => <div>loading</div>
    const Space = { Compact: ({ children }: Record<string, unknown>) => <div>{children as never}</div> }
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { showIcon, type, ...dom } = rest
        void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const notification = { error: vi.fn() }
    return { Alert, Button, Input, Modal, notification, Popconfirm, Skeleton, Space, Tag, Tooltip }
})

const baseProps = {
    branch: 'main',
    canDelete: true,
    canWrite: true,
    onChanged: vi.fn(),
    onDeleted: vi.fn(),
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
        vi.mocked(copyFile).mockResolvedValue()
        vi.mocked(deleteFile).mockResolvedValue()
        vi.mocked(updateFileContent).mockResolvedValue()
    })

    it('keeps the dirty file open when a file switch is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

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
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="second.txt" reloadToken={0} />)
        await userEvent.click(screen.getByTestId('file-discard-confirm'))

        await waitFor(() => expect(getFileContent).toHaveBeenCalledWith('p1', 'second.txt'))
        await waitFor(() => expect(screen.getByDisplayValue('second content')).toBeTruthy())
        expect(screen.getByText('second.txt')).toBeTruthy()
    })

    it('keeps dirty content when a reload is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

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
        await userEvent.click(screen.getByText('browser.files.download'))
        expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx')
    })

    it('saves edited text content', async () => {
        render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'saved content')
        await userEvent.click(screen.getByTestId('file-save'))

        await waitFor(() => expect(updateFileContent).toHaveBeenCalledWith('p1', 'first.txt', 'saved content'))
        expect(baseProps.onChanged).toHaveBeenCalled()
    })

    it('deletes the active file after confirmation', async () => {
        render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

        await userEvent.click(screen.getByTestId('file-delete'))
        await userEvent.click(screen.getByTestId('popconfirm-ok'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalledWith('p1', 'first.txt'))
        expect(baseProps.onDeleted).toHaveBeenCalled()
    })

    it('copies the active file to a suggested sibling path', async () => {
        render(<FilePreviewPane {...baseProps} path="rules/first.txt" reloadToken={0} />)
        await waitFor(() => expect(screen.getByDisplayValue('first content')).toBeTruthy())

        await userEvent.click(screen.getByTestId('file-copy'))
        await userEvent.click(screen.getByTestId('file-copy-submit'))

        await waitFor(() => expect(copyFile).toHaveBeenCalledWith('p1', 'rules/first.txt', 'rules/first-copy.txt'))
    })

    it('surfaces load failures in the preview pane', async () => {
        vi.mocked(getFileContent).mockRejectedValue(new Error('load failed'))

        render(<FilePreviewPane {...baseProps} path="broken.txt" reloadToken={0} />)

        await waitFor(() => expect(screen.getByTestId('file-preview-error')).toBeTruthy())
    })
})

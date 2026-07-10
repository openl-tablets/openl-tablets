import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FolderActionsPane } from './FolderActionsPane'
import { copyFile, deleteFile, downloadFolder } from '../../services/files'

vi.mock('../../services/files', () => ({
    copyFile: vi.fn(),
    deleteFile: vi.fn(),
    downloadFolder: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('./projectsTheme', () => ({ MOCKUP: { fontMono: 'mono' } }))

vi.mock('antd', () => {
    const Button = ({ children, onClick, icon, ...rest }: Record<string, unknown>) => {
        const { size, danger, type, ...dom } = rest
        void size; void danger; void type
        return <button onClick={onClick as never} {...dom}>{icon as never}{children as never}</button>
    }
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Space = { Compact: ({ children }: Record<string, unknown>) => <>{children as never}</> }
    const Popconfirm = ({ children, onConfirm }: Record<string, unknown>) =>
        <span onClick={onConfirm as never}>{children as never}</span>
    const Input = ({ value, onChange, ...rest }: Record<string, unknown>) => {
        const { onPressEnter, ...dom } = rest
        void onPressEnter
        return <input onChange={onChange as never} value={value as string} {...dom} />
    }
    const Modal = ({ children, open, onOk, okButtonProps }: Record<string, unknown>) =>
        open ? (
            <div>
                {children as never}
                <button data-testid={(okButtonProps as { 'data-testid'?: string })?.['data-testid']} onClick={onOk as never}>OK</button>
            </div>
        ) : null
    const notification = { error: vi.fn() }
    return { Button, Tooltip, Space, Popconfirm, Input, Modal, notification }
})

const baseProps = { projectId: 'p1', path: 'rules', canWrite: true, canDelete: true, onChanged: vi.fn(), onDeleted: vi.fn() }

describe('FolderActionsPane', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyFile).mockResolvedValue()
        vi.mocked(deleteFile).mockResolvedValue()
    })

    it('downloads the folder as an archive', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-download'))
        expect(downloadFolder).toHaveBeenCalledWith('p1', 'rules')
    })

    it('copies the folder to the suggested sibling path', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-copy'))
        await userEvent.click(screen.getByTestId('folder-copy-submit'))
        await waitFor(() => expect(copyFile).toHaveBeenCalledWith('p1', 'rules', 'rules-copy'))
    })

    it('deletes the folder and notifies the parent', async () => {
        const onDeleted = vi.fn()
        render(<FolderActionsPane {...baseProps} onDeleted={onDeleted} />)
        await userEvent.click(screen.getByTestId('folder-delete'))
        await waitFor(() => expect(deleteFile).toHaveBeenCalledWith('p1', 'rules'))
        await waitFor(() => expect(onDeleted).toHaveBeenCalled())
    })

    it('hides copy and delete without permission but keeps download', () => {
        render(<FolderActionsPane {...baseProps} canDelete={false} canWrite={false} />)
        expect(screen.queryByTestId('folder-copy')).toBeNull()
        expect(screen.queryByTestId('folder-delete')).toBeNull()
        expect(screen.getByTestId('folder-download')).toBeTruthy()
    })

    it('closes the copy dialog without copying when the target path is unchanged', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-copy'))
        await userEvent.clear(screen.getByTestId('folder-copy-input'))
        await userEvent.type(screen.getByTestId('folder-copy-input'), 'rules')
        await userEvent.click(screen.getByTestId('folder-copy-submit'))
        expect(copyFile).not.toHaveBeenCalled()
    })

    it('surfaces copy and delete failures', async () => {
        vi.mocked(copyFile).mockRejectedValue(new Error('copy failed'))
        vi.mocked(deleteFile).mockRejectedValue(new Error('delete failed'))

        render(<FolderActionsPane {...baseProps} />)

        await userEvent.click(screen.getByTestId('folder-copy'))
        await userEvent.clear(screen.getByTestId('folder-copy-input'))
        await userEvent.type(screen.getByTestId('folder-copy-input'), 'rules-copy')
        await userEvent.click(screen.getByTestId('folder-copy-submit'))
        await waitFor(() => expect(copyFile).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('folder-delete'))
    })

    it('offers only a client-side remove for a virtual folder and hits no server action', async () => {
        const onRemoveVirtual = vi.fn()
        render(<FolderActionsPane {...baseProps} virtual onRemoveVirtual={onRemoveVirtual} path="drafts/wip" />)

        // A virtual folder is not on the server: no download/copy/delete.
        expect(screen.queryByTestId('folder-download')).toBeNull()
        expect(screen.queryByTestId('folder-copy')).toBeNull()
        expect(screen.queryByTestId('folder-delete')).toBeNull()
        expect(screen.getByText('browser.files.empty_folder_hint')).toBeTruthy()

        await userEvent.click(screen.getByTestId('folder-remove-virtual'))
        expect(onRemoveVirtual).toHaveBeenCalled()
        expect(deleteFile).not.toHaveBeenCalled()
        expect(downloadFolder).not.toHaveBeenCalled()
    })
})

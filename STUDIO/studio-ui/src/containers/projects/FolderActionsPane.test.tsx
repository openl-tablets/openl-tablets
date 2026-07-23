import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FolderActionsPane } from './FolderActionsPane'
import { downloadFolder } from '../../services/files'

vi.mock('../../services/files', () => ({
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
    const Space = ({ children }: Record<string, unknown>) => <>{children as never}</>
    Space.Compact = ({ children }: Record<string, unknown>) => <>{children as never}</>
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

vi.mock('./CopyFileModal', () => ({
    CopyFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="copy-file-modal" /> : null,
}))
vi.mock('./DeleteFileModal', () => ({
    DeleteFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="delete-file-modal" /> : null,
}))

const baseProps = {
    folders: ['rules'], projectId: 'p1', path: 'rules', canWrite: true, canDelete: true, onChanged: vi.fn(), onDeleted: vi.fn() }

describe('FolderActionsPane', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('downloads the folder as an archive', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-download'))
        expect(downloadFolder).toHaveBeenCalledWith('p1', 'rules')
    })

    it('opens the copy dialog for the folder', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-copy'))
        expect(screen.getByTestId('copy-file-modal')).toBeInTheDocument()
    })

    it('asks in a dialog before deleting the folder', async () => {
        render(<FolderActionsPane {...baseProps} />)
        await userEvent.click(screen.getByTestId('folder-delete'))
        expect(screen.getByTestId('delete-file-modal')).toBeInTheDocument()
    })

    it('hides copy and delete without permission but keeps download', () => {
        render(<FolderActionsPane {...baseProps} canDelete={false} canWrite={false} />)
        expect(screen.queryByTestId('folder-copy')).toBeNull()
        expect(screen.queryByTestId('folder-delete')).toBeNull()
        expect(screen.getByTestId('folder-download')).toBeTruthy()
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
        expect(screen.queryByTestId('delete-file-modal')).toBeNull()
        expect(downloadFolder).not.toHaveBeenCalled()
    })
})

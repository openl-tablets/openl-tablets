import { act, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { FileEditorModal } from './FileEditorModal'
import { deleteFile, getFileContent, updateFileContent } from '../../services/files'

vi.mock('../../services/files', () => ({
    getFileContent: vi.fn(),
    updateFileContent: vi.fn(),
    deleteFile: vi.fn(),
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('antd', () => {
    const Modal = ({ open, children, footer, title }: Record<string, unknown>) =>
        open ? <div>{title as never}{children as never}{footer as never}</div> : null
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { type, loading, danger, ...dom } = rest
        void type; void loading; void danger
        return <button onClick={onClick as never} {...dom}>{children as never}</button>
    }
    const Popconfirm = ({ children, onConfirm }: { children?: unknown, onConfirm?: () => void }) =>
        <span onClick={onConfirm}>{children as never}</span>
    const Input = { TextArea: ({ onChange, readOnly, ...rest }: Record<string, unknown>) => (
        <textarea onChange={onChange as never} readOnly={readOnly as never} {...rest} />
    ) }
    const Alert = ({ title, showIcon, type, ...rest }: Record<string, unknown>) => {
        void showIcon; void type
        return <div {...rest}>{title as never}</div>
    }
    const Skeleton = () => <div>loading</div>
    const Space = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    const Tag = ({ children }: { children?: unknown }) => <span>{children as never}</span>
    return { Modal, Button, Popconfirm, Input, Alert, Skeleton, Space, Tag }
})

async function renderEditor(
    canWrite: boolean,
    { onSaved = vi.fn(), canDelete = false, onDeleted = vi.fn() }: { onSaved?: () => void, canDelete?: boolean, onDeleted?: () => void } = {}
) {
    await act(async () => {
        render(
            <FileEditorModal
                open
                canDelete={canDelete}
                canWrite={canWrite}
                onClose={vi.fn()}
                onDeleted={onDeleted}
                onSaved={onSaved}
                path="rules/Script.groovy"
                projectId="p1"
            />
        )
        await new Promise(resolve => setTimeout(resolve, 0))
    })
}

describe('FileEditorModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(getFileContent).mockResolvedValue('line one\nline two')
        vi.mocked(updateFileContent).mockResolvedValue()
        vi.mocked(deleteFile).mockResolvedValue()
    })

    it('loads the file content when opened', async () => {
        await renderEditor(true)
        await waitFor(() => expect(getFileContent).toHaveBeenCalledWith('p1', 'rules/Script.groovy'))
        expect((screen.getByTestId('file-editor-content') as HTMLTextAreaElement).value).toContain('line one')
    })

    it('saves edited content', async () => {
        const onSaved = vi.fn()
        await renderEditor(true, { onSaved })
        await waitFor(() => expect(getFileContent).toHaveBeenCalled())

        const textarea = screen.getByTestId('file-editor-content')
        await userEvent.clear(textarea)
        await userEvent.type(textarea, 'changed')
        await userEvent.click(screen.getByTestId('file-editor-save'))

        await waitFor(() => expect(updateFileContent).toHaveBeenCalledWith('p1', 'rules/Script.groovy', 'changed'))
        await waitFor(() => expect(onSaved).toHaveBeenCalled())
    })

    it('hides the save action without write access', async () => {
        await renderEditor(false)
        await waitFor(() => expect(getFileContent).toHaveBeenCalled())
        expect(screen.queryByTestId('file-editor-save')).toBeNull()
    })

    it('deletes the file when allowed', async () => {
        const onDeleted = vi.fn()
        await renderEditor(true, { canDelete: true, onDeleted })
        await waitFor(() => expect(getFileContent).toHaveBeenCalled())

        await userEvent.click(screen.getByTestId('file-editor-delete'))

        await waitFor(() => expect(deleteFile).toHaveBeenCalledWith('p1', 'rules/Script.groovy'))
        await waitFor(() => expect(onDeleted).toHaveBeenCalled())
    })

    it('hides the delete action without delete access', async () => {
        await renderEditor(true, { canDelete: false })
        await waitFor(() => expect(getFileContent).toHaveBeenCalled())
        expect(screen.queryByTestId('file-editor-delete')).toBeNull()
    })
})

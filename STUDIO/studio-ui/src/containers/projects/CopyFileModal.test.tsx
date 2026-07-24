import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CopyFileModal, parentFolder, suggestCopyName } from './CopyFileModal'
import { copyFile } from '../../services/files'

vi.mock('../../services/files', () => ({ copyFile: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okButtonProps, title }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <h2>{title as never}</h2>
            {children as never}
            <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>ok</button>
        </div>
    ) : null
    const Input = ({ value, onChange, onPressEnter, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return <input data-testid={rest['data-testid'] as string} onChange={onChange as never} value={value as string} />
    }
    const notification = { error: vi.fn() }
    return { Input, Modal, notification }
})

vi.mock('./ProjectFolderInput', () => ({
    ProjectFolderInput: ({ value, onChange, ...rest }: Record<string, unknown>) => (
        <input
            data-testid={rest['data-testid'] as string}
            onChange={event => (onChange as (v: string) => void)(event.target.value)}
            value={value as string}
        />
    ),
}))

vi.mock('../../components/FieldRow', () => ({
    FieldRow: ({ children, label }: Record<string, unknown>) => <label>{label as never}{children as never}</label>,
}))

const props = {
    open: true,
    projectId: 'p1',
    path: 'rules/Main.xlsx',
    folders: ['rules'],
    onClose: vi.fn(),
    onCopied: vi.fn(),
}

describe('suggestCopyName', () => {
    it('keeps the extension after the copy suffix', () => {
        expect(suggestCopyName('rules/Main.xlsx')).toBe('Main-copy.xlsx')
        expect(suggestCopyName('README')).toBe('README-copy')
    })
})

describe('parentFolder', () => {
    it('is empty for a file in the project root', () => {
        expect(parentFolder('Main.xlsx')).toBe('')
        expect(parentFolder('rules/nested/Main.xlsx')).toBe('rules/nested')
    })
})

describe('CopyFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(copyFile).mockResolvedValue()
    })

    it('starts from the copy name and the folder the file sits in', () => {
        render(<CopyFileModal {...props} />)

        expect((screen.getByTestId('file-copy-name') as HTMLInputElement).value).toBe('Main-copy.xlsx')
        expect((screen.getByTestId('file-copy-path') as HTMLInputElement).value).toBe('rules')
    })

    it('copies into the folder the user picks', async () => {
        render(<CopyFileModal {...props} />)

        fireEvent.change(screen.getByTestId('file-copy-name'), { target: { value: 'Rates.xlsx' } })
        fireEvent.change(screen.getByTestId('file-copy-path'), { target: { value: 'rules/nested' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(copyFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx', 'rules/nested/Rates.xlsx'))
        expect(props.onCopied).toHaveBeenCalled()
    })

    it('copies nothing when the destination is the file itself', async () => {
        render(<CopyFileModal {...props} />)

        fireEvent.change(screen.getByTestId('file-copy-name'), { target: { value: 'Main.xlsx' } })
        await userEvent.click(screen.getByText('ok'))

        expect(copyFile).not.toHaveBeenCalled()
        expect(props.onClose).toHaveBeenCalled()
    })

    it('names the dialog after what is being copied', () => {
        render(<CopyFileModal {...props} folder />)

        expect(screen.getByRole('heading')).toHaveTextContent('browser.files.copy_folder_title')
    })
})

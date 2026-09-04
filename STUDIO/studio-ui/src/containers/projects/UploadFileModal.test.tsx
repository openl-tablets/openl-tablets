import { render, screen, waitFor } from '@testing-library/react'
import { fireEvent } from '@testing-library/dom'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { UploadFileModal } from './UploadFileModal'
import { uploadFile, uploadFiles } from '../../services/files'

vi.mock('../../services/files', () => ({ uploadFile: vi.fn(), uploadFiles: vi.fn() }))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('@ant-design/icons', () => ({ InboxOutlined: () => null }))

vi.mock('antd', () => {
    const Modal = ({ open, children, onOk, okButtonProps }: Record<string, unknown>) => open ? (
        <div role="dialog">
            {children as never}
            <button {...(okButtonProps as Record<string, unknown>)} onClick={onOk as never}>ok</button>
        </div>
    ) : null
    const Input = ({ value, onChange, ...rest }: Record<string, unknown>) => (
        <input data-testid={rest['data-testid'] as string} onChange={onChange as never} value={value as string} />
    )
    const Upload = () => null
    // The real Dragger stages a file and reports it through onChange; beforeUpload only stops the upload.
    Upload.Dragger = ({ beforeUpload, onChange, children, ...rest }: Record<string, unknown>) => (
        <div>
            {children as never}
            <input
                data-testid={rest['data-testid'] as string}
                type="file"
                onChange={event => {
                    const picked = Array.from(event.target.files ?? [])
                    picked.forEach(file => (beforeUpload as (file: File) => void)(file))
                    ;(onChange as (info: { fileList: unknown[] }) => void)({
                        // Ant Design stages each pick as a file entry carrying its name and the raw file.
                        fileList: picked.map((file, index) => ({
                            uid: String(index),
                            name: file.name,
                            originFileObj: file,
                        })),
                    })
                }}
            />
        </div>
    )
    const notification = { error: vi.fn() }
    return { Input, Modal, Upload, notification }
})

// The path field has its own tests; here it is just an input carrying the path.
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
    folders: ['rules'],
    targetFolder: '',
    onClose: vi.fn(),
    onUploaded: vi.fn(),
}

const pick = (name: string) => fireEvent.change(screen.getByTestId('files-upload-dragger'), {
    target: { files: [new File(['x'], name)]},
})

describe('UploadFileModal', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(uploadFile).mockResolvedValue()
        vi.mocked(uploadFiles).mockResolvedValue()
    })

    it('takes the name from the picked file and uploads it into the chosen folder', async () => {
        render(<UploadFileModal {...props} />)

        pick('Rates.xlsx')
        await waitFor(() => expect((screen.getByTestId('files-upload-name') as HTMLInputElement).value)
            .toBe('Rates.xlsx'))
        fireEvent.change(screen.getByTestId('files-upload-path'), { target: { value: 'rules' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(uploadFile)
            .toHaveBeenCalledWith('p1', 'rules/', expect.any(File), 'Rates.xlsx'))
        expect(props.onUploaded).toHaveBeenCalled()
    })

    it('uploads under the name the user typed instead', async () => {
        render(<UploadFileModal {...props} />)

        pick('Rates.xlsx')
        await screen.findByTestId('files-upload-name')
        fireEvent.change(screen.getByTestId('files-upload-name'), { target: { value: 'Rates 2026.xlsx' } })
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(uploadFile)
            .toHaveBeenCalledWith('p1', '', expect.any(File), 'Rates 2026.xlsx'))
    })

    it('uploads several files at once, keeping their own names', async () => {
        render(<UploadFileModal {...props} />)

        fireEvent.change(screen.getByTestId('files-upload-dragger'), {
            target: { files: [new File(['x'], 'A.xlsx'), new File(['y'], 'B.xlsx')]},
        })
        // A batch has no single name to rename, so the name field steps aside.
        await waitFor(() => expect(screen.queryByTestId('files-upload-name')).toBeNull())
        await userEvent.click(screen.getByText('ok'))

        await waitFor(() => expect(uploadFiles).toHaveBeenCalledWith('p1', '', [expect.any(File), expect.any(File)]))
        expect(uploadFile).not.toHaveBeenCalled()
    })

    it('uploads nothing until a file is picked', async () => {
        render(<UploadFileModal {...props} />)

        await userEvent.click(screen.getByText('ok'))

        expect(uploadFile).not.toHaveBeenCalled()
    })
})

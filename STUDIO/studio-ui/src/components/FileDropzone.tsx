import type { CSSProperties, ReactNode } from 'react'
import { Upload, type UploadFile } from 'antd'
import { InboxOutlined } from '@ant-design/icons'

interface FileDropzoneProps {
    /** What the user is asked to drop here. */
    hint: ReactNode
    /** The files currently staged, so the caller keeps the single source of truth. */
    fileList: UploadFile[]
    onChange: (files: UploadFile[]) => void
    /** Accepted extensions, e.g. `['.xlsx', '.xls']`. Any file is accepted when omitted. */
    accept?: string[] | undefined
    /** Rejects a picked file before it is staged; return false to drop it. */
    validate?: ((file: File) => boolean) | undefined
    multiple?: boolean
    style?: CSSProperties | undefined
    'data-testid'?: string
}

/**
 * The drop area of every dialog that takes files. Nothing is uploaded here — a picked file is staged and
 * the dialog sends it when it is confirmed.
 */
export const FileDropzone = ({
    hint,
    fileList,
    onChange,
    accept,
    validate,
    multiple = false,
    style,
    'data-testid': testId,
}: FileDropzoneProps) => (
    <Upload.Dragger
        data-testid={testId}
        fileList={fileList}
        multiple={multiple}
        {...(multiple ? {} : { maxCount: 1 })}
        onChange={info => onChange(info.fileList)}
        {...(style ? { style } : {})}
        {...(accept ? { accept: accept.join(',') } : {})}
        beforeUpload={file => {
            // The dialog sends the file itself, so the upload is always stopped here.
            if (!validate || validate(file)) {
                return false
            }
            return Upload.LIST_IGNORE
        }}
    >
        <p className="ant-upload-drag-icon"><InboxOutlined /></p>
        <p className="ant-upload-text">{hint}</p>
    </Upload.Dragger>
)

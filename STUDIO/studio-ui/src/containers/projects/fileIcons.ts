// The icon a file of a project wears, the same one wherever its name is drawn — the Files tab and the
// projects rail both read it from here, so a workbook is never green on one screen and grey on another.

import { FileExcelOutlined, FileOutlined, FileTextOutlined, FolderOpenOutlined, FolderOutlined } from '@ant-design/icons'
import { isWorkbookPath } from '../../utils/workbooks'

/** The colours the icons are drawn in, taken from the theme by the screen that draws them. */
interface FileIconColors {
    success: string
    info: string
    warning: string
    muted: string
}

/** Icon and colour for a file or folder, chosen from the extension (green Excel, blue XML, amber props). */
export const iconFor = (name: string, isFile: boolean, colors: FileIconColors) => {
    if (!isFile) {
        return { Icon: FolderOutlined, color: colors.muted, OpenIcon: FolderOpenOutlined }
    }
    if (isWorkbookPath(name)) {
        return { Icon: FileExcelOutlined, color: colors.success }
    }
    const extension = name.slice(name.lastIndexOf('.') + 1).toLowerCase()
    if (extension === 'xml' || extension === 'json' || extension === 'yaml' || extension === 'yml') {
        return { Icon: FileTextOutlined, color: colors.info }
    }
    if (extension === 'properties') {
        return { Icon: FileTextOutlined, color: colors.warning }
    }
    return { Icon: FileOutlined, color: colors.muted }
}

import type { ReactNode } from 'react'
import {
    CopyOutlined,
    DeleteOutlined,
    DiffOutlined,
    DownloadOutlined,
    FolderOpenOutlined,
    HistoryOutlined,
    MergeOutlined,
    MinusCircleOutlined,
    RocketOutlined,
    SaveOutlined,
    UnlockOutlined,
} from '@ant-design/icons'
import type { ActionId } from './projectActions'

/**
 * The mark each action of a project wears, wherever it is offered from — the project's own screen, its row
 * in the list, or the editor of one of its modules — so the same action never reads differently.
 */
export const ACTION_ICONS: Record<ActionId, ReactNode> = {
    save: <SaveOutlined />,
    open: <FolderOpenOutlined />,
    close: <MinusCircleOutlined />,
    deploy: <RocketOutlined />,
    compare: <DiffOutlined />,
    copy: <CopyOutlined />,
    openRevision: <HistoryOutlined />,
    sync: <MergeOutlined />,
    deleteBranch: <DeleteOutlined />,
    export: <DownloadOutlined />,
    delete: <DeleteOutlined />,
    unlock: <UnlockOutlined />,
}

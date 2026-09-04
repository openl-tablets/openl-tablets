import type React from 'react'
import {
    CloudUploadOutlined,
    DeleteOutlined,
    EditOutlined,
    FolderOpenOutlined,
    FolderOutlined,
    HistoryOutlined,
} from '@ant-design/icons'
import { ProjectStatus } from './project'

/** Ant Design theme token keys used as status colours (resolved at render time). */
export type StatusColorToken =
    | 'colorPrimary'
    | 'colorSuccess'
    | 'colorWarning'
    | 'colorInfo'
    | 'colorTextSecondary'
    | 'colorTextTertiary'

export interface StatusMeta {
    tokenColor: StatusColorToken
    icon: React.ComponentType<{ className?: string; style?: React.CSSProperties; 'aria-hidden'?: boolean }>
    labelKey: string
    /** What the status means, shown by the icon marking a project name. */
    hintKey?: string
    /** Deleted projects recede: muted text, strikethrough name, dimmed row. */
    muted?: boolean
}

/**
 * Single source of truth for project status semantics — colour, icon and wording. Every screen reads the
 * status from here, so a backend constant is never turned into text anywhere else. Colours are Ant Design
 * semantic tokens, so the palette never clashes with Studio.
 */
export const STATUS_META: Record<ProjectStatus, StatusMeta> = {
    [ProjectStatus.Editing]: {
        tokenColor: 'colorPrimary',
        icon: EditOutlined,
        labelKey: 'browser.status.editing',
        hintKey: 'browser.status.editing_hint',
    },
    [ProjectStatus.Opened]: {
        tokenColor: 'colorSuccess',
        icon: FolderOpenOutlined,
        labelKey: 'browser.status.no_changes',
    },
    [ProjectStatus.ViewingVersion]: {
        tokenColor: 'colorWarning',
        icon: HistoryOutlined,
        labelKey: 'browser.status.viewing_version',
        hintKey: 'browser.status.viewing_version_hint',
    },
    [ProjectStatus.Local]: {
        tokenColor: 'colorInfo',
        icon: CloudUploadOutlined,
        labelKey: 'browser.status.local',
    },
    [ProjectStatus.Closed]: {
        tokenColor: 'colorTextTertiary',
        icon: FolderOutlined,
        labelKey: 'browser.status.closed',
    },
    [ProjectStatus.Deleted]: {
        tokenColor: 'colorTextSecondary',
        icon: DeleteOutlined,
        labelKey: 'browser.status.deleted',
        muted: true,
    },
}

/** Statuses for which the backend actually compiles a project; others are always shown as idle. */
export const COMPILE_RELEVANT_STATUSES: ReadonlySet<ProjectStatus> = new Set([
    ProjectStatus.Local,
    ProjectStatus.Opened,
    ProjectStatus.Editing,
    ProjectStatus.ViewingVersion,
])

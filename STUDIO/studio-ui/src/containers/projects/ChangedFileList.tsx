import React from 'react'
import { Flex, Listy, Space, theme, Tooltip, Typography } from 'antd'
import { RightOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { ProjectFileChange } from '../../services/projectStatus'
import { FileChangeIcon } from './FileChangeIcon'
import { basename, parentFolder } from './projectPaths'

/**
 * The height a list of projects or files grows to. A longer list scrolls and is drawn a screenful at a time, so
 * hundreds of projects or files keep the dialog and its buttons in the window.
 */
export const LIST_HEIGHT = 320

/**
 * A path on one line of a row whose other parts stay in sight. A path too long for the line loses the end of its
 * folder rather than the name of the file, and shows whole when pointed at.
 */
export const FilePath: React.FC<{ path: string }> = ({ path }) => {
    const folder = parentFolder(path)
    return (
        <Typography.Text
            ellipsis={folder ? { tooltip: path, suffix: `/${basename(path)}` } : { tooltip: path }}
            style={{ flex: 1, minWidth: 0 }}
        >
            {folder || path}
        </Typography.Text>
    )
}

interface ChangesHeaderProps {
    /** What the files belong to, such as a project. */
    name: string
    /** How many files change, as the screen says it. */
    count: string
    /** The marks shown between the name and the count. */
    children?: React.ReactNode
}

/** The head of a group of changed files: its name, its marks and how many of its files change. */
export const ChangesHeader: React.FC<ChangesHeaderProps> = ({ name, count, children }) => (
    <Space wrap size="small">
        <Typography.Text strong>{name}</Typography.Text>
        {children}
        <Typography.Text type="secondary">{count}</Typography.Text>
    </Space>
)

/** How a list compares a modified file: the tooltip of its Compare, and what Compare opens. */
export interface FileComparison<T extends ProjectFileChange> {
    hint: string
    open: (file: T) => void
    /**
     * Whether an added or a deleted file is offered Compare too, against nothing on the side that does not hold it;
     * only a modified file is otherwise.
     */
    everyFile?: boolean | undefined
}

interface ChangedFileListProps<T extends ProjectFileChange> {
    files: T[]
    /** Names the rows `<testId>-file` and their Compare `<testId>-compare`. */
    testId: string
    /** How a modified file is compared; a list without it offers no Compare. */
    compare?: FileComparison<T> | undefined
    /** What a file tells under its row. */
    details?: ((file: T) => React.ReactNode) | undefined
    /**
     * Whether the list scrolls within {@link LIST_HEIGHT}, drawing a screenful at a time; on by default. A list shown a
     * page at a time is short, so it is drawn whole.
     */
    virtual?: boolean | undefined
    /** The path of the file compared last, which the list marks so the reader sees where they were. */
    selected?: string | null | undefined
}

/**
 * The files a change touches, each named after the icon that marks it added, modified or deleted: the name of the file
 * first, then the folder it is in.
 *
 * A file that can be compared is compared by a click anywhere on its row, which ends with an arrow. A modified file
 * can always be compared; an added or a deleted file has one version only, so only where the list is told to compare
 * every file.
 */
export const ChangedFileList = <T extends ProjectFileChange>({
    files,
    testId,
    compare,
    details,
    virtual = true,
    selected,
}: ChangedFileListProps<T>) => {
    const { t } = useTranslation()
    const { token } = theme.useToken()
    const renderFile = (file: T) => {
        const comparable = !!compare && (compare.everyFile || file.type === 'modified')
        const open = () => compare?.open(file)
        const folder = parentFolder(file.path)
        // The one compared last stays marked.
        const look = {
            paddingBlock: token.paddingXS,
            paddingInline: token.paddingSM,
            borderRadius: token.borderRadius,
            background: file.path === selected ? token.controlItemBgActive : 'none',
        }
        const content = (
            <Flex vertical gap={2}>
                <Flex align="center" gap="small">
                    <FileChangeIcon title={t(`repository:browser.files.change.${file.type}`)} type={file.type} />
                    {/* The folder gives way first; a name too long for the row on its own is shortened too, so the
                        arrow stays in sight. */}
                    <Flex align="baseline" gap="small" style={{ flex: 1, minWidth: 0 }}>
                        <Typography.Text strong ellipsis={{ tooltip: basename(file.path) }} style={{ minWidth: 0 }}>
                            {basename(file.path)}
                        </Typography.Text>
                        {folder && (
                            <Typography.Text
                                ellipsis={{ tooltip: folder }}
                                style={{ flex: '1 1 0', minWidth: 0 }}
                                type="secondary"
                            >
                                {folder}
                            </Typography.Text>
                        )}
                    </Flex>
                    {comparable && (
                        <Tooltip title={compare.hint}>
                            <RightOutlined data-testid={`${testId}-compare`} />
                        </Tooltip>
                    )}
                </Flex>
                {details?.(file)}
            </Flex>
        )
        // A row that compares its file is a button, the whole of it; a button inside a form must not send it.
        return comparable
            ? (
                <button
                    aria-label={`${compare.hint}: ${file.path}`}
                    data-testid={`${testId}-file`}
                    onClick={open}
                    type="button"
                    style={{
                        ...look,
                        display: 'block',
                        width: '100%',
                        border: 0,
                        font: 'inherit',
                        color: 'inherit',
                        textAlign: 'start',
                        cursor: 'pointer',
                    }}
                >
                    {content}
                </button>
            )
            : <div data-testid={`${testId}-file`} style={look}>{content}</div>
    }
    return (
        <Listy
            {...(virtual ? { height: LIST_HEIGHT } : {})}
            itemRender={renderFile}
            items={files}
            rowKey={file => file.path}
            // The rows carry their own room, so that all of a row takes the click, and no rule between them.
            styles={{ item: { padding: 0, borderBottom: 'none', borderRadius: token.borderRadius } }}
            virtual={virtual}
        />
    )
}

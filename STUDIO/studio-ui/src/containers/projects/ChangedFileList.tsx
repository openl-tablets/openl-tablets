import React from 'react'
import { Button, Flex, Listy, Space, Tooltip, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import type { ProjectFileChange } from '../../services/projectStatus'
import { FileChangeIcon } from './FileChangeIcon'
import { ACTION_ICONS } from './projectActionIcons'
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
interface FileComparison<T extends ProjectFileChange> {
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
}

/**
 * The files a change touches, each named by its path after the icon that marks it added, modified or deleted.
 *
 * A modified file offers Compare at the end of its row, which stays in sight however long the path is. An added or a
 * deleted file has one version only, so it offers Compare only where the list is told to compare every file.
 */
export const ChangedFileList = <T extends ProjectFileChange>({
    files,
    testId,
    compare,
    details,
    virtual = true,
}: ChangedFileListProps<T>) => {
    const { t } = useTranslation()
    const renderFile = (file: T) => (
        <Flex vertical data-testid={`${testId}-file`} gap={2} style={{ paddingBlock: 4 }}>
            <Flex align="center" gap="small">
                <FileChangeIcon title={t(`repository:browser.files.change.${file.type}`)} type={file.type} />
                <FilePath path={file.path} />
                {compare && (compare.everyFile || file.type === 'modified') && (
                    <Tooltip title={compare.hint}>
                        <Button
                            data-testid={`${testId}-compare`}
                            icon={ACTION_ICONS.compare}
                            onClick={() => compare.open(file)}
                            size="small"
                            type="link"
                        >
                            {t('repository:browser.compare')}
                        </Button>
                    </Tooltip>
                )}
            </Flex>
            {details?.(file)}
        </Flex>
    )
    return (
        <Listy
            {...(virtual ? { height: LIST_HEIGHT } : {})}
            itemRender={renderFile}
            items={files}
            rowKey={file => file.path}
            virtual={virtual}
        />
    )
}

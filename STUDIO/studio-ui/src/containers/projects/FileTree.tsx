import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { Empty, Skeleton, Tree, type TreeDataNode } from 'antd'
import { createStyles, useTheme } from 'antd-style'
import type { FsNode } from '../../types/files'
import { buildFileNodes, type FileNode } from './fileNodes'
import { iconFor } from './fileIcons'
import type { ProjectFileChangeType } from '../../services/projectStatus'
import { FileChangeIcon } from './FileChangeIcon'
import { useSharedStyles } from './sharedStyles'

const useStyles = createStyles(({ css, token }) => ({
    /**
     * A deep path scrolls sideways instead of squeezing the file names: a name clipped to nothing under
     * the scrollbar is unreadable, so the pane grows to the widest row (like the projects tree does) and
     * the wrapper around the tree owns the horizontal scroll.
     */
    tree: css`
        width: max-content;
        min-width: 100%;
        padding: ${token.paddingSM}px ${token.padding}px;

        .ant-tree-list,
        .ant-tree-list-holder-inner {
            min-width: max-content;
        }

        .ant-tree-treenode {
            white-space: nowrap;
        }

        .ant-tree-node-content-wrapper {
            display: flex;
            align-items: center;
            flex: auto;
        }

        .ant-tree-title {
            flex: 1;
        }
    `,
    node: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXS}px;
    `,
    name: css`
        flex: 1;
        white-space: nowrap;
    `,
    size: css`
        flex: none;
        color: ${token.colorTextTertiary};
        font-size: 11px;
        white-space: nowrap;
    `,
}))

/**
 * Draws the tree of the project's files: the shape comes from the paths, the icons and the titles from
 * this screen. {@link virtualFolders} are folder paths that do not exist on the server yet — they are
 * shown as empty folders until a file is created inside them.
 */
const buildTreeData = (
    files: FsNode[],
    virtualFolders: string[],
    sizeByPath: Map<string, number>,
    colors: { success: string, info: string, warning: string, muted: string },
    renderTitle: (name: string, path: string, size: number | undefined) => TreeDataNode['title']
): TreeDataNode[] => {
    const draw = (nodes: FileNode[]): TreeDataNode[] => nodes.map(node => {
        const { Icon, color } = iconFor(node.name, node.isFile, colors)
        const drawn: TreeDataNode = {
            key: node.path,
            isLeaf: node.isFile,
            icon: <Icon style={{ color }} />,
            title: renderTitle(node.name, node.path, node.isFile ? sizeByPath.get(node.path) : undefined),
        }
        if (node.children) {
            drawn.children = draw(node.children)
        }
        return drawn
    })
    return draw(buildFileNodes(files.map(file => file.path), virtualFolders))
}

/** Human-readable file size in the current UI locale. */
const formatSize = (bytes?: number, locale?: string): string | null => {
    if (bytes === undefined || bytes === null) {
        return null
    }
    const kb = bytes / 1024
    const fractionDigits = kb >= 10 ? 0 : 1
    return new Intl.NumberFormat(locale, {
        style: 'unit',
        unit: 'kilobyte',
        unitDisplay: 'short',
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits,
    }).format(kb)
}

interface FileTreeProps {
    projectId: string
    files: FsNode[] | 'loading' | 'error' | undefined
    reducedMotion?: boolean
    /** Case-insensitive path filter. */
    filter?: string
    /** The currently open file's path, highlighted in the tree. */
    selectedPath?: string | null
    /** Local pending changes indexed by mount-relative or project-scoped file path. */
    changes?: Map<string, ProjectFileChangeType>
    /** Client-side folder paths not yet persisted on the server, shown as empty folders. */
    virtualFolders?: string[]
    /** Called with a selected file or folder path. */
    onSelectFile?: (path: string) => void
}

/**
 * Renders a project's files as a collapsible tree with type-coloured icons and sizes. Selecting a file
 * opens it in the preview pane; file actions live there, not on each row. A filter narrows visible files.
 */
export const FileTree = ({
    projectId,
    files,
    reducedMotion,
    filter,
    selectedPath,
    changes,
    virtualFolders,
    onSelectFile,
}: FileTreeProps) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { i18n, t } = useTranslation('repository')
    const token = useTheme()
    const locale = i18n.resolvedLanguage ?? i18n.language

    const query = (filter ?? '').trim().toLowerCase()
    const treeData = useMemo(() => {
        if (!Array.isArray(files)) {
            return []
        }
        const colors = {
            success: token.colorSuccess,
            info: token.colorInfo,
            warning: token.colorWarning,
            muted: token.colorTextTertiary,
        }
        const sizeByPath = new Map<string, number>()
        for (const node of files) {
            if (node.type === 'file' && node.size !== undefined) {
                sizeByPath.set(node.path, node.size)
            }
        }
        const visible = files.filter(node => node.type === 'file' && (!query || node.path.toLowerCase().includes(query)))
        const visibleVirtual = (virtualFolders ?? []).filter(path => !query || path.toLowerCase().includes(query))
        const renderTitle = (name: string, path: string, size: number | undefined) => {
            const label = formatSize(size, locale)
            const changeType = changes?.get(path)
            return (
                <span className={styles.node}>
                    <span className={cx(shared.ellipsis, styles.name)}>{name}</span>
                    {changeType && changeType !== 'deleted' && (
                        <FileChangeIcon
                            testId={`file-change-${changeType}-${path}`}
                            title={t(`browser.files.change.${changeType}`)}
                            type={changeType}
                        />
                    )}
                    {label && <span className={styles.size}>{label}</span>}
                </span>
            )
        }
        return buildTreeData(visible, visibleVirtual, sizeByPath, colors, renderTitle)
    }, [changes, files, locale, query, t, token, styles, virtualFolders])

    if (files === undefined || files === 'loading') {
        return <Skeleton active className={styles.tree} paragraph={{ rows: 4 }} title={false} />
    }
    if (files === 'error') {
        return <Empty description={t('browser.files_error')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }
    if (treeData.length === 0) {
        return <Empty description={query ? t('browser.files.no_match') : t('browser.no_files')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
    }

    return (
        <div className={styles.tree} data-testid={`files-${projectId}`}>
            <Tree
                blockNode
                showIcon
                defaultExpandAll={treeData.length <= 20}
                motion={reducedMotion ? false : undefined}
                selectable={!!onSelectFile}
                selectedKeys={selectedPath ? [selectedPath] : []}
                treeData={treeData}
                onSelect={(_keys, info) => {
                    // Both files (leaf) and folders (non-leaf) are selectable; the pane decides what to show.
                    if (onSelectFile) {
                        onSelectFile(String(info.node.key))
                    }
                }}
            />
        </div>
    )
}

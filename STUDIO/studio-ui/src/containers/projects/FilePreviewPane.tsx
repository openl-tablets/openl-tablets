import { lazy, Suspense, useEffect, useMemo, useRef, useState } from 'react'
import { errorMessage as message } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { useBlocker } from 'react-router'
import type { TFunction } from 'i18next'
import { App, Alert, Button, Dropdown, Modal, Skeleton, Space, Tag, type MenuProps } from 'antd'
import {
    CloseOutlined,
    CopyOutlined,
    DeleteOutlined,
    DownloadOutlined,
    DragOutlined,
    EditOutlined,
    FolderOpenOutlined,
    FontColorsOutlined,
    MoreOutlined,
    SaveOutlined,
    TableOutlined,
    TranslationOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'
import { isMessageFilePath } from '../../services/localization'
import type { ProjectStatusDetailedMessage, ProjectStatusUpdate } from '../../services/projectStatus'
import { searchTables } from '../../services/modules'
import { isMarkdownPath } from '../../utils/workbooks'
import { useSharedStyles } from './sharedStyles'
import { basename, trimTrailingSlashes } from './projectPaths'
import { CopyFileModal } from './CopyFileModal'
import { DeleteFileModal } from './DeleteFileModal'
import { MoveFileModal } from './MoveFileModal'
import { UpdateFileModal } from './UpdateFileModal'
import { useMessageFileProblems } from './useMessageFileProblems'
import type { EditorProblem } from './CodeEditor'

// The code editor pulls in CodeMirror; load it only when a text file is actually opened.
const CodeEditor = lazy(() => import('./CodeEditor').then(module => ({ default: module.CodeEditor })))

const useStyles = createStyles(({ css, token }) => ({
    badge: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 10px;
    `,
    actions: css`
        margin-left: auto;
        flex: none;
    `,
    /** The file path fills the header and clips, so the actions beside it never leave the pane. */
    path: css`
        flex: 1 1 auto;
        min-width: 0;
    `,
    body: css`
        flex: 1;
        min-width: 0;
        min-height: 0;
        overflow: auto;
    `,
    error: css`
        margin: 12px;
    `,
}))

interface FilePreviewPaneProps {
    projectId: string
    /** Folders of the project, offered as the destination when copying. */
    folders: string[]
    repositoryId: string
    projectName: string
    branch?: string | null
    path: string | null
    /** The line of the file to show the cursor at, counted from 1: the one a compilation message points to. */
    line?: number | undefined
    canWrite: boolean
    canDelete: boolean
    onChanged: () => void
    onDeleted: () => void
    /** The open file was renamed or moved: follow it to its new path so the pane does not 404 on the old one. */
    onMoved: (newPath: string) => void
    /** Bumped when the project reloads (save, close, …), forcing content to refetch and edit mode to reset. */
    reloadToken?: number
    /**
     * The files the reload behind the current token is known to cover — a folder stands for anything
     * under it. When the open file is not among them, its content is not re-fetched; null means the
     * change may have touched anything.
     */
    changedFiles?: string[] | null
    /** The name of the module each workbook of the project is, indexed by the file's path. */
    modules?: Record<string, string>
    /** Opens the module the file is, in the editor. */
    onOpenModule?: (moduleName: string) => void
    /** Opens the messages of the project, for a localization file, on the Localization screen. */
    onOpenLocalization?: () => void
    /**
     * Whether the project is open: only the files of an open project are checked, as the check reads the copy of the
     * project in the workspace and refuses a closed one. Every caller tells it from the status of the project.
     */
    projectOpened: boolean
    /** Opens a table of a module in the editor: the one a Markdown file of a module folder holds. */
    onOpenTable?: (moduleName: string, tableId: string) => void
    /**
     * The compilation status of the project: its messages tell where a Markdown file of a module folder breaks the
     * format, once the module of the file is compiled. Absent while the project is not compiled.
     */
    compileStatus?: ProjectStatusUpdate | null | undefined
}

/**
 * Whether the messages of a compilation are those of a module: the compilation is over, so every message of it is told,
 * and it compiled the module, the one opened or the whole project.
 */
const isCompiled = (status: ProjectStatusUpdate | null | undefined, module: string): boolean =>
    status !== null && status !== undefined && status.compileState !== 'idle' && status.compileState !== 'compiling'
    && (status.compilation?.modules?.compiledModules ?? []).includes(module)

/**
 * Where the compilation found a Markdown file breaking the format, each at the place of the file it is in, whether the
 * message names a cell of the table or a line of the file.
 */
const formatProblemsOf = (messages: readonly ProjectStatusDetailedMessage[], path: string): EditorProblem[] =>
    messages.flatMap(({ location, summary, severity }) => {
        if (location === undefined || location.type === 'localization' || location.path !== path) {
            return []
        }
        const told = { message: summary, severity: severity === 'ERROR' ? 'error' as const : 'warning' as const }
        return (location.places ?? []).map(place => ({ ...told, place }))
    })

/**
 * The module whose table a Markdown file holds: the module of the folder the file is in, at any depth.
 *
 * <p>A Markdown file of no module folder holds no table: it is documentation.
 */
const tableModuleOf = (modules: Record<string, string>, path: string): string | undefined => {
    const folder = Object.keys(modules)
        .filter(modulePath => path.startsWith(`${trimTrailingSlashes(modulePath)}/`))
        .sort((a, b) => b.length - a.length)[0]
    return folder === undefined ? undefined : modules[folder]
}

/**
 * The text of the file a selection names, re-read whenever the selection or the project reloads.
 *
 * <p>A reload that names what it touched and left the open file alone keeps what is on screen. A re-read
 * that brings back something other than what was last read says so, since someone else wrote the file.
 */
const useFileText = (selection: FileSelection, editable: boolean, changedFiles: string[] | null, t: TFunction) => {
    const { notification } = App.useApp()
    const [content, setContent] = useState('')
    const [original, setOriginal] = useState('')
    // The file the text read belongs to, as `project|path`: the selection names another while its text is read.
    const [readFor, setReadFor] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    // What the last fetch brought for which file — read to tell a re-read that changed the file
    // (worth telling the user about) from a first open or a switch to another file.
    const lastLoaded = useRef<{ key: string, text: string } | null>(null)
    // The toast reads `t` through a ref: a re-bound translator must not re-download the open file.
    const tRef = useRef(t)
    tRef.current = t
    // Rides a ref: the list always belongs to the reload that bumped the token, never re-runs the effect.
    const changedFilesRef = useRef(changedFiles)
    changedFilesRef.current = changedFiles
    const { path, projectId, reloadToken } = selection

    useEffect(() => {
        if (!path || !editable) {
            // Nothing is read for it, so nothing is remembered as read: a file opened again after this one
            // would otherwise be taken for the one already on screen and left empty.
            lastLoaded.current = null
            setContent('')
            setOriginal('')
            setReadFor(null)
            setLoading(false)
            setError(null)
            return
        }
        const touched = changedFilesRef.current
        const key = `${projectId}|${path}`
        if (touched !== null && lastLoaded.current?.key === key && !coversFile(touched, path)) {
            return
        }
        let cancelled = false
        setLoading(true)
        setError(null)
        getFileContent(projectId, path)
            .then(text => {
                if (cancelled) {
                    return
                }
                setContent(text)
                setOriginal(text)
                setReadFor(key)
                announceOtherWriter(notification, lastLoaded.current, key, text, path, tRef.current)
                lastLoaded.current = { key, text }
            })
            .catch(e => { if (!cancelled) setError(message(e)) })
            .finally(() => { if (!cancelled) setLoading(false) })
        return () => { cancelled = true }
    }, [projectId, path, editable, reloadToken])

    /** Takes what was just written as what the file now holds, so the re-read after it is no news. */
    const markSaved = (text: string) => {
        setOriginal(text)
        lastLoaded.current = { key: `${projectId}|${path}`, text }
    }

    // The text read of the file the selection names, or null while it is read: until then the text is another file's.
    const originalOfSelection = readFor === `${projectId}|${path}` ? original : null

    return { content, setContent, original, originalOfSelection, loading, error, setError, markSaved }
}

/** Says that the file changed under the reader, when the re-read brought back something else. */
const announceOtherWriter = (
    notification: ReturnType<typeof App.useApp>['notification'],
    last: { key: string, text: string } | null,
    key: string,
    text: string,
    path: string,
    t: TFunction
) => {
    if (last?.key === key && last.text !== text) {
        notification.info({ title: t('browser.live_file_synced', { name: path.split('/').pop() }) })
    }
}

/** True when one of the changed paths is the file itself or a folder above it. */
const coversFile = (changed: string[], filePath: string): boolean =>
    changed.some(entry => filePath === entry || filePath.startsWith(`${entry}/`))

interface FileSelection {
    projectId: string
    repositoryId: string
    projectName: string
    branch: string | null
    path: string | null
    reloadToken: number | undefined
}

/** Whether the pane is already on the file the screen is asking for, in the branch and revision it asks for. */
const sameSelection = (one: FileSelection, other: FileSelection) =>
    one.branch === other.branch
    && one.path === other.path
    && one.projectId === other.projectId
    && one.projectName === other.projectName
    && one.reloadToken === other.reloadToken
    && one.repositoryId === other.repositoryId

/** What the file menu offers, which is what the reader may do to the project. */
const fileMenuItems = (
    canWrite: boolean,
    canDelete: boolean,
    t: (key: string) => string
): NonNullable<MenuProps['items']> => [
    ...(canWrite ? [
        { key: 'rename', icon: <FontColorsOutlined />, label: t('browser.files.rename') },
        { key: 'move', icon: <DragOutlined />, label: t('browser.files.move') },
        { key: 'update', icon: <UploadOutlined />, label: t('browser.files.update') },
        { key: 'copy', icon: <CopyOutlined />, label: t('browser.files.copy') },
    ] : []),
    ...(canDelete
        ? [{ key: 'delete', danger: true, icon: <DeleteOutlined />, label: t('browser.files.delete') }]
        : []),
]

/**
 * What the pane does with a localization file: its text is marked where the server finds something wrong in it, which
 * the server checks for a project it holds open.
 *
 * @param path       the file on screen
 * @param checkable  whether the file is read as text of an open project
 */
const messageFileOf = (path: string | null, checkable: boolean) => ({
    checked: checkable ? path : null,
})

/**
 * The module of a Markdown file of a module folder, which holds a table; none for any other file.
 */
const markdownTableModuleOf = (modules: Record<string, string>, path: string | null): string | undefined =>
    path && isMarkdownPath(path) ? tableModuleOf(modules, path) : undefined

/**
 * Where the text of a file is marked as wrong. A localization file is checked by the server for a project it holds
 * open. A Markdown file of a module folder holds a table: the compilation of its module tells where the file breaks
 * the format. Until the module is compiled, nothing is known about it, so nothing is marked.
 */
const useFileProblems = (
    selection: FileSelection,
    editable: boolean,
    projectOpened: boolean,
    originalOfSelection: string | null,
    modules: Record<string, string>,
    compileStatus: ProjectStatusUpdate | null | undefined
): EditorProblem[] | undefined => {
    const { path } = selection
    const messageFile = messageFileOf(path, editable && projectOpened)
    const messageProblems = useMessageFileProblems(selection.projectId, messageFile.checked, originalOfSelection)
    const formatModule = editable ? markdownTableModuleOf(modules, path) : undefined
    const formatChecked = formatModule !== undefined && isCompiled(compileStatus, formatModule)
    const formatProblems = useMemo(
        () => (formatChecked && path
            ? formatProblemsOf(compileStatus?.compilation?.messages?.items ?? [], path)
            : undefined),
        [path, compileStatus, formatChecked]
    )
    return messageProblems ?? formatProblems
}

/**
 * What the pane shows for a file it cannot show as text: a workbook the project declares as a module is read in the
 * editor, any other file is downloaded.
 */
const BinaryFilePlaceholder = ({ module, onDownload, onOpenModule }: {
    module: string | undefined
    onDownload: () => void
    onOpenModule: ((module: string) => void) | undefined
}) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    return (
        <div className={shared.panePlaceholder} data-testid="file-preview-binary">
            <span>{t(module ? 'browser.files.module_hint' : 'browser.files.binary_hint')}</span>
            {module ? (
                <Button
                    data-testid="file-open-module"
                    icon={<TableOutlined />}
                    onClick={() => onOpenModule?.(module)}
                    type="primary"
                >
                    {t('browser.files.open_in_editor')}
                </Button>
            ) : (
                <Button icon={<DownloadOutlined />} onClick={onDownload}>
                    {t('browser.files.download')}
                </Button>
            )}
        </div>
    )
}

/**
 * What a project page shows for a file picked in the projects tree. Text files open read-only in a
 * syntax-highlighted viewer; the pencil switches to an editable view with save and cancel. Binary files
 * (e.g. .xlsx) offer a download. File-level actions live here, grouped in one button bar, rather than on
 * each tree row.
 */
export const FilePreviewPane = ({
    projectId,
    repositoryId,
    projectName,
    branch,
    path,
    line,
    folders,
    canWrite,
    canDelete,
    onChanged,
    onDeleted,
    onMoved,
    reloadToken,
    changedFiles = null,
    modules = {},
    onOpenModule,
    onOpenLocalization,
    onOpenTable,
    projectOpened,
    compileStatus,
}: FilePreviewPaneProps) => {
    const { t } = useTranslation('repository')
    const { notification } = App.useApp()
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [activeSelection, setActiveSelection] = useState<FileSelection>({
        branch: branch ?? null,
        path,
        projectId,
        projectName,
        reloadToken,
        repositoryId,
    })
    const [pendingSelection, setPendingSelection] = useState<FileSelection | null>(null)
    const [saving, setSaving] = useState(false)
    const [updating, setUpdating] = useState(false)
    const [copying, setCopying] = useState(false)
    const [moving, setMoving] = useState<'move' | 'rename' | null>(null)
    const [deleting, setDeleting] = useState(false)
    const [editing, setEditing] = useState(false)
    const [openingTable, setOpeningTable] = useState(false)

    const activePath = activeSelection.path
    const editable = !!activePath && isEditableTextFile(activePath)
    const { content, setContent, original, originalOfSelection, loading, error, setError, markSaved } =
        useFileText(activeSelection, editable, changedFiles, t)
    const problems = useFileProblems(activeSelection, editable, projectOpened, originalOfSelection, modules,
        compileStatus)
    // A workbook the project declares as a module is opened in the editor rather than exported to be read.
    const openableModule = activePath && onOpenModule ? modules[activePath] : undefined
    // A localization file is read with the other languages on the Localization screen, besides as text here.
    const openableMessages = !!activePath && onOpenLocalization !== undefined && isMessageFilePath(activePath)
    // A Markdown file of a module folder is a table, which the editor shows as any table of a workbook.
    const tableModule = onOpenTable ? markdownTableModuleOf(modules, activePath) : undefined
    const dirty = content !== original
    const incomingSelection: FileSelection = {
        branch: branch ?? null,
        path,
        projectId,
        projectName,
        reloadToken,
        repositoryId,
    }

    // The file the screen asks for is taken up at once, unless what is on screen has unsaved changes: that
    // one waits until the reader says whether to keep it or let it go.
    useEffect(() => {
        if (sameSelection(activeSelection, incomingSelection)) {
            return
        }
        if (dirty) {
            setPendingSelection(incomingSelection)
            return
        }
        setPendingSelection(null)
        setActiveSelection(incomingSelection)
        // The incoming selection is built from these on every render, so they are what it changes with; the
        // object itself is new each time and would run this effect — and set state from it — for ever.
    }, [activeSelection, branch, dirty, path, projectId, projectName, reloadToken, repositoryId])

    // A draft not saved lives in this pane alone, which goes when the page shows anything else: another file, a folder,
    // a tab, another screen or project. The reader is asked first, and before closing the page.
    const leaving = useBlocker(({ currentLocation, nextLocation }) => dirty
        && (currentLocation.pathname !== nextLocation.pathname
            || new URLSearchParams(currentLocation.search).get('file') !== new URLSearchParams(nextLocation.search).get('file')))
    useEffect(() => {
        if (!dirty) {
            return undefined
        }
        const ask = (event: BeforeUnloadEvent) => event.preventDefault()
        window.addEventListener('beforeunload', ask)
        return () => window.removeEventListener('beforeunload', ask)
    }, [dirty])

    // A newly accepted file — or a project reload (save/close) — returns the pane to read-only view.
    useEffect(() => {
        setEditing(false)
        setError(null)
    }, [activePath, activeSelection.reloadToken])

    const save = async () => {
        if (!activePath) {
            return
        }
        setSaving(true)
        setError(null)
        try {
            await updateFileContent(activeSelection.projectId, activePath, content)
            markSaved(content)
            setEditing(false)
            onChanged()
        } catch (e) {
            setError(message(e))
        } finally {
            setSaving(false)
        }
    }

    const openTable = async () => {
        if (!activePath || !tableModule) {
            return
        }
        setOpeningTable(true)
        setError(null)
        try {
            const tables = await searchTables(activeSelection.projectId, { module: tableModule })
            // A table is known by the file it is written in, named from the root of the workspace
            const table = tables.find(candidate => candidate.file?.endsWith(`/${activePath}`))
            if (table) {
                onOpenTable?.(tableModule, table.id)
            } else {
                notification.info({ title: t('browser.files.no_table', { name: basename(activePath) }) })
            }
        } catch (e) {
            setError(message(e))
        } finally {
            setOpeningTable(false)
        }
    }

    const cancelEdit = () => {
        setContent(original)
        setEditing(false)
        setError(null)
    }

    const keepCurrentFile = () => {
        setPendingSelection(null)
        leaving.reset?.()
    }

    const discardCurrentFile = () => {
        if (leaving.state === 'blocked') {
            // The draft is let go before the way out is taken, so the file or folder it leads to is not asked about again.
            setContent(original)
            leaving.proceed()
            return
        }
        if (pendingSelection) {
            setPendingSelection(null)
            setActiveSelection(pendingSelection)
        }
    }

    if (!activePath) {
        return (
            <div className={shared.paneColumn}>
                <div className={shared.panePlaceholder} data-testid="file-preview-empty">
                    <FolderOpenOutlined />
                    <span>{t('browser.files.select_hint')}</span>
                </div>
            </div>
        )
    }

    // Edit and Export stay to hand; the rarer file actions gather under one menu, as they do elsewhere.
    const menuItems = fileMenuItems(canWrite, canDelete, t)
    const runFromMenu: MenuProps['onClick'] = ({ key }) => {
        const chosen: Record<string, () => void> = {
            rename: () => setMoving('rename'),
            move: () => setMoving('move'),
            update: () => setUpdating(true),
            copy: () => setCopying(true),
            delete: () => setDeleting(true),
        }
        chosen[key]?.()
    }

    const textBadge = editing ? t('browser.files.text_editor') : t('browser.editor.read_only')
    const textView = loading
        ? <Skeleton active paragraph={{ rows: 10 }} style={{ padding: 16 }} title={false} />
        : (
            <div className={styles.body}>
                <Suspense fallback={<Skeleton active paragraph={{ rows: 10 }} style={{ padding: 16 }} title={false} />}>
                    <CodeEditor
                        // The line belongs to the file the URL names, not to one still shown while it is left
                        line={activePath === path ? line : undefined}
                        onChange={setContent}
                        path={activePath}
                        problems={problems}
                        readOnly={!editing}
                        value={content}
                    />
                </Suspense>
            </div>
        )

    return (
        <div className={shared.paneColumn} data-testid="file-preview">
            <div className={shared.paneHeader}>
                <span className={cx(shared.valueText, shared.ellipsis, styles.path)}>{activePath}</span>
                <Tag className={styles.badge}>
                    {editable ? textBadge : t('browser.files.binary')}
                </Tag>
                <Space className={styles.actions}>
                    {editing ? (
                        <>
                            <Button
                                data-testid="file-save"
                                disabled={!dirty}
                                icon={<SaveOutlined />}
                                loading={saving}
                                onClick={save}
                                size="small"
                                type="primary"
                            >
                                {t('browser.editor.save')}
                            </Button>
                            <Button data-testid="file-cancel" icon={<CloseOutlined />} onClick={cancelEdit} size="small">
                                {t('browser.editor.cancel')}
                            </Button>
                        </>
                    ) : (
                        <>
                            {openableMessages && (
                                <Button
                                    data-testid="file-open-localization"
                                    icon={<TranslationOutlined />}
                                    onClick={onOpenLocalization}
                                    size="small"
                                    type="primary"
                                >
                                    {t('browser.files.open_in_editor')}
                                </Button>
                            )}
                            {tableModule && (
                                <Button
                                    data-testid="file-open-table"
                                    icon={<TableOutlined />}
                                    loading={openingTable}
                                    onClick={() => void openTable()}
                                    size="small"
                                    type="primary"
                                >
                                    {t('browser.files.open_in_editor')}
                                </Button>
                            )}
                            {editable && canWrite && (
                                <Button data-testid="file-edit" icon={<EditOutlined />} onClick={() => setEditing(true)} size="small">
                                    {t('browser.editor.edit')}
                                </Button>
                            )}
                            <Button
                                data-testid="file-download"
                                icon={<DownloadOutlined />}
                                onClick={() => downloadFile(activeSelection.projectId, activePath)}
                                size="small"
                            >
                                {t('browser.files.download')}
                            </Button>
                            {menuItems.length > 0 && (
                                <Dropdown menu={{ items: menuItems, onClick: runFromMenu }} trigger={['click']}>
                                    <Button
                                        aria-label={t('browser.files.more_actions')}
                                        data-testid="file-actions"
                                        icon={<MoreOutlined />}
                                        size="small"
                                    />
                                </Dropdown>
                            )}
                        </>
                    )}
                </Space>
            </div>
            {error && <Alert showIcon className={styles.error} data-testid="file-preview-error" title={error} type="error" />}
            {/* A problem of the whole file is told over the text: it is about no line of it. */}
            {editable && !loading && problems?.filter(problem => !problem.place).map(problem => (
                <Alert
                    key={problem.message}
                    banner
                    data-testid="file-problem"
                    title={problem.message}
                    type={problem.severity}
                />
            ))}
            {editable ? textView : (
                <BinaryFilePlaceholder
                    module={openableModule}
                    onDownload={() => downloadFile(activeSelection.projectId, activePath)}
                    onOpenModule={onOpenModule}
                />
            )}
            <Modal
                destroyOnHidden
                cancelButtonProps={{ 'data-testid': 'file-discard-cancel' }}
                okButtonProps={{ 'data-testid': 'file-discard-confirm' }}
                okText={t('browser.files.discard_changes')}
                onCancel={keepCurrentFile}
                onOk={discardCurrentFile}
                open={pendingSelection !== null || leaving.state === 'blocked'}
                title={t('browser.files.discard_changes_title')}
            >
                {t('browser.files.discard_changes_desc')}
            </Modal>
            {activePath && (
                <CopyFileModal
                    folders={folders}
                    onClose={() => setCopying(false)}
                    onCopied={onChanged}
                    open={copying}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && moving && (
                <MoveFileModal
                    open
                    folders={folders}
                    mode={moving}
                    onClose={() => setMoving(null)}
                    onMoved={onMoved}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && (
                <DeleteFileModal
                    onClose={() => setDeleting(false)}
                    onDeleted={onDeleted}
                    open={deleting}
                    path={activePath}
                    projectId={activeSelection.projectId}
                />
            )}
            {activePath && (
                <UpdateFileModal
                    onClose={() => setUpdating(false)}
                    onUpdated={onChanged}
                    open={updating}
                    path={activePath}
                    projectId={activeSelection.projectId}
                    title={t('browser.files.update_title', { name: activePath.split('/').pop() })}
                />
            )}
        </div>
    )
}

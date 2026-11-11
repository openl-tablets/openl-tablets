import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { notification } from 'antd'
import { FilePreviewPane } from './FilePreviewPane'
import {
    downloadFile,
    getFileContent,
    isEditableTextFile,
    updateFileContent,
} from '../../services/files'
import { searchTables } from '../../services/modules'
import type {
    ProjectCompileState,
    ProjectStatusDetailedMessage,
    ProjectStatusMessageSource,
    ProjectStatusUpdate,
} from '../../services/projectStatus'

vi.mock('../../services/modules', () => ({
    searchTables: vi.fn(),
}))

vi.mock('../../services/files', () => ({
    downloadFile: vi.fn(),
    getFileContent: vi.fn(),
    isEditableTextFile: vi.fn(),
    updateFileContent: vi.fn(),
}))

const checkMessageFiles = vi.fn()
vi.mock('../../services/localization', async importOriginal => ({
    ...(await importOriginal<typeof import('../../services/localization')>()),
    checkMessageFiles: (...args: unknown[]) => checkMessageFiles(...args),
}))

vi.mock('react-i18next', () => {
    // One t for every render: a fresh function each call would re-run the effects depending on it.
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

vi.mock('antd-style', () => ({
    createStyles: () => () => ({
        styles: new Proxy({}, { get: () => '' }),
        cx: (...args: unknown[]) => args.filter(Boolean).join(' '),
    }),
}))

vi.mock('@ant-design/icons', () => ({
    CloseOutlined: () => null,
    CopyOutlined: () => null,
    DeleteOutlined: () => null,
    DiffOutlined: () => null,
    DownloadOutlined: () => null,
    DragOutlined: () => null,
    EditOutlined: () => null,
    FontColorsOutlined: () => null,
    FolderOpenOutlined: () => null,
    MoreOutlined: () => null,
    SaveOutlined: () => null,
    TableOutlined: () => null,
    UploadOutlined: () => null,
}))

vi.mock('./MoveFileModal', () => ({
    MoveFileModal: ({ open, mode }: { open: boolean, mode: string }) => open ? <div data-testid={`move-file-modal-${mode}`} /> : null,
}))

vi.mock('./CodeEditor', () => ({
    CodeEditor: ({ line, onChange, problems, readOnly, value }: Record<string, unknown>) => (
        <textarea
            data-line={line as number | undefined}
            data-problems={problems === undefined ? undefined : JSON.stringify(problems)}
            data-testid="code-editor"
            onChange={event => (onChange as (value: string) => void)(event.target.value)}
            readOnly={readOnly as boolean}
            value={value as string}
        />
    ),
}))


vi.mock('antd', async () => {
    const Button = ({ children, onClick, ...rest }: Record<string, unknown>) => {
        const { danger, disabled, icon, loading, size, type, ...dom } = rest
        void danger; void icon; void loading; void size
        return (
            <button
                data-type={type as string | undefined}
                disabled={disabled as boolean}
                onClick={onClick as never}
                {...dom}
            >
                {children as never}
            </button>
        )
    }
    const Input = ({ onChange, onPressEnter, value, ...rest }: Record<string, unknown>) => {
        void onPressEnter
        return <input onChange={onChange as never} value={value as string} {...rest} />
    }
    const Modal = ({
        cancelButtonProps,
        children,
        okButtonProps,
        okText,
        onCancel,
        onOk,
        open,
        title,
    }: Record<string, unknown>) => open ? (
        <div role="dialog">
            <span>{title as never}</span>
            <div>{children as never}</div>
            <button
                data-testid={(cancelButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                onClick={onCancel as never}
            >
                Cancel
            </button>
            <button
                data-testid={(okButtonProps as { 'data-testid'?: string } | undefined)?.['data-testid']}
                onClick={onOk as never}
            >
                {(okText as string | undefined) ?? 'OK'}
            </button>
        </div>
    ) : null
    const Popconfirm = ({ children, onConfirm }: Record<string, unknown>) => (
        <>
            {children as never}
            <button data-testid="popconfirm-ok" onClick={onConfirm as never} type="button">confirm</button>
        </>
    )
    interface MenuItem { key: string, label: unknown }
    const Dropdown = ({ children, menu }: Record<string, unknown>) => {
        const { items, onClick } = menu as { items?: MenuItem[], onClick?: (info: { key: string }) => void }
        return (
            <div>
                {children as never}
                {items?.map(item => (
                    <button key={item.key} data-testid={`file-${item.key}`} onClick={() => onClick?.({ key: item.key })} type="button">
                        {item.label as never}
                    </button>
                ))}
            </div>
        )
    }
    const Skeleton = () => <div>loading</div>
    const Space = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    Space.Compact = ({ children }: Record<string, unknown>) => <div>{children as never}</div>
    const Tag = ({ children }: Record<string, unknown>) => <span>{children as never}</span>
    const Tooltip = ({ children }: Record<string, unknown>) => <>{children as never}</>
    const Alert = ({ title, ...rest }: Record<string, unknown>) => {
        const { banner, showIcon, type, ...dom } = rest
        void banner; void showIcon; void type
        return <div {...dom}>{title as never}</div>
    }
    const notification = { error: vi.fn(), info: vi.fn() }
    const { withStaticApp } = await import('testing/staticAntdApp')
    return withStaticApp({
        Alert, Button, Dropdown, Input, Modal, notification, Popconfirm, Skeleton, Space, Tag, Tooltip
    })
})

vi.mock('./CopyFileModal', () => ({
    CopyFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="copy-file-modal" /> : null,
}))
vi.mock('./DeleteFileModal', () => ({
    DeleteFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="delete-file-modal" /> : null,
}))
vi.mock('./UpdateFileModal', () => ({
    UpdateFileModal: ({ open }: { open: boolean }) => open ? <div data-testid="update-file-modal" /> : null,
}))

const baseProps = {
    projectOpened: true,
    folders: ['rules'],
    branch: 'main',
    canDelete: true,
    canWrite: true,
    onChanged: vi.fn(),
    onDeleted: vi.fn(),
    onMoved: vi.fn(),
    projectId: 'p1',
    projectName: 'Project',
    repositoryId: 'repo1',
}

/** An error of a compilation, at the place given. */
const error = (id: number, summary: string, location: ProjectStatusMessageSource): ProjectStatusDetailedMessage => ({
    id,
    summary,
    severity: 'ERROR',
    stacktrace: false,
    location,
})

/** The status of a compilation of the modules given, in the state given, which raised the messages given. */
const compiled = (
    compileState: ProjectCompileState,
    compiledModules: string[],
    items: ProjectStatusDetailedMessage[]
): ProjectStatusUpdate => ({
    projectId: 'p1',
    branch: 'main',
    compileState,
    compilation: {
        messages: { items, total: items.length, errors: items.length, warnings: 0 },
        modules: { compiledModules, total: 1, compiled: compiledModules.length },
    },
})

describe('FilePreviewPane', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        vi.mocked(isEditableTextFile).mockReturnValue(true)
        vi.mocked(getFileContent).mockImplementation((_projectId, path) => {
            if (path === 'second.txt') {
                return Promise.resolve('second content')
            }
            return Promise.resolve('first content')
        })
        vi.mocked(updateFileContent).mockResolvedValue()
        checkMessageFiles.mockResolvedValue([])
    })

    it('checks a localization file once it is read, and once more after each save', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="i18n/message.properties" reloadToken={0} />)
        await screen.findByDisplayValue('first content')
        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(1))

        // The text of the file opened next is the only one it is checked with: not the text of the file left.
        rerender(<FilePreviewPane {...baseProps} path="i18n/message_fr.properties" reloadToken={0} />)
        await waitFor(() => expect(getFileContent).toHaveBeenCalledWith('p1', 'i18n/message_fr.properties'))
        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(2))

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.type(screen.getByTestId('code-editor'), '!')
        await userEvent.click(screen.getByTestId('file-save'))

        await waitFor(() => expect(checkMessageFiles).toHaveBeenCalledTimes(3))
        expect(checkMessageFiles).toHaveBeenCalledWith('p1', 'file')
    })

    it('tells a problem of the whole localization file over its text, and leaves those of its lines to the editor', async () => {
        const location = { type: 'localization', path: 'i18n/message_fr.properties', line: 1, language: 'fr' }
        checkMessageFiles.mockResolvedValue([
            { id: 1, summary: '2 keys are not translated', severity: 'WARN', location },
            {
                id: 2,
                summary: 'Full-width comma',
                severity: 'WARN',
                location: { ...location, line: 2, places: [{ line: 2, column: 4, endLine: 2, endColumn: 5 }]},
            },
        ])
        render(<FilePreviewPane {...baseProps} path="i18n/message_fr.properties" reloadToken={0} />)

        const told = await screen.findByTestId('file-problem')
        expect(told).toHaveTextContent('2 keys are not translated')
        expect(screen.getAllByTestId('file-problem')).toHaveLength(1)
    })

    it('checks no localization file of a closed project', async () => {
        render(<FilePreviewPane {...baseProps} path="i18n/message.properties" projectOpened={false} reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        expect(checkMessageFiles).not.toHaveBeenCalled()
    })

    it('checks no file that is no localization file', async () => {
        render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        expect(checkMessageFiles).not.toHaveBeenCalled()
    })

    it('marks where the compilation of its module found a Markdown file breaking the format', async () => {
        const place = { line: 4, column: 0, endLine: 4, endColumn: 19 }
        const cellPlace = { line: 9, column: 0, endLine: 9, endColumn: 34 }
        render(
            <FilePreviewPane
                {...baseProps}
                modules={{ 'rules/Rating': 'Rating' }}
                path="rules/Rating/rate.md"
                compileStatus={compiled('errors', ['Rating'], [
                    error(1, "'license' is not a key of the front matter",
                        { type: 'module', name: 'Rating', path: 'rules/Rating/rate.md', line: 4, places: [place]}),
                    error(2, 'The row has more cells than the first row of the table',
                        { type: 'table', id: 't1', cell: 'B3', path: 'rules/Rating/rate.md', line: 9,
                            places: [cellPlace]}),
                    error(3, 'Another file',
                        { type: 'module', name: 'Rating', path: 'rules/Rating/other.md', line: 1, places: [place]}),
                ])}
            />
        )

        await waitFor(() => expect(screen.getByTestId('code-editor')).toHaveAttribute('data-problems', JSON.stringify([
            { message: "'license' is not a key of the front matter", severity: 'error', place },
            { message: 'The row has more cells than the first row of the table', severity: 'error', place: cellPlace },
        ])))
        // Every such problem has its line: none is told over the text.
        expect(screen.queryByTestId('file-problem')).toBeNull()
    })

    it.each([
        ['while the project is not compiled', undefined],
        ['while its module is compiling', compiled('compiling', ['Rating'], [])],
        ['when the module compiled is another one', compiled('ok', ['Main'], [])],
    ])('marks nothing in a Markdown file %s', async (unused, compileStatus) => {
        render(
            <FilePreviewPane
                {...baseProps}
                compileStatus={compileStatus}
                modules={{ 'rules/Rating': 'Rating' }}
                path="rules/Rating/rate.md"
            />
        )
        await screen.findByDisplayValue('first content')

        // The messages of another compilation, or of a running one, are not those of the file
        expect(screen.getByTestId('code-editor')).not.toHaveAttribute('data-problems')
    })

    it('neither marks nor checks a Markdown file of no module folder', async () => {
        render(
            <FilePreviewPane
                {...baseProps}
                compileStatus={compiled('ok', ['Rating'], [])}
                modules={{ 'rules/Rating': 'Rating' }}
                path="docs/README.md"
            />
        )
        await screen.findByDisplayValue('first content')

        expect(screen.getByTestId('code-editor')).not.toHaveAttribute('data-problems')
    })

    it('keeps the dirty file open when a file switch is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="second.txt" reloadToken={0} />)

        expect(screen.getByText('browser.files.discard_changes_title')).toBeTruthy()
        expect(screen.getByText('first.txt')).toBeTruthy()
        expect(getFileContent).not.toHaveBeenCalledWith('p1', 'second.txt')

        await userEvent.click(screen.getByTestId('file-discard-cancel'))

        expect(screen.queryByText('browser.files.discard_changes_title')).toBeNull()
        expect(screen.getByText('first.txt')).toBeTruthy()
        expect(screen.getByDisplayValue('modified content')).toBeTruthy()
    })

    it('loads the selected file after dirty changes are discarded', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="second.txt" reloadToken={0} />)
        await userEvent.click(screen.getByTestId('file-discard-confirm'))

        await waitFor(() => expect(getFileContent).toHaveBeenCalledWith('p1', 'second.txt'))
        await screen.findByDisplayValue('second content')
        expect(screen.getByText('second.txt')).toBeTruthy()
    })

    it('keeps dirty content when a reload is cancelled', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'modified content')

        rerender(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={1} />)

        expect(screen.getByText('browser.files.discard_changes_title')).toBeTruthy()
        expect(getFileContent).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('file-discard-cancel'))

        expect(screen.queryByText('browser.files.discard_changes_title')).toBeNull()
        expect(screen.getByDisplayValue('modified content')).toBeTruthy()
        expect(getFileContent).toHaveBeenCalledTimes(1)
    })

    it('tells the user when a reload brought other content of the open file', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        // The file changed elsewhere; the page reload re-fetches it with new content.
        vi.mocked(getFileContent).mockResolvedValue('changed elsewhere')
        rerender(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={1} />)

        await screen.findByDisplayValue('changed elsewhere')
        expect(notification.info).toHaveBeenCalledWith({ title: 'browser.live_file_synced' })
    })

    it('stays quiet when a reload brings back the same content', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        rerender(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={1} />)

        await screen.findByDisplayValue('first content')
        expect(notification.info).not.toHaveBeenCalled()
    })

    it('does not re-fetch the open file when the reload names only other files', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')
        expect(getFileContent).toHaveBeenCalledTimes(1)

        rerender(<FilePreviewPane {...baseProps} changedFiles={['rules/Other.xlsx']} path="first.txt" reloadToken={1} />)

        await screen.findByDisplayValue('first content')
        expect(getFileContent).toHaveBeenCalledTimes(1)
    })

    it('re-fetches the open file when a changed folder covers it', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} path="rules/first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')
        expect(getFileContent).toHaveBeenCalledTimes(1)

        rerender(<FilePreviewPane {...baseProps} changedFiles={['rules']} path="rules/first.txt" reloadToken={1} />)

        await waitFor(() => expect(getFileContent).toHaveBeenCalledTimes(2))
    })

    it('shows the file at the line it is opened on, and no line for a file still shown while it is left', async () => {
        const { rerender } = render(<FilePreviewPane {...baseProps} line={3} path="first.txt" reloadToken={0} />)

        expect((await screen.findByDisplayValue('first content')).getAttribute('data-line')).toBe('3')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.type(screen.getByTestId('code-editor'), '!')
        rerender(<FilePreviewPane {...baseProps} line={5} path="second.txt" reloadToken={0} />)

        // The switch waits for the changes to be discarded; the line is the next file's
        expect(screen.getByTestId('code-editor').getAttribute('data-line')).toBeNull()
    })

    it('shows the empty state when no file is selected', () => {
        render(<FilePreviewPane {...baseProps} path={null} />)
        expect(screen.getByTestId('file-preview-empty')).toBeTruthy()
    })

    it('shows a binary preview and triggers download', async () => {
        vi.mocked(isEditableTextFile).mockReturnValue(false)

        render(<FilePreviewPane {...baseProps} path="rules/Main.xlsx" />)

        expect(screen.getByTestId('file-preview-binary')).toBeTruthy()
        // The toolbar action and the placeholder both offer the download; use the toolbar one.
        await userEvent.click(screen.getByTestId('file-download'))
        expect(downloadFile).toHaveBeenCalledWith('p1', 'rules/Main.xlsx')
    })

    it('opens a workbook the project declares as a module in the editor', async () => {
        vi.mocked(isEditableTextFile).mockReturnValue(false)
        const onOpenModule = vi.fn()

        render(
            <FilePreviewPane
                {...baseProps}
                modules={{ 'rules/Main.xlsx': 'Main' }}
                onOpenModule={onOpenModule}
                path="rules/Main.xlsx"
            />
        )

        expect(screen.getByText('browser.files.module_hint')).toBeTruthy()
        await userEvent.click(screen.getByTestId('file-open-module'))

        expect(onOpenModule).toHaveBeenCalledWith('Main')
        // Exporting it stays in the toolbar; what the placeholder offers is the editor.
        expect(downloadFile).not.toHaveBeenCalled()
    })

    it('opens in the editor the table a Markdown file of a module folder holds', async () => {
        vi.mocked(searchTables).mockResolvedValue([
            {
                file: 'Project/rules/Rating/Factors/baseRate.md', id: 't1', kind: 'Rules', name: 'baseRate',
                tableType: 'SmartRules',
            },
            {
                file: 'Project/rules/Rating/Factors/rate.md', id: 't2', kind: 'Rules', name: 'rate',
                tableType: 'SmartRules',
            },
        ])
        const onOpenTable = vi.fn()

        render(
            <FilePreviewPane
                {...baseProps}
                modules={{ 'rules': 'Main', 'rules/Rating': 'Rating' }}
                onOpenTable={onOpenTable}
                path="rules/Rating/Factors/rate.md"
            />
        )
        await screen.findByDisplayValue('first content')
        // The way into the table is the action of the file, as the way into the messages is for a localization file
        expect(screen.getByTestId('file-open-table')).toHaveAttribute('data-type', 'primary')
        await userEvent.click(screen.getByTestId('file-open-table'))

        // The deepest module folder holding the file is its module
        await waitFor(() => expect(onOpenTable).toHaveBeenCalledWith('Rating', 't2'))
        expect(searchTables).toHaveBeenCalledWith('p1', { module: 'Rating' })
    })

    it('tells that a Markdown file of a module folder holds no table to open', async () => {
        vi.mocked(searchTables).mockResolvedValue([])
        const onOpenTable = vi.fn()

        render(
            <FilePreviewPane
                {...baseProps}
                modules={{ 'rules/Rating': 'Rating' }}
                onOpenTable={onOpenTable}
                path="rules/Rating/Factors/notes.md"
            />
        )
        await userEvent.click(await screen.findByTestId('file-open-table'))

        await waitFor(() => expect(notification.info).toHaveBeenCalledWith({ title: 'browser.files.no_table' }))
        expect(onOpenTable).not.toHaveBeenCalled()
    })

    it.each(['docs/README.md', 'rules/RatingNotes/rate.md', 'rules/Rating/Factors/rate.txt'])(
        'offers no table to open for %s, which no module folder holds as a table',
        async path => {
            render(
                <FilePreviewPane
                    {...baseProps}
                    modules={{ 'rules/Rating': 'Rating' }}
                    onOpenTable={vi.fn()}
                    path={path}
                />
            )
            await screen.findByDisplayValue('first content')

            expect(screen.queryByTestId('file-open-table')).toBeNull()
        }
    )

    it('offers the export for a workbook that is no module of the project', async () => {
        vi.mocked(isEditableTextFile).mockReturnValue(false)

        render(
            <FilePreviewPane
                {...baseProps}
                modules={{ 'rules/Main.xlsx': 'Main' }}
                onOpenModule={vi.fn()}
                path="doc/Handbook.xlsx"
            />
        )

        expect(screen.getByText('browser.files.binary_hint')).toBeTruthy()
        expect(screen.queryByTestId('file-open-module')).toBeNull()
    })

    it('saves edited text content', async () => {
        render(<FilePreviewPane {...baseProps} path="first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId('file-edit'))
        await userEvent.clear(screen.getByTestId('code-editor'))
        await userEvent.type(screen.getByTestId('code-editor'), 'saved content')
        await userEvent.click(screen.getByTestId('file-save'))

        await waitFor(() => expect(updateFileContent).toHaveBeenCalledWith('p1', 'first.txt', 'saved content'))
        expect(baseProps.onChanged).toHaveBeenCalled()
    })

    // Every file action opens its own dialog; one parameterized test covers the whole menu.
    it.each([
        ['delete', 'file-delete', 'delete-file-modal'],
        ['copy', 'file-copy', 'copy-file-modal'],
        ['replace', 'file-update', 'update-file-modal'],
        ['rename', 'file-rename', 'move-file-modal-rename'],
        ['move', 'file-move', 'move-file-modal-move'],
    ])('opens the %s dialog for the active file', async (_action, trigger, modal) => {
        render(<FilePreviewPane {...baseProps} path="rules/first.txt" reloadToken={0} />)
        await screen.findByDisplayValue('first content')

        await userEvent.click(screen.getByTestId(trigger))

        expect(screen.getByTestId(modal)).toBeInTheDocument()
    })

    it('surfaces load failures in the preview pane', async () => {
        vi.mocked(getFileContent).mockRejectedValue(new Error('load failed'))

        render(<FilePreviewPane {...baseProps} path="broken.txt" reloadToken={0} />)

        expect(await screen.findByTestId('file-preview-error')).toBeInTheDocument()
    })
})

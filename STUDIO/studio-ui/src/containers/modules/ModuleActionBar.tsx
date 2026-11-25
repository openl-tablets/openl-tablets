import { useState, type ReactNode } from 'react'
import type { ConfirmDiscard } from './useDiscardConfirm'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Modal, Tooltip } from 'antd'
import {
    CheckCircleOutlined,
    DiffOutlined,
    ExperimentOutlined,
    FieldTimeOutlined,
    FormatPainterOutlined,
    HistoryOutlined,
    PartitionOutlined,
    PlusOutlined,
    UploadOutlined,
} from '@ant-design/icons'
import type { SummaryTable } from 'types/tables'
import type { Project } from '../../types/projects'
import { supportsRevisionSearch } from '../../utils/repositoryFeatures'
import { LocalChangesView } from '../projects/LocalChangesView'
import { RevisionsPanel } from '../projects/RevisionsPanel'
import { openExcelFilesCompareWindow } from '../projects/compare'
import { isActionAvailable, PROJECT_ACTIONS } from '../projects/projectActions'
import { ACTION_ICONS } from '../projects/projectActionIcons'
import { useProjectDialogs, type ProjectDialogActions } from '../projects/useProjectDialogs'
import { ReformatExcelFilesModal } from './ReformatExcelFilesModal'
import { useOverwriteConfirm } from './useOverwriteConfirm'
import { isMarkdownPath, isWorkbookPath } from '../../utils/workbooks'
import { CollapsingActionBar, type ActionMenu } from '../../components/CollapsingActionBar'

/** The history and the local changes are read in a window over the module, not on a screen of their own. */
const DIALOG_BODY = { body: { maxHeight: '70vh', overflow: 'auto' } }

/** The project's own actions the editor offers, in the order the old Editor kept them. */
const PROJECT_LEVEL: Array<keyof ProjectDialogActions> = ['save', 'sync', 'deploy', 'copy']

/** A button of the row, and what it runs, from the button or from the item of More it becomes. */
interface BarAction {
    key: string
    label: string
    icon: ReactNode
    disabled?: boolean | undefined
    primary?: boolean
    /** Why the action stands disabled, said when the pointer rests on it. */
    hint?: string | undefined
    /** How many there are of what it acts on, shown beside its name. */
    count?: number | undefined
    /** Runs the action; given what was clicked, which a launch anchors to. */
    run: (from: HTMLElement) => void
}

interface ModuleActionBarProps {
    project: Project
    /** The module the screen is of, or the one the project is compiled through on a screen of the whole project. */
    moduleName: string
    /**
     * Whether the screen is of the module. A screen of the whole project, as its messages, offers what acts on the
     * project alone: neither the workbook of the module nor a table written into it.
     */
    ofModule?: boolean
    /** The workbook the module is written in, so it can be exported. */
    modulePath?: string | undefined
    /** How many tests the module holds, as the status channel reports them. */
    testCount?: number | undefined
    /**
     * Whether every module of the project is compiled.
     *
     * <p>Until it is, only this module's tests can be run: the ones written elsewhere are not built yet.
     */
    projectCompiled?: boolean
    /** Nothing here acts on a project nobody has opened, so everything stands disabled until it is. */
    disabled?: boolean
    /** Opening a revision replaces the workspace copy, so the module is read again from it. */
    onRevisionOpened?: (() => void) | undefined
    /** An action that changed the project — a save, a copy, a sync, a new workbook — is read back. */
    onProjectChanged?: (() => void) | undefined
    /** A table written from this row, and the module it landed in, so the editor can open it. */
    onTableCreated?: ((written: SummaryTable, moduleName: string) => void) | undefined
    /**
     * Whether the module is waiting for the reader to compile it: a write landed while automatic compilation
     * is switched off, so what the compiler says about it is what it said before that write.
     */
    verifyNeeded?: boolean
    /** Compiles the module the reader has been editing, and reads it back. */
    onVerify?: (() => void) | undefined
    /**
     * Runs the save of the project after asking whatever has to be asked first, as whether to save what the screen
     * holds written and not saved: the project would be saved without it.
     */
    beforeSave?: ((save: () => void) => void) | undefined
    /**
     * Runs what reads the workbook afresh after asking whatever has to be asked first.
     *
     * <p>Opening a revision and restoring a local version both replace what the workspace holds, so cells
     * written on screen and not yet saved go with it.
     */
    confirmDiscard?: ConfirmDiscard | undefined
}

/** What the file of a module, by its path, lets the row offer; a module whose path is not known yet is none of these. */
const moduleFileOf = (modulePath: string | undefined) => {
    const workbook = !!modulePath && isWorkbookPath(modulePath)
    const markdown = isMarkdownPath(modulePath)
    return {
        workbook,
        // A module of a single Markdown file holds its one table and takes no other. A folder of Markdown files takes
        // a table as a file of its own, and a module whose path is not known yet may still take one.
        singleFile: markdown,
        // Only a module of a single file is downloaded from here: a folder is downloaded with the project.
        exportable: workbook || markdown,
        exportHint: modulePath ? 'browser.module.export_folder' : 'browser.module.export_unavailable',
        // The local changes are the versions a workbook is saved in: a Markdown file keeps none.
        keepsVersions: !modulePath || workbook,
    }
}

/**
 * What can be done to the open module, in the place the project's own actions sit — arranged as the old Editor
 * arranged them, so the row reads the same.
 *
 * Exporting the module, running its tests, writing a new table, the history of the project and what is about its
 * tables are answered here; the project's own actions are offered exactly as its own screen offers them. Every action
 * is a button of the row; the ones the row has no room for go to the menu at its end, as on the screen of the project.
 * What can be done to the table on screen belongs to the table, and stands in a band above it.
 *
 * <p>A screen of the whole project, as its messages, shows the same row without what acts on a module: its
 * workbook and a table written into it.
 */
export const ModuleActionBar = ({
    project,
    moduleName,
    ofModule = true,
    modulePath,
    testCount,
    disabled = false,
    projectCompiled = false,
    onRevisionOpened,
    onProjectChanged,
    onTableCreated,
    verifyNeeded = false,
    onVerify,
    beforeSave,
    confirmDiscard,
}: ModuleActionBarProps) => {
    const { t } = useTranslation('repository')
    const [revisionsOpen, setRevisionsOpen] = useState(false)
    const [reformatOpen, setReformatOpen] = useState(false)
    const [localChangesOpen, setLocalChangesOpen] = useState(false)
    // Saving, syncing, deploying and copying belong to the project, not to the module being read: they are
    // offered here exactly as the project's own screen offers them, by the same capabilities.
    const { actions, dialogs } = useProjectDialogs(project, { onChanged: () => onProjectChanged?.() })
    // The old Editor let a module's workbook be replaced from this row; the dialog it opens is the one the
    // Add menu of a project uses, restricted to Excel. A Markdown module is changed as text in the file editor: no workbook
    // replaces it.
    const { workbook, singleFile, exportable, exportHint, keepsVersions } = moduleFileOf(modulePath)
    const canUpdateModule = ofModule && !!project.capabilities?.canWrite && workbook
    // A read-only project shows it, disabled, so the reader sees why no table is written into it.
    const readOnly = project.readOnly === true
    // A module that keeps no versions offers no local changes, and neither does a read-only project, which is never
    // saved.
    const keepsLocalChanges = !readOnly && keepsVersions
    // Writing a table into the project is the project's own right, so it is offered by the same capability
    // the files of a project are written by.
    const canWrite = !!project.capabilities?.canWrite
    // Writing the first table into a project opened on an older revision saves it over the newer one.
    const confirmWrite = useOverwriteConfirm(project)

    // The panel of EPBDS-16560 answers this: it runs the project's tests, with the choice of only this
    // module's — which is the only choice left while the rest of the project is still being compiled.
    const openTests = (from: HTMLElement) => {
        const { top, left, width, height } = from.getBoundingClientRect()
        window.dispatchEvent(new CustomEvent('openTestsLaunch', {
            detail: {
                projectId: project.id,
                moduleName,
                anchor: { top, left, width, height },
                moduleOnlyLocked: !projectCompiled,
            },
        }))
    }

    // Every table of every module of the project is formatted, so a dialog says so before anything is written.
    const reformat = () => confirmWrite(() => setReformatOpen(true))

    /**
     * The buttons of the row, in their order: what the project needs done first, then what it offers, then the history
     * of the project and what is about its tables. A button the row has no room for is not dropped: it goes to the menu
     * at the end of the row.
     */
    const barActions: BarAction[] = [
        // Offered only while something is waiting: with automatic compilation on there is never anything to verify,
        // and the row does not carry a button that can do nothing.
        ...(verifyNeeded ? [{
            key: 'verify',
            label: t('browser.module.verify'),
            icon: <CheckCircleOutlined />,
            primary: true,
            run: () => onVerify?.(),
        }] : []),
        // Saving, syncing, deploying and copying belong to the project: offered where the project says they are.
        ...PROJECT_LEVEL.filter(id => isActionAvailable(project, id)).map((id): BarAction => ({
            key: id,
            label: t(PROJECT_ACTIONS[id].labelKey),
            icon: ACTION_ICONS[id],
            disabled,
            primary: id === 'save',
            run: id === 'save' && beforeSave !== undefined ? () => beforeSave(actions.save) : () => actions[id](),
        })),
        ...(canUpdateModule ? [{
            key: 'update',
            label: t('browser.module.update'),
            icon: <UploadOutlined />,
            disabled,
            run: () => window.dispatchEvent(new CustomEvent('openUpdateModuleModal', {
                detail: { projectId: project.id, modulePath, onSuccess: () => onProjectChanged?.() },
            })),
        }] : []),
        ...(ofModule ? [{
            key: 'export',
            label: t('browser.module.export'),
            icon: ACTION_ICONS.export,
            disabled: disabled || !exportable,
            hint: exportable ? undefined : t(exportHint),
            run: () => window.dispatchEvent(new CustomEvent('openExportProjectModal', {
                detail: { projectId: project.id, filePath: modulePath },
            })),
        }] : []),
        {
            key: 'test',
            label: t('browser.module.test'),
            icon: <ExperimentOutlined />,
            disabled: disabled || !testCount,
            count: testCount,
            run: openTests,
        },
        ...((canWrite || readOnly) && ofModule && !singleFile ? [{
            key: 'createTable',
            label: t('browser.module.createTable'),
            icon: <PlusOutlined />,
            disabled: disabled || readOnly,
            hint: readOnly ? t('browser.module.read_only') : undefined,
            run: () => confirmWrite(() => window.dispatchEvent(new CustomEvent('openCreateTableModal', {
                detail: { projectId: project.id, currentModuleName: moduleName, onSuccess: onTableCreated },
            }))),
        }] : []),
        ...(project.capabilities?.canViewHistory ? [{
            key: 'revisions',
            label: t('browser.module.revisions'),
            icon: <HistoryOutlined />,
            disabled,
            run: () => setRevisionsOpen(true),
        }] : []),
        ...(keepsLocalChanges ? [{
            key: 'localChanges',
            label: t('browser.module.local_changes'),
            icon: <FieldTimeOutlined />,
            disabled,
            run: () => setLocalChangesOpen(true),
        }] : []),
        {
            key: 'dependencies',
            label: t('browser.module.dependencies'),
            icon: <PartitionOutlined />,
            disabled,
            run: () => window.dispatchEvent(new CustomEvent('openTableGraphModal', {
                detail: { projectId: project.id, projectName: project.name, module: ofModule ? moduleName : undefined },
            })),
        },
        {
            key: 'compare',
            label: t('browser.module.compare'),
            icon: <DiffOutlined />,
            disabled,
            // Two files of the reader's own, not two revisions of the project: those the project screen compares.
            run: () => openExcelFilesCompareWindow(),
        },
        ...(canWrite ? [{
            key: 'reformat',
            label: t('browser.module.reformat'),
            icon: <FormatPainterOutlined />,
            disabled,
            hint: t('browser.module.reformat_tooltip'),
            run: reformat,
        }] : []),
    ]

    const shownKeys = barActions.map(action => action.key).join()

    /**
     * A button of the row, with the hint that says why it stands disabled or what it does. Run from the menu of the row,
     * it closes the menu, and what it opens stands at the trigger of the menu, which stays on screen.
     */
    const button = (action: BarAction, menu?: ActionMenu) => {
        const node = (
            <Button
                data-testid={`module-${action.key}`}
                disabled={action.disabled === true}
                icon={action.icon}
                type={action.primary ? 'primary' : 'default'}
                onClick={event => {
                    menu?.close()
                    action.run(menu?.anchor() ?? event.currentTarget)
                }}
            >
                {action.label}
                {!!action.count && (
                    <Badge color="blue" count={action.count} data-testid={`module-${action.key}-count`} />
                )}
            </Button>
        )
        return action.hint
            ? <Tooltip placement={menu ? 'left' : 'top'} title={action.hint}>{node}</Tooltip>
            : node
    }

    return (
        <CollapsingActionBar
            actions={barActions}
            deps={[shownKeys]}
            keyOf={action => action.key}
            menuDisabled={disabled}
            menuLabel={t('home.row_actions')}
            render={button}
            testId="module-actions"
            renderMeasure={action => (
                <Button icon={action.icon}>
                    {action.label}
                    {!!action.count && <Badge color="blue" count={action.count} />}
                </Button>
            )}
        >
            <Modal
                destroyOnHidden
                footer={null}
                onCancel={() => setRevisionsOpen(false)}
                open={revisionsOpen}
                styles={DIALOG_BODY}
                title={t('browser.module.revisions')}
                width={900}
            >
                <RevisionsPanel
                    beforeOpen={confirmDiscard}
                    branch={project.branch}
                    currentRevision={project.revision}
                    projectId={project.id}
                    searchable={supportsRevisionSearch({ features: project.repositoryInfo?.features })}
                    onOpened={() => {
                        setRevisionsOpen(false)
                        onRevisionOpened?.()
                    }}
                />
            </Modal>
            <Modal
                destroyOnHidden
                footer={null}
                onCancel={() => setLocalChangesOpen(false)}
                open={localChangesOpen}
                styles={DIALOG_BODY}
                title={t('browser.module.local_changes')}
                width={900}
            >
                <LocalChangesView
                    beforeRestore={confirmDiscard}
                    moduleName={moduleName}
                    projectId={project.id}
                    onRestored={() => {
                        setLocalChangesOpen(false)
                        onRevisionOpened?.()
                    }}
                />
            </Modal>
            {dialogs}
            <ReformatExcelFilesModal
                onClose={() => setReformatOpen(false)}
                onFormatted={() => onProjectChanged?.()}
                open={reformatOpen}
                projectId={project.id}
            />
        </CollapsingActionBar>
    )
}

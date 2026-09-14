import { useState, type MouseEvent as ReactMouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Dropdown, Modal, Space, Tooltip } from 'antd'
import { DownOutlined, ExperimentOutlined, PlusOutlined, UploadOutlined } from '@ant-design/icons'
import type { SummaryTable } from 'types/tables'
import type { Project } from '../../types/projects'
import { supportsRevisionSearch } from '../../utils/repositoryFeatures'
import { LocalChangesView } from '../projects/LocalChangesView'
import { RevisionsPanel } from '../projects/RevisionsPanel'
import { openCompareWindow } from '../projects/compare'
import { isActionAvailable, PROJECT_ACTIONS } from '../projects/projectActions'
import { ACTION_ICONS } from '../projects/projectActionIcons'
import { useProjectDialogs, type ProjectDialogActions } from '../projects/useProjectDialogs'

/** The history and the local changes are read in a window over the module, not on a screen of their own. */
const DIALOG_BODY = { body: { maxHeight: '70vh', overflow: 'auto' } }

/** The project's own actions the editor offers, in the order the old Editor kept them. */
const PROJECT_LEVEL: Array<keyof ProjectDialogActions> = ['save', 'sync', 'deploy', 'copy']

interface ModuleActionBarProps {
    project: Project
    moduleName: string
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
}

/**
 * What can be done to the open module, in the place the project's own actions sit — arranged as the old Editor
 * arranged them, so the row reads the same.
 *
 * Exporting the module, running its tests, writing a new table and the More menu are answered here; the
 * project's own actions are offered exactly as its own screen offers them. What can be done to the table on
 * screen belongs to the table, and stands in a band above it.
 */
export const ModuleActionBar = ({
    project,
    moduleName,
    modulePath,
    testCount,
    disabled = false,
    projectCompiled = false,
    onRevisionOpened,
    onProjectChanged,
    onTableCreated,
}: ModuleActionBarProps) => {
    const { t } = useTranslation('repository')
    const [revisionsOpen, setRevisionsOpen] = useState(false)
    const [localChangesOpen, setLocalChangesOpen] = useState(false)
    // Saving, syncing, deploying and copying belong to the project, not to the module being read: they are
    // offered here exactly as the project's own screen offers them, by the same capabilities.
    const { actions, dialogs } = useProjectDialogs(project, { onChanged: () => onProjectChanged?.() })
    // The old Editor let a module's workbook be replaced from this row; the dialog it opens is the one the
    // Files tab uses, restricted to Excel.
    const canUpdateModule = !!project.capabilities?.canWrite && !!modulePath
    // Writing a table into the project is the project's own right, so it is offered by the same capability
    // the Files tab writes by.
    const canWrite = !!project.capabilities?.canWrite

    // The panel of EPBDS-16560 answers this: it runs the project's tests, with the choice of only this
    // module's — which is the only choice left while the rest of the project is still being compiled.
    const openTests = (from: ReactMouseEvent<HTMLElement>) => {
        const { top, left, width, height } = from.currentTarget.getBoundingClientRect()
        window.dispatchEvent(new CustomEvent('openTestsLaunch', {
            detail: {
                projectId: project.id,
                moduleName,
                anchor: { top, left, width, height },
                moduleOnlyLocked: !projectCompiled,
            },
        }))
    }

    // What the old More menu offered: the project's own history first, then what is about its tables.
    const more = [
        ...(project.capabilities?.canViewHistory ? [{ key: 'revisions', label: t('browser.module.revisions') }] : []),
        { key: 'localChanges', label: t('browser.module.local_changes') },
        { type: 'divider' as const },
        { key: 'dependencies', label: t('browser.module.dependencies') },
        { key: 'compare', label: t('browser.module.compare') },
    ]

    const chooseMore = (key: string) => {
        if (key === 'revisions') {
            setRevisionsOpen(true)
        } else if (key === 'localChanges') {
            setLocalChangesOpen(true)
        } else if (key === 'dependencies') {
            window.dispatchEvent(new CustomEvent('openTableGraphModal', {
                detail: { projectId: project.id, projectName: project.name, module: moduleName },
            }))
        } else if (key === 'compare') {
            openCompareWindow({ id: project.id })
        }
    }

    /** One of the project's own actions, offered only where the project says it is allowed. */
    const projectAction = (id: keyof ProjectDialogActions) => isActionAvailable(project, id) && (
        <Button
            key={id}
            data-testid={`module-${id}`}
            disabled={disabled}
            icon={ACTION_ICONS[id]}
            onClick={actions[id]}
            type={id === 'save' ? 'primary' : 'default'}
        >
            {t(PROJECT_ACTIONS[id].labelKey)}
        </Button>
    )

    return (
        <Space data-testid="module-actions">
            {PROJECT_LEVEL.map(projectAction)}
            {canUpdateModule && (
                <Button
                    data-testid="module-update"
                    disabled={disabled}
                    icon={<UploadOutlined />}
                    onClick={() => window.dispatchEvent(new CustomEvent('openUpdateModuleModal', {
                        detail: { projectId: project.id, modulePath, onSuccess: () => onProjectChanged?.() },
                    }))}
                >
                    {t('browser.module.update')}
                </Button>
            )}
            <Tooltip title={modulePath ? undefined : t('browser.module.export_unavailable')}>
                <Button
                    data-testid="module-export"
                    disabled={disabled || !modulePath}
                    icon={ACTION_ICONS.export}
                    onClick={() => window.dispatchEvent(new CustomEvent('openExportProjectModal', {
                        detail: { projectId: project.id, filePath: modulePath },
                    }))}
                >
                    {t('browser.module.export')}
                </Button>
            </Tooltip>
            <Button
                data-testid="module-test"
                disabled={disabled || !testCount}
                icon={<ExperimentOutlined />}
                onClick={openTests}
            >
                {t('browser.module.test')}
                {!!testCount && (
                    <Badge color="blue" count={testCount} data-testid="module-test-count" />
                )}
            </Button>
            {canWrite && (
                <Button
                    data-testid="module-createTable"
                    disabled={disabled}
                    icon={<PlusOutlined />}
                    onClick={() => window.dispatchEvent(new CustomEvent('openCreateTableModal', {
                        detail: {
                            projectId: project.id,
                            currentModuleName: moduleName,
                            onSuccess: onTableCreated,
                        },
                    }))}
                >
                    {t('browser.module.createTable')}
                </Button>
            )}
            <Dropdown
                disabled={disabled}
                menu={{ items: more, onClick: ({ key }) => chooseMore(key) }}
                trigger={['click']}
            >
                <Button data-testid="module-more" disabled={disabled}>
                    {t('browser.module.more')} <DownOutlined />
                </Button>
            </Dropdown>
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
                <LocalChangesView moduleName={moduleName} projectId={project.id} />
            </Modal>
            {dialogs}
        </Space>
    )
}

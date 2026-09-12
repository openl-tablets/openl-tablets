import { useState, type MouseEvent as ReactMouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Badge, Button, Dropdown, Modal, Space, Tooltip } from 'antd'
import { DownOutlined } from '@ant-design/icons'
import type { Project } from '../../types/projects'
import { supportsRevisionSearch } from '../../utils/repositoryFeatures'
import { LocalChangesView } from '../projects/LocalChangesView'
import { RevisionsPanel } from '../projects/RevisionsPanel'
import { openCompareWindow } from '../projects/compare'

/** The history and the local changes are read in a window over the module, not on a screen of their own. */
const DIALOG_BODY = { body: { maxHeight: '70vh', overflow: 'auto' } }

/** The actions that arrive with the editing phase; they stand in their old places, saying so. */
const PLANNED = ['copy', 'update', 'createTable'] as const

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
}

/**
 * What can be done to the open module, in the place the project's own actions sit — arranged as the old Editor
 * arranged them, so the row reads the same.
 *
 * Exporting the module, running its tests and the More menu are answered here; the rest belong to editing and
 * stand disabled, naming the phase they arrive with. What can be done to the table on screen belongs to the
 * table, and stands in a band above it.
 */
export const ModuleActionBar = ({
    project,
    moduleName,
    modulePath,
    testCount,
    disabled = false,
    projectCompiled = false,
    onRevisionOpened,
}: ModuleActionBarProps) => {
    const { t } = useTranslation('repository')
    const [revisionsOpen, setRevisionsOpen] = useState(false)
    const [localChangesOpen, setLocalChangesOpen] = useState(false)

    const planned = (key: string) => (
        <Tooltip key={key} title={t('browser.module.planned')}>
            <Button disabled data-testid={`module-${key}`}>{t(`browser.module.${key}`)}</Button>
        </Tooltip>
    )

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

    return (
        <Space data-testid="module-actions">
            {planned(PLANNED[0])}
            {planned(PLANNED[1])}
            <Tooltip title={modulePath ? undefined : t('browser.module.export_unavailable')}>
                <Button
                    data-testid="module-export"
                    disabled={disabled || !modulePath}
                    onClick={() => window.dispatchEvent(new CustomEvent('openExportProjectModal', {
                        detail: { projectId: project.id, filePath: modulePath },
                    }))}
                >
                    {t('browser.module.export')}
                </Button>
            </Tooltip>
            <Button data-testid="module-test" disabled={disabled || !testCount} onClick={openTests}>
                {t('browser.module.test')}
                {!!testCount && (
                    <Badge color="blue" count={testCount} data-testid="module-test-count" />
                )}
            </Button>
            {planned(PLANNED[2])}
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
        </Space>
    )
}

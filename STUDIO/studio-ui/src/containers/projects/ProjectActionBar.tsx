import { useMemo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Popconfirm, Space, type DropdownProps } from 'antd'
import { DownOutlined, PlusOutlined } from '@ant-design/icons'
import { SplitButton } from '../../components/SplitButton'
import type { Project } from '../../types/projects'
import { isActionAvailable, PROJECT_ACTIONS, type ActionId, type BusyId } from './projectActions'
import { ACTION_ICONS } from './projectActionIcons'
import { MenuButton } from '../../components/MenuButton'
import { addFilesMenuItems, type AddFilesKind } from './AddFilesMenu'
import { CollapsingActionBar, type ActionMenu } from '../../components/CollapsingActionBar'


/** A button of the bar: an action of the project, or the Add menu, which adds files to the project. */
type BarActionId = ActionId | 'addFiles'

/**
 * Every action in display order: Save while there is something to save, then Close — or Open, in its place, on a
 * project that is not open — Copy, Sync, Deploy, Export, Add and Compare, then the rest. Opening an earlier revision
 * is not a button of its own while the project can still be opened: it hangs off Open. Once the project is open it
 * is a button of the rest.
 */
const ORDER: BarActionId[] = [
    'save',
    'close',
    'open',
    'copy',
    'sync',
    'deploy',
    'export',
    'addFiles',
    'compare',
    'openRevision',
    'deleteBranch',
    'delete',
    'unlock'
]

/**
 * The first available of these becomes the single primary button. Opening a revision is never it: on an
 * open project the primary action stays Close.
 */
const PRIMARY_LADDER: ActionId[] = ['save', 'open', 'close']

export type ProjectActionHandlers = Record<ActionId, () => void>

interface ActionBase {
    id: BarActionId
    testId: string
    label: string
    icon: ReactNode
    danger?: boolean
    confirm?: string
}

/** A button that runs an action of the project. */
interface RunAction extends ActionBase {
    run: () => void
}

/** A button whose only job is to open a menu: its entries, and what picking one does. */
interface MenuAction extends ActionBase {
    menu: NonNullable<DropdownProps['menu']>
}

type ActionDesc = RunAction | MenuAction

interface ProjectActionBarProps {
    project: Project
    /**
     * What the project is busy with, or null. Drives per-button loading and bar-wide disabling. Work that
     * no button of its own offers — a branch switch — spins nowhere here, but blocks the bar just the same.
     */
    pendingId: BusyId | null
    handlers: ProjectActionHandlers
    /**
     * Adds a folder or a file where the reader is: into the folder shown, beside the file shown, or at the root of the
     * project. The Add menu is offered only with it.
     */
    onAddFiles?: ((kind: AddFilesKind) => void) | undefined
}

/**
 * The single home for every project action, resolved strictly from the server-provided access model.
 * The first available action is the primary button and the rest follow in a fixed order; the two that
 * destroy the project confirm first and are the only red ones.
 *
 * The bar never wraps. When the actions do not all fit in the row, the trailing ones collapse into
 * an overflow menu behind a three-dots button, leaving at least the primary action in view.
 */
export const ProjectActionBar = ({ project, pendingId, handlers, onAddFiles }: ProjectActionBarProps) => {
    const { t } = useTranslation('repository')

    const { actions, revision, primaryId } = useMemo(() => {
        const isAvailable = (id: ActionId) => isActionAvailable(project, id)
        const addFiles = (add: (kind: AddFilesKind) => void): MenuAction => ({
            id: 'addFiles',
            testId: `addFiles-${project.id}`,
            label: t('browser.files.add'),
            icon: <PlusOutlined />,
            menu: { items: addFilesMenuItems(t), onClick: ({ key }) => add(key as AddFilesKind) },
        })
        const buildDesc = (id: ActionId): RunAction => ({
            id,
            testId: `${id}-${project.id}`,
            label: t(PROJECT_ACTIONS[id].labelKey),
            icon: ACTION_ICONS[id],
            run: handlers[id],
            ...(PROJECT_ACTIONS[id].danger ? { danger: true } : {}),
            // Only unlock confirms inline via Popconfirm; delete delegates to the global delete modal.
            ...(id === 'unlock' ? { confirm: t(`browser.${id}_confirm`) } : {}),
        })

        // Opening a revision rides in the Open menu while the project can be opened, and stands as a button of its own
        // once the project is already open.
        const canOpen = isAvailable('open')
        return {
            actions: ORDER.flatMap((id): ActionDesc[] => {
                if (id === 'addFiles') {
                    // Writing files is what the Add menu needs, the same right the files of a folder are added with.
                    return onAddFiles && project.capabilities?.canWrite ? [addFiles(onAddFiles)] : []
                }
                if (id === 'openRevision') {
                    return !canOpen && isAvailable(id) ? [buildDesc(id)] : []
                }
                return isAvailable(id) ? [buildDesc(id)] : []
            }),
            revision: isAvailable('openRevision') ? buildDesc('openRevision') : null,
            primaryId: PRIMARY_LADDER.find(isAvailable) ?? null,
        }
    }, [project, t, handlers, onAddFiles])

    const busy = pendingId !== null

    if (actions.length === 0 && !revision) {
        return null
    }

    const buttonProps = (action: ActionDesc) => ({
        'data-testid': action.testId,
        disabled: busy && pendingId !== action.id,
        icon: action.icon,
        loading: pendingId === action.id,
        ...(action.danger ? { danger: true } : {}),
        ...(action.id === primaryId ? { type: 'primary' as const } : {}),
    })

    // Run from the menu of the bar, an action closes the menu first.
    const renderAction = (action: ActionDesc, menu?: ActionMenu) => {
        const closing = (run: () => void) => () => {
            menu?.close()
            run()
        }
        if ('menu' in action) {
            const { onClick } = action.menu
            return (
                <MenuButton
                    {...buttonProps(action)}
                    menu={{
                        ...action.menu,
                        onClick: info => {
                            menu?.close()
                            onClick?.(info)
                        },
                    }}
                >
                    {action.label}
                </MenuButton>
            )
        }
        // Open carries opening an earlier revision as its menu item while the project is still closed.
        if (action.id === 'open' && revision) {
            return (
                <SplitButton
                    {...buttonProps(action)}
                    arrowLabel={revision.label}
                    arrowTestId={`${action.testId}-more`}
                    onClick={closing(action.run)}
                    menu={{
                        items: [{
                            key: revision.id,
                            label: <span data-testid={revision.testId}>{revision.label}</span>,
                        }],
                        onClick: closing(revision.run),
                    }}
                >
                    {action.label}
                </SplitButton>
            )
        }
        if (action.confirm) {
            return (
                <Popconfirm onConfirm={closing(action.run)} title={action.confirm}>
                    <Button {...buttonProps(action)}>{action.label}</Button>
                </Popconfirm>
            )
        }
        return <Button {...buttonProps(action)} onClick={closing(action.run)}>{action.label}</Button>
    }

    // A plain, id-free copy of an action, sized like the real one, so measuring never clashes with the
    // live buttons' test ids.
    const renderMeasure = (action: ActionDesc) => {
        if ('menu' in action) {
            return <Button icon={action.icon}>{action.label}<DownOutlined /></Button>
        }
        if (action.id === 'open' && revision) {
            return (
                <Space.Compact>
                    <Button icon={action.icon}>{action.label}</Button>
                    <Button icon={<DownOutlined />} />
                </Space.Compact>
            )
        }
        return <Button icon={action.icon}>{action.label}</Button>
    }

    return (
        <CollapsingActionBar
            actions={actions}
            // Depends on the whole action set, not just its size: a different set of the same size (e.g. a project
            // opened, so its buttons change and widen) must measure again, not reuse the stale fit.
            deps={[actions]}
            keyOf={action => action.id}
            menuDisabled={busy}
            menuLabel={t('home.row_actions')}
            render={renderAction}
            renderMeasure={renderMeasure}
            testId="project-actions"
        />
    )
}

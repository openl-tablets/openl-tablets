import { useCallback, useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Dropdown, Popconfirm, Space } from 'antd'
import {
    CopyOutlined,
    DeleteOutlined,
    DiffOutlined,
    DownloadOutlined,
    DownOutlined,
    FolderOpenOutlined,
    HistoryOutlined,
    MergeOutlined,
    MinusCircleOutlined,
    MoreOutlined,
    RocketOutlined,
    SaveOutlined,
    UnlockOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { SplitButton } from '../../components/SplitButton'
import type { Project } from '../../types/projects'
import { isActionAvailable, PROJECT_ACTIONS, type ActionId } from './projectActions'

export type { ActionId }

/** Gap between buttons, in px — kept in sync with the `bar` style so the fit maths matches the layout. */
const GAP = 8
/** Space reserved for the project title beside the bar, so collapsing actions never hides it entirely. */
const TITLE_MIN = 80
/** The gap of the title row the bar sits in — kept in sync with `titleRow` in {@link ProjectDetail}. */
const TITLE_ROW_GAP = 12

const useStyles = createStyles(({ css }) => ({
    bar: css`
        position: relative;
        display: flex;
        flex: none;
        flex-wrap: nowrap;
        align-items: center;
        justify-content: flex-end;
        gap: ${GAP}px;
    `,
    /** An off-screen copy of every action, laid out at its natural width, read to decide what fits. */
    measure: css`
        position: absolute;
        top: 0;
        left: 0;
        display: flex;
        gap: ${GAP}px;
        visibility: hidden;
        pointer-events: none;
    `,
    /** The collapsed actions, stacked as full-width buttons the way the row would have shown them. */
    overflow: css`
        display: flex;
        flex-direction: column;
        align-items: stretch;
        gap: 4px;
        min-width: 180px;
        padding: 4px;

        button {
            justify-content: flex-start;
        }
    `,
}))

const ACTION_ICONS: Record<ActionId, ReactNode> = {
    save: <SaveOutlined />,
    open: <FolderOpenOutlined />,
    close: <MinusCircleOutlined />,
    deploy: <RocketOutlined />,
    compare: <DiffOutlined />,
    copy: <CopyOutlined />,
    openRevision: <HistoryOutlined />,
    sync: <MergeOutlined />,
    deleteBranch: <DeleteOutlined />,
    export: <DownloadOutlined />,
    delete: <DeleteOutlined />,
    unlock: <UnlockOutlined />,
}

/**
 * Every action in display order. Opening leads, then what changes the project, then the read-only
 * operations. Opening an earlier revision is not a button of its own: it hangs off Open while the project
 * can still be opened, and replaces it once it is already open.
 */
const ORDER: ActionId[] = ['save', 'open', 'close', 'sync', 'copy', 'deleteBranch', 'delete', 'deploy',
    'compare', 'export', 'unlock']

/**
 * The first available of these becomes the single primary button. Opening a revision is never it: on an
 * open project the primary action stays Close.
 */
const PRIMARY_LADDER: ActionId[] = ['save', 'open', 'close']

export type ProjectActionHandlers = Record<ActionId, () => void>

interface ActionDesc {
    id: ActionId
    testId: string
    label: string
    run: () => void
    confirm?: string
}

interface ProjectActionBarProps {
    project: Project
    /** Id of the action currently running, or null. Drives per-button loading and bar-wide disabling. */
    pendingId: ActionId | null
    handlers: ProjectActionHandlers
}

/**
 * How many leading actions fit in `available` px. When they do not all fit, room for the overflow trigger
 * of `moreWidth` is kept, and the first (primary) action is always shown, however little space there is.
 */
export const fitActionCount = (widths: number[], moreWidth: number, available: number, gap: number): number => {
    if (widths.length === 0) {
        return 0
    }
    const total = widths.reduce((sum, w) => sum + w, 0) + gap * (widths.length - 1)
    if (total <= available) {
        return widths.length
    }
    let used = 0
    let count = 0
    for (let index = 0; index < widths.length; index++) {
        const next = used + widths[index]! + (index > 0 ? gap : 0)
        if (next + gap + moreWidth > available) {
            break
        }
        used = next
        count = index + 1
    }
    return Math.max(1, count)
}

/**
 * The single home for every project action, resolved strictly from the server-provided access model.
 * The first available action is the primary button and the rest follow in a fixed order; the two that
 * destroy the project confirm first and are the only red ones.
 *
 * The bar never wraps. When the actions do not all fit beside the title, the trailing ones collapse into
 * an overflow menu behind a three-dots button, leaving at least the primary action in view.
 */
export const ProjectActionBar = ({ project, pendingId, handlers }: ProjectActionBarProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')

    const { actions, revision, primaryId } = useMemo(() => {
        const isAvailable = (id: ActionId) => isActionAvailable(project, id)
        const buildDesc = (id: ActionId): ActionDesc => ({
            id,
            testId: `${id}-${project.id}`,
            label: t(PROJECT_ACTIONS[id].labelKey),
            run: handlers[id],
            // Only unlock confirms inline via Popconfirm; delete delegates to the global delete modal.
            ...(id === 'unlock' ? { confirm: t(`browser.${id}_confirm`) } : {}),
        })

        // Opening a revision never takes a slot of its own: it rides in the Open menu, and takes the Open
        // slot itself once the project is already open.
        const openSlot = (['open', 'openRevision'] as const).find(isAvailable) ?? null
        return {
            actions: ORDER.flatMap(id => {
                if (id === 'open') {
                    return openSlot ? [buildDesc(openSlot)] : []
                }
                return isAvailable(id) ? [buildDesc(id)] : []
            }),
            revision: isAvailable('openRevision') ? buildDesc('openRevision') : null,
            primaryId: PRIMARY_LADDER.find(isAvailable) ?? null,
        }
    }, [project, t, handlers])

    const busy = pendingId !== null

    const barRef = useRef<HTMLDivElement>(null)
    const measureRef = useRef<HTMLDivElement>(null)
    const [visibleCount, setVisibleCount] = useState(actions.length)

    const recompute = useCallback(() => {
        const measure = measureRef.current
        const parent = barRef.current?.parentElement
        if (!measure || !parent) {
            return
        }
        const nodes = Array.from(measure.children) as HTMLElement[]
        const widths = nodes.slice(0, actions.length).map(node => node.offsetWidth)
        const moreWidth = nodes.at(-1)?.offsetWidth ?? 0
        // Before the browser has laid the bar out (and in jsdom, which reports no widths) keep every
        // action rather than collapse blindly.
        if (parent.clientWidth <= 0 || widths.some(width => width <= 0)) {
            setVisibleCount(actions.length)
            return
        }
        setVisibleCount(fitActionCount(widths, moreWidth, parent.clientWidth - TITLE_MIN - TITLE_ROW_GAP, GAP))
        // Depends on the whole action set, not just its size: a different set of the same size (e.g. a
        // project opened, so its buttons change and widen) must re-measure, not reuse the stale fit.
    }, [actions])

    useLayoutEffect(() => {
        recompute()
        const parent = barRef.current?.parentElement
        if (!parent || typeof ResizeObserver === 'undefined') {
            return
        }
        const observer = new ResizeObserver(() => recompute())
        observer.observe(parent)
        return () => observer.disconnect()
    }, [recompute])

    if (actions.length === 0 && !revision) {
        return null
    }

    const buttonProps = (action: ActionDesc) => ({
        'data-testid': action.testId,
        disabled: busy && pendingId !== action.id,
        icon: ACTION_ICONS[action.id],
        loading: pendingId === action.id,
        ...(PROJECT_ACTIONS[action.id].danger ? { danger: true } : {}),
        ...(action.id === primaryId ? { type: 'primary' as const } : {}),
    })

    const renderAction = (action: ActionDesc) => {
        // Open carries opening an earlier revision as its menu item while the project is still closed.
        if (action.id === 'open' && revision) {
            return (
                <SplitButton
                    key={action.id}
                    {...buttonProps(action)}
                    arrowLabel={revision.label}
                    arrowTestId={`${action.testId}-more`}
                    onClick={action.run}
                    menu={{
                        items: [{
                            key: revision.id,
                            label: <span data-testid={revision.testId}>{revision.label}</span>,
                        }],
                        onClick: revision.run,
                    }}
                >
                    {action.label}
                </SplitButton>
            )
        }
        if (action.confirm) {
            return (
                <Popconfirm key={action.id} onConfirm={action.run} title={action.confirm}>
                    <Button {...buttonProps(action)}>{action.label}</Button>
                </Popconfirm>
            )
        }
        return <Button key={action.id} {...buttonProps(action)} onClick={action.run}>{action.label}</Button>
    }

    // A plain, id-free copy of an action, sized like the real one, so measuring never clashes with the
    // live buttons' test ids.
    const renderMeasure = (action: ActionDesc) => {
        if (action.id === 'open' && revision) {
            return (
                <Space.Compact key={action.id}>
                    <Button icon={ACTION_ICONS[action.id]}>{action.label}</Button>
                    <Button icon={<DownOutlined />} />
                </Space.Compact>
            )
        }
        return <Button key={action.id} icon={ACTION_ICONS[action.id]}>{action.label}</Button>
    }

    const shown = Math.min(visibleCount, actions.length)
    const overflow = actions.slice(shown)
    // Built once per render, not inside popupRender: a function creating elements would read as a new
    // component every time the dropdown opens.
    const overflowPanel = (
        <div className={styles.overflow} data-testid="project-actions-overflow">
            {overflow.map(renderAction)}
        </div>
    )

    return (
        <div ref={barRef} className={styles.bar} data-testid="project-actions">
            {actions.slice(0, shown).map(renderAction)}
            {overflow.length > 0 && (
                <Dropdown
                    placement="bottomRight"
                    popupRender={() => overflowPanel}
                    trigger={['click']}
                >
                    <Button
                        aria-label={t('home.row_actions')}
                        data-testid="project-actions-more"
                        disabled={busy}
                        icon={<MoreOutlined />}
                    />
                </Dropdown>
            )}
            <div ref={measureRef} aria-hidden className={styles.measure}>
                {actions.map(renderMeasure)}
                <Button icon={<MoreOutlined />} />
            </div>
        </div>
    )
}

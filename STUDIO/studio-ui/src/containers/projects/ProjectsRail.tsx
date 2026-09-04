import { readStored, writeStored } from '../../utils/localStore'
import { useCallback, useRef, useState } from 'react'
import { Button, Segmented, Tooltip } from 'antd'
import { LeftOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import type { Project } from '../../types/projects'
import type { NodeFilters } from './projectGrouping'
import type { Repository } from '../../types/repositories'
import { useSharedStyles } from './sharedStyles'
import { ProjectsTree, type ProjectsSource } from './ProjectsTree'

/** What the rail shows: the facet filters, or the grouped project tree. */
export type RailMode = 'filters' | 'tree'

const STORAGE_KEY = 'openl.projects.rail'
const WIDTH_STORAGE_KEY = 'openl.projects.rail.width'
const COLLAPSED_STORAGE_KEY = 'openl.projects.rail.collapsed'

/** How narrow and how wide the rail may be dragged. */
const MIN_WIDTH = 200
const MAX_WIDTH = 720
const DEFAULT_WIDTH = 256

const loadRailWidth = (): number => {
    const stored = Number(readStored(WIDTH_STORAGE_KEY))
    return Number.isFinite(stored) && stored >= MIN_WIDTH && stored <= MAX_WIDTH ? stored : DEFAULT_WIDTH
}

const saveRailWidth = (width: number) => writeStored(WIDTH_STORAGE_KEY, String(width))

export const loadRailMode = (): RailMode => readStored(STORAGE_KEY) === 'tree' ? 'tree' : 'filters'

const saveRailMode = (mode: RailMode) => writeStored(STORAGE_KEY, mode)

const loadCollapsed = (): boolean => readStored(COLLAPSED_STORAGE_KEY) === 'yes'

const saveCollapsed = (collapsed: boolean) => writeStored(COLLAPSED_STORAGE_KEY, collapsed ? 'yes' : 'no')

const useStyles = createStyles(({ css, token }) => ({
    rail: css`
        position: relative;
        flex: none;
    `,
    /** The panel put away: a strip carrying the handle that brings it back. */
    collapsed: css`
        width: 28px;
        min-width: 28px;
        align-items: center;
        padding-top: 8px;
    `,
    /** The row the panel is folded and unfolded from, above whatever the panel shows. */
    top: css`
        padding: 8px 12px 0;
    `,
    segmented: css`
        width: 100%;
    `,
    /** The edge the rail is dragged by; it widens on hover so it can be grabbed without aiming. */
    resizer: css`
        position: absolute;
        top: 0;
        right: -3px;
        bottom: 0;
        width: 6px;
        height: auto;
        margin: 0;
        border: none;
        background: transparent;
        cursor: col-resize;
        touch-action: none;
        z-index: 2;

        &:hover,
        &:active {
            background: ${token.colorPrimaryBorder};
        }
    `,
}))

interface ProjectsRailBaseProps {
    /** The design repositories, when the screen has read them; the tree manages without. */
    repositories?: Repository[] | undefined
    currentProjectId?: string | undefined
    onOpenProject: (project: Project) => void
    /** A repository or tag group was picked in the tree: show the projects it holds. */
    onOpenGroup: (filters: NodeFilters) => void
    /** The title of the tree was picked: show every project again. */
    onShowAll: () => void
    /** The filter facets, rendered when the rail is in its filter mode, with the rail's own actions. */
    filters?: (headerActions: React.ReactNode) => React.ReactNode
    /** Which mode the rail starts in; the rail remembers what the user picks afterwards. */
    initialMode?: RailMode
    /** Bumped by the screen when it changed the workspace, so the tree reads it again. */
    reloadToken?: number | undefined
}

/**
 * The rail carries the same projects source the tree does: the screen either supplies the projects it
 * already loaded (and the refresh that re-reads them) or, on a single-project page, supplies neither and
 * the tree reads them itself.
 */
type ProjectsRailProps = ProjectsRailBaseProps & ProjectsSource

/**
 * The left rail of the project screens. It carries the filters of the list, and — for a user who thinks
 * in folders rather than facets — the same projects as a grouped tree, which is also the quickest way to
 * step from one project to another while looking at one.
 *
 * The tree is only built once the user asks for it, and it never holds up the screen beside it.
 */
export const ProjectsRail = (props: ProjectsRailProps) => {
    const {
        repositories,
        currentProjectId,
        onOpenProject,
        onOpenGroup,
        onShowAll,
        filters,
        initialMode,
        reloadToken,
    } = props
    // Forward the projects source as one unit so its controlled/uncontrolled shape survives the hop: a
    // controlled rail always hands the tree its refresh, an uncontrolled one hands it neither.
    const treeSource: ProjectsSource = props.projects !== undefined
        ? { projects: props.projects, onRefresh: props.onRefresh }
        : {}
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [mode, setMode] = useState<RailMode>(() => initialMode ?? loadRailMode())
    const [width, setWidth] = useState(loadRailWidth)
    const [collapsed, setCollapsed] = useState(loadCollapsed)
    const railRef = useRef<HTMLElement>(null)


    const pick = (next: RailMode) => {
        setMode(next)
        saveRailMode(next)
    }

    const fold = (next: boolean) => {
        setCollapsed(next)
        saveCollapsed(next)
    }

    // The handle sits on the header row of whatever the rail shows, beside that panel's own actions.
    const foldHandle = (
        <Tooltip title={t('home.tree.hide_panel')}>
            <Button
                aria-label={t('home.tree.hide_panel')}
                data-testid="projects-rail-collapse"
                icon={<LeftOutlined />}
                onClick={() => fold(true)}
                size="small"
                type="text"
            />
        </Tooltip>
    )

    // Dragging the edge sizes the rail; the width it is left at is where it opens next time.
    const startResize = useCallback((event: React.PointerEvent<HTMLDivElement>) => {
        event.preventDefault()
        const left = railRef.current?.getBoundingClientRect().left ?? 0
        const widthAt = (moved: PointerEvent) =>
            Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, Math.round(moved.clientX - left)))
        const resize = (moved: PointerEvent) => setWidth(widthAt(moved))
        const stop = (moved: PointerEvent) => {
            resize(moved)
            window.removeEventListener('pointermove', resize)
            window.removeEventListener('pointerup', stop)
            saveRailWidth(widthAt(moved))
        }
        window.addEventListener('pointermove', resize)
        window.addEventListener('pointerup', stop)
    }, [])

    if (collapsed) {
        return (
            <aside className={cx(shared.rail, styles.rail, styles.collapsed)} data-testid="projects-rail-collapsed">
                <Tooltip title={t('home.tree.show_panel')}>
                    <Button
                        aria-label={t('home.tree.show_panel')}
                        data-testid="projects-rail-expand"
                        icon={<RightOutlined />}
                        onClick={() => fold(false)}
                        size="small"
                        type="text"
                    />
                </Tooltip>
            </aside>
        )
    }

    return (
        <aside ref={railRef} className={cx(shared.rail, styles.rail)} data-testid="projects-rail" style={{ width }}>
            {filters && (
                <div className={styles.top}>
                    <Segmented
                        block
                        className={styles.segmented}
                        data-testid="projects-rail-mode"
                        onChange={value => pick(value as RailMode)}
                        size="small"
                        value={mode}
                        options={[
                            { label: t('home.tree.mode_filters'), value: 'filters' },
                            { label: t('home.tree.mode_tree'), value: 'tree' },
                        ]}
                    />
                </div>
            )}
            {mode === 'tree' || !filters
                ? (
                    <ProjectsTree
                        {...treeSource}
                        currentProjectId={currentProjectId}
                        headerActions={foldHandle}
                        onOpenGroup={onOpenGroup}
                        onOpenProject={onOpenProject}
                        onShowAll={onShowAll}
                        reloadToken={reloadToken}
                        repositories={repositories}
                    />
                )
                : filters(foldHandle)}
            {/* A real hr: the native separator, so no role is needed for what it is. */}
            <hr
                aria-label={t('home.tree.resize')}
                className={styles.resizer}
                data-testid="projects-rail-resizer"
                onPointerDown={startResize}
            />
        </aside>
    )
}

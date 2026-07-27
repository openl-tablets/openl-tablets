import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, Tooltip } from 'antd'
import {
    DownOutlined,
    EyeInvisibleOutlined,
    HolderOutlined,
    PlusOutlined,
    RightOutlined,
    SettingOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    closestCenter,
    DndContext,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    type DragEndEvent,
} from '@dnd-kit/core'
import { restrictToParentElement, restrictToVerticalAxis } from '@dnd-kit/modifiers'
import {
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import type { FacetCount, ProjectStatusSummary, TagFacetSummary } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { useSharedStyles } from './sharedStyles'
import { RepoBadge } from './RepoBadge'
import {
    loadFilterLayout,
    moveGroup,
    orderGroups,
    REPOSITORY_GROUP,
    saveFilterLayout,
    STATUS_GROUP,
    tagGroupId,
    type FilterLayout,
} from './filterLayout'
import { LOCAL_REPO_KEY, statusCount } from './projectListing'

const STATUS_ORDER: ProjectStatus[] = [
    ProjectStatus.Local,
    ProjectStatus.Opened,
    ProjectStatus.Editing,
    ProjectStatus.ViewingVersion,
    ProjectStatus.Closed,
    ProjectStatus.Deleted,
]

const useStyles = createStyles(({ css, token }) => ({
    section: css`
        padding: 4px 16px 8px;
    `,
    /** The group caption row; its type comes from {@link useSharedStyles.microLabel}. */
    sectionHead: css`
        display: flex;
        align-items: center;
        gap: 4px;
        margin: 0 0 6px;
    `,
    /** The fold chevron of the group head, sized down; the rest is {@link useSharedStyles.sectionToggle}. */
    sectionToggle: css`
        gap: 6px;

        .anticon {
            font-size: 10px;
        }
    `,
    divider: css`
        margin: 0 16px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    headActions: css`
        display: inline-flex;
        align-items: center;
        gap: 4px;
    `,
    hidden: css`
        padding: 4px 12px 16px;
    `,
    /** The caption above the put-away groups; its type comes from {@link useSharedStyles.microLabel}. */
    hiddenHead: css`
        padding: 0 4px 2px;
    `,
    hiddenRow: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        padding: 2px 4px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    label: css`
        flex: 1;
        min-width: 0;
    `,
    count: css`
        flex: none;
        color: ${token.colorTextTertiary};
    `,
}))

interface ProjectsFilterRailProps {
    repositories: Repository[]
    statusCounts: ProjectStatusSummary | undefined
    repositoryCounts: FacetCount[] | undefined
    tagCounts: TagFacetSummary[] | undefined
    statuses: Set<string>
    repos: Set<string>
    tags: Set<string>
    onToggleStatus: (status: string) => void
    onToggleRepo: (repoId: string) => void
    onToggleTag: (key: string) => void
    onReset: () => void
    /** What the rail hangs on the header row, beside the reset of the filters. */
    headerActions?: ReactNode
}

interface FilterGroup {
    id: string
    title: string
    rows: ReactNode
}

/**
 * Left facet rail for the projects list: the repositories, then a group per tag type, then the project
 * states — the order they are asked for in, and one the user can change.
 *
 * Every group folds on its own. Rearranging the rail — dragging a group elsewhere, putting one away or
 * bringing it back — is a mode of its own, entered from the head of the rail, so the plain rail stays
 * free of controls. How the user leaves the rail is how they find it next time.
 */
export const ProjectsFilterRail = ({
    repositories,
    statusCounts,
    repositoryCounts,
    tagCounts,
    statuses,
    repos,
    tags,
    onToggleStatus,
    onToggleRepo,
    onToggleTag,
    onReset,
    headerActions,
}: ProjectsFilterRailProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [layout, setLayout] = useState<FilterLayout>(loadFilterLayout)
    const [arranging, setArranging] = useState(false)
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    )

    const apply = useCallback((next: FilterLayout) => {
        setLayout(next)
        saveFilterLayout(next)
    }, [])

    const repoCounts = useMemo(() => {
        const counts = new Map<string, number>()
        for (const repositoryCount of repositoryCounts ?? []) {
            counts.set(repositoryCount.id, repositoryCount.count)
        }
        return counts
    }, [repositoryCounts])

    const hasLocal = repoCounts.has(LOCAL_REPO_KEY)
    const hasFilters = statuses.size > 0 || repos.size > 0 || tags.size > 0

    const renderRow = (testId: string, checked: boolean, onChange: () => void, label: ReactNode, count?: number) => (
        <label key={testId} className={shared.railRow}>
            <Checkbox checked={checked} data-testid={testId} onChange={onChange} />
            <span className={cx(shared.ellipsis, styles.label)}>{label}</span>
            {count !== undefined && <span className={cx(shared.valueText, styles.count)}>{count}</span>}
        </label>
    )

    // Repositories first, a group per tag type next, the states last; a stored arrangement rearranges
    // them from here, and a tag type added since falls in at its place instead of being lost.
    const groups: FilterGroup[] = [
        {
            id: REPOSITORY_GROUP,
            title: t('home.facet_repository'),
            rows: (
                <>
                    {repositories.map(repo =>
                        renderRow(
                            `filter-repo-${repo.id}`,
                            repos.has(repo.id),
                            () => onToggleRepo(repo.id),
                            <RepoBadge name={repo.name} type={repo.type} />,
                            repoCounts.get(repo.id) ?? 0
                        )
                    )}
                    {hasLocal && renderRow(
                        `filter-repo-${LOCAL_REPO_KEY}`,
                        repos.has(LOCAL_REPO_KEY),
                        () => onToggleRepo(LOCAL_REPO_KEY),
                        <RepoBadge name={t('home.local')} type="repo-file" />,
                        repoCounts.get(LOCAL_REPO_KEY) ?? 0
                    )}
                </>
            ),
        },
        ...(tagCounts ?? []).map(facet => ({
            id: tagGroupId(facet.type),
            title: facet.type,
            rows: (
                <>
                    {facet.values.map(({ id, count }) => {
                        const key = `${facet.type}:${id}`
                        return renderRow(`filter-tag-${key}`, tags.has(key), () => onToggleTag(key), id, count)
                    })}
                </>
            ),
        })),
        {
            id: STATUS_GROUP,
            title: t('home.facet_status'),
            rows: (
                <>
                    {STATUS_ORDER
                        // A state no project is in is noise and is not offered. A ticked one stays even at
                        // zero so it can be unticked; until the counts arrive every state is kept.
                        .filter(status => statusCounts === undefined
                            || statusCount(statusCounts, status) > 0
                            || statuses.has(status))
                        .map(status =>
                            renderRow(
                                `filter-status-${status}`,
                                statuses.has(status),
                                () => onToggleStatus(status),
                                t(STATUS_META[status].labelKey),
                                statusCount(statusCounts, status)
                            )
                        )}
                </>
            ),
        },
    ]

    const arranged = orderGroups(groups, layout.order)
    const shown = arranged.filter(group => !layout.hidden.includes(group.id))
    const hidden = arranged.filter(group => layout.hidden.includes(group.id))

    const onDragEnd = ({ active, over }: DragEndEvent) => {
        if (!over || active.id === over.id) {
            return
        }
        // The stored order names every group, so moving one never loses the place of another.
        apply({ ...layout, order: moveGroup(arranged.map(group => group.id), String(active.id), String(over.id)) })
    }

    return (
        <>
            <div className={shared.railHead}>
                <span>{t('home.filters')}</span>
                <span className={styles.headActions}>
                    {hasFilters && !arranging && (
                        <Button data-testid="projects-filter-reset" onClick={onReset} size="small" type="link">
                            {t('home.reset')}
                        </Button>
                    )}
                    {arranging ? (
                        <Button
                            data-testid="projects-filter-arrange-done"
                            onClick={() => setArranging(false)}
                            size="small"
                            type="link"
                        >
                            {t('home.filter_group.done')}
                        </Button>
                    ) : (
                        <Tooltip title={t('home.filter_group.customize')}>
                            <Button
                                aria-label={t('home.filter_group.customize')}
                                data-testid="projects-filter-arrange"
                                icon={<SettingOutlined />}
                                onClick={() => setArranging(true)}
                                size="small"
                                type="text"
                            />
                        </Tooltip>
                    )}
                    {headerActions}
                </span>
            </div>
            <div className={shared.railScroll}>
                <DndContext
                    collisionDetection={closestCenter}
                    modifiers={[restrictToVerticalAxis, restrictToParentElement]}
                    onDragEnd={onDragEnd}
                    sensors={sensors}
                >
                    <SortableContext items={shown.map(group => group.id)} strategy={verticalListSortingStrategy}>
                        {shown.map((group, index) => (
                            <SortableGroup
                                key={group.id}
                                arranging={arranging}
                                collapsed={layout.collapsed.includes(group.id)}
                                first={index === 0}
                                group={group}
                                onHide={() => apply({ ...layout, hidden: [...layout.hidden, group.id]})}
                                onToggle={() => apply({
                                    ...layout,
                                    collapsed: layout.collapsed.includes(group.id)
                                        ? layout.collapsed.filter(id => id !== group.id)
                                        : [...layout.collapsed, group.id],
                                })}
                            />
                        ))}
                    </SortableContext>
                </DndContext>
                {arranging && hidden.length > 0 && (
                    <div className={styles.hidden} data-testid="filter-hidden">
                        <div className={cx(shared.microLabel, styles.hiddenHead)}>
                            {t('home.filter_group.hidden', { count: hidden.length })}
                        </div>
                        {hidden.map(group => (
                            <div key={group.id} className={styles.hiddenRow}>
                                <span className={shared.ellipsis}>{group.title}</span>
                                <Tooltip title={t('home.filter_group.show')}>
                                    <Button
                                        aria-label={t('home.filter_group.show')}
                                        data-testid={`filter-show-${group.id}`}
                                        icon={<PlusOutlined />}
                                        size="small"
                                        type="text"
                                        onClick={() => apply({
                                            ...layout,
                                            hidden: layout.hidden.filter(id => id !== group.id),
                                        })}
                                    />
                                </Tooltip>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </>
    )
}

interface SortableGroupProps {
    group: FilterGroup
    collapsed: boolean
    /** The first group carries no divider above it. */
    first: boolean
    /** Whether the rail is being arranged: only then can the group be moved or put away. */
    arranging: boolean
    onToggle: () => void
    onHide: () => void
}

/** One group of the rail: folds by its head, and while the rail is arranged moves by its grip. */
const SortableGroup = ({ group, collapsed, first, arranging, onToggle, onHide }: SortableGroupProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
        id: group.id,
        disabled: !arranging,
    })

    return (
        <div
            ref={setNodeRef}
            className={isDragging ? shared.dragging : undefined}
            data-testid={`filter-group-${group.id}`}
            style={{ transform: CSS.Transform.toString(transform), transition }}
        >
            {!first && <div className={styles.divider} />}
            <div className={styles.section}>
                <div className={cx(shared.microLabel, styles.sectionHead)}>
                    {arranging && (
                        <span
                            {...attributes}
                            {...listeners}
                            aria-label={t('home.filter_group.move')}
                            className={shared.dragHandle}
                            data-testid={`filter-drag-${group.id}`}
                        >
                            <HolderOutlined />
                        </span>
                    )}
                    <button
                        aria-expanded={!collapsed}
                        className={cx(shared.microLabel, shared.sectionToggle, styles.sectionToggle)}
                        data-testid={`filter-toggle-${group.id}`}
                        onClick={onToggle}
                        type="button"
                    >
                        <span className={shared.ellipsis}>{group.title}</span>
                        {collapsed ? <RightOutlined /> : <DownOutlined />}
                    </button>
                    {arranging && (
                        <Tooltip title={t('home.filter_group.hide')}>
                            <Button
                                aria-label={t('home.filter_group.hide')}
                                data-testid={`filter-hide-${group.id}`}
                                icon={<EyeInvisibleOutlined />}
                                onClick={onHide}
                                size="small"
                                type="text"
                            />
                        </Tooltip>
                    )}
                </div>
                {!collapsed && group.rows}
            </div>
        </div>
    )
}

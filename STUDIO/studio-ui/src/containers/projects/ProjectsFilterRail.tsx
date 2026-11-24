import { useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, ConfigProvider, Tooltip, type ThemeConfig } from 'antd'
import {
    DownOutlined,
    EyeInvisibleOutlined,
    HolderOutlined,
    PlusOutlined,
    RightOutlined,
    SettingOutlined,
} from '@ant-design/icons'
import { createStyles, useTheme } from 'antd-style'
import {
    closestCenter,
    DndContext,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    type Active,
    type Announcements,
    type DragEndEvent,
    type Over,
} from '@dnd-kit/core'
import { restrictToParentElement, restrictToVerticalAxis } from '@dnd-kit/modifiers'
import {
    hasSortableData,
    SortableContext,
    sortableKeyboardCoordinates,
    useSortable,
    verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { IconAction } from '../../components/IconAction'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import type { FacetCount, ProjectStatusSummary, TagFacetSummary } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { useSharedStyles } from './sharedStyles'
import { RepoBadge } from './RepoBadge'
import { BranchMarks } from './BranchMarks'
import { TagTypeMark } from '../tags/tagColor'
import {
    BRANCH_GROUP,
    filterLayoutSetting,
    moveGroup,
    orderGroups,
    REPOSITORY_GROUP,
    STATUS_GROUP,
    tagGroupId,
} from './filterLayout'
import {
    isFiltered,
    LOCAL_REPO_KEY,
    statusCount,
    type BranchFacetCount,
    type FilterParam,
} from './projectListing'
import { ClearFiltersButton } from './ClearFiltersButton'
import { useStoredSetting } from '../../hooks/useStoredSetting'

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
        padding: ${token.paddingXXS}px ${token.padding}px ${token.paddingXS}px;
    `,
    tagMark: css`
        margin-inline-end: ${token.marginXXS}px;
    `,
    /** A branch facet row: the name reads in the rail's own colour and size, only the marks are borrowed. */
    branchFacet: css`
        display: inline-flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        min-width: 0;
    `,
    /** The group caption row; its type comes from {@link useSharedStyles.microLabel}. */
    sectionHead: css`
        display: flex;
        align-items: center;
        gap: ${token.marginXXS}px;
        margin: 0 0 ${token.marginXXS}px;
    `,
    /** The place of the check box that picks every value of a group: centred over the check boxes of its values. */
    pickAll: css`
        display: inline-flex;
        justify-content: center;
        box-sizing: content-box;
        width: ${token.controlInteractiveSize}px;
        padding-left: ${token.paddingXS}px;
        margin-right: ${token.marginXXS}px;
    `,
    /** The fold chevron of the group head, sized down; the rest is {@link useSharedStyles.sectionToggle}. */
    sectionToggle: css`
        gap: ${token.marginXXS}px;

        .anticon {
            font-size: ${token.fontSizeIcon - 2}px;
        }
    `,
    /** The drag handle of a group being arranged; a touch on it drags the group instead of scrolling the rail. */
    handle: css`
        display: flex;
        flex: 1;
        align-items: center;
        align-self: stretch;
        gap: ${token.marginXXS}px;
        min-width: 0;
        cursor: grab;
        touch-action: none;

        &:active {
            cursor: grabbing;
        }
    `,
    divider: css`
        margin: 0 ${token.margin}px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    headActions: css`
        gap: ${token.marginXXS}px;
    `,
    hidden: css`
        padding: ${token.paddingXXS}px ${token.paddingSM}px ${token.padding}px;
    `,
    /** The caption above the put-away groups; its type comes from {@link useSharedStyles.microLabel}. */
    hiddenHead: css`
        padding: 0 ${token.paddingXXS}px ${token.paddingXXS / 2}px;
    `,
    hiddenRow: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: ${token.marginXS}px;
        padding: ${token.paddingXXS / 2}px ${token.paddingXXS}px;
        color: ${token.colorTextTertiary};
        font-size: ${token.fontSizeSM}px;
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
    branchCounts: BranchFacetCount[] | undefined
    statuses: Set<string>
    repos: Set<string>
    tags: Set<string>
    branches: Set<string>
    /** Picks the given values of one group, or drops them from the picks. */
    onPick: (param: FilterParam, values: string[], picked: boolean) => void
    /** Clears every pick, bringing the list back to its default view. */
    onClearFilters: () => void
    /** What the rail hangs on the header row, beside the actions of the filters themselves. */
    headerActions?: ReactNode
}

interface FilterValue {
    value: string
    label: ReactNode
    count: number
}

interface FilterGroup {
    id: string
    title: string
    /** What marks the group before its title, such as the tag icon of a tag type in the color of the type. */
    icon?: ReactNode
    /** The parameter of the list that the values of the group pick. */
    param: FilterParam
    /** The values the group offers, in the order it lists them. */
    values: FilterValue[]
    /** The values picked, of this group and maybe of others. */
    picked: Set<string>
}

/**
 * Left facet rail for the projects list: the repositories, the branches, then a group per tag type, then
 * the project states — the order they are asked for in, and one the user can change.
 *
 * Every group folds on its own. Rearranging the rail — dragging a group elsewhere, putting one away or
 * bringing it back — is a mode of its own, entered from the head of the rail, so the plain rail stays
 * free of controls. How the user leaves the rail is how they find it next time.
 *
 * The header row has no title: while a value is picked, it starts with the button that clears the filters.
 */
export const ProjectsFilterRail = ({
    repositories,
    statusCounts,
    repositoryCounts,
    tagCounts,
    branchCounts,
    statuses,
    repos,
    tags,
    branches,
    onPick,
    onClearFilters,
    headerActions,
}: ProjectsFilterRailProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [layout, apply] = useStoredSetting(filterLayoutSetting)
    const [arranging, setArranging] = useState(false)
    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 4 } }),
        useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
    )
    // What a screen reader hears while a group is moved: the group by its title, and the place it takes.
    const accessibility = useMemo(() => {
        const name = ({ data, id }: Active | Over): string => {
            const title: unknown = data.current?.['title']
            return typeof title === 'string' ? title : String(id)
        }
        // The place a group is over, counted from one, or nothing while it is over none.
        const placeOf = (over: Over | null) => (hasSortableData(over)
            ? { place: over.data.current.sortable.index + 1, count: over.data.current.sortable.items.length }
            : undefined)
        const announcements: Announcements = {
            onDragStart: ({ active }) => t('home.filter_group.picked_up', { name: name(active) }),
            onDragOver: ({ active, over }) => {
                const place = placeOf(over)
                return place && t('home.filter_group.moved', { name: name(active), ...place })
            },
            onDragEnd: ({ active, over }) => {
                const place = placeOf(over)
                return place && t('home.filter_group.dropped', { name: name(active), ...place })
            },
            onDragCancel: ({ active }) => t('home.filter_group.cancelled', { name: name(active) }),
        }
        return { announcements, screenReaderInstructions: { draggable: t('home.filter_group.instructions') } }
    }, [t])

    const repoCounts = useMemo(() => {
        const counts = new Map<string, number>()
        for (const repositoryCount of repositoryCounts ?? []) {
            counts.set(repositoryCount.id, repositoryCount.count)
        }
        return counts
    }, [repositoryCounts])

    const hasLocal = repoCounts.has(LOCAL_REPO_KEY)
    const hasFilters = isFiltered({ statuses, repositories: repos, tags, branches })

    // The groups in the order asked for; a stored arrangement rearranges them from here, and a tag type
    // added since falls in at its place instead of being lost.
    const groups: FilterGroup[] = [
        {
            id: REPOSITORY_GROUP,
            title: t('home.facet_repository'),
            param: 'repo',
            picked: repos,
            values: [
                ...repositories.map(repo => ({
                    value: repo.id,
                    label: <RepoBadge name={repo.name} type={repo.type} />,
                    count: repoCounts.get(repo.id) ?? 0,
                })),
                ...(hasLocal ? [{
                    value: LOCAL_REPO_KEY,
                    label: <RepoBadge name={t('home.local')} type="repo-file" />,
                    count: repoCounts.get(LOCAL_REPO_KEY) ?? 0,
                }] : []),
            ],
        },
        {
            id: BRANCH_GROUP,
            title: t('home.facet_branch'),
            param: 'branch',
            picked: branches,
            values: (branchCounts ?? []).map(({ id, count, isDefault, isProtected }) => ({
                value: id,
                label: (
                    <span className={styles.branchFacet}>
                        <span className={shared.ellipsis}>{id}</span>
                        <BranchMarks isDefault={isDefault} isProtected={isProtected} testId={`filter-branch-label-${id}`} />
                    </span>
                ),
                count,
            })),
        },
        ...(tagCounts ?? []).map((facet): FilterGroup => ({
            id: tagGroupId(facet.type),
            title: facet.type,
            icon: <TagTypeMark className={styles.tagMark} testId={`filter-tag-mark-${facet.type}`} type={facet.type} />,
            param: 'tags',
            picked: tags,
            values: facet.values.map(({ id, count }) => ({ value: `${facet.type}:${id}`, label: id, count })),
        })),
        {
            id: STATUS_GROUP,
            title: t('home.facet_status'),
            param: 'status',
            picked: statuses,
            values: STATUS_ORDER
                // A state no project is in is noise and is not offered. A ticked one stays even at
                // zero so it can be unticked; until the counts arrive every state is kept.
                .filter(status => statusCounts === undefined
                    || statusCount(statusCounts, status) > 0
                    || statuses.has(status))
                .map(status => ({
                    value: status,
                    label: t(STATUS_META[status].labelKey),
                    count: statusCount(statusCounts, status),
                })),
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
                {hasFilters && !arranging && (
                    <ClearFiltersButton data-testid="projects-filter-clear" onClick={onClearFilters} />
                )}
                <span className={cx(shared.railHeadEnd, styles.headActions)}>
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
                        <IconAction
                            data-testid="projects-filter-arrange"
                            icon={<SettingOutlined />}
                            onClick={() => setArranging(true)}
                            size="small"
                            title={t('home.filter_group.customize')}
                        />
                    )}
                    {headerActions}
                </span>
            </div>
            <div className={shared.railScroll} data-testid="projects-filter-scroll">
                {/* A dragged group stays within the list of groups, however little of it the rail shows, and lands
                    by its centre: a group reaches any place only while all of them are short, so an arranged group
                    shows its head alone. */}
                <DndContext
                    accessibility={accessibility}
                    collisionDetection={closestCenter}
                    modifiers={[restrictToVerticalAxis, restrictToParentElement]}
                    onDragEnd={onDragEnd}
                    sensors={sensors}
                >
                    <SortableContext items={shown.map(group => group.id)} strategy={verticalListSortingStrategy}>
                        <div>
                            {shown.map((group, index) => (
                                <SortableGroup
                                    key={group.id}
                                    arranging={arranging}
                                    collapsed={layout.collapsed.includes(group.id)}
                                    first={index === 0}
                                    group={group}
                                    onHide={() => apply({ ...layout, hidden: [...layout.hidden, group.id] })}
                                    onPick={onPick}
                                    onToggle={() => apply({
                                        ...layout,
                                        collapsed: layout.collapsed.includes(group.id)
                                            ? layout.collapsed.filter(id => id !== group.id)
                                            : [...layout.collapsed, group.id],
                                    })}
                                />
                            ))}
                        </div>
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
                                <IconAction
                                    data-testid={`filter-show-${group.id}`}
                                    icon={<PlusOutlined />}
                                    size="small"
                                    title={t('home.filter_group.show')}
                                    onClick={() => apply({
                                        ...layout,
                                        hidden: layout.hidden.filter(id => id !== group.id),
                                    })}
                                />
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
    onPick: ProjectsFilterRailProps['onPick']
}

/**
 * One group of the rail: folds by its head, and picks or drops all its values by the {@link PickAllBox} before
 * the title.
 *
 * While the rail is arranged, the group shows its head alone and keeps its fold for later. The whole head
 * picks the group up, except the button that puts the group away.
 */
const SortableGroup = ({ group, collapsed, first, arranging, onToggle, onHide, onPick }: SortableGroupProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
        id: group.id,
        disabled: !arranging,
        // What a screen reader calls the group while it is being moved.
        data: { title: group.title },
    })
    const title = (
        <span className={shared.ellipsis}>
            {group.icon}
            {group.title}
        </span>
    )
    const { id, param, picked, values } = group

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
                    {arranging ? (
                        <>
                            <div
                                ref={setActivatorNodeRef}
                                {...attributes}
                                {...listeners}
                                className={styles.handle}
                                data-testid={`filter-drag-${group.id}`}
                            >
                                <span aria-hidden className={shared.dragHandle}>
                                    <HolderOutlined />
                                </span>
                                {title}
                            </div>
                            <IconAction
                                data-testid={`filter-hide-${group.id}`}
                                icon={<EyeInvisibleOutlined />}
                                onClick={onHide}
                                size="small"
                                title={t('home.filter_group.hide')}
                            />
                        </>
                    ) : (
                        <>
                            <PickAllBox className={styles.pickAll} group={group} onPick={onPick} />
                            <button
                                aria-expanded={!collapsed}
                                className={cx(shared.microLabel, shared.sectionToggle, styles.sectionToggle)}
                                data-testid={`filter-toggle-${id}`}
                                onClick={onToggle}
                                type="button"
                            >
                                {title}
                                {collapsed ? <RightOutlined /> : <DownOutlined />}
                            </button>
                        </>
                    )}
                </div>
                {!arranging && !collapsed && values.map(({ value, label, count }) => (
                    <label key={value} className={shared.railRow}>
                        <Checkbox
                            checked={picked.has(value)}
                            data-testid={`filter-${param === 'tags' ? 'tag' : param}-${value}`}
                            onChange={() => onPick(param, [value], !picked.has(value))}
                        />
                        <span className={cx(shared.ellipsis, styles.label)}>{label}</span>
                        <span className={cx(shared.valueText, styles.count)}>{count}</span>
                    </label>
                ))}
            </div>
        </div>
    )
}

interface PickAllBoxProps {
    className: string
    group: FilterGroup
    onPick: ProjectsFilterRailProps['onPick']
}

/**
 * The check box before the title of a group: picks every value the group offers, or drops them all once all are
 * picked. It shows a dash while only some are, and is disabled while the group offers none.
 *
 * It is smaller than the check boxes of the values and takes the grey of the group's caption rather than their
 * accent, so it reads as part of the head.
 */
const PickAllBox = ({ className, group: { id, title, param, picked, values }, onPick }: PickAllBoxProps) => {
    const { t } = useTranslation('repository')
    const { controlInteractiveSize, openl } = useTheme()
    // Ant Design derives its palettes from a colour, so the tokens take the colours of the palette in force.
    const theme = useMemo<ThemeConfig>(() => ({
        components: {
            Checkbox: {
                controlInteractiveSize: controlInteractiveSize - 4,
                // The dash of a partly picked group is half as wide as this.
                fontSizeLG: controlInteractiveSize - 4,
                colorPrimary: openl.textTertiary,
                colorPrimaryHover: openl.textSecondary,
                colorBorder: openl.textQuaternary,
            },
        },
    }), [controlInteractiveSize, openl])
    const pickedCount = values.filter(({ value }) => picked.has(value)).length
    const allPicked = pickedCount > 0 && pickedCount === values.length
    const label = t(allPicked ? 'home.filter_group.deselect_all' : 'home.filter_group.select_all', { name: title })

    return (
        <Tooltip title={label}>
            <span className={className}>
                <ConfigProvider theme={theme}>
                    <Checkbox
                        aria-label={label}
                        checked={allPicked}
                        data-testid={`filter-all-${id}`}
                        disabled={values.length === 0}
                        indeterminate={pickedCount > 0 && !allPicked}
                        onChange={() => onPick(param, values.map(({ value }) => value), !allPicked)}
                    />
                </ConfigProvider>
            </span>
        </Tooltip>
    )
}

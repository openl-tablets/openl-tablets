import { useCallback, useLayoutEffect, useRef, useState, type KeyboardEvent } from 'react'
import type { TFunction } from 'i18next'
import { Tag, Tooltip } from 'antd'
import { createStyles } from 'antd-style'
import { useSharedStyles } from './sharedStyles'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import { formatDateTime } from '../../utils/dateFormat'
import type { Project } from '../../types/projects'
import type { RepositoryInfo } from '../../types/repositories'
import { supportsBranches } from '../../utils/repositoryFeatures'
import { BranchLabel } from './BranchLabel'
import { BranchSwitcher } from './BranchSwitcher'
import { useTagColor } from '../tags/tagColor'
import { inTagTypeThenNameOrder, useTagTypeOrder } from '../tags/tagOrder'
import { useElementSize } from '../../hooks/useElementSize'
import { useAppTheme } from '../../providers/AppThemeProvider'

/** The space between two chips of the tags line, in pixels. */
const TAG_GAP = 4

interface ProjectRowData {
    muted: boolean
    supportsBranches: boolean
    lockLabel: string | undefined
    /** The tags of the project, each value by its tag type. */
    tags: Record<string, string>
    date: string | null
}

/** Derive the display values of the row of a project in the list. */
export function deriveProjectRow(project: Project, repoInfoOf: (project: Project) => RepositoryInfo, t: TFunction): ProjectRowData {
    const repository = repoInfoOf(project)
    return {
        muted: !!STATUS_META[project.status]?.muted,
        supportsBranches: supportsBranches(repository),
        lockLabel: project.lockInfo
            ? t('browser.locked_by', {
                by: project.lockInfo.lockedBy,
                at: formatDateTime(project.lockInfo.lockedAt) ?? project.lockInfo.lockedAt,
            })
            : undefined,
        tags: project.tags ?? {},
        date: formatDateTime(project.modifiedAt),
    }
}

/** Activate a non-button table row from the keyboard: Enter or Space run the handler. */
export const activateOnKey = (onActivate: () => void) => (event: KeyboardEvent) => {
    if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault()
        onActivate()
    }
}

const useStyles = createStyles(({ css, token }) => ({
    /**
     * One line of chips: what does not fit is taken out of the line, not wrapped onto the next one. The chips do not
     * widen the column the line is in: the line takes the width it is given and fits the chips into it.
     */
    tags: css`
        contain: inline-size;
        position: relative;
        display: flex;
        flex-wrap: nowrap;
        align-items: center;
        gap: ${TAG_GAP}px;
        width: 100%;
        min-width: 0;
        overflow: hidden;
    `,
    tag: css`
        flex: none;
        border-radius: ${token.borderRadiusSM}px;
    `,
    /** A chip kept out of the line: not shown, but still measured, so the line knows how wide it would be. */
    offLine: css`
        position: absolute;
        top: 0;
        left: 0;
        visibility: hidden;
        pointer-events: none;
    `,
    /** The tags the "+N" chip stands for, in its tooltip. */
    moreList: css`
        display: flex;
        flex-wrap: wrap;
        gap: ${TAG_GAP}px;
    `,
    branch: css`
        max-width: 200px;
        font-size: ${token.fontSizeSM}px;
    `,
}))

/**
 * How many tags of a line fit in the space it has. When not every tag fits, the room of the "+N" chip that stands
 * for the rest is kept free. Tags are taken in order: the first one that does not fit ends the line.
 *
 * @param widths the widths of the tags, in their order
 * @param available the width of the line
 * @param gap the space between two chips
 * @param moreWidth the width of the "+N" chip
 */
export const fittingTags = (widths: readonly number[], available: number, gap: number, moreWidth: number): number => {
    const all = widths.reduce((sum, width) => sum + width, 0) + gap * Math.max(widths.length - 1, 0)
    if (all <= available) {
        return widths.length
    }
    let used = moreWidth
    let count = 0
    while (count < widths.length && used + gap + (widths[count] ?? Infinity) <= available) {
        used += gap + (widths[count] ?? 0)
        count++
    }
    return count
}

/**
 * The project's tags as chips on one line. As many chips as fit are shown; the rest collapse into a "+N" chip,
 * whose tooltip shows them.
 *
 * A chip shows the value alone, in the color of its tag type, and names the type in its tooltip. The chips
 * follow the order an administrator arranged the tag types in; the tags of types not configured come last, by name.
 * The line fits the chips again whenever its width changes.
 */
export const ProjectTags = ({ tags }: { tags: Record<string, string> }) => {
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const colorOf = useTagColor()
    const entries = inTagTypeThenNameOrder(useTagTypeOrder(), Object.entries(tags), ([type]) => type)
    const { ref: observe, size } = useElementSize<HTMLDivElement>()
    const line = useRef<HTMLDivElement | null>(null)
    const setLine = useCallback((element: HTMLDivElement | null) => {
        line.current = element
        observe(element)
    }, [observe])
    const [shown, setShown] = useState(entries.length)
    const content = entries.map(([type, value]) => `${type}=${value}`).join('\n')
    // The compact density makes the chips smaller, so the line is fitted again when it changes.
    const { compact } = useAppTheme()

    // Every chip stays drawn, those off the line too, so each one can be measured before the line is fitted. A line
    // that is not laid out, hidden for one, has no width to fit the chips in, and keeps them as they are.
    // The widths keep their fractions: rounded, ten chips of 60.25 px would seem to fit in 636 px and the last be cut.
    useLayoutEffect(() => {
        const available = line.current?.getBoundingClientRect().width ?? 0
        if (line.current && available > 0) {
            const widthOf = (element: Element | null) => element?.getBoundingClientRect().width ?? 0
            const widths = [...line.current.querySelectorAll('[data-tag-chip]')].map(widthOf)
            const more = widthOf(line.current.querySelector('[data-tag-more-measure]'))
            setShown(fittingTags(widths, available, TAG_GAP, more))
        }
    }, [compact, content, size.width])

    if (entries.length === 0) {
        return null
    }
    const chip = ([type, value]: [string, string], offLine = false) => (
        <Tag
            key={type}
            data-tag-chip
            aria-hidden={offLine || undefined}
            className={cx(shared.chipTag, styles.tag, offLine && styles.offLine)}
            color={colorOf(type)}
            data-testid={`project-tag-${type}`}
            title={`${type}: ${value}`}
        >
            {value}
        </Tag>
    )
    const hidden = entries.slice(shown)
    return (
        <div ref={setLine} className={styles.tags}>
            {/* A project carries one tag per type, so the type identifies the chip. */}
            {entries.map((entry, index) => chip(entry, index >= shown))}
            {hidden.length > 0 && (
                <Tooltip
                    title={(
                        <span className={styles.moreList}>
                            {hidden.map(([type, value]) => (
                                <Tag key={type} className={cx(shared.chipTag, styles.tag)} color={colorOf(type)} title={`${type}: ${value}`}>
                                    {value}
                                </Tag>
                            ))}
                        </span>
                    )}
                >
                    <Tag className={cx(shared.chipTag, styles.tag)} data-testid="project-tags-more">+{hidden.length}</Tag>
                </Tooltip>
            )}
            {/* The widest "+N" chip the line may need, measured to keep its room free. */}
            <Tag aria-hidden data-tag-more-measure className={cx(shared.chipTag, styles.tag, styles.offLine)}>
                +{entries.length}
            </Tag>
        </div>
    )
}

/** Whether the project is shown on a branch: a local copy and a repository without branches have none. */
const showsBranch = (project: Project, repositorySupportsBranches: boolean): boolean =>
    repositorySupportsBranches && !!project.branch && project.status !== ProjectStatus.Local

/** {@link showsBranch}, resolving the repository's support from the repository itself. */
export const hasBranch = (project: Project, repository: RepositoryInfo): boolean =>
    showsBranch(project, supportsBranches(repository))

/**
 * The branch control of a project in the list: the branch it is on, switchable, and blocked while the project is
 * busy with something else. A project shown without a branch renders none.
 */
export const ProjectBranchSwitch = ({ project, supportsBranches, busy, onSwitched, onSwitching }: {
    project: Project
    supportsBranches: boolean
    /** Whether the project is occupied by another operation, which this switch waits for. */
    busy: boolean
    onSwitched: () => void | Promise<unknown>
    onSwitching?: ((project: Project, busy: boolean) => void) | undefined
}) => {
    if (!showsBranch(project, supportsBranches)) {
        return null
    }
    return (
        <BranchSwitcher
            currentBranch={project.branch}
            currentBranchDefault={project.branchDefault}
            currentBranchProtected={project.branchProtected}
            data-testid={`row-branch-${project.id}`}
            disabled={busy}
            onBusyChange={switching => onSwitching?.(project, switching)}
            onSwitched={onSwitched}
            projectId={project.id}
        />
    )
}

/** The project's branch with its marks, or nothing for a project shown without one. */
export const ProjectBranch = ({ project, supportsBranches }: { project: Project, supportsBranches: boolean }) => {
    const { styles } = useStyles()
    if (!showsBranch(project, supportsBranches)) {
        return null
    }
    return (
        <BranchLabel
            withIcon
            className={styles.branch}
            isDefault={project.branchDefault}
            isProtected={project.branchProtected}
            name={project.branch}
            testId="row-branch"
        />
    )
}

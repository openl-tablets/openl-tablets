import { useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox } from 'antd'
import { DownOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { STATUS_META } from '../../constants/projectStatusMeta'
import type { FacetCount, ProjectStatusSummary, TagFacetSummary } from '../../types/projects'
import type { Repository } from '../../types/repositories'
import { ELLIPSIS, MOCKUP } from './projectsTheme'
import { RepoBadge } from './RepoBadge'

/** Local-only projects (not yet committed to any repository) are grouped under this synthetic facet id. */
export const LOCAL_REPO_KEY = '__local__'

const STATUS_ORDER: ProjectStatus[] = [
    ProjectStatus.Local,
    ProjectStatus.Opened,
    ProjectStatus.Editing,
    ProjectStatus.ViewingVersion,
    ProjectStatus.Closed,
    ProjectStatus.Deleted,
]

const useStyles = createStyles(({ css, token }) => ({
    rail: css`
        display: flex;
        flex-direction: column;
        width: 256px;
        flex: none;
        border-right: 1px solid ${token.colorBorderSecondary};
        background: ${MOCKUP.sidebarBg};
        overflow: hidden;
    `,
    head: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px;
    `,
    headTitle: css`
        font-size: 14px;
        font-weight: 600;
    `,
    scroll: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding-bottom: 12px;
    `,
    section: css`
        padding: 8px 16px 12px;
    `,
    sectionTitle: css`
        margin: 0 0 8px;
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.06em;
        text-transform: uppercase;
    `,
    sectionToggle: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        margin: 0 0 8px;
        padding: 0;
        border: none;
        background: transparent;
        color: ${token.colorTextTertiary};
        cursor: pointer;
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.06em;
        text-align: left;
        text-transform: uppercase;

        .anticon {
            font-size: 10px;
        }
    `,
    tagFacet: css`
        margin-top: 10px;

        &:first-of-type {
            margin-top: 0;
        }
    `,
    tagFacetTitle: css`
        margin: 0 0 6px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
        font-weight: 500;
    `,
    divider: css`
        margin: 0 16px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 4px 6px;
        border-radius: ${token.borderRadiusSM}px;
        cursor: pointer;
        font-size: 14px;

        &:hover {
            background: ${MOCKUP.accent};
        }
    `,
    label: css`
        flex: 1;
        min-width: 0;
        ${ELLIPSIS}
    `,
    count: css`
        flex: none;
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 12px;
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
}

/**
 * Left facet rail for the projects list. Repositories, statuses and each tag type are shown as
 * checkbox facets with live counts — a project's repository is a facet here, not a navigation hierarchy.
 * Counts are supplied by the server-side projects summary so the list can render one page at a time.
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
}: ProjectsFilterRailProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const [tagsOpen, setTagsOpen] = useState(true)

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
        <label key={testId} className={styles.row}>
            <Checkbox checked={checked} data-testid={testId} onChange={onChange} />
            <span className={styles.label}>{label}</span>
            {count !== undefined && <span className={styles.count}>{count}</span>}
        </label>
    )

    return (
        <aside className={styles.rail} data-testid="projects-filter-rail">
            <div className={styles.head}>
                <span className={styles.headTitle}>{t('home.filters')}</span>
                {hasFilters && (
                    <Button data-testid="projects-filter-reset" onClick={onReset} size="small" type="link">
                        {t('home.reset')}
                    </Button>
                )}
            </div>
            <div className={styles.scroll}>
                <div className={styles.section}>
                    <h3 className={styles.sectionTitle}>{t('home.facet_status')}</h3>
                    {STATUS_ORDER.map(status =>
                        renderRow(
                            `filter-status-${status}`,
                            statuses.has(status),
                            () => onToggleStatus(status),
                            t(STATUS_META[status].labelKey),
                            countStatus(statusCounts, status)
                        )
                    )}
                </div>
                <div className={styles.divider} />
                <div className={styles.section}>
                    <h3 className={styles.sectionTitle}>{t('home.facet_repository')}</h3>
                    {repositories.map(repo =>
                        renderRow(
                            `filter-repo-${repo.id}`,
                            repos.has(repo.id),
                            () => onToggleRepo(repo.id),
                            <RepoBadge name={repo.name} type={repo.type} />,
                            repoCounts.get(repo.id) ?? 0
                        )
                    )}
                    {hasLocal &&
                        renderRow(
                            `filter-repo-${LOCAL_REPO_KEY}`,
                            repos.has(LOCAL_REPO_KEY),
                            () => onToggleRepo(LOCAL_REPO_KEY),
                            <RepoBadge name={t('home.local')} type="repo-file" />,
                            repoCounts.get(LOCAL_REPO_KEY) ?? 0
                        )}
                </div>
                {(tagCounts ?? []).length > 0 && (
                    <>
                        <div className={styles.divider} />
                        <div className={styles.section} data-testid="filter-tags">
                            <button
                                aria-expanded={tagsOpen}
                                className={styles.sectionToggle}
                                data-testid="filter-tags-toggle"
                                onClick={() => setTagsOpen(open => !open)}
                                type="button"
                            >
                                <span>{t('home.facet_tags')}</span>
                                {tagsOpen ? <DownOutlined /> : <RightOutlined />}
                            </button>
                            {tagsOpen && (tagCounts ?? []).map(facet => (
                                <div key={facet.type} className={styles.tagFacet}>
                                    <h4 className={styles.tagFacetTitle}>{facet.type}</h4>
                                    {facet.values.map(({ id, count }) => {
                                        const key = `${facet.type}:${id}`
                                        return renderRow(`filter-tag-${key}`, tags.has(key), () => onToggleTag(key), id, count)
                                    })}
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </div>
        </aside>
    )
}

const countStatus = (counts: ProjectStatusSummary | undefined, status: ProjectStatus): number => {
    if (!counts) {
        return 0
    }
    return {
        [ProjectStatus.Local]: counts.local,
        [ProjectStatus.Opened]: counts.opened,
        [ProjectStatus.Editing]: counts.editing,
        [ProjectStatus.ViewingVersion]: counts.viewingVersion,
        [ProjectStatus.Closed]: counts.closed,
        [ProjectStatus.Deleted]: counts.deleted,
    }[status]
}

import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { Button, Tag, Tooltip } from 'antd'
import {
    BranchesOutlined,
    CodeOutlined,
    DownOutlined,
    EditOutlined,
    ExportOutlined,
    FileTextOutlined,
    ImportOutlined,
    LockOutlined,
    ProductOutlined,
    ProfileOutlined,
    RightOutlined,
    SafetyOutlined,
    SwapOutlined,
    UnlockOutlined,
} from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { ProjectStatus } from '../../constants/project'
import { COMPILE_RELEVANT_STATUSES } from '../../constants/projectStatusMeta'
import type { Project, ProjectDependency } from '../../types/projects'
import { formatDateTime } from '../../utils/dateFormat'
import {
    subscribeProjectStatus,
    type ProjectCompileState,
    type ProjectStatusDetailedMessage,
    type ProjectStatusUpdate,
} from '../../services/projectStatus'
import { ELLIPSIS, MOCKUP } from './projectsTheme'
import { StatusPill } from './StatusIndicator'
import { CompileDot, getCompileTooltip } from './CompileIndicator'
import { MonoChip } from './MonoChip'
import { RepoBadge } from './RepoBadge'
import { GitCommitMessage } from './GitCommitMessage'

const COMPILE_MESSAGES_PAGE_SIZE = 10
const COMPILE_MESSAGE_PREVIEW_CHARS = 260
const COMPILE_MESSAGE_PREVIEW_LINES = 4

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 24px;
        padding: 16px;

        @media (max-width: 1100px) {
            grid-template-columns: 1fr;
        }
    `,
    left: css`
        display: flex;
        flex-direction: column;
        gap: 16px;
        min-width: 0;
    `,
    right: css`
        display: flex;
        flex-direction: column;
        gap: 20px;
        min-width: 0;
    `,
    compilePanel: css`
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
        background: ${token.colorBgContainer};
    `,
    banner: css`
        width: 100%;
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 12px;
        border: 0;
        background: transparent;
        color: ${token.colorText};
        cursor: pointer;
        font: inherit;
        text-align: left;
    `,
    bannerStatic: css`
        cursor: default;
    `,
    bannerSpacer: css`
        flex: 1;
    `,
    bannerToggle: css`
        margin-left: auto;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    compileMessages: css`
        display: flex;
        flex-direction: column;
        gap: 12px;
        max-height: min(52vh, 480px);
        overflow-y: auto;
        padding: 0 12px 12px;
        border-top: 1px solid ${token.colorBorderSecondary};
    `,
    compileMessageGroup: css`
        padding-top: 12px;
    `,
    compileMessageTitle: css`
        margin: 0 0 6px;
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    compileMessageTitleError: css`
        color: ${token.colorErrorText};
    `,
    compileMessageTitleWarning: css`
        color: ${token.colorWarningText};
    `,
    compileMessageList: css`
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    compileMessage: css`
        padding: 6px 8px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        white-space: pre-wrap;
        word-break: break-word;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
    compileMessageAction: css`
        margin-top: 2px;
        padding: 0;
        height: auto;
        font-size: 12px;
    `,
    compileMessagePager: css`
        display: flex;
        gap: 8px;
        margin-top: 8px;
    `,
    lockBanner: css`
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 8px 12px;
        border: 1px solid ${token.colorWarningBorder};
        border-radius: ${token.borderRadius}px;
        background: ${token.colorWarningBg};
        font-size: 14px;

        .anticon-lock {
            color: ${token.colorWarning};
        }
    `,
    lockUnlock: css`
        margin-left: auto;
    `,
    sectionTitle: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin: 0 0 8px;
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        font-weight: 500;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    sectionTitleText: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;

        .anticon {
            font-size: 13px;
        }
    `,
    twoCol: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 16px;

        @media (max-width: 640px) {
            grid-template-columns: 1fr;
        }
    `,
    description: css`
        margin: 0;
        font-size: 14px;
        line-height: 1.6;
        color: ${token.colorTextSecondary};
    `,
    rows: css`
        list-style: none;
        margin: 0;
        padding: 0;
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 10px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
        background: ${token.colorBgContainer};
        font-size: 14px;
        min-width: 0;

        .anticon {
            flex: none;
            color: ${token.colorTextTertiary};
        }
    `,
    rowName: css`
        flex: 1;
        min-width: 0;
        ${ELLIPSIS}
    `,
    rowLink: css`
        flex: 1;
        min-width: 0;
        ${ELLIPSIS}
        color: ${token.colorLink};

        &:hover {
            color: ${token.colorLinkHover};
        }
    `,
    rowMeta: css`
        flex: none;
    `,
    meta: css`
        margin: 0;
    `,
    metaRow: css`
        padding: 10px 0;
        border-top: 1px solid ${token.colorBorderSecondary};

        &:first-of-type {
            border-top: none;
            padding-top: 0;
        }
    `,
    metaLabel: css`
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
        letter-spacing: 0.05em;
        text-transform: uppercase;
    `,
    metaValue: css`
        margin-top: 4px;
        font-size: 14px;
    `,
    metaSub: css`
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    statusValue: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
    `,
    branchValue: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;

        .anticon {
            color: ${token.colorTextTertiary};
            font-size: 13px;
        }

        .anticon-safety {
            color: ${token.colorInfo};
        }
    `,
    tags: css`
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
    `,
    tagPair: css`
        display: inline-flex;
        align-items: center;
        gap: 6px;
        margin: 0;
        padding: 2px 8px;
        border-radius: ${token.borderRadiusSM}px;
    `,
    tagType: css`
        color: ${token.colorTextTertiary};
    `,
    tagArrow: css`
        color: ${token.colorTextQuaternary};
    `,
    tagValue: css`
        font-weight: 500;
    `,
    patterns: css`
        display: flex;
        flex-direction: column;
        gap: 6px;
    `,
    pattern: css`
        padding: 6px 8px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusSM}px;
        background: ${token.colorFillQuaternary};
        font-family: ${MOCKUP.fontMono};
        font-size: 12px;
        word-break: break-all;
    `,
    exposedGroup: css`
        display: flex;
        flex-direction: column;
        gap: 12px;
    `,
    exposedLabel: css`
        margin: 0 0 6px;
        font-size: 12px;
        font-weight: 500;
    `,
    exposedIncludes: css`
        color: ${token.colorSuccessText};
    `,
    exposedExcludes: css`
        color: ${token.colorErrorText};
    `,
    chips: css`
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
    `,
    chip: css`
        padding: 1px 8px;
        border-radius: ${token.borderRadiusSM}px;
        border: 1px solid ${token.colorBorderSecondary};
        font-family: ${MOCKUP.fontMono};
        font-size: 12px;
    `,
    chipInclude: css`
        border-color: ${token.colorSuccessBorder};
        background: ${token.colorSuccessBg};
        color: ${token.colorText};
    `,
    chipExclude: css`
        border-color: ${token.colorErrorBorder};
        background: ${token.colorErrorBg};
        color: ${token.colorText};
    `,
}))

const buildStatus = (project: Project, state: ProjectCompileState, supportsBranches: boolean): ProjectStatusUpdate => ({
    projectId: project.id,
    branch: supportsBranches ? project.branch || null : null,
    compileState: state,
})

const errorMessagesOf = (status: ProjectStatusUpdate): ProjectStatusDetailedMessage[] =>
    (status.compilation?.messages?.items ?? []).filter(message => message.severity === 'ERROR')

const warningMessagesOf = (status: ProjectStatusUpdate): ProjectStatusDetailedMessage[] =>
    (status.compilation?.messages?.items ?? []).filter(message => message.severity === 'WARN')

const truncateCompileMessage = (value: string): string => {
    const lines = value.split(/\r?\n/)
    const byLines = lines.length > COMPILE_MESSAGE_PREVIEW_LINES
        ? lines.slice(0, COMPILE_MESSAGE_PREVIEW_LINES).join('\n')
        : value
    return byLines.length > COMPILE_MESSAGE_PREVIEW_CHARS
        ? byLines.slice(0, COMPILE_MESSAGE_PREVIEW_CHARS).trimEnd()
        : byLines
}

const isLongCompileMessage = (value: string): boolean =>
    value.length > COMPILE_MESSAGE_PREVIEW_CHARS || value.split(/\r?\n/).length > COMPILE_MESSAGE_PREVIEW_LINES

const CompileMessageText = ({ value }: { value: string }) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [expanded, setExpanded] = useState(false)
    const long = isLongCompileMessage(value)
    const text = !long || expanded ? value : `${truncateCompileMessage(value)}...`

    useEffect(() => {
        setExpanded(false)
    }, [value])

    return (
        <>
            {text}
            {long && (
                <div>
                    <Button
                        className={styles.compileMessageAction}
                        onClick={() => setExpanded(value => !value)}
                        size="small"
                        type="link"
                    >
                        {expanded ? t('browser.compile.show_less') : t('browser.compile.show_more_text')}
                    </Button>
                </div>
            )}
        </>
    )
}

const CompileMessageGroup = ({
    title,
    messages,
    titleClassName,
}: {
    title: string
    messages: ProjectStatusDetailedMessage[]
    titleClassName: string
}) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const [visibleCount, setVisibleCount] = useState(COMPILE_MESSAGES_PAGE_SIZE)

    useEffect(() => {
        setVisibleCount(COMPILE_MESSAGES_PAGE_SIZE)
    }, [messages])

    if (messages.length === 0) {
        return null
    }
    const visibleMessages = messages.slice(0, visibleCount)
    const remaining = messages.length - visibleCount
    return (
        <div className={styles.compileMessageGroup}>
            <h4 className={cx(styles.compileMessageTitle, titleClassName)}>{title}</h4>
            <ul className={styles.compileMessageList}>
                {visibleMessages.map(message => (
                    <li key={message.id} className={styles.compileMessage} data-testid={`compile-message-${message.id}`}>
                        <CompileMessageText value={message.summary} />
                    </li>
                ))}
            </ul>
            {(remaining > 0 || visibleCount > COMPILE_MESSAGES_PAGE_SIZE) && (
                <div className={styles.compileMessagePager}>
                    {remaining > 0 && (
                        <Button
                            onClick={() => setVisibleCount(count => count + COMPILE_MESSAGES_PAGE_SIZE)}
                            size="small"
                            type="link"
                        >
                            {t('browser.compile.show_more', {
                                count: Math.min(COMPILE_MESSAGES_PAGE_SIZE, remaining),
                            })}
                        </Button>
                    )}
                    {visibleCount > COMPILE_MESSAGES_PAGE_SIZE && (
                        <Button
                            onClick={() => setVisibleCount(COMPILE_MESSAGES_PAGE_SIZE)}
                            size="small"
                            type="link"
                        >
                            {t('browser.compile.show_less')}
                        </Button>
                    )}
                </div>
            )}
        </div>
    )
}

/** Live compilation banner. Subscribes to pushed status for compile-relevant projects; idle otherwise. */
const CompileBanner = ({ project, supportsBranches = true }: { project: Project, supportsBranches?: boolean }) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')
    const live = COMPILE_RELEVANT_STATUSES.has(project.status)
    const [expanded, setExpanded] = useState(false)
    const [status, setStatus] = useState<ProjectStatusUpdate>(
        live
            ? project.compileStatus ?? buildStatus(project, 'compiling', supportsBranches)
            : buildStatus(project, 'idle', supportsBranches)
    )

    useEffect(() => {
        if (!live) {
            setStatus(buildStatus(project, 'idle', supportsBranches))
            setExpanded(false)
            return
        }
        let cancelled = false
        setStatus(project.compileStatus ?? buildStatus(project, 'compiling', supportsBranches))
        const subscription = subscribeProjectStatus(project.id, supportsBranches ? project.branch || null : null, update => {
            if (!cancelled) {
                setStatus(update)
            }
        })
        return () => {
            cancelled = true
            subscription.unsubscribe()
        }
    }, [project, live, supportsBranches])

    const state = status.compileState
    const errors = errorMessagesOf(status)
    const warnings = warningMessagesOf(status)
    const hasMessages = errors.length > 0 || warnings.length > 0
    const tooltip = getCompileTooltip(status, state, t)
    const ToggleIcon = expanded ? DownOutlined : RightOutlined

    return (
        <div className={styles.compilePanel}>
            <button
                aria-expanded={hasMessages ? expanded : undefined}
                className={cx(styles.banner, !hasMessages && styles.bannerStatic)}
                type="button"
                onClick={() => {
                    if (hasMessages) {
                        setExpanded(value => !value)
                    }
                }}
            >
                <CompileDot showLabel state={state} tooltip={tooltip} />
                <span className={styles.bannerSpacer} />
                {hasMessages && <ToggleIcon aria-hidden className={styles.bannerToggle} />}
            </button>
            {hasMessages && expanded && (
                <div className={styles.compileMessages}>
                    <CompileMessageGroup
                        messages={errors}
                        title={t('browser.compile.error_count', { count: errors.length })}
                        titleClassName={styles.compileMessageTitleError}
                    />
                    <CompileMessageGroup
                        messages={warnings}
                        title={t('browser.compile.warning_count', { count: warnings.length })}
                        titleClassName={styles.compileMessageTitleWarning}
                    />
                </div>
            )}
        </div>
    )
}

const Section = ({ icon, title, action, children }: { icon?: ReactNode; title: string; action?: ReactNode; children: ReactNode }) => {
    const { styles } = useStyles()
    return (
        <section>
            <h3 className={styles.sectionTitle}>
                <span className={styles.sectionTitleText}>
                    {icon}
                    {title}
                </span>
                {action}
            </h3>
            {children}
        </section>
    )
}

interface OverviewPanelProps {
    project: Project
    repoLabel: string
    repoType?: string | undefined
    supportsBranches?: boolean
    onEditTags: () => void
    onUnlock: () => void
}

/**
 * The project Overview tab: a live compile banner, lock state, and identity metadata on the right, with
 * descriptive sections on the left. Every section and metadata field is driven by the project model and
 * omitted entirely when it has no value — no placeholders or dashes. Depends-on and used-by entries link
 * to the referenced projects.
 */
export const OverviewPanel = ({
    project,
    repoLabel,
    repoType,
    supportsBranches = true,
    onEditTags,
    onUnlock,
}: OverviewPanelProps) => {
    const { styles, cx } = useStyles()
    const { t } = useTranslation('repository')

    const tags = Object.entries(project.tags ?? {})
    const date = formatDateTime(project.modifiedAt)
    const dependsOn = project.dependencies ?? []
    const usedBy = project.usedBy ?? []
    const modules = project.modules ?? []
    const versionPatterns = project.versionPatterns ?? []
    const includes = project.exposedMethods?.includes ?? []
    const excludes = project.exposedMethods?.excludes ?? []
    const renderExposed = (patterns: string[], labelStyle: string, chipStyle: string, labelKey: string) =>
        patterns.length > 0 && (
            <div>
                <p className={cx(styles.exposedLabel, labelStyle)}>{t(labelKey)}</p>
                <div className={styles.chips}>
                    {patterns.map(pattern => (
                        <span key={pattern} className={cx(styles.chip, chipStyle)}>{pattern}</span>
                    ))}
                </div>
            </div>
        )
    const canEditTags = project.capabilities?.canEditTags ?? false

    const metaRow = (label: string, value: ReactNode) => (
        <div className={styles.metaRow}>
            <div className={styles.metaLabel}>{label}</div>
            <div className={styles.metaValue}>{value}</div>
        </div>
    )

    const dependencyList = (deps: ProjectDependency[]) => (
        <ul className={styles.rows}>
            {deps.map(dep => (
                <li key={dep.id} className={styles.row}>
                    <ProductOutlined />
                    <Link className={styles.rowLink} to={`/projects/${encodeURIComponent(dep.id)}`}>{dep.name}</Link>
                    {dep.branch && <MonoChip className={styles.rowMeta}>{dep.branch}</MonoChip>}
                </li>
            ))}
        </ul>
    )

    return (
        <div className={styles.panel} data-testid="overview-panel">
            <div className={styles.left} data-testid="overview-left">
                <CompileBanner project={project} supportsBranches={supportsBranches} />
                {project.lockInfo && (
                    <div className={styles.lockBanner}>
                        <LockOutlined />
                        <span>{t('browser.locked_by', { by: project.lockInfo.lockedBy, at: project.lockInfo.lockedAt })}</span>
                        {project.capabilities?.canUnlock && (
                            <Button className={styles.lockUnlock} icon={<UnlockOutlined />} onClick={onUnlock} size="small" type="text">
                                {t('browser.overview.unlock')}
                            </Button>
                        )}
                    </div>
                )}
                {project.description && (
                    <Section icon={<FileTextOutlined />} title={t('browser.overview.description')}>
                        <p className={styles.description}>{project.description}</p>
                    </Section>
                )}
                {(tags.length > 0 || canEditTags) && (
                    <Section
                        title={t('browser.overview.tags')}
                        action={canEditTags && (
                            <Button data-testid="edit-tags" icon={<EditOutlined />} onClick={onEditTags} size="small" type="link">
                                {t('browser.tags.edit')}
                            </Button>
                        )}
                    >
                        {tags.length > 0 && (
                            <div className={styles.tags}>
                                {tags.map(([type, value]) => (
                                    <Tag key={type} className={styles.tagPair}>
                                        <span className={styles.tagType}>{type}</span>
                                        <span className={styles.tagArrow}>→</span>
                                        <span className={styles.tagValue}>{value}</span>
                                    </Tag>
                                ))}
                            </div>
                        )}
                    </Section>
                )}
                {modules.length > 0 && (
                    <Section icon={<ProductOutlined />} title={t('browser.overview.modules', { count: modules.length })}>
                        <ul className={styles.rows}>
                            {modules.map(module => (
                                <li key={module.name} className={styles.row}>
                                    <ProfileOutlined />
                                    <span className={styles.rowName}>{module.name}</span>
                                    {module.path && <MonoChip className={styles.rowMeta}>{module.path}</MonoChip>}
                                </li>
                            ))}
                        </ul>
                    </Section>
                )}
                {versionPatterns.length > 0 && (
                    <Section icon={<SwapOutlined />} title={t('browser.overview.version_patterns')}>
                        <div className={styles.patterns}>
                            {versionPatterns.map(pattern => (
                                <code key={pattern} className={styles.pattern}>{pattern}</code>
                            ))}
                        </div>
                    </Section>
                )}
                {(includes.length > 0 || excludes.length > 0) && (
                    <Section icon={<CodeOutlined />} title={t('browser.overview.exposed_methods')}>
                        <div className={styles.exposedGroup}>
                            {renderExposed(includes, styles.exposedIncludes, styles.chipInclude, 'browser.overview.exposed_includes')}
                            {renderExposed(excludes, styles.exposedExcludes, styles.chipExclude, 'browser.overview.exposed_excludes')}
                        </div>
                    </Section>
                )}
                {(dependsOn.length > 0 || usedBy.length > 0) && (
                    <div className={styles.twoCol}>
                        {dependsOn.length > 0 && (
                            <Section icon={<ExportOutlined />} title={t('browser.overview.depends_on')}>
                                {dependencyList(dependsOn)}
                            </Section>
                        )}
                        {usedBy.length > 0 && (
                            <Section icon={<ImportOutlined />} title={t('browser.overview.used_by')}>
                                {dependencyList(usedBy)}
                            </Section>
                        )}
                    </div>
                )}
            </div>
            <div className={styles.right} data-testid="overview-right">
                <dl className={styles.meta}>
                    {metaRow(t('browser.overview.status'), (
                        <span className={styles.statusValue}>
                            <StatusPill status={project.status} />
                            {project.status === ProjectStatus.Editing && (
                                <span className={styles.metaSub}>{t('browser.overview.unsaved')}</span>
                            )}
                        </span>
                    ))}
                    {metaRow(t('browser.overview.repository'), <RepoBadge name={repoLabel} type={repoType} />)}
                    {project.path && metaRow(t('browser.overview.path'), <MonoChip>{project.path}</MonoChip>)}
                    {supportsBranches && project.branch && metaRow(t('browser.overview.branch'), (
                        <span className={styles.branchValue}>
                            <BranchesOutlined />
                            <MonoChip>{project.branch}</MonoChip>
                            {project.branchProtected && (
                                <Tooltip title={t('browser.branch.protected_tag')}>
                                    <SafetyOutlined />
                                </Tooltip>
                            )}
                        </span>
                    ))}
                    {project.revision && metaRow(t('browser.overview.revision'), <MonoChip>{project.revision}</MonoChip>)}
                    {(project.modifiedBy || date) && metaRow(t('browser.overview.last_change'), (
                        <>
                            {project.modifiedBy}
                            {date && <div className={styles.metaSub}>{date}</div>}
                        </>
                    ))}
                    {project.comment && metaRow(t('browser.overview.comment'), (
                        <GitCommitMessage className={styles.metaSub} message={project.comment} />
                    ))}
                </dl>
            </div>
        </div>
    )
}

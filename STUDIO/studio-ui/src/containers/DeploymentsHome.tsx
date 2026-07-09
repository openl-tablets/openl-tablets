import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Input, Skeleton, Tooltip, Typography } from 'antd'
import { ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../utils/errorMessage'
import {
    getDeployment,
    getDeployments,
    getProductionRepositories,
    type Deployment,
    type DeploymentItem,
} from '../services/deployments'
import type { Repository } from '../types/repositories'
import { formatDateTime } from '../utils/dateFormat'
import { MOCKUP, MONO_TEXT } from './projects/projectsTheme'
import { RepoBadge } from './projects/RepoBadge'

type DeploymentItemsState = DeploymentItem[] | 'error' | 'loading'
type DeploymentsState = Deployment[] | 'error' | 'loading'

const useStyles = createStyles(({ css, token }) => ({
    page: css`
        display: flex;
        height: calc(100vh - 64px);
        overflow: hidden;
        background: ${token.colorBgLayout};
    `,
    rail: css`
        display: flex;
        flex-direction: column;
        width: 256px;
        flex: none;
        border-right: 1px solid ${token.colorBorderSecondary};
        background: ${MOCKUP.sidebarBg};
        overflow: hidden;
    `,
    railHead: css`
        padding: 12px 16px;
        font-size: 14px;
        font-weight: 600;
    `,
    railScroll: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
        padding: 0 10px 12px;
    `,
    repoButton: css`
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
        min-height: 34px;
        margin: 0;
        padding: 6px;
        border: none;
        border-radius: ${token.borderRadiusSM}px;
        background: transparent;
        cursor: pointer;
        text-align: left;

        &:hover {
            background: ${MOCKUP.accent};
        }
    `,
    repoButtonActive: css`
        background: ${MOCKUP.accent};
        color: ${MOCKUP.accentFg};
    `,
    repoBadge: css`
        flex: 1;
    `,
    main: css`
        display: flex;
        flex-direction: column;
        flex: 1;
        min-width: 0;
    `,
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
        background: ${token.colorBgContainer};
    `,
    headTop: css`
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    title: css`
        margin: 0;
        font-size: 18px;
        font-weight: 600;
    `,
    subtitle: css`
        margin-top: 2px;
        color: ${token.colorTextTertiary};
        font-size: 12px;
    `,
    headActions: css`
        display: flex;
        align-items: center;
        gap: 8px;
        flex: none;
    `,
    toolbar: css`
        display: flex;
        align-items: center;
        gap: 8px;
    `,
    search: css`
        width: min(360px, 100%);
    `,
    content: css`
        flex: 1;
        min-height: 0;
        overflow: auto;
    `,
    loading: css`
        padding: 24px;
    `,
    stateBox: css`
        margin: 24px;
        padding: 48px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        background: ${token.colorBgContainer};
    `,
    table: css`
        width: 100%;
        border-collapse: collapse;
        font-size: 14px;
    `,
    head: css`
        th {
            padding: 8px 12px;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            color: ${token.colorTextTertiary};
            font-family: ${MOCKUP.fontMono};
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.05em;
            text-align: left;
            text-transform: uppercase;
            white-space: nowrap;
        }

        th:first-of-type {
            padding-left: 16px;
        }
    `,
    row: css`
        td {
            padding: 12px;
            border-bottom: 1px solid ${token.colorFillQuaternary};
            vertical-align: top;
        }

        td:first-of-type {
            padding-left: 16px;
        }

        &:hover {
            background: ${token.colorFillQuaternary};
        }
    `,
    deploymentName: css`
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    `,
    expandButton: css`
        flex: none;
    `,
    chevron: css`
        color: ${token.colorTextQuaternary};
        font-size: 12px;
        transition: transform 0.15s ease;

        @media (prefers-reduced-motion: reduce) {
            transition: none;
        }
    `,
    chevronOpen: css`
        transform: rotate(90deg);
    `,
    name: css`
        min-width: 0;
        max-width: 460px;
        font-weight: 600;
    `,
    nestedRow: css`
        td {
            padding: 0;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            background: ${token.colorBgContainer};
        }
    `,
    nested: css`
        padding: 8px 16px 16px 44px;
    `,
    nestedTable: css`
        width: 100%;
        border-collapse: collapse;

        th {
            padding: 6px 10px;
            color: ${token.colorTextTertiary};
            ${MONO_TEXT}
            text-align: left;
            white-space: nowrap;
        }

        td {
            padding: 8px 10px;
            border-top: 1px solid ${token.colorFillQuaternary};
        }
    `,
    projectName: css`
        max-width: 360px;
        font-weight: 500;
    `,
    muted: css`
        color: ${token.colorTextTertiary};
    `,
    mono: css`
        ${MONO_TEXT}
    `,
    count: css`
        color: ${token.colorTextTertiary};
        ${MONO_TEXT}
    `,
    hideMd: css`
        @media (max-width: 768px) {
            display: none;
        }
    `,
    hideLg: css`
        @media (max-width: 992px) {
            display: none;
        }
    `,
}))

const deploymentMatches = (deployment: Deployment, search: string): boolean =>
    deployment.name.toLowerCase().includes(search.toLowerCase())

const projectRows = (items: DeploymentItem[], styles: ReturnType<typeof useStyles>['styles'], t: (key: string) => string) => (
    <table className={styles.nestedTable} data-testid="deployment-projects-table">
        <thead>
            <tr>
                <th>{t('deployments.col_project')}</th>
                <th>{t('deployments.col_revision')}</th>
                <th className={styles.hideMd}>{t('deployments.col_modified_by')}</th>
                <th className={styles.hideLg}>{t('deployments.col_modified_at')}</th>
            </tr>
        </thead>
        <tbody>
            {items.map(item => (
                <tr key={item.name} data-testid={`deployment-project-row-${item.name}`}>
                    <td>
                        <Typography.Text className={styles.projectName} ellipsis={{ tooltip: item.name }}>
                            {item.name}
                        </Typography.Text>
                    </td>
                    <td className={styles.mono}>{item.revision ?? '—'}</td>
                    <td className={styles.hideMd}>{item.modifiedBy ?? '—'}</td>
                    <td className={styles.hideLg}>{formatDateTime(item.modifiedAt) ?? '—'}</td>
                </tr>
            ))}
        </tbody>
    </table>
)

/**
 * Production deployments browser. It mirrors the Projects page structure while showing production repositories,
 * their deployments, and the deployed projects inside each deployment.
 */
export const DeploymentsHome = () => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const [params, setParams] = useSearchParams()
    const [repositories, setRepositories] = useState<Repository[] | null>(null)
    const [repositoriesError, setRepositoriesError] = useState<string | null>(null)
    const [deploymentsByRepo, setDeploymentsByRepo] = useState<Record<string, DeploymentsState>>({})
    const [itemsByDeployment, setItemsByDeployment] = useState<Record<string, DeploymentItemsState>>({})
    const [expanded, setExpanded] = useState<Set<string>>(() => new Set())

    const repoParam = params.get('repo') ?? ''
    const search = params.get('q') ?? ''
    const selectedRepository = useMemo(() => {
        if (!repositories || repositories.length === 0) {
            return null
        }
        return repositories.find(repo => repo.id === repoParam) ?? repositories[0] ?? null
    }, [repoParam, repositories])

    const setParam = useCallback((key: string, value: string | null) => {
        setParams(prev => {
            const next = new URLSearchParams(prev)
            if (value === null || value === '') {
                next.delete(key)
            } else {
                next.set(key, value)
            }
            return next
        }, { replace: true })
    }, [setParams])

    const loadRepositories = useCallback(() => {
        setRepositoriesError(null)
        setRepositories(null)
        getProductionRepositories()
            .then(setRepositories)
            .catch((e: unknown) => {
                setRepositories([])
                setRepositoriesError(errorMessage(e))
            })
    }, [])

    const loadDeployments = useCallback((repoId: string) => {
        setDeploymentsByRepo(prev => ({ ...prev, [repoId]: 'loading' }))
        getDeployments(repoId)
            .then(deployments => {
                setDeploymentsByRepo(prev => ({ ...prev, [repoId]: deployments }))
            })
            .catch(() => {
                setDeploymentsByRepo(prev => ({ ...prev, [repoId]: 'error' }))
            })
    }, [])

    const loadItems = useCallback((deploymentId: string) => {
        setItemsByDeployment(prev => ({ ...prev, [deploymentId]: 'loading' }))
        getDeployment(deploymentId)
            .then(deployment => {
                setItemsByDeployment(prev => ({ ...prev, [deploymentId]: deployment.items ?? []}))
            })
            .catch(() => {
                setItemsByDeployment(prev => ({ ...prev, [deploymentId]: 'error' }))
            })
    }, [])

    useEffect(() => {
        loadRepositories()
    }, [loadRepositories])

    useEffect(() => {
        if (!selectedRepository || deploymentsByRepo[selectedRepository.id] !== undefined) {
            return
        }
        loadDeployments(selectedRepository.id)
    }, [deploymentsByRepo, loadDeployments, selectedRepository])

    useEffect(() => {
        const onProjectDeployed = () => {
            if (selectedRepository) {
                loadDeployments(selectedRepository.id)
            }
        }
        window.addEventListener('projectDeployed', onProjectDeployed)
        return () => window.removeEventListener('projectDeployed', onProjectDeployed)
    }, [loadDeployments, selectedRepository])

    const currentDeploymentsState = selectedRepository ? deploymentsByRepo[selectedRepository.id] : undefined
    const deployments = Array.isArray(currentDeploymentsState) ? currentDeploymentsState : []
    const filteredDeployments = useMemo(
        () => deployments.filter(deployment => deploymentMatches(deployment, search.trim())),
        [deployments, search]
    )

    const selectRepository = (repo: Repository) => {
        setParam('repo', repo.id)
        setExpanded(new Set())
    }

    const toggleDeployment = (deployment: Deployment) => {
        setExpanded(prev => {
            const next = new Set(prev)
            if (next.has(deployment.id)) {
                next.delete(deployment.id)
            } else {
                next.add(deployment.id)
                if (itemsByDeployment[deployment.id] === undefined) {
                    loadItems(deployment.id)
                }
            }
            return next
        })
    }

    const renderDeploymentProjects = (deployment: Deployment) => {
        const state = itemsByDeployment[deployment.id]
        if (state === undefined || state === 'loading') {
            return <Skeleton active paragraph={{ rows: 2 }} title={false} />
        }
        if (state === 'error') {
            return <Alert showIcon title={t('deployments.projects_load_failed')} type="error" />
        }
        if (state.length === 0) {
            return <Empty description={t('deployments.no_projects')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return projectRows(state, styles, t)
    }

    const content = () => {
        if (repositories !== null && repositories.length === 0) {
            return (
                <div className={styles.stateBox}>
                    <Empty data-testid="deployments-empty-repositories" description={t('deployments.no_repositories')} />
                </div>
            )
        }
        if (repositories === null || currentDeploymentsState === undefined || currentDeploymentsState === 'loading') {
            return (
                <div className={styles.loading} data-testid="deployments-loading">
                    <Skeleton active paragraph={{ rows: 6 }} />
                </div>
            )
        }
        if (currentDeploymentsState === 'error') {
            return (
                <Alert
                    showIcon
                    data-testid="deployments-error"
                    title={t('deployments.load_failed')}
                    type="error"
                    action={selectedRepository && (
                        <Button onClick={() => loadDeployments(selectedRepository.id)}>
                            {t('deployments.retry')}
                        </Button>
                    )}
                />
            )
        }
        if (deployments.length === 0) {
            return (
                <div className={styles.stateBox}>
                    <Empty data-testid="deployments-empty" description={t('deployments.empty')} />
                </div>
            )
        }
        if (filteredDeployments.length === 0) {
            return (
                <div className={styles.stateBox}>
                    <Empty data-testid="deployments-no-match" description={t('deployments.no_match')} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                        <Button onClick={() => setParam('q', null)}>{t('deployments.clear_search')}</Button>
                    </Empty>
                </div>
            )
        }
        return (
            <table className={styles.table} data-testid="deployments-table">
                <thead className={styles.head}>
                    <tr>
                        <th>{t('deployments.col_deployment')}</th>
                        <th className={styles.hideMd}>{t('deployments.col_projects')}</th>
                    </tr>
                </thead>
                <tbody>
                    {filteredDeployments.map(deployment => {
                        const open = expanded.has(deployment.id)
                        const itemState = itemsByDeployment[deployment.id]
                        const itemCount = Array.isArray(itemState) ? itemState.length : undefined
                        return (
                            <Fragment key={deployment.id}>
                                <tr key={deployment.id} className={styles.row} data-testid={`deployment-row-${deployment.id}`}>
                                    <td>
                                        <div className={styles.deploymentName}>
                                            <Button
                                                aria-label={open ? t('deployments.collapse') : t('deployments.expand')}
                                                className={styles.expandButton}
                                                icon={<RightOutlined className={cx(styles.chevron, open && styles.chevronOpen)} />}
                                                onClick={() => toggleDeployment(deployment)}
                                                shape="circle"
                                                size="small"
                                                type="text"
                                            />
                                            <Typography.Text className={styles.name} ellipsis={{ tooltip: deployment.name }}>
                                                {deployment.name}
                                            </Typography.Text>
                                        </div>
                                    </td>
                                    <td className={cx(styles.hideMd, styles.count)}>
                                        {itemCount === undefined ? '—' : itemCount}
                                    </td>
                                </tr>
                                {open && (
                                    <tr key={`${deployment.id}-items`} className={styles.nestedRow}>
                                        <td colSpan={2}>
                                            <div className={styles.nested}>{renderDeploymentProjects(deployment)}</div>
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        )
                    })}
                </tbody>
            </table>
        )
    }

    if (repositoriesError) {
        return (
            <Alert
                showIcon
                data-testid="deployments-repositories-error"
                description={repositoriesError}
                title={t('deployments.repositories_load_failed')}
                type="error"
                action={(
                    <Button onClick={loadRepositories}>
                        {t('deployments.retry')}
                    </Button>
                )}
            />
        )
    }

    const repositoryCount = repositories?.length ?? 0
    const deploymentCount = Array.isArray(currentDeploymentsState) ? currentDeploymentsState.length : 0

    return (
        <div className={styles.page} data-testid="deployments-home">
            <aside className={styles.rail}>
                <div className={styles.railHead}>{t('deployments.repositories')}</div>
                <div className={styles.railScroll}>
                    {repositories === null && <Skeleton active paragraph={{ rows: 4 }} title={false} />}
                    {repositories !== null && repositories.length === 0 && (
                        <Empty data-testid="deployments-no-repositories" description={t('deployments.no_repositories')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    )}
                    {repositories?.map(repo => (
                        <button
                            key={repo.id}
                            className={cx(styles.repoButton, selectedRepository?.id === repo.id && styles.repoButtonActive)}
                            data-testid={`deployment-repository-${repo.id}`}
                            onClick={() => selectRepository(repo)}
                            type="button"
                        >
                            <RepoBadge className={styles.repoBadge} name={repo.name} type={repo.type} />
                        </button>
                    ))}
                </div>
            </aside>
            <div className={styles.main}>
                <div className={styles.header}>
                    <div className={styles.headTop}>
                        <div>
                            <h1 className={styles.title}>{t('deployments.title')}</h1>
                            <div className={styles.subtitle} data-testid="deployments-summary">
                                {selectedRepository
                                    ? t('deployments.summary', {
                                        deployments: deploymentCount,
                                        repositories: repositoryCount,
                                        repository: selectedRepository.name,
                                    })
                                    : t('deployments.summary_empty', { repositories: repositoryCount })}
                            </div>
                        </div>
                        <div className={styles.headActions}>
                            <Tooltip title={t('deployments.refresh')}>
                                <Button
                                    aria-label={t('deployments.refresh')}
                                    disabled={!selectedRepository}
                                    icon={<ReloadOutlined />}
                                    onClick={() => selectedRepository && loadDeployments(selectedRepository.id)}
                                />
                            </Tooltip>
                        </div>
                    </div>
                    <div className={styles.toolbar}>
                        <Input
                            allowClear
                            className={styles.search}
                            onChange={event => setParam('q', event.target.value)}
                            placeholder={t('deployments.search_placeholder')}
                            value={search}
                        />
                    </div>
                </div>
                <div className={styles.content}>{content()}</div>
            </div>
        </div>
    )
}

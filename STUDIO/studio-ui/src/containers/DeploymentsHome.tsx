import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, Pagination, Skeleton, Typography } from 'antd'
import { createStyles } from 'antd-style'
import { errorMessage } from '../utils/errorMessage'
import {
    getDeployments,
    getProductionRepositories,
    type Deployment,
} from '../services/deployments'
import type { Repository } from '../types/repositories'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../constants/ui'
import { useSharedStyles } from './projects/sharedStyles'
import { RepoBadge } from './projects/RepoBadge'
import { SearchInput } from '../components/SearchInput'
import { toUrlSafeId } from '../services/projectId'

type DeploymentsState = Deployment[] | 'error' | 'loading'

const useStyles = createStyles(({ css, token }) => ({
    /** The repository entry of the rail carries the badge across the whole line. */
    repoBadge: css`
        flex: 1;
    `,
    /** The search owns the toolbar row: nothing else competes for its width. */
    search: css`
        width: 100%;
    `,
    /**
     * The deployment name is the way into the deployment, so the button owns the cell: it carries the
     * padding the row would have given it, and the whole height of the line opens the deployment.
     */
    nameCell: css`
        && {
            padding: 0;
        }
    `,
    nameButton: css`
        display: flex;
        align-items: center;
        width: 100%;
        min-height: 46px;
        padding: 12px 16px;
        border: none;
        background: none;
        text-align: left;
        cursor: pointer;
        color: inherit;

        &:hover {
            color: ${token.colorPrimary};
        }
    `,
    name: css`
        min-width: 0;
        max-width: 460px;
        font-weight: 600;
    `,
    paginationBar: css`
        display: flex;
        justify-content: flex-end;
        padding: 0 16px 16px;
    `,
}))

const deploymentMatches = (deployment: Deployment, search: string): boolean =>
    deployment.name.toLowerCase().includes(search.toLowerCase())

/**
 * Production deployments browser. It mirrors the Projects page: the production repositories on the rail,
 * their deployments in the list, and a deployment opens its own page.
 */
export const DeploymentsHome = () => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const navigate = useNavigate()
    const [params, setParams] = useSearchParams()
    const [repositories, setRepositories] = useState<Repository[] | null>(null)
    const [repositoriesError, setRepositoriesError] = useState<string | null>(null)
    const [deploymentsByRepo, setDeploymentsByRepo] = useState<Record<string, DeploymentsState>>({})

    const repoParam = params.get('repo') ?? ''
    const search = params.get('q') ?? ''
    const pageSize = Number(params.get('size')) > 0 ? Number(params.get('size')) : DEFAULT_PAGE_SIZE
    const requestedPage = Number(params.get('page')) > 0 ? Number(params.get('page')) : 1
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
    const totalPages = Math.max(1, Math.ceil(filteredDeployments.length / pageSize))
    const currentPage = Math.min(requestedPage, totalPages)
    const pageDeployments = useMemo(
        () => filteredDeployments.slice((currentPage - 1) * pageSize, currentPage * pageSize),
        [currentPage, filteredDeployments, pageSize]
    )

    const selectRepository = (repo: Repository) => {
        setParam('repo', repo.id)
    }

    const openDeployment = (deployment: Deployment) => navigate(`/deployments/${toUrlSafeId(deployment.id)}`)

    const content = () => {
        if (repositories !== null && repositories.length === 0) {
            return (
                <div className={shared.stateBox}>
                    <Empty data-testid="deployments-empty-repositories" description={t('deployments.no_repositories')} />
                </div>
            )
        }
        if (repositories === null || currentDeploymentsState === undefined || currentDeploymentsState === 'loading') {
            return (
                <div className={shared.loading} data-testid="deployments-loading">
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
                <div className={shared.stateBox}>
                    <Empty data-testid="deployments-empty" description={t('deployments.empty')} />
                </div>
            )
        }
        if (filteredDeployments.length === 0) {
            return (
                <div className={shared.stateBox}>
                    <Empty data-testid="deployments-no-match" description={t('deployments.no_match')} image={Empty.PRESENTED_IMAGE_SIMPLE}>
                        <Button onClick={() => setParam('q', null)}>{t('deployments.clear_search')}</Button>
                    </Empty>
                </div>
            )
        }
        return (
            <table className={shared.listTable} data-testid="deployments-table">
                <thead className={cx(shared.listHead, shared.microLabel)}>
                    <tr>
                        <th>{t('deployments.col_deployment')}</th>
                    </tr>
                </thead>
                <tbody>
                    {pageDeployments.map(deployment => (
                        <tr key={deployment.id} className={shared.listRow} data-testid={`deployment-row-${deployment.id}`}>
                            <td className={styles.nameCell}>
                                <button
                                    className={styles.nameButton}
                                    data-testid={`deployment-open-${deployment.id}`}
                                    onClick={() => openDeployment(deployment)}
                                    type="button"
                                >
                                    <Typography.Text className={styles.name} ellipsis={{ tooltip: deployment.name }}>
                                        {deployment.name}
                                    </Typography.Text>
                                </button>
                            </td>
                        </tr>
                    ))}
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

    const deploymentCount = Array.isArray(currentDeploymentsState) ? currentDeploymentsState.length : 0

    return (
        <div className={shared.page} data-testid="deployments-home">
            <aside className={shared.rail}>
                <div className={shared.railHead}>{t('deployments.repositories')}</div>
                <div className={shared.railScroll}>
                    {repositories === null && <Skeleton active paragraph={{ rows: 4 }} title={false} />}
                    {repositories !== null && repositories.length === 0 && (
                        <Empty data-testid="deployments-no-repositories" description={t('deployments.no_repositories')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                    )}
                    {repositories?.map(repo => (
                        <button
                            key={repo.id}
                            className={cx(shared.railRow, selectedRepository?.id === repo.id && shared.railRowActive)}
                            data-testid={`deployment-repository-${repo.id}`}
                            onClick={() => selectRepository(repo)}
                            type="button"
                        >
                            <RepoBadge className={styles.repoBadge} name={repo.name} type={repo.type} />
                        </button>
                    ))}
                </div>
            </aside>
            <div className={shared.main}>
                <div className={shared.header}>
                    <div className={shared.headTop}>
                        <div>
                            <h1 className={shared.pageTitle}>{t('deployments.title')}</h1>
                            <div className={shared.subtitle} data-testid="deployments-summary">
                                {t('deployments.summary', { count: deploymentCount })}
                            </div>
                        </div>
                    </div>
                    <div>
                        <SearchInput
                            className={styles.search}
                            data-testid="deployments-search"
                            onChange={event => setParam('q', event.target.value)}
                            placeholder={t('deployments.search_placeholder')}
                            value={search}
                        />
                    </div>
                </div>
                <div className={shared.content}>
                    {content()}
                    {filteredDeployments.length > pageSize && (
                        <div className={styles.paginationBar}>
                            <Pagination
                                showSizeChanger
                                current={currentPage}
                                data-testid="deployments-pagination"
                                pageSize={pageSize}
                                pageSizeOptions={PAGE_SIZE_OPTIONS}
                                total={filteredDeployments.length}
                                onChange={(nextPage, nextSize) => {
                                    setParams(prev => {
                                        const next = new URLSearchParams(prev)
                                        const apply = (key: string, value: string | null) =>
                                            value === null ? next.delete(key) : next.set(key, value)
                                        apply('page', nextPage === 1 ? null : String(nextPage))
                                        apply('size', nextSize === DEFAULT_PAGE_SIZE ? null : String(nextSize))
                                        return next
                                    }, { replace: true })
                                }}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

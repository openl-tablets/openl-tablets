import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Collapse, Drawer, Empty, List, Skeleton } from 'antd'
import { getDeployment, getDeployments, getProductionRepositories, type Deployment, type DeploymentItem } from '../../services/deployments'
import type { Repository } from '../../types/repositories'

interface DeploymentsDrawerProps {
    open: boolean
    onClose: () => void
}

/**
 * Read-only browser for production (deployment) repositories, drilling down repository → deployment →
 * deployed projects. Repositories load when the drawer opens; each deployment's projects load lazily the
 * first time it is expanded.
 */
export const DeploymentsDrawer = ({ open, onClose }: DeploymentsDrawerProps) => {
    const { t } = useTranslation('repository')
    const [repositories, setRepositories] = useState<Repository[] | null>(null)
    const [deploymentsByRepo, setDeploymentsByRepo] = useState<Record<string, Deployment[] | 'loading'>>({})
    const [itemsByDeployment, setItemsByDeployment] = useState<Record<string, DeploymentItem[] | 'loading'>>({})

    useEffect(() => {
        if (!open || repositories !== null) {
            return
        }
        let cancelled = false
        getProductionRepositories()
            .then(repos => {
                if (!cancelled) {
                    setRepositories(repos)
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setRepositories([])
                }
            })
        return () => {
            cancelled = true
        }
    }, [open, repositories])

    const loadDeployments = (repoId: string) => {
        if (deploymentsByRepo[repoId] !== undefined) {
            return
        }
        setDeploymentsByRepo(prev => ({ ...prev, [repoId]: 'loading' }))
        getDeployments(repoId)
            .then(list => setDeploymentsByRepo(prev => ({ ...prev, [repoId]: list })))
            .catch(() => setDeploymentsByRepo(prev => ({ ...prev, [repoId]: []})))
    }

    const loadItems = (deploymentId: string) => {
        if (itemsByDeployment[deploymentId] !== undefined) {
            return
        }
        setItemsByDeployment(prev => ({ ...prev, [deploymentId]: 'loading' }))
        getDeployment(deploymentId)
            .then(detail => setItemsByDeployment(prev => ({ ...prev, [deploymentId]: detail.items ?? []})))
            .catch(() => setItemsByDeployment(prev => ({ ...prev, [deploymentId]: []})))
    }

    const projectDescription = (item: DeploymentItem): string =>
        [item.revision && t('browser.deployments.revision', { revision: item.revision }), item.modifiedBy]
            .filter(Boolean)
            .join(' · ')

    const deploymentProjects = (deploymentId: string) => {
        const items = itemsByDeployment[deploymentId]
        if (items === undefined || items === 'loading') {
            return <Skeleton active paragraph={{ rows: 2 }} title={false} />
        }
        if (items.length === 0) {
            return <Empty description={t('browser.deployments.no_projects')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
        }
        return (
            <List
                dataSource={items}
                size="small"
                renderItem={item => (
                    <List.Item data-testid={`deployment-project-${item.name}`}>
                        <List.Item.Meta description={projectDescription(item)} title={item.name} />
                    </List.Item>
                )}
            />
        )
    }

    const repositoryDeployments = (deployments: Deployment[]) => (
        <Collapse
            ghost
            onChange={keys => (Array.isArray(keys) ? keys : [keys]).forEach(key => key && loadItems(String(key)))}
            size="small"
            items={deployments.map(deployment => ({
                key: deployment.id,
                label: <span data-testid={`deployment-${deployment.id}`}>{deployment.name}</span>,
                children: deploymentProjects(deployment.id),
            }))}
        />
    )

    return (
        <Drawer
            data-testid="deployments-drawer"
            onClose={onClose}
            open={open}
            title={t('browser.deployments.title')}
            width={420}
        >
            {repositories === null && <Skeleton active paragraph={{ rows: 4 }} />}
            {repositories !== null && repositories.length === 0 && (
                <Empty data-testid="deployments-empty" description={t('browser.deployments.none')} />
            )}
            {repositories !== null && repositories.length > 0 && (
                <Collapse
                    accordion
                    items={repositories.map(repo => {
                        const entry = deploymentsByRepo[repo.id]
                        return {
                            key: repo.id,
                            label: <span data-testid={`deployment-repo-${repo.id}`}>{repo.name}</span>,
                            children: entry === undefined || entry === 'loading'
                                ? <Skeleton active paragraph={{ rows: 2 }} title={false} />
                                : entry.length === 0
                                    ? <Empty description={t('browser.deployments.no_deployments')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
                                    : repositoryDeployments(entry),
                        }
                    })}
                    onChange={keys => {
                        const key = Array.isArray(keys) ? keys[0] : keys
                        if (key) {
                            loadDeployments(String(key))
                        }
                    }}
                />
            )}
        </Drawer>
    )
}

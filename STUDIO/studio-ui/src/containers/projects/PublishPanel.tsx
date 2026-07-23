import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Alert, Skeleton, Tag } from 'antd'
import { createStyles } from 'antd-style'
import { getProductionRepositories, getProjectDeployments } from '../../services/deployments'
import { formatDateTime } from '../../utils/dateFormat'
import { MOCKUP } from './projectsTheme'
import { useSharedStyles } from './sharedStyles'
import { shortRevision } from './revisions'
import { DeployConfigPanel } from './DeployConfigPanel'
import { MonoChip } from './MonoChip'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 24px;
        padding: 16px;

        @media (max-width: 1000px) {
            grid-template-columns: 1fr;
        }
    `,
    col: css`
        min-width: 0;
    `,
    title: css`
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 12px;
        font-size: 14px;
        font-weight: 600;
    `,
    cards: css`
        display: flex;
        flex-direction: column;
        gap: 8px;
    `,
    card: css`
        padding: 12px;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
    `,
    cardTop: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
    `,
    envTag: css`
        border-radius: ${token.borderRadiusSM}px;
    `,
    cardMeta: css`
        display: flex;
        gap: 12px;
        margin-top: 6px;
        color: ${token.colorTextTertiary};
        font-family: ${MOCKUP.fontMono};
        font-size: 11px;
    `,
}))

interface PublishPanelProps {
    projectId: string
    projectName: string
    canWrite: boolean
    onChanged: () => void
    /** Bumped when the project reloads; forwarded so the deploy descriptor refetches instead of going stale. */
    reloadToken?: number
}

interface DeploymentCard {
    key: string
    service: string
    env: string
    revision?: string
    date?: string
}

interface ProjectDeployedDetail {
    projectId?: string
}

/** Rough environment colour from the production repository name (production/staging/qa). */
const envColor = (name: string): string => {
    const lower = name.toLowerCase()
    if (lower.includes('prod')) {
        return 'green'
    }
    if (lower.includes('stag')) {
        return 'gold'
    }
    if (lower.includes('qa') || lower.includes('test')) {
        return 'blue'
    }
    return 'default'
}

/**
 * The Publish tab: the project's `rules-deploy.xml` configuration on the left, and where this project is
 * currently deployed on the right (service, environment, deployed revision and date). Deploying opens the
 * shared deploy dialog.
 */
export const PublishPanel = ({
    projectId,
    projectName,
    canWrite,
    onChanged,
    reloadToken = 0,
}: PublishPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useSharedStyles()
    const { styles, cx } = useStyles()
    const [deployments, setDeployments] = useState<DeploymentCard[] | 'error' | null>(null)
    const [deploymentsReloadToken, setDeploymentsReloadToken] = useState(0)

    useEffect(() => {
        const onProjectDeployed = (event: Event) => {
            const detail = (event as CustomEvent<ProjectDeployedDetail>).detail
            if (detail?.projectId === projectId) {
                setDeploymentsReloadToken(token => token + 1)
            }
        }
        window.addEventListener('projectDeployed', onProjectDeployed)
        return () => window.removeEventListener('projectDeployed', onProjectDeployed)
    }, [projectId])

    useEffect(() => {
        let cancelled = false
        setDeployments(null)
        getProductionRepositories()
            .then(async repos => {
                const perRepo = await Promise.all(repos.map(async repo => {
                    try {
                        const list = await getProjectDeployments(repo.id, projectName)
                        return list.flatMap(deployment => {
                            const item = deployment.items?.find(candidate => candidate.name === projectName)
                            if (!item) {
                                return []
                            }
                            const card: DeploymentCard = {
                                key: `${repo.id}:${deployment.id}`,
                                service: deployment.name,
                                env: repo.name,
                                ...(item.revision ? { revision: item.revision } : {}),
                                ...(item.modifiedAt ? { date: item.modifiedAt } : {}),
                            }
                            return [card]
                        })
                    } catch {
                        return []
                    }
                }))
                if (!cancelled) {
                    setDeployments(perRepo.flat())
                }
            })
            .catch(() => { if (!cancelled) setDeployments('error') })
        return () => { cancelled = true }
    }, [deploymentsReloadToken, projectId, projectName, reloadToken])

    const cards = Array.isArray(deployments) ? deployments : []

    return (
        <div className={styles.panel} data-testid="publish-panel">
            <div className={styles.col}>
                <DeployConfigPanel
                    canWrite={canWrite}
                    onSaved={onChanged}
                    projectId={projectId}
                    reloadToken={reloadToken}
                />
            </div>
            <div className={styles.col}>
                <h3 className={styles.title}>{t('browser.publish.deployments_title')}</h3>
                {deployments === null && <Skeleton active paragraph={{ rows: 3 }} />}
                {deployments === 'error' && (
                    <Alert showIcon data-testid="publish-error" title={t('browser.publish.load_failed')} type="error" />
                )}
                {cards.length === 0 && deployments !== null && deployments !== 'error' && (
                    <div className={shared.dashedEmpty} data-testid="publish-no-deployments">
                        {t('browser.publish.no_deployments')}
                    </div>
                )}
                {cards.length > 0 && (
                    <div className={styles.cards}>
                        {cards.map(card => (
                            <div key={card.key} className={styles.card} data-testid={`publish-deployment-${card.key}`}>
                                <div className={styles.cardTop}>
                                    <MonoChip>{card.service}</MonoChip>
                                    <Tag className={cx(shared.chipTag, styles.envTag)} color={envColor(card.env)}>{card.env}</Tag>
                                </div>
                                <div className={styles.cardMeta}>
                                    {card.revision && (
                                        <span>
                                            {t('browser.deployments.revision', {
                                                revision: shortRevision(card.revision),
                                            })}
                                        </span>
                                    )}
                                    {card.date && <span>{formatDateTime(card.date)}</span>}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    )
}

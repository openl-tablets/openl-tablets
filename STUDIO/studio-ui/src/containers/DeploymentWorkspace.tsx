import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Button, Empty, Pagination, Skeleton, Tabs, Tooltip, Typography } from 'antd'
import { RocketOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { errorMessage } from '../utils/errorMessage'
import { getDeployment, type DeploymentDetail, type DesignRevision } from '../services/deployments'
import { formatDateTime } from '../utils/dateFormat'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS } from '../constants/ui'
import { useSharedStyles } from './projects/sharedStyles'
import { AuthorDate } from './projects/AuthorDate'

const useStyles = createStyles(({ css, token }) => ({
    page: css`
        display: flex;
        flex-direction: column;
        height: calc(100vh - 64px);
        overflow: hidden;
        background: ${token.colorBgContainer};
    `,
    header: css`
        padding: 12px 16px;
        border-bottom: 1px solid ${token.colorBorderSecondary};
    `,
    crumb: css`
        display: flex;
        align-items: center;
        gap: 6px;
        color: ${token.colorTextTertiary};
        font-size: 14px;

        a {
            color: ${token.colorTextSecondary};

            &:hover {
                color: ${token.colorPrimary};
            }
        }
    `,
    titleRow: css`
        display: flex;
        align-items: center;
        gap: 12px;
        min-width: 0;
        margin-top: 8px;
    `,
    title: css`
        margin: 0 !important;
        min-width: 0;
        font-size: 22px;
        font-weight: 600;
        letter-spacing: -0.01em;
    `,
    tabs: css`
        flex: 1;
        min-height: 0;
        display: flex;
        flex-direction: column;

        .ant-tabs-nav {
            flex: none;
            padding: 0 16px;
            margin: 0;
        }

        .ant-tabs-body-holder,
        .ant-tabs-body {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
        }

        .ant-tabs-content-active {
            display: flex;
            flex-direction: column;
            flex: 1;
            min-height: 0;
            overflow: auto;
        }
    `,
    projectName: css`
        max-width: 460px;
        font-weight: 500;
    `,
    paginationBar: css`
        display: flex;
        justify-content: flex-end;
        padding: 0 16px 16px;
    `,
    centered: css`
        display: flex;
        align-items: center;
        justify-content: center;
        flex: 1;
        padding: ${token.paddingXL}px;
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

/**
 * A single deployment, addressed by its id in the URL, laid out like a project's workspace: the
 * deployment names the page, and its one tab lists the projects that are deployed in it.
 */
export const DeploymentWorkspace = () => {
    const { t } = useTranslation('repository')
    const { styles, cx } = useStyles()
    const { styles: shared } = useSharedStyles()
    const navigate = useNavigate()
    const { deploymentId = '' } = useParams()
    const [deployment, setDeployment] = useState<DeploymentDetail | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [page, setPage] = useState(1)
    const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE)

    const load = useCallback(() => {
        setLoading(true)
        setError(null)
        getDeployment(deploymentId)
            .then(setDeployment)
            .catch((e: unknown) => {
                setDeployment(null)
                setError(errorMessage(e))
            })
            .finally(() => setLoading(false))
    }, [deploymentId])

    useEffect(load, [load])

    const items = useMemo(() => deployment?.items ?? [], [deployment])
    const pageItems = useMemo(
        () => items.slice((page - 1) * pageSize, page * pageSize),
        [items, page, pageSize]
    )

    /**
     * The design repository revision a deployed project was built from, read the way a business user reads
     * one: who committed it, and when. The technical revision id stays in the response, unshown.
     *
     * The revision is recognized by comparing content, which the server indexes in the background — a
     * deployment read right after it was made, or one made from another OpenL Studio, has none to show.
     */
    const designRevision = (revision: DesignRevision | undefined) => {
        if (!revision) {
            return <Tooltip title={t('deployments.revision_unknown')}><span>—</span></Tooltip>
        }
        return <AuthorDate author={revision.modifiedBy} date={formatDateTime(revision.modifiedAt)} />
    }

    const projects = () => {
        if (items.length === 0) {
            return (
                <div className={shared.stateBox}>
                    <Empty data-testid="deployment-no-projects" description={t('deployments.no_projects')} />
                </div>
            )
        }
        return (
            <table className={shared.listTable} data-testid="deployment-projects-table">
                <thead className={cx(shared.listHead, shared.microLabel)}>
                    <tr>
                        <th>{t('deployments.col_project')}</th>
                        <th>{t('deployments.col_revision')}</th>
                        <th className={styles.hideMd}>{t('deployments.col_modified_by')}</th>
                        <th className={styles.hideLg}>{t('deployments.col_modified_at')}</th>
                    </tr>
                </thead>
                <tbody>
                    {pageItems.map(item => (
                        <tr key={item.name} className={shared.listRow} data-testid={`deployment-project-row-${item.name}`}>
                            <td>
                                <Typography.Text className={styles.projectName} ellipsis={{ tooltip: item.name }}>
                                    {item.name}
                                </Typography.Text>
                            </td>
                            <td>{designRevision(item.designRevision)}</td>
                            <td className={styles.hideMd}>{item.modifiedBy ?? '—'}</td>
                            <td className={styles.hideLg}>{formatDateTime(item.modifiedAt) ?? '—'}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        )
    }

    const pagination = () => items.length > pageSize && (
        <div className={styles.paginationBar}>
            <Pagination
                showSizeChanger
                current={page}
                data-testid="deployment-projects-pagination"
                pageSize={pageSize}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                total={items.length}
                onChange={(nextPage, nextSize) => {
                    setPage(nextPage)
                    setPageSize(nextSize)
                }}
            />
        </div>
    )

    if (loading) {
        return (
            <div className={shared.loading} data-testid="deployment-loading">
                <Skeleton active paragraph={{ rows: 6 }} />
            </div>
        )
    }

    if (error || !deployment) {
        return (
            <div className={styles.centered}>
                <Empty data-testid="deployment-missing" description={error ?? t('deployments.not_found')}>
                    <Button onClick={() => navigate('/deployments')} type="primary">
                        {t('deployments.back')}
                    </Button>
                </Empty>
            </div>
        )
    }

    return (
        <div className={styles.page} data-testid="deployment-workspace">
            <div className={styles.header}>
                <div className={styles.crumb}>
                    <Link to="/deployments">{t('deployments.title')}</Link>
                    <span aria-hidden>/</span>
                </div>
                <div className={styles.titleRow}>
                    <RocketOutlined />
                    <Typography.Title className={styles.title} data-testid="deployment-title" ellipsis={{ tooltip: deployment.name }} level={3}>
                        {deployment.name}
                    </Typography.Title>
                </div>
            </div>
            <Tabs
                className={styles.tabs}
                data-testid="deployment-tabs"
                items={[{
                    key: 'projects',
                    label: t('deployments.tab_projects'),
                    children: <>{projects()}{pagination()}</>,
                }]}
            />
        </div>
    )
}

export default DeploymentWorkspace

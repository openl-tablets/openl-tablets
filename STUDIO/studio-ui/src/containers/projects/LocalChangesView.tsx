import { useCallback, useEffect, useState } from 'react'
import { Alert, Button, Checkbox, Empty, Modal, notification, Spin, Typography } from 'antd'
import { useTranslation } from 'react-i18next'
import { createStyles } from 'antd-style'
import { notifyLoadFailure } from '../../services/apiCall'
import CONFIG from '../../services/config'
import { getLocalHistory, restoreLocalHistory, type LocalHistoryItem } from '../../services/localHistory'
import { useListPageStyles } from '../../styles/listPageStyles'
import { LIST_PAGE_COLORS } from '../../styles/listPageTheme'
import { errorMessage } from '../../utils/errorMessage'

interface LocalChangesViewProps {
    projectId: string
    moduleName: string
}

interface LegacyWorkspaceApi {
    nav: {
        reload(force: boolean): void
    }
}

const reloadLegacyWorkspace = (): void => {
    const workspaceApi = (globalThis as typeof globalThis & { ws?: LegacyWorkspaceApi }).ws
    workspaceApi?.nav.reload(true)
}

const useStyles = createStyles(({ css, token }) => ({
    root: css`
        min-height: 100%;
        background: ${LIST_PAGE_COLORS.containerBg};
    `,
    table: css`
        table-layout: auto;
    `,
    fit: css`
        width: 1px;
        white-space: nowrap;
    `,
    compareCell: css`
        text-align: center !important;
    `,
    actionCell: css`
        text-align: right !important;
    `,
    actionText: css`
        display: inline-block;
        margin: 0;
        padding: 0;
        color: ${LIST_PAGE_COLORS.primary};
        font-family: inherit;
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
    `,
    selectedRow: css`
        td {
            background: ${LIST_PAGE_COLORS.accent};
        }

        &:hover td {
            background: ${LIST_PAGE_COLORS.accent};
        }
    `,
    actions: css`
        display: flex;
        justify-content: flex-end;
        padding: 0 ${token.padding}px ${token.padding}px;
    `,
    state: css`
        padding: ${token.padding}px;
    `,
}))

export const LocalChangesView = ({ projectId, moduleName }: LocalChangesViewProps) => {
    const { t } = useTranslation('repository')
    const { styles: shared } = useListPageStyles()
    const { styles, cx } = useStyles()
    const [history, setHistory] = useState<LocalHistoryItem[]>([])
    const [selected, setSelected] = useState<string[]>([])
    const [restoreItem, setRestoreItem] = useState<LocalHistoryItem | null>(null)
    const [loading, setLoading] = useState(true)
    const [loadFailed, setLoadFailed] = useState(false)
    const [restoring, setRestoring] = useState(false)
    const changeCount = history.filter(item => !item.current).length

    const loadHistory = useCallback(async () => {
        setLoading(true)
        setLoadFailed(false)
        try {
            const items = await getLocalHistory(projectId, moduleName)
            setHistory(items)
            const current = items.find(item => item.current)
            setSelected(current ? [current.id] : [])
        } catch (error) {
            setHistory([])
            setSelected([])
            setLoadFailed(true)
            notifyLoadFailure(t('browser.local_history.load_failed'), error)
        } finally {
            setLoading(false)
        }
    }, [moduleName, projectId, t])

    useEffect(() => {
        void loadHistory()
    }, [loadHistory])

    const toggleSelected = (id: string, checked: boolean) => {
        setSelected(previous => checked
            ? [id, ...previous.filter(selectedId => selectedId !== id)].slice(0, 2)
            : previous.filter(selectedId => selectedId !== id))
    }

    const compare = () => {
        const [version1, version2] = selected
        if (!version1 || !version2) {
            return
        }
        const params = new URLSearchParams({
            disableUpload: 'true',
            projectId,
            module: moduleName,
            version1,
            version2,
        })
        window.open(
            `${CONFIG.CONTEXT}/faces/pages/modules/compare.xhtml?${params}`,
            'Compare',
            'width=1240,height=700,screenX=50,screenY=100,resizable=yes,scrollbars=yes,status=yes'
        )
    }

    const restore = async () => {
        if (!restoreItem) {
            return
        }
        setRestoring(true)
        try {
            await restoreLocalHistory(projectId, moduleName, restoreItem.id)
            await loadHistory()
            notification.success({ title: t('browser.local_history.restore_succeeded') })
            setRestoreItem(null)
            reloadLegacyWorkspace()
        } catch (error) {
            notification.error({
                title: t('browser.local_history.restore_failed'),
                description: errorMessage(error),
            })
            setRestoreItem(null)
        } finally {
            setRestoring(false)
        }
    }

    return (
        <div className={cx(shared.listPageRoot, styles.root)} data-testid="local-changes-view">
            <div className={shared.header}>
                <h1 className={shared.pageTitle}>{t('browser.local_history.title')}</h1>
                <div className={shared.subtitle} data-testid="local-changes-count">
                    {t('browser.local_history.summary', { count: changeCount })}
                </div>
            </div>
            {loadFailed && (
                <div className={styles.state}>
                    <Alert showIcon title={t('browser.local_history.load_failed')} type="error" />
                </div>
            )}
            {!loadFailed && !loading && history.length === 0 && (
                <div className={shared.stateBox}>
                    <Empty
                        description={t('browser.local_history.empty')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                </div>
            )}
            {!loadFailed && (loading || history.length > 0) && (
                <>
                    <Spin spinning={loading}>
                        <table className={cx(shared.listTable, styles.table)}>
                            <thead className={cx(shared.listHead, shared.microLabel)}>
                                <tr>
                                    <th className={cx(styles.fit, styles.compareCell)}>
                                        {t('browser.local_history.compare')}
                                    </th>
                                    <th>{t('browser.local_history.modified_on')}</th>
                                    <th className={cx(styles.fit, styles.actionCell)}>
                                        {t('browser.local_history.action')}
                                    </th>
                                </tr>
                            </thead>
                            <tbody>
                                {history.map(item => (
                                    <tr
                                        key={item.id}
                                        className={cx(shared.listRow, selected.includes(item.id) && styles.selectedRow)}
                                    >
                                        <td className={cx(styles.fit, styles.compareCell)}>
                                            <Checkbox
                                                checked={selected.includes(item.id)}
                                                onChange={event => toggleSelected(item.id, event.target.checked)}
                                                aria-label={t('browser.local_history.select_version', {
                                                    modifiedOn: item.modifiedOn,
                                                })}
                                            />
                                        </td>
                                        <td>{item.modifiedOn}</td>
                                        <td className={cx(styles.fit, styles.actionCell)}>
                                            {item.current
                                                ? (
                                                    <span className={styles.actionText}>
                                                        {t('browser.local_history.current')}
                                                    </span>
                                                )
                                                : (
                                                    <Typography.Link
                                                        className={styles.actionText}
                                                        href="#restore"
                                                        onClick={(event) => {
                                                            event.preventDefault()
                                                            setRestoreItem(item)
                                                        }}
                                                    >
                                                        {t('browser.local_history.restore')}
                                                    </Typography.Link>
                                                )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </Spin>
                    {history.length > 0 && (
                        <div className={styles.actions}>
                            <Button
                                data-testid="compare-local-history"
                                disabled={selected.length !== 2}
                                onClick={compare}
                                type="primary"
                            >
                                {t('browser.local_history.compare')}
                            </Button>
                        </div>
                    )}
                </>
            )}
            <Modal
                destroyOnHidden
                confirmLoading={restoring}
                okText={t('browser.local_history.restore')}
                onCancel={() => setRestoreItem(null)}
                onOk={restore}
                open={restoreItem !== null}
                title={t('browser.local_history.confirm_restore')}
            >
                {restoreItem && t('browser.local_history.confirm_restore_message', {
                    modifiedOn: restoreItem.modifiedOn,
                })}
            </Modal>
        </div>
    )
}

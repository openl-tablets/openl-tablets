import { useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { useTranslation } from 'react-i18next'
import { Alert, Button, Empty, notification, Popconfirm, Select, Skeleton, Tag } from 'antd'
import { DeleteOutlined, PlusOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { Role } from '../../constants'
import { getProjectAcl, removeProjectAcl, setProjectAcl } from '../../services/acl'
import { useGuardedReload } from '../../hooks'
import { MOCKUP } from './projectsTheme'
import { AddAccessModal } from './AddAccessModal'

const useStyles = createStyles(({ css, token }) => ({
    panel: css`
        padding: 16px;
    `,
    head: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    hint: css`
        color: ${token.colorTextTertiary};
        font-size: 14px;
    `,
    table: css`
        width: 100%;
        border-collapse: collapse;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadiusLG}px;
        overflow: hidden;
        font-size: 14px;

        th {
            padding: 10px 12px;
            border-bottom: 1px solid ${token.colorBorderSecondary};
            background: ${token.colorFillQuaternary};
            color: ${token.colorTextTertiary};
            font-family: ${MOCKUP.fontMono};
            font-size: 11px;
            font-weight: 500;
            letter-spacing: 0.05em;
            text-align: left;
            text-transform: uppercase;
        }

        td {
            padding: 10px 12px;
            border-bottom: 1px solid ${token.colorFillQuaternary};
        }

        tr:last-child td {
            border-bottom: none;
        }
    `,
    subject: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;

        .anticon {
            color: ${token.colorTextTertiary};
        }
    `,
    kind: css`
        color: ${token.colorTextTertiary};
    `,
    roleCell: css`
        width: 200px;
    `,
    sourceCell: css`
        width: 160px;
    `,
    actionsCell: css`
        width: 56px;
        text-align: right;
    `,
}))

interface AccessPanelProps {
    projectId: string
    projectName: string
    canManage: boolean
}

/** Per-project ACL tab: lists the subjects and their roles, and lets a manager change or revoke access. */
export const AccessPanel = ({ projectId, projectName, canManage }: AccessPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { data: entries, reload } = useGuardedReload(
        projectId,
        id => getProjectAcl(id, { inherited: true })
    )
    const [busy, setBusy] = useState(false)
    const [addOpen, setAddOpen] = useState(false)

    const changeRole = async (sid: string, role: Role, principal: boolean) => {
        setBusy(true)
        try {
            await setProjectAcl(projectId, sid, role, principal)
            reload()
        } catch (e) {
            notification.error({ title: t('browser.access.update_failed'), description: errorMessage(e) })
        } finally {
            setBusy(false)
        }
    }

    const revoke = async (sid: string, principal: boolean) => {
        setBusy(true)
        try {
            await removeProjectAcl(projectId, sid, principal)
            reload()
        } catch (e) {
            notification.error({ title: t('browser.access.remove_failed'), description: errorMessage(e) })
        } finally {
            setBusy(false)
        }
    }

    const roleOptions = Object.values(Role).map(value => ({ value, label: t(`browser.access.role_${value}`) }))

    return (
        <div className={styles.panel} data-testid="access-panel">
            <div className={styles.head}>
                <span className={styles.hint}>{t('browser.access.title')}</span>
                {canManage && (
                    <Button data-testid="access-add" icon={<PlusOutlined />} onClick={() => setAddOpen(true)} type="primary">
                        {t('browser.access.add')}
                    </Button>
                )}
            </div>
            {entries === null && <Skeleton active paragraph={{ rows: 3 }} />}
            {entries === 'error' && <Alert showIcon title={t('browser.access.load_failed')} type="error" />}
            {Array.isArray(entries) && entries.length === 0 && (
                <Empty data-testid="access-empty" description={t('browser.access.none')} image={Empty.PRESENTED_IMAGE_SIMPLE} />
            )}
            {Array.isArray(entries) && entries.length > 0 && (
                <table className={styles.table}>
                    <thead>
                        <tr>
                            <th>{t('browser.access.col_subject')}</th>
                            <th>{t('browser.access.col_type')}</th>
                            <th>{t('browser.access.col_role')}</th>
                            <th>{t('browser.access.col_source')}</th>
                            <th aria-label={t('browser.access.remove')} />
                        </tr>
                    </thead>
                    <tbody>
                        {entries.map(entry => {
                            const inherited = entry.source === 'REPOSITORY'
                            const sourceKey = entry.source ?? 'PROJECT'
                            const source = inherited ? t('browser.access.source_repository') : t('browser.access.source_project')
                            return (
                                <tr key={`${sourceKey}:${entry.sub.principal ? 'u' : 'g'}:${entry.sub.sid}`}>
                                    <td>
                                        <span className={styles.subject}>
                                            {entry.sub.principal ? <UserOutlined /> : <TeamOutlined />}
                                            {entry.sub.sid}
                                        </span>
                                    </td>
                                    <td className={styles.kind}>
                                        {entry.sub.principal ? t('browser.access.type_user') : t('browser.access.type_group')}
                                    </td>
                                    <td className={styles.roleCell}>
                                        <Select
                                            data-testid={`access-role-${sourceKey}-${entry.sub.sid}`}
                                            disabled={inherited || !canManage || busy}
                                            onChange={value => changeRole(entry.sub.sid, value, entry.sub.principal ?? false)}
                                            options={roleOptions}
                                            style={{ width: 180 }}
                                            value={entry.role}
                                            {...(inherited ? { title: t('browser.access.inherited_readonly') } : {})}
                                        />
                                    </td>
                                    <td className={styles.sourceCell}>
                                        <Tag>{source}</Tag>
                                    </td>
                                    <td className={styles.actionsCell}>
                                        {canManage && !inherited && (
                                            <Popconfirm
                                                onConfirm={() => revoke(entry.sub.sid, entry.sub.principal ?? false)}
                                                title={t('browser.access.remove_confirm', { subject: entry.sub.sid })}
                                            >
                                                <Button
                                                    danger
                                                    data-testid={`access-remove-${sourceKey}-${entry.sub.sid}`}
                                                    disabled={busy}
                                                    icon={<DeleteOutlined />}
                                                    size="small"
                                                    type="text"
                                                />
                                            </Popconfirm>
                                        )}
                                    </td>
                                </tr>
                            )
                        })}
                    </tbody>
                </table>
            )}
            <AddAccessModal
                onClose={() => setAddOpen(false)}
                onGranted={reload}
                open={addOpen}
                projectId={projectId}
                projectName={projectName}
            />
        </div>
    )
}

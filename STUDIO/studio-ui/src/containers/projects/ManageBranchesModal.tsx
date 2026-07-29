import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Modal, Skeleton, notification } from 'antd'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import { SearchInput } from '../../components/SearchInput'
import { getProjectBranches, type ProjectBranch } from '../../services/repositories'
import { BranchLabel } from './BranchLabel'

const useStyles = createStyles(({ css, token }) => ({
    hint: css`
        margin: 0 0 12px;
        color: ${token.colorTextSecondary};
        font-size: 13px;
    `,
    list: css`
        display: flex;
        flex-direction: column;
        max-height: 320px;
        overflow-y: auto;
        border: 1px solid ${token.colorBorderSecondary};
        border-radius: ${token.borderRadius}px;
        margin-top: 12px;
    `,
    row: css`
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 6px 12px;
        border-top: 1px solid ${token.colorBorderSecondary};

        &:first-of-type {
            border-top: none;
        }
    `,
    empty: css`
        padding: 12px;
        color: ${token.colorTextTertiary};
        text-align: center;
    `,
}))

interface ManageBranchesModalProps {
    open: boolean
    projectId: string
    onClose: () => void
}

/**
 * Shows the repository branches whose current Git tree contains the project.
 *
 * Membership is derived from repository content. Project copy and deletion use their explicit actions.
 */
export const ManageBranchesModal = ({
    open,
    projectId,
    onClose,
}: ManageBranchesModalProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [branches, setBranches] = useState<ProjectBranch[] | null>(null)
    const [filter, setFilter] = useState('')
    // Every visit starts from a fresh branch list.
    useEffect(() => {
        if (!open) {
            return
        }
        setFilter('')
        setBranches(null)
        getProjectBranches(projectId)
            .then(setBranches)
            .catch(e => {
                notification.error({ title: t('browser.branch.load_failed'), description: errorMessage(e) })
                setBranches([])
            })
    }, [open, projectId, t])

    const needle = filter.trim().toLowerCase()
    const filtered = (branches ?? [])
        .filter(branch => branch.containsProject)
        .filter(branch => branch.name.toLowerCase().includes(needle))

    return (
        <Modal
            destroyOnHidden
            data-testid="manage-branches-modal"
            onCancel={onClose}
            open={open}
            title={t('browser.branch.manage')}
            footer={[
                <Button key="close" data-testid="manage-branches-close" onClick={onClose}>
                    {t('browser.close')}
                </Button>,
            ]}
        >
            <p className={styles.hint}>{t('browser.branch.manage_hint')}</p>
            <SearchInput
                data-testid="manage-branches-filter"
                onChange={event => setFilter(event.target.value)}
                placeholder={t('browser.branch.filter')}
                value={filter}
            />
            {branches === null ? <Skeleton active paragraph={{ rows: 4 }} /> : (
                <div className={styles.list}>
                    {filtered.map(branch => (
                        <div key={branch.name} className={styles.row} data-testid={`manage-branches-item-${branch.name}`}>
                            <BranchLabel isDefault={branch.base} isProtected={branch.protected} name={branch.name} />
                        </div>
                    ))}
                    {filtered.length === 0 && (
                        <div className={styles.empty} data-testid="manage-branches-no-match">
                            {t('browser.branch.no_match')}
                        </div>
                    )}
                </div>
            )}
        </Modal>
    )
}

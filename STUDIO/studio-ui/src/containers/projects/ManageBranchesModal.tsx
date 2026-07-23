import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, Modal, Popconfirm, Skeleton, Tooltip, notification } from 'antd'
import { createStyles } from 'antd-style'
import { errorMessage } from '../../utils/errorMessage'
import { SearchInput } from '../../components/SearchInput'
import { getProjectBranches, type ProjectBranch } from '../../services/repositories'
import { BranchLabel } from './BranchLabel'
import { useSelectedBranchesEditor } from './selectedBranches'

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
    /** The branch the project is open on. */
    currentBranch: string
    /** Branches the project takes part in, as the server knows them. */
    selectedBranches: string[]
    onClose: () => void
    onSaved: () => void
}

/**
 * Picks the branches a project takes part in, out of every branch of its repository.
 *
 * The list carries branch names only — merging, deleting and commit details stay on the Branches tab. The
 * repository main branch and the branch the project is open on always keep the project and cannot be
 * cleared. Clearing any other branch deletes the project from it, so such a save is confirmed first.
 */
export const ManageBranchesModal = ({
    open,
    projectId,
    currentBranch,
    selectedBranches,
    onClose,
    onSaved,
}: ManageBranchesModalProps) => {
    const { styles } = useStyles()
    const { t } = useTranslation('repository')
    const [branches, setBranches] = useState<ProjectBranch[] | null>(null)
    const [filter, setFilter] = useState('')
    const editor = useSelectedBranchesEditor({
        projectId,
        selectedBranches,
        currentBranch,
        onSaved: () => {
            onSaved()
            onClose()
        },
    })
    const { reset } = editor

    // Every visit starts from the saved selection and a fresh branch list.
    useEffect(() => {
        if (!open) {
            return
        }
        setFilter('')
        reset()
        setBranches(null)
        getProjectBranches(projectId)
            .then(setBranches)
            .catch(e => {
                notification.error({ title: t('browser.branch.load_failed'), description: errorMessage(e) })
                setBranches([])
            })
    }, [open, projectId, reset, t])

    const needle = filter.trim().toLowerCase()
    const filtered = (branches ?? []).filter(branch => branch.name.toLowerCase().includes(needle))

    const saveButton = (
        <Button
            key="save"
            data-testid="manage-branches-save"
            disabled={!editor.dirty}
            loading={editor.saving}
            onClick={editor.removing.length === 0 ? () => void editor.save() : undefined}
            type="primary"
        >
            {t('browser.branch.save_selection')}
        </Button>
    )

    return (
        <>
            <Modal
                destroyOnHidden
                data-testid="manage-branches-modal"
                onCancel={onClose}
                open={open}
                title={t('browser.branch.manage')}
                footer={[
                    <Button key="cancel" data-testid="manage-branches-cancel" onClick={onClose}>
                        {t('common:btn.cancel')}
                    </Button>,
                    editor.removing.length > 0 ? (
                        <Popconfirm
                            key="save"
                            okButtonProps={{ 'data-testid': 'manage-branches-save-confirm' } as never}
                            onConfirm={() => void editor.save()}
                            title={t('browser.branch.remove_confirm', { count: editor.removing.length })}
                        >
                            {saveButton}
                        </Popconfirm>
                    ) : saveButton,
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
                            <label key={branch.name} className={styles.row}>
                                <Tooltip title={editor.isLocked(branch) ? t('browser.branch.selection_locked') : ''}>
                                    <Checkbox
                                        checked={editor.selected.has(branch.name)}
                                        data-testid={`manage-branches-select-${branch.name}`}
                                        disabled={editor.isLocked(branch)}
                                        onChange={event => editor.toggle(branch.name, event.target.checked)}
                                    />
                                </Tooltip>
                                <BranchLabel isDefault={branch.base} isProtected={branch.protected} name={branch.name} />
                            </label>
                        ))}
                        {filtered.length === 0 && (
                            <div className={styles.empty} data-testid="manage-branches-no-match">
                                {t('browser.branch.no_match')}
                            </div>
                        )}
                    </div>
                )}
            </Modal>
            {editor.commitInfoModal}
        </>
    )
}

import { useEffect, useState } from 'react'
import { errorMessage } from '../../utils/errorMessage'
import { formatDateTime } from '../../utils/dateFormat'
import { useTranslation } from 'react-i18next'
import { Button, Checkbox, Input, Modal, notification, Popconfirm, Select, Skeleton, Switch, Tag, Tooltip } from 'antd'
import { BranchesOutlined, DeleteOutlined, MergeOutlined, PlusOutlined, SafetyOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import {
    createProjectBranch,
    getProjectBranches,
    isProjectModifiedConflict,
    setSelectedBranches,
    switchProjectBranch,
    type ProjectBranch,
} from '../../services/repositories'
import { MOCKUP } from './projectsTheme'
import { MonoChip } from './MonoChip'
import { GitCommitMessage } from './GitCommitMessage'
import { DiscardChangesModal } from '../DiscardChangesModal'
import { useCommitInfoGuard, useGuardedReload } from '../../hooks'

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
    current: css`
        display: flex;
        align-items: center;
        gap: 8px;
        color: ${token.colorTextTertiary};
        font-size: 14px;
    `,
    toolbar: css`
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        margin-bottom: 12px;
    `,
    filter: css`
        max-width: 280px;
    `,
    configControls: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        color: ${token.colorTextSecondary};
        font-size: 14px;
        white-space: nowrap;
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
            vertical-align: middle;
        }

        tr:last-child td {
            border-bottom: none;
        }
    `,
    checkCell: css`
        width: 40px;
        text-align: center;
    `,
    name: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;

        .anticon-branches {
            color: ${token.colorTextTertiary};
        }

        .anticon-safety {
            color: ${token.colorInfo};
        }
    `,
    commit: css`
        color: ${token.colorTextTertiary};
        font-size: 13px;
        max-width: 360px;
    `,
    commitMeta: css`
        margin-bottom: 2px;
    `,
    actionsCell: css`
        text-align: right;
        white-space: nowrap;
    `,
    baseTag: css`
        margin: 0;
        border-radius: ${token.borderRadiusSM}px;
        font-size: 11px;
    `,
    empty: css`
        padding: 16px;
        color: ${token.colorTextTertiary};
        text-align: center;
    `,
    switchAfter: css`
        display: inline-flex;
        align-items: center;
        gap: 8px;
        margin-top: 12px;
        color: ${token.colorTextSecondary};
    `,
}))

interface BranchesPanelProps {
    projectId: string
    projectName: string
    repositoryId: string
    currentBranch: string
    canWrite: boolean
    /** Branches this project participates in — the source for the Current-branch dropdown. */
    selectedBranches: string[]
    onChanged: () => void
}

/**
 * The Branches tab for a branch-capable (Git) project. The table lists every branch in the repository
 * with a name filter and per-branch merge/delete. Flip the "configure" toggle to reveal a checkbox on
 * each branch and choose which the project participates in; the Current-branch dropdown offers only
 * those. Each branch also shows its last commit when the repository can provide it.
 */
export const BranchesPanel = ({ projectId, projectName, repositoryId, currentBranch, canWrite, selectedBranches, onChanged }: BranchesPanelProps) => {
    const { t } = useTranslation('repository')
    const { styles } = useStyles()
    const { runWithCommitInfo, commitInfoModal } = useCommitInfoGuard()
    const { data: branches, reload } = useGuardedReload(projectId, getProjectBranches)
    const [switching, setSwitching] = useState(false)
    const [creating, setCreating] = useState(false)
    const [newBranch, setNewBranch] = useState('')
    const [switchAfterCreate, setSwitchAfterCreate] = useState(false)
    const [submitting, setSubmitting] = useState(false)
    const [filter, setFilter] = useState('')
    const [configMode, setConfigMode] = useState(false)
    const [selected, setSelected] = useState<Set<string>>(new Set())
    const [savingSelection, setSavingSelection] = useState(false)
    const [discardSwitchBranch, setDiscardSwitchBranch] = useState<string | null>(null)

    useEffect(() => {
        if (!canWrite) {
            setConfigMode(false)
        }
    }, [canWrite])

    const switchTo = async (branch: string, discardChanges = false) => {
        if (branch === currentBranch) {
            return
        }
        setSwitching(true)
        try {
            await switchProjectBranch(projectId, branch, discardChanges ? { discardChanges: true } : {})
            reload()
            onChanged()
        } catch (e) {
            if (!discardChanges && isProjectModifiedConflict(e)) {
                setDiscardSwitchBranch(branch)
                return
            }
            notification.error({ title: t('browser.branch.switch_failed'), description: errorMessage(e) })
        } finally {
            setSwitching(false)
        }
    }

    const createBranch = async () => {
        const name = newBranch.trim()
        if (!name) {
            return
        }
        await runWithCommitInfo(async () => {
            setSubmitting(true)
            try {
                await createProjectBranch(projectId, name)
                setCreating(false)
                setNewBranch('')
                // Creating a branch leaves the project on the current one; switch only when asked.
                if (switchAfterCreate) {
                    await switchTo(name)
                } else {
                    reload()
                    onChanged()
                }
            } catch (e) {
                notification.error({ title: t('browser.branch.create_failed'), description: errorMessage(e) })
            } finally {
                setSubmitting(false)
            }
        })
    }

    // Entering configure mode seeds the checkboxes from the saved selection; leaving it discards edits.
    const toggleConfigMode = (on: boolean) => {
        if (on) {
            setSelected(new Set(selectedBranches))
        }
        setConfigMode(on)
    }

    const toggle = (name: string, checked: boolean) => {
        setSelected(prev => {
            const next = new Set(prev)
            if (checked) {
                next.add(name)
            } else {
                next.delete(name)
            }
            return next
        })
    }

    const saveSelection = async () => {
        await runWithCommitInfo(async () => {
            setSavingSelection(true)
            try {
                await setSelectedBranches(projectId, [...selected])
                notification.success({ title: t('browser.branch.selection_saved') })
                setConfigMode(false)
                reload()
                onChanged()
            } catch (e) {
                notification.error({ title: t('browser.branch.selection_failed'), description: errorMessage(e) })
            } finally {
                setSavingSelection(false)
            }
        })
    }

    const openMerge = (list: ProjectBranch[], targetBranch: string) => {
        window.dispatchEvent(new CustomEvent('openMergeModal', {
            detail: {
                projectId,
                projectName,
                repositoryId,
                repositoryType: 'repo-git',
                currentBranch,
                targetBranch,
                branches: list,
                onSuccess: onChanged,
            },
        }))
    }

    const openDeleteBranch = (branch: string, mainBranch?: string) => {
        window.dispatchEvent(new CustomEvent('openDeleteBranchModal', {
            detail: { repositoryId, projectName, branch, mainBranch, onSuccess: () => { reload(); onChanged() } },
        }))
    }

    if (branches === null) {
        return <div className={styles.panel}><Skeleton active paragraph={{ rows: 4 }} /></div>
    }

    const list = branches === 'error' ? [] : branches
    const needle = filter.trim().toLowerCase()
    const filtered = needle ? list.filter(branch => branch.name.toLowerCase().includes(needle)) : list

    // The dropdown offers only the branches the project participates in, falling back to the current one.
    const currentOptions = selectedBranches.length > 0
        ? selectedBranches.map(name => ({ value: name, label: name }))
        : [{ value: currentBranch, label: currentBranch }]
    const mainBranch = list.find(branch => branch.base)?.name

    const removing = list.filter(branch => selectedBranches.includes(branch.name) && !selected.has(branch.name))
    const dirty = removing.length > 0 || [...selected].some(name => !selectedBranches.includes(name))
    const showActions = canWrite
    const columns = (configMode ? 3 : 2) + (showActions ? 1 : 0)

    return (
        <>
            <div className={styles.panel} data-testid="branches-panel">
                <div className={styles.head}>
                    <div className={styles.current}>
                        <span>{t('browser.branch.current')}:</span>
                        <Select
                            data-testid="branches-current"
                            loading={switching}
                            onChange={branch => void switchTo(branch)}
                            options={currentOptions}
                            popupMatchSelectWidth={false}
                            style={{ minWidth: 220 }}
                            value={currentBranch}
                        />
                    </div>
                    {canWrite && (
                        <Button data-testid="branches-create" icon={<PlusOutlined />} onClick={() => { setNewBranch(''); setSwitchAfterCreate(false); setCreating(true) }} type="primary">
                            {t('browser.branch.create')}
                        </Button>
                    )}
                </div>
                <div className={styles.toolbar}>
                    <Input.Search
                        allowClear
                        className={styles.filter}
                        data-testid="branches-filter"
                        onChange={event => setFilter(event.target.value)}
                        placeholder={t('browser.branch.filter')}
                        value={filter}
                    />
                    {canWrite && (
                        <span className={styles.configControls}>
                            <Switch checked={configMode} data-testid="branches-config-toggle" onChange={toggleConfigMode} size="small" />
                            {t('browser.branch.configure')}
                            {configMode && dirty && (
                                <Popconfirm
                                    disabled={removing.length === 0}
                                    okButtonProps={{ 'data-testid': 'branches-save-confirm' } as never}
                                    onConfirm={saveSelection}
                                    title={t('browser.branch.remove_confirm', { count: removing.length })}
                                >
                                    <Button
                                        data-testid="branches-save-selection"
                                        loading={savingSelection}
                                        onClick={removing.length === 0 ? saveSelection : undefined}
                                        size="small"
                                        type="primary"
                                    >
                                        {t('browser.branch.save_selection')}
                                    </Button>
                                </Popconfirm>
                            )}
                        </span>
                    )}
                </div>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            {configMode && <th aria-label={t('browser.branch.col_select')} className={styles.checkCell} />}
                            <th>{t('browser.branch.col_branch')}</th>
                            <th>{t('browser.branch.col_commit')}</th>
                            {showActions && <th className={styles.actionsCell}>{t('browser.branch.col_actions')}</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {filtered.map(branch => {
                            const deleteBlocked = branch.base || branch.name !== currentBranch || (branch.protected && !branch.bypassEligible)
                            const deleteBlockedTitle = branch.name !== currentBranch && !branch.base
                                ? t('browser.branch.delete_current_only')
                                : t('browser.branch.delete_protected')
                            // The base branch and the branch in use must stay selected.
                            const lockSelection = branch.base || branch.name === currentBranch
                            return (
                                <tr key={branch.name}>
                                    {configMode && (
                                        <td className={styles.checkCell}>
                                            <Checkbox
                                                checked={selected.has(branch.name)}
                                                data-testid={`branch-select-${branch.name}`}
                                                disabled={lockSelection}
                                                onChange={event => toggle(branch.name, event.target.checked)}
                                            />
                                        </td>
                                    )}
                                    <td>
                                        <span className={styles.name}>
                                            <BranchesOutlined />
                                            <MonoChip>{branch.name}</MonoChip>
                                            {branch.base && <Tag className={styles.baseTag}>{t('browser.branch.base')}</Tag>}
                                            {branch.protected && (
                                                <Tooltip title={t('browser.branch.protected_tag')}>
                                                    <SafetyOutlined />
                                                </Tooltip>
                                            )}
                                        </span>
                                    </td>
                                    <td className={styles.commit}>
                                        {branch.lastCommit ? (
                                            <div data-testid={`branch-commit-${branch.name}`}>
                                                <div className={styles.commitMeta}>
                                                    {branch.lastCommit.author} · {formatDateTime(branch.lastCommit.modifiedAt)}
                                                    {branch.lastCommit.revision && (
                                                        <>
                                                            {' · '}
                                                            <MonoChip data-testid={`branch-commit-revision-${branch.name}`}>
                                                                {branch.lastCommit.revision.slice(0, 8)}
                                                            </MonoChip>
                                                        </>
                                                    )}
                                                </div>
                                                <GitCommitMessage
                                                    maxChars={140}
                                                    maxLines={1}
                                                    message={branch.lastCommit.message}
                                                    testId={`branch-commit-message-${branch.name}`}
                                                />
                                            </div>
                                        ) : t('browser.branch.no_stats')}
                                    </td>
                                    {showActions && (
                                        <td className={styles.actionsCell}>
                                            <Button
                                                data-testid={`branch-merge-${branch.name}`}
                                                disabled={branch.name === currentBranch}
                                                icon={<MergeOutlined />}
                                                onClick={() => openMerge(list, branch.name)}
                                                size="small"
                                                style={{ marginRight: 4 }}
                                            >
                                                {t('browser.branch.merge')}
                                            </Button>
                                            <Tooltip title={deleteBlocked ? deleteBlockedTitle : ''}>
                                                <Button
                                                    danger
                                                    data-testid={`branch-delete-${branch.name}`}
                                                    disabled={deleteBlocked}
                                                    icon={<DeleteOutlined />}
                                                    onClick={() => openDeleteBranch(branch.name, mainBranch)}
                                                    size="small"
                                                    type="text"
                                                />
                                            </Tooltip>
                                        </td>
                                    )}
                                </tr>
                            )
                        })}
                        {filtered.length === 0 && (
                            <tr>
                                <td className={styles.empty} colSpan={columns} data-testid="branches-no-match">{t('browser.branch.no_match')}</td>
                            </tr>
                        )}
                    </tbody>
                </table>
                <Modal
                    destroyOnHidden
                    confirmLoading={submitting}
                    okButtonProps={{ 'data-testid': 'branches-create-submit', disabled: !newBranch.trim() }}
                    onCancel={() => setCreating(false)}
                    onOk={createBranch}
                    open={creating}
                    title={t('browser.branch.create_title')}
                >
                    <Input
                        data-testid="branches-new-name"
                        onChange={event => setNewBranch(event.target.value)}
                        onPressEnter={createBranch}
                        placeholder={t('browser.branch.name')}
                        value={newBranch}
                    />
                    <label className={styles.switchAfter}>
                        <Switch checked={switchAfterCreate} data-testid="branches-switch-after" onChange={setSwitchAfterCreate} size="small" />
                        {t('browser.branch.switch_after_create')}
                    </label>
                </Modal>
                <DiscardChangesModal
                    cancelButtonTestId="branch-discard-switch-cancel"
                    confirmButtonTestId="branch-discard-switch-confirm"
                    confirmText={t('browser.switch_branch_discard_confirm_unsafe')}
                    onCancel={() => setDiscardSwitchBranch(null)}
                    open={discardSwitchBranch !== null}
                    warning={t('browser.switch_branch_discard_warning')}
                    onConfirm={() => {
                        const branch = discardSwitchBranch
                        setDiscardSwitchBranch(null)
                        if (branch) {
                            void switchTo(branch, true)
                        }
                    }}
                />
            </div>
            {commitInfoModal}
        </>
    )
}

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { App, Alert, Button, Checkbox, Form, Space, Spin, Tooltip } from 'antd'
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { createStyles } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { apiCall, ApiHttpError, isApiHttpError } from '../../services'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { BranchInfo, CheckMergeResult, MergeMode, MergeResultResponse, MergeSummary } from './types'
import { MergeBranchLabel } from './MergeBranchLabel'
import { MergeScopeTabs } from './MergeScopeTabs'
import { MERGE_API_OPTIONS, mergeBranches } from './mergeBranches'
import { BranchSelect } from '../projects/BranchSelect'
import { getProjectBranches } from '../../services/repositories'

const BYPASS_REQUIRED_CODE = 'openl.error.409.protected.branch.bypass.required'

const useStyles = createStyles(({ css }) => ({
    form: css`
        .ant-form-item-row {
            flex-wrap: nowrap;
        }

        .ant-form-item-label {
            flex: 0 0 ${WIDTH_OF_FORM_LABEL_MODAL};
            max-width: ${WIDTH_OF_FORM_LABEL_MODAL};
        }

        .ant-form-item-control {
            flex: 1 1 0;
            min-width: 0;
        }
    `,
    branchField: css`
        display: flex;
        flex-direction: column;
        gap: 8px;
        width: 100%;
        min-width: 0;
    `,
}))

const isBypassRequired = (err: unknown): err is ApiHttpError => {
    if (!isApiHttpError(err)) {
        return false
    }
    const payload = err.payload as { code?: unknown } | undefined
    return payload?.code === BYPASS_REQUIRED_CODE
}

interface MergeBranchesStepProps {
    projectId: string
    projectName: string
    repositoryType: string
    currentBranch: string
    targetBranch?: string
    branches: BranchInfo[]
    /** Reports the wider list so the marks survive into the conflict step. */
    onBranchesWidened?: (branches: BranchInfo[]) => void
    /** Called once the merge is committed, with what it changed when the server tells it. */
    onMergeSuccess: (summary?: MergeSummary) => void
    onMergeConflicts: (result: MergeResultResponse) => void
    onCheckCommitInfo: (callback: () => void) => void
    /** Tells whether a merge runs: the dialog is then left through its Cancel button, which stops the merge. */
    onMergingChange?: (merging: boolean) => void
}

export const MergeBranchesStep: React.FC<MergeBranchesStepProps> = ({
    projectId,
    projectName: _projectName,
    repositoryType,
    currentBranch,
    targetBranch,
    branches,
    onBranchesWidened,
    onMergeSuccess,
    onMergeConflicts,
    onCheckCommitInfo,
    onMergingChange,
}) => {
    const { modal, notification } = App.useApp()
    const { t } = useTranslation()
    const { styles } = useStyles()
    const [form] = Form.useForm()
    const [selectedBranch, setSelectedBranch] = useState<string | undefined>(undefined)
    const autoCheckedBranch = useRef<string | null>(null)

    // Only the branches that hold the project are offered by default — the common case, and the shortest
    // list to pick from. Merging into a branch that does not hold it yet is legitimate (it is how a project
    // created in its own branch reaches the main branch), so the whole repository is one click away.
    const [showEveryBranch, setShowEveryBranch] = useState(false)
    const widenedOnOpen = useRef(false)
    const [repositoryBranches, setRepositoryBranches] = useState<BranchInfo[] | null>(null)
    const [isLoadingBranches, setIsLoadingBranches] = useState(false)
    const [branchesError, setBranchesError] = useState<string | null>(null)

    const [isChecking, setIsChecking] = useState(false)
    const [isMerging, setIsMerging] = useState(false)
    const [checkResultReceive, setCheckResultReceive] = useState<CheckMergeResult | null>(null)
    const [checkResultSend, setCheckResultSend] = useState<CheckMergeResult | null>(null)
    const [receiveError, setReceiveError] = useState<string | null>(null)
    const [sendError, setSendError] = useState<string | null>(null)
    const [mergeError, setMergeError] = useState<string | null>(null)
    // Cancel is offered once the server has taken the merge: a cancel sent before finds no merge to stop.
    const [isMergeAccepted, setIsMergeAccepted] = useState(false)
    const [isCancelling, setIsCancelling] = useState(false)

    // The dialog stays open while the merge runs, and is free again once the step is gone.
    useEffect(() => {
        onMergingChange?.(isMerging)
        return () => onMergingChange?.(false)
    }, [isMerging, onMergingChange])

    // Read when the merge ends, after the renders since the cancel: a ref, not state.
    const cancelRequested = useRef(false)

    /**
     * Widens or narrows the target list. The repository branches are read once and kept, so ticking the box
     * back and forth costs nothing. A failed read leaves the box off and says why.
     */
    const toggleEveryBranch = useCallback(async (checked: boolean) => {
        if (checked && !repositoryBranches?.length) {
            setIsLoadingBranches(true)
            try {
                const branchList = await getProjectBranches(projectId, 'repository', MERGE_API_OPTIONS)
                const widened = branchList.map(b => ({ ...b, protected: b.protected ?? false }))
                setRepositoryBranches(widened)
                onBranchesWidened?.(widened)
                setBranchesError(null)
            } catch (err) {
                setBranchesError(isApiHttpError(err) && err.message ? err.message : t('merge:branches.load_failed'))
                return
            } finally {
                setIsLoadingBranches(false)
            }
        }
        setShowEveryBranch(checked)
    }, [projectId, repositoryBranches, onBranchesWidened, t])

    const isGitRepository = repositoryType === 'repo-git'

    const offeredBranches = showEveryBranch && repositoryBranches?.length ? repositoryBranches : branches

    // The branches to merge with — every branch but the current one — and the marks each carries.
    const branchNames = useMemo(
        () => offeredBranches.filter(b => b.name !== currentBranch).map(b => b.name),
        [offeredBranches, currentBranch]
    )
    const branchByName = useMemo(() => new Map(offeredBranches.map(b => [b.name, b])), [offeredBranches])
    // `branches` stays the project-scope seed, so a target missing from it is a branch the project has never
    // reached: the merge introduces it there rather than updating something that already exists.
    const targetIsNewToProject = useMemo(
        () => !!selectedBranch && !branches.some(b => b.name === selectedBranch),
        [branches, selectedBranch]
    )


    // A selection that the narrowed list no longer offers must go, together with its check results: the
    // buttons they enable would otherwise still merge into a branch the dialog stopped showing and marking.
    useEffect(() => {
        if (selectedBranch && !branchNames.includes(selectedBranch)) {
            setSelectedBranch(undefined)
            setCheckResultReceive(null)
            setCheckResultSend(null)
            setReceiveError(null)
            setSendError(null)
            autoCheckedBranch.current = null
        }
    }, [branchNames, selectedBranch])

    // A project that lives only on its own branch has no target within its own scope, which is the very case
    // this dialog exists for. Widen once so it opens usable instead of empty — see EPBDS-16411. Only once:
    // a failed read leaves the list just as empty, and asking again would repeat that request without end.
    // The user still retries through the option itself.
    useEffect(() => {
        if (widenedOnOpen.current || showEveryBranch || isLoadingBranches) {
            return
        }
        if (branches.every(b => b.name === currentBranch)) {
            widenedOnOpen.current = true
            void toggleEveryBranch(true)
        }
    }, [branches, currentBranch, showEveryBranch, isLoadingBranches, toggleEveryBranch])

    const branchMarks = useCallback((name: string) => {
        const info = branchByName.get(name)
        return { isDefault: info?.base, isProtected: info?.protected }
    }, [branchByName])

    /**
     * Why a merge that has something to merge still cannot be performed, or `null` when it can — a bypass
     * counts as performable, since the user confirms it themselves.
     */
    const blockedMessage = useCallback((result: CheckMergeResult | null, target: string | undefined): string | null => {
        if (!result || result.canMerge || result.status !== 'mergeable' || result.blockedBy === 'bypass-required') {
            return null
        }
        return result.blockedBy === 'locked'
            ? t('merge:blocked.locked', { branch: target })
            : t('merge:blocked.protected', { branch: target })
    }, [t])

    const canSelectBranch = useCallback((branch: string): boolean =>
        branch !== currentBranch && offeredBranches.some(item => item.name === branch), [offeredBranches, currentBranch])

    /**
     * Asks where the two branches stand. The answer holds both parts: whether they differ, and whether this
     * user may merge them — so a protected target reports the difference instead of an error.
     */
    const runCheck = useCallback(async (mode: MergeMode, branch: string): Promise<{ result: CheckMergeResult | null, error: string | null }> => {
        try {
            const result = await apiCall(
                `/projects/${projectId}/merge/check`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ mode, otherBranch: branch }),
                },
                MERGE_API_OPTIONS
            ) as CheckMergeResult
            return { result, error: null }
        } catch (err: unknown) {
            return { result: null, error: isApiHttpError(err) ? err.message : t('merge:errors.check_failed') }
        }
    }, [projectId, t])

    const checkMergeStatus = useCallback(async (branch: string) => {
        setIsChecking(true)
        setCheckResultReceive(null)
        setCheckResultSend(null)
        setReceiveError(null)
        setSendError(null)
        setMergeError(null)

        // The two directions are independent, so check receive (branch → current) and send
        // (current → branch) at once rather than waiting for the first before starting the second.
        const [receiveOutcome, sendOutcome] = await Promise.all([
            runCheck('receive', branch),
            runCheck('send', branch),
        ])
        setCheckResultReceive(receiveOutcome.result)
        setReceiveError(receiveOutcome.error)
        setCheckResultSend(sendOutcome.result)
        setSendError(sendOutcome.error)

        setIsChecking(false)
    }, [runCheck])

    useEffect(() => {
        if (!targetBranch || !canSelectBranch(targetBranch) || autoCheckedBranch.current === targetBranch) {
            return
        }
        autoCheckedBranch.current = targetBranch
        setSelectedBranch(targetBranch)
        void checkMergeStatus(targetBranch)
    }, [canSelectBranch, checkMergeStatus, targetBranch])

    /**
     * Cancels the merge of the project: stops it while it runs, or drops the conflicts it has left. A merge that has
     * left nothing to cancel answers 404, which is no failure: it reports its end on its own. Any other failure is
     * thrown.
     */
    const requestCancel = async () => {
        try {
            await apiCall(`/projects/${projectId}/merge/conflicts`, { method: 'DELETE' }, MERGE_API_OPTIONS)
        } catch (err: unknown) {
            if (!isApiHttpError(err) || err.status !== 404) {
                throw err
            }
        }
    }

    /**
     * Asks the running merge to stop. It stops within seconds, before it commits anything, and reports its end as
     * usual, which the merge reads as cancelled. A cancel that fails leaves the merge running, and Cancel can be
     * clicked again.
     */
    const cancelMerge = async () => {
        cancelRequested.current = true
        setIsCancelling(true)
        try {
            await requestCancel()
        } catch (err: unknown) {
            cancelRequested.current = false
            setIsCancelling(false)
            const message = err instanceof Error ? err.message : undefined
            setMergeError(message || t('merge:errors.cancel_failed'))
        }
    }

    const notifyCancelled = () => notification.info({
        title: t('merge:notifications.merge_cancelled'),
        description: t('merge:notifications.merge_cancelled_description'),
    })

    const handleMerge = (mode: MergeMode) => {
        if (!selectedBranch) return

        const targetBranch = mode === 'send' ? selectedBranch : currentBranch

        const doMerge = async (force = false) => {
            setIsMerging(true)
            setMergeError(null)
            cancelRequested.current = false

            try {
                const result = await mergeBranches(projectId, mode, selectedBranch, force,
                    () => setIsMergeAccepted(true))

                if (result.status === 'success') {
                    // The cancel came after the merge had started to commit: it is merged.
                    onMergeSuccess(result.summary)
                } else if (result.status === 'conflicts' && !cancelRequested.current) {
                    onMergeConflicts(result)
                } else {
                    if (result.status === 'conflicts') {
                        // The merge ended before the cancel reached it: the conflicts it left are cancelled too.
                        try {
                            await requestCancel()
                        } catch {
                            // They stay on the server, so they are shown for resolution, where they can be
                            // cancelled again.
                            onMergeConflicts(result)
                            return
                        }
                    }
                    notifyCancelled()
                }
            } catch (err: unknown) {
                if (cancelRequested.current && isApiHttpError(err) && err.status === 404) {
                    // The merge ended with conflicts just before the cancel reached it, and the cancel dropped them:
                    // no result is left to read.
                    notifyCancelled()
                    return
                }
                if (!force && isBypassRequired(err)) {
                    setIsMerging(false)
                    modal.confirm({
                        title: t('merge:bypass.title'),
                        content: t('merge:bypass.description', { branch: targetBranch }),
                        okText: t('merge:bypass.confirm'),
                        okButtonProps: { danger: true },
                        cancelText: t('merge:buttons.cancel'),
                        onOk: () => doMerge(true),
                    })
                    return
                }
                // An API error is an Error too, carrying the message the server answered with.
                const message = err instanceof Error ? err.message : undefined
                setMergeError(message || t('merge:errors.merge_failed'))
            } finally {
                setIsMerging(false)
                setIsMergeAccepted(false)
                setIsCancelling(false)
            }
        }

        // For Git repositories, check commit info first
        if (isGitRepository) {
            onCheckCommitInfo(() => doMerge(false))
        } else {
            void doMerge(false)
        }
    }

    const bypassRequiredReceive = checkResultReceive?.blockedBy === 'bypass-required'
    const bypassRequiredSend = checkResultSend?.blockedBy === 'bypass-required'
    // A merge the server would refuse is not offered; a bypass is, as an explicit confirmation.
    const canReceive = checkResultReceive?.status === 'mergeable' && (checkResultReceive.canMerge || bypassRequiredReceive)
    const canSend = checkResultSend?.status === 'mergeable' && (checkResultSend.canMerge || bypassRequiredSend)
    const isReceiveUpToDate = checkResultReceive?.status === 'up-to-date'
    const isSendUpToDate = checkResultSend?.status === 'up-to-date'
    const receiveBlocked = blockedMessage(checkResultReceive, currentBranch)
    const sendBlocked = blockedMessage(checkResultSend, selectedBranch)
    const receiveActionTooltip = bypassRequiredReceive
        ? t('merge:bypass.action_tooltip')
        : t('merge:actions.receive_description')
    const sendActionTooltip = bypassRequiredSend
        ? t('merge:bypass.action_tooltip')
        : t('merge:actions.send_description')

    return (
        <Space orientation="vertical" size="middle" style={{ width: '100%', paddingTop: 8 }}>
            <Form
                labelWrap
                className={styles.form}
                form={form}
                labelAlign="right"
                labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                name="merge_branches_form"
                wrapperCol={{ flex: 1, style: { minWidth: 0 } }}
            >
                <Form.Item label={t('merge:branches.current')}>
                    <MergeBranchLabel
                        withIcon
                        branches={branches}
                        name={currentBranch}
                        testId="merge-current-branch"
                    />
                </Form.Item>
                <Form.Item label={t('merge:branches.target')} style={{ marginBottom: 0 }}>
                    <div className={styles.branchField} data-testid="merge-target-branch-field">
                        <BranchSelect
                            branchNames={branchNames}
                            data-testid="merge-target-branch"
                            loading={isLoadingBranches}
                            marksOf={branchMarks}
                            placeholder={t('merge:branches.select_placeholder')}
                            value={selectedBranch}
                            onChange={branch => {
                                setSelectedBranch(branch)
                                void checkMergeStatus(branch)
                            }}
                        />
                        <Tooltip title={t('merge:branches.show_all_hint')}>
                            <Checkbox
                                checked={showEveryBranch}
                                data-testid="merge-show-every-branch"
                                disabled={isLoadingBranches}
                                onChange={event => void toggleEveryBranch(event.target.checked)}
                            >
                                {t('merge:branches.show_all')}
                            </Checkbox>
                        </Tooltip>
                    </div>
                </Form.Item>
            </Form>
            {branchesError && (
                <Alert showIcon data-testid="merge-branches-error" title={branchesError} type="error" />
            )}
            {targetIsNewToProject && (
                <Alert
                    showIcon
                    data-testid="merge-target-without-project"
                    title={t('merge:branches.target_without_project', { branch: selectedBranch })}
                    type="info"
                />
            )}
            {mergeError && (
                <Alert
                    showIcon
                    title={mergeError}
                    type="error"
                />
            )}
            {isChecking && (
                <div style={{ textAlign: 'center', padding: 16 }}>
                    <Spin description={t('merge:status.checking')} />
                </div>
            )}
            {!isChecking && selectedBranch && (
                <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
                    {receiveError && (
                        <Alert
                            showIcon
                            title={receiveError}
                            type="error"
                        />
                    )}
                    {sendError && sendError !== receiveError && (
                        <Alert
                            showIcon
                            title={sendError}
                            type="error"
                        />
                    )}
                    {receiveBlocked && (
                        <Alert showIcon data-testid="merge-blocked-receive" title={receiveBlocked} type="info" />
                    )}
                    {sendBlocked && sendBlocked !== receiveBlocked && (
                        <Alert showIcon data-testid="merge-blocked-send" title={sendBlocked} type="info" />
                    )}
                    {(bypassRequiredReceive || bypassRequiredSend) && (
                        <Alert
                            showIcon
                            title={t('merge:bypass.title')}
                            type="warning"
                            description={bypassRequiredReceive && bypassRequiredSend
                                ? t('merge:bypass.description_both', {
                                    send: selectedBranch,
                                    receive: currentBranch,
                                })
                                : t('merge:bypass.description', {
                                    branch: bypassRequiredSend ? selectedBranch : currentBranch,
                                })}
                        />
                    )}
                    {(checkResultReceive || checkResultSend) && (
                        <MergeScopeTabs receive={checkResultReceive} send={checkResultSend} />
                    )}
                    <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                        {isMergeAccepted && (
                            <Button
                                data-testid="merge-cancel"
                                disabled={isCancelling}
                                onClick={() => void cancelMerge()}
                            >
                                {t('merge:buttons.cancel')}
                            </Button>
                        )}
                        <Tooltip
                            title={receiveBlocked ?? (isReceiveUpToDate ? t('merge:status.up_to_date_receive') : receiveActionTooltip)}
                        >
                            <Button
                                danger={bypassRequiredReceive}
                                disabled={!canReceive || isMerging}
                                icon={<DownloadOutlined />}
                                loading={isMerging}
                                onClick={() => handleMerge('receive')}
                                type="primary"
                            >
                                {t('merge:actions.receive')}
                            </Button>
                        </Tooltip>
                        <Tooltip
                            title={sendBlocked ?? (isSendUpToDate ? t('merge:status.up_to_date_send') : sendActionTooltip)}
                        >
                            <Button
                                danger={bypassRequiredSend}
                                disabled={!canSend || isMerging}
                                icon={<UploadOutlined />}
                                loading={isMerging}
                                onClick={() => handleMerge('send')}
                                type="primary"
                            >
                                {t('merge:actions.send')}
                            </Button>
                        </Tooltip>
                    </Space>
                </Space>
            )}
        </Space>
    )
}

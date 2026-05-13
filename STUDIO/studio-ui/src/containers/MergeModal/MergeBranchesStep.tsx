import React, { useCallback, useMemo, useState } from 'react'
import { Alert, Button, Form, Modal, Space, Spin, Tooltip, Typography } from 'antd'
import { BranchesOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { Select } from '../../components/form'
import type { ApiCallOptions } from '../../services'
import { apiCall, ApiHttpError, isApiHttpError } from '../../services'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { BranchInfo, CheckMergeResult, MergeMode, MergeResultResponse } from './types'

const MERGE_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

const BYPASS_REQUIRED_CODE = 'openl.error.409.protected.branch.bypass.required'

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
    branches: BranchInfo[]
    onMergeSuccess: () => void
    onMergeConflicts: (result: MergeResultResponse) => void
    onCheckCommitInfo: (callback: () => void) => void
}

export const MergeBranchesStep: React.FC<MergeBranchesStepProps> = ({
    projectId,
    projectName: _projectName,
    repositoryType,
    currentBranch,
    branches,
    onMergeSuccess,
    onMergeConflicts,
    onCheckCommitInfo,
}) => {
    const { t } = useTranslation()
    const [form] = Form.useForm()
    const [selectedBranch, setSelectedBranch] = useState<string | undefined>(undefined)

    const [isChecking, setIsChecking] = useState(false)
    const [isMerging, setIsMerging] = useState(false)
    const [checkResultReceive, setCheckResultReceive] = useState<CheckMergeResult | null>(null)
    const [checkResultSend, setCheckResultSend] = useState<CheckMergeResult | null>(null)
    const [receiveError, setReceiveError] = useState<string | null>(null)
    const [sendError, setSendError] = useState<string | null>(null)
    const [bypassRequiredReceive, setBypassRequiredReceive] = useState(false)
    const [bypassRequiredSend, setBypassRequiredSend] = useState(false)
    const [mergeError, setMergeError] = useState<string | null>(null)

    const isGitRepository = repositoryType === 'repo-git'

    // Filter out current branch from options
    const branchOptions = useMemo(() => {
        return branches
            .filter(b => b.name !== currentBranch)
            .map(b => ({
                value: b.name,
                label: b.protected ? `${b.name} (protected)` : b.name,
            }))
    }, [branches, currentBranch])

    const runCheck = useCallback(async (mode: MergeMode, branch: string): Promise<{ result: CheckMergeResult | null, bypassRequired: boolean, error: string | null }> => {
        const callCheck = async (force: boolean) => {
            const url = force
                ? `/projects/${projectId}/merge/check?force=true`
                : `/projects/${projectId}/merge/check`
            return apiCall(
                url,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ mode, otherBranch: branch }),
                },
                MERGE_API_OPTIONS
            ) as Promise<CheckMergeResult>
        }
        try {
            const result = await callCheck(false)
            return { result, bypassRequired: false, error: null }
        } catch (err: unknown) {
            if (isBypassRequired(err)) {
                try {
                    const result = await callCheck(true)
                    return { result, bypassRequired: true, error: null }
                } catch (innerErr: unknown) {
                    return {
                        result: null,
                        bypassRequired: false,
                        error: isApiHttpError(innerErr) ? innerErr.message : t('merge:errors.check_failed'),
                    }
                }
            }
            return {
                result: null,
                bypassRequired: false,
                error: isApiHttpError(err) ? err.message : t('merge:errors.check_failed'),
            }
        }
    }, [projectId, t])

    const checkMergeStatus = useCallback(async (branch: string) => {
        setIsChecking(true)
        setCheckResultReceive(null)
        setCheckResultSend(null)
        setReceiveError(null)
        setSendError(null)
        setBypassRequiredReceive(false)
        setBypassRequiredSend(false)
        setMergeError(null)

        // Check receive (from selectedBranch to currentBranch)
        const receiveOutcome = await runCheck('receive', branch)
        setCheckResultReceive(receiveOutcome.result)
        setBypassRequiredReceive(receiveOutcome.bypassRequired)
        setReceiveError(receiveOutcome.error)

        // Check send (from currentBranch to selectedBranch)
        const sendOutcome = await runCheck('send', branch)
        setCheckResultSend(sendOutcome.result)
        setBypassRequiredSend(sendOutcome.bypassRequired)
        setSendError(sendOutcome.error)

        setIsChecking(false)
    }, [runCheck])

    const handleMerge = async (mode: MergeMode) => {
        if (!selectedBranch) return

        const targetBranch = mode === 'send' ? selectedBranch : currentBranch

        const doMerge = async (force = false) => {
            setIsMerging(true)
            setMergeError(null)

            try {
                const url = force
                    ? `/projects/${projectId}/merge?force=true`
                    : `/projects/${projectId}/merge`
                const result: MergeResultResponse = await apiCall(
                    url,
                    {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            mode,
                            otherBranch: selectedBranch,
                        }),
                    },
                    MERGE_API_OPTIONS
                )

                if (result.status === 'success') {
                    onMergeSuccess()
                } else if (result.status === 'conflicts') {
                    onMergeConflicts(result)
                }
            } catch (err: unknown) {
                if (!force && isBypassRequired(err)) {
                    setIsMerging(false)
                    Modal.confirm({
                        title: t('merge:bypass.title'),
                        content: t('merge:bypass.description', { branch: targetBranch }),
                        okText: t('merge:bypass.confirm'),
                        okButtonProps: { danger: true },
                        cancelText: t('merge:buttons.cancel'),
                        onOk: () => doMerge(true),
                    })
                    return
                }
                const message = isApiHttpError(err) ? err.message : (err instanceof Error ? err.message : undefined)
                setMergeError(message || t('merge:errors.merge_failed'))
            } finally {
                setIsMerging(false)
            }
        }

        // For Git repositories, check commit info first
        if (isGitRepository) {
            onCheckCommitInfo(() => doMerge(false))
        } else {
            doMerge(false)
        }
    }

    const canReceive = checkResultReceive?.status === 'mergeable'
    const canSend = checkResultSend?.status === 'mergeable'
    const isReceiveUpToDate = checkResultReceive?.status === 'up-to-date'
    const isSendUpToDate = checkResultSend?.status === 'up-to-date'
    const receiveActionTooltip = bypassRequiredReceive
        ? t('merge:bypass.action_tooltip')
        : t('merge:actions.receive_description')
    const sendActionTooltip = bypassRequiredSend
        ? t('merge:bypass.action_tooltip')
        : t('merge:actions.send_description')

    return (
        <Space orientation="vertical" size="large" style={{ width: '100%', paddingTop: 16 }}>
            <Form
                labelWrap
                form={form}
                labelAlign="right"
                labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                name="merge_branches_form"
                wrapperCol={{ flex: 1 }}
            >
                <Form.Item label={t('merge:branches.current')}>
                    <Typography.Text strong ellipsis={{ tooltip: currentBranch }}>
                        <BranchesOutlined style={{ marginRight: 8 }} />
                        {currentBranch}
                    </Typography.Text>
                </Form.Item>
                <Select
                    label={t('merge:branches.target')}
                    name="targetBranch"
                    options={branchOptions}
                    placeholder={t('merge:branches.select_placeholder')}
                    suffixIcon={<BranchesOutlined />}
                    onChange={(value) => {
                        const branch = value as string
                        setSelectedBranch(branch)
                        checkMergeStatus(branch)
                    }}
                />
            </Form>
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
                    <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Tooltip
                            title={isReceiveUpToDate ? t('merge:status.up_to_date_receive') : receiveActionTooltip}
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
                            title={isSendUpToDate ? t('merge:status.up_to_date_send') : sendActionTooltip}
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

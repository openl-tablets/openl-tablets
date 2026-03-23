import React, { useCallback, useMemo, useState } from 'react'
import { Alert, Button, Form, Space, Spin, Tooltip, Typography } from 'antd'
import { BranchesOutlined, DownloadOutlined, UploadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { Select } from '../../components/form'
import type { ApiCallOptions } from '../../services'
import { apiCall, isApiHttpError } from '../../services'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { BranchInfo, CheckMergeResult, MergeMode, MergeResultResponse } from './types'

const MERGE_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

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

    const checkMergeStatus = useCallback(async (branch: string) => {
        setIsChecking(true)
        setCheckResultReceive(null)
        setCheckResultSend(null)
        setReceiveError(null)
        setSendError(null)
        setMergeError(null)

        // Check receive (from selectedBranch to currentBranch)
        try {
            const receiveResult: CheckMergeResult = await apiCall(
                `/projects/${projectId}/merge/check`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        mode: 'receive',
                        otherBranch: branch,
                    }),
                },
                MERGE_API_OPTIONS
            )
            setCheckResultReceive(receiveResult)
        } catch (err: unknown) {
            setCheckResultReceive(null)
            setReceiveError(isApiHttpError(err) ? err.message : t('merge:errors.check_failed'))
        }

        // Check send (from currentBranch to selectedBranch)
        try {
            const sendResult: CheckMergeResult = await apiCall(
                `/projects/${projectId}/merge/check`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        mode: 'send',
                        otherBranch: branch,
                    }),
                },
                MERGE_API_OPTIONS
            )
            setCheckResultSend(sendResult)
        } catch (err: unknown) {
            setCheckResultSend(null)
            setSendError(isApiHttpError(err) ? err.message : t('merge:errors.check_failed'))
        }

        setIsChecking(false)
    }, [projectId, t])

    const handleMerge = async (mode: MergeMode) => {
        if (!selectedBranch) return

        const doMerge = async () => {
            setIsMerging(true)
            setMergeError(null)

            try {
                const result: MergeResultResponse = await apiCall(
                    `/projects/${projectId}/merge`,
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
                const message = isApiHttpError(err) ? err.message : (err instanceof Error ? err.message : undefined)
                setMergeError(message || t('merge:errors.merge_failed'))
            } finally {
                setIsMerging(false)
            }
        }

        // For Git repositories, check commit info first
        if (isGitRepository) {
            onCheckCommitInfo(doMerge)
        } else {
            doMerge()
        }
    }

    const canReceive = checkResultReceive?.status === 'mergeable'
    const canSend = checkResultSend?.status === 'mergeable'
    const isReceiveUpToDate = checkResultReceive?.status === 'up-to-date'
    const isSendUpToDate = checkResultSend?.status === 'up-to-date'

    return (
        <Space direction="vertical" size="large" style={{ width: '100%', paddingTop: 16 }}>
            <Form
                labelWrap
                form={form}
                labelAlign="right"
                labelCol={{ flex: WIDTH_OF_FORM_LABEL_MODAL }}
                name="merge_branches_form"
                wrapperCol={{ flex: 1 }}
            >
                <Form.Item label={t('merge:branches.current')}>
                    <Typography.Text strong>
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
                    <Spin tip={t('merge:status.checking')} />
                </div>
            )}
            {!isChecking && selectedBranch && (
                <Space direction="vertical" size="middle" style={{ width: '100%' }}>
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
                    <Space style={{ display: 'flex', justifyContent: 'flex-end' }}>
                        <Tooltip
                            title={isReceiveUpToDate
                                ? t('merge:status.up_to_date_receive')
                                : t('merge:actions.receive_description')}
                        >
                            <Button
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
                            title={isSendUpToDate
                                ? t('merge:status.up_to_date_send')
                                : t('merge:actions.send_description')}
                        >
                            <Button
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

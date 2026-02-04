import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { Alert, Button, Form, Space, Spin, Tooltip, Typography } from 'antd'
import { BranchesOutlined, DownloadOutlined, UploadOutlined, WarningOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { Select } from '../../components/form'
import { apiCall } from '../../services'
import { WIDTH_OF_FORM_LABEL_MODAL } from '../../constants'
import { BranchInfo, CheckMergeResult, MergeMode, MergeResultResponse } from './types'

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
    const selectedBranch = Form.useWatch('targetBranch', form)

    const [isChecking, setIsChecking] = useState(false)
    const [isMerging, setIsMerging] = useState(false)
    const [checkResultReceive, setCheckResultReceive] = useState<CheckMergeResult | null>(null)
    const [checkResultSend, setCheckResultSend] = useState<CheckMergeResult | null>(null)
    const [error, setError] = useState<string | null>(null)

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

    // Find if selected branch is protected
    const selectedBranchInfo = useMemo(() => {
        return branches.find(b => b.name === selectedBranch)
    }, [branches, selectedBranch])

    const checkMergeStatus = useCallback(async () => {
        if (!selectedBranch) return

        setIsChecking(true)
        setError(null)

        try {
            // Check receive (from selectedBranch to currentBranch)
            const receiveResult: CheckMergeResult = await apiCall(
                `/projects/${projectId}/merge/check`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        mode: 'receive',
                        otherBranch: selectedBranch,
                    }),
                },
                true
            )
            setCheckResultReceive(receiveResult)

            // Check send (from currentBranch to selectedBranch)
            const sendResult: CheckMergeResult = await apiCall(
                `/projects/${projectId}/merge/check`,
                {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        mode: 'send',
                        otherBranch: selectedBranch,
                    }),
                },
                true
            )
            setCheckResultSend(sendResult)
        } catch (_err) {
            setError(t('merge:errors.check_failed'))
        } finally {
            setIsChecking(false)
        }
    }, [projectId, selectedBranch, t])

    // Check merge status when branch is selected
    useEffect(() => {
        if (selectedBranch) {
            checkMergeStatus()
        } else {
            setCheckResultReceive(null)
            setCheckResultSend(null)
        }
    }, [selectedBranch, checkMergeStatus])

    const handleMerge = async (mode: MergeMode) => {
        if (!selectedBranch) return

        const doMerge = async () => {
            setIsMerging(true)
            setError(null)

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
                    true
                )

                if (result.status === 'success') {
                    onMergeSuccess()
                } else if (result.status === 'conflicts') {
                    onMergeConflicts(result)
                }
            } catch (err: any) {
                setError(err?.message || t('merge:errors.merge_failed'))
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
                />
            </Form>
            {error && (
                <Alert
                    showIcon
                    title={error}
                    type="error"
                />
            )}
            {selectedBranchInfo?.protected && (
                <Alert
                    showIcon
                    icon={<WarningOutlined />}
                    title={t('merge:status.protected_warning')}
                    type="warning"
                />
            )}
            {isChecking && (
                <div style={{ textAlign: 'center', padding: 16 }}>
                    <Spin tip={t('merge:status.checking')} />
                </div>
            )}
            {!isChecking && selectedBranch && (
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
                            disabled={!canSend || isMerging || selectedBranchInfo?.protected}
                            icon={<UploadOutlined />}
                            loading={isMerging}
                            onClick={() => handleMerge('send')}
                            type="primary"
                        >
                            {t('merge:actions.send')}
                        </Button>
                    </Tooltip>
                </Space>
            )}
        </Space>
    )
}

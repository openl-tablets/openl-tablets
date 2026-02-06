import React, { useCallback, useMemo } from 'react'
import { Button, Tooltip } from 'antd'
import { CopyOutlined, LoadingOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import type { TraceParameterValue } from 'types/trace'
import { useCopyToClipboard } from '../hooks/useCopyToClipboard'

interface CopyJsonButtonProps {
    /** Single parameter (for result) or array of parameters */
    data?: TraceParameterValue | TraceParameterValue[]
    /** i18n key for tooltip: 'copy.parameters' or 'copy.result' */
    tooltipKey: string
}

/**
 * Check if a parameter has unfetched lazy value.
 */
const hasUnfetchedLazyValue = (param: TraceParameterValue): boolean => {
    return param.lazy === true && param.value === undefined
}

/**
 * Button to copy trace parameters or result as JSON.
 * Only available when all parameter values are already fetched.
 */
export const CopyJsonButton: React.FC<CopyJsonButtonProps> = ({ data, tooltipKey }) => {
    const { t } = useTranslation('trace')
    const { copied, copying, copyToClipboard } = useCopyToClipboard()

    // Check if all parameters are fetched (no lazy values pending)
    const allFetched = useMemo(() => {
        if (!data) return false
        if (Array.isArray(data)) {
            return data.every(param => !hasUnfetchedLazyValue(param))
        }
        return !hasUnfetchedLazyValue(data)
    }, [data])

    const handleCopy = useCallback(async () => {
        if (!data || !allFetched) return

        try {
            let jsonValue: unknown

            if (Array.isArray(data)) {
                // Multiple parameters - build object { name: value, ... }
                const result: Record<string, unknown> = {}
                for (const param of data) {
                    result[param.name] = param.value
                }
                jsonValue = result
            } else {
                // Single parameter (result) - just get the value
                jsonValue = data.value
            }

            const jsonString = JSON.stringify(jsonValue, null, 2)
            await copyToClipboard(jsonString)
        } catch (error) {
            console.error('Failed to copy:', error)
        }
    }, [data, allFetched, copyToClipboard])

    // Don't render if no data
    if (!data || (Array.isArray(data) && data.length === 0)) {
        return null
    }

    const tooltipTitle = copied ? t('copy.copied') : t(tooltipKey)

    return (
        <Tooltip title={tooltipTitle}>
            <Button
                type="text"
                size="small"
                icon={copying ? <LoadingOutlined spin /> : <CopyOutlined />}
                onClick={handleCopy}
                disabled={copying || !allFetched}
                className="trace-copy-button"
            />
        </Tooltip>
    )
}

export default CopyJsonButton

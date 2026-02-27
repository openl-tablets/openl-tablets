import React, { useEffect, useMemo, useState } from 'react'
import { Card, Spin, Empty } from 'antd'
import DOMPurify from 'dompurify'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import { NotFoundError, isApiHttpError } from 'services'

interface TraceTableViewProps {
    nodeId: number
}

/**
 * Component for displaying traced table HTML with highlighted cells.
 * Fetches HTML fragment from backend and renders it.
 */
const TraceTableView: React.FC<TraceTableViewProps> = ({ nodeId }) => {
    const { t } = useTranslation('trace')
    const { projectId } = useTraceStore()
    const [html, setHtml] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        // Use explicit null check - 0 is a valid node ID
        if (projectId == null || nodeId == null) return

        const fetchTable = async () => {
            setLoading(true)
            setError(null)
            try {
                const tableHtml = await traceService.getTraceTableHtml(
                    projectId,
                    nodeId
                )
                setHtml(tableHtml)
            } catch (err: unknown) {
                // 404 is expected if node has no table view
                if (err instanceof NotFoundError || (isApiHttpError(err) && err.status === 404)) {
                    setHtml(null)
                } else {
                    const errorMessage = err instanceof Error ? err.message : t('errors.tableFailed')
                    setError(errorMessage)
                }
            } finally {
                setLoading(false)
            }
        }

        fetchTable()
    }, [projectId, nodeId, t])

    const sanitizedHtml = useMemo(
        () => (html ? DOMPurify.sanitize(html, { USE_PROFILES: { html: true } }) : ''),
        [html]
    )

    if (loading) {
        return (
            <Card
                className="trace-table-card"
                size="small"
                title={t('details.table')}
            >
                <div className="trace-table-loading">
                    <Spin tip={t('loadingTable')} />
                </div>
            </Card>
        )
    }

    if (error) {
        return (
            <Card
                className="trace-table-card"
                size="small"
                title={t('details.table')}
            >
                <Empty description={error} />
            </Card>
        )
    }

    if (!html) {
        return null // Don't render card if no table available
    }

    return (
        <Card
            className="trace-table-card"
            size="small"
            title={t('details.table')}
        >
            <div
                className="trace-table-content"
                dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
            />
        </Card>
    )
}

export default TraceTableView

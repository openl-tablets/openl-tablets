import React, { useEffect, useState } from 'react'
import { Card, Spin, Empty } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'

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
            } catch (err: any) {
                // 404 is expected if node has no table view
                if (err?.message?.includes('404')) {
                    setHtml(null)
                } else {
                    setError(err?.message || t('errors.tableFailed'))
                }
            } finally {
                setLoading(false)
            }
        }

        fetchTable()
    }, [projectId, nodeId, t])

    if (loading) {
        return (
            <Card
                title={t('details.table')}
                size="small"
                className="trace-table-card"
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
                title={t('details.table')}
                size="small"
                className="trace-table-card"
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
            title={t('details.table')}
            size="small"
            className="trace-table-card"
        >
            <div
                className="trace-table-content"
                dangerouslySetInnerHTML={{ __html: html }}
            />
        </Card>
    )
}

export default TraceTableView

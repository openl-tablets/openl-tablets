import React, { useEffect, useMemo, useState } from 'react'
import { Card, Spin, Empty } from 'antd'
import DOMPurify from 'dompurify'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import { NotFoundError, isApiHttpError } from 'services'
import { useStyles } from './TraceTableView.styles'

interface TraceTableViewProps {
    frameIndex: number
}

/**
 * Displays a stack frame's table HTML.
 * Fetches the HTML fragment from the backend and renders it.
 */
const TraceTableView: React.FC<TraceTableViewProps> = ({ frameIndex }) => {
    const { t } = useTranslation('trace')
    const { styles } = useStyles()
    const projectId = useTraceStore(s => s.projectId)
    // Refetch when execution advances (the highlighted current line changes within the same frame).
    const stackVersion = useTraceStore(s => s.stackVersion)
    const [html, setHtml] = useState<string | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        // Use explicit null check - 0 is a valid frame index
        if (projectId == null || frameIndex == null) return

        const fetchTable = async () => {
            setLoading(true)
            setError(null)
            try {
                const tableHtml = await traceService.getFrameTableHtml(
                    projectId,
                    frameIndex
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
    }, [projectId, frameIndex, stackVersion, t])

    const sanitizedHtml = useMemo(
        () => (html ? DOMPurify.sanitize(html, { USE_PROFILES: { html: true } }) : ''),
        [html]
    )

    if (loading) {
        return (
            <Card
                className={styles.card}
                size="small"
                title={t('details.table')}
            >
                <div className={styles.loading}>
                    <Spin description={t('loadingTable')} />
                </div>
            </Card>
        )
    }

    if (error) {
        return (
            <Card
                className={styles.card}
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
            className={styles.card}
            size="small"
            title={t('details.table')}
        >
            <div
                className={styles.content}
                dangerouslySetInnerHTML={{ __html: sanitizedHtml }}
            />
        </Card>
    )
}

export default TraceTableView

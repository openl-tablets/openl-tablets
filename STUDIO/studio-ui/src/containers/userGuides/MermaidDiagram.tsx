import React, { useEffect, useId, useState } from 'react'
import { Alert, Skeleton } from 'antd'
import { useThemeMode } from 'antd-style'
import { useTranslation } from 'react-i18next'
import { errorHandler } from 'utils/errorHandling'
import { useStyles } from './UserGuides.styles'

interface MermaidDiagramProps {
    /** The Mermaid text of the diagram. */
    source: string
}

/**
 * Draws a Mermaid diagram of a page in the appearance of the application.
 *
 * Mermaid is loaded with the first diagram a reader opens, so a page without diagrams never downloads it. A diagram
 * that cannot be drawn shows its text instead.
 */
export const MermaidDiagram: React.FC<MermaidDiagramProps> = ({ source }) => {
    const { t } = useTranslation()
    const { styles } = useStyles()
    const { isDarkMode } = useThemeMode()
    // Mermaid draws the diagram into an element of this id, so it has to be a valid one.
    const id = `guide-diagram-${useId().replaceAll(/\W/g, '')}`
    const [svg, setSvg] = useState<string>()
    const [failed, setFailed] = useState(false)

    useEffect(() => {
        let current = true
        import('mermaid')
            .then(async ({ default: mermaid }) => {
                mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', theme: isDarkMode ? 'dark' : 'default' })
                const drawn = await mermaid.render(id, source)
                if (current) {
                    setSvg(drawn.svg)
                    setFailed(false)
                }
            })
            .catch((error: unknown) => {
                errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
                if (current) {
                    setFailed(true)
                }
            })
        return () => {
            current = false
        }
    }, [id, isDarkMode, source])

    if (failed) {
        return (
            <>
                <Alert showIcon title={t('guides:diagram_failed')} type="warning" />
                <pre>{source}</pre>
            </>
        )
    }
    if (svg === undefined) {
        return <Skeleton active paragraph={{ rows: 4 }} title={false} />
    }
    // Mermaid sanitizes the diagram it draws under the strict security level.
    return <div className={styles.diagram} dangerouslySetInnerHTML={{ __html: svg }} data-testid="guide-diagram" />
}

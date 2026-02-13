import { useState, useRef, useCallback } from 'react'
import { notification } from 'antd'
import { useTranslation } from 'react-i18next'

interface UseCopyToClipboardResult {
    /** Whether content was recently copied (resets after 2 seconds) */
    copied: boolean
    /** Whether a copy operation is in progress */
    copying: boolean
    /** Copy text to clipboard with feedback */
    copyToClipboard: (text: string) => Promise<void>
}

/**
 * Hook for copying text to clipboard with visual feedback.
 * Shows 'Copied!' state for 2 seconds after successful copy.
 * Shows error notification on failure.
 */
export const useCopyToClipboard = (): UseCopyToClipboardResult => {
    const { t } = useTranslation('trace')
    const [copied, setCopied] = useState(false)
    const [copying, setCopying] = useState(false)
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    const copyToClipboard = useCallback(async (text: string) => {
        setCopying(true)
        try {
            await navigator.clipboard.writeText(text)
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
            }
            setCopied(true)
            timeoutRef.current = setTimeout(() => setCopied(false), 2000)
        } catch {
            notification.error({ message: t('copy.failed') })
        } finally {
            setCopying(false)
        }
    }, [t])

    return { copied, copying, copyToClipboard }
}

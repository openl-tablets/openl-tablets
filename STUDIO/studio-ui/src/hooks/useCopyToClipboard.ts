import { useCallback, useEffect, useRef, useState } from 'react'
import { notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { errorHandler } from 'utils/errorHandling'

interface UseCopyToClipboardResult {
    /** Whether content was recently copied (resets after 2 seconds) */
    copied: boolean
    /** Whether a copy operation is in progress */
    copying: boolean
    /** Copy text to clipboard with feedback */
    copyToClipboard: (text: string) => Promise<void>
}

/**
 * Copies text through a hidden textarea and the `execCommand` API.
 *
 * `execCommand` is deprecated, but it is the only copy mechanism in insecure
 * contexts (plain HTTP on a non-localhost origin), where `navigator.clipboard`
 * does not exist.
 */
const legacyCopy = (text: string): boolean => {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    try {
        return document.execCommand('copy')
    } finally {
        textarea.remove()
    }
}

const writeToClipboard = async (text: string): Promise<boolean> => {
    if (navigator.clipboard) {
        try {
            await navigator.clipboard.writeText(text)
            return true
        } catch {
            // Write permission denied or the document lost focus — try the legacy path
        }
    }
    return legacyCopy(text)
}

/**
 * Hook for copying text to clipboard with visual feedback.
 * Shows the copied state for 2 seconds after a successful copy.
 * Shows an error notification on failure.
 *
 * Works both in secure contexts (async Clipboard API) and on plain HTTP
 * origins (legacy `execCommand` fallback).
 */
export const useCopyToClipboard = (): UseCopyToClipboardResult => {
    const { t } = useTranslation()
    const [copied, setCopied] = useState(false)
    const [copying, setCopying] = useState(false)
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

    const copyToClipboard = useCallback(async (text: string) => {
        setCopying(true)
        try {
            const success = await writeToClipboard(text)
            if (!success) {
                throw new Error('The copy command was rejected')
            }
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
            }
            setCopied(true)
            timeoutRef.current = setTimeout(() => setCopied(false), 2000)
        } catch (error) {
            errorHandler.logError(error instanceof Error ? error : new Error(String(error)))
            notification.error({ title: t('common:copy_failed') })
        } finally {
            setCopying(false)
        }
    }, [t])

    useEffect(() => {
        return () => {
            if (timeoutRef.current) {
                clearTimeout(timeoutRef.current)
            }
        }
    }, [])

    return { copied, copying, copyToClipboard }
}

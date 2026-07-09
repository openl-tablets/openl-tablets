import React from 'react'
import { Spin } from 'antd'
import { useAppStore } from 'store'
import { useStyles } from './LoadingOverlay.styles'

/**
 * Full-screen click shield with a centered spinner, shown while an operation is running.
 *
 * The overlay opens on the first {@code showLoader()} and closes when every call is paired
 * with {@code hideLoader()}, so concurrent operations keep it open until the last one ends.
 * Legacy JSF pages drive it through {@code globalThis.openl.loader}.
 */
export const LoadingOverlay: React.FC = () => {
    const loading = useAppStore((state) => state.loaderCount > 0)
    const { styles } = useStyles()

    if (!loading) {
        return null
    }
    return (
        <div className={styles.overlay} data-testid="loading-overlay">
            <Spin size="large" />
        </div>
    )
}

import React from 'react'
import { Button } from 'antd'
import { useActionBandStyles } from './actionBand.styles'

interface BandActionProps {
    label: string
    icon: React.ReactNode
    onClick: (event: React.MouseEvent<HTMLElement>) => void
    disabled?: boolean | undefined
    /** Whether what the action started still goes on. */
    loading?: boolean | undefined
    testId: string
}

/** An action of the band over what a screen shows: its picture with its name under it. */
export const BandAction = ({ label, icon, onClick, disabled = false, loading = false, testId }: BandActionProps) => {
    const { styles } = useActionBandStyles()
    return (
        <Button
            className={styles.action}
            data-testid={testId}
            disabled={disabled}
            icon={icon}
            loading={loading}
            onClick={onClick}
            size="small"
            type="text"
        >
            <span className={styles.label}>{label}</span>
        </Button>
    )
}
